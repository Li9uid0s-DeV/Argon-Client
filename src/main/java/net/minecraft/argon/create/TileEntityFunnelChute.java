package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityFunnelChute extends TileEntityCreateBase
{
    public static final int KIND_FUNNEL = 0;
    public static final int KIND_CHUTE = 1;

    private int kind = KIND_FUNNEL;
    private int cooldown = 0;

    public TileEntityFunnelChute()
    {
    }

    public TileEntityFunnelChute(int kind)
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
        return this.kind == KIND_CHUTE ? "chute" : "funnel";
    }

    @Override
    public void update()
    {
        if (this.world == null || this.world.isRemote)
        {
            return;
        }

        if (++this.cooldown < 8)
        {
            return;
        }

        this.cooldown = 0;

        if (this.kind == KIND_CHUTE)
        {
            this.tickChute();
        }
        else
        {
            this.tickFunnel();
        }
    }

    private void tickChute()
    {
        BlockPos abovePos = this.pos.up();
        BlockPos belowPos = this.pos.down();

        // Suck dropped items directly above chute and drop below or into chest/belt/basin
        List<EntityItem> drops = this.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(
            abovePos.getX(), abovePos.getY(), abovePos.getZ(),
            abovePos.getX() + 1.0D, abovePos.getY() + 1.0D, abovePos.getZ() + 1.0D));

        if (!drops.isEmpty())
        {
            EntityItem ei = drops.get(0);
            ItemStack st = ei.getItem();

            if (!st.isEmpty() && this.tryDeliverDown(belowPos, st))
            {
                if (st.isEmpty())
                {
                    ei.setDead();
                }

                return;
            }
        }

        TileEntity aboveTe = this.world.getTileEntity(abovePos);

        if (aboveTe instanceof IInventory)
        {
            IInventory inv = (IInventory)aboveTe;

            for (int i = 0; i < inv.getSizeInventory(); ++i)
            {
                ItemStack s = inv.getStackInSlot(i);

                if (!s.isEmpty())
                {
                    ItemStack one = s.copy();
                    one.setCount(1);

                    if (this.tryDeliverDown(belowPos, one))
                    {
                        inv.decrStackSize(i, 1);
                        inv.markDirty();
                        break;
                    }
                }
            }
        }
    }

    private boolean tryDeliverDown(BlockPos belowPos, ItemStack stack)
    {
        TileEntity belowTe = this.world.getTileEntity(belowPos);

        if (belowTe instanceof TileEntityBelt)
        {
            ItemStack one = stack.copy();
            one.setCount(1);

            if (((TileEntityBelt)belowTe).tryInsertItem(one))
            {
                stack.shrink(1);
                return true;
            }

            return false;
        }
        else if (belowTe instanceof TileEntityBasin)
        {
            ItemStack one = stack.copy();
            one.setCount(1);

            if (((TileEntityBasin)belowTe).insertItem(one))
            {
                stack.shrink(1);
                return true;
            }

            return false;
        }
        else if (belowTe instanceof IInventory)
        {
            ItemStack one = stack.copy();
            one.setCount(1);

            if (TileEntityBelt.insertIntoInventory((IInventory)belowTe, one))
            {
                stack.shrink(1);
                return true;
            }

            return false;
        }
        else if (this.world.isAirBlock(belowPos))
        {
            ItemStack one = stack.splitStack(1);
            EntityItem drop = new EntityItem(this.world, (double)belowPos.getX() + 0.5D, (double)belowPos.getY() + 0.6D, (double)belowPos.getZ() + 0.5D, one);
            drop.motionX = 0.0D;
            drop.motionY = -0.1D;
            drop.motionZ = 0.0D;
            this.world.spawnEntity(drop);
            return true;
        }

        return false;
    }

    private void tickFunnel()
    {
        // Funnel sits attached against facing.getOpposite() inventory/belt and outputs/inputs along facing
        BlockPos attachedPos = this.pos.offset(this.facing.getOpposite());
        TileEntity attachedTe = this.world.getTileEntity(attachedPos);
        BlockPos frontPos = this.pos.offset(this.facing);

        if (attachedTe instanceof IInventory)
        {
            // Extract 1 item out onto front belt or world
            IInventory inv = (IInventory)attachedTe;

            for (int i = 0; i < inv.getSizeInventory(); ++i)
            {
                ItemStack s = inv.getStackInSlot(i);

                if (!s.isEmpty())
                {
                    ItemStack one = s.copy();
                    one.setCount(1);
                    TileEntity frontTe = this.world.getTileEntity(frontPos);

                    if (frontTe instanceof TileEntityBelt && ((TileEntityBelt)frontTe).tryInsertItem(one))
                    {
                        inv.decrStackSize(i, 1);
                        inv.markDirty();
                        return;
                    }
                    else if (this.world.isAirBlock(this.pos.down()))
                    {
                        TileEntity belowTe = this.world.getTileEntity(this.pos.down());

                        if (belowTe instanceof TileEntityBelt && ((TileEntityBelt)belowTe).tryInsertItem(one))
                        {
                            inv.decrStackSize(i, 1);
                            inv.markDirty();
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.kind = compound.getByte("FunnelChuteKind");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("FunnelChuteKind", (byte)this.kind);
        return compound;
    }
}
