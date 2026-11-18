package net.minecraft.argon.tnt;

import net.minecraft.item.Item;

/**
 * Registers custom ItemBlockArgonTnt items for every Argon TNT block
 * so each TNT displays its localized name, category, radius, and scheduler tooltip
 * inside the dedicated Argon TNT creative tab.
 */
public final class TntItems
{
    private TntItems()
    {
    }

    public static void registerItemsAndBlocks()
    {
        for (int i = 0; i < TntBlocks.ALL_BLOCKS.length; ++i)
        {
            BlockArgonTNT block = TntBlocks.ALL_BLOCKS[i];
            Item.registerItemBlock(block, new ItemBlockArgonTnt(block));
        }
    }
}
