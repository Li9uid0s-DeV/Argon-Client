package net.minecraft.argon.create;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockWindmillBearing extends BlockKineticBase
{
    public BlockWindmillBearing()
    {
        super(Material.ROCK, MapColor.STONE, "createWindmillBearing");
    }

    @Override
    public String getLocalizedName()
    {
        return "Windmill Bearing (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityWindmillBearing();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ))
        {
            return true;
        }

        if (hand == EnumHand.MAIN_HAND && worldIn.isRemote)
        {
            TileEntity te = worldIn.getTileEntity(pos);

            if (te instanceof TileEntityWindmillBearing)
            {
                TileEntityWindmillBearing wb = (TileEntityWindmillBearing)te;
                playerIn.sendMessage(new TextComponentString("\u00a7b[Windmill] \u00a7fOutput: \u00a7a" + (int)wb.getGeneratedCapacitySU() + " SU \u00a77@ \u00a7e" + String.format("%.1f", Math.abs(wb.getGeneratedSpeedRPM())) + " RPM"));
            }
        }

        return true;
    }
}
