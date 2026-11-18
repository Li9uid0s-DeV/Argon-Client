package net.minecraft.argon.create;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public final class CreateClientHooks
{
    private CreateClientHooks()
    {
    }

    public static void registerBlockModels(BlockModelShapes shapes)
    {
        shapes.registerBuiltInBlocks(CreateBlocks.ALL_BLOCKS);

        StateMapperBase oakMapper = new StateMapperBase()
        {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state)
            {
                return new ModelResourceLocation("spruce_planks", "normal");
            }
        };
        StateMapperBase stoneMapper = new StateMapperBase()
        {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state)
            {
                return new ModelResourceLocation("smooth_andesite", "normal");
            }
        };
        StateMapperBase woolMapper = new StateMapperBase()
        {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state)
            {
                return new ModelResourceLocation("white_wool", "normal");
            }
        };

        shapes.registerBlockWithStateMapper(CreateBlocks.SAIL, woolMapper);
        shapes.registerBlockWithStateMapper(CreateBlocks.PISTON_POLE, oakMapper);
        shapes.registerBlockWithStateMapper(CreateBlocks.GEARBOX, oakMapper);
        shapes.registerBlockWithStateMapper(CreateBlocks.WATER_WHEEL, oakMapper);

        for (int i = 0; i < CreateBlocks.ALL_BLOCKS.length; ++i)
        {
            Block b = CreateBlocks.ALL_BLOCKS[i];

            if (b != CreateBlocks.SAIL && b != CreateBlocks.PISTON_POLE && b != CreateBlocks.GEARBOX && b != CreateBlocks.WATER_WHEEL)
            {
                shapes.registerBlockWithStateMapper(b, stoneMapper);
            }
        }
    }

    public static TextureAtlasSprite getFallbackTexture(BlockModelShapes shapes, Block block)
    {
        if (block == CreateBlocks.SAIL)
        {
            return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/wool_colored_white");
        }

        if (block == CreateBlocks.WATER_WHEEL || block == CreateBlocks.GEARBOX || block == CreateBlocks.PISTON_POLE)
        {
            return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/planks_spruce");
        }

        if (block == CreateBlocks.FLUID_PIPE || block == CreateBlocks.FLUID_TANK || block == CreateBlocks.STEAM_ENGINE || block == CreateBlocks.TRAIN_STATION || block == CreateBlocks.TRAIN_CONTROLS)
        {
            return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/gold_block");
        }

        if (block == CreateBlocks.BELT || block == CreateBlocks.FUNNEL || block == CreateBlocks.CHUTE || block == CreateBlocks.BASIN)
        {
            return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/cauldron_side");
        }

        if (block == CreateBlocks.TRAIN_TRACK || block == CreateBlocks.TRAIN_BOGEY || block == CreateBlocks.GANTRY_SHAFT)
        {
            return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/iron_block");
        }

        if (block == CreateBlocks.BLAZE_BURNER)
        {
            return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/furnace_front_on");
        }

        for (int i = 0; i < CreateBlocks.ALL_BLOCKS.length; ++i)
        {
            if (CreateBlocks.ALL_BLOCKS[i] == block)
            {
                return shapes.getModelManager().getTextureMap().getAtlasSprite("minecraft:blocks/stone_andesite_smooth");
            }
        }

        return null;
    }

    public static void registerItemModels(RenderItem renderItem)
    {
        ItemModelMesher mesher = renderItem.getItemModelMesher();

        registerBlockModel(mesher, CreateBlocks.SHAFT, "stone_andesite_smooth");
        registerBlockModel(mesher, CreateBlocks.COGWHEEL_SMALL, "spruce_planks");
        registerBlockModel(mesher, CreateBlocks.COGWHEEL_LARGE, "oak_planks");
        registerBlockModel(mesher, CreateBlocks.GEARBOX, "spruce_planks");
        registerBlockModel(mesher, CreateBlocks.WATER_WHEEL, "oak_planks");
        registerBlockModel(mesher, CreateBlocks.WINDMILL_BEARING, "piston");
        registerBlockModel(mesher, CreateBlocks.SAIL, "white_wool");
        registerBlockModel(mesher, CreateBlocks.SPEEDOMETER, "compass");
        registerBlockModel(mesher, CreateBlocks.STRESSOMETER, "clock");
        registerBlockModel(mesher, CreateBlocks.BELT, "obsidian");
        registerBlockModel(mesher, CreateBlocks.FUNNEL, "hopper");
        registerBlockModel(mesher, CreateBlocks.CHUTE, "cauldron");
        registerBlockModel(mesher, CreateBlocks.BASIN, "cauldron");
        registerBlockModel(mesher, CreateBlocks.MECHANICAL_PRESS, "anvil");
        registerBlockModel(mesher, CreateBlocks.MECHANICAL_MIXER, "iron_block");
        registerBlockModel(mesher, CreateBlocks.CRUSHING_WHEEL, "cobblestone");
        registerBlockModel(mesher, CreateBlocks.ENCASED_FAN, "dispenser");
        registerBlockModel(mesher, CreateBlocks.FLUID_PIPE, "gold_block");
        registerBlockModel(mesher, CreateBlocks.MECHANICAL_PUMP, "gold_block");
        registerBlockModel(mesher, CreateBlocks.FLUID_TANK, "glass");
        registerBlockModel(mesher, CreateBlocks.BLAZE_BURNER, "furnace");
        registerBlockModel(mesher, CreateBlocks.STEAM_ENGINE, "gold_block");
        registerBlockModel(mesher, CreateBlocks.MECHANICAL_PISTON, "sticky_piston");
        registerBlockModel(mesher, CreateBlocks.PISTON_POLE, "spruce_fence");
        registerBlockModel(mesher, CreateBlocks.GANTRY_SHAFT, "iron_bars");
        registerBlockModel(mesher, CreateBlocks.GANTRY_CARRIAGE, "piston");
        registerBlockModel(mesher, CreateBlocks.TRAIN_TRACK, "rail");
        registerBlockModel(mesher, CreateBlocks.TRAIN_BOGEY, "minecart");
        registerBlockModel(mesher, CreateBlocks.TRAIN_STATION, "beacon");
        registerBlockModel(mesher, CreateBlocks.TRAIN_CONTROLS, "lever");

        registerItemModel(mesher, CreateItems.WRENCH, "gold_pickaxe");
        registerItemModel(mesher, CreateItems.SUPER_GLUE, "slime_ball");
        registerItemModel(mesher, CreateItems.TRAIN_SCHEDULE, "writable_book");
        registerItemModel(mesher, CreateItems.IRON_SHEET, "iron_ingot");
        registerItemModel(mesher, CreateItems.GOLD_SHEET, "gold_ingot");
        registerItemModel(mesher, CreateItems.BRASS_INGOT, "gold_ingot");
        registerItemModel(mesher, CreateItems.BRASS_SHEET, "gold_nugget");
        registerItemModel(mesher, CreateItems.CRUSHED_IRON_ORE, "gunpowder");
        registerItemModel(mesher, CreateItems.CRUSHED_GOLD_ORE, "glowstone_dust");
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

    public static void renderSelectionOverlays(EntityPlayer player, float partialTicks)
    {
        if (player == null)
        {
            return;
        }

        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTicks;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTicks;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTicks;

        ItemStack held = player.getHeldItemMainhand();

        if (!held.isEmpty() && held.getItem() == CreateItems.SUPER_GLUE)
        {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            GlStateManager.glLineWidth(2.5F);

            BlockPos anchor = SuperGlueManager.getPendingCorner(player);

            if (anchor != null)
            {
                AxisAlignedBB box = new AxisAlignedBB(anchor).grow(0.03D).offset(-camX, -camY, -camZ);
                RenderGlobal.drawSelectionBoundingBox(box, 0.2F, 1.0F, 0.35F, 0.9F);
            }

            if (player.world != null)
            {
                java.util.List<SuperGlueManager.GlueBox> boxes = SuperGlueManager.getBoxesForWorld(player.world);

                for (int i = 0; i < boxes.size(); ++i)
                {
                    SuperGlueManager.GlueBox gb = boxes.get(i);

                    if (player.getDistanceSq((double)gb.min.getX(), (double)gb.min.getY(), (double)gb.min.getZ()) < 48.0D * 48.0D)
                    {
                        AxisAlignedBB box = new AxisAlignedBB(
                            (double)gb.min.getX(), (double)gb.min.getY(), (double)gb.min.getZ(),
                            (double)gb.max.getX() + 1.0D, (double)gb.max.getY() + 1.0D, (double)gb.max.getZ() + 1.0D);
                        RenderGlobal.drawSelectionBoundingBox(box.grow(0.01D).offset(-camX, -camY, -camZ), 0.15F, 0.95F, 0.30F, 0.65F);
                    }
                }
            }

            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }

        BlockPos trackStart = BlockTrainTrack.getPendingCurveStart(player);

        if (trackStart != null)
        {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            GlStateManager.glLineWidth(2.5F);
            AxisAlignedBB box = new AxisAlignedBB(trackStart).grow(0.05D).offset(-camX, -camY, -camZ);
            RenderGlobal.drawSelectionBoundingBox(box, 0.25F, 0.70F, 1.0F, 0.9F);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }
}
