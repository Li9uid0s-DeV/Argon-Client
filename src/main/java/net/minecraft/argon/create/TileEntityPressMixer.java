package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityPressMixer extends TileEntityKinetic
{
    public static final int KIND_PRESS = 0;
    public static final int KIND_MIXER = 1;

    private int kind = KIND_PRESS;
    private int processTimer = 0;
    private float animProgress = 0.0F;
    private float prevAnimProgress = 0.0F;

    public TileEntityPressMixer()
    {
    }

    public TileEntityPressMixer(int kind)
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
        return this.kind == KIND_MIXER ? "mechanical_mixer" : "mechanical_press";
    }

    @Override
    public boolean hasSmallCogwheel()
    {
        return this.kind == KIND_MIXER;
    }

    @Override
    public float getStressImpactFactor()
    {
        return this.kind == KIND_MIXER ? 4.0F : 8.0F;
    }

    @Override
    public void update()
    {
        super.update();
        this.prevAnimProgress = this.animProgress;

        if (this.world == null || this.overstressed || Math.abs(this.speedRPM) < 0.1F)
        {
            this.animProgress *= 0.85F;
            return;
        }

        if (this.kind == KIND_PRESS)
        {
            this.tickPress();
        }
        else
        {
            this.tickMixer();
        }
    }

    private void tickPress()
    {
        if (this.processTimer > 0)
        {
            --this.processTimer;
            this.animProgress = (float)Math.sin((1.0F - (float)this.processTimer / 20.0F) * Math.PI);
            return;
        }

        this.animProgress = 0.0F;
        // Scan 1 and 2 blocks below for dropped items, belt items, or Basin
        BlockPos scanPos = this.pos.down();
        TileEntity belowTe = this.world.getTileEntity(scanPos);

        if (!(belowTe instanceof TileEntityBasin) && !(belowTe instanceof TileEntityBelt))
        {
            scanPos = this.pos.down(2);
            belowTe = this.world.getTileEntity(scanPos);
        }

        // Check Basin items
        if (belowTe instanceof TileEntityBasin)
        {
            TileEntityBasin basin = (TileEntityBasin)belowTe;
            List<ItemStack> items = basin.getItems();

            for (int i = 0; i < items.size(); ++i)
            {
                ItemStack s = items.get(i);
                ItemStack res = CreateRecipes.getPressingResult(s);

                if (!res.isEmpty())
                {
                    if (!this.world.isRemote)
                    {
                        s.shrink(1);
                        EntityItem drop = new EntityItem(this.world, (double)scanPos.getX() + 0.5D, (double)scanPos.getY() + 0.9D, (double)scanPos.getZ() + 0.5D, res.copy());
                        this.world.spawnEntity(drop);
                        this.world.playSound(null, this.pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.35F, 1.3F);
                    }

                    this.processTimer = Math.max(8, (int)(40.0F / Math.max(1.0F, Math.abs(this.speedRPM) / 16.0F)));
                    return;
                }
            }
        }

        // Check dropped items on block/depot/belt below
        AxisAlignedBB box = new AxisAlignedBB(
            this.pos.getX(), this.pos.getY() - 2.0D, this.pos.getZ(),
            this.pos.getX() + 1.0D, this.pos.getY(), this.pos.getZ() + 1.0D);
        List<EntityItem> drops = this.world.getEntitiesWithinAABB(EntityItem.class, box);

        for (int i = 0; i < drops.size(); ++i)
        {
            EntityItem ei = drops.get(i);
            ItemStack res = CreateRecipes.getPressingResult(ei.getItem());

            if (!res.isEmpty())
            {
                if (!this.world.isRemote)
                {
                    ei.getItem().shrink(1);

                    if (ei.getItem().isEmpty())
                    {
                        ei.setDead();
                    }

                    EntityItem out = new EntityItem(this.world, ei.posX, ei.posY, ei.posZ, res.copy());
                    out.motionX = 0.0D;
                    out.motionY = 0.12D;
                    out.motionZ = 0.0D;
                    this.world.spawnEntity(out);
                    this.world.playSound(null, this.pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.35F, 1.4F);
                }

                this.processTimer = Math.max(8, (int)(36.0F / Math.max(1.0F, Math.abs(this.speedRPM) / 16.0F)));
                return;
            }
        }
    }

    private void tickMixer()
    {
        if (Math.abs(this.speedRPM) < 30.0F)
        {
            this.animProgress = 0.0F;
            return;
        }

        BlockPos basinPos = this.pos.down(2);
        TileEntity te = this.world.getTileEntity(basinPos);

        if (!(te instanceof TileEntityBasin))
        {
            this.animProgress = 0.0F;
            return;
        }

        TileEntityBasin basin = (TileEntityBasin)te;
        boolean heated = false;
        TileEntity underTe = this.world.getTileEntity(basinPos.down());

        if (underTe instanceof TileEntityBlazeSteam && ((TileEntityBlazeSteam)underTe).isHeated())
        {
            heated = true;
        }

        ItemStack preview = CreateRecipes.tryMixBasin(basin.getItems(), heated, basin.getFluidType(), basin.getFluidMb(), false);

        if (!preview.isEmpty())
        {
            this.animProgress = Math.min(1.0F, this.animProgress + 0.1F);

            if (++this.processTimer >= Math.max(15, (int)(60.0F / (Math.abs(this.speedRPM) / 32.0F))))
            {
                this.processTimer = 0;

                if (!this.world.isRemote)
                {
                    ItemStack result = CreateRecipes.tryMixBasin(basin.getItems(), heated, basin.getFluidType(), basin.getFluidMb(), true);

                    if (!result.isEmpty())
                    {
                        EntityItem out = new EntityItem(this.world, (double)basinPos.getX() + 0.5D, (double)basinPos.getY() + 0.9D, (double)basinPos.getZ() + 0.5D, result);
                        this.world.spawnEntity(out);
                        this.world.playSound(null, this.pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.5F, 1.1F);
                        basin.markDirty();
                    }
                }
            }
        }
        else
        {
            this.processTimer = 0;
            this.animProgress *= 0.85F;
        }
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        super.appendJadeInfo(lines);

        if (this.kind == KIND_MIXER && Math.abs(this.speedRPM) > 0.01F && Math.abs(this.speedRPM) < 30.0F)
        {
            lines.add("\u00a7cRequires >= 30 RPM to mix!");
        }
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        float ang = this.getInterpolatedAngle(partialTicks);
        float anim = this.prevAnimProgress + (this.animProgress - this.prevAnimProgress) * partialTicks;

        if (this.kind == KIND_PRESS)
        {
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.PRESS_CASING, this.pos, EnumFacing.Axis.Y, 0.0F, light);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.SHAFT, this.pos, this.axis, ang, light);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.PRESS_HEAD, (float)this.pos.getX() + 0.5F, (float)this.pos.getY() + 0.5F - anim * 0.72F, (float)this.pos.getZ() + 0.5F, EnumFacing.Axis.Y, 0.0F, 1.0F, 1.0F, 1.0F, light);
        }
        else
        {
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.MIXER_CASING, this.pos, EnumFacing.Axis.Y, 0.0F, light);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.COGWHEEL_SMALL, this.pos, EnumFacing.Axis.Y, ang, light);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.MIXER_WHISK, (float)this.pos.getX() + 0.5F, (float)this.pos.getY() + 0.5F - anim * 0.65F, (float)this.pos.getZ() + 0.5F, EnumFacing.Axis.Y, ang * 1.5F, 1.0F, 1.0F, 1.0F, light);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.kind = compound.getByte("PressMixerKind");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("PressMixerKind", (byte)this.kind);
        return compound;
    }
}
