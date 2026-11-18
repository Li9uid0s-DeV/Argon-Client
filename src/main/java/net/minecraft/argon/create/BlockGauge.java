package net.minecraft.argon.create;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockGauge extends BlockKineticBase
{
    private final int kind;

    public BlockGauge(int kind, String unlocalizedName)
    {
        super(Material.IRON, MapColor.IRON, unlocalizedName);
        this.kind = kind;
    }

    @Override
    public String getLocalizedName()
    {
        return this.kind == TileEntityGauge.KIND_STRESSOMETER ? "Stressometer (Create)" : "Speedometer (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityGauge(this.kind);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state)
    {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        return te instanceof TileEntityGauge ? ((TileEntityGauge)te).getComparatorOutput() : 0;
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

            if (te instanceof TileEntityGauge)
            {
                TileEntityGauge gauge = (TileEntityGauge)te;

                if (this.kind == TileEntityGauge.KIND_STRESSOMETER)
                {
                    float cap = gauge.getNetworkCapacitySU();
                    float used = gauge.getNetworkStressSU();
                    float pct = cap > 0.0F ? (used / cap) * 100.0F : 0.0F;
                    playerIn.sendMessage(new TextComponentString("\u00a76[Stressometer] \u00a7fLoad: \u00a7e" + String.format("%.0f%%", pct) + " \u00a77(" + MathHelper.ceil(used) + " / " + MathHelper.ceil(cap) + " SU)" + (gauge.isOverstressed() ? " \u00a7c[OVERSTRESSED]" : "")));
                }
                else
                {
                    playerIn.sendMessage(new TextComponentString("\u00a7b[Speedometer] \u00a7fSpeed: \u00a7a" + String.format("%.1f RPM", Math.abs(gauge.getSpeedRPM()))));
                }
            }
        }

        return true;
    }
}
