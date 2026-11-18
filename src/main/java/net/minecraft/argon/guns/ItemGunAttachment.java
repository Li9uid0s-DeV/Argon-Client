package net.minecraft.argon.guns;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Weapon attachment module item used at the Gun Attachment Workbench.
 */
public class ItemGunAttachment extends Item
{
    private final String displayName;
    private final String slotLabel;
    private final String effectLabel;
    private final int slotCategory;
    private final int attachmentIndex;

    public ItemGunAttachment(String unlocalizedName, String displayName, String slotLabel, String effectLabel, int slotCategory, int attachmentIndex)
    {
        this.displayName = displayName;
        this.slotLabel = slotLabel;
        this.effectLabel = effectLabel;
        this.slotCategory = slotCategory;
        this.attachmentIndex = attachmentIndex;
        this.setUnlocalizedName(unlocalizedName);
        this.setMaxStackSize(16);
        this.setCreativeTab(CreativeTabs.COMBAT);
    }

    public int getSlotCategory()
    {
        return this.slotCategory;
    }

    public int getAttachmentIndex()
    {
        return this.attachmentIndex;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        return this.displayName;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add("\u00a77Slot: \u00a7b" + this.slotLabel);
        tooltip.add("\u00a7a" + this.effectLabel);
        tooltip.add("\u00a78Equip at the Argon Gun Workbench.");
    }
}
