package net.minecraft.world.biome;

import java.util.Random;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockNewLeaf;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTallGrass;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraft.world.gen.feature.WorldGenerator;

public class BiomeArgonScenic extends Biome
{
    public static enum ScenicType
    {
        CHERRY_BLOSSOM,
        AUTUMN_MAPLE,
        ALPINE_PEAKS,
        GRAND_CANYON;
    }

    private static final WorldGenBirchTree CHERRY_BIRCH_TREE = new WorldGenBirchTree(false, false);
    private static final WorldGenBigTree GRAND_CANOPY_TREE = new WorldGenBigTree(false);
    private static final WorldGenTaiga1 ALPINE_PINE_1 = new WorldGenTaiga1();
    private static final WorldGenTaiga2 ALPINE_PINE_2 = new WorldGenTaiga2(false);

    private final ScenicType scenicType;

    public BiomeArgonScenic(ScenicType typeIn, Biome.BiomeProperties properties)
    {
        super(properties);
        this.scenicType = typeIn;

        if (typeIn == ScenicType.CHERRY_BLOSSOM)
        {
            this.topBlock = Blocks.GRASS.getDefaultState();
            this.fillerBlock = Blocks.DIRT.getDefaultState();
            this.decorator.treesPerChunk = 8;
            this.decorator.flowersPerChunk = 16;
            this.decorator.grassPerChunk = 6;
            this.spawnableCreatureList.add(new Biome.SpawnListEntry(EntityRabbit.class, 8, 2, 3));
        }
        else if (typeIn == ScenicType.AUTUMN_MAPLE)
        {
            this.topBlock = Blocks.GRASS.getDefaultState();
            this.fillerBlock = Blocks.DIRT.getDefaultState();
            this.decorator.treesPerChunk = 10;
            this.decorator.flowersPerChunk = 4;
            this.decorator.grassPerChunk = 8;
            this.decorator.mushroomsPerChunk = 4;
            this.spawnableCreatureList.add(new Biome.SpawnListEntry(EntityWolf.class, 6, 2, 4));
            this.spawnableCreatureList.add(new Biome.SpawnListEntry(EntityRabbit.class, 6, 2, 3));
        }
        else if (typeIn == ScenicType.ALPINE_PEAKS)
        {
            this.topBlock = Blocks.GRASS.getDefaultState();
            this.fillerBlock = Blocks.STONE.getDefaultState();
            this.decorator.treesPerChunk = 3;
            this.decorator.flowersPerChunk = 1;
            this.decorator.grassPerChunk = 2;
            this.spawnableCreatureList.add(new Biome.SpawnListEntry(EntityLlama.class, 8, 2, 4));
            this.spawnableCreatureList.add(new Biome.SpawnListEntry(EntityRabbit.class, 6, 2, 3));
        }
        else if (typeIn == ScenicType.GRAND_CANYON)
        {
            this.topBlock = Blocks.STAINED_HARDENED_CLAY.getDefaultState();
            this.fillerBlock = Blocks.HARDENED_CLAY.getDefaultState();
            this.decorator.treesPerChunk = 1;
            this.decorator.deadBushPerChunk = 10;
            this.decorator.cactiPerChunk = 3;
            this.decorator.grassPerChunk = 2;
        }
    }

    public ScenicType getScenicType()
    {
        return this.scenicType;
    }

    public WorldGenAbstractTree getRandomTreeFeature(Random rand)
    {
        if (this.scenicType == ScenicType.CHERRY_BLOSSOM)
        {
            return (WorldGenAbstractTree)(rand.nextInt(3) == 0 ? GRAND_CANOPY_TREE : CHERRY_BIRCH_TREE);
        }
        else if (this.scenicType == ScenicType.AUTUMN_MAPLE)
        {
            if (rand.nextInt(4) == 0)
            {
                return GRAND_CANOPY_TREE;
            }
            else
            {
                return (WorldGenAbstractTree)(rand.nextInt(3) == 0 ? ALPINE_PINE_2 : TREE_FEATURE);
            }
        }
        else if (this.scenicType == ScenicType.ALPINE_PEAKS)
        {
            return (WorldGenAbstractTree)(rand.nextInt(2) == 0 ? ALPINE_PINE_1 : ALPINE_PINE_2);
        }
        else
        {
            return TREE_FEATURE;
        }
    }

    public WorldGenerator getRandomWorldGenForGrass(Random rand)
    {
        if (this.scenicType == ScenicType.AUTUMN_MAPLE && rand.nextInt(3) == 0)
        {
            return new WorldGenTallGrass(BlockTallGrass.EnumType.FERN);
        }

        return new WorldGenTallGrass(BlockTallGrass.EnumType.GRASS);
    }

    public BlockFlower.EnumFlowerType pickRandomFlower(Random rand, BlockPos pos)
    {
        if (this.scenicType == ScenicType.CHERRY_BLOSSOM)
        {
            int r = rand.nextInt(4);

            if (r == 0)
            {
                return BlockFlower.EnumFlowerType.PINK_TULIP;
            }
            else if (r == 1)
            {
                return BlockFlower.EnumFlowerType.ALLIUM;
            }
            else if (r == 2)
            {
                return BlockFlower.EnumFlowerType.WHITE_TULIP;
            }
            else
            {
                return BlockFlower.EnumFlowerType.HOUSTONIA;
            }
        }
        else if (this.scenicType == ScenicType.AUTUMN_MAPLE)
        {
            return rand.nextInt(2) == 0 ? BlockFlower.EnumFlowerType.ORANGE_TULIP : BlockFlower.EnumFlowerType.RED_TULIP;
        }

        return super.pickRandomFlower(rand, pos);
    }

    public int getGrassColorAtPos(BlockPos pos)
    {
        if (this.scenicType == ScenicType.CHERRY_BLOSSOM)
        {
            return 0x76D275;
        }
        else if (this.scenicType == ScenicType.AUTUMN_MAPLE)
        {
            double n = GRASS_COLOR_NOISE.getValue((double)pos.getX() * 0.035D, (double)pos.getZ() * 0.035D);
            return n > 0.0D ? 0xD89632 : 0xB88228;
        }
        else if (this.scenicType == ScenicType.ALPINE_PEAKS)
        {
            return pos.getY() > 130 ? 0x8AB89E : 0x62A86C;
        }
        else
        {
            return 0xC49A45;
        }
    }

    public int getFoliageColorAtPos(BlockPos pos)
    {
        if (this.scenicType == ScenicType.CHERRY_BLOSSOM)
        {
            double n = GRASS_COLOR_NOISE.getValue((double)pos.getX() * 0.05D, (double)pos.getZ() * 0.05D);
            return n > 0.15D ? 0xFFB2D6 : (n < -0.15D ? 0xF48FB1 : 0xFFA4C8);
        }
        else if (this.scenicType == ScenicType.AUTUMN_MAPLE)
        {
            double n = GRASS_COLOR_NOISE.getValue((double)pos.getX() * 0.04D, (double)pos.getZ() * 0.04D);

            if (n > 0.25D)
            {
                return 0xE53935; // Crimson maple
            }
            else if (n > -0.2D)
            {
                return 0xFB8C00; // Bright autumn orange
            }
            else
            {
                return 0xFDD835; // Golden ginkgo yellow
            }
        }
        else if (this.scenicType == ScenicType.ALPINE_PEAKS)
        {
            return 0x4E8C6F;
        }
        else
        {
            return super.getFoliageColorAtPos(pos);
        }
    }

    public int getSkyColorByTemp(float currentTemperature)
    {
        if (this.scenicType == ScenicType.CHERRY_BLOSSOM)
        {
            return 0x8CC8FF;
        }
        else if (this.scenicType == ScenicType.ALPINE_PEAKS)
        {
            return 0x6BA8FF;
        }

        return super.getSkyColorByTemp(currentTemperature);
    }

    public void genTerrainBlocks(World worldIn, Random rand, ChunkPrimer chunkPrimerIn, int x, int z, double noiseVal)
    {
        if (this.scenicType == ScenicType.AUTUMN_MAPLE)
        {
            this.topBlock = Blocks.GRASS.getDefaultState();
            this.fillerBlock = Blocks.DIRT.getDefaultState();

            if (noiseVal > 1.35D)
            {
                this.topBlock = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL);
            }
            else if (noiseVal < -1.4D)
            {
                this.topBlock = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
            }

            this.generateBiomeTerrain(worldIn, rand, chunkPrimerIn, x, z, noiseVal);
        }
        else if (this.scenicType == ScenicType.ALPINE_PEAKS)
        {
            this.topBlock = Blocks.GRASS.getDefaultState();
            this.fillerBlock = Blocks.DIRT.getDefaultState();

            if (noiseVal > 1.0D)
            {
                this.topBlock = Blocks.STONE.getDefaultState();
                this.fillerBlock = Blocks.STONE.getDefaultState();
            }

            this.generateBiomeTerrain(worldIn, rand, chunkPrimerIn, x, z, noiseVal);

            // Altitude-stratified Alpine snow & glacier ice caps
            int localX = z & 15;
            int localZ = x & 15;

            for (int y = 255; y >= 95; --y)
            {
                IBlockState state = chunkPrimerIn.getBlockState(localX, y, localZ);

                if (state.getMaterial() != Material.AIR)
                {
                    if (y >= 165)
                    {
                        chunkPrimerIn.setBlockState(localX, y, localZ, Blocks.SNOW.getDefaultState());

                        if (y - 1 >= 0)
                        {
                            chunkPrimerIn.setBlockState(localX, y - 1, localZ, Blocks.PACKED_ICE.getDefaultState());
                        }
                    }
                    else if (y >= 132)
                    {
                        chunkPrimerIn.setBlockState(localX, y, localZ, noiseVal > 0.2D ? Blocks.SNOW.getDefaultState() : Blocks.STONE.getDefaultState());
                    }
                    else if (y >= 108 && noiseVal > 0.6D)
                    {
                        chunkPrimerIn.setBlockState(localX, y, localZ, Blocks.GRAVEL.getDefaultState());
                    }

                    break;
                }
            }
        }
        else if (this.scenicType == ScenicType.GRAND_CANYON)
        {
            this.topBlock = Blocks.SAND.getDefaultState();
            this.fillerBlock = Blocks.HARDENED_CLAY.getDefaultState();
            this.generateBiomeTerrain(worldIn, rand, chunkPrimerIn, x, z, noiseVal);
        }
        else
        {
            super.genTerrainBlocks(worldIn, rand, chunkPrimerIn, x, z, noiseVal);
        }
    }

    public void decorate(World worldIn, Random rand, BlockPos pos)
    {
        if (this.scenicType == ScenicType.CHERRY_BLOSSOM)
        {
            DOUBLE_PLANT_GENERATOR.setPlantType(BlockDoublePlant.EnumPlantType.PAEONIA);

            for (int i = 0; i < 4; ++i)
            {
                int dx = rand.nextInt(16) + 8;
                int dz = rand.nextInt(16) + 8;
                int dy = rand.nextInt(worldIn.getHeight(pos.add(dx, 0, dz)).getY() + 32);
                DOUBLE_PLANT_GENERATOR.generate(worldIn, rand, pos.add(dx, dy, dz));
            }
        }

        super.decorate(worldIn, rand, pos);
    }
}
