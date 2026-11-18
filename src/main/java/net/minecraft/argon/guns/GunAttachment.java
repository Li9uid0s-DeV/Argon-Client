package net.minecraft.argon.guns;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Modular weapon attachment types, NBT persistence, and stat modifiers.
 * Supports Scope, Muzzle, Magazine, and Underbarrel slots.
 */
public final class GunAttachment
{
    public static final int SLOT_SCOPE = 0;
    public static final int SLOT_MUZZLE = 1;
    public static final int SLOT_MAGAZINE = 2;
    public static final int SLOT_UNDERBARREL = 3;

    public static final String NBT_SCOPE = "ArgonScope";
    public static final String NBT_MUZZLE = "ArgonMuzzle";
    public static final String NBT_MAGAZINE = "ArgonMag";
    public static final String NBT_UNDERBARREL = "ArgonGrip";

    private GunAttachment()
    {
    }

    public enum ScopeType
    {
        NONE("None", 1.0F, 1.0F, false, "Iron Sights"),
        REFLEX("Reflex Sight", 0.78F, 0.85F, false, "1.3x Holographic"),
        SCOPE_4X("4x Tactical Scope", 0.42F, 0.65F, true, "4x ACOG Optics"),
        SCOPE_8X("8x Sniper Scope", 0.22F, 0.45F, true, "8x Long-Range Optics");

        public final String displayName;
        public final float adsFovMultiplier;
        public final float adsSpreadMultiplier;
        public final boolean usesFullScopeOverlay;
        public final String shortLabel;

        ScopeType(String displayName, float adsFovMultiplier, float adsSpreadMultiplier, boolean usesFullScopeOverlay, String shortLabel)
        {
            this.displayName = displayName;
            this.adsFovMultiplier = adsFovMultiplier;
            this.adsSpreadMultiplier = adsSpreadMultiplier;
            this.usesFullScopeOverlay = usesFullScopeOverlay;
            this.shortLabel = shortLabel;
        }

        public static ScopeType byIndex(int idx)
        {
            ScopeType[] vals = values();
            return idx >= 0 && idx < vals.length ? vals[idx] : NONE;
        }
    }

    public enum MuzzleType
    {
        NONE("None", 1.0F, 1.0F, 1.0F, false),
        SILENCER("Silencer", 0.94F, 0.85F, 0.30F, true),
        COMPENSATOR("Compensator", 1.05F, 0.65F, 1.15F, false);

        public final String displayName;
        public final float damageMultiplier;
        public final float recoilMultiplier;
        public final float soundVolumeMultiplier;
        public final boolean hidesMuzzleFlash;

        MuzzleType(String displayName, float damageMultiplier, float recoilMultiplier, float soundVolumeMultiplier, boolean hidesMuzzleFlash)
        {
            this.displayName = displayName;
            this.damageMultiplier = damageMultiplier;
            this.recoilMultiplier = recoilMultiplier;
            this.soundVolumeMultiplier = soundVolumeMultiplier;
            this.hidesMuzzleFlash = hidesMuzzleFlash;
        }

        public static MuzzleType byIndex(int idx)
        {
            MuzzleType[] vals = values();
            return idx >= 0 && idx < vals.length ? vals[idx] : NONE;
        }
    }

    public enum MagType
    {
        STANDARD("Standard Mag", 1.0F, 1.0F),
        EXTENDED("Extended Mag", 1.5F, 1.18F);

        public final String displayName;
        public final float capacityMultiplier;
        public final float reloadTimeMultiplier;

        MagType(String displayName, float capacityMultiplier, float reloadTimeMultiplier)
        {
            this.displayName = displayName;
            this.capacityMultiplier = capacityMultiplier;
            this.reloadTimeMultiplier = reloadTimeMultiplier;
        }

        public static MagType byIndex(int idx)
        {
            MagType[] vals = values();
            return idx >= 0 && idx < vals.length ? vals[idx] : STANDARD;
        }
    }

    public enum GripType
    {
        NONE("None", 1.0F, 1.0F, false),
        FOREGRIP("Vertical Foregrip", 0.72F, 0.82F, false),
        LASER("Tactical Laser", 0.90F, 0.58F, true);

        public final String displayName;
        public final float recoilMultiplier;
        public final float hipSpreadMultiplier;
        public final boolean emitsLaser;

        GripType(String displayName, float recoilMultiplier, float hipSpreadMultiplier, boolean emitsLaser)
        {
            this.displayName = displayName;
            this.recoilMultiplier = recoilMultiplier;
            this.hipSpreadMultiplier = hipSpreadMultiplier;
            this.emitsLaser = emitsLaser;
        }

        public static GripType byIndex(int idx)
        {
            GripType[] vals = values();
            return idx >= 0 && idx < vals.length ? vals[idx] : NONE;
        }
    }

    public static ScopeType getScope(ItemStack stack)
    {
        if (stack.isEmpty() || !stack.hasTagCompound())
        {
            return ScopeType.NONE;
        }

        return ScopeType.byIndex(stack.getTagCompound().getInteger(NBT_SCOPE));
    }

    public static MuzzleType getMuzzle(ItemStack stack)
    {
        if (stack.isEmpty() || !stack.hasTagCompound())
        {
            return MuzzleType.NONE;
        }

        return MuzzleType.byIndex(stack.getTagCompound().getInteger(NBT_MUZZLE));
    }

    public static MagType getMag(ItemStack stack)
    {
        if (stack.isEmpty() || !stack.hasTagCompound())
        {
            return MagType.STANDARD;
        }

        return MagType.byIndex(stack.getTagCompound().getInteger(NBT_MAGAZINE));
    }

    public static GripType getGrip(ItemStack stack)
    {
        if (stack.isEmpty() || !stack.hasTagCompound())
        {
            return GripType.NONE;
        }

        return GripType.byIndex(stack.getTagCompound().getInteger(NBT_UNDERBARREL));
    }

    public static void setAttachmentIndex(ItemStack stack, int slotCategory, int valueIndex)
    {
        if (stack.isEmpty())
        {
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();

        if (tag == null)
        {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }

        if (slotCategory == SLOT_SCOPE)
        {
            tag.setInteger(NBT_SCOPE, ScopeType.byIndex(valueIndex).ordinal());
        }
        else if (slotCategory == SLOT_MUZZLE)
        {
            tag.setInteger(NBT_MUZZLE, MuzzleType.byIndex(valueIndex).ordinal());
        }
        else if (slotCategory == SLOT_MAGAZINE)
        {
            tag.setInteger(NBT_MAGAZINE, MagType.byIndex(valueIndex).ordinal());
        }
        else if (slotCategory == SLOT_UNDERBARREL)
        {
            tag.setInteger(NBT_UNDERBARREL, GripType.byIndex(valueIndex).ordinal());
        }
    }

    public static int getAttachmentIndex(ItemStack stack, int slotCategory)
    {
        if (slotCategory == SLOT_SCOPE)
        {
            return getScope(stack).ordinal();
        }
        else if (slotCategory == SLOT_MUZZLE)
        {
            return getMuzzle(stack).ordinal();
        }
        else if (slotCategory == SLOT_MAGAZINE)
        {
            return getMag(stack).ordinal();
        }
        else if (slotCategory == SLOT_UNDERBARREL)
        {
            return getGrip(stack).ordinal();
        }

        return 0;
    }

    public static Item getItemForAttachment(int slotCategory, int valueIndex)
    {
        if (valueIndex <= 0)
        {
            return null;
        }

        if (slotCategory == SLOT_SCOPE)
        {
            if (valueIndex == ScopeType.REFLEX.ordinal()) return GunItems.ATTACH_SCOPE_REFLEX;
            if (valueIndex == ScopeType.SCOPE_4X.ordinal()) return GunItems.ATTACH_SCOPE_4X;
            if (valueIndex == ScopeType.SCOPE_8X.ordinal()) return GunItems.ATTACH_SCOPE_8X;
        }
        else if (slotCategory == SLOT_MUZZLE)
        {
            if (valueIndex == MuzzleType.SILENCER.ordinal()) return GunItems.ATTACH_SILENCER;
            if (valueIndex == MuzzleType.COMPENSATOR.ordinal()) return GunItems.ATTACH_COMPENSATOR;
        }
        else if (slotCategory == SLOT_MAGAZINE)
        {
            if (valueIndex == MagType.EXTENDED.ordinal()) return GunItems.ATTACH_EXTENDED_MAG;
        }
        else if (slotCategory == SLOT_UNDERBARREL)
        {
            if (valueIndex == GripType.FOREGRIP.ordinal()) return GunItems.ATTACH_FOREGRIP;
            if (valueIndex == GripType.LASER.ordinal()) return GunItems.ATTACH_LASER;
        }

        return null;
    }

    public static int getEffectiveMagazineSize(GunType type, ItemStack stack)
    {
        MagType mag = getMag(stack);
        int size = Math.round((float)type.baseMagazineSize * mag.capacityMultiplier);
        return size < 1 ? 1 : size;
    }

    public static int getEffectiveReloadTicks(GunType type, ItemStack stack)
    {
        MagType mag = getMag(stack);
        int ticks = Math.round((float)type.baseReloadTicks * mag.reloadTimeMultiplier);
        return ticks < 10 ? 10 : ticks;
    }

    public static float getEffectiveDamage(GunType type, ItemStack stack)
    {
        return type.baseDamage * getMuzzle(stack).damageMultiplier;
    }

    public static float getEffectiveRecoil(GunType type, ItemStack stack, boolean isAds)
    {
        float recoil = type.baseRecoilPitch * getMuzzle(stack).recoilMultiplier * getGrip(stack).recoilMultiplier;
        return isAds ? recoil * 0.65F : recoil;
    }

    public static float getEffectiveSpread(GunType type, ItemStack stack, boolean isAds)
    {
        float spread = type.baseSpread;

        if (isAds)
        {
            spread *= 0.35F * getScope(stack).adsSpreadMultiplier;
        }
        else
        {
            spread *= getGrip(stack).hipSpreadMultiplier;
        }

        return spread < 0.05F ? 0.05F : spread;
    }

    public static float getEffectiveAdsFovMultiplier(GunType type, ItemStack stack)
    {
        ScopeType scope = getScope(stack);
        float mult = type.baseAdsFovMultiplier;

        if (scope != ScopeType.NONE)
        {
            mult = Math.min(mult, scope.adsFovMultiplier);
        }

        return mult;
    }

    public static boolean usesFullScopeOverlay(GunType type, ItemStack stack)
    {
        ScopeType scope = getScope(stack);
        return scope.usesFullScopeOverlay || (type.builtInScope && scope == ScopeType.NONE);
    }
}
