package net.minecraft.argon.tnt;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * ItemBlock for BlockArgonTNT displaying clear localized names and category/radius tooltips.
 */
public class ItemBlockArgonTnt extends ItemBlock
{
    private final TntType tntType;

    public ItemBlockArgonTnt(BlockArgonTNT block)
    {
        super(block);
        this.tntType = block.getTntType();
        this.setUnlocalizedName(this.tntType.registryName);
        this.setCreativeTab(TntCreativeTab.TAB_ARGON_TNT);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        return this.tntType.displayName;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add(this.tntType.category.colorCode + "[" + this.tntType.category.label + "] \u00a77Radius: \u00a7f" + this.tntType.radius);
        tooltip.add("\u00a77" + this.tntType.description);
        tooltip.add("\u00a78Queued via ArgonExplosionScheduler (500 blk / 20 ent per tick)");
    }
}
