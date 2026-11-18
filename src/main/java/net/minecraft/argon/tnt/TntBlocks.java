package net.minecraft.argon.tnt;

import net.minecraft.block.Block;

/**
 * Central registration for all 17 Argon Lucky / Too Much / Unlucky TNT blocks (IDs 492..508).
 */
public final class TntBlocks
{
    public static BlockArgonTNT LUCKY_TNT;
    public static BlockArgonTNT SUPER_LUCKY_TNT;
    public static BlockArgonTNT MINERAL_TNT;
    public static BlockArgonTNT HOUSE_TNT;

    public static BlockArgonTNT TNT_X5;
    public static BlockArgonTNT TNT_X20;
    public static BlockArgonTNT TNT_X100;
    public static BlockArgonTNT NUKE_TNT;
    public static BlockArgonTNT METEOR_TNT;
    public static BlockArgonTNT OCEAN_TNT;
    public static BlockArgonTNT FLAT_TNT;
    public static BlockArgonTNT MINING_FLAT_TNT;
    public static BlockArgonTNT CLUSTER_TNT;

    public static BlockArgonTNT UNLUCKY_TNT;
    public static BlockArgonTNT CURSED_NUKE_TNT;
    public static BlockArgonTNT LAVA_TRAP_TNT;
    public static BlockArgonTNT MOB_SWARM_TNT;

    public static BlockArgonTNT[] ALL_BLOCKS = new BlockArgonTNT[0];

    private TntBlocks()
    {
    }

    public static void registerBlocks()
    {
        LUCKY_TNT = registerTnt(492, TntType.LUCKY_TNT);
        SUPER_LUCKY_TNT = registerTnt(493, TntType.SUPER_LUCKY_TNT);
        MINERAL_TNT = registerTnt(494, TntType.MINERAL_TNT);
        HOUSE_TNT = registerTnt(495, TntType.HOUSE_TNT);

        TNT_X5 = registerTnt(496, TntType.TNT_X5);
        TNT_X20 = registerTnt(497, TntType.TNT_X20);
        TNT_X100 = registerTnt(498, TntType.TNT_X100);
        NUKE_TNT = registerTnt(499, TntType.NUKE_TNT);
        METEOR_TNT = registerTnt(500, TntType.METEOR_TNT);
        OCEAN_TNT = registerTnt(501, TntType.OCEAN_TNT);
        FLAT_TNT = registerTnt(502, TntType.FLAT_TNT);
        MINING_FLAT_TNT = registerTnt(503, TntType.MINING_FLAT_TNT);
        CLUSTER_TNT = registerTnt(504, TntType.CLUSTER_TNT);

        UNLUCKY_TNT = registerTnt(505, TntType.UNLUCKY_TNT);
        CURSED_NUKE_TNT = registerTnt(506, TntType.CURSED_NUKE_TNT);
        LAVA_TRAP_TNT = registerTnt(507, TntType.LAVA_TRAP_TNT);
        MOB_SWARM_TNT = registerTnt(508, TntType.MOB_SWARM_TNT);

        ALL_BLOCKS = new BlockArgonTNT[] {
            LUCKY_TNT,
            SUPER_LUCKY_TNT,
            MINERAL_TNT,
            HOUSE_TNT,
            TNT_X5,
            TNT_X20,
            TNT_X100,
            NUKE_TNT,
            METEOR_TNT,
            OCEAN_TNT,
            FLAT_TNT,
            MINING_FLAT_TNT,
            CLUSTER_TNT,
            UNLUCKY_TNT,
            CURSED_NUKE_TNT,
            LAVA_TRAP_TNT,
            MOB_SWARM_TNT
        };
    }

    private static BlockArgonTNT registerTnt(int blockId, TntType type)
    {
        BlockArgonTNT block = new BlockArgonTNT(type);
        Block.registerBlock(blockId, type.registryName, block);
        return block;
    }
}
