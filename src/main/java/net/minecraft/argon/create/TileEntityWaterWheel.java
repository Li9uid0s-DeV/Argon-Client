package net.minecraft.argon.create;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class TileEntityWaterWheel extends TileEntityKinetic
{
    private int scanTimer = 0;
    private int activeWaterFaces = 0;

    @Override
    public String getCreatePartId()
    {
        return "water_wheel";
    }

    @Override
    public float getGeneratedCapacitySU()
    {
        return this.activeWaterFaces > 0 ? (float)(128 + this.activeWaterFaces * 64) : 0.0F;
    }

    @Override
    public float getGeneratedSpeedRPM()
    {
        return this.activeWaterFaces > 0 ? (float)(8 + Math.min(24, this.activeWaterFaces * 4)) * (float)this.directionMultiplier : 0.0F;
    }

    @Override
    public void update()
    {
        super.update();

        if (this.world != null && (++this.scanTimer % 20 == 0 || this.scanTimer == 1))
        {
            int waterCount = this.scanSurroundingWater();

            if (waterCount != this.activeWaterFaces)
            {
                this.activeWaterFaces = waterCount;
                KineticNetwork.scheduleUpdate(this.world, this.pos);
            }
        }
    }

    private int scanSurroundingWater()
    {
        int count = 0;

        for (int dx = -1; dx <= 1; ++dx)
        {
            for (int dy = -1; dy <= 1; ++dy)
            {
                for (int dz = -1; dz <= 1; ++dz)
                {
                    if (dx == 0 && dy == 0 && dz == 0)
                    {
                        continue;
                    }

                    if (this.axis == EnumFacing.Axis.X && dx != 0) continue;
                    if (this.axis == EnumFacing.Axis.Y && dy != 0) continue;
                    if (this.axis == EnumFacing.Axis.Z && dz != 0) continue;

                    BlockPos check = this.pos.add(dx, dy, dz);

                    if (this.world.isBlockLoaded(check))
                    {
                        IBlockState st = this.world.getBlockState(check);

                        if (st.getMaterial() == Material.WATER)
                        {
                            ++count;
                        }
                    }
                }
            }
        }

        return count;
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        float ang = this.getInterpolatedAngle(partialTicks);
        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.WATER_WHEEL, this.pos, this.axis, ang, light);
    }
}
