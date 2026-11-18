package net.minecraft.argon.create;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockTrainStation extends BlockContainer
{
    public BlockTrainStation()
    {
        super(Material.IRON, MapColor.GOLD);
        this.setHardness(3.0F);
        this.setSoundType(SoundType.METAL);
        this.setUnlocalizedName("createTrainStation");
        this.setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    @Override
    public String getLocalizedName()
    {
        return "Train Station (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityTrainStation();
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

        if (te instanceof TileEntityTrainStation && placer != null)
        {
            ((TileEntityTrainStation)te).setFacing(placer.getHorizontalFacing().getOpposite());
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (hand != EnumHand.MAIN_HAND)
        {
            return true;
        }

        ItemStack held = playerIn.getHeldItem(hand);

        if (!held.isEmpty() && held.getItem() == CreateItems.WRENCH && playerIn.isSneaking())
        {
            worldIn.destroyBlock(pos, !playerIn.capabilities.isCreativeMode);
            return true;
        }

        if (worldIn.isRemote)
        {
            TileEntity te = worldIn.getTileEntity(pos);

            if (te instanceof TileEntityTrainStation)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiTrainStation((TileEntityTrainStation)te));
            }
        }

        return true;
    }
}
