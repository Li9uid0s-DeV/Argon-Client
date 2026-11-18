package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class TileEntityTrainStation extends TileEntityCreateBase
{
    private String stationName = "Station";

    @Override
    public String getCreatePartId()
    {
        return "train_station";
    }

    public String getStationName()
    {
        return this.stationName;
    }

    public void setStationName(String name)
    {
        this.stationName = (name != null && name.trim().length() > 0) ? name.trim() : "Station";
        this.markDirty();
    }

    public BlockPos findNearbyTrackPos()
    {
        if (this.world == null)
        {
            return null;
        }

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dy = -2; dy <= 2; ++dy)
        {
            for (int dx = -4; dx <= 4; ++dx)
            {
                for (int dz = -4; dz <= 4; ++dz)
                {
                    BlockPos check = this.pos.add(dx, dy, dz);

                    if (this.world.isBlockLoaded(check) && this.world.getBlockState(check).getBlock() == CreateBlocks.TRAIN_TRACK)
                    {
                        double d = this.pos.distanceSq(check);

                        if (d < bestDist)
                        {
                            bestDist = d;
                            best = check;
                        }
                    }
                }
            }
        }

        return best;
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        lines.add("\u00a76Station: \u00a7f" + this.stationName);
        BlockPos tr = this.findNearbyTrackPos();
        lines.add(tr != null ? "\u00a7aTrack Linked" : "\u00a7cNo Track Within 4 Blocks");
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        if (compound.hasKey("StationName"))
        {
            this.stationName = compound.getString("StationName");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setString("StationName", this.stationName);
        return compound;
    }
}
