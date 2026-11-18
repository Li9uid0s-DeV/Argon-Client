package net.minecraft.argon.guns;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Placeable Gun Attachment Modification Workbench block.
 * Right-clicking opens GuiGunWorkbench (vanilla GuiScreen) to customize the held firearm.
 */
public class BlockGunWorkbench extends Block implements ITileEntityProvider
{
    public BlockGunWorkbench()
    {
        super(Material.IRON);
        this.setUnlocalizedName("argon_gun_workbench");
        this.setHardness(3.5F);
        this.setResistance(8.0F);
        this.setSoundType(SoundType.METAL);
        this.setCreativeTab(CreativeTabs.COMBAT);
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
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityGunWorkbench();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (worldIn.isRemote)
        {
            Minecraft.getMinecraft().displayGuiScreen(new GuiGunWorkbench(pos));
        }

        return true;
    }
}
