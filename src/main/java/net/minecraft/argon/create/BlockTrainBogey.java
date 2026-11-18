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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockTrainBogey extends BlockContainer
{
    public BlockTrainBogey()
    {
        super(Material.IRON, MapColor.IRON);
        this.setHardness(3.0F);
        this.setSoundType(SoundType.METAL);
        this.setUnlocalizedName("createTrainBogey");
        this.setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    @Override
    public String getLocalizedName()
    {
        return "Train Bogey (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityTrainDecor(TileEntityTrainDecor.KIND_BOGEY);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.INVISIBLE;
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
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityTrainDecor && placer != null)
        {
            ((TileEntityTrainDecor)te).setFacing(placer.getHorizontalFacing());
        }
    }
}
