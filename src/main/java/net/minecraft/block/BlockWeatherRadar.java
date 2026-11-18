package net.minecraft.block;

import java.util.List;
import java.util.Random;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityWeatherStorm;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.src.Config;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockWeatherRadar extends Block
{
    public BlockWeatherRadar()
    {
        super(Material.IRON, MapColor.DIAMOND);
        this.setHardness(3.0F);
        this.setResistance(15.0F);
        this.setSoundType(SoundType.METAL);
        this.setLightLevel(0.5F);
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setUnlocalizedName("weatherRadar");
    }

    @Override
    public String getLocalizedName()
    {
        return "Weather Radar (\u6c17\u8c61\u30ec\u30fc\u30c0\u30fc)";
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (hand != EnumHand.MAIN_HAND)
        {
            return true;
        }

        if (Config.getWeather2Quality() <= 0)
        {
            if (worldIn.isRemote)
            {
                playerIn.sendMessage(new TextComponentString("\u00a7c[Weather Radar] \u00a77Weather 2 is currently \u00a7cOFF\u00a77. Enable it in \u00a7bArgon Settings\u00a77 (Low / Medium / High)."));
            }

            return true;
        }

        EntityWeatherStorm nearestStorm = findNearestStorm(worldIn, (double)pos.getX() + 0.5D, (double)pos.getZ() + 0.5D, 512.0D);

        if (playerIn.isSneaking())
        {
            if (nearestStorm == null)
            {
                float yawRad = playerIn.rotationYaw * 0.017453292F;
                double spawnX = (double)pos.getX() - (double)(MathHelper.sin(yawRad) * 40.0F);
                double spawnZ = (double)pos.getZ() + (double)(MathHelper.cos(yawRad) * 40.0F);
                BlockPos topPos = worldIn.getPrecipitationHeight(new BlockPos(spawnX, 0.0D, spawnZ));
                EntityWeatherStorm newStorm = new EntityWeatherStorm(worldIn, spawnX, (double)topPos.getY(), spawnZ, 1);
                worldIn.addWeatherEffect(newStorm);
                worldIn.playSound(playerIn, pos, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 1.0F, 1.4F);

                if (worldIn.isRemote)
                {
                    playerIn.sendMessage(new TextComponentString("\u00a7b[Weather Radar] \u00a7eSpawned \u00a76EF-1 Tornado \u00a7eat (" + (int)spawnX + ", " + (int)spawnZ + "). Shift+Right-Click again to upgrade EF-2..EF-7."));
                }
            }
            else if (nearestStorm.getEfScale() < 7)
            {
                int nextEf = nearestStorm.getEfScale() + 1;
                nearestStorm.setEfScale(nextEf);
                worldIn.playSound(playerIn, pos, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 1.0F, 0.8F + (float)nextEf * 0.15F);

                if (worldIn.isRemote)
                {
                    String color = nextEf >= 5 ? "\u00a74\u00a7l" : (nextEf >= 3 ? "\u00a7c" : "\u00a76");
                    playerIn.sendMessage(new TextComponentString("\u00a7b[Weather Radar] \u00a7fUpgraded vortex to " + color + "EF-" + nextEf + " Tornado\u00a7f!"));
                }
            }
            else
            {
                int cleared = clearAllStorms(worldIn);
                worldIn.playSound(playerIn, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.9F, 1.2F);

                if (worldIn.isRemote)
                {
                    playerIn.sendMessage(new TextComponentString("\u00a7b[Weather Radar] \u00a7aDissipated " + cleared + " active storm(s). Skies cleared."));
                }
            }

            return true;
        }

        worldIn.playSound(playerIn, pos, SoundEvents.BLOCK_NOTE_HARP, SoundCategory.BLOCKS, 0.8F, 1.6F);

        if (worldIn.isRemote)
        {
            if (nearestStorm == null)
            {
                playerIn.sendMessage(new TextComponentString("\u00a7b[Weather Radar] \u00a7aNo active tornadoes within 512m. \u00a77(Shift+Right-Click radar to spawn/cycle EF-1~EF-7 Tornado)"));
            }
            else
            {
                double dx = nearestStorm.posX - ((double)pos.getX() + 0.5D);
                double dz = nearestStorm.posZ - ((double)pos.getZ() + 0.5D);
                int dist = MathHelper.floor(Math.sqrt(dx * dx + dz * dz));
                String dir = getCompassBearing(dx, dz);
                int ef = nearestStorm.getEfScale();
                String efColor = ef >= 5 ? "\u00a74\u00a7l" : (ef >= 3 ? "\u00a7c" : (ef >= 1 ? "\u00a76" : "\u00a7e"));
                int debrisCount = nearestStorm.getLiftedDebris().size();
                int maxDebris = EntityWeatherStorm.getMaxLiftedBlocks(Config.getWeather2Quality());

                playerIn.sendMessage(new TextComponentString(
                    "\u00a7b[Weather Radar] \u00a7fDetected: " + efColor + nearestStorm.getEfLabel()
                    + " \u00a77| Dist: \u00a7f" + dist + "m (" + dir + ")"
                    + " \u00a77| Pos: \u00a7f(" + (int)nearestStorm.posX + ", " + (int)nearestStorm.posZ + ")"
                    + " \u00a77| Debris: \u00a7b" + debrisCount + "/" + maxDebris
                ));
            }
        }

        return true;
    }

    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand)
    {
        EntityWeatherStorm nearest = findNearestStorm(worldIn, (double)pos.getX() + 0.5D, (double)pos.getZ() + 0.5D, 512.0D);
        double cx = (double)pos.getX() + 0.5D;
        double cy = (double)pos.getY() + 1.15D;
        double cz = (double)pos.getZ() + 0.5D;
        float sweepAngle = (float)(worldIn.getTotalWorldTime() % 40L) / 40.0F * ((float)Math.PI * 2.0F);
        double px = cx + (double)(MathHelper.cos(sweepAngle) * 0.35F);
        double pz = cz + (double)(MathHelper.sin(sweepAngle) * 0.35F);

        if (nearest != null && nearest.getEfScale() >= 1)
        {
            worldIn.spawnParticle(EnumParticleTypes.REDSTONE, px, cy, pz, 1.0D, 0.15D, 0.1D);
        }
        else
        {
            worldIn.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, px, cy, pz, 0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state)
    {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        EntityWeatherStorm nearest = findNearestStorm(worldIn, (double)pos.getX() + 0.5D, (double)pos.getZ() + 0.5D, 512.0D);

        if (nearest == null)
        {
            return 0;
        }

        return MathHelper.clamp(1 + nearest.getEfScale() * 2, 1, 15);
    }

    public static EntityWeatherStorm findNearestStorm(World worldIn, double x, double z, double maxRange)
    {
        if (worldIn == null)
        {
            return null;
        }

        List<Entity> effects = worldIn.weatherEffects;
        EntityWeatherStorm best = null;
        double bestDistSq = maxRange * maxRange;

        for (int i = 0; i < effects.size(); ++i)
        {
            Entity entity = effects.get(i);

            if (entity instanceof EntityWeatherStorm && !entity.isDead)
            {
                double dx = entity.posX - x;
                double dz = entity.posZ - z;
                double dSq = dx * dx + dz * dz;

                if (dSq <= bestDistSq)
                {
                    bestDistSq = dSq;
                    best = (EntityWeatherStorm)entity;
                }
            }
        }

        return best;
    }

    private static int clearAllStorms(World worldIn)
    {
        int count = 0;
        List<Entity> effects = worldIn.weatherEffects;

        for (int i = 0; i < effects.size(); ++i)
        {
            Entity entity = effects.get(i);

            if (entity instanceof EntityWeatherStorm)
            {
                entity.setDead();
                ++count;
            }
        }

        return count;
    }

    public static String getCompassBearing(double dx, double dz)
    {
        double angle = Math.toDegrees(Math.atan2(dz, dx));

        if (angle < 0.0D)
        {
            angle += 360.0D;
        }

        String[] dirs = new String[] {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
        int idx = (int)Math.round(angle / 45.0D) & 7;
        return dirs[idx];
    }
}
