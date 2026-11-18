package net.minecraft.argon.tnt;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Dedicated Creative Tab for Argon Lucky / Too Much / Unlucky TNT blocks.
 */
public final class TntCreativeTab extends CreativeTabs
{
    public static final CreativeTabs TAB_ARGON_TNT = new TntCreativeTab();

    private TntCreativeTab()
    {
        super(CreativeTabs.getNextID(), "argonTnt");
    }

    @Override
    public ItemStack getTabIconItem()
    {
        if (TntBlocks.LUCKY_TNT != null)
        {
            return new ItemStack(Item.getItemFromBlock(TntBlocks.LUCKY_TNT));
        }

        return new ItemStack(Item.getItemFromBlock(Blocks.TNT));
    }

    @Override
    public String getTranslatedTabLabel()
    {
        return "Argon TNT";
    }
}
