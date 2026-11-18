package net.minecraft.argon.create;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockTrainTrack extends BlockContainer
{
    private static final AxisAlignedBB TRACK_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.1875D, 1.0D);
    private static final Map<Integer, BlockPos> pendingCurveStart = new HashMap<Integer, BlockPos>();

    public BlockTrainTrack()
    {
        super(Material.IRON, MapColor.IRON);
        this.setHardness(2.5F);
        this.setSoundType(SoundType.METAL);
        this.setUnlocalizedName("createTrainTrack");
        this.setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    @Override
    public String getLocalizedName()
    {
        return "Train Track (Create)";
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityTrainTrack();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.INVISIBLE;
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
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return TRACK_AABB;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos)
    {
        return TRACK_AABB;
    }

    public static BlockPos getPendingCurveStart(EntityPlayer player)
    {
        return player != null ? pendingCurveStart.get(Integer.valueOf(player.getEntityId())) : null;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntityTrainTrack && placer != null)
        {
            ((TileEntityTrainTrack)te).setFacing(placer.getHorizontalFacing());
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (hand != EnumHand.MAIN_HAND)
        {
            return true;
        }

        ItemStack held = playerIn.getHeldItem(hand);
        boolean isWrench = !held.isEmpty() && held.getItem() == CreateItems.WRENCH;

        if (isWrench && playerIn.isSneaking())
        {
            worldIn.destroyBlock(pos, !playerIn.capabilities.isCreativeMode);
            return true;
        }

        if (playerIn.isSneaking() || isWrench || (!held.isEmpty() && held.getItem() == net.minecraft.item.Item.getItemFromBlock(CreateBlocks.TRAIN_TRACK)))
        {
            Integer key = Integer.valueOf(playerIn.getEntityId());
            BlockPos start = pendingCurveStart.get(key);

            if (start == null)
            {
                pendingCurveStart.put(key, pos);

                if (worldIn.isRemote)
                {
                    playerIn.sendMessage(new TextComponentString("\u00a7b[Train Track] \u00a7fStart anchor set at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "). Click another track to build a curve."));
                }
            }
            else if (start.equals(pos))
            {
                pendingCurveStart.remove(key);

                if (worldIn.isRemote)
                {
                    playerIn.sendMessage(new TextComponentString("\u00a77[Train Track] \u00a7fCurve selection cleared."));
                }
            }
            else
            {
                pendingCurveStart.remove(key);
                double distSq = start.distanceSq(pos);

                if (distSq > 48.0D * 48.0D)
                {
                    if (worldIn.isRemote)
                    {
                        playerIn.sendMessage(new TextComponentString("\u00a7c[Train Track] Target track too far (max 48 blocks)."));
                    }

                    return true;
                }

                TileEntity startTe = worldIn.getTileEntity(start);
                TileEntity endTe = worldIn.getTileEntity(pos);

                if (startTe instanceof TileEntityTrainTrack && endTe instanceof TileEntityTrainTrack)
                {
                    ((TileEntityTrainTrack)startTe).setCurvedLink(pos, ((TileEntityTrainTrack)endTe).getFacing());

                    if (worldIn.isRemote)
                    {
                        playerIn.sendMessage(new TextComponentString("\u00a7a[Train Track] Curved spline linked!"));
                    }
                }
            }

            return true;
        }

        return false;
    }
}
