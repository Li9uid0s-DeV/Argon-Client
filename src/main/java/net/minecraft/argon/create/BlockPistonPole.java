package net.minecraft.argon.create;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.EnumBlockRenderType;

public class BlockPistonPole extends Block
{
    public BlockPistonPole()
    {
        super(Material.WOOD, MapColor.WOOD);
        this.setHardness(1.2F);
        this.setSoundType(SoundType.WOOD);
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setUnlocalizedName("createPistonPole");
    }

    @Override
    public String getLocalizedName()
    {
        return "Piston Extension Pole (Create)";
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
        return EnumBlockRenderType.MODEL;
    }
}
