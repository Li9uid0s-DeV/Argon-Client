package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;

public abstract class TileEntityKinetic extends TileEntityCreateBase
{
    protected float speedRPM = 0.0F;
    protected float networkCapacitySU = 0.0F;
    protected float networkStressSU = 0.0F;
    protected boolean overstressed = false;
    protected long networkId = 0L;
    protected int networkNodeCount = 1;
    protected boolean needsNetworkRefresh = true;
    protected int directionMultiplier = 1;

    protected float angle = 0.0F;
    protected float prevAngle = 0.0F;

    public float getSpeedRPM()
    {
        return this.speedRPM;
    }

    public float getNetworkCapacitySU()
    {
        return this.networkCapacitySU;
    }

    public float getNetworkStressSU()
    {
        return this.networkStressSU;
    }

    public boolean isOverstressed()
    {
        return this.overstressed;
    }

    public float getInterpolatedAngle(float partialTicks)
    {
        return this.prevAngle + (this.angle - this.prevAngle) * partialTicks;
    }

    public float getGeneratedCapacitySU()
    {
        return 0.0F;
    }

    public float getGeneratedSpeedRPM()
    {
        return 0.0F;
    }

    public float getStressImpactFactor()
    {
        return 0.0F;
    }

    public float getStressImpactSU(float absRpm)
    {
        return this.getStressImpactFactor() * absRpm;
    }

    public boolean hasShaftOnSide(EnumFacing side)
    {
        return side.getAxis() == this.axis;
    }

    public boolean hasSmallCogwheel()
    {
        return false;
    }

    public boolean hasLargeCogwheel()
    {
        return false;
    }

    public boolean isGearbox()
    {
        return false;
    }

    public boolean isGantryShaft()
    {
        return false;
    }

    public void applyNetworkState(long netId, float finalRpm, float capSU, float stressSU, boolean isOver, int count)
    {
        this.networkId = netId;
        this.speedRPM = finalRpm;
        this.networkCapacitySU = capSU;
        this.networkStressSU = stressSU;
        this.overstressed = isOver;
        this.networkNodeCount = count;
    }

    @Override
    public void update()
    {
        if (this.world != null)
        {
            if (this.needsNetworkRefresh)
            {
                this.needsNetworkRefresh = false;
                KineticNetwork.scheduleUpdate(this.world, this.pos);
            }
        }

        this.prevAngle = this.angle;

        if (!this.overstressed && Math.abs(this.speedRPM) > 0.01F)
        {
            // 1 RPM = 6 deg/sec = 0.3 deg/tick
            this.angle = (this.angle + this.speedRPM * 0.3F) % 360.0F;
        }
    }

    @Override
    public boolean onWrenchUse(EntityPlayer player, EnumFacing clickedSide)
    {
        if (player.isSneaking())
        {
            return false;
        }

        if (this.getGeneratedCapacitySU() > 0.0F)
        {
            this.directionMultiplier = -this.directionMultiplier;
        }
        else
        {
            EnumFacing.Axis[] axes = EnumFacing.Axis.values();
            int next = (this.axis.ordinal() + 1) % axes.length;
            this.axis = axes[next];
        }

        this.markDirty();
        KineticNetwork.scheduleUpdate(this.world, this.pos);

        if (this.world != null && this.world.isRemote)
        {
            player.sendMessage(new TextComponentString("\u00a7e[Create] \u00a7fAxis: \u00a7b" + this.axis.getName().toUpperCase() + " \u00a77| Speed: \u00a7a" + String.format("%.1f", this.speedRPM) + " RPM"));
        }

        return true;
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        if (this.overstressed)
        {
            lines.add("\u00a7c\u26a0 OVERSTRESSED (" + MathHelper.ceil(this.networkStressSU) + "/" + MathHelper.ceil(this.networkCapacitySU) + " SU)");
        }
        else
        {
            String rpmCol = Math.abs(this.speedRPM) > 0.01F ? "\u00a7a" : "\u00a77";
            lines.add(rpmCol + String.format("%.1f RPM", Math.abs(this.speedRPM)) + " \u00a77| Stress: \u00a7e" + MathHelper.ceil(this.networkStressSU) + "\u00a77/\u00a7b" + MathHelper.ceil(this.networkCapacitySU) + " SU");
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.speedRPM = compound.getFloat("CreateRPM");
        this.networkCapacitySU = compound.getFloat("CreateCapSU");
        this.networkStressSU = compound.getFloat("CreateStressSU");
        this.overstressed = compound.getBoolean("CreateOverstressed");
        this.directionMultiplier = compound.hasKey("CreateDirMul") ? compound.getByte("CreateDirMul") : 1;

        if (this.directionMultiplier == 0)
        {
            this.directionMultiplier = 1;
        }

        this.needsNetworkRefresh = true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setFloat("CreateRPM", this.speedRPM);
        compound.setFloat("CreateCapSU", this.networkCapacitySU);
        compound.setFloat("CreateStressSU", this.networkStressSU);
        compound.setBoolean("CreateOverstressed", this.overstressed);
        compound.setByte("CreateDirMul", (byte)this.directionMultiplier);
        return compound;
    }

    public abstract void queueFlywheelInstances(float partialTicks, float light);
}
