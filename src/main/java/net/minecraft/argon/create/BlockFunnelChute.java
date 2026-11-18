package net.minecraft.argon.create;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockFunnelChute extends BlockContainer
{
    private final int kind;

    public BlockFunnelChute(int kind, String unlocalizedName)
    {
        super(Material.IRON, MapColor.IRON);
        this.kind = kind;
        this.setHardness(1.6F);
        this.setSoundType(SoundType.METAL);
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setUnlocalizedName(unlocalizedName);
    }

    @Override
    public String getLocalizedName()
    {
        return this.kind == TileEntityFunnelChute.KIND_CHUTE ? "Chute (Create)" : "Andesite Funnel (Create)";
    }

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state)
    {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityFunnelChute(this.kind);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityFunnelChute && placer != null)
        {
            ((TileEntityFunnelChute)te).setFacing(placer.getHorizontalFacing().getOpposite());
        }
    }
}
