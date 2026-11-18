package net.minecraft.argon.guns;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/**
 * 3D Firearm Item supporting modular attachments, magazines, reload timers,
 * ADS spread/recoil reduction, full-auto/semi-auto fire, and Techguns heavy weapons.
 */
public class ItemArgonGun extends Item
{
    public static final String NBT_AMMO = "ArgonGunAmmo";
    public static final String NBT_RELOAD_TICKS = "ArgonGunReloadTicks";
    public static final String NBT_RELOAD_MAX = "ArgonGunReloadMax";
    public static final String NBT_COOLDOWN = "ArgonGunCooldown";
    public static final String NBT_INITIALIZED = "ArgonGunInit";

    private final GunType gunType;

    public ItemArgonGun(GunType gunType)
    {
        this.gunType = gunType;
        this.setUnlocalizedName("argon_" + gunType.registryName);
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.COMBAT);
    }

    public GunType getGunType()
    {
        return this.gunType;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        return this.gunType.displayName;
    }

    public static NBTTagCompound getOrInitTag(ItemStack stack, GunType type)
    {
        NBTTagCompound tag = stack.getTagCompound();

        if (tag == null)
        {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }

        if (!tag.getBoolean(NBT_INITIALIZED))
        {
            tag.setBoolean(NBT_INITIALIZED, true);
            tag.setInteger(NBT_AMMO, GunAttachment.getEffectiveMagazineSize(type, stack));
            tag.setInteger(NBT_RELOAD_TICKS, 0);
            tag.setInteger(NBT_RELOAD_MAX, 0);
            tag.setInteger(NBT_COOLDOWN, 0);
        }

        return tag;
    }

    public int getAmmoInMag(ItemStack stack)
    {
        return getOrInitTag(stack, this.gunType).getInteger(NBT_AMMO);
    }

    public int getMaxMagazine(ItemStack stack)
    {
        return GunAttachment.getEffectiveMagazineSize(this.gunType, stack);
    }

    public int getReloadTicks(ItemStack stack)
    {
        return getOrInitTag(stack, this.gunType).getInteger(NBT_RELOAD_TICKS);
    }

    public int getReloadMax(ItemStack stack)
    {
        return getOrInitTag(stack, this.gunType).getInteger(NBT_RELOAD_MAX);
    }

    public boolean isReloading(ItemStack stack)
    {
        return this.getReloadTicks(stack) > 0;
    }

    public int getCooldown(ItemStack stack)
    {
        return getOrInitTag(stack, this.gunType).getInteger(NBT_COOLDOWN);
    }

    public int countReserveAmmo(EntityPlayer player)
    {
        if (player == null)
        {
            return 0;
        }

        if (player.capabilities.isCreativeMode)
        {
            return 999;
        }

        ItemGunAmmo ammoItem = this.gunType.getAmmoItem();
        int total = 0;

        for (int i = 0; i < player.inventory.getSizeInventory(); ++i)
        {
            ItemStack invStack = player.inventory.getStackInSlot(i);

            if (!invStack.isEmpty() && invStack.getItem() == ammoItem)
            {
                total += invStack.getCount() * ammoItem.getRoundsPerItem();
            }
        }

        return total;
    }

    /**
     * Right-click does not consume the gun; ADS is handled smoothly by GunClientState.
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        return new ActionResult<ItemStack>(EnumActionResult.PASS, playerIn.getHeldItem(handIn));
    }

    /**
     * Server-side firing invoked via GunNetworkHandler when player pulls trigger.
     */
    public boolean fireServer(World world, EntityPlayer player, ItemStack stack, boolean isAds)
    {
        if (world.isRemote || player == null || stack.isEmpty())
        {
            return false;
        }

        NBTTagCompound tag = getOrInitTag(stack, this.gunType);

        if (tag.getInteger(NBT_RELOAD_TICKS) > 0 || tag.getInteger(NBT_COOLDOWN) > 0)
        {
            return false;
        }

        int ammo = tag.getInteger(NBT_AMMO);

        if (ammo <= 0)
        {
            world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.5F, 1.8F);
            this.startReloadServer(world, player, stack);
            return false;
        }

        tag.setInteger(NBT_AMMO, ammo - 1);
        tag.setInteger(NBT_COOLDOWN, this.gunType.fireIntervalTicks);

        float damage = GunAttachment.getEffectiveDamage(this.gunType, stack);
        float spread = GunAttachment.getEffectiveSpread(this.gunType, stack, isAds);
        int pellets = this.gunType.pelletsPerShot;

        for (int p = 0; p < pellets; ++p)
        {
            EntityArgonBullet bullet = new EntityArgonBullet(world, player, this.gunType.bulletKind, damage);
            bullet.setHeadingFromThrower(player, player.rotationPitch, player.rotationYaw, 0.0F, this.gunType.projectileSpeed, spread);
            world.spawnEntity(bullet);
        }

        GunAttachment.MuzzleType muzzle = GunAttachment.getMuzzle(stack);
        SoundEvent shotSound = this.getFireSound();
        float volume = (this.gunType == GunType.ROCKET_LAUNCHER ? 1.8F : 1.15F) * muzzle.soundVolumeMultiplier;
        float pitch = this.getFirePitch(muzzle == GunAttachment.MuzzleType.SILENCER);
        world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, shotSound, SoundCategory.PLAYERS, volume, pitch);

        if (ammo - 1 <= 0)
        {
            this.startReloadServer(world, player, stack);
        }

        return true;
    }

    private SoundEvent getFireSound()
    {
        switch (this.gunType)
        {
            case ROCKET_LAUNCHER:
                return SoundEvents.ENTITY_BLAZE_SHOOT;
            case PLASMA_RIFLE:
                return SoundEvents.ENTITY_WITHER_SHOOT;
            case SNIPER_50:
            case SHOTGUN_12G:
                return SoundEvents.ENTITY_FIREWORK_BLAST;
            case MINIGUN:
                return SoundEvents.BLOCK_DISPENSER_LAUNCH;
            default:
                return SoundEvents.ENTITY_FIREWORK_BLAST_FAR;
        }
    }

    private float getFirePitch(boolean silenced)
    {
        if (silenced)
        {
            return 1.85F;
        }

        switch (this.gunType)
        {
            case PISTOL_9MM:
                return 1.45F;
            case ASSAULT_RIFLE:
                return 1.20F;
            case SHOTGUN_12G:
                return 0.72F;
            case SNIPER_50:
                return 0.62F;
            case MINIGUN:
                return 1.55F;
            case ROCKET_LAUNCHER:
                return 0.65F;
            case PLASMA_RIFLE:
                return 1.75F;
            default:
                return 1.0F;
        }
    }

    /**
     * Initiates magazine reload if below max capacity and reserve ammo exists.
     */
    public boolean startReloadServer(World world, EntityPlayer player, ItemStack stack)
    {
        if (world.isRemote || player == null || stack.isEmpty())
        {
            return false;
        }

        NBTTagCompound tag = getOrInitTag(stack, this.gunType);

        if (tag.getInteger(NBT_RELOAD_TICKS) > 0)
        {
            return false;
        }

        int maxMag = this.getMaxMagazine(stack);
        int current = tag.getInteger(NBT_AMMO);

        if (current >= maxMag)
        {
            return false;
        }

        if (!player.capabilities.isCreativeMode && this.countReserveAmmo(player) <= 0)
        {
            world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.5F, 1.9F);
            return false;
        }

        int reloadTicks = GunAttachment.getEffectiveReloadTicks(this.gunType, stack);
        tag.setInteger(NBT_RELOAD_TICKS, reloadTicks);
        tag.setInteger(NBT_RELOAD_MAX, reloadTicks);
        world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 0.55F, 1.45F);
        return true;
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
    {
        if (worldIn.isRemote || !(entityIn instanceof EntityPlayer))
        {
            return;
        }

        EntityPlayer player = (EntityPlayer)entityIn;
        NBTTagCompound tag = getOrInitTag(stack, this.gunType);

        int cooldown = tag.getInteger(NBT_COOLDOWN);

        if (cooldown > 0)
        {
            tag.setInteger(NBT_COOLDOWN, cooldown - 1);
        }

        int reload = tag.getInteger(NBT_RELOAD_TICKS);

        if (reload > 0)
        {
            if (!isSelected)
            {
                // Switching away cancels in-progress reload
                tag.setInteger(NBT_RELOAD_TICKS, 0);
                tag.setInteger(NBT_RELOAD_MAX, 0);
                return;
            }

            reload--;
            tag.setInteger(NBT_RELOAD_TICKS, reload);

            if (reload <= 0)
            {
                this.completeReload(worldIn, player, stack, tag);
            }
        }
    }

    private void completeReload(World world, EntityPlayer player, ItemStack stack, NBTTagCompound tag)
    {
        int maxMag = this.getMaxMagazine(stack);
        int current = tag.getInteger(NBT_AMMO);
        int needed = maxMag - current;

        if (needed <= 0)
        {
            tag.setInteger(NBT_RELOAD_MAX, 0);
            return;
        }

        if (player.capabilities.isCreativeMode)
        {
            tag.setInteger(NBT_AMMO, maxMag);
        }
        else
        {
            ItemGunAmmo ammoItem = this.gunType.getAmmoItem();
            int roundsPerItem = Math.max(1, ammoItem.getRoundsPerItem());
            int itemsNeeded = (needed + roundsPerItem - 1) / roundsPerItem;
            int consumedRounds = 0;

            for (int i = 0; i < player.inventory.getSizeInventory() && itemsNeeded > 0; ++i)
            {
                ItemStack invStack = player.inventory.getStackInSlot(i);

                if (!invStack.isEmpty() && invStack.getItem() == ammoItem)
                {
                    int take = Math.min(itemsNeeded, invStack.getCount());
                    invStack.shrink(take);
                    itemsNeeded -= take;
                    consumedRounds += take * roundsPerItem;
                }
            }

            int loaded = Math.min(maxMag, current + consumedRounds);
            tag.setInteger(NBT_AMMO, loaded);
        }

        tag.setInteger(NBT_RELOAD_MAX, 0);
        world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.7F, 1.35F);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        int ammo = this.getAmmoInMag(stack);
        int maxMag = this.getMaxMagazine(stack);
        float dmg = GunAttachment.getEffectiveDamage(this.gunType, stack);

        tooltip.add("\u00a77Ammo: \u00a7f" + ammo + " / " + maxMag + " \u00a78(" + this.gunType.caliberLabel + ")");

        if (this.gunType.pelletsPerShot > 1)
        {
            tooltip.add("\u00a77Damage: \u00a7c" + String.format("%.1f", dmg) + " x" + this.gunType.pelletsPerShot);
        }
        else
        {
            tooltip.add("\u00a77Damage: \u00a7c" + String.format("%.1f", dmg));
        }

        tooltip.add("\u00a77Mode: \u00a7e" + (this.gunType.fullAuto ? "Full-Auto" : "Semi-Auto"));

        GunAttachment.ScopeType scope = GunAttachment.getScope(stack);
        GunAttachment.MuzzleType muzzle = GunAttachment.getMuzzle(stack);
        GunAttachment.MagType mag = GunAttachment.getMag(stack);
        GunAttachment.GripType grip = GunAttachment.getGrip(stack);

        if (scope != GunAttachment.ScopeType.NONE || muzzle != GunAttachment.MuzzleType.NONE || mag != GunAttachment.MagType.STANDARD || grip != GunAttachment.GripType.NONE)
        {
            tooltip.add("\u00a7bAttachments:");
            if (scope != GunAttachment.ScopeType.NONE) tooltip.add(" \u00a77- Scope: \u00a7f" + scope.displayName);
            if (muzzle != GunAttachment.MuzzleType.NONE) tooltip.add(" \u00a77- Muzzle: \u00a7f" + muzzle.displayName);
            if (mag != GunAttachment.MagType.STANDARD) tooltip.add(" \u00a77- Mag: \u00a7f" + mag.displayName);
            if (grip != GunAttachment.GripType.NONE) tooltip.add(" \u00a77- Underbarrel: \u00a7f" + grip.displayName);
        }

        tooltip.add("\u00a78[LMB] Fire | [RMB] Aim (ADS) | [R] Reload");
    }
}
