package net.minecraft.argon.guns;

/**
 * Core weapon definitions combining MrCrayfish's Gun Mod tactical firearms
 * and Techguns heavy sci-fi / military ordnance.
 * TeaVM / single-thread compatible Java 8 enum.
 */
public enum GunType
{
    PISTOL_9MM(
        "pistol_9mm",
        "9mm Tactical Pistol",
        "9mm Parabellum",
        6.5F,
        5,
        15,
        26,
        1.8F,
        2.2F,
        3.4F,
        1,
        false,
        0,
        0.82F,
        false,
        EntityArgonBullet.BulletKind.BALLISTIC,
        0x3C424A,
        0x22262B
    ),
    ASSAULT_RIFLE(
        "assault_rifle",
        "M4A1 Assault Rifle",
        "5.56mm NATO",
        7.2F,
        3,
        30,
        40,
        2.4F,
        2.6F,
        3.9F,
        1,
        true,
        0,
        0.72F,
        false,
        EntityArgonBullet.BulletKind.BALLISTIC,
        0x2D333B,
        0x4A5240
    ),
    SHOTGUN_12G(
        "shotgun_12g",
        "M870 Combat Shotgun",
        "12-Gauge Buckshot",
        3.4F,
        14,
        8,
        46,
        7.8F,
        5.5F,
        3.1F,
        7,
        false,
        0,
        0.85F,
        false,
        EntityArgonBullet.BulletKind.PELLET,
        0x363A40,
        0x6B4423
    ),
    SNIPER_50(
        "sniper_50",
        "M82 .50 Cal Sniper",
        ".50 BMG",
        22.0F,
        22,
        5,
        54,
        0.35F,
        6.8F,
        5.2F,
        1,
        false,
        0,
        0.35F,
        true,
        EntityArgonBullet.BulletKind.SNIPER,
        0x252B31,
        0x3F4A3C
    ),
    MINIGUN(
        "minigun",
        "M134 Vulcan Minigun",
        "5.56mm NATO",
        5.2F,
        2,
        120,
        70,
        4.6F,
        1.9F,
        3.8F,
        1,
        true,
        10,
        0.88F,
        false,
        EntityArgonBullet.BulletKind.BALLISTIC,
        0x2A2F36,
        0x525961
    ),
    ROCKET_LAUNCHER(
        "rocket_launcher",
        "AT4 Rocket Launcher",
        "84mm HE Rocket",
        24.0F,
        34,
        1,
        58,
        1.0F,
        7.5F,
        1.95F,
        1,
        false,
        0,
        0.76F,
        false,
        EntityArgonBullet.BulletKind.ROCKET,
        0x3B4D35,
        0x252D22
    ),
    PLASMA_RIFLE(
        "plasma_rifle",
        "TG-9 Pulse Plasma Rifle",
        "Plasma Energy Cell",
        11.5F,
        4,
        24,
        44,
        1.2F,
        1.8F,
        3.6F,
        1,
        true,
        0,
        0.70F,
        false,
        EntityArgonBullet.BulletKind.PLASMA,
        0xD5DCE6,
        0x1B9CE5
    );

    public final String registryName;
    public final String displayName;
    public final String caliberLabel;
    public final float baseDamage;
    public final int fireIntervalTicks;
    public final int baseMagazineSize;
    public final int baseReloadTicks;
    public final float baseSpread;
    public final float baseRecoilPitch;
    public final float projectileSpeed;
    public final int pelletsPerShot;
    public final boolean fullAuto;
    public final int spinUpTicks;
    public final float baseAdsFovMultiplier;
    public final boolean builtInScope;
    public final EntityArgonBullet.BulletKind bulletKind;
    public final int primaryColor;
    public final int accentColor;

    GunType(
        String registryName,
        String displayName,
        String caliberLabel,
        float baseDamage,
        int fireIntervalTicks,
        int baseMagazineSize,
        int baseReloadTicks,
        float baseSpread,
        float baseRecoilPitch,
        float projectileSpeed,
        int pelletsPerShot,
        boolean fullAuto,
        int spinUpTicks,
        float baseAdsFovMultiplier,
        boolean builtInScope,
        EntityArgonBullet.BulletKind bulletKind,
        int primaryColor,
        int accentColor)
    {
        this.registryName = registryName;
        this.displayName = displayName;
        this.caliberLabel = caliberLabel;
        this.baseDamage = baseDamage;
        this.fireIntervalTicks = fireIntervalTicks;
        this.baseMagazineSize = baseMagazineSize;
        this.baseReloadTicks = baseReloadTicks;
        this.baseSpread = baseSpread;
        this.baseRecoilPitch = baseRecoilPitch;
        this.projectileSpeed = projectileSpeed;
        this.pelletsPerShot = pelletsPerShot;
        this.fullAuto = fullAuto;
        this.spinUpTicks = spinUpTicks;
        this.baseAdsFovMultiplier = baseAdsFovMultiplier;
        this.builtInScope = builtInScope;
        this.bulletKind = bulletKind;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
    }

    public ItemGunAmmo getAmmoItem()
    {
        switch (this)
        {
            case PISTOL_9MM:
                return GunItems.AMMO_9MM;
            case ASSAULT_RIFLE:
            case MINIGUN:
                return GunItems.AMMO_556;
            case SHOTGUN_12G:
                return GunItems.AMMO_12G;
            case SNIPER_50:
                return GunItems.AMMO_50CAL;
            case ROCKET_LAUNCHER:
                return GunItems.AMMO_ROCKET;
            case PLASMA_RIFLE:
                return GunItems.AMMO_PLASMA_CELL;
            default:
                return GunItems.AMMO_9MM;
        }
    }

    public boolean supportsAttachments()
    {
        return this != ROCKET_LAUNCHER;
    }
}
