package net.minecraft.argon.guns;

import net.minecraft.tileentity.TileEntity;

/**
 * Registers Argon Gun Mod TileEntities.
 */
public final class GunTileEntities
{
    private GunTileEntities()
    {
    }

    public static void registerTileEntities()
    {
        TileEntity.register("argon_gun_workbench", TileEntityGunWorkbench.class);
        TileEntity.register("argon_auto_turret", TileEntityAutoTurret.class);
    }
}
