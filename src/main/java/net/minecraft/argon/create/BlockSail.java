package net.minecraft.argon.create;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.EnumBlockRenderType;

public class BlockSail extends Block
{
    public BlockSail()
    {
        super(Material.CLOTH, MapColor.SNOW);
        this.setHardness(0.8F);
        this.setSoundType(SoundType.CLOTH);
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setUnlocalizedName("createSail");
    }

    @Override
    public String getLocalizedName()
    {
        return "Windmill Sail (Create)";
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
