package net.minecraft.argon.tnt;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Distributed Explosion Scheduler for Argon Client (Step 5-3A).
 * Prevents frame freezes and GC spikes on low-spec hardware (MediaTek Kompanio 500 / 4GB RAM)
 * by queuing massive block replacements, crater excavations, and entity spawns across ticks:
 *   - Maximum 500 block changes per tick
 *   - Maximum 20 entity spawns per tick
 *
 * Single-threaded, reflection-free, TeaVM / WASM compatible Java 8 implementation.
 */
public final class ArgonExplosionScheduler
{
    public static final int MAX_BLOCKS_PER_TICK = 500;
    public static final int MAX_ENTITIES_PER_TICK = 20;
    private static final int MAX_SCANS_PER_TICK = 2400;
    private static final int MAX_PARTICLES_PER_TICK = 48;
    private static final int MAX_QUEUED_BLOCK_TASKS = 40000;
    private static final int MAX_QUEUED_ENTITY_TASKS = 600;

    private static final Map<Integer, WorldQueue> SERVER_QUEUES = new HashMap<Integer, WorldQueue>();
    private static final Map<Integer, WorldQueue> CLIENT_QUEUES = new HashMap<Integer, WorldQueue>();
    private static final ArrayDeque<ParticleTask> CLIENT_PARTICLES = new ArrayDeque<ParticleTask>();

    private static int lastServerQueuedBlocks = 0;
    private static int lastServerQueuedEntities = 0;

    private ArgonExplosionScheduler()
    {
    }

    /**
     * Functional interface for zero-allocation streaming volume explosion jobs.
     * Returns true if a block was actually modified (counts toward the 500 blocks/tick budget).
     */
    public interface VolumeCellProcessor
    {
        boolean processCell(World world, BlockPos.MutableBlockPos pos, int dx, int dy, int dz);
    }

    private static final class BlockChangeTask
    {
        final int x;
        final int y;
        final int z;
        final IBlockState state;
        final int flags;

        BlockChangeTask(int x, int y, int z, IBlockState state, int flags)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.state = state;
            this.flags = flags;
        }
    }

    private static final class EntitySpawnTask
    {
        final Entity entity;
        final boolean weatherEffect;

        EntitySpawnTask(Entity entity, boolean weatherEffect)
        {
            this.entity = entity;
            this.weatherEffect = weatherEffect;
        }
    }

    private static final class ParticleTask
    {
        final EnumParticleTypes type;
        final double x;
        final double y;
        final double z;
        final double vx;
        final double vy;
        final double vz;

        ParticleTask(EnumParticleTypes type, double x, double y, double z, double vx, double vy, double vz)
        {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
        }
    }

    /**
     * Stateful 3D bounding-box scanner that avoids allocating tens of thousands of BlockPos objects.
     */
    public static final class VolumeJob
    {
        private final int originX;
        private final int originY;
        private final int originZ;
        private final int minDx;
        private final int maxDx;
        private final int minDy;
        private final int maxDy;
        private final int minDz;
        private final int maxDz;
        private final VolumeCellProcessor processor;

        private int curDy;
        private int curDx;
        private int curDz;
        private boolean finished;

        public VolumeJob(int originX, int originY, int originZ, int minDx, int maxDx, int minDy, int maxDy, int minDz, int maxDz, VolumeCellProcessor processor)
        {
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.minDx = minDx;
            this.maxDx = maxDx;
            this.minDy = minDy;
            this.maxDy = maxDy;
            this.minDz = minDz;
            this.maxDz = maxDz;
            this.processor = processor;
            // Scan top-down so falling blocks/sand collapse cleanly
            this.curDy = maxDy;
            this.curDx = minDx;
            this.curDz = minDz;
            this.finished = (minDx > maxDx || minDy > maxDy || minDz > maxDz);
        }

        public boolean isFinished()
        {
            return this.finished;
        }

        public int estimateRemainingCells()
        {
            if (this.finished)
            {
                return 0;
            }

            int layersLeft = this.curDy - this.minDy + 1;
            int area = (this.maxDx - this.minDx + 1) * (this.maxDz - this.minDz + 1);
            return Math.max(0, layersLeft * area);
        }

        int step(World world, BlockPos.MutableBlockPos mutablePos, int maxBlockChanges, int maxScans)
        {
            int changed = 0;
            int scanned = 0;

            while (!this.finished && changed < maxBlockChanges && scanned < maxScans)
            {
                int wx = this.originX + this.curDx;
                int wy = this.originY + this.curDy;
                int wz = this.originZ + this.curDz;

                if (wy >= 1 && wy < world.getHeight())
                {
                    mutablePos.setPos(wx, wy, wz);

                    if (world.isBlockLoaded(mutablePos))
                    {
                        if (this.processor.processCell(world, mutablePos, this.curDx, this.curDy, this.curDz))
                        {
                            changed++;
                        }
                    }
                }

                scanned++;
                this.advanceCursor();
            }

            return changed;
        }

        private void advanceCursor()
        {
            this.curDz++;

            if (this.curDz > this.maxDz)
            {
                this.curDz = this.minDz;
                this.curDx++;

                if (this.curDx > this.maxDx)
                {
                    this.curDx = this.minDx;
                    this.curDy--;

                    if (this.curDy < this.minDy)
                    {
                        this.finished = true;
                    }
                }
            }
        }
    }

    private static final class WorldQueue
    {
        final ArrayDeque<BlockChangeTask> blockTasks = new ArrayDeque<BlockChangeTask>();
        final ArrayDeque<VolumeJob> volumeJobs = new ArrayDeque<VolumeJob>();
        final ArrayDeque<EntitySpawnTask> entityTasks = new ArrayDeque<EntitySpawnTask>();
        final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

        boolean isEmpty()
        {
            return this.blockTasks.isEmpty() && this.volumeJobs.isEmpty() && this.entityTasks.isEmpty();
        }
    }

    private static int getDimensionId(World world)
    {
        if (world != null && world.provider != null && world.provider.getDimensionType() != null)
        {
            return world.provider.getDimensionType().getId();
        }

        return 0;
    }

    private static WorldQueue getQueue(World world)
    {
        Map<Integer, WorldQueue> map = world.isRemote ? CLIENT_QUEUES : SERVER_QUEUES;
        int dimId = getDimensionId(world);
        Integer key = Integer.valueOf(dimId);
        WorldQueue queue = map.get(key);

        if (queue == null)
        {
            queue = new WorldQueue();
            map.put(key, queue);
        }

        return queue;
    }

    public static void queueBlockChange(World world, BlockPos pos, IBlockState state)
    {
        queueBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), state, 2);
    }

    public static void queueBlockChange(World world, int x, int y, int z, IBlockState state, int flags)
    {
        if (world == null || state == null || y < 1 || y >= world.getHeight())
        {
            return;
        }

        WorldQueue q = getQueue(world);

        if (q.blockTasks.size() < MAX_QUEUED_BLOCK_TASKS)
        {
            q.blockTasks.addLast(new BlockChangeTask(x, y, z, state, flags));
        }
    }

    public static void queueVolumeJob(World world, VolumeJob job)
    {
        if (world == null || job == null || job.isFinished())
        {
            return;
        }

        getQueue(world).volumeJobs.addLast(job);
    }

    public static void queueEntitySpawn(World world, Entity entity)
    {
        if (world == null || entity == null)
        {
            return;
        }

        WorldQueue q = getQueue(world);

        if (q.entityTasks.size() < MAX_QUEUED_ENTITY_TASKS)
        {
            boolean weather = entity instanceof EntityLightningBolt;
            q.entityTasks.addLast(new EntitySpawnTask(entity, weather));
        }
    }

    public static void queueClientParticle(EnumParticleTypes type, double x, double y, double z, double vx, double vy, double vz)
    {
        if (type == null)
        {
            return;
        }

        if (CLIENT_PARTICLES.size() < 256)
        {
            CLIENT_PARTICLES.addLast(new ParticleTask(type, x, y, z, vx, vy, vz));
        }
    }

    /**
     * Called every tick from Minecraft.runTick().
     * Processes client-side visual particle bursts and any client-world queued tasks
     * within the strict 500 blocks / 20 entities per tick budget.
     */
    public static void onClientTick(Minecraft mc)
    {
        if (mc == null || mc.world == null)
        {
            CLIENT_QUEUES.clear();
            CLIENT_PARTICLES.clear();
            return;
        }

        int spawnedParticles = 0;

        while (spawnedParticles < MAX_PARTICLES_PER_TICK && !CLIENT_PARTICLES.isEmpty())
        {
            ParticleTask pt = CLIENT_PARTICLES.pollFirst();

            if (pt != null)
            {
                mc.world.spawnParticle(pt.type, pt.x, pt.y, pt.z, pt.vx, pt.vy, pt.vz);
                spawnedParticles++;
            }
        }

        drainWorldQueue(mc.world, CLIENT_QUEUES);
    }

    /**
     * Called every tick from WorldServer.tick().
     * Executes authoritative block modifications (max 500/tick) and entity spawns (max 20/tick)
     * across multiple ticks without stalling the game loop.
     */
    public static void onServerWorldTick(World world)
    {
        if (world == null || world.isRemote)
        {
            return;
        }

        drainWorldQueue(world, SERVER_QUEUES);
        updateServerMetrics();
    }

    private static void drainWorldQueue(World world, Map<Integer, WorldQueue> map)
    {
        int dimId = getDimensionId(world);
        WorldQueue queue = map.get(Integer.valueOf(dimId));

        if (queue == null || queue.isEmpty())
        {
            return;
        }

        int blocksProcessed = 0;
        int scansUsed = 0;

        // 1. Drain explicit block change queue first (up to MAX_BLOCKS_PER_TICK)
        while (blocksProcessed < MAX_BLOCKS_PER_TICK && !queue.blockTasks.isEmpty())
        {
            BlockChangeTask task = queue.blockTasks.pollFirst();

            if (task == null)
            {
                break;
            }

            queue.scratchPos.setPos(task.x, task.y, task.z);

            if (world.isBlockLoaded(queue.scratchPos))
            {
                IBlockState current = world.getBlockState(queue.scratchPos);

                if (current.getBlock() != Blocks.BEDROCK || task.state.getBlock() == Blocks.BEDROCK)
                {
                    if (current != task.state)
                    {
                        world.setBlockState(queue.scratchPos, task.state, task.flags);
                        blocksProcessed++;
                    }
                }
            }
        }

        // 2. Drain streaming spherical/planar volume explosion jobs within remaining block budget
        while (blocksProcessed < MAX_BLOCKS_PER_TICK && scansUsed < MAX_SCANS_PER_TICK && !queue.volumeJobs.isEmpty())
        {
            VolumeJob job = queue.volumeJobs.peekFirst();

            if (job == null || job.isFinished())
            {
                queue.volumeJobs.pollFirst();
                continue;
            }

            int remainingBlocks = MAX_BLOCKS_PER_TICK - blocksProcessed;
            int remainingScans = MAX_SCANS_PER_TICK - scansUsed;
            int changed = job.step(world, queue.scratchPos, remainingBlocks, remainingScans);
            blocksProcessed += changed;
            scansUsed += Math.max(1, changed);

            if (job.isFinished())
            {
                queue.volumeJobs.pollFirst();
            }
            else if (changed == 0)
            {
                break;
            }
        }

        // 3. Drain entity spawn queue (strictly capped at MAX_ENTITIES_PER_TICK)
        int entitiesProcessed = 0;

        while (entitiesProcessed < MAX_ENTITIES_PER_TICK && !queue.entityTasks.isEmpty())
        {
            EntitySpawnTask task = queue.entityTasks.pollFirst();

            if (task != null && task.entity != null)
            {
                if (task.weatherEffect)
                {
                    world.addWeatherEffect(task.entity);
                }
                else
                {
                    world.spawnEntity(task.entity);
                }

                entitiesProcessed++;
            }
        }

        if (queue.isEmpty())
        {
            map.remove(Integer.valueOf(dimId));
        }
    }

    private static void updateServerMetrics()
    {
        int pendingBlocks = 0;
        int pendingEntities = 0;

        Iterator<WorldQueue> it = SERVER_QUEUES.values().iterator();

        while (it.hasNext())
        {
            WorldQueue q = it.next();
            pendingBlocks += q.blockTasks.size();
            pendingEntities += q.entityTasks.size();

            Iterator<VolumeJob> jit = q.volumeJobs.iterator();

            while (jit.hasNext())
            {
                pendingBlocks += jit.next().estimateRemainingCells();
            }
        }

        lastServerQueuedBlocks = pendingBlocks;
        lastServerQueuedEntities = pendingEntities;
    }

    public static void clearAll()
    {
        SERVER_QUEUES.clear();
        CLIENT_QUEUES.clear();
        CLIENT_PARTICLES.clear();
        lastServerQueuedBlocks = 0;
        lastServerQueuedEntities = 0;
    }

    public static int getPendingBlocks()
    {
        return lastServerQueuedBlocks;
    }

    public static int getPendingEntities()
    {
        return lastServerQueuedEntities;
    }
}
