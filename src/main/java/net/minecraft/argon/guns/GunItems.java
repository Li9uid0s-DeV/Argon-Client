package net.minecraft.argon.guns;

import net.minecraft.item.Item;

/**
 * Central registration for Argon Firearms, Ammunition, Attachment Modules,
 * and Gun BlockItems (Item IDs 560..580).
 */
public final class GunItems
{
    // Firearms (560..566)
    public static ItemArgonGun PISTOL_9MM;
    public static ItemArgonGun ASSAULT_RIFLE;
    public static ItemArgonGun SHOTGUN_12G;
    public static ItemArgonGun SNIPER_50;
    public static ItemArgonGun MINIGUN;
    public static ItemArgonGun ROCKET_LAUNCHER;
    public static ItemArgonGun PLASMA_RIFLE;

    // Ammunition (567..572)
    public static ItemGunAmmo AMMO_9MM;
    public static ItemGunAmmo AMMO_556;
    public static ItemGunAmmo AMMO_12G;
    public static ItemGunAmmo AMMO_50CAL;
    public static ItemGunAmmo AMMO_ROCKET;
    public static ItemGunAmmo AMMO_PLASMA_CELL;

    // Attachments (573..580)
    public static ItemGunAttachment ATTACH_SCOPE_REFLEX;
    public static ItemGunAttachment ATTACH_SCOPE_4X;
    public static ItemGunAttachment ATTACH_SCOPE_8X;
    public static ItemGunAttachment ATTACH_SILENCER;
    public static ItemGunAttachment ATTACH_COMPENSATOR;
    public static ItemGunAttachment ATTACH_EXTENDED_MAG;
    public static ItemGunAttachment ATTACH_FOREGRIP;
    public static ItemGunAttachment ATTACH_LASER;

    private GunItems()
    {
    }

    public static void registerItemsAndBlocks()
    {
        for (int i = 0; i < GunBlocks.ALL_BLOCKS.length; ++i)
        {
            Item.registerItemBlock(GunBlocks.ALL_BLOCKS[i]);
        }

        PISTOL_9MM = new ItemArgonGun(GunType.PISTOL_9MM);
        ASSAULT_RIFLE = new ItemArgonGun(GunType.ASSAULT_RIFLE);
        SHOTGUN_12G = new ItemArgonGun(GunType.SHOTGUN_12G);
        SNIPER_50 = new ItemArgonGun(GunType.SNIPER_50);
        MINIGUN = new ItemArgonGun(GunType.MINIGUN);
        ROCKET_LAUNCHER = new ItemArgonGun(GunType.ROCKET_LAUNCHER);
        PLASMA_RIFLE = new ItemArgonGun(GunType.PLASMA_RIFLE);

        Item.registerItem(560, "argon_pistol_9mm", PISTOL_9MM);
        Item.registerItem(561, "argon_assault_rifle", ASSAULT_RIFLE);
        Item.registerItem(562, "argon_shotgun_12g", SHOTGUN_12G);
        Item.registerItem(563, "argon_sniper_50", SNIPER_50);
        Item.registerItem(564, "argon_minigun", MINIGUN);
        Item.registerItem(565, "argon_rocket_launcher", ROCKET_LAUNCHER);
        Item.registerItem(566, "argon_plasma_rifle", PLASMA_RIFLE);

        AMMO_9MM = new ItemGunAmmo("argon_ammo_9mm", "9mm Ammo Pack", "9mm Parabellum", 64, 15);
        AMMO_556 = new ItemGunAmmo("argon_ammo_556", "5.56mm Rifle Ammo", "5.56mm NATO", 64, 30);
        AMMO_12G = new ItemGunAmmo("argon_ammo_12g", "12-Gauge Shells", "12G Buckshot", 64, 8);
        AMMO_50CAL = new ItemGunAmmo("argon_ammo_50cal", ".50 Cal Sniper Rounds", ".50 BMG", 64, 5);
        AMMO_ROCKET = new ItemGunAmmo("argon_ammo_rocket", "84mm HE Rocket", "84mm High-Explosive", 16, 1);
        AMMO_PLASMA_CELL = new ItemGunAmmo("argon_ammo_plasma", "Plasma Energy Cell", "High-Density Plasma", 32, 24);

        Item.registerItem(567, "argon_ammo_9mm", AMMO_9MM);
        Item.registerItem(568, "argon_ammo_556", AMMO_556);
        Item.registerItem(569, "argon_ammo_12g", AMMO_12G);
        Item.registerItem(570, "argon_ammo_50cal", AMMO_50CAL);
        Item.registerItem(571, "argon_ammo_rocket", AMMO_ROCKET);
        Item.registerItem(572, "argon_ammo_plasma", AMMO_PLASMA_CELL);

        ATTACH_SCOPE_REFLEX = new ItemGunAttachment("argon_scope_reflex", "Reflex Sight", "Scope", "1.3x Holographic Sight, -15% ADS Spread", GunAttachment.SLOT_SCOPE, GunAttachment.ScopeType.REFLEX.ordinal());
        ATTACH_SCOPE_4X = new ItemGunAttachment("argon_scope_4x", "4x Tactical Scope", "Scope", "4x Optical Zoom, -35% ADS Spread", GunAttachment.SLOT_SCOPE, GunAttachment.ScopeType.SCOPE_4X.ordinal());
        ATTACH_SCOPE_8X = new ItemGunAttachment("argon_scope_8x", "8x Sniper Scope", "Scope", "8x Long-Range Zoom, -55% ADS Spread", GunAttachment.SLOT_SCOPE, GunAttachment.ScopeType.SCOPE_8X.ordinal());
        ATTACH_SILENCER = new ItemGunAttachment("argon_silencer", "Tactical Silencer", "Muzzle", "-70% Gunshot Noise, Hides Muzzle Flash, -15% Recoil", GunAttachment.SLOT_MUZZLE, GunAttachment.MuzzleType.SILENCER.ordinal());
        ATTACH_COMPENSATOR = new ItemGunAttachment("argon_compensator", "Muzzle Compensator", "Muzzle", "-35% Recoil, +5% Damage", GunAttachment.SLOT_MUZZLE, GunAttachment.MuzzleType.COMPENSATOR.ordinal());
        ATTACH_EXTENDED_MAG = new ItemGunAttachment("argon_extended_mag", "Extended Magazine", "Magazine", "+50% Magazine Capacity", GunAttachment.SLOT_MAGAZINE, GunAttachment.MagType.EXTENDED.ordinal());
        ATTACH_FOREGRIP = new ItemGunAttachment("argon_foregrip", "Vertical Foregrip", "Underbarrel", "-28% Recoil, -18% Hip Spread", GunAttachment.SLOT_UNDERBARREL, GunAttachment.GripType.FOREGRIP.ordinal());
        ATTACH_LASER = new ItemGunAttachment("argon_laser", "Tactical Laser Sight", "Underbarrel", "-42% Hip Spread, Projects Laser Beam", GunAttachment.SLOT_UNDERBARREL, GunAttachment.GripType.LASER.ordinal());

        Item.registerItem(573, "argon_scope_reflex", ATTACH_SCOPE_REFLEX);
        Item.registerItem(574, "argon_scope_4x", ATTACH_SCOPE_4X);
        Item.registerItem(575, "argon_scope_8x", ATTACH_SCOPE_8X);
        Item.registerItem(576, "argon_silencer", ATTACH_SILENCER);
        Item.registerItem(577, "argon_compensator", ATTACH_COMPENSATOR);
        Item.registerItem(578, "argon_extended_mag", ATTACH_EXTENDED_MAG);
        Item.registerItem(579, "argon_foregrip", ATTACH_FOREGRIP);
        Item.registerItem(580, "argon_laser", ATTACH_LASER);
    }
}
