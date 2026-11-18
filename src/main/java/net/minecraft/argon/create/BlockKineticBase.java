package net.minecraft.argon.create;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class BlockKineticBase extends BlockContainer
{
    protected BlockKineticBase(Material materialIn, MapColor color, String unlocalizedName)
    {
        super(materialIn, color);
        this.setHardness(1.8F);
        this.setResistance(6.0F);
        this.setSoundType(SoundType.WOOD);
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setUnlocalizedName(unlocalizedName);
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
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityCreateBase)
        {
            TileEntityCreateBase cte = (TileEntityCreateBase)te;
            EnumFacing lookFacing = EnumFacing.getDirectionFromEntityLiving(pos, placer);
            cte.setFacing(lookFacing);
            cte.setAxis(lookFacing.getAxis());
            cte.markDirty();
        }

        KineticNetwork.scheduleUpdate(worldIn, pos);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        super.breakBlock(worldIn, pos, state);
        KineticNetwork.scheduleUpdate(worldIn, pos);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        ItemStack held = playerIn.getHeldItem(hand);

        if (!held.isEmpty() && held.getItem() instanceof ItemWrench)
        {
            TileEntity te = worldIn.getTileEntity(pos);

            if (te instanceof TileEntityCreateBase)
            {
                if (playerIn.isSneaking())
                {
                    if (!worldIn.isRemote)
                    {
                        this.dropBlockAsItem(worldIn, pos, state, 0);
                        worldIn.setBlockToAir(pos);
                    }
                    else
                    {
                        this.dropBlockAsItem(worldIn, pos, state, 0);
                        worldIn.setBlockToAir(pos);
                    }

                    return true;
                }

                return ((TileEntityCreateBase)te).onWrenchUse(playerIn, facing);
            }
        }

        return false;
    }

    protected static float getBlockLight(IBlockAccess world, BlockPos pos)
    {
        int packed = world.getCombinedLight(pos, 0);
        int blockL = (packed >> 4) & 15;
        int skyL = (packed >> 20) & 15;
        float level = Math.max(blockL, skyL) / 15.0F;
        return 0.35F + level * 0.65F;
    }
}
