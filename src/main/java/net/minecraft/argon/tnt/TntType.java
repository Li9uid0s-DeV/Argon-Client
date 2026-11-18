package net.minecraft.argon.tnt;

/**
 * Definitions for Lucky TNT, Too Much TNT, and Unlucky TNT blocks/entities.
 */
public enum TntType
{
    // Lucky TNT Series
    LUCKY_TNT(
        "argon_lucky_tnt",
        "Lucky TNT",
        Category.LUCKY,
        8,
        70,
        0xF5C842,
        0xFFF6C2,
        0xE08E1B,
        "Random lucky treasures, ore fountains, rainbow domes, or friendly golems."
    ),
    SUPER_LUCKY_TNT(
        "argon_super_lucky_tnt",
        "Super Lucky TNT",
        Category.LUCKY,
        12,
        85,
        0x3CE6B8,
        0xF5E663,
        0x1CA88A,
        "Stacks multiple jackpot blessings: diamond spire, enchanted loot & XP."
    ),
    MINERAL_TNT(
        "argon_mineral_tnt",
        "Mineral Ore TNT",
        Category.LUCKY,
        11,
        75,
        0x36A2EB,
        0xE8F4FA,
        0x1F5E8C,
        "Transforms surrounding stone into rich Diamond, Emerald, Gold & Iron veins."
    ),
    HOUSE_TNT(
        "argon_house_tnt",
        "Instant House TNT",
        Category.LUCKY,
        7,
        65,
        0xB67D42,
        0xEFE4D0,
        0x6D4520,
        "Clears ground and builds a complete lit wooden shelter via scheduler."
    ),

    // Too Much TNT Series
    TNT_X5(
        "argon_tnt_x5",
        "TNT x5",
        Category.TOO_MUCH,
        10,
        80,
        0xD93829,
        0xF2F2F2,
        0x8A1C14,
        "5x power explosive blast (radius 10)."
    ),
    TNT_X20(
        "argon_tnt_x20",
        "TNT x20",
        Category.TOO_MUCH,
        17,
        90,
        0xC7261C,
        0xFFCC4D,
        0x6E120C,
        "20x massive crater explosion (radius 17)."
    ),
    TNT_X100(
        "argon_tnt_x100",
        "TNT x100",
        Category.TOO_MUCH,
        26,
        110,
        0xA61414,
        0xFF7B29,
        0x4A0808,
        "100x mega-crater (radius 26), smoothly carved at 500 blocks/tick."
    ),
    NUKE_TNT(
        "argon_nuke_tnt",
        "Nuclear TNT",
        Category.TOO_MUCH,
        32,
        130,
        0x4FA832,
        0xE6F23A,
        0x22381C,
        "Massive nuclear crater (radius 32) with scorched obsidian & mushroom cloud."
    ),
    METEOR_TNT(
        "argon_meteor_tnt",
        "Meteor TNT",
        Category.TOO_MUCH,
        18,
        95,
        0x3B2B2B,
        0xFF5A1F,
        0x7D2310,
        "Blazing magma & obsidian impact crater with molten core."
    ),
    OCEAN_TNT(
        "argon_ocean_tnt",
        "Ocean TNT",
        Category.TOO_MUCH,
        15,
        80,
        0x1E6FD9,
        0xA8E0FF,
        0x0E3D7A,
        "Carves and floods a wide water basin (radius 15)."
    ),
    FLAT_TNT(
        "argon_flat_tnt",
        "Flat TNT",
        Category.TOO_MUCH,
        20,
        75,
        0x7F8C8D,
        0xECF0F1,
        0x4C5658,
        "Flattens all terrain at or above fuse height within radius 20."
    ),
    MINING_FLAT_TNT(
        "argon_mining_flat_tnt",
        "Mining Flat TNT",
        Category.TOO_MUCH,
        16,
        75,
        0x5D6D7E,
        0x58D68D,
        0x2E4053,
        "Excavates stone & dirt across radius 16 while preserving all ores."
    ),
    CLUSTER_TNT(
        "argon_cluster_tnt",
        "Cluster TNT",
        Category.TOO_MUCH,
        7,
        70,
        0xE67E22,
        0xFFF0D4,
        0x934A10,
        "Launches 14 secondary primed TNT charges outward across the blast zone."
    ),

    // Unlucky TNT Series
    UNLUCKY_TNT(
        "argon_unlucky_tnt",
        "Unlucky TNT",
        Category.UNLUCKY,
        10,
        70,
        0x5B2C6F,
        0xD7BDE2,
        0x2C1238,
        "Random cursed hazard: cobweb prison, anvil rain, lava pit, or ambush."
    ),
    CURSED_NUKE_TNT(
        "argon_cursed_nuke_tnt",
        "Cursed Nuke TNT",
        Category.UNLUCKY,
        20,
        115,
        0x311B47,
        0xB026FF,
        0x1A0B26,
        "Corrupts terrain into Netherrack/Soul Sand & summons Wither Skeletons + Lightning."
    ),
    LAVA_TRAP_TNT(
        "argon_lava_trap_tnt",
        "Lava Trap TNT",
        Category.UNLUCKY,
        11,
        75,
        0x8E240E,
        0xFF6F00,
        0x3B0C04,
        "Turns the blast floor into a deep magma & lava hazard with Magma Cubes."
    ),
    MOB_SWARM_TNT(
        "argon_mob_swarm_tnt",
        "Mob Swarm TNT",
        Category.UNLUCKY,
        10,
        75,
        0x2E4032,
        0xE74C3C,
        0x172119,
        "Unleashes a hostile mob & Argon Soldier ambush staggered at 20 mobs/tick."
    );

    public enum Category
    {
        LUCKY("Lucky TNT", "\u00a7e"),
        TOO_MUCH("Too Much TNT", "\u00a7c"),
        UNLUCKY("Unlucky TNT", "\u00a75");

        public final String label;
        public final String colorCode;

        Category(String label, String colorCode)
        {
            this.label = label;
            this.colorCode = colorCode;
        }
    }

    public final String registryName;
    public final String displayName;
    public final Category category;
    public final int radius;
    public final int fuseTicks;
    public final int bodyColor;
    public final int bandColor;
    public final int topColor;
    public final String description;

    TntType(
        String registryName,
        String displayName,
        Category category,
        int radius,
        int fuseTicks,
        int bodyColor,
        int bandColor,
        int topColor,
        String description)
    {
        this.registryName = registryName;
        this.displayName = displayName;
        this.category = category;
        this.radius = radius;
        this.fuseTicks = fuseTicks;
        this.bodyColor = bodyColor;
        this.bandColor = bandColor;
        this.topColor = topColor;
        this.description = description;
    }

    public static TntType byOrdinal(int idx)
    {
        TntType[] vals = values();
        return idx >= 0 && idx < vals.length ? vals[idx] : LUCKY_TNT;
    }
}
