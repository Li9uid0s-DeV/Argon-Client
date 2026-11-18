package net.minecraft.argon.create;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class CreateItems
{
    public static final Item WRENCH = new ItemWrench();
    public static final Item SUPER_GLUE = new ItemSuperGlue();
    public static final Item TRAIN_SCHEDULE = new ItemTrainSchedule();
    public static final Item IRON_SHEET = makeSimpleMaterial("createIronSheet", "Iron Sheet (Create)");
    public static final Item GOLD_SHEET = makeSimpleMaterial("createGoldSheet", "Golden Sheet (Create)");
    public static final Item BRASS_INGOT = makeSimpleMaterial("createBrassIngot", "Brass Ingot (Create)");
    public static final Item BRASS_SHEET = makeSimpleMaterial("createBrassSheet", "Brass Sheet (Create)");
    public static final Item CRUSHED_IRON_ORE = makeSimpleMaterial("createCrushedIronOre", "Crushed Raw Iron (Create)");
    public static final Item CRUSHED_GOLD_ORE = makeSimpleMaterial("createCrushedGoldOre", "Crushed Raw Gold (Create)");

    private static boolean registered = false;

    private CreateItems()
    {
    }

    private static Item makeSimpleMaterial(final String unloc, final String displayName)
    {
        return new Item()
        {
            {
                this.setUnlocalizedName(unloc);
                this.setCreativeTab(CreativeTabs.MATERIALS);
            }

            @Override
            public String getItemStackDisplayName(ItemStack stack)
            {
                return displayName;
            }
        };
    }

    public static void registerItemsAndBlocks()
    {
        if (registered)
        {
            return;
        }

        registered = true;

        for (int i = 0; i < CreateBlocks.ALL_BLOCKS.length; ++i)
        {
            Block blk = CreateBlocks.ALL_BLOCKS[i];
            Item.registerItemBlock(blk);
        }

        Item.registerItem(550, "create_wrench", WRENCH);
        Item.registerItem(551, "create_super_glue", SUPER_GLUE);
        Item.registerItem(552, "create_train_schedule", TRAIN_SCHEDULE);
        Item.registerItem(553, "create_iron_sheet", IRON_SHEET);
        Item.registerItem(554, "create_gold_sheet", GOLD_SHEET);
        Item.registerItem(555, "create_brass_ingot", BRASS_INGOT);
        Item.registerItem(556, "create_brass_sheet", BRASS_SHEET);
        Item.registerItem(557, "create_crushed_iron_ore", CRUSHED_IRON_ORE);
        Item.registerItem(558, "create_crushed_gold_ore", CRUSHED_GOLD_ORE);
    }
}
