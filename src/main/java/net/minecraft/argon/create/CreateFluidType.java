package net.minecraft.argon.create;

public enum CreateFluidType
{
    EMPTY("empty", 0x00000000, "minecraft:blocks/water_still"),
    WATER("water", 0xCC2F6BFF, "minecraft:blocks/water_still"),
    LAVA("lava", 0xFFFF6B1A, "minecraft:blocks/lava_still"),
    STEAM("steam", 0xCCEEF6FF, "minecraft:blocks/wool_colored_white"),
    MILK("milk", 0xFFFDFDFD, "minecraft:blocks/wool_colored_white"),
    HONEY("honey", 0xFFEAA11B, "minecraft:blocks/lava_still"),
    SEED_OIL("seed_oil", 0xFFD9C938, "minecraft:blocks/water_still");

    public final String id;
    public final int colorARGB;
    public final String spriteName;

    private CreateFluidType(String id, int colorARGB, String spriteName)
    {
        this.id = id;
        this.colorARGB = colorARGB;
        this.spriteName = spriteName;
    }

    public static CreateFluidType byId(String id)
    {
        for (CreateFluidType t : values())
        {
            if (t.id.equals(id))
            {
                return t;
            }
        }

        return EMPTY;
    }

    public String getDisplayName()
    {
        switch (this)
        {
            case WATER:
                return "Water";
            case LAVA:
                return "Lava";
            case STEAM:
                return "Steam";
            case MILK:
                return "Milk";
            case HONEY:
                return "Honey";
            case SEED_OIL:
                return "Seed Oil";
            default:
                return "Empty";
        }
    }
}
