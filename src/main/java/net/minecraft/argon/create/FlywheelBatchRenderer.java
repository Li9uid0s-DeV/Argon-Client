package net.minecraft.argon.create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Pseudo-Flywheel Instanced Batch Renderer.
 * Collects all Create kinetic gears, shafts, water wheels, windmills, belts,
 * machines, pipes, gauges, and rail bogeys into pre-allocated mesh instances
 * and flushes them in a single batched draw call per frame for 60 FPS on Kompanio 500.
 */
public final class FlywheelBatchRenderer
{
    public enum ModelId
    {
        SHAFT,
        COGWHEEL_SMALL,
        COGWHEEL_LARGE,
        GEARBOX_CASING,
        WATER_WHEEL,
        WINDMILL_BEARING,
        WINDMILL_SAIL_ROTOR,
        GAUGE_CASING,
        GAUGE_NEEDLE,
        BELT_FRAME,
        BELT_BAND,
        FUNNEL,
        CHUTE,
        PRESS_CASING,
        PRESS_HEAD,
        MIXER_CASING,
        MIXER_WHISK,
        BASIN,
        CRUSHING_WHEEL,
        FAN_CASING,
        FAN_BLADES,
        PIPE_CORE,
        PIPE_ARM_POS_Y,
        PUMP_COLLAR,
        TANK_CASING,
        BLAZE_BURNER_BODY,
        BLAZE_CORE,
        STEAM_ENGINE_BODY,
        STEAM_PISTON,
        PISTON_CASING,
        PISTON_POLE,
        GANTRY_SHAFT,
        GANTRY_CARRIAGE,
        TRAIN_BOGEY,
        TRAIN_STATION,
        TRAIN_CONTROLS,
        SAIL_BLOCK
    }

    private static final CreateMeshTemplate[] TEMPLATES = new CreateMeshTemplate[ModelId.values().length];
    private static boolean templatesBuilt = false;

    private static final int MAX_INSTANCES = 4096;
    private static final ModelId[] instModel = new ModelId[MAX_INSTANCES];
    private static final float[] instX = new float[MAX_INSTANCES];
    private static final float[] instY = new float[MAX_INSTANCES];
    private static final float[] instZ = new float[MAX_INSTANCES];
    private static final int[] instAxis = new int[MAX_INSTANCES];
    private static final float[] instAngle = new float[MAX_INSTANCES];
    private static final float[] instTintR = new float[MAX_INSTANCES];
    private static final float[] instTintG = new float[MAX_INSTANCES];
    private static final float[] instTintB = new float[MAX_INSTANCES];
    private static final float[] instLight = new float[MAX_INSTANCES];
    private static int instanceCount = 0;

    // Extra curved track segments queued for batch flush
    private static final int MAX_CURVE_SEGMENTS = 1024;
    private static final float[] segAx = new float[MAX_CURVE_SEGMENTS];
    private static final float[] segAy = new float[MAX_CURVE_SEGMENTS];
    private static final float[] segAz = new float[MAX_CURVE_SEGMENTS];
    private static final float[] segBx = new float[MAX_CURVE_SEGMENTS];
    private static final float[] segBy = new float[MAX_CURVE_SEGMENTS];
    private static final float[] segBz = new float[MAX_CURVE_SEGMENTS];
    private static int curveSegCount = 0;

    private FlywheelBatchRenderer()
    {
    }

    private static void ensureTemplates()
    {
        if (templatesBuilt)
        {
            return;
        }

        templatesBuilt = true;

        // 1. SHAFT (centered along Y-axis)
        TEMPLATES[ModelId.SHAFT.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.125F, -0.5F, -0.125F, 0.125F, 0.5F, 0.125F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.85F, 0.88F, 0.90F, 1.0F)
            .build();

        // 2. COGWHEEL_SMALL (shaft + wooden gear disc + 8 teeth)
        CreateMeshTemplate.Builder smallCog = new CreateMeshTemplate.Builder()
            .addBox(-0.125F, -0.5F, -0.125F, 0.125F, 0.5F, 0.125F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.85F, 0.88F, 0.90F, 1.0F)
            .addBox(-0.34F, -0.09F, -0.34F, 0.34F, 0.09F, 0.34F, CreateMeshTemplate.SpriteKey.PLANKS_SPRUCE, 0.95F, 0.85F, 0.72F, 1.0F);
        for (int t = 0; t < 4; ++t)
        {
            smallCog.addRotatedBox(0.0F, 0.0F, 0.0F, 0.48F, 0.07F, 0.075F, (float)(t * Math.PI / 4.0D), CreateMeshTemplate.SpriteKey.PLANKS_OAK, 0.98F, 0.90F, 0.78F, 1.0F);
        }
        TEMPLATES[ModelId.COGWHEEL_SMALL.ordinal()] = smallCog.build();

        // 3. COGWHEEL_LARGE (shaft + brass/spruce ring + 16 teeth)
        CreateMeshTemplate.Builder largeCog = new CreateMeshTemplate.Builder()
            .addBox(-0.125F, -0.5F, -0.125F, 0.125F, 0.5F, 0.125F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.85F, 0.88F, 0.90F, 1.0F)
            .addBox(-0.72F, -0.08F, -0.72F, 0.72F, 0.08F, 0.72F, CreateMeshTemplate.SpriteKey.PLANKS_SPRUCE, 0.85F, 0.75F, 0.62F, 1.0F);
        for (int t = 0; t < 8; ++t)
        {
            largeCog.addRotatedBox(0.0F, 0.0F, 0.0F, 0.96F, 0.065F, 0.075F, (float)(t * Math.PI / 8.0D), CreateMeshTemplate.SpriteKey.PLANKS_OAK, 0.95F, 0.88F, 0.72F, 1.0F);
        }
        TEMPLATES[ModelId.COGWHEEL_LARGE.ordinal()] = largeCog.build();

        // 4. GEARBOX_CASING
        TEMPLATES[ModelId.GEARBOX_CASING.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.48F, -0.48F, 0.48F, 0.48F, 0.48F, CreateMeshTemplate.SpriteKey.PLANKS_SPRUCE, 0.80F, 0.68F, 0.55F, 1.0F)
            .addBox(-0.50F, -0.14F, -0.14F, 0.50F, 0.14F, 0.14F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.75F, 0.78F, 0.80F, 1.0F)
            .addBox(-0.14F, -0.14F, -0.50F, 0.14F, 0.14F, 0.50F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.75F, 0.78F, 0.80F, 1.0F)
            .build();

        // 5. WATER_WHEEL (hub + 8 paddles)
        CreateMeshTemplate.Builder wheel = new CreateMeshTemplate.Builder()
            .addBox(-0.18F, -0.49F, -0.18F, 0.18F, 0.49F, 0.18F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.78F, 0.80F, 0.82F, 1.0F);
        for (int t = 0; t < 8; ++t)
        {
            float ang = (float)(t * Math.PI / 4.0D);
            float px = (float)Math.cos(ang) * 0.95F;
            float pz = (float)Math.sin(ang) * 0.95F;
            wheel.addRotatedBox(px * 0.5F, 0.0F, pz * 0.5F, 0.52F, 0.38F, 0.06F, -ang, CreateMeshTemplate.SpriteKey.PLANKS_OAK, 0.88F, 0.72F, 0.55F, 1.0F);
            wheel.addRotatedBox(px, 0.0F, pz, 0.08F, 0.42F, 0.24F, -ang, CreateMeshTemplate.SpriteKey.PLANKS_SPRUCE, 0.75F, 0.60F, 0.45F, 1.0F);
        }
        TEMPLATES[ModelId.WATER_WHEEL.ordinal()] = wheel.build();

        // 6. WINDMILL_BEARING & SAIL ROTOR
        TEMPLATES[ModelId.WINDMILL_BEARING.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.49F, -0.49F, -0.49F, 0.49F, 0.35F, 0.49F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.80F, 0.82F, 0.84F, 1.0F)
            .addBox(-0.35F, 0.35F, -0.35F, 0.35F, 0.50F, 0.35F, CreateMeshTemplate.SpriteKey.GOLD, 0.92F, 0.82F, 0.45F, 1.0F)
            .build();

        CreateMeshTemplate.Builder rotor = new CreateMeshTemplate.Builder()
            .addBox(-0.15F, -0.15F, -0.15F, 0.15F, 0.55F, 0.15F, CreateMeshTemplate.SpriteKey.PLANKS_SPRUCE, 0.85F, 0.75F, 0.60F, 1.0F);
        for (int arm = 0; arm < 4; ++arm)
        {
            float ang = (float)(arm * Math.PI * 0.5D);
            rotor.addRotatedBox((float)Math.cos(ang) * 1.35F, 0.28F, (float)Math.sin(ang) * 1.35F, 1.25F, 0.06F, 0.08F, -ang, CreateMeshTemplate.SpriteKey.PLANKS_OAK, 0.90F, 0.78F, 0.62F, 1.0F);
            rotor.addRotatedBox((float)Math.cos(ang) * 1.45F, 0.28F, (float)Math.sin(ang) * 1.45F + (float)Math.cos(ang) * 0.24F, 1.05F, 0.03F, 0.24F, -ang, CreateMeshTemplate.SpriteKey.WOOL_WHITE, 0.96F, 0.95F, 0.90F, 1.0F);
        }
        TEMPLATES[ModelId.WINDMILL_SAIL_ROTOR.ordinal()] = rotor.build();

        // 7. GAUGE_CASING & GAUGE_NEEDLE
        TEMPLATES[ModelId.GAUGE_CASING.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.46F, -0.46F, -0.46F, 0.46F, 0.46F, 0.46F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.78F, 0.80F, 0.82F, 1.0F)
            .addBox(-0.34F, 0.46F, -0.34F, 0.34F, 0.49F, 0.34F, CreateMeshTemplate.SpriteKey.WOOL_WHITE, 0.95F, 0.95F, 0.92F, 1.0F)
            .build();
        TEMPLATES[ModelId.GAUGE_NEEDLE.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.04F, 0.47F, -0.02F, 0.04F, 0.51F, 0.28F, CreateMeshTemplate.SpriteKey.GOLD, 1.0F, 0.35F, 0.20F, 1.0F)
            .build();

        // 8. BELT
        TEMPLATES[ModelId.BELT_FRAME.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.46F, -0.18F, -0.49F, 0.46F, 0.14F, 0.49F, CreateMeshTemplate.SpriteKey.OBSIDIAN, 0.35F, 0.36F, 0.40F, 1.0F)
            .build();
        TEMPLATES[ModelId.BELT_BAND.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.14F, -0.14F, -0.14F, 0.14F, 0.14F, 0.14F, CreateMeshTemplate.SpriteKey.IRON, 0.85F, 0.85F, 0.88F, 1.0F)
            .build();

        // 9. FUNNEL & CHUTE
        TEMPLATES[ModelId.FUNNEL.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.38F, -0.38F, -0.48F, 0.38F, 0.38F, -0.05F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.75F, 0.78F, 0.80F, 1.0F)
            .addBox(-0.26F, -0.26F, -0.05F, 0.26F, 0.26F, 0.35F, CreateMeshTemplate.SpriteKey.OBSIDIAN, 0.40F, 0.42F, 0.45F, 1.0F)
            .build();
        TEMPLATES[ModelId.CHUTE.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.36F, -0.50F, -0.36F, 0.36F, 0.50F, 0.36F, CreateMeshTemplate.SpriteKey.CAULDRON, 0.70F, 0.74F, 0.78F, 1.0F)
            .build();

        // 10. PRESS, MIXER, BASIN
        TEMPLATES[ModelId.PRESS_CASING.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.25F, -0.48F, 0.48F, 0.48F, 0.48F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.78F, 0.80F, 0.82F, 1.0F)
            .build();
        TEMPLATES[ModelId.PRESS_HEAD.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.22F, -0.48F, -0.22F, 0.22F, 0.25F, 0.22F, CreateMeshTemplate.SpriteKey.IRON, 0.90F, 0.92F, 0.95F, 1.0F)
            .addBox(-0.34F, -0.55F, -0.34F, 0.34F, -0.40F, 0.34F, CreateMeshTemplate.SpriteKey.ANVIL, 0.85F, 0.85F, 0.88F, 1.0F)
            .build();

        TEMPLATES[ModelId.MIXER_CASING.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.10F, -0.48F, 0.48F, 0.48F, 0.48F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.78F, 0.80F, 0.82F, 1.0F)
            .build();
        TEMPLATES[ModelId.MIXER_WHISK.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.08F, -0.60F, -0.08F, 0.08F, 0.05F, 0.08F, CreateMeshTemplate.SpriteKey.IRON, 0.90F, 0.90F, 0.92F, 1.0F)
            .addBox(-0.30F, -0.65F, -0.04F, 0.30F, -0.45F, 0.04F, CreateMeshTemplate.SpriteKey.IRON, 0.85F, 0.85F, 0.88F, 1.0F)
            .addBox(-0.04F, -0.65F, -0.30F, 0.04F, -0.45F, 0.30F, CreateMeshTemplate.SpriteKey.IRON, 0.85F, 0.85F, 0.88F, 1.0F)
            .build();

        TEMPLATES[ModelId.BASIN.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.48F, -0.48F, 0.48F, -0.32F, 0.48F, CreateMeshTemplate.SpriteKey.CAULDRON, 0.72F, 0.74F, 0.76F, 1.0F)
            .addBox(-0.48F, -0.32F, -0.48F, 0.48F, 0.40F, -0.36F, CreateMeshTemplate.SpriteKey.CAULDRON, 0.75F, 0.77F, 0.79F, 1.0F)
            .addBox(-0.48F, -0.32F, 0.36F, 0.48F, 0.40F, 0.48F, CreateMeshTemplate.SpriteKey.CAULDRON, 0.75F, 0.77F, 0.79F, 1.0F)
            .addBox(-0.48F, -0.32F, -0.36F, -0.36F, 0.40F, 0.36F, CreateMeshTemplate.SpriteKey.CAULDRON, 0.75F, 0.77F, 0.79F, 1.0F)
            .addBox(0.36F, -0.32F, -0.36F, 0.48F, 0.40F, 0.36F, CreateMeshTemplate.SpriteKey.CAULDRON, 0.75F, 0.77F, 0.79F, 1.0F)
            .build();

        // 11. CRUSHING WHEEL
        CreateMeshTemplate.Builder crusher = new CreateMeshTemplate.Builder()
            .addBox(-0.44F, -0.46F, -0.44F, 0.44F, 0.46F, 0.44F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.65F, 0.68F, 0.72F, 1.0F);
        for (int t = 0; t < 8; ++t)
        {
            crusher.addRotatedBox(0.0F, 0.0F, 0.0F, 0.56F, 0.40F, 0.06F, (float)(t * Math.PI / 8.0D), CreateMeshTemplate.SpriteKey.IRON, 0.85F, 0.85F, 0.88F, 1.0F);
        }
        TEMPLATES[ModelId.CRUSHING_WHEEL.ordinal()] = crusher.build();

        // 12. ENCASED FAN
        TEMPLATES[ModelId.FAN_CASING.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.48F, -0.48F, 0.48F, 0.48F, 0.48F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.80F, 0.80F, 0.82F, 1.0F)
            .build();
        TEMPLATES[ModelId.FAN_BLADES.ordinal()] = new CreateMeshTemplate.Builder()
            .addRotatedBox(0.0F, 0.50F, 0.0F, 0.38F, 0.02F, 0.10F, 0.35F, CreateMeshTemplate.SpriteKey.IRON, 0.92F, 0.92F, 0.95F, 1.0F)
            .addRotatedBox(0.0F, 0.50F, 0.0F, 0.10F, 0.02F, 0.38F, 0.35F, CreateMeshTemplate.SpriteKey.IRON, 0.92F, 0.92F, 0.95F, 1.0F)
            .build();

        // 13. FLUID PIPE, PUMP, TANK
        TEMPLATES[ModelId.PIPE_CORE.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.20F, -0.20F, -0.20F, 0.20F, 0.20F, 0.20F, CreateMeshTemplate.SpriteKey.GOLD, 0.88F, 0.58F, 0.34F, 1.0F)
            .build();
        TEMPLATES[ModelId.PIPE_ARM_POS_Y.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.16F, 0.18F, -0.16F, 0.16F, 0.50F, 0.16F, CreateMeshTemplate.SpriteKey.GOLD, 0.84F, 0.52F, 0.28F, 1.0F)
            .build();
        TEMPLATES[ModelId.PUMP_COLLAR.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.28F, -0.28F, -0.28F, 0.28F, 0.28F, 0.28F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.75F, 0.78F, 0.82F, 1.0F)
            .build();
        TEMPLATES[ModelId.TANK_CASING.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.49F, -0.48F, 0.48F, -0.40F, 0.48F, CreateMeshTemplate.SpriteKey.GOLD, 0.88F, 0.62F, 0.30F, 1.0F)
            .addBox(-0.48F, 0.40F, -0.48F, 0.48F, 0.49F, 0.48F, CreateMeshTemplate.SpriteKey.GOLD, 0.88F, 0.62F, 0.30F, 1.0F)
            .addBox(-0.46F, -0.40F, -0.46F, 0.46F, 0.40F, 0.46F, CreateMeshTemplate.SpriteKey.GLASS, 0.85F, 0.92F, 0.96F, 0.75F)
            .build();

        // 14. BLAZE BURNER & STEAM ENGINE
        TEMPLATES[ModelId.BLAZE_BURNER_BODY.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.46F, -0.48F, -0.46F, 0.46F, 0.12F, 0.46F, CreateMeshTemplate.SpriteKey.FURNACE, 0.65F, 0.65F, 0.68F, 1.0F)
            .addBox(-0.42F, 0.12F, -0.42F, 0.42F, 0.44F, 0.42F, CreateMeshTemplate.SpriteKey.ANVIL, 0.50F, 0.48F, 0.52F, 1.0F)
            .build();
        TEMPLATES[ModelId.BLAZE_CORE.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.24F, 0.05F, -0.24F, 0.24F, 0.48F, 0.24F, CreateMeshTemplate.SpriteKey.LAVA, 1.0F, 0.88F, 0.40F, 1.0F)
            .build();
        TEMPLATES[ModelId.STEAM_ENGINE_BODY.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.48F, -0.48F, 0.48F, 0.48F, 0.48F, CreateMeshTemplate.SpriteKey.GOLD, 0.95F, 0.78F, 0.32F, 1.0F)
            .addBox(-0.26F, 0.48F, -0.26F, 0.26F, 0.68F, 0.26F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.75F, 0.75F, 0.78F, 1.0F)
            .build();
        TEMPLATES[ModelId.STEAM_PISTON.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.14F, 0.10F, -0.14F, 0.14F, 0.86F, 0.14F, CreateMeshTemplate.SpriteKey.IRON, 0.95F, 0.95F, 0.95F, 1.0F)
            .build();

        // 15. MECHANICAL PISTON, GANTRY, TRAINS
        TEMPLATES[ModelId.PISTON_CASING.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.48F, -0.48F, 0.48F, 0.48F, 0.48F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.76F, 0.78F, 0.80F, 1.0F)
            .addBox(-0.42F, 0.46F, -0.42F, 0.42F, 0.52F, 0.42F, CreateMeshTemplate.SpriteKey.PLANKS_OAK, 0.95F, 0.85F, 0.68F, 1.0F)
            .build();
        TEMPLATES[ModelId.PISTON_POLE.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.14F, -0.50F, -0.14F, 0.14F, 0.50F, 0.14F, CreateMeshTemplate.SpriteKey.PLANKS_SPRUCE, 0.85F, 0.72F, 0.52F, 1.0F)
            .build();
        TEMPLATES[ModelId.GANTRY_SHAFT.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.16F, -0.50F, -0.16F, 0.16F, 0.50F, 0.16F, CreateMeshTemplate.SpriteKey.ANVIL, 0.78F, 0.42F, 0.40F, 1.0F)
            .build();
        TEMPLATES[ModelId.GANTRY_CARRIAGE.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.48F, -0.48F, 0.48F, 0.48F, 0.48F, CreateMeshTemplate.SpriteKey.ANDESITE, 0.82F, 0.72F, 0.58F, 1.0F)
            .build();
        TEMPLATES[ModelId.TRAIN_BOGEY.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.49F, -0.35F, -0.49F, 0.49F, 0.18F, 0.49F, CreateMeshTemplate.SpriteKey.ANVIL, 0.42F, 0.44F, 0.48F, 1.0F)
            .addBox(-0.49F, -0.48F, -0.42F, -0.38F, -0.10F, -0.12F, CreateMeshTemplate.SpriteKey.IRON, 0.82F, 0.82F, 0.85F, 1.0F)
            .addBox(0.38F, -0.48F, -0.42F, 0.49F, -0.10F, -0.12F, CreateMeshTemplate.SpriteKey.IRON, 0.82F, 0.82F, 0.85F, 1.0F)
            .addBox(-0.49F, -0.48F, 0.12F, -0.38F, -0.10F, 0.42F, CreateMeshTemplate.SpriteKey.IRON, 0.82F, 0.82F, 0.85F, 1.0F)
            .addBox(0.38F, -0.48F, 0.12F, 0.49F, -0.10F, 0.42F, CreateMeshTemplate.SpriteKey.IRON, 0.82F, 0.82F, 0.85F, 1.0F)
            .build();
        TEMPLATES[ModelId.TRAIN_STATION.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.48F, -0.48F, -0.48F, 0.48F, 0.48F, 0.48F, CreateMeshTemplate.SpriteKey.GOLD, 0.88F, 0.72F, 0.35F, 1.0F)
            .addBox(-0.32F, 0.48F, -0.32F, 0.32F, 0.85F, 0.32F, CreateMeshTemplate.SpriteKey.OBSIDIAN, 0.38F, 0.42F, 0.48F, 1.0F)
            .build();
        TEMPLATES[ModelId.TRAIN_CONTROLS.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.45F, -0.48F, -0.45F, 0.45F, 0.12F, 0.45F, CreateMeshTemplate.SpriteKey.GOLD, 0.90F, 0.74F, 0.36F, 1.0F)
            .addBox(-0.28F, 0.12F, -0.28F, 0.28F, 0.44F, 0.28F, CreateMeshTemplate.SpriteKey.ANVIL, 0.48F, 0.50F, 0.54F, 1.0F)
            .build();
        TEMPLATES[ModelId.SAIL_BLOCK.ordinal()] = new CreateMeshTemplate.Builder()
            .addBox(-0.49F, -0.49F, -0.49F, 0.49F, 0.49F, 0.49F, CreateMeshTemplate.SpriteKey.WOOL_WHITE, 0.96F, 0.94F, 0.88F, 1.0F)
            .build();
    }

    public static void enqueue(ModelId model, float cx, float cy, float cz, EnumFacing.Axis axis, float angleDeg, float r, float g, float b, float light)
    {
        if (instanceCount >= MAX_INSTANCES || model == null)
        {
            return;
        }

        int idx = instanceCount++;
        instModel[idx] = model;
        instX[idx] = cx;
        instY[idx] = cy;
        instZ[idx] = cz;
        instAxis[idx] = axis == EnumFacing.Axis.X ? 0 : (axis == EnumFacing.Axis.Z ? 2 : 1);
        instAngle[idx] = angleDeg * 0.017453292F;
        instTintR[idx] = r;
        instTintG[idx] = g;
        instTintB[idx] = b;
        instLight[idx] = light;
    }

    public static void enqueue(ModelId model, BlockPos pos, EnumFacing.Axis axis, float angleDeg, float light)
    {
        enqueue(model, (float)pos.getX() + 0.5F, (float)pos.getY() + 0.5F, (float)pos.getZ() + 0.5F, axis, angleDeg, 1.0F, 1.0F, 1.0F, light);
    }

    public static void enqueueRailSegment(float ax, float ay, float az, float bx, float by, float bz)
    {
        if (curveSegCount >= MAX_CURVE_SEGMENTS)
        {
            return;
        }

        int idx = curveSegCount++;
        segAx[idx] = ax;
        segAy[idx] = ay;
        segAz[idx] = az;
        segBx[idx] = bx;
        segBy[idx] = by;
        segBz[idx] = bz;
    }

    public static void flush(double camX, double camY, double camZ)
    {
        if (instanceCount == 0 && curveSegCount == 0)
        {
            return;
        }

        ensureTemplates();

        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        buf.setTranslation(-camX, -camY, -camZ);

        // Pre-resolve sprite lookup cache for this frame
        CreateMeshTemplate.SpriteKey[] spriteKeys = CreateMeshTemplate.SpriteKey.values();
        TextureAtlasSprite[] sprites = new TextureAtlasSprite[spriteKeys.length];

        for (int s = 0; s < spriteKeys.length; ++s)
        {
            sprites[s] = CreateMeshTemplate.resolveSprite(spriteKeys[s]);
        }

        for (int i = 0; i < instanceCount; ++i)
        {
            CreateMeshTemplate tmpl = TEMPLATES[instModel[i].ordinal()];

            if (tmpl == null)
            {
                continue;
            }

            float cx = instX[i];
            float cy = instY[i];
            float cz = instZ[i];
            int ax = instAxis[i];
            float ang = instAngle[i];
            float cos = (float)Math.cos(ang);
            float sin = (float)Math.sin(ang);
            float tr = instTintR[i] * instLight[i];
            float tg = instTintG[i] * instLight[i];
            float tb = instTintB[i] * instLight[i];

            CreateMeshTemplate.Quad[] quads = tmpl.quads;

            for (int qIdx = 0; qIdx < quads.length; ++qIdx)
            {
                CreateMeshTemplate.Quad q = quads[qIdx];
                TextureAtlasSprite sp = sprites[q.sprite.ordinal()];
                float u0 = sp.getMinU();
                float u1 = sp.getMaxU();
                float v0 = sp.getMinV();
                float v1 = sp.getMaxV();
                float cr = q.r * tr;
                float cg = q.g * tg;
                float cb = q.b * tb;
                float ca = q.a;

                for (int v = 0; v < 4; ++v)
                {
                    float lx = q.x[v];
                    float ly = q.y[v];
                    float lz = q.z[v];

                    // Spin around local Y first
                    float sx = lx * cos - lz * sin;
                    float sz = lx * sin + lz * cos;
                    float sy = ly;

                    // Re-orient local Y onto target axis (0=X, 1=Y, 2=Z)
                    float wx;
                    float wy;
                    float wz;

                    if (ax == 0)
                    {
                        wx = sy;
                        wy = -sx;
                        wz = sz;
                    }
                    else if (ax == 2)
                    {
                        wx = sx;
                        wy = -sz;
                        wz = sy;
                    }
                    else
                    {
                        wx = sx;
                        wy = sy;
                        wz = sz;
                    }

                    float u = (v == 0 || v == 1) ? u0 : u1;
                    float vv = (v == 1 || v == 2) ? v1 : v0;
                    buf.pos((double)(cx + wx), (double)(cy + wy), (double)(cz + wz))
                       .tex((double)u, (double)vv)
                       .color(cr, cg, cb, ca)
                       .endVertex();
                }
            }
        }

        // Flush queued rail track & curved track segments in the exact same batched draw call!
        if (curveSegCount > 0)
        {
            TextureAtlasSprite ironSp = sprites[CreateMeshTemplate.SpriteKey.IRON.ordinal()];
            TextureAtlasSprite woodSp = sprites[CreateMeshTemplate.SpriteKey.PLANKS_SPRUCE.ordinal()];

            for (int s = 0; s < curveSegCount; ++s)
            {
                emitTrackQuadSegment(buf, segAx[s], segAy[s], segAz[s], segBx[s], segBy[s], segBz[s], ironSp, woodSp);
            }
        }

        buf.setTranslation(0.0D, 0.0D, 0.0D);
        tess.draw();

        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();

        instanceCount = 0;
        curveSegCount = 0;
    }

    private static void emitTrackQuadSegment(
        BufferBuilder buf,
        float ax, float ay, float az,
        float bx, float by, float bz,
        TextureAtlasSprite ironSp, TextureAtlasSprite woodSp)
    {
        float dx = bx - ax;
        float dz = bz - az;
        float len = (float)Math.sqrt(dx * dx + dz * dz);

        if (len < 0.001F)
        {
            return;
        }

        float nx = -dz / len;
        float nz = dx / len;

        // Left & Right steel rails (+/- 0.72m gauge) + wooden sleeper tie at midpoint
        float u0 = ironSp.getMinU();
        float u1 = ironSp.getMaxU();
        float v0 = ironSp.getMinV();
        float v1 = ironSp.getMaxV();

        float[] offsets = new float[] { -0.72F, 0.72F };

        for (int r = 0; r < 2; ++r)
        {
            float off = offsets[r];
            float rx0 = ax + nx * (off - 0.06F);
            float rz0 = az + nz * (off - 0.06F);
            float rx1 = ax + nx * (off + 0.06F);
            float rz1 = az + nz * (off + 0.06F);
            float tx0 = bx + nx * (off - 0.06F);
            float tz0 = bz + nz * (off - 0.06F);
            float tx1 = bx + nx * (off + 0.06F);
            float tz1 = bz + nz * (off + 0.06F);

            buf.pos((double)rx1, (double)(ay + 0.14F), (double)rz1).tex((double)u1, (double)v0).color(0.75F, 0.78F, 0.82F, 1.0F).endVertex();
            buf.pos((double)tx1, (double)(by + 0.14F), (double)tz1).tex((double)u1, (double)v1).color(0.75F, 0.78F, 0.82F, 1.0F).endVertex();
            buf.pos((double)tx0, (double)(by + 0.14F), (double)tz0).tex((double)u0, (double)v1).color(0.75F, 0.78F, 0.82F, 1.0F).endVertex();
            buf.pos((double)rx0, (double)(ay + 0.14F), (double)rz0).tex((double)u0, (double)v0).color(0.75F, 0.78F, 0.82F, 1.0F).endVertex();
        }

        // Cross sleeper tie
        float mx = (ax + bx) * 0.5F;
        float my = (ay + by) * 0.5F + 0.08F;
        float mz = (az + bz) * 0.5F;
        float tx = dx / len * 0.12F;
        float tz = dz / len * 0.12F;
        float wx = nx * 1.15F;
        float wz = nz * 1.15F;

        float wu0 = woodSp.getMinU();
        float wu1 = woodSp.getMaxU();
        float wv0 = woodSp.getMinV();
        float wv1 = woodSp.getMaxV();

        buf.pos((double)(mx + wx - tx), (double)my, (double)(mz + wz - tz)).tex((double)wu1, (double)wv0).color(0.65F, 0.50F, 0.36F, 1.0F).endVertex();
        buf.pos((double)(mx + wx + tx), (double)my, (double)(mz + wz + tz)).tex((double)wu1, (double)wv1).color(0.65F, 0.50F, 0.36F, 1.0F).endVertex();
        buf.pos((double)(mx - wx + tx), (double)my, (double)(mz - wz + tz)).tex((double)wu0, (double)wv1).color(0.65F, 0.50F, 0.36F, 1.0F).endVertex();
        buf.pos((double)(mx - wx - tx), (double)my, (double)(mz - wz - tz)).tex((double)wu0, (double)wv0).color(0.65F, 0.50F, 0.36F, 1.0F).endVertex();
    }

    public static void clearPending()
    {
        instanceCount = 0;
        curveSegCount = 0;
    }
}
