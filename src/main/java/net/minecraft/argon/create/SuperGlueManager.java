package net.minecraft.argon.create;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class SuperGlueManager
{
    public static final class GlueBox
    {
        public final BlockPos min;
        public final BlockPos max;

        public GlueBox(BlockPos a, BlockPos b)
        {
            this.min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
            this.max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        }

        public boolean contains(BlockPos p)
        {
            return p.getX() >= this.min.getX() && p.getX() <= this.max.getX()
                && p.getY() >= this.min.getY() && p.getY() <= this.max.getY()
                && p.getZ() >= this.min.getZ() && p.getZ() <= this.max.getZ();
        }
    }

    private static final Map<World, List<GlueBox>> WORLD_GLUE = new HashMap<World, List<GlueBox>>();
    private static final Map<EntityPlayer, BlockPos> PENDING_FIRST_CORNER = new HashMap<EntityPlayer, BlockPos>();

    private SuperGlueManager()
    {
    }

    public static BlockPos getPendingCorner(EntityPlayer player)
    {
        return PENDING_FIRST_CORNER.get(player);
    }

    public static void setPendingCorner(EntityPlayer player, BlockPos pos)
    {
        if (pos == null)
        {
            PENDING_FIRST_CORNER.remove(player);
        }
        else
        {
            PENDING_FIRST_CORNER.put(player, pos.toImmutable());
        }
    }

    public static void addGlueBox(World world, BlockPos a, BlockPos b)
    {
        if (world == null || a == null || b == null)
        {
            return;
        }

        List<GlueBox> list = WORLD_GLUE.get(world);

        if (list == null)
        {
            list = new ArrayList<GlueBox>();
            WORLD_GLUE.put(world, list);
        }

        list.add(new GlueBox(a, b));
    }

    public static int removeGlueAt(World world, BlockPos pos)
    {
        List<GlueBox> list = WORLD_GLUE.get(world);

        if (list == null || list.isEmpty())
        {
            return 0;
        }

        int removed = 0;

        for (int i = list.size() - 1; i >= 0; --i)
        {
            if (list.get(i).contains(pos))
            {
                list.remove(i);
                ++removed;
            }
        }

        return removed;
    }

    public static List<GlueBox> getBoxesForWorld(World world)
    {
        List<GlueBox> list = WORLD_GLUE.get(world);
        return list != null ? list : java.util.Collections.<GlueBox>emptyList();
    }

    public static boolean areGluedTogether(World world, BlockPos a, BlockPos b)
    {
        List<GlueBox> list = WORLD_GLUE.get(world);

        if (list == null)
        {
            return false;
        }

        for (int i = 0; i < list.size(); ++i)
        {
            GlueBox box = list.get(i);

            if (box.contains(a) && box.contains(b))
            {
                return true;
            }
        }

        return false;
    }

    public static void clearWorld(World world)
    {
        WORLD_GLUE.remove(world);
    }

    /**
     * Collects all glued blocks connected to seedPos (also includes Chassis/Linear sticky attachment up to maxBlocks).
     */
    public static List<BlockPos> collectGluedBlocks(World world, BlockPos seedPos, BlockPos excludeAnchor, int maxBlocks)
    {
        List<BlockPos> result = new ArrayList<BlockPos>();

        if (world == null || seedPos == null)
        {
            return result;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
        Set<BlockPos> visited = new HashSet<BlockPos>();

        if (excludeAnchor != null)
        {
            visited.add(excludeAnchor);
        }

        IBlockState seedState = world.getBlockState(seedPos);

        if (isMovableContraptionBlock(world, seedPos, seedState))
        {
            queue.add(seedPos);
            visited.add(seedPos);
        }

        while (!queue.isEmpty() && result.size() < maxBlocks)
        {
            BlockPos curr = queue.poll();
            result.add(curr);

            for (EnumFacing dir : EnumFacing.VALUES)
            {
                BlockPos np = curr.offset(dir);

                if (visited.contains(np) || !world.isBlockLoaded(np))
                {
                    continue;
                }

                IBlockState ns = world.getBlockState(np);

                if (!isMovableContraptionBlock(world, np, ns))
                {
                    continue;
                }

                if (areGluedTogether(world, curr, np) || ns.getBlock() == Blocks.SLIME_BLOCK || world.getBlockState(curr).getBlock() == Blocks.SLIME_BLOCK)
                {
                    visited.add(np);
                    queue.add(np);
                }
            }
        }

        return result;
    }

    public static boolean isMovableContraptionBlock(World world, BlockPos pos, IBlockState state)
    {
        if (state == null || state.getMaterial() == Material.AIR || state.getMaterial().isLiquid())
        {
            return false;
        }

        if (state.getBlock() == Blocks.BEDROCK || state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == CreateBlocks.TRAIN_TRACK)
        {
            return false;
        }

        return state.getBlockHardness(world, pos) >= 0.0F;
    }
}
