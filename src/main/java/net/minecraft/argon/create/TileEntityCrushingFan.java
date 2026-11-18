package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityCrushingFan extends TileEntityKinetic
{
    public static final int KIND_CRUSHING_WHEEL = 0;
    public static final int KIND_ENCASED_FAN = 1;

    private int kind = KIND_CRUSHING_WHEEL;
    private int processTick = 0;

    public TileEntityCrushingFan()
    {
    }

    public TileEntityCrushingFan(int kind)
    {
        this.kind = kind;
    }

    public int getKind()
    {
        return this.kind;
    }

    @Override
    public String getCreatePartId()
    {
        return this.kind == KIND_ENCASED_FAN ? "encased_fan" : "crushing_wheel";
    }

    @Override
    public float getStressImpactFactor()
    {
        return this.kind == KIND_ENCASED_FAN ? 2.0F : 8.0F;
    }

    @Override
    public void update()
    {
        super.update();

        if (this.world == null || this.overstressed || Math.abs(this.speedRPM) < 0.1F)
        {
            return;
        }

        if (this.kind == KIND_CRUSHING_WHEEL)
        {
            this.tickCrusher();
        }
        else
        {
            this.tickFan();
        }
    }

    private void tickCrusher()
    {
        if (++this.processTick < 10)
        {
            return;
        }

        this.processTick = 0;
        AxisAlignedBB zone = new AxisAlignedBB(
            this.pos.getX() - 1.0D, this.pos.getY() - 0.5D, this.pos.getZ() - 1.0D,
            this.pos.getX() + 2.0D, this.pos.getY() + 1.5D, this.pos.getZ() + 2.0D);
        List<EntityItem> items = this.world.getEntitiesWithinAABB(EntityItem.class, zone);

        for (int i = 0; i < items.size(); ++i)
        {
            EntityItem ei = items.get(i);
            ItemStack res = CreateRecipes.getCrushingResult(ei.getItem());

            if (!res.isEmpty())
            {
                if (!this.world.isRemote)
                {
                    ei.getItem().shrink(1);

                    if (ei.getItem().isEmpty())
                    {
                        ei.setDead();
                    }

                    EntityItem out = new EntityItem(this.world, ei.posX, (double)this.pos.getY() - 0.3D, ei.posZ, res.copy());
                    out.motionX = 0.0D;
                    out.motionY = -0.15D;
                    out.motionZ = 0.0D;
                    this.world.spawnEntity(out);
                    this.world.playSound(null, this.pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 0.45F, 0.85F);
                }

                break;
            }
        }
    }

    private void tickFan()
    {
        int range = 4 + Math.min(12, (int)(Math.abs(this.speedRPM) / 16.0F));
        boolean pushing = this.speedRPM >= 0.0F;
        EnumFacing blowDir = this.facing;
        int catalyst = 0; // 1=Water, 2=Fire, 3=Lava

        // Check catalyst right in front of fan
        BlockPos front1 = this.pos.offset(blowDir);

        if (this.world.isBlockLoaded(front1))
        {
            Block b = this.world.getBlockState(front1).getBlock();

            if (b == Blocks.LAVA || b == Blocks.FLOWING_LAVA)
            {
                catalyst = 3;
            }
            else if (b == Blocks.FIRE || b == Blocks.MAGMA)
            {
                catalyst = 2;
            }
            else if (b == Blocks.WATER || b == Blocks.FLOWING_WATER || this.world.getBlockState(front1).getMaterial() == Material.WATER)
            {
                catalyst = 1;
            }
        }

        // Compute AABB beam along blowDir
        int minX = Math.min(this.pos.getX(), this.pos.getX() + blowDir.getFrontOffsetX() * range);
        int minY = Math.min(this.pos.getY(), this.pos.getY() + blowDir.getFrontOffsetY() * range);
        int minZ = Math.min(this.pos.getZ(), this.pos.getZ() + blowDir.getFrontOffsetZ() * range);
        int maxX = Math.max(this.pos.getX(), this.pos.getX() + blowDir.getFrontOffsetX() * range) + 1;
        int maxY = Math.max(this.pos.getY(), this.pos.getY() + blowDir.getFrontOffsetY() * range) + 1;
        int maxZ = Math.max(this.pos.getZ(), this.pos.getZ() + blowDir.getFrontOffsetZ() * range) + 1;
        AxisAlignedBB beam = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        double force = Math.min(0.22D, Math.abs(this.speedRPM) * 0.0022D) * (pushing ? 1.0D : -1.0D);
        List<Entity> ents = this.world.getEntitiesWithinAABB(Entity.class, beam);

        for (int i = 0; i < ents.size(); ++i)
        {
            Entity e = ents.get(i);

            if (!e.isSneaking())
            {
                e.motionX += (double)blowDir.getFrontOffsetX() * force;
                e.motionY += (double)blowDir.getFrontOffsetY() * force * 0.85D;
                e.motionZ += (double)blowDir.getFrontOffsetZ() * force;
                e.fallDistance = 0.0F;
            }

            if (catalyst > 0 && pushing && e instanceof EntityItem && !this.world.isRemote && (e.ticksExisted % 20 == 0))
            {
                EntityItem ei = (EntityItem)e;
                ItemStack res = CreateRecipes.getFanResult(ei.getItem(), catalyst);

                if (!res.isEmpty())
                {
                    ei.getItem().shrink(1);

                    if (ei.getItem().isEmpty())
                    {
                        ei.setDead();
                    }

                    EntityItem out = new EntityItem(this.world, ei.posX, ei.posY, ei.posZ, res.copy());
                    this.world.spawnEntity(out);
                }
            }
        }

        if (this.world.isRemote && (this.world.getTotalWorldTime() % 4L == 0L))
        {
            EnumParticleTypes pt = catalyst == 3 ? EnumParticleTypes.FLAME : (catalyst == 2 ? EnumParticleTypes.SMOKE_NORMAL : (catalyst == 1 ? EnumParticleTypes.WATER_SPLASH : EnumParticleTypes.CLOUD));
            this.world.spawnParticle(pt,
                (double)this.pos.getX() + 0.5D + (double)blowDir.getFrontOffsetX() * 0.65D,
                (double)this.pos.getY() + 0.5D + (double)blowDir.getFrontOffsetY() * 0.65D,
                (double)this.pos.getZ() + 0.5D + (double)blowDir.getFrontOffsetZ() * 0.65D,
                (double)blowDir.getFrontOffsetX() * force * 1.4D,
                (double)blowDir.getFrontOffsetY() * force * 1.4D,
                (double)blowDir.getFrontOffsetZ() * force * 1.4D);
        }
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        float ang = this.getInterpolatedAngle(partialTicks);

        if (this.kind == KIND_CRUSHING_WHEEL)
        {
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.CRUSHING_WHEEL, this.pos, this.axis, ang, light);
        }
        else
        {
            EnumFacing.Axis fanAx = this.facing != null ? this.facing.getAxis() : this.axis;
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.FAN_CASING, this.pos, fanAx, 0.0F, light);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.FAN_BLADES, this.pos, fanAx, ang * 2.0F, light);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.kind = compound.getByte("CrushingFanKind");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("CrushingFanKind", (byte)this.kind);
        return compound;
    }
}
