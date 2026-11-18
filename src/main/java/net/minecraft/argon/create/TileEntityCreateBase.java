package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

public abstract class TileEntityCreateBase extends TileEntity implements ITickable
{
    protected EnumFacing facing = EnumFacing.NORTH;
    protected EnumFacing.Axis axis = EnumFacing.Axis.Y;

    public EnumFacing getFacing()
    {
        return this.facing;
    }

    public void setFacing(EnumFacing facingIn)
    {
        this.facing = facingIn != null ? facingIn : EnumFacing.NORTH;
        this.markDirty();
    }

    public EnumFacing.Axis getAxis()
    {
        return this.axis;
    }

    public void setAxis(EnumFacing.Axis axisIn)
    {
        this.axis = axisIn != null ? axisIn : EnumFacing.Axis.Y;
        this.markDirty();
    }

    public boolean onWrenchUse(EntityPlayer player, EnumFacing clickedSide)
    {
        return false;
    }

    public void appendJadeInfo(List<String> lines)
    {
    }

    public abstract String getCreatePartId();

    @Override
    public void update()
    {
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        if (compound.hasKey("CreateFacing"))
        {
            this.facing = EnumFacing.getFront(compound.getByte("CreateFacing") & 7);
        }

        if (compound.hasKey("CreateAxis"))
        {
            int ax = compound.getByte("CreateAxis");
            this.axis = ax == 0 ? EnumFacing.Axis.X : (ax == 2 ? EnumFacing.Axis.Z : EnumFacing.Axis.Y);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("CreateFacing", (byte)this.facing.getIndex());
        int ax = this.axis == EnumFacing.Axis.X ? 0 : (this.axis == EnumFacing.Axis.Z ? 2 : 1);
        compound.setByte("CreateAxis", (byte)ax);
        return compound;
    }

    public void syncStateToBlock(IBlockState state)
    {
        if (this.world != null)
        {
            this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }
}
