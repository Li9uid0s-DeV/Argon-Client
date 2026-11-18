package net.minecraft.argon.create;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockBasin extends BlockContainer
{
    public BlockBasin()
    {
        super(Material.IRON, MapColor.GRAY);
        this.setHardness(2.0F);
        this.setSoundType(SoundType.METAL);
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setUnlocalizedName("createBasin");
    }

    @Override
    public String getLocalizedName()
    {
        return "Basin (Create)";
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
        return new TileEntityBasin();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityBasin)
        {
            TileEntityBasin basin = (TileEntityBasin)te;

            for (int i = 0; i < basin.getItems().size(); ++i)
            {
                ItemStack s = basin.getItems().get(i);

                if (!s.isEmpty())
                {
                    spawnAsEntity(worldIn, pos, s);
                }
            }
        }

        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (hand != EnumHand.MAIN_HAND)
        {
            return true;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        return te instanceof TileEntityBasin && ((TileEntityBasin)te).onPlayerInteract(playerIn, hand);
    }
}
