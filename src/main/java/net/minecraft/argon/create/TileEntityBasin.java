package net.minecraft.argon.create;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;

public class TileEntityBasin extends TileEntityCreateBase
{
    public static final int SLOT_COUNT = 6;
    private final List<ItemStack> items = new ArrayList<ItemStack>(SLOT_COUNT);
    private CreateFluidType fluidType = CreateFluidType.EMPTY;
    private int fluidMb = 0;

    public TileEntityBasin()
    {
        for (int i = 0; i < SLOT_COUNT; ++i)
        {
            this.items.add(ItemStack.EMPTY);
        }
    }

    @Override
    public String getCreatePartId()
    {
        return "basin";
    }

    public List<ItemStack> getItems()
    {
        return this.items;
    }

    public CreateFluidType getFluidType()
    {
        return this.fluidType;
    }

    public int getFluidMb()
    {
        return this.fluidMb;
    }

    public boolean insertItem(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return false;
        }

        for (int i = 0; i < SLOT_COUNT; ++i)
        {
            ItemStack cur = this.items.get(i);

            if (!cur.isEmpty() && cur.getItem() == stack.getItem() && cur.getMetadata() == stack.getMetadata())
            {
                int add = Math.min(stack.getCount(), cur.getMaxStackSize() - cur.getCount());

                if (add > 0)
                {
                    cur.grow(add);
                    stack.shrink(add);
                    this.markDirty();
                    return true;
                }
            }
        }

        for (int i = 0; i < SLOT_COUNT; ++i)
        {
            if (this.items.get(i).isEmpty())
            {
                this.items.set(i, stack.splitStack(stack.getCount()));
                this.markDirty();
                return true;
            }
        }

        return false;
    }

    public ItemStack extractFirstItem()
    {
        for (int i = SLOT_COUNT - 1; i >= 0; --i)
        {
            ItemStack cur = this.items.get(i);

            if (!cur.isEmpty())
            {
                ItemStack out = cur.copy();
                this.items.set(i, ItemStack.EMPTY);
                this.markDirty();
                return out;
            }
        }

        return ItemStack.EMPTY;
    }

    public boolean onPlayerInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);

        if (!held.isEmpty() && held.getItem() == Items.WATER_BUCKET && this.fluidMb == 0)
        {
            this.fluidType = CreateFluidType.WATER;
            this.fluidMb = 1000;

            if (!player.capabilities.isCreativeMode)
            {
                player.setHeldItem(hand, new ItemStack(Items.BUCKET));
            }

            this.markDirty();
            return true;
        }

        if (!held.isEmpty())
        {
            ItemStack copy = held.copy();
            copy.setCount(1);

            if (this.insertItem(copy))
            {
                if (!player.capabilities.isCreativeMode)
                {
                    held.shrink(1);
                }

                return true;
            }
        }
        else
        {
            ItemStack extracted = this.extractFirstItem();

            if (!extracted.isEmpty())
            {
                if (!player.inventory.addItemStackToInventory(extracted))
                {
                    player.dropItem(extracted, false);
                }

                return true;
            }

            if (this.world != null && this.world.isRemote)
            {
                player.sendMessage(new TextComponentString("\u00a77[Basin] Empty"));
            }

            return true;
        }

        return false;
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        int totalItems = 0;

        for (int i = 0; i < SLOT_COUNT; ++i)
        {
            if (!this.items.get(i).isEmpty())
            {
                totalItems += this.items.get(i).getCount();
            }
        }

        lines.add("\u00a77Items: \u00a7f" + totalItems + (this.fluidMb > 0 ? " \u00a77| \u00a7b" + this.fluidType.getDisplayName() + " " + this.fluidMb + "mB" : ""));
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.fluidType = CreateFluidType.byId(compound.getString("FluidId"));
        this.fluidMb = compound.getInteger("FluidMb");
        NBTTagList list = compound.getTagList("BasinItems", 10);

        for (int i = 0; i < SLOT_COUNT; ++i)
        {
            this.items.set(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < list.tagCount(); ++i)
        {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 255;

            if (slot < SLOT_COUNT)
            {
                this.items.set(slot, new ItemStack(tag));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setString("FluidId", this.fluidType.id);
        compound.setInteger("FluidMb", this.fluidMb);
        NBTTagList list = new NBTTagList();

        for (int i = 0; i < SLOT_COUNT; ++i)
        {
            ItemStack s = this.items.get(i);

            if (!s.isEmpty())
            {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte("Slot", (byte)i);
                s.writeToNBT(tag);
                list.appendTag(tag);
            }
        }

        compound.setTag("BasinItems", list);
        return compound;
    }
}
