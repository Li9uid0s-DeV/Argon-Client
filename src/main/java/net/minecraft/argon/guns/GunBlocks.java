package net.minecraft.argon.guns;

import net.minecraft.block.Block;

/**
 * Central registration for Argon Gun Mod blocks (IDs 490..491).
 */
public final class GunBlocks
{
    public static Block GUN_WORKBENCH;
    public static Block AUTO_TURRET;

    public static Block[] ALL_BLOCKS = new Block[0];

    private GunBlocks()
    {
    }

    public static void registerBlocks()
    {
        GUN_WORKBENCH = new BlockGunWorkbench();
        AUTO_TURRET = new BlockAutoTurret();

        Block.registerBlock(490, "argon_gun_workbench", GUN_WORKBENCH);
        Block.registerBlock(491, "argon_auto_turret", AUTO_TURRET);

        ALL_BLOCKS = new Block[] {
            GUN_WORKBENCH,
            AUTO_TURRET
        };
    }
}
