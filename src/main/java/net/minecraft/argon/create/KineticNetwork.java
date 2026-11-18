package net.minecraft.argon.create;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class KineticNetwork
{
    public static final float DEFAULT_STRESS_IMPACT = 4.0F;
    private static final int MAX_NETWORK_NODES = 1024;
    private static final Map<World, Set<BlockPos>> PENDING_UPDATES = new HashMap<World, Set<BlockPos>>();

    public long networkId;
    public float totalCapacitySU;
    public float totalStressSU;
    public boolean overstressed;
    public int nodeCount;

    public static void scheduleUpdate(World world, BlockPos pos)
    {
        if (world == null || pos == null)
        {
            return;
        }

        Set<BlockPos> set = PENDING_UPDATES.get(world);

        if (set == null)
        {
            set = new HashSet<BlockPos>();
            PENDING_UPDATES.put(world, set);
        }

        set.add(pos.toImmutable());
    }

    public static void clearWorld(World world)
    {
        PENDING_UPDATES.remove(world);
    }

    public static void processPendingUpdates(World world)
    {
        Set<BlockPos> pending = PENDING_UPDATES.get(world);

        if (pending == null || pending.isEmpty())
        {
            return;
        }

        List<BlockPos> seeds = new ArrayList<BlockPos>(pending);
        pending.clear();

        Set<BlockPos> visitedGlobal = new HashSet<BlockPos>();

        for (int i = 0; i < seeds.size(); ++i)
        {
            BlockPos seed = seeds.get(i);

            if (visitedGlobal.contains(seed))
            {
                continue;
            }

            if (!world.isBlockLoaded(seed))
            {
                continue;
            }

            TileEntity te = world.getTileEntity(seed);

            if (te instanceof TileEntityKinetic)
            {
                recomputeNetworkFrom(world, (TileEntityKinetic)te, visitedGlobal);
            }
            else
            {
                for (EnumFacing dir : EnumFacing.VALUES)
                {
                    BlockPos neighborPos = seed.offset(dir);

                    if (!visitedGlobal.contains(neighborPos) && world.isBlockLoaded(neighborPos))
                    {
                        TileEntity nte = world.getTileEntity(neighborPos);

                        if (nte instanceof TileEntityKinetic)
                        {
                            recomputeNetworkFrom(world, (TileEntityKinetic)nte, visitedGlobal);
                        }
                    }
                }
            }
        }
    }

    private static void recomputeNetworkFrom(World world, TileEntityKinetic startNode, Set<BlockPos> visitedGlobal)
    {
        List<TileEntityKinetic> members = new ArrayList<TileEntityKinetic>();
        Map<BlockPos, Float> assignedSpeed = new HashMap<BlockPos, Float>();
        ArrayDeque<TileEntityKinetic> queue = new ArrayDeque<TileEntityKinetic>();
        Set<BlockPos> componentVisited = new HashSet<BlockPos>();

        // 1. Discover entire connected kinetic component first via BFS
        queue.add(startNode);
        componentVisited.add(startNode.getPos());

        while (!queue.isEmpty() && members.size() < MAX_NETWORK_NODES)
        {
            TileEntityKinetic curr = queue.poll();
            members.add(curr);
            visitedGlobal.add(curr.getPos());

            List<KineticConnection> links = collectConnections(world, curr);

            for (int i = 0; i < links.size(); ++i)
            {
                KineticConnection conn = links.get(i);
                BlockPos np = conn.target.getPos();

                if (!componentVisited.contains(np))
                {
                    componentVisited.add(np);
                    queue.add(conn.target);
                }
            }
        }

        // 2. Find primary driver source (or multiple compatible sources)
        float totalCap = 0.0F;
        float totalStress = 0.0F;
        TileEntityKinetic primarySource = null;
        float highestSpeed = 0.0F;

        for (int i = 0; i < members.size(); ++i)
        {
            TileEntityKinetic node = members.get(i);
            float genCap = node.getGeneratedCapacitySU();
            float genRpm = Math.abs(node.getGeneratedSpeedRPM());

            if (genCap > 0.0F && genRpm > 0.0F)
            {
                totalCap += genCap;

                if (genRpm > highestSpeed)
                {
                    highestSpeed = genRpm;
                    primarySource = node;
                }
            }
        }

        boolean hasConflict = false;

        if (primarySource != null)
        {
            assignedSpeed.put(primarySource.getPos(), primarySource.getGeneratedSpeedRPM());
            queue.clear();
            queue.add(primarySource);

            while (!queue.isEmpty())
            {
                TileEntityKinetic curr = queue.poll();
                float currSpeed = assignedSpeed.get(curr.getPos()).floatValue();
                List<KineticConnection> links = collectConnections(world, curr);

                for (int i = 0; i < links.size(); ++i)
                {
                    KineticConnection conn = links.get(i);
                    float nextSpeed = currSpeed * conn.ratio;
                    BlockPos np = conn.target.getPos();
                    Float prev = assignedSpeed.get(np);

                    if (prev == null)
                    {
                        assignedSpeed.put(np, Float.valueOf(nextSpeed));
                        queue.add(conn.target);
                    }
                    else
                    {
                        if (Math.abs(prev.floatValue() - nextSpeed) > 0.5F)
                        {
                            // Direction / gear-ratio conflict locks the component
                            hasConflict = true;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < members.size(); ++i)
        {
            TileEntityKinetic node = members.get(i);
            Float spd = assignedSpeed.get(node.getPos());
            float finalSpd = (spd != null && !hasConflict) ? spd.floatValue() : 0.0F;
            float impact = node.getStressImpactSU(Math.abs(finalSpd));

            if (impact > 0.0F)
            {
                totalStress += impact;
            }
        }

        boolean overstressed = hasConflict || (totalStress > totalCap + 0.001F);
        long netId = members.isEmpty() ? 0L : members.get(0).getPos().toLong();

        for (int i = 0; i < members.size(); ++i)
        {
            TileEntityKinetic node = members.get(i);
            Float spd = assignedSpeed.get(node.getPos());
            float finalSpd = (spd != null && !overstressed) ? spd.floatValue() : 0.0F;
            node.applyNetworkState(netId, finalSpd, totalCap, totalStress, overstressed, members.size());
        }
    }

    private static List<KineticConnection> collectConnections(World world, TileEntityKinetic curr)
    {
        List<KineticConnection> out = new ArrayList<KineticConnection>(8);
        BlockPos pos = curr.getPos();
        EnumFacing.Axis axis = curr.getAxis();
        boolean currSmallCog = curr.hasSmallCogwheel();
        boolean currLargeCog = curr.hasLargeCogwheel();
        boolean currGearbox = curr.isGearbox();

        // 1. Axial shaft/cogwheel connections along current axis (and Gearbox 4-way horizontal / omni axes)
        for (EnumFacing dir : EnumFacing.VALUES)
        {
            boolean currConnectsAxis = currGearbox
                ? (dir.getAxis() != axis)
                : (dir.getAxis() == axis && curr.hasShaftOnSide(dir));

            if (!currConnectsAxis)
            {
                continue;
            }

            BlockPos np = pos.offset(dir);

            if (!world.isBlockLoaded(np))
            {
                continue;
            }

            TileEntity te = world.getTileEntity(np);

            if (!(te instanceof TileEntityKinetic))
            {
                continue;
            }

            TileEntityKinetic target = (TileEntityKinetic)te;
            boolean targetConnects = target.isGearbox()
                ? (dir.getOpposite().getAxis() != target.getAxis())
                : (dir.getOpposite().getAxis() == target.getAxis() && target.hasShaftOnSide(dir.getOpposite()));

            if (targetConnects)
            {
                float ratio = (currGearbox || target.isGearbox()) ? getGearboxRatio(dir, curr, target) : 1.0F;
                out.add(new KineticConnection(target, ratio));
            }
        }

        // 2. Same-axis adjacent cogwheel meshing (8-tooth <-> 8-tooth, 16-tooth <-> 16-tooth)
        if (currSmallCog || currLargeCog)
        {
            for (EnumFacing dir : EnumFacing.VALUES)
            {
                if (dir.getAxis() == axis)
                {
                    continue;
                }

                BlockPos np = pos.offset(dir);

                if (!world.isBlockLoaded(np))
                {
                    continue;
                }

                TileEntity te = world.getTileEntity(np);

                if (!(te instanceof TileEntityKinetic))
                {
                    continue;
                }

                TileEntityKinetic target = (TileEntityKinetic)te;

                if (target.getAxis() != axis)
                {
                    continue;
                }

                if (currSmallCog && target.hasSmallCogwheel())
                {
                    out.add(new KineticConnection(target, -1.0F));
                }
                else if (currLargeCog && target.hasLargeCogwheel())
                {
                    out.add(new KineticConnection(target, -1.0F));
                }
            }

            // 3. Diagonal Large <-> Small cogwheel (2:1 / 1:2 speed stepping in same axis plane)
            for (EnumFacing dir1 : EnumFacing.VALUES)
            {
                if (dir1.getAxis() == axis)
                {
                    continue;
                }

                for (EnumFacing dir2 : EnumFacing.VALUES)
                {
                    if (dir2.getAxis() == axis || dir2.getAxis() == dir1.getAxis())
                    {
                        continue;
                    }

                    BlockPos diag = pos.offset(dir1).offset(dir2);

                    if (!world.isBlockLoaded(diag))
                    {
                        continue;
                    }

                    TileEntity te = world.getTileEntity(diag);

                    if (!(te instanceof TileEntityKinetic))
                    {
                        continue;
                    }

                    TileEntityKinetic target = (TileEntityKinetic)te;

                    if (target.getAxis() != axis)
                    {
                        continue;
                    }

                    if (currLargeCog && target.hasSmallCogwheel())
                    {
                        out.add(new KineticConnection(target, -2.0F));
                    }
                    else if (currSmallCog && target.hasLargeCogwheel())
                    {
                        out.add(new KineticConnection(target, -0.5F));
                    }
                }
            }
        }

        // 4. Perpendicular Large Cogwheel bevel gear coupling (90-degree speed transmission)
        if (currLargeCog)
        {
            for (EnumFacing dir1 : EnumFacing.VALUES)
            {
                if (dir1.getAxis() == axis)
                {
                    continue;
                }

                for (EnumFacing dir2 : EnumFacing.VALUES)
                {
                    if (dir2.getAxis() != axis)
                    {
                        continue;
                    }

                    BlockPos bevelPos = pos.offset(dir1).offset(dir2);

                    if (!world.isBlockLoaded(bevelPos))
                    {
                        continue;
                    }

                    TileEntity te = world.getTileEntity(bevelPos);

                    if (te instanceof TileEntityKinetic)
                    {
                        TileEntityKinetic target = (TileEntityKinetic)te;

                        if (target.hasLargeCogwheel() && target.getAxis() == dir1.getAxis())
                        {
                            out.add(new KineticConnection(target, -1.0F));
                        }
                    }
                }
            }
        }

        // 5. Mechanical Belt chain link between connected belt segments
        if (curr instanceof TileEntityBelt)
        {
            TileEntityBelt belt = (TileEntityBelt)curr;
            BlockPos next = belt.getNextBeltPos();
            BlockPos prev = belt.getPrevBeltPos();

            if (next != null && world.isBlockLoaded(next))
            {
                TileEntity te = world.getTileEntity(next);

                if (te instanceof TileEntityBelt)
                {
                    out.add(new KineticConnection((TileEntityBelt)te, 1.0F));
                }
            }

            if (prev != null && world.isBlockLoaded(prev))
            {
                TileEntity te = world.getTileEntity(prev);

                if (te instanceof TileEntityBelt)
                {
                    out.add(new KineticConnection((TileEntityBelt)te, 1.0F));
                }
            }
        }

        // 6. Gantry Shaft continuous rail propagation
        if (curr.isGantryShaft())
        {
            for (EnumFacing dir : EnumFacing.VALUES)
            {
                if (dir.getAxis() != axis)
                {
                    continue;
                }

                BlockPos np = pos.offset(dir);

                if (world.isBlockLoaded(np))
                {
                    TileEntity te = world.getTileEntity(np);

                    if (te instanceof TileEntityGantryCarriage)
                    {
                        out.add(new KineticConnection((TileEntityKinetic)te, 1.0F));
                    }
                }
            }
        }

        return out;
    }

    private static float getGearboxRatio(EnumFacing dir, TileEntityKinetic curr, TileEntityKinetic target)
    {
        if (curr.isGearbox())
        {
            return dir.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 1.0F : -1.0F;
        }

        return dir.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? 1.0F : -1.0F;
    }

    private static final class KineticConnection
    {
        final TileEntityKinetic target;
        final float ratio;

        KineticConnection(TileEntityKinetic target, float ratio)
        {
            this.target = target;
            this.ratio = ratio;
        }
    }
}
