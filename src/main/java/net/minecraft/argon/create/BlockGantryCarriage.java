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

public class BlockGantryCarriage extends BlockKineticBase
{
    public BlockGantryCarriage()
    {
        super(Material.IRON, MapColor.STONE, "createGantryCarriage");
    }

    @Override
    public String getLocalizedName()
    {
        return "Gantry Carriage (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityGantryCarriage();
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityGantryCarriage && placer != null)
        {
            ((TileEntityGantryCarriage)te).setFacing(EnumFacing.getDirectionFromEntityLiving(pos, placer));
        }
    }
}
