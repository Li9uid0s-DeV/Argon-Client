package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;

public class TileEntityGauge extends TileEntityKinetic
{
    public static final int KIND_SPEEDOMETER = 0;
    public static final int KIND_STRESSOMETER = 1;

    private int gaugeKind = KIND_SPEEDOMETER;

    public TileEntityGauge()
    {
    }

    public TileEntityGauge(int gaugeKind)
    {
        this.gaugeKind = gaugeKind;
    }

    public int getGaugeKind()
    {
        return this.gaugeKind;
    }

    @Override
    public String getCreatePartId()
    {
        return this.gaugeKind == KIND_STRESSOMETER ? "stressometer" : "speedometer";
    }

    public int getComparatorOutput()
    {
        if (this.gaugeKind == KIND_STRESSOMETER)
        {
            if (this.networkCapacitySU <= 0.001F)
            {
                return this.overstressed ? 15 : 0;
            }

            float ratio = MathHelper.clamp(this.networkStressSU / this.networkCapacitySU, 0.0F, 1.0F);
            return MathHelper.ceil(ratio * 15.0F);
        }

        float rpmRatio = MathHelper.clamp(Math.abs(this.speedRPM) / 256.0F, 0.0F, 1.0F);
        return MathHelper.ceil(rpmRatio * 15.0F);
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        super.appendJadeInfo(lines);

        if (this.gaugeKind == KIND_STRESSOMETER)
        {
            float pct = this.networkCapacitySU > 0.0F ? (this.networkStressSU / this.networkCapacitySU) * 100.0F : 0.0F;
            lines.add("\u00a76Network Load: \u00a7f" + String.format("%.0f%%", pct) + " \u00a77(" + MathHelper.ceil(this.networkStressSU) + "/" + MathHelper.ceil(this.networkCapacitySU) + " SU)");
        }
        else
        {
            lines.add("\u00a7bSpeedometer: \u00a7f" + String.format("%.1f RPM", Math.abs(this.speedRPM)) + (this.speedRPM < 0.0F ? " \u00a77(CCW)" : " \u00a77(CW)"));
        }
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        float ang = this.getInterpolatedAngle(partialTicks);
        float tr = this.gaugeKind == KIND_STRESSOMETER ? 0.96F : 0.80F;
        float tg = this.gaugeKind == KIND_STRESSOMETER ? 0.82F : 0.90F;
        float tb = this.gaugeKind == KIND_STRESSOMETER ? 0.65F : 1.0F;

        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.GAUGE_CASING, (float)this.pos.getX() + 0.5F, (float)this.pos.getY() + 0.5F, (float)this.pos.getZ() + 0.5F, this.axis, 0.0F, tr, tg, tb, light);

        float needleAngle = this.gaugeKind == KIND_STRESSOMETER
            ? MathHelper.clamp(this.networkCapacitySU > 0.0F ? (this.networkStressSU / this.networkCapacitySU) * 240.0F : 0.0F, 0.0F, 240.0F) - 120.0F
            : MathHelper.clamp(Math.abs(this.speedRPM) / 256.0F * 240.0F, 0.0F, 240.0F) - 120.0F;
        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.GAUGE_NEEDLE, (float)this.pos.getX() + 0.5F, (float)this.pos.getY() + 0.5F, (float)this.pos.getZ() + 0.5F, this.axis, needleAngle, 1.0F, 1.0F, 1.0F, light);
        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.SHAFT, this.pos, this.axis, ang, light);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.gaugeKind = compound.getByte("GaugeKind");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("GaugeKind", (byte)this.gaugeKind);
        return compound;
    }
}
