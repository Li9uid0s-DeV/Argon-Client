package net.minecraft.argon.create;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockKineticPart extends BlockKineticBase
{
    private static final AxisAlignedBB SHAFT_X_AABB = new AxisAlignedBB(0.0D, 0.3125D, 0.3125D, 1.0D, 0.6875D, 0.6875D);
    private static final AxisAlignedBB SHAFT_Y_AABB = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
    private static final AxisAlignedBB SHAFT_Z_AABB = new AxisAlignedBB(0.3125D, 0.3125D, 0.0D, 0.6875D, 0.6875D, 1.0D);
    private static final AxisAlignedBB COG_AABB = new AxisAlignedBB(0.0625D, 0.0625D, 0.0625D, 0.9375D, 0.9375D, 0.9375D);

    private final int kind;
    private final String localizedTitle;

    public BlockKineticPart(int kind, String unlocalizedName, String localizedTitle)
    {
        super(kind == TileEntityKineticBasic.TYPE_GEARBOX ? Material.WOOD : Material.ROCK, MapColor.STONE, unlocalizedName);
        this.kind = kind;
        this.localizedTitle = localizedTitle;
    }

    @Override
    public String getLocalizedName()
    {
        return this.localizedTitle;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityKineticBasic(this.kind);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityKineticBasic && placer != null)
        {
            if (this.kind == TileEntityKineticBasic.TYPE_GEARBOX)
            {
                ((TileEntityKineticBasic)te).setAxis(placer.isSneaking() ? placer.getHorizontalFacing().getAxis() : EnumFacing.Axis.Y);
            }
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        if (this.kind == TileEntityKineticBasic.TYPE_SHAFT || this.kind == TileEntityKineticBasic.TYPE_GANTRY_SHAFT)
        {
            TileEntity te = source.getTileEntity(pos);

            if (te instanceof TileEntityCreateBase)
            {
                EnumFacing.Axis ax = ((TileEntityCreateBase)te).getAxis();

                if (ax == EnumFacing.Axis.X)
                {
                    return SHAFT_X_AABB;
                }

                if (ax == EnumFacing.Axis.Z)
                {
                    return SHAFT_Z_AABB;
                }

                return SHAFT_Y_AABB;
            }
        }
        else if (this.kind == TileEntityKineticBasic.TYPE_COGWHEEL_SMALL || this.kind == TileEntityKineticBasic.TYPE_COGWHEEL_LARGE)
        {
            return COG_AABB;
        }

        return FULL_BLOCK_AABB;
    }
}
