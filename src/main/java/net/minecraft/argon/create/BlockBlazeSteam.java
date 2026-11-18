package net.minecraft.argon.create;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockBlazeSteam extends BlockKineticBase
{
    private final int kind;

    public BlockBlazeSteam(int kind, String unlocalizedName)
    {
        super(Material.IRON, MapColor.GOLD, unlocalizedName);
        this.kind = kind;
    }

    @Override
    public String getLocalizedName()
    {
        return this.kind == TileEntityBlazeSteam.KIND_STEAM_ENGINE ? "Steam Engine (Create)" : "Blaze Burner (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityBlazeSteam(this.kind);
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
        return te instanceof TileEntityBlazeSteam && ((TileEntityBlazeSteam)te).onPlayerInteract(playerIn, hand);
    }
}
