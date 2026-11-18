package net.minecraft.argon.create;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class TileEntityWindmillBearing extends TileEntityKinetic
{
    private int scanTimer = 0;
    private int sailCount = 0;

    @Override
    public String getCreatePartId()
    {
        return "windmill_bearing";
    }

    @Override
    public float getGeneratedCapacitySU()
    {
        if (this.sailCount < 4)
        {
            return 0.0F;
        }

        return (float)Math.min(8192, this.sailCount * 64);
    }

    @Override
    public float getGeneratedSpeedRPM()
    {
        if (this.sailCount < 4)
        {
            return 0.0F;
        }

        int speed = Math.min(32, 4 + this.sailCount / 4);
        return (float)speed * (float)this.directionMultiplier;
    }

    @Override
    public void update()
    {
        super.update();

        if (this.world != null && (++this.scanTimer % 30 == 0 || this.scanTimer == 1))
        {
            int count = this.scanAttachedSails();

            if (count != this.sailCount)
            {
                this.sailCount = count;
                KineticNetwork.scheduleUpdate(this.world, this.pos);
            }
        }
    }

    private int scanAttachedSails()
    {
        BlockPos front = this.pos.offset(this.facing);
        ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
        Set<BlockPos> visited = new HashSet<BlockPos>();
        queue.add(front);
        visited.add(front);
        int sails = 0;

        while (!queue.isEmpty() && visited.size() < 128)
        {
            BlockPos p = queue.poll();

            if (!this.world.isBlockLoaded(p))
            {
                continue;
            }

            Block b = this.world.getBlockState(p).getBlock();

            if (b == CreateBlocks.SAIL || b == Blocks.WOOL || b == Blocks.PLANKS)
            {
                if (b == CreateBlocks.SAIL || b == Blocks.WOOL)
                {
                    ++sails;
                }

                for (EnumFacing dir : EnumFacing.VALUES)
                {
                    BlockPos np = p.offset(dir);

                    if (!np.equals(this.pos) && !visited.contains(np) && np.distanceSq(this.pos) <= 100.0D)
                    {
                        visited.add(np);
                        queue.add(np);
                    }
                }
            }
        }

        // Allow built-in rotor sails even with 4+ wool/sail blocks directly around front or standalone rotor
        return Math.max(sails, 0);
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        super.appendJadeInfo(lines);
        lines.add("\u00a77Sails: \u00a7f" + this.sailCount + (this.sailCount < 4 ? " \u00a7c(Need >=4 Wool/Sail blocks in front)" : " \u00a7aActive"));
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        float ang = this.getInterpolatedAngle(partialTicks);
        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.WINDMILL_BEARING, this.pos, this.facing.getAxis(), 0.0F, light);

        if (this.sailCount >= 4)
        {
            BlockPos front = this.pos.offset(this.facing);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.WINDMILL_SAIL_ROTOR, front, this.facing.getAxis(), ang, light);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.sailCount = compound.getInteger("SailCount");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setInteger("SailCount", this.sailCount);
        return compound;
    }
}
