package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class CreateRecipes
{
    private CreateRecipes()
    {
    }

    public static ItemStack getPressingResult(ItemStack input)
    {
        if (input.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        Item item = input.getItem();

        if (item == Items.IRON_INGOT)
        {
            return new ItemStack(CreateItems.IRON_SHEET, 1);
        }

        if (item == Items.GOLD_INGOT)
        {
            return new ItemStack(CreateItems.GOLD_SHEET, 1);
        }

        if (item == CreateItems.BRASS_INGOT)
        {
            return new ItemStack(CreateItems.BRASS_SHEET, 1);
        }

        if (item == Items.WHEAT)
        {
            return new ItemStack(Items.BREAD, 1);
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getCrushingResult(ItemStack input)
    {
        if (input.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        Item item = input.getItem();

        if (item == Item.getItemFromBlock(Blocks.IRON_ORE))
        {
            return new ItemStack(CreateItems.CRUSHED_IRON_ORE, 2);
        }

        if (item == Item.getItemFromBlock(Blocks.GOLD_ORE))
        {
            return new ItemStack(CreateItems.CRUSHED_GOLD_ORE, 2);
        }

        if (item == Item.getItemFromBlock(Blocks.COAL_ORE))
        {
            return new ItemStack(Items.COAL, 2);
        }

        if (item == Item.getItemFromBlock(Blocks.DIAMOND_ORE))
        {
            return new ItemStack(Items.DIAMOND, 2);
        }

        if (item == Item.getItemFromBlock(Blocks.EMERALD_ORE))
        {
            return new ItemStack(Items.EMERALD, 2);
        }

        if (item == Item.getItemFromBlock(Blocks.COBBLESTONE))
        {
            return new ItemStack(Blocks.GRAVEL, 1);
        }

        if (item == Item.getItemFromBlock(Blocks.GRAVEL))
        {
            return new ItemStack(Blocks.SAND, 1);
        }

        if (item == Items.BLAZE_ROD)
        {
            return new ItemStack(Items.BLAZE_POWDER, 3);
        }

        if (item == Items.BONE)
        {
            return new ItemStack(Items.DYE, 4, 15);
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getFanResult(ItemStack input, int catalystType)
    {
        // catalystType: 1=WASHING (Water), 2=SMOKING (Fire), 3=BLASTING (Lava)
        if (input.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        Item item = input.getItem();

        if (catalystType == 1)
        {
            if (item == CreateItems.CRUSHED_IRON_ORE)
            {
                return new ItemStack(Items.IRON_NUGGET, 12);
            }

            if (item == CreateItems.CRUSHED_GOLD_ORE)
            {
                return new ItemStack(Items.GOLD_NUGGET, 12);
            }

            if (item == Item.getItemFromBlock(Blocks.GRAVEL))
            {
                return new ItemStack(Items.FLINT, 1);
            }

            if (item == Item.getItemFromBlock(Blocks.SAND))
            {
                return new ItemStack(Items.CLAY_BALL, 1);
            }
        }
        else if (catalystType == 2)
        {
            if (item == Items.BEEF) return new ItemStack(Items.COOKED_BEEF, 1);
            if (item == Items.PORKCHOP) return new ItemStack(Items.COOKED_PORKCHOP, 1);
            if (item == Items.CHICKEN) return new ItemStack(Items.COOKED_CHICKEN, 1);
            if (item == Items.MUTTON) return new ItemStack(Items.COOKED_MUTTON, 1);
            if (item == Items.RABBIT) return new ItemStack(Items.COOKED_RABBIT, 1);
            if (item == Items.POTATO) return new ItemStack(Items.BAKED_POTATO, 1);
            if (item == Items.FISH) return new ItemStack(Items.COOKED_FISH, 1, input.getMetadata());
        }
        else if (catalystType == 3)
        {
            if (item == CreateItems.CRUSHED_IRON_ORE || item == Item.getItemFromBlock(Blocks.IRON_ORE))
            {
                return new ItemStack(Items.IRON_INGOT, 1);
            }

            if (item == CreateItems.CRUSHED_GOLD_ORE || item == Item.getItemFromBlock(Blocks.GOLD_ORE))
            {
                return new ItemStack(Items.GOLD_INGOT, 1);
            }

            if (item == Item.getItemFromBlock(Blocks.SAND))
            {
                return new ItemStack(Blocks.GLASS, 1);
            }

            if (item == Item.getItemFromBlock(Blocks.COBBLESTONE))
            {
                return new ItemStack(Blocks.STONE, 1);
            }
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack tryMixBasin(List<ItemStack> basinSlots, boolean heated, CreateFluidType fluid, int fluidMb, boolean consume)
    {
        int goldIdx = -1;
        int ironIdx = -1;
        int redstoneIdx = -1;
        int coalIdx = -1;
        int wheatIdx = -1;
        int cocoaIdx = -1;

        for (int i = 0; i < basinSlots.size(); ++i)
        {
            ItemStack st = basinSlots.get(i);

            if (st.isEmpty())
            {
                continue;
            }

            if (st.getItem() == Items.GOLD_INGOT && goldIdx < 0) goldIdx = i;
            else if (st.getItem() == Items.IRON_INGOT && ironIdx < 0) ironIdx = i;
            else if (st.getItem() == Items.REDSTONE && redstoneIdx < 0) redstoneIdx = i;
            else if (st.getItem() == Items.COAL && coalIdx < 0) coalIdx = i;
            else if (st.getItem() == Items.WHEAT && wheatIdx < 0) wheatIdx = i;
            else if (st.getItem() == Items.DYE && st.getMetadata() == 3 && cocoaIdx < 0) cocoaIdx = i;
        }

        // Heated Brass Alloying: Gold Ingot + Iron Ingot (or Redstone) -> 2 Brass Ingot
        if (heated && goldIdx >= 0 && (ironIdx >= 0 || redstoneIdx >= 0))
        {
            if (consume)
            {
                basinSlots.get(goldIdx).shrink(1);
                basinSlots.get(ironIdx >= 0 ? ironIdx : redstoneIdx).shrink(1);
            }

            return new ItemStack(CreateItems.BRASS_INGOT, 2);
        }

        // Shapeless Cookie Dough: Wheat + Cocoa Beans -> 8 Cookies
        if (wheatIdx >= 0 && cocoaIdx >= 0)
        {
            if (consume)
            {
                basinSlots.get(wheatIdx).shrink(1);
                basinSlots.get(cocoaIdx).shrink(1);
            }

            return new ItemStack(Items.COOKIE, 8);
        }

        // Andesite Alloy / Steel Grit: Iron + Coal -> 2 Iron Ingot bonus or Coal Block
        if (heated && ironIdx >= 0 && coalIdx >= 0 && basinSlots.get(coalIdx).getCount() >= 2)
        {
            if (consume)
            {
                basinSlots.get(ironIdx).shrink(1);
                basinSlots.get(coalIdx).shrink(2);
            }

            return new ItemStack(Items.IRON_INGOT, 2);
        }

        return ItemStack.EMPTY;
    }
}
