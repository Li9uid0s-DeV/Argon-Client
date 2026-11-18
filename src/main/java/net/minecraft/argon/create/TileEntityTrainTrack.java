package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityTrainTrack extends TileEntityCreateBase
{
    private boolean hasLink = false;
    private BlockPos linkedPos = BlockPos.ORIGIN;
    private EnumFacing linkedFacing = EnumFacing.NORTH;

    @Override
    public String getCreatePartId()
    {
        return "train_track";
    }

    public boolean hasCurvedLink()
    {
        return this.hasLink && this.linkedPos != null && !this.linkedPos.equals(this.pos);
    }

    public BlockPos getLinkedPos()
    {
        return this.linkedPos;
    }

    public void setCurvedLink(BlockPos targetPos, EnumFacing targetFacing)
    {
        if (targetPos == null || targetPos.equals(this.pos))
        {
            this.hasLink = false;
            this.linkedPos = BlockPos.ORIGIN;
        }
        else
        {
            this.hasLink = true;
            this.linkedPos = targetPos;
            this.linkedFacing = targetFacing != null ? targetFacing : EnumFacing.NORTH;
        }

        this.markDirty();

        if (this.world != null)
        {
            IBlockState st = this.world.getBlockState(this.pos);
            this.syncStateToBlock(st);
        }
    }

    public void evaluateSpline(double t, double[] outPosYaw)
    {
        double p0x = (double)this.pos.getX() + 0.5D;
        double p0y = (double)this.pos.getY();
        double p0z = (double)this.pos.getZ() + 0.5D;

        if (!this.hasCurvedLink())
        {
            outPosYaw[0] = p0x;
            outPosYaw[1] = p0y;
            outPosYaw[2] = p0z;
            outPosYaw[3] = (double)this.facing.getHorizontalAngle();
            return;
        }

        double p1x = (double)this.linkedPos.getX() + 0.5D;
        double p1y = (double)this.linkedPos.getY();
        double p1z = (double)this.linkedPos.getZ() + 0.5D;

        double dist = Math.sqrt((p1x - p0x) * (p1x - p0x) + (p1y - p0y) * (p1y - p0y) + (p1z - p0z) * (p1z - p0z));
        double scale = Math.max(2.0D, dist * 0.65D);

        EnumFacing d0 = this.facing;
        double dx = p1x - p0x;
        double dz = p1z - p0z;

        if (d0.getFrontOffsetX() * dx + d0.getFrontOffsetZ() * dz < 0.0D)
        {
            d0 = d0.getOpposite();
        }

        EnumFacing d1 = this.linkedFacing;

        if (d1.getFrontOffsetX() * dx + d1.getFrontOffsetZ() * dz < 0.0D)
        {
            d1 = d1.getOpposite();
        }

        double m0x = (double)d0.getFrontOffsetX() * scale;
        double m0z = (double)d0.getFrontOffsetZ() * scale;
        double m1x = (double)d1.getFrontOffsetX() * scale;
        double m1z = (double)d1.getFrontOffsetZ() * scale;

        double t2 = t * t;
        double t3 = t2 * t;
        double h00 = 2.0D * t3 - 3.0D * t2 + 1.0D;
        double h10 = t3 - 2.0D * t2 + t;
        double h01 = -2.0D * t3 + 3.0D * t2;
        double h11 = t3 - t2;

        double x = h00 * p0x + h10 * m0x + h01 * p1x + h11 * m1x;
        double y = (1.0D - t) * p0y + t * p1y;
        double z = h00 * p0z + h10 * m0z + h01 * p1z + h11 * m1z;

        double dh00 = 6.0D * t2 - 6.0D * t;
        double dh10 = 3.0D * t2 - 4.0D * t + 1.0D;
        double dh01 = -6.0D * t2 + 6.0D * t;
        double dh11 = 3.0D * t2 - 2.0D * t;

        double vx = dh00 * p0x + dh10 * m0x + dh01 * p1x + dh11 * m1x;
        double vz = dh00 * p0z + dh10 * m0z + dh01 * p1z + dh11 * m1z;
        double yaw = Math.toDegrees(Math.atan2(-vx, vz));

        outPosYaw[0] = x;
        outPosYaw[1] = y;
        outPosYaw[2] = z;
        outPosYaw[3] = yaw;
    }

    public void queueRailBatches()
    {
        float ax = (float)this.pos.getX() + 0.5F - (float)this.facing.getFrontOffsetX() * 0.5F;
        float ay = (float)this.pos.getY();
        float az = (float)this.pos.getZ() + 0.5F - (float)this.facing.getFrontOffsetZ() * 0.5F;
        float bx = (float)this.pos.getX() + 0.5F + (float)this.facing.getFrontOffsetX() * 0.5F;
        float by = (float)this.pos.getY();
        float bz = (float)this.pos.getZ() + 0.5F + (float)this.facing.getFrontOffsetZ() * 0.5F;
        FlywheelBatchRenderer.enqueueRailSegment(ax, ay, az, bx, by, bz);

        if (this.hasCurvedLink())
        {
            double[] prev = new double[4];
            double[] cur = new double[4];
            this.evaluateSpline(0.0D, prev);
            int segments = 16;

            for (int i = 1; i <= segments; ++i)
            {
                this.evaluateSpline((double)i / (double)segments, cur);
                FlywheelBatchRenderer.enqueueRailSegment(
                    (float)prev[0], (float)prev[1], (float)prev[2],
                    (float)cur[0], (float)cur[1], (float)cur[2]);
                prev[0] = cur[0];
                prev[1] = cur[1];
                prev[2] = cur[2];
            }
        }
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        lines.add("\u00a77Track Dir: \u00a7f" + this.facing.getName().toUpperCase());

        if (this.hasCurvedLink())
        {
            lines.add("\u00a7bCurved Link -> \u00a7f(" + this.linkedPos.getX() + ", " + this.linkedPos.getY() + ", " + this.linkedPos.getZ() + ")");
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.hasLink = compound.getBoolean("HasLink");

        if (this.hasLink)
        {
            this.linkedPos = new BlockPos(compound.getInteger("LX"), compound.getInteger("LY"), compound.getInteger("LZ"));
            this.linkedFacing = EnumFacing.getFront(compound.getByte("LFacing") & 7);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setBoolean("HasLink", this.hasLink);

        if (this.hasLink)
        {
            compound.setInteger("LX", this.linkedPos.getX());
            compound.setInteger("LY", this.linkedPos.getY());
            compound.setInteger("LZ", this.linkedPos.getZ());
            compound.setByte("LFacing", (byte)this.linkedFacing.getIndex());
        }

        return compound;
    }
}
