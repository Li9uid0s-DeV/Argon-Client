package net.minecraft.argon.tnt;

import java.util.List;
import java.util.Random;
import net.minecraft.argon.guns.EntityArgonSoldier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Detonation handlers for all Lucky TNT, Too Much TNT, and Unlucky TNT variants.
 * Every block edit, terrain conversion, and entity spawn is routed exclusively through
 * ArgonExplosionScheduler (max 500 blocks/tick, max 20 entities/tick) for 60 FPS stability.
 */
public final class TntEffects
{
    private TntEffects()
    {
    }

    public static void detonate(World world, EntityArgonTNTPrimed tnt, TntType type)
    {
        if (world == null || world.isRemote || tnt == null || type == null)
        {
            return;
        }

        int cx = MathHelper.floor(tnt.posX);
        int cy = MathHelper.floor(tnt.posY);
        int cz = MathHelper.floor(tnt.posZ);
        Random rand = world.rand;

        switch (type)
        {
            case LUCKY_TNT:
                triggerLuckyEvent(world, tnt, cx, cy, cz, rand);
                break;

            case SUPER_LUCKY_TNT:
                triggerSuperLucky(world, tnt, cx, cy, cz, rand);
                break;

            case MINERAL_TNT:
                triggerMineralConversion(world, cx, cy, cz, type.radius, rand);
                world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.4F, 1.1F);
                break;

            case HOUSE_TNT:
                triggerInstantHouse(world, cx, cy, cz);
                world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_FIREWORK_LARGE_BLAST, SoundCategory.BLOCKS, 1.4F, 1.0F);
                break;

            case TNT_X5:
                triggerSphericalCrater(world, tnt, cx, cy, cz, type.radius, false, false, rand);
                break;

            case TNT_X20:
                triggerSphericalCrater(world, tnt, cx, cy, cz, type.radius, true, false, rand);
                break;

            case TNT_X100:
                triggerSphericalCrater(world, tnt, cx, cy, cz, type.radius, true, false, rand);
                break;

            case NUKE_TNT:
                triggerNuclearBlast(world, tnt, cx, cy, cz, type.radius, rand);
                break;

            case METEOR_TNT:
                triggerMeteorImpact(world, tnt, cx, cy, cz, type.radius, rand);
                break;

            case OCEAN_TNT:
                triggerOceanBasin(world, tnt, cx, cy, cz, type.radius);
                break;

            case FLAT_TNT:
                triggerFlatClear(world, tnt, cx, cy, cz, type.radius);
                break;

            case MINING_FLAT_TNT:
                triggerMiningFlat(world, tnt, cx, cy, cz, type.radius);
                break;

            case CLUSTER_TNT:
                triggerClusterBlast(world, tnt, cx, cy, cz, rand);
                break;

            case UNLUCKY_TNT:
                triggerUnluckyEvent(world, tnt, cx, cy, cz, rand);
                break;

            case CURSED_NUKE_TNT:
                triggerCursedNuke(world, tnt, cx, cy, cz, type.radius, rand);
                break;

            case LAVA_TRAP_TNT:
                triggerLavaTrap(world, tnt, cx, cy, cz, type.radius, rand);
                break;

            case MOB_SWARM_TNT:
                triggerMobSwarm(world, tnt, cx, cy, cz, 28, rand);
                break;

            default:
                triggerSphericalCrater(world, tnt, cx, cy, cz, 8, false, false, rand);
                break;
        }
    }

    public static void spawnClientDetonationBurst(double x, double y, double z, TntType type)
    {
        if (type == null)
        {
            return;
        }

        int count = Math.min(40, 12 + type.radius);
        EnumParticleTypes mainParticle = EnumParticleTypes.EXPLOSION_LARGE;

        if (type.category == TntType.Category.LUCKY)
        {
            mainParticle = EnumParticleTypes.FIREWORKS_SPARK;
        }
        else if (type.category == TntType.Category.UNLUCKY)
        {
            mainParticle = EnumParticleTypes.SPELL_WITCH;
        }

        for (int i = 0; i < count; ++i)
        {
            double angle = (Math.PI * 2.0D * i) / (double)count;
            double spread = 0.25D + (i % 5) * 0.14D;
            double vx = Math.cos(angle) * spread;
            double vz = Math.sin(angle) * spread;
            double vy = 0.15D + (i % 4) * 0.12D;
            ArgonExplosionScheduler.queueClientParticle(mainParticle, x, y, z, vx, vy, vz);
        }

        if (type == TntType.NUKE_TNT || type == TntType.CURSED_NUKE_TNT)
        {
            for (int h = 0; h < 20; ++h)
            {
                double py = y + h * 1.3D;
                double ring = h > 13 ? 4.5D : 1.1D;
                double a = h * 0.9D;
                ArgonExplosionScheduler.queueClientParticle(
                    EnumParticleTypes.EXPLOSION_HUGE,
                    x + Math.cos(a) * ring,
                    py,
                    z + Math.sin(a) * ring,
                    0.0D,
                    0.1D,
                    0.0D
                );
            }
        }
    }

    // -------------------------------------------------------------------------
    // LUCKY TNT SERIES
    // -------------------------------------------------------------------------

    private static void triggerLuckyEvent(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, Random rand)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_FIREWORK_LARGE_BLAST, SoundCategory.BLOCKS, 1.5F, 1.0F);
        int roll = rand.nextInt(5);

        switch (roll)
        {
            case 0:
                // Treasure & diamond fountain
                spawnTreasureShower(world, tnt.posX, tnt.posY + 1.0D, tnt.posZ, 18, rand);
                spawnXpBurst(world, tnt.posX, tnt.posY + 1.0D, tnt.posZ, 10, 15, rand);
                break;

            case 1:
                // Rainbow wool dome + glowstone chandelier
                queueRainbowDome(world, cx, cy, cz, 7);
                break;

            case 2:
                // Rich ore vein conversion
                triggerMineralConversion(world, cx, cy, cz, 8, rand);
                break;

            case 3:
                // Friendly Iron Golem guardians + golden apples
                for (int i = 0; i < 3; ++i)
                {
                    EntityIronGolem golem = new EntityIronGolem(world);
                    golem.setPlayerCreated(true);
                    double ang = (Math.PI * 2.0D * i) / 3.0D;
                    golem.setLocationAndAngles(tnt.posX + Math.cos(ang) * 2.5D, tnt.posY + 0.5D, tnt.posZ + Math.sin(ang) * 2.5D, rand.nextFloat() * 360.0F, 0.0F);
                    ArgonExplosionScheduler.queueEntitySpawn(world, golem);
                }

                spawnDrop(world, tnt.posX, tnt.posY + 1.2D, tnt.posZ, new ItemStack(Items.GOLDEN_APPLE, 3), rand);
                break;

            default:
                // Lucky gold & emerald pillar
                queueTreasurePillar(world, cx, cy, cz, 9);
                spawnXpBurst(world, tnt.posX, tnt.posY + 1.0D, tnt.posZ, 12, 20, rand);
                break;
        }
    }

    private static void triggerSuperLucky(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, Random rand)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.6F, 1.0F);
        triggerMineralConversion(world, cx, cy, cz, 11, rand);
        queueTreasurePillar(world, cx, cy, cz, 12);
        spawnTreasureShower(world, tnt.posX, tnt.posY + 1.5D, tnt.posZ, 26, rand);
        spawnXpBurst(world, tnt.posX, tnt.posY + 1.5D, tnt.posZ, 16, 25, rand);
    }

    private static void triggerMineralConversion(World world, int cx, int cy, int cz, final int radius, final Random rand)
    {
        final int rSq = radius * radius;
        final IBlockState[] ores = new IBlockState[] {
            Blocks.COAL_ORE.getDefaultState(),
            Blocks.IRON_ORE.getDefaultState(),
            Blocks.IRON_ORE.getDefaultState(),
            Blocks.GOLD_ORE.getDefaultState(),
            Blocks.REDSTONE_ORE.getDefaultState(),
            Blocks.LAPIS_ORE.getDefaultState(),
            Blocks.DIAMOND_ORE.getDefaultState(),
            Blocks.EMERALD_ORE.getDefaultState()
        };

        ArgonExplosionScheduler.VolumeJob job = new ArgonExplosionScheduler.VolumeJob(
            cx, cy, cz,
            -radius, radius,
            -radius, radius,
            -radius, radius,
            new ArgonExplosionScheduler.VolumeCellProcessor()
            {
                @Override
                public boolean processCell(World w, BlockPos.MutableBlockPos pos, int dx, int dy, int dz)
                {
                    if (dx * dx + dy * dy + dz * dz > rSq)
                    {
                        return false;
                    }

                    IBlockState state = w.getBlockState(pos);
                    Block b = state.getBlock();

                    if (b == Blocks.STONE || b == Blocks.DIRT || b == Blocks.GRAVEL || b == Blocks.COBBLESTONE || b == Blocks.NETHERRACK)
                    {
                        if (rand.nextInt(100) < 42)
                        {
                            w.setBlockState(pos, ores[rand.nextInt(ores.length)], 2);
                            return true;
                        }
                    }

                    return false;
                }
            }
        );

        ArgonExplosionScheduler.queueVolumeJob(world, job);
    }

    private static void triggerInstantHouse(World world, int cx, int cy, int cz)
    {
        int halfW = 4;
        int height = 5;
        IBlockState floor = Blocks.COBBLESTONE.getDefaultState();
        IBlockState planks = Blocks.PLANKS.getDefaultState();
        IBlockState log = Blocks.LOG.getDefaultState();
        IBlockState glass = Blocks.GLASS.getDefaultState();
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState torch = Blocks.TORCH.getDefaultState();

        for (int dy = 0; dy <= height + 1; ++dy)
        {
            for (int dx = -halfW; dx <= halfW; ++dx)
            {
                for (int dz = -halfW; dz <= halfW; ++dz)
                {
                    int x = cx + dx;
                    int y = cy + dy;
                    int z = cz + dz;

                    if (dy == 0)
                    {
                        ArgonExplosionScheduler.queueBlockChange(world, x, y, z, floor, 2);
                    }
                    else if (dy == height)
                    {
                        ArgonExplosionScheduler.queueBlockChange(world, x, y, z, planks, 2);
                    }
                    else if (dy == height + 1)
                    {
                        if (Math.abs(dx) < halfW && Math.abs(dz) < halfW)
                        {
                            ArgonExplosionScheduler.queueBlockChange(world, x, y, z, log, 2);
                        }
                        else
                        {
                            ArgonExplosionScheduler.queueBlockChange(world, x, y, z, air, 2);
                        }
                    }
                    else
                    {
                        boolean edgeX = Math.abs(dx) == halfW;
                        boolean edgeZ = Math.abs(dz) == halfW;

                        if (edgeX && edgeZ)
                        {
                            ArgonExplosionScheduler.queueBlockChange(world, x, y, z, log, 2);
                        }
                        else if (edgeX || edgeZ)
                        {
                            // Front entrance opening at dz == halfW, dx == 0
                            if (dz == halfW && dx == 0 && (dy == 1 || dy == 2))
                            {
                                ArgonExplosionScheduler.queueBlockChange(world, x, y, z, air, 2);
                            }
                            else if (dy == 2 && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1))
                            {
                                ArgonExplosionScheduler.queueBlockChange(world, x, y, z, glass, 2);
                            }
                            else
                            {
                                ArgonExplosionScheduler.queueBlockChange(world, x, y, z, planks, 2);
                            }
                        }
                        else
                        {
                            ArgonExplosionScheduler.queueBlockChange(world, x, y, z, air, 2);
                        }
                    }
                }
            }
        }

        // Interior furnishings
        ArgonExplosionScheduler.queueBlockChange(world, cx - 3, cy + 1, cz - 3, Blocks.CRAFTING_TABLE.getDefaultState(), 3);
        ArgonExplosionScheduler.queueBlockChange(world, cx - 2, cy + 1, cz - 3, Blocks.FURNACE.getDefaultState(), 3);
        ArgonExplosionScheduler.queueBlockChange(world, cx + 3, cy + 1, cz - 3, Blocks.CHEST.getDefaultState(), 3);
        ArgonExplosionScheduler.queueBlockChange(world, cx - 3, cy + 1, cz + 3, torch, 3);
        ArgonExplosionScheduler.queueBlockChange(world, cx + 3, cy + 1, cz + 3, torch, 3);
        ArgonExplosionScheduler.queueBlockChange(world, cx, cy + 4, cz, Blocks.GLOWSTONE.getDefaultState(), 3);
    }

    private static void queueRainbowDome(World world, int cx, int cy, int cz, final int radius)
    {
        final EnumDyeColor[] colors = EnumDyeColor.values();
        final int rSq = radius * radius;
        final int innerSq = (radius - 1) * (radius - 1);

        ArgonExplosionScheduler.VolumeJob job = new ArgonExplosionScheduler.VolumeJob(
            cx, cy, cz,
            -radius, radius,
            0, radius,
            -radius, radius,
            new ArgonExplosionScheduler.VolumeCellProcessor()
            {
                @Override
                public boolean processCell(World w, BlockPos.MutableBlockPos pos, int dx, int dy, int dz)
                {
                    int distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq <= rSq && distSq >= innerSq)
                    {
                        EnumDyeColor color = colors[(dy + Math.abs(dx + dz)) % colors.length];
                        IBlockState wool = Blocks.WOOL.getDefaultState().withProperty(BlockColored.COLOR, color);
                        w.setBlockState(pos, wool, 2);
                        return true;
                    }

                    if (dy == 0 && distSq < innerSq)
                    {
                        w.setBlockState(pos, Blocks.GOLD_BLOCK.getDefaultState(), 2);
                        return true;
                    }

                    return false;
                }
            }
        );

        ArgonExplosionScheduler.queueVolumeJob(world, job);
        ArgonExplosionScheduler.queueBlockChange(world, cx, cy + radius - 1, cz, Blocks.GLOWSTONE.getDefaultState(), 3);
    }

    private static void queueTreasurePillar(World world, int cx, int cy, int cz, int height)
    {
        for (int dy = 0; dy < height; ++dy)
        {
            IBlockState blockState;

            if (dy == height - 1)
            {
                blockState = Blocks.BEACON.getDefaultState();
            }
            else if (dy % 3 == 0)
            {
                blockState = Blocks.DIAMOND_BLOCK.getDefaultState();
            }
            else if (dy % 3 == 1)
            {
                blockState = Blocks.EMERALD_BLOCK.getDefaultState();
            }
            else
            {
                blockState = Blocks.GOLD_BLOCK.getDefaultState();
            }

            ArgonExplosionScheduler.queueBlockChange(world, cx, cy + dy, cz, blockState, 2);
        }
    }

    private static void spawnTreasureShower(World world, double x, double y, double z, int drops, Random rand)
    {
        Item[] loot = new Item[] {
            Items.DIAMOND,
            Items.EMERALD,
            Items.GOLD_INGOT,
            Items.IRON_INGOT,
            Items.GOLDEN_APPLE,
            Items.ENDER_PEARL,
            Items.EXPERIENCE_BOTTLE,
            Items.DIAMOND_SWORD,
            Items.DIAMOND_PICKAXE
        };

        for (int i = 0; i < drops; ++i)
        {
            Item chosen = loot[rand.nextInt(loot.length)];
            int count = (chosen == Items.DIAMOND_SWORD || chosen == Items.DIAMOND_PICKAXE) ? 1 : (1 + rand.nextInt(3));
            spawnDrop(world, x, y, z, new ItemStack(chosen, count), rand);
        }
    }

    private static void spawnDrop(World world, double x, double y, double z, ItemStack stack, Random rand)
    {
        EntityItem item = new EntityItem(world, x, y, z, stack);
        item.motionX = (rand.nextDouble() - 0.5D) * 0.55D;
        item.motionY = 0.35D + rand.nextDouble() * 0.35D;
        item.motionZ = (rand.nextDouble() - 0.5D) * 0.55D;
        item.setDefaultPickupDelay();
        ArgonExplosionScheduler.queueEntitySpawn(world, item);
    }

    private static void spawnXpBurst(World world, double x, double y, double z, int orbs, int xpPerOrb, Random rand)
    {
        for (int i = 0; i < orbs; ++i)
        {
            EntityXPOrb orb = new EntityXPOrb(
                world,
                x + (rand.nextDouble() - 0.5D) * 2.0D,
                y + rand.nextDouble(),
                z + (rand.nextDouble() - 0.5D) * 2.0D,
                xpPerOrb
            );
            ArgonExplosionScheduler.queueEntitySpawn(world, orb);
        }
    }

    // -------------------------------------------------------------------------
    // TOO MUCH TNT SERIES
    // -------------------------------------------------------------------------

    private static void triggerSphericalCrater(
        World world,
        EntityArgonTNTPrimed tnt,
        int cx,
        int cy,
        int cz,
        final int radius,
        final boolean igniteEdge,
        final boolean scorchedRim,
        final Random rand)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, Math.min(4.0F, 1.4F + radius * 0.1F), 0.72F + rand.nextFloat() * 0.25F);
        applyShockwave(world, tnt, (double)radius * 1.35D, 14.0F + radius * 0.65F);

        final int rSq = radius * radius;
        final int rimMinSq = (radius - 2) * (radius - 2);
        final EntityLivingBase igniter = tnt.getTntPlacedBy();

        ArgonExplosionScheduler.VolumeJob job = new ArgonExplosionScheduler.VolumeJob(
            cx, cy, cz,
            -radius, radius,
            -radius, radius,
            -radius, radius,
            new ArgonExplosionScheduler.VolumeCellProcessor()
            {
                @Override
                public boolean processCell(World w, BlockPos.MutableBlockPos pos, int dx, int dy, int dz)
                {
                    int distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq > rSq)
                    {
                        return false;
                    }

                    // Organic jagged crater rim
                    if (distSq > rimMinSq && rand.nextInt(100) < 28)
                    {
                        return false;
                    }

                    IBlockState state = w.getBlockState(pos);
                    Block block = state.getBlock();

                    if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.BARRIER)
                    {
                        return false;
                    }

                    if (tryChainIgnite(w, pos, block, igniter, rand))
                    {
                        return true;
                    }

                    if (scorchedRim && distSq >= rimMinSq && dy <= 0)
                    {
                        IBlockState rimState = rand.nextInt(3) == 0 ? Blocks.OBSIDIAN.getDefaultState() : Blocks.NETHERRACK.getDefaultState();
                        w.setBlockState(pos, rimState, 2);
                        return true;
                    }

                    w.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);

                    if (igniteEdge && dy <= 0 && distSq >= rimMinSq - radius && rand.nextInt(24) == 0)
                    {
                        BlockPos below = pos.down();
                        if (w.isBlockLoaded(below) && w.getBlockState(below).isFullCube())
                        {
                            w.setBlockState(pos, Blocks.FIRE.getDefaultState(), 2);
                        }
                    }

                    return true;
                }
            }
        );

        ArgonExplosionScheduler.queueVolumeJob(world, job);
    }

    private static void triggerNuclearBlast(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, int radius, Random rand)
    {
        triggerSphericalCrater(world, tnt, cx, cy, cz, radius, true, true, rand);

        // Staggered lightning discharges around ground zero
        for (int i = 0; i < 6; ++i)
        {
            double ang = (Math.PI * 2.0D * i) / 6.0D;
            double dist = 4.0D + rand.nextDouble() * 10.0D;
            EntityLightningBolt bolt = new EntityLightningBolt(world, tnt.posX + Math.cos(ang) * dist, tnt.posY, tnt.posZ + Math.sin(ang) * dist, true);
            ArgonExplosionScheduler.queueEntitySpawn(world, bolt);
        }
    }

    private static void triggerMeteorImpact(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, final int radius, final Random rand)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 3.2F, 0.55F);
        applyShockwave(world, tnt, (double)radius * 1.4D, 24.0F);

        final int rSq = radius * radius;
        final int coreSq = 25; // radius 5 molten meteor core
        final EntityLivingBase igniter = tnt.getTntPlacedBy();

        ArgonExplosionScheduler.VolumeJob job = new ArgonExplosionScheduler.VolumeJob(
            cx, cy, cz,
            -radius, radius,
            -radius, radius,
            -radius, radius,
            new ArgonExplosionScheduler.VolumeCellProcessor()
            {
                @Override
                public boolean processCell(World w, BlockPos.MutableBlockPos pos, int dx, int dy, int dz)
                {
                    int distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq > rSq)
                    {
                        return false;
                    }

                    IBlockState state = w.getBlockState(pos);
                    Block block = state.getBlock();

                    if (block == Blocks.BEDROCK || block == Blocks.BARRIER)
                    {
                        return false;
                    }

                    if (distSq <= coreSq)
                    {
                        int roll = rand.nextInt(10);
                        IBlockState meteorState;

                        if (roll < 4)
                        {
                            meteorState = Blocks.OBSIDIAN.getDefaultState();
                        }
                        else if (roll < 7)
                        {
                            meteorState = Blocks.MAGMA.getDefaultState();
                        }
                        else if (roll < 9)
                        {
                            meteorState = Blocks.NETHERRACK.getDefaultState();
                        }
                        else
                        {
                            meteorState = Blocks.GOLD_ORE.getDefaultState();
                        }

                        w.setBlockState(pos, meteorState, 2);
                        return true;
                    }

                    if (block == Blocks.AIR)
                    {
                        return false;
                    }

                    if (tryChainIgnite(w, pos, block, igniter, rand))
                    {
                        return true;
                    }

                    if (dy <= -(radius / 2) && rand.nextInt(4) == 0)
                    {
                        w.setBlockState(pos, Blocks.LAVA.getDefaultState(), 2);
                        return true;
                    }

                    w.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                    return true;
                }
            }
        );

        ArgonExplosionScheduler.queueVolumeJob(world, job);
    }

    private static void triggerOceanBasin(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, final int radius)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 2.0F, 0.9F);
        final int rSq = radius * radius;
        final int depth = Math.max(6, radius / 2);

        ArgonExplosionScheduler.VolumeJob job = new ArgonExplosionScheduler.VolumeJob(
            cx, cy, cz,
            -radius, radius,
            -depth, 3,
            -radius, radius,
            new ArgonExplosionScheduler.VolumeCellProcessor()
            {
                @Override
                public boolean processCell(World w, BlockPos.MutableBlockPos pos, int dx, int dy, int dz)
                {
                    int horizSq = dx * dx + dz * dz;

                    if (horizSq > rSq)
                    {
                        return false;
                    }

                    IBlockState state = w.getBlockState(pos);
                    Block block = state.getBlock();

                    if (block == Blocks.BEDROCK || block == Blocks.BARRIER)
                    {
                        return false;
                    }

                    if (dy > 0)
                    {
                        if (block != Blocks.AIR)
                        {
                            w.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                            return true;
                        }

                        return false;
                    }

                    // Parabolic bowl filled with still water
                    double norm = (double)horizSq / (double)rSq;
                    int bowlDepth = -(int)((1.0D - norm) * (double)depth);

                    if (dy >= bowlDepth)
                    {
                        if (block != Blocks.WATER)
                        {
                            w.setBlockState(pos, Blocks.WATER.getDefaultState(), 2);
                            return true;
                        }
                    }

                    return false;
                }
            }
        );

        ArgonExplosionScheduler.queueVolumeJob(world, job);
    }

    private static void triggerFlatClear(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, final int radius)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 2.0F, 1.05F);
        final int rSq = radius * radius;
        final EntityLivingBase igniter = tnt.getTntPlacedBy();

        ArgonExplosionScheduler.VolumeJob job = new ArgonExplosionScheduler.VolumeJob(
            cx, cy, cz,
            -radius, radius,
            0, Math.min(18, radius),
            -radius, radius,
            new ArgonExplosionScheduler.VolumeCellProcessor()
            {
                @Override
                public boolean processCell(World w, BlockPos.MutableBlockPos pos, int dx, int dy, int dz)
                {
                    if (dx * dx + dz * dz > rSq)
                    {
                        return false;
                    }

                    IBlockState state = w.getBlockState(pos);
                    Block block = state.getBlock();

                    if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.BARRIER)
                    {
                        return false;
                    }

                    if (tryChainIgnite(w, pos, block, igniter, w.rand))
                    {
                        return true;
                    }

                    w.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                    return true;
                }
            }
        );

        ArgonExplosionScheduler.queueVolumeJob(world, job);
    }

    private static void triggerMiningFlat(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, final int radius)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.8F, 1.15F);
        final int rSq = radius * radius;

        ArgonExplosionScheduler.VolumeJob job = new ArgonExplosionScheduler.VolumeJob(
            cx, cy, cz,
            -radius, radius,
            0, 5,
            -radius, radius,
            new ArgonExplosionScheduler.VolumeCellProcessor()
            {
                @Override
                public boolean processCell(World w, BlockPos.MutableBlockPos pos, int dx, int dy, int dz)
                {
                    if (dx * dx + dz * dz > rSq)
                    {
                        return false;
                    }

                    Block block = w.getBlockState(pos).getBlock();

                    if (block == Blocks.STONE
                        || block == Blocks.DIRT
                        || block == Blocks.GRASS
                        || block == Blocks.GRAVEL
                        || block == Blocks.SAND
                        || block == Blocks.SANDSTONE
                        || block == Blocks.COBBLESTONE
                        || block == Blocks.NETHERRACK)
                    {
                        w.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                        return true;
                    }

                    return false;
                }
            }
        );

        ArgonExplosionScheduler.queueVolumeJob(world, job);
    }

    private static void triggerClusterBlast(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, Random rand)
    {
        triggerSphericalCrater(world, tnt, cx, cy, cz, 5, false, false, rand);
        int subCharges = 14;

        for (int i = 0; i < subCharges; ++i)
        {
            double angle = (Math.PI * 2.0D * i) / (double)subCharges + rand.nextDouble() * 0.2D;
            double speed = 0.45D + rand.nextDouble() * 0.45D;
            EntityArgonTNTPrimed sub = new EntityArgonTNTPrimed(
                world,
                tnt.posX,
                tnt.posY + 0.6D,
                tnt.posZ,
                tnt.getTntPlacedBy(),
                TntType.TNT_X5
            );
            sub.motionX = Math.cos(angle) * speed;
            sub.motionY = 0.55D + rand.nextDouble() * 0.35D;
            sub.motionZ = Math.sin(angle) * speed;
            sub.setFuse(28 + rand.nextInt(26));
            ArgonExplosionScheduler.queueEntitySpawn(world, sub);
        }
    }

    // -------------------------------------------------------------------------
    // UNLUCKY TNT SERIES
    // -------------------------------------------------------------------------

    private static void triggerUnluckyEvent(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, Random rand)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 1.3F, 0.85F);
        int roll = rand.nextInt(5);

        switch (roll)
        {
            case 0:
                // Cobweb & Obsidian prison cage
                queueCobwebPrison(world, cx, cy, cz, 4);
                break;

            case 1:
                // Falling Anvil Rain from Y + 14
                for (int i = 0; i < 16; ++i)
                {
                    double ox = cx + (rand.nextInt(11) - 5) + 0.5D;
                    double oz = cz + (rand.nextInt(11) - 5) + 0.5D;
                    EntityFallingBlock anvil = new EntityFallingBlock(world, ox, (double)(cy + 14 + rand.nextInt(5)), oz, Blocks.ANVIL.getDefaultState());
                    anvil.setHurtEntities(true);
                    ArgonExplosionScheduler.queueEntitySpawn(world, anvil);
                }

                world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 1.6F, 0.7F);
                break;

            case 2:
                // Lava Trap
                triggerLavaTrap(world, tnt, cx, cy, cz, 8, rand);
                break;

            case 3:
                // Lightning Storm
                for (int i = 0; i < 10; ++i)
                {
                    double lx = tnt.posX + (rand.nextDouble() - 0.5D) * 14.0D;
                    double lz = tnt.posZ + (rand.nextDouble() - 0.5D) * 14.0D;
                    EntityLightningBolt bolt = new EntityLightningBolt(world, lx, tnt.posY, lz, false);
                    ArgonExplosionScheduler.queueEntitySpawn(world, bolt);
                }
                break;

            default:
                // Hostile Mob & Soldier Ambush
                triggerMobSwarm(world, tnt, cx, cy, cz, 16, rand);
                break;
        }
    }

    private static void queueCobwebPrison(World world, int cx, int cy, int cz, int half)
    {
        for (int dy = 0; dy <= 4; ++dy)
        {
            for (int dx = -half; dx <= half; ++dx)
            {
                for (int dz = -half; dz <= half; ++dz)
                {
                    boolean shell = Math.abs(dx) == half || Math.abs(dz) == half || dy == 0 || dy == 4;
                    IBlockState state = shell ? Blocks.OBSIDIAN.getDefaultState() : Blocks.WEB.getDefaultState();
                    ArgonExplosionScheduler.queueBlockChange(world, cx + dx, cy + dy, cz + dz, state, 2);
                }
            }
        }
    }

    private static void triggerCursedNuke(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, final int radius, final Random rand)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 2.2F, 0.65F);
        applyShockwave(world, tnt, (double)radius * 1.3D, 20.0F);

        final int rSq = radius * radius;
        final int innerCraterSq = (radius / 2) * (radius / 2);

        ArgonExplosionScheduler.VolumeJob job = new ArgonExplosionScheduler.VolumeJob(
            cx, cy, cz,
            -radius, radius,
            -radius / 2, radius / 2,
            -radius, radius,
            new ArgonExplosionScheduler.VolumeCellProcessor()
            {
                @Override
                public boolean processCell(World w, BlockPos.MutableBlockPos pos, int dx, int dy, int dz)
                {
                    int distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq > rSq)
                    {
                        return false;
                    }

                    IBlockState state = w.getBlockState(pos);
                    Block block = state.getBlock();

                    if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.BARRIER)
                    {
                        return false;
                    }

                    if (distSq <= innerCraterSq)
                    {
                        w.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                        return true;
                    }

                    int pick = rand.nextInt(10);
                    IBlockState corrupt = pick < 5
                        ? Blocks.NETHERRACK.getDefaultState()
                        : (pick < 8 ? Blocks.SOUL_SAND.getDefaultState() : Blocks.OBSIDIAN.getDefaultState());
                    w.setBlockState(pos, corrupt, 2);

                    if (rand.nextInt(14) == 0)
                    {
                        BlockPos above = pos.up();
                        if (w.isBlockLoaded(above) && w.isAirBlock(above))
                        {
                            w.setBlockState(above, Blocks.FIRE.getDefaultState(), 2);
                        }
                    }

                    return true;
                }
            }
        );

        ArgonExplosionScheduler.queueVolumeJob(world, job);

        // Queue Wither Skeletons, Blazes & Lightning bolts through scheduler
        for (int i = 0; i < 12; ++i)
        {
            double ang = (Math.PI * 2.0D * i) / 12.0D;
            double dist = 4.0D + rand.nextDouble() * 6.0D;
            double mx = tnt.posX + Math.cos(ang) * dist;
            double mz = tnt.posZ + Math.sin(ang) * dist;
            Entity mob = (i % 3 == 0) ? new EntityBlaze(world) : new EntityWitherSkeleton(world);
            mob.setLocationAndAngles(mx, tnt.posY + 2.0D, mz, rand.nextFloat() * 360.0F, 0.0F);
            ArgonExplosionScheduler.queueEntitySpawn(world, mob);
        }

        for (int i = 0; i < 6; ++i)
        {
            double lx = tnt.posX + (rand.nextDouble() - 0.5D) * 18.0D;
            double lz = tnt.posZ + (rand.nextDouble() - 0.5D) * 18.0D;
            ArgonExplosionScheduler.queueEntitySpawn(world, new EntityLightningBolt(world, lx, tnt.posY, lz, false));
        }
    }

    private static void triggerLavaTrap(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, final int radius, final Random rand)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.8F, 0.65F);
        final int rSq = radius * radius;
        final int lavaSq = (radius - 2) * (radius - 2);

        ArgonExplosionScheduler.VolumeJob job = new ArgonExplosionScheduler.VolumeJob(
            cx, cy, cz,
            -radius, radius,
            -4, 2,
            -radius, radius,
            new ArgonExplosionScheduler.VolumeCellProcessor()
            {
                @Override
                public boolean processCell(World w, BlockPos.MutableBlockPos pos, int dx, int dy, int dz)
                {
                    int horizSq = dx * dx + dz * dz;

                    if (horizSq > rSq)
                    {
                        return false;
                    }

                    Block block = w.getBlockState(pos).getBlock();

                    if (block == Blocks.BEDROCK || block == Blocks.BARRIER)
                    {
                        return false;
                    }

                    if (dy > 0)
                    {
                        if (block != Blocks.AIR)
                        {
                            w.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                            return true;
                        }

                        return false;
                    }

                    if (horizSq <= lavaSq && dy >= -3)
                    {
                        w.setBlockState(pos, Blocks.LAVA.getDefaultState(), 2);
                        return true;
                    }

                    if (dy == 0)
                    {
                        w.setBlockState(pos, Blocks.MAGMA.getDefaultState(), 2);
                        return true;
                    }

                    return false;
                }
            }
        );

        ArgonExplosionScheduler.queueVolumeJob(world, job);

        for (int i = 0; i < 6; ++i)
        {
            EntityMagmaCube cube = new EntityMagmaCube(world);
            double ang = (Math.PI * 2.0D * i) / 6.0D;
            cube.setLocationAndAngles(tnt.posX + Math.cos(ang) * 4.0D, tnt.posY + 2.0D, tnt.posZ + Math.sin(ang) * 4.0D, rand.nextFloat() * 360.0F, 0.0F);
            ArgonExplosionScheduler.queueEntitySpawn(world, cube);
        }
    }

    private static void triggerMobSwarm(World world, EntityArgonTNTPrimed tnt, int cx, int cy, int cz, int count, Random rand)
    {
        world.playSound((EntityPlayer)null, tnt.posX, tnt.posY, tnt.posZ, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 1.5F, 1.1F);

        for (int i = 0; i < count; ++i)
        {
            double ang = (Math.PI * 2.0D * i) / (double)count + rand.nextDouble() * 0.3D;
            double dist = 2.5D + rand.nextDouble() * 6.5D;
            double mx = (double)cx + 0.5D + Math.cos(ang) * dist;
            double mz = (double)cz + 0.5D + Math.sin(ang) * dist;
            double my = (double)cy + 1.2D;

            Entity mob;
            int pick = rand.nextInt(6);

            switch (pick)
            {
                case 0:
                    mob = new EntityZombie(world);
                    break;
                case 1:
                    mob = new EntityCreeper(world);
                    break;
                case 2:
                    mob = new EntitySkeleton(world);
                    break;
                case 3:
                    mob = new EntitySpider(world);
                    break;
                case 4:
                    mob = new EntityWitherSkeleton(world);
                    break;
                default:
                    mob = new EntityArgonSoldier(world);
                    break;
            }

            mob.setLocationAndAngles(mx, my, mz, rand.nextFloat() * 360.0F, 0.0F);
            ArgonExplosionScheduler.queueEntitySpawn(world, mob);
        }
    }

    // -------------------------------------------------------------------------
    // SHARED HELPERS
    // -------------------------------------------------------------------------

    private static boolean tryChainIgnite(World world, BlockPos pos, Block block, EntityLivingBase igniter, Random rand)
    {
        if (block instanceof BlockArgonTNT)
        {
            BlockArgonTNT argonTnt = (BlockArgonTNT)block;
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
            EntityArgonTNTPrimed chain = new EntityArgonTNTPrimed(
                world,
                (double)pos.getX() + 0.5D,
                (double)pos.getY(),
                (double)pos.getZ() + 0.5D,
                igniter,
                argonTnt.getTntType()
            );
            int baseFuse = Math.max(16, argonTnt.getTntType().fuseTicks);
            chain.setFuse(rand.nextInt(baseFuse / 4) + baseFuse / 8);
            ArgonExplosionScheduler.queueEntitySpawn(world, chain);
            return true;
        }

        if (block instanceof BlockTNT)
        {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
            EntityTNTPrimed chain = new EntityTNTPrimed(
                world,
                (double)pos.getX() + 0.5D,
                (double)pos.getY(),
                (double)pos.getZ() + 0.5D,
                igniter
            );
            chain.setFuse((short)(rand.nextInt(20) + 10));
            ArgonExplosionScheduler.queueEntitySpawn(world, chain);
            return true;
        }

        return false;
    }

    private static void applyShockwave(World world, EntityArgonTNTPrimed tnt, double range, float maxDamage)
    {
        AxisAlignedBB box = new AxisAlignedBB(
            tnt.posX - range, tnt.posY - range, tnt.posZ - range,
            tnt.posX + range, tnt.posY + range, tnt.posZ + range
        );
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(tnt, box);
        DamageSource source = DamageSource.causeExplosionDamage(tnt.getTntPlacedBy());

        for (int i = 0; i < entities.size(); ++i)
        {
            Entity e = entities.get(i);

            if (e.isImmuneToExplosions())
            {
                continue;
            }

            double dx = e.posX - tnt.posX;
            double dy = (e.posY + (double)e.getEyeHeight()) - tnt.posY;
            double dz = e.posZ - tnt.posZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < range && dist > 0.001D)
            {
                double falloff = 1.0D - (dist / range);
                float dmg = (float)(falloff * falloff * (double)maxDamage);

                if (dmg > 0.5F)
                {
                    e.attackEntityFrom(source, dmg);
                }

                double knock = falloff * 1.4D;
                e.motionX += (dx / dist) * knock;
                e.motionY += Math.max(0.2D, (dy / dist) * knock);
                e.motionZ += (dz / dist) * knock;
            }
        }
    }
}
