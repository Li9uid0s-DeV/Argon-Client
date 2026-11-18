package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityTrainDecor extends TileEntityCreateBase
{
    public static final int KIND_BOGEY = 0;
    public static final int KIND_CONTROLS = 1;

    private int kind = KIND_BOGEY;

    public TileEntityTrainDecor()
    {
    }

    public TileEntityTrainDecor(int kind)
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
        return this.kind == KIND_CONTROLS ? "train_controls" : "train_bogey";
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        if (this.kind == KIND_CONTROLS)
        {
            lines.add("\u00a77Driver Console (W/S throttle when assembled)");
        }
        else
        {
            lines.add("\u00a77Rail Carriage Wheel Assembly");
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.kind = compound.getInteger("TrainDecorKind");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setInteger("TrainDecorKind", this.kind);
        return compound;
    }
}
