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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockFluidNode extends BlockKineticBase
{
    private final int kind;

    public BlockFluidNode(int kind, String unlocalizedName)
    {
        super(Material.IRON, MapColor.GOLD, unlocalizedName);
        this.kind = kind;
    }

    @Override
    public String getLocalizedName()
    {
        if (this.kind == TileEntityFluidNode.KIND_PUMP) return "Mechanical Pump (Create)";
        if (this.kind == TileEntityFluidNode.KIND_TANK) return "Fluid Tank (Create)";
        return "Fluid Pipe (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityFluidNode(this.kind);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityFluidNode && placer != null && this.kind == TileEntityFluidNode.KIND_PUMP)
        {
            EnumFacing pushDir = EnumFacing.getDirectionFromEntityLiving(pos, placer);
            ((TileEntityFluidNode)te).setFacing(pushDir);
            ((TileEntityFluidNode)te).setAxis(pushDir.rotateY().getAxis());
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ))
        {
            return true;
        }

        if (hand != EnumHand.MAIN_HAND)
        {
            return true;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        return te instanceof TileEntityFluidNode && ((TileEntityFluidNode)te).onPlayerInteract(playerIn, hand);
    }
}
