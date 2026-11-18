package net.minecraft.argon.create;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class ItemSuperGlue extends Item
{
    public ItemSuperGlue()
    {
        this.setMaxStackSize(1);
        this.setMaxDamage(128);
        this.setCreativeTab(CreativeTabs.TOOLS);
        this.setUnlocalizedName("createSuperGlue");
    }

    @Override
    public String getItemStackDisplayName(net.minecraft.item.ItemStack stack)
    {
        return "Super Glue (Create)";
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking())
        {
            int removed = SuperGlueManager.removeGlueAt(worldIn, pos);
            SuperGlueManager.setPendingCorner(player, null);

            if (worldIn.isRemote)
            {
                player.sendMessage(new TextComponentString("\u00a7a[Super Glue] \u00a77Removed " + removed + " glue region(s) at target."));
            }

            return EnumActionResult.SUCCESS;
        }

        BlockPos first = SuperGlueManager.getPendingCorner(player);

        if (first == null)
        {
            SuperGlueManager.setPendingCorner(player, pos);

            if (worldIn.isRemote)
            {
                player.sendMessage(new TextComponentString("\u00a7a[Super Glue] \u00a7fFirst corner set at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "). Click second corner to bond."));
            }

            return EnumActionResult.SUCCESS;
        }

        int dx = Math.abs(pos.getX() - first.getX()) + 1;
        int dy = Math.abs(pos.getY() - first.getY()) + 1;
        int dz = Math.abs(pos.getZ() - first.getZ()) + 1;
        int volume = dx * dy * dz;

        if (volume > 512)
        {
            SuperGlueManager.setPendingCorner(player, null);

            if (worldIn.isRemote)
            {
                player.sendMessage(new TextComponentString("\u00a7c[Super Glue] \u00a77Selection too large (" + volume + " blocks, max 512)."));
            }

            return EnumActionResult.FAIL;
        }

        SuperGlueManager.addGlueBox(worldIn, first, pos);
        SuperGlueManager.setPendingCorner(player, null);
        worldIn.playSound(player, pos, SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.BLOCKS, 0.7F, 1.2F);

        if (worldIn.isRemote)
        {
            player.sendMessage(new TextComponentString("\u00a7a[Super Glue] \u00a7fGlued " + dx + "x" + dy + "x" + dz + " (" + volume + " blocks)!"));
        }

        return EnumActionResult.SUCCESS;
    }
}
