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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockBelt extends BlockKineticBase
{
    private static final AxisAlignedBB BELT_AABB = new AxisAlignedBB(0.0D, 0.125D, 0.0D, 1.0D, 0.5625D, 1.0D);

    public BlockBelt()
    {
        super(Material.CLOTH, MapColor.BLACK, "createBelt");
    }

    @Override
    public String getLocalizedName()
    {
        return "Mechanical Belt (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityBelt();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return BELT_AABB;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityBelt && placer != null)
        {
            EnumFacing moveDir = placer.getHorizontalFacing();
            ((TileEntityBelt)te).setFacing(moveDir);
            ((TileEntityBelt)te).setAxis(moveDir.rotateY().getAxis());
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ))
        {
            return true;
        }

        ItemStack held = playerIn.getHeldItem(hand);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityBelt && !held.isEmpty())
        {
            TileEntityBelt belt = (TileEntityBelt)te;

            if (belt.getStoredStack().isEmpty())
            {
                ItemStack single = held.copy();
                single.setCount(1);

                if (belt.tryInsertItem(single) && !playerIn.capabilities.isCreativeMode)
                {
                    held.shrink(1);
                }

                return true;
            }
        }

        return false;
    }
}
