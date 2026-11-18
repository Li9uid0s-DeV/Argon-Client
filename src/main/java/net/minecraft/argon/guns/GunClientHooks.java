package net.minecraft.argon.guns;

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
 * Registers fallback block states, particle sprites, and inventory icon models
 * for Argon Gun Mod items and blocks so no missing textures appear.
 */
public final class GunClientHooks
{
    private GunClientHooks()
    {
    }

    public static void registerBlockModels(BlockModelShapes shapes)
    {
        shapes.registerBuiltInBlocks(GunBlocks.ALL_BLOCKS);

        StateMapperBase ironMapper = new StateMapperBase()
        {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state)
            {
                return new ModelResourceLocation("iron_block", "normal");
            }
        };

        shapes.registerBlockWithStateMapper(GunBlocks.GUN_WORKBENCH, ironMapper);
        shapes.registerBlockWithStateMapper(GunBlocks.AUTO_TURRET, ironMapper);
    }

    public static TextureAtlasSprite getFallbackTexture(BlockModelShapes shapes, Block block)
    {
        if (block == GunBlocks.GUN_WORKBENCH || block == GunBlocks.AUTO_TURRET)
        {
            return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/iron_block");
        }

        return null;
    }

    public static void registerItemModels(RenderItem renderItem)
    {
        ItemModelMesher mesher = renderItem.getItemModelMesher();

        registerBlockModel(mesher, GunBlocks.GUN_WORKBENCH, "anvil");
        registerBlockModel(mesher, GunBlocks.AUTO_TURRET, "dispenser");

        // Firearms use procedural 3D models in-hand and clean metallic icons in inventory slots
        registerItemModel(mesher, GunItems.PISTOL_9MM, "iron_hoe");
        registerItemModel(mesher, GunItems.ASSAULT_RIFLE, "iron_pickaxe");
        registerItemModel(mesher, GunItems.SHOTGUN_12G, "stone_hoe");
        registerItemModel(mesher, GunItems.SNIPER_50, "diamond_hoe");
        registerItemModel(mesher, GunItems.MINIGUN, "iron_axe");
        registerItemModel(mesher, GunItems.ROCKET_LAUNCHER, "blaze_rod");
        registerItemModel(mesher, GunItems.PLASMA_RIFLE, "diamond_pickaxe");

        // Ammunition icons
        registerItemModel(mesher, GunItems.AMMO_9MM, "iron_nugget");
        registerItemModel(mesher, GunItems.AMMO_556, "gold_nugget");
        registerItemModel(mesher, GunItems.AMMO_12G, "redstone");
        registerItemModel(mesher, GunItems.AMMO_50CAL, "arrow");
        registerItemModel(mesher, GunItems.AMMO_ROCKET, "fireworks");
        registerItemModel(mesher, GunItems.AMMO_PLASMA_CELL, "prismarine_crystals");

        // Attachment module icons
        registerItemModel(mesher, GunItems.ATTACH_SCOPE_REFLEX, "ender_eye");
        registerItemModel(mesher, GunItems.ATTACH_SCOPE_4X, "compass");
        registerItemModel(mesher, GunItems.ATTACH_SCOPE_8X, "clock");
        registerItemModel(mesher, GunItems.ATTACH_SILENCER, "iron_ingot");
        registerItemModel(mesher, GunItems.ATTACH_COMPENSATOR, "flint");
        registerItemModel(mesher, GunItems.ATTACH_EXTENDED_MAG, "hopper");
        registerItemModel(mesher, GunItems.ATTACH_FOREGRIP, "lever");
        registerItemModel(mesher, GunItems.ATTACH_LASER, "redstone_torch");
    }

    private static void registerBlockModel(ItemModelMesher mesher, Block block, String modelName)
    {
        Item item = Item.getItemFromBlock(block);

        if (item != null)
        {
            mesher.register(item, 0, new ModelResourceLocation(modelName, "inventory"));
        }
    }

    private static void registerItemModel(ItemModelMesher mesher, Item item, String modelName)
    {
        mesher.register(item, 0, new ModelResourceLocation(modelName, "inventory"));
    }
}
