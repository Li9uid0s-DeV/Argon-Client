package net.minecraft.argon.create;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ContraptionStructure
{
    public static final class BlockEntry
    {
        public final BlockPos relPos;
        public final IBlockState state;

        public BlockEntry(BlockPos relPos, IBlockState state)
        {
            this.relPos = relPos;
            this.state = state;
        }
    }

    public final List<BlockEntry> entries = new ArrayList<BlockEntry>();
    public double minX = -0.5D;
    public double minY = 0.0D;
    public double minZ = -0.5D;
    public double maxX = 0.5D;
    public double maxY = 1.0D;
    public double maxZ = 0.5D;
    public boolean hasControlsSeat = false;
    public BlockPos seatLocalPos = BlockPos.ORIGIN;

    public static ContraptionStructure captureAndRemoveFromWorld(World world, BlockPos anchor, List<BlockPos> worldBlocks)
    {
        ContraptionStructure struct = new ContraptionStructure();

        if (worldBlocks == null || worldBlocks.isEmpty())
        {
            return struct;
        }

        double mnX = Double.MAX_VALUE;
        double mnY = Double.MAX_VALUE;
        double mnZ = Double.MAX_VALUE;
        double mxX = -Double.MAX_VALUE;
        double mxY = -Double.MAX_VALUE;
        double mxZ = -Double.MAX_VALUE;

        for (int i = 0; i < worldBlocks.size(); ++i)
        {
            BlockPos wp = worldBlocks.get(i);
            IBlockState st = world.getBlockState(wp);

            if (st.getBlock() == Blocks.AIR)
            {
                continue;
            }

            BlockPos rel = wp.subtract(anchor);
            struct.entries.add(new BlockEntry(rel, st));

            if (st.getBlock() == CreateBlocks.TRAIN_CONTROLS)
            {
                struct.hasControlsSeat = true;
                struct.seatLocalPos = rel;
            }

            mnX = Math.min(mnX, (double)rel.getX() - 0.5D);
            mnY = Math.min(mnY, (double)rel.getY());
            mnZ = Math.min(mnZ, (double)rel.getZ() - 0.5D);
            mxX = Math.max(mxX, (double)rel.getX() + 0.5D);
            mxY = Math.max(mxY, (double)rel.getY() + 1.0D);
            mxZ = Math.max(mxZ, (double)rel.getZ() + 0.5D);
        }

        if (!struct.entries.isEmpty())
        {
            struct.minX = mnX;
            struct.minY = mnY;
            struct.minZ = mnZ;
            struct.maxX = mxX;
            struct.maxY = mxY;
            struct.maxZ = mxZ;
        }

        for (int i = 0; i < worldBlocks.size(); ++i)
        {
            BlockPos wp = worldBlocks.get(i);
            world.removeTileEntity(wp);
            world.setBlockState(wp, Blocks.AIR.getDefaultState(), 3);
        }

        return struct;
    }

    public void placeIntoWorld(World world, BlockPos anchor)
    {
        for (int i = 0; i < this.entries.size(); ++i)
        {
            BlockEntry e = this.entries.get(i);
            BlockPos target = anchor.add(e.relPos);

            if (world.isAirBlock(target) || world.getBlockState(target).getMaterial().isReplaceable())
            {
                world.setBlockState(target, e.state, 3);
            }
        }
    }

    public AxisAlignedBB getWorldBounds(double originX, double originY, double originZ)
    {
        return new AxisAlignedBB(
            originX + this.minX, originY + this.minY, originZ + this.minZ,
            originX + this.maxX, originY + this.maxY, originZ + this.maxZ);
    }

    public NBTTagCompound writeToNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();

        for (int i = 0; i < this.entries.size(); ++i)
        {
            BlockEntry e = this.entries.get(i);
            NBTTagCompound bt = new NBTTagCompound();
            bt.setInteger("x", e.relPos.getX());
            bt.setInteger("y", e.relPos.getY());
            bt.setInteger("z", e.relPos.getZ());
            bt.setInteger("sid", Block.getStateId(e.state));
            list.appendTag(bt);
        }

        tag.setTag("Entries", list);
        tag.setDouble("minX", this.minX);
        tag.setDouble("minY", this.minY);
        tag.setDouble("minZ", this.minZ);
        tag.setDouble("maxX", this.maxX);
        tag.setDouble("maxY", this.maxY);
        tag.setDouble("maxZ", this.maxZ);
        tag.setBoolean("HasSeat", this.hasControlsSeat);
        tag.setInteger("SeatX", this.seatLocalPos.getX());
        tag.setInteger("SeatY", this.seatLocalPos.getY());
        tag.setInteger("SeatZ", this.seatLocalPos.getZ());
        return tag;
    }

    public static ContraptionStructure readFromNBT(NBTTagCompound tag)
    {
        ContraptionStructure struct = new ContraptionStructure();
        NBTTagList list = tag.getTagList("Entries", 10);

        for (int i = 0; i < list.tagCount(); ++i)
        {
            NBTTagCompound bt = list.getCompoundTagAt(i);
            BlockPos rel = new BlockPos(bt.getInteger("x"), bt.getInteger("y"), bt.getInteger("z"));
            IBlockState st = Block.getStateById(bt.getInteger("sid"));
            struct.entries.add(new BlockEntry(rel, st));
        }

        if (tag.hasKey("maxX"))
        {
            struct.minX = tag.getDouble("minX");
            struct.minY = tag.getDouble("minY");
            struct.minZ = tag.getDouble("minZ");
            struct.maxX = tag.getDouble("maxX");
            struct.maxY = tag.getDouble("maxY");
            struct.maxZ = tag.getDouble("maxZ");
        }

        struct.hasControlsSeat = tag.getBoolean("HasSeat");
        struct.seatLocalPos = new BlockPos(tag.getInteger("SeatX"), tag.getInteger("SeatY"), tag.getInteger("SeatZ"));
        return struct;
    }
}
