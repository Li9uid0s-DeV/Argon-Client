package net.minecraft.argon.create;

import net.minecraft.tileentity.TileEntity;

public final class CreateTileEntities
{
    private static boolean registered = false;

    private CreateTileEntities()
    {
    }

    public static void registerTileEntities()
    {
        if (registered)
        {
            return;
        }

        registered = true;
        TileEntity.register("create_kinetic_basic", TileEntityKineticBasic.class);
        TileEntity.register("create_water_wheel", TileEntityWaterWheel.class);
        TileEntity.register("create_windmill_bearing", TileEntityWindmillBearing.class);
        TileEntity.register("create_gauge", TileEntityGauge.class);
        TileEntity.register("create_belt", TileEntityBelt.class);
        TileEntity.register("create_funnel_chute", TileEntityFunnelChute.class);
        TileEntity.register("create_basin", TileEntityBasin.class);
        TileEntity.register("create_press_mixer", TileEntityPressMixer.class);
        TileEntity.register("create_crushing_fan", TileEntityCrushingFan.class);
        TileEntity.register("create_fluid_node", TileEntityFluidNode.class);
        TileEntity.register("create_blaze_steam", TileEntityBlazeSteam.class);
        TileEntity.register("create_mechanical_piston", TileEntityMechanicalPiston.class);
        TileEntity.register("create_gantry_carriage", TileEntityGantryCarriage.class);
        TileEntity.register("create_train_track", TileEntityTrainTrack.class);
        TileEntity.register("create_train_station", TileEntityTrainStation.class);
        TileEntity.register("create_train_decor", TileEntityTrainDecor.class);
    }
}
