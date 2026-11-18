package net.minecraft.argon.create;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCrushingFan extends BlockKineticBase
{
    private final int kind;

    public BlockCrushingFan(int kind, String unlocalizedName)
    {
        super(Material.IRON, MapColor.STONE, unlocalizedName);
        this.kind = kind;
    }

    @Override
    public String getLocalizedName()
    {
        return this.kind == TileEntityCrushingFan.KIND_ENCASED_FAN ? "Encased Fan (Create)" : "Crushing Wheel (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityCrushingFan(this.kind);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityCrushingFan && placer != null)
        {
            if (this.kind == TileEntityCrushingFan.KIND_ENCASED_FAN)
            {
                EnumFacing dir = EnumFacing.getDirectionFromEntityLiving(pos, placer);
                ((TileEntityCrushingFan)te).setFacing(dir);
                ((TileEntityCrushingFan)te).setAxis(dir.getAxis());
            }
            else
            {
                ((TileEntityCrushingFan)te).setAxis(placer.getHorizontalFacing().rotateY().getAxis());
            }
        }
    }
}
