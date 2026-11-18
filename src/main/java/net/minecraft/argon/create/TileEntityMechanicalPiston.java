package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityMechanicalPiston extends TileEntityKinetic
{
    private float offset = 0.0F;
    private float prevOffset = 0.0F;
    private int maxRange = 8;
    private ContraptionStructure activePayload = null;
    private final ContraptionMeshCache meshCache = new ContraptionMeshCache();

    @Override
    public String getCreatePartId()
    {
        return "mechanical_piston";
    }

    @Override
    public float getStressImpactFactor()
    {
        return 4.0F;
    }

    public ContraptionStructure getActivePayload()
    {
        return this.activePayload;
    }

    public ContraptionMeshCache getMeshCache()
    {
        return this.meshCache;
    }

    public float getInterpolatedOffset(float partialTicks)
    {
        return this.prevOffset + (this.offset - this.prevOffset) * partialTicks;
    }

    @Override
    public void update()
    {
        super.update();
        this.prevOffset = this.offset;

        if (this.world == null)
        {
            return;
        }

        if (this.overstressed || Math.abs(this.speedRPM) < 0.1F)
        {
            return;
        }

        float step = Math.min(0.25F, Math.abs(this.speedRPM) * 0.0025F);
        boolean extending = this.speedRPM > 0.0F;

        if (extending && this.offset < (float)this.maxRange)
        {
            if (this.activePayload == null)
            {
                this.maxRange = Math.max(1, this.countAttachedPoles());
                BlockPos headStart = this.pos.offset(this.facing);
                List<BlockPos> glued = SuperGlueManager.collectGluedBlocks(this.world, headStart, this.pos, 256);

                if (!glued.isEmpty())
                {
                    this.activePayload = ContraptionStructure.captureAndRemoveFromWorld(this.world, headStart, glued);
                }
            }

            float prev = this.offset;
            this.offset = Math.min((float)this.maxRange, this.offset + step);
            this.carryEntities(this.offset - prev);
        }
        else if (!extending && this.offset > 0.0F)
        {
            float prev = this.offset;
            this.offset = Math.max(0.0F, this.offset - step);
            this.carryEntities(this.offset - prev);

            if (this.offset <= 0.001F && this.activePayload != null)
            {
                BlockPos headStart = this.pos.offset(this.facing);
                this.activePayload.placeIntoWorld(this.world, headStart);
                this.activePayload = null;
                this.meshCache.dispose();
            }
        }
    }

    private int countAttachedPoles()
    {
        int poles = 0;
        EnumFacing back = this.facing.getOpposite();

        for (int i = 1; i <= 16; ++i)
        {
            BlockPos check = this.pos.offset(back, i);

            if (this.world.isBlockLoaded(check) && this.world.getBlockState(check).getBlock() == CreateBlocks.PISTON_POLE)
            {
                ++poles;
            }
            else
            {
                break;
            }
        }

        return Math.max(4, poles);
    }

    private void carryEntities(float deltaMove)
    {
        if (this.activePayload == null || Math.abs(deltaMove) < 0.0001F)
        {
            return;
        }

        double dx = (double)this.facing.getFrontOffsetX() * (double)this.offset;
        double dy = (double)this.facing.getFrontOffsetY() * (double)this.offset;
        double dz = (double)this.facing.getFrontOffsetZ() * (double)this.offset;
        BlockPos headStart = this.pos.offset(this.facing);
        AxisAlignedBB box = this.activePayload.getWorldBounds(
            (double)headStart.getX() + 0.5D + dx,
            (double)headStart.getY() + dy,
            (double)headStart.getZ() + 0.5D + dz).grow(0.35D);

        List<Entity> list = this.world.getEntitiesWithinAABB(Entity.class, box);

        for (int i = 0; i < list.size(); ++i)
        {
            Entity e = list.get(i);
            e.setPosition(
                e.posX + (double)this.facing.getFrontOffsetX() * (double)deltaMove,
                e.posY + (double)this.facing.getFrontOffsetY() * (double)deltaMove,
                e.posZ + (double)this.facing.getFrontOffsetZ() * (double)deltaMove);
        }
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        this.meshCache.dispose();
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        super.appendJadeInfo(lines);
        lines.add("\u00a77Extension: \u00a7f" + String.format("%.1f / %d m", this.offset, this.maxRange));
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        EnumFacing.Axis faceAx = this.facing != null ? this.facing.getAxis() : EnumFacing.Axis.Y;
        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.PISTON_CASING, this.pos, faceAx, 0.0F, light);
        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.SHAFT, this.pos, this.axis, this.getInterpolatedAngle(partialTicks), light);

        float ext = this.getInterpolatedOffset(partialTicks);
        int poleSteps = (int)Math.ceil(ext);

        for (int i = 1; i <= poleSteps; ++i)
        {
            float d = Math.min(ext, (float)i) - 0.5F;
            FlywheelBatchRenderer.enqueue(
                FlywheelBatchRenderer.ModelId.PISTON_POLE,
                (float)this.pos.getX() + 0.5F + (float)this.facing.getFrontOffsetX() * d,
                (float)this.pos.getY() + 0.5F + (float)this.facing.getFrontOffsetY() * d,
                (float)this.pos.getZ() + 0.5F + (float)this.facing.getFrontOffsetZ() * d,
                faceAx, 0.0F, 1.0F, 1.0F, 1.0F, light);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.offset = compound.getFloat("PistonOff");
        this.prevOffset = this.offset;
        this.maxRange = Math.max(1, compound.getInteger("PistonRange"));

        if (compound.hasKey("Payload"))
        {
            this.activePayload = ContraptionStructure.readFromNBT(compound.getCompoundTag("Payload"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setFloat("PistonOff", this.offset);
        compound.setInteger("PistonRange", this.maxRange);

        if (this.activePayload != null)
        {
            compound.setTag("Payload", this.activePayload.writeToNBT());
        }

        return compound;
    }
}
