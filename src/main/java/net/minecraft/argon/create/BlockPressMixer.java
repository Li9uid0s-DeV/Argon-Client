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

public class BlockPressMixer extends BlockKineticBase
{
    private final int kind;

    public BlockPressMixer(int kind, String unlocalizedName)
    {
        super(Material.IRON, MapColor.IRON, unlocalizedName);
        this.kind = kind;
    }

    @Override
    public String getLocalizedName()
    {
        return this.kind == TileEntityPressMixer.KIND_MIXER ? "Mechanical Mixer (Create)" : "Mechanical Press (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityPressMixer(this.kind);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityPressMixer && placer != null)
        {
            if (this.kind == TileEntityPressMixer.KIND_MIXER)
            {
                ((TileEntityPressMixer)te).setAxis(EnumFacing.Axis.Y);
            }
            else
            {
                ((TileEntityPressMixer)te).setAxis(placer.getHorizontalFacing().rotateY().getAxis());
            }
        }
    }
}
