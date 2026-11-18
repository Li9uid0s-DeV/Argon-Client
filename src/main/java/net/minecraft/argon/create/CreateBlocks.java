package net.minecraft.argon.create;

import net.minecraft.block.Block;

public final class CreateBlocks
{
    public static final Block SHAFT = new BlockKineticPart(TileEntityKineticBasic.TYPE_SHAFT, "createShaft", "Shaft (Create)");
    public static final Block COGWHEEL_SMALL = new BlockKineticPart(TileEntityKineticBasic.TYPE_COGWHEEL_SMALL, "createCogwheel", "Cogwheel (Create)");
    public static final Block COGWHEEL_LARGE = new BlockKineticPart(TileEntityKineticBasic.TYPE_COGWHEEL_LARGE, "createLargeCogwheel", "Large Cogwheel (Create)");
    public static final Block GEARBOX = new BlockKineticPart(TileEntityKineticBasic.TYPE_GEARBOX, "createGearbox", "Gearbox (Create)");
    public static final Block WATER_WHEEL = new BlockWaterWheel();
    public static final Block WINDMILL_BEARING = new BlockWindmillBearing();
    public static final Block SAIL = new BlockSail();
    public static final Block SPEEDOMETER = new BlockGauge(TileEntityGauge.KIND_SPEEDOMETER, "createSpeedometer");
    public static final Block STRESSOMETER = new BlockGauge(TileEntityGauge.KIND_STRESSOMETER, "createStressometer");

    public static final Block BELT = new BlockBelt();
    public static final Block FUNNEL = new BlockFunnelChute(TileEntityFunnelChute.KIND_FUNNEL, "createFunnel");
    public static final Block CHUTE = new BlockFunnelChute(TileEntityFunnelChute.KIND_CHUTE, "createChute");
    public static final Block BASIN = new BlockBasin();
    public static final Block MECHANICAL_PRESS = new BlockPressMixer(TileEntityPressMixer.KIND_PRESS, "createMechanicalPress");
    public static final Block MECHANICAL_MIXER = new BlockPressMixer(TileEntityPressMixer.KIND_MIXER, "createMechanicalMixer");
    public static final Block CRUSHING_WHEEL = new BlockCrushingFan(TileEntityCrushingFan.KIND_CRUSHING_WHEEL, "createCrushingWheel");
    public static final Block ENCASED_FAN = new BlockCrushingFan(TileEntityCrushingFan.KIND_ENCASED_FAN, "createEncasedFan");
    public static final Block FLUID_PIPE = new BlockFluidNode(TileEntityFluidNode.KIND_PIPE, "createFluidPipe");
    public static final Block MECHANICAL_PUMP = new BlockFluidNode(TileEntityFluidNode.KIND_PUMP, "createMechanicalPump");
    public static final Block FLUID_TANK = new BlockFluidNode(TileEntityFluidNode.KIND_TANK, "createFluidTank");
    public static final Block BLAZE_BURNER = new BlockBlazeSteam(TileEntityBlazeSteam.KIND_BLAZE_BURNER, "createBlazeBurner");
    public static final Block STEAM_ENGINE = new BlockBlazeSteam(TileEntityBlazeSteam.KIND_STEAM_ENGINE, "createSteamEngine");

    public static final Block MECHANICAL_PISTON = new BlockMechanicalPiston();
    public static final Block PISTON_POLE = new BlockPistonPole();
    public static final Block GANTRY_SHAFT = new BlockKineticPart(TileEntityKineticBasic.TYPE_GANTRY_SHAFT, "createGantryShaft", "Gantry Shaft (Create)");
    public static final Block GANTRY_CARRIAGE = new BlockGantryCarriage();
    public static final Block TRAIN_TRACK = new BlockTrainTrack();
    public static final Block TRAIN_BOGEY = new BlockTrainBogey();
    public static final Block TRAIN_STATION = new BlockTrainStation();
    public static final Block TRAIN_CONTROLS = new BlockTrainControls();

    public static final Block[] ALL_BLOCKS = new Block[]
    {
        SHAFT,
        COGWHEEL_SMALL,
        COGWHEEL_LARGE,
        GEARBOX,
        WATER_WHEEL,
        WINDMILL_BEARING,
        SAIL,
        SPEEDOMETER,
        STRESSOMETER,
        BELT,
        FUNNEL,
        CHUTE,
        BASIN,
        MECHANICAL_PRESS,
        MECHANICAL_MIXER,
        CRUSHING_WHEEL,
        ENCASED_FAN,
        FLUID_PIPE,
        MECHANICAL_PUMP,
        FLUID_TANK,
        BLAZE_BURNER,
        STEAM_ENGINE,
        MECHANICAL_PISTON,
        PISTON_POLE,
        GANTRY_SHAFT,
        GANTRY_CARRIAGE,
        TRAIN_TRACK,
        TRAIN_BOGEY,
        TRAIN_STATION,
        TRAIN_CONTROLS
    };

    private static boolean registered = false;

    private CreateBlocks()
    {
    }

    public static void registerBlocks()
    {
        if (registered)
        {
            return;
        }

        registered = true;
        Block.registerBlock(460, "create_shaft", SHAFT);
        Block.registerBlock(461, "create_cogwheel", COGWHEEL_SMALL);
        Block.registerBlock(462, "create_large_cogwheel", COGWHEEL_LARGE);
        Block.registerBlock(463, "create_gearbox", GEARBOX);
        Block.registerBlock(464, "create_water_wheel", WATER_WHEEL);
        Block.registerBlock(465, "create_windmill_bearing", WINDMILL_BEARING);
        Block.registerBlock(466, "create_sail", SAIL);
        Block.registerBlock(467, "create_speedometer", SPEEDOMETER);
        Block.registerBlock(468, "create_stressometer", STRESSOMETER);
        Block.registerBlock(469, "create_belt", BELT);
        Block.registerBlock(470, "create_funnel", FUNNEL);
        Block.registerBlock(471, "create_chute", CHUTE);
        Block.registerBlock(472, "create_basin", BASIN);
        Block.registerBlock(473, "create_mechanical_press", MECHANICAL_PRESS);
        Block.registerBlock(474, "create_mechanical_mixer", MECHANICAL_MIXER);
        Block.registerBlock(475, "create_crushing_wheel", CRUSHING_WHEEL);
        Block.registerBlock(476, "create_encased_fan", ENCASED_FAN);
        Block.registerBlock(477, "create_fluid_pipe", FLUID_PIPE);
        Block.registerBlock(478, "create_mechanical_pump", MECHANICAL_PUMP);
        Block.registerBlock(479, "create_fluid_tank", FLUID_TANK);
        Block.registerBlock(480, "create_blaze_burner", BLAZE_BURNER);
        Block.registerBlock(481, "create_steam_engine", STEAM_ENGINE);
        Block.registerBlock(482, "create_mechanical_piston", MECHANICAL_PISTON);
        Block.registerBlock(483, "create_piston_pole", PISTON_POLE);
        Block.registerBlock(484, "create_gantry_shaft", GANTRY_SHAFT);
        Block.registerBlock(485, "create_gantry_carriage", GANTRY_CARRIAGE);
        Block.registerBlock(486, "create_train_track", TRAIN_TRACK);
        Block.registerBlock(487, "create_train_bogey", TRAIN_BOGEY);
        Block.registerBlock(488, "create_train_station", TRAIN_STATION);
        Block.registerBlock(489, "create_train_controls", TRAIN_CONTROLS);
    }
}
