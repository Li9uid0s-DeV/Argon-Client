package net.minecraft.argon.guns;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Ammunition item consumed when reloading Argon firearms or refilling Auto Turrets.
 */
public class ItemGunAmmo extends Item
{
    private final String displayName;
    private final String caliberInfo;
    private final int roundsPerItem;

    public ItemGunAmmo(String unlocalizedName, String displayName, String caliberInfo, int maxStackSize, int roundsPerItem)
    {
        this.displayName = displayName;
        this.caliberInfo = caliberInfo;
        this.roundsPerItem = roundsPerItem;
        this.setUnlocalizedName(unlocalizedName);
        this.setMaxStackSize(maxStackSize);
        this.setCreativeTab(CreativeTabs.COMBAT);
    }

    public int getRoundsPerItem()
    {
        return this.roundsPerItem;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        return this.displayName;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add("\u00a77Caliber: \u00a7e" + this.caliberInfo);
        tooltip.add("\u00a78Press [R] while holding matching gun to reload.");
        tooltip.add("\u00a78Right-click an Auto Turret to refill ammo.");
    }
}
