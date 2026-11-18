package net.minecraft.argon.tnt;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.Item;

/**
 * Registers fallback block state models, particle sprites, and inventory icon models
 * for all 17 Argon TNT blocks so zero missing textures appear in-world or in the creative tab.
 */
public final class TntClientHooks
{
    private TntClientHooks()
    {
    }

    public static void registerBlockModels(BlockModelShapes shapes)
    {
        shapes.registerBuiltInBlocks(TntBlocks.ALL_BLOCKS);

        for (int i = 0; i < TntBlocks.ALL_BLOCKS.length; ++i)
        {
            final BlockArgonTNT block = TntBlocks.ALL_BLOCKS[i];
            final String modelName = getVanillaModelName(block.getTntType());

            shapes.registerBlockWithStateMapper(block, new StateMapperBase()
            {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state)
                {
                    return new ModelResourceLocation(modelName, "normal");
                }
            });
        }
    }

    public static TextureAtlasSprite getFallbackTexture(BlockModelShapes shapes, Block block)
    {
        if (block instanceof BlockArgonTNT)
        {
            TntType type = ((BlockArgonTNT)block).getTntType();

            if (type.category == TntType.Category.LUCKY)
            {
                return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/gold_block");
            }
            if (type.category == TntType.Category.UNLUCKY)
            {
                return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/obsidian");
            }

            return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/tnt_side");
        }

        return null;
    }

    public static void registerItemModels(RenderItem renderItem)
    {
        ItemModelMesher mesher = renderItem.getItemModelMesher();

        for (int i = 0; i < TntBlocks.ALL_BLOCKS.length; ++i)
        {
            BlockArgonTNT block = TntBlocks.ALL_BLOCKS[i];
            Item item = Item.getItemFromBlock(block);

            if (item != null)
            {
                String modelName = getVanillaModelName(block.getTntType());
                mesher.register(item, 0, new ModelResourceLocation(modelName, "inventory"));
            }
        }
    }

    private static String getVanillaModelName(TntType type)
    {
        switch (type)
        {
            case LUCKY_TNT:
                return "gold_block";
            case SUPER_LUCKY_TNT:
                return "emerald_block";
            case MINERAL_TNT:
                return "diamond_ore";
            case HOUSE_TNT:
                return "bookshelf";
            case NUKE_TNT:
                return "slime";
            case METEOR_TNT:
                return "magma";
            case OCEAN_TNT:
                return "lapis_block";
            case FLAT_TNT:
                return "iron_block";
            case MINING_FLAT_TNT:
                return "cobblestone";
            case UNLUCKY_TNT:
                return "obsidian";
            case CURSED_NUKE_TNT:
                return "purpur_block";
            case LAVA_TRAP_TNT:
                return "magma";
            case MOB_SWARM_TNT:
                return "netherrack";
            case TNT_X5:
            case TNT_X20:
            case TNT_X100:
            case CLUSTER_TNT:
            default:
                return "tnt";
        }
    }
}
