package net.minecraft.argon.create;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ItemWrench extends Item
{
    public ItemWrench()
    {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.TOOLS);
        this.setUnlocalizedName("createWrench");
    }

    @Override
    public String getItemStackDisplayName(net.minecraft.item.ItemStack stack)
    {
        return "Create Wrench";
    }
}
