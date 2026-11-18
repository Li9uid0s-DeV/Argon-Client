package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityGantryCarriage extends TileEntityKinetic
{
    private float slidePos = 0.0F;
    private float prevSlidePos = 0.0F;
    private EnumFacing slideAxisPositive = EnumFacing.EAST;
    private ContraptionStructure activePayload = null;
    private final ContraptionMeshCache meshCache = new ContraptionMeshCache();

    @Override
    public String getCreatePartId()
    {
        return "gantry_carriage";
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

    public EnumFacing getSlideDirection()
    {
        return this.slideAxisPositive;
    }

    public float getInterpolatedSlide(float partialTicks)
    {
        return this.prevSlidePos + (this.slidePos - this.prevSlidePos) * partialTicks;
    }

    @Override
    public void update()
    {
        super.update();
        this.prevSlidePos = this.slidePos;

        if (this.world == null)
        {
            return;
        }

        // Look for adjacent Gantry Shaft rail
        TileEntityKinetic shaftNode = null;

        for (EnumFacing dir : EnumFacing.VALUES)
        {
            TileEntity te = this.world.getTileEntity(this.pos.offset(dir));

            if (te instanceof TileEntityKinetic && ((TileEntityKinetic)te).isGantryShaft())
            {
                shaftNode = (TileEntityKinetic)te;
                EnumFacing.Axis sAx = shaftNode.getAxis();
                this.slideAxisPositive = sAx == EnumFacing.Axis.X ? EnumFacing.EAST : (sAx == EnumFacing.Axis.Z ? EnumFacing.SOUTH : EnumFacing.UP);
                break;
            }
        }

        if (shaftNode == null || shaftNode.isOverstressed() || Math.abs(shaftNode.getSpeedRPM()) < 0.1F)
        {
            return;
        }

        float speed = shaftNode.getSpeedRPM();
        float delta = Math.min(0.25F, Math.abs(speed) * 0.0025F) * (speed >= 0.0F ? 1.0F : -1.0F);
        int maxForward = this.countGantryShafts(this.slideAxisPositive);
        int maxBackward = this.countGantryShafts(this.slideAxisPositive.getOpposite());

        float nextSlide = this.slidePos + delta;

        if (nextSlide > (float)maxForward) nextSlide = (float)maxForward;
        if (nextSlide < -(float)maxBackward) nextSlide = -(float)maxBackward;

        if (Math.abs(nextSlide - this.slidePos) > 0.0001F)
        {
            if (this.activePayload == null)
            {
                BlockPos attachSeed = this.pos.offset(this.facing);
                List<BlockPos> glued = SuperGlueManager.collectGluedBlocks(this.world, attachSeed, this.pos, 256);

                if (!glued.isEmpty())
                {
                    this.activePayload = ContraptionStructure.captureAndRemoveFromWorld(this.world, this.pos, glued);
                }
            }

            float moved = nextSlide - this.slidePos;
            this.slidePos = nextSlide;
            this.carryEntities(moved);
        }
    }

    private int countGantryShafts(EnumFacing dir)
    {
        int count = 0;

        for (int i = 1; i <= 24; ++i)
        {
            BlockPos check = this.pos.offset(dir, i);

            if (!this.world.isBlockLoaded(check))
            {
                break;
            }

            // Gantry carriage slides parallel to gantry shaft block adjacent to it
            boolean foundShaft = false;

            for (EnumFacing side : EnumFacing.VALUES)
            {
                if (this.world.getBlockState(check.offset(side)).getBlock() == CreateBlocks.GANTRY_SHAFT)
                {
                    foundShaft = true;
                    break;
                }
            }

            if (foundShaft)
            {
                ++count;
            }
            else
            {
                break;
            }
        }

        return Math.max(4, count);
    }

    private void carryEntities(float delta)
    {
        if (this.activePayload == null)
        {
            return;
        }

        double ox = (double)this.pos.getX() + 0.5D + (double)this.slideAxisPositive.getFrontOffsetX() * (double)this.slidePos;
        double oy = (double)this.pos.getY() + (double)this.slideAxisPositive.getFrontOffsetY() * (double)this.slidePos;
        double oz = (double)this.pos.getZ() + 0.5D + (double)this.slideAxisPositive.getFrontOffsetZ() * (double)this.slidePos;
        AxisAlignedBB box = this.activePayload.getWorldBounds(ox, oy, oz).grow(0.35D);
        List<Entity> ents = this.world.getEntitiesWithinAABB(Entity.class, box);

        for (int i = 0; i < ents.size(); ++i)
        {
            Entity e = ents.get(i);
            e.setPosition(
                e.posX + (double)this.slideAxisPositive.getFrontOffsetX() * (double)delta,
                e.posY + (double)this.slideAxisPositive.getFrontOffsetY() * (double)delta,
                e.posZ + (double)this.slideAxisPositive.getFrontOffsetZ() * (double)delta);
        }
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        this.meshCache.dispose();
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        float s = this.getInterpolatedSlide(partialTicks);
        float cx = (float)this.pos.getX() + 0.5F + (float)this.slideAxisPositive.getFrontOffsetX() * s;
        float cy = (float)this.pos.getY() + 0.5F + (float)this.slideAxisPositive.getFrontOffsetY() * s;
        float cz = (float)this.pos.getZ() + 0.5F + (float)this.slideAxisPositive.getFrontOffsetZ() * s;
        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.GANTRY_CARRIAGE, cx, cy, cz, EnumFacing.Axis.Y, 0.0F, 1.0F, 1.0F, 1.0F, light);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.slidePos = compound.getFloat("GantrySlide");
        this.prevSlidePos = this.slidePos;

        if (compound.hasKey("Payload"))
        {
            this.activePayload = ContraptionStructure.readFromNBT(compound.getCompoundTag("Payload"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setFloat("GantrySlide", this.slidePos);

        if (this.activePayload != null)
        {
            compound.setTag("Payload", this.activePayload.writeToNBT());
        }

        return compound;
    }
}
