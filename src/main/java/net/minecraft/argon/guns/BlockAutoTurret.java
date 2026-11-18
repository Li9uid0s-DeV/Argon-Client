package net.minecraft.argon.guns;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Techguns-style Automated Defense Turret block.
 * Tracks and fires at hostile mobs while protecting players.
 */
public class BlockAutoTurret extends Block implements ITileEntityProvider
{
    private static final AxisAlignedBB TURRET_AABB = new AxisAlignedBB(0.125D, 0.0D, 0.125D, 0.875D, 0.92D, 0.875D);

    public BlockAutoTurret()
    {
        super(Material.IRON);
        this.setUnlocalizedName("argon_auto_turret");
        this.setHardness(4.0F);
        this.setResistance(12.0F);
        this.setSoundType(SoundType.METAL);
        this.setCreativeTab(CreativeTabs.COMBAT);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return TURRET_AABB;
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
        return new TileEntityAutoTurret();
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityAutoTurret && placer instanceof EntityPlayer)
        {
            ((TileEntityAutoTurret)te).setOwnerName(placer.getName());
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        TileEntity te = worldIn.getTileEntity(pos);

        if (!(te instanceof TileEntityAutoTurret))
        {
            return false;
        }

        TileEntityAutoTurret turret = (TileEntityAutoTurret)te;

        if (worldIn.isRemote)
        {
            return true;
        }

        ItemStack held = playerIn.getHeldItem(hand);

        if (!held.isEmpty() && held.getItem() instanceof ItemGunAmmo)
        {
            int added = turret.addAmmo(30);

            if (added > 0)
            {
                if (!playerIn.capabilities.isCreativeMode)
                {
                    held.shrink(1);
                }

                worldIn.playSound((EntityPlayer)null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.7F, 1.4F);
                playerIn.sendStatusMessage(new TextComponentString("\u00a7b[Auto Turret] \u00a7fLoaded +" + added + " rounds (" + turret.getAmmoCount() + "/" + TileEntityAutoTurret.MAX_AMMO + ")"), true);
                return true;
            }
        }

        if (playerIn.isSneaking())
        {
            turret.toggleEnabled();
            String status = turret.isEnabled() ? "\u00a7aONLINE" : "\u00a7cSTANDBY";
            worldIn.playSound((EntityPlayer)null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.6F, turret.isEnabled() ? 1.6F : 0.8F);
            playerIn.sendStatusMessage(new TextComponentString("\u00a7b[Auto Turret] \u00a7fStatus: " + status + " \u00a77| Ammo: " + turret.getAmmoCount()), true);
        }
        else
        {
            String status = turret.isEnabled() ? "\u00a7aONLINE" : "\u00a7cSTANDBY";
            playerIn.sendStatusMessage(new TextComponentString("\u00a7b[Auto Turret] \u00a7f" + status + " \u00a77| Ammo: \u00a7e" + turret.getAmmoCount() + "/" + TileEntityAutoTurret.MAX_AMMO + " \u00a78(Sneak+RMB toggle, RMB with Ammo to refill)"), true);
        }

        return true;
    }
}
