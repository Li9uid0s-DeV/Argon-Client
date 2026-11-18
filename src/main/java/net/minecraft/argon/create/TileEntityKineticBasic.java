package net.minecraft.argon.create;

import net.minecraft.nbt.NBTTagCompound;

public class TileEntityKineticBasic extends TileEntityKinetic
{
    public static final int TYPE_SHAFT = 0;
    public static final int TYPE_COGWHEEL_SMALL = 1;
    public static final int TYPE_COGWHEEL_LARGE = 2;
    public static final int TYPE_GEARBOX = 3;
    public static final int TYPE_GANTRY_SHAFT = 4;

    private int kind = TYPE_SHAFT;

    public TileEntityKineticBasic()
    {
    }

    public TileEntityKineticBasic(int kind)
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
        switch (this.kind)
        {
            case TYPE_COGWHEEL_SMALL:
                return "cogwheel";
            case TYPE_COGWHEEL_LARGE:
                return "large_cogwheel";
            case TYPE_GEARBOX:
                return "gearbox";
            case TYPE_GANTRY_SHAFT:
                return "gantry_shaft";
            default:
                return "shaft";
        }
    }

    @Override
    public boolean hasSmallCogwheel()
    {
        return this.kind == TYPE_COGWHEEL_SMALL;
    }

    @Override
    public boolean hasLargeCogwheel()
    {
        return this.kind == TYPE_COGWHEEL_LARGE;
    }

    @Override
    public boolean isGearbox()
    {
        return this.kind == TYPE_GEARBOX;
    }

    @Override
    public boolean isGantryShaft()
    {
        return this.kind == TYPE_GANTRY_SHAFT;
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        float ang = this.getInterpolatedAngle(partialTicks);

        switch (this.kind)
        {
            case TYPE_COGWHEEL_SMALL:
                FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.COGWHEEL_SMALL, this.pos, this.axis, ang, light);
                break;

            case TYPE_COGWHEEL_LARGE:
                FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.COGWHEEL_LARGE, this.pos, this.axis, ang, light);
                break;

            case TYPE_GEARBOX:
                FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.GEARBOX_CASING, this.pos, this.axis, 0.0F, light);
                FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.SHAFT, this.pos, this.axis, ang, light);
                break;

            case TYPE_GANTRY_SHAFT:
                FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.GANTRY_SHAFT, this.pos, this.axis, ang, light);
                break;

            case TYPE_SHAFT:
            default:
                FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.SHAFT, this.pos, this.axis, ang, light);
                break;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.kind = compound.getByte("CreateBasicKind");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("CreateBasicKind", (byte)this.kind);
        return compound;
    }
}
