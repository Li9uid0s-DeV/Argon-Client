package net.minecraft.entity.effect;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.src.Config;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityWeatherStorm extends EntityWeatherEffect
{
    public static class LiftedBlockDebris
    {
        public final IBlockState blockState;
        public float orbitRadius;
        public float heightOffset;
        public float orbitAngle;
        public float angularVelocity;
        public float tumbleYaw;
        public float tumblePitch;
        public int age;
        public final int maxAge;

        public LiftedBlockDebris(IBlockState state, float orbitRadius, float heightOffset, float orbitAngle, float angularVelocity, int maxAge)
        {
            this.blockState = state;
            this.orbitRadius = orbitRadius;
            this.heightOffset = heightOffset;
            this.orbitAngle = orbitAngle;
            this.angularVelocity = angularVelocity;
            this.tumbleYaw = orbitAngle * 57.295776F;
            this.tumblePitch = heightOffset * 12.0F;
            this.age = 0;
            this.maxAge = maxAge;
        }
    }

    private int efScale;
    private float stormIntensity;
    private float windHeading;
    private float windSpeed;
    private int maxAgeTicks;
    private float funnelRotation;
    private float prevFunnelRotation;
    private final List<LiftedBlockDebris> liftedDebris = Lists.<LiftedBlockDebris>newArrayList();
    private final BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();

    public EntityWeatherStorm(World worldIn)
    {
        super(worldIn);
        this.setSize(6.0F, 64.0F);
        this.ignoreFrustumCheck = true;
        this.efScale = 1;
        this.stormIntensity = 0.65F;
        this.windHeading = worldIn != null ? worldIn.rand.nextFloat() * ((float)Math.PI * 2.0F) : 0.0F;
        this.windSpeed = 0.14F;
        this.maxAgeTicks = 2400;
    }

    public EntityWeatherStorm(World worldIn, double x, double y, double z, int efScaleIn)
    {
        this(worldIn);
        this.setPosition(x, y, z);
        this.setEfScale(efScaleIn);
        this.maxAgeTicks = 2400 + worldIn.rand.nextInt(2400);
    }

    protected void entityInit()
    {
    }

    @Override
    public boolean isLocalizedStorm()
    {
        return true;
    }

    @Override
    public int getEfScale()
    {
        return this.efScale;
    }

    public void setEfScale(int scale)
    {
        this.efScale = MathHelper.clamp(scale, 0, 7);
        this.stormIntensity = this.efScale == 0 ? 0.35F : MathHelper.clamp(0.30F + (float)this.efScale * 0.10F, 0.35F, 1.0F);
        this.windSpeed = 0.09F + (float)this.efScale * 0.022F;
        float radius = this.getStormRadius();
        this.setEntityBoundingBox(new AxisAlignedBB(this.posX - (double)radius, this.posY, this.posZ - (double)radius, this.posX + (double)radius, this.posY + (double)this.getFunnelHeight(), this.posZ + (double)radius));
    }

    @Override
    public float getStormIntensity()
    {
        return this.stormIntensity;
    }

    @Override
    public float getStormRadius()
    {
        return this.efScale <= 0 ? 18.0F : 8.0F + (float)this.efScale * 5.5F;
    }

    public float getFunnelBaseRadius()
    {
        return this.efScale <= 0 ? 2.5F : 1.4F + (float)this.efScale * 1.35F;
    }

    public float getFunnelTopRadius()
    {
        return this.efScale <= 0 ? 14.0F : 9.0F + (float)this.efScale * 4.5F;
    }

    public float getFunnelHeight()
    {
        return this.efScale <= 0 ? 42.0F : 46.0F + (float)this.efScale * 6.5F;
    }

    public float getWindHeadingDegrees()
    {
        float deg = (float)Math.toDegrees((double)this.windHeading);
        deg = deg % 360.0F;
        return deg < 0.0F ? deg + 360.0F : deg;
    }

    public String getEfLabel()
    {
        return this.efScale <= 0 ? "Supercell Storm" : "EF-" + this.efScale + " Tornado";
    }

    public float getInterpolatedRotation(float partialTicks)
    {
        return this.prevFunnelRotation + (this.funnelRotation - this.prevFunnelRotation) * partialTicks;
    }

    public List<LiftedBlockDebris> getLiftedDebris()
    {
        return this.liftedDebris;
    }

    public static int getMaxLiftedBlocks(int quality)
    {
        switch (quality)
        {
            case 0:
                return 0;

            case 1:
                return 8;

            case 3:
                return 64;

            case 2:
            default:
                return 24;
        }
    }

    public static int getMaxParticleSegments(int quality)
    {
        switch (quality)
        {
            case 0:
                return 0;

            case 1:
                return 24;

            case 3:
                return 140;

            case 2:
            default:
                return 64;
        }
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        int quality = Config.getWeather2Quality();

        if (quality <= 0)
        {
            this.liftedDebris.clear();
            this.setDead();
            return;
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevFunnelRotation = this.funnelRotation;

        float spinSpeed = 8.0F + (float)this.efScale * 3.8F;
        this.funnelRotation += spinSpeed;

        // Meandering storm trajectory
        this.windHeading += (this.rand.nextFloat() - 0.5F) * 0.035F;
        this.motionX = (double)(MathHelper.cos(this.windHeading) * this.windSpeed);
        this.motionZ = (double)(MathHelper.sin(this.windHeading) * this.windSpeed);
        this.posX += this.motionX;
        this.posZ += this.motionZ;

        // Snap storm base to surface height every 4 ticks
        if ((this.ticksExisted & 3) == 0 && this.world != null)
        {
            BlockPos topPos = this.world.getPrecipitationHeight(new BlockPos(this.posX, 0.0D, this.posZ));
            double targetY = (double)MathHelper.clamp(topPos.getY(), 48, 180);
            this.posY += (targetY - this.posY) * 0.25D;
        }

        float radius = this.getStormRadius();
        this.setEntityBoundingBox(new AxisAlignedBB(this.posX - (double)radius, this.posY, this.posZ - (double)radius, this.posX + (double)radius, this.posY + (double)this.getFunnelHeight(), this.posZ + (double)radius));

        // Update & prune lifted debris
        int maxDebris = getMaxLiftedBlocks(quality);

        while (this.liftedDebris.size() > maxDebris)
        {
            this.liftedDebris.remove(0);
        }

        float funnelH = this.getFunnelHeight();
        float baseR = this.getFunnelBaseRadius();
        float topR = this.getFunnelTopRadius();

        for (int i = 0; i < this.liftedDebris.size(); ++i)
        {
            LiftedBlockDebris debris = this.liftedDebris.get(i);
            ++debris.age;

            if (debris.age >= debris.maxAge || debris.heightOffset > funnelH)
            {
                this.liftedDebris.remove(i--);
                continue;
            }

            debris.orbitAngle += debris.angularVelocity;
            debris.heightOffset += 0.18F + (float)this.efScale * 0.04F;
            float hNorm = MathHelper.clamp(debris.heightOffset / funnelH, 0.0F, 1.0F);
            debris.orbitRadius = baseR + (topR - baseR) * (hNorm * hNorm) + MathHelper.sin((float)debris.age * 0.15F) * 0.6F;
            debris.tumbleYaw += 9.0F + (float)this.efScale * 2.0F;
            debris.tumblePitch += 6.5F + (float)this.efScale * 1.5F;
        }

        // Lift surface blocks into vortex (strictly capped by Kompanio 500 quality budget)
        if (this.efScale >= 1 && this.liftedDebris.size() < maxDebris)
        {
            int pickupInterval = quality == 1 ? 10 : (quality == 2 ? 5 : 3);

            if (this.ticksExisted % pickupInterval == 0)
            {
                this.tryLiftSurfaceBlock(maxDebris);
            }
        }

        // Apply vortex wind force to nearby entities
        if (this.efScale >= 1 && (this.ticksExisted & 1) == 0)
        {
            this.applyVortexPull(radius, funnelH);
        }

        // Spawn lightweight ground dust particles and storm audio cues
        if (this.world.isRemote)
        {
            this.updateClientEffects(quality);
        }

        // Occasional lightning strikes inside severe storms / EF-3+ tornadoes
        if (!this.world.isRemote && this.ticksExisted % 80 == 0 && this.rand.nextInt(Math.max(2, 9 - this.efScale)) == 0)
        {
            double lx = this.posX + (this.rand.nextDouble() - 0.5D) * (double)radius;
            double lz = this.posZ + (this.rand.nextDouble() - 0.5D) * (double)radius;
            BlockPos strikePos = this.world.getPrecipitationHeight(new BlockPos(lx, 0.0D, lz));
            this.world.addWeatherEffect(new EntityLightningBolt(this.world, (double)strikePos.getX(), (double)strikePos.getY(), (double)strikePos.getZ(), true));
        }

        if (this.ticksExisted >= this.maxAgeTicks)
        {
            this.setDead();
        }
    }

    private void tryLiftSurfaceBlock(int maxDebris)
    {
        if (this.world == null || this.liftedDebris.size() >= maxDebris)
        {
            return;
        }

        float grabRadius = this.getFunnelBaseRadius() * 2.2F;
        int ox = MathHelper.floor(this.posX + (double)((this.rand.nextFloat() - 0.5F) * 2.0F * grabRadius));
        int oz = MathHelper.floor(this.posZ + (double)((this.rand.nextFloat() - 0.5F) * 2.0F * grabRadius));
        BlockPos top = this.world.getTopSolidOrLiquidBlock(this.scanPos.setPos(ox, 0, oz));
        BlockPos surfacePos = top.down();

        if (surfacePos.getY() <= 4 || surfacePos.getY() >= 250)
        {
            return;
        }

        IBlockState state = this.world.getBlockState(surfacePos);
        Block block = state.getBlock();

        if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.BARRIER || block == Blocks.WEATHER_RADAR || block.hasTileEntity())
        {
            surfacePos = top;
            state = this.world.getBlockState(surfacePos);
            block = state.getBlock();
        }

        if (!this.canTornadoLiftBlock(state, surfacePos))
        {
            return;
        }

        float startAngle = this.rand.nextFloat() * ((float)Math.PI * 2.0F);
        float angVel = 0.14F + (float)this.efScale * 0.025F + this.rand.nextFloat() * 0.04F;
        int life = 100 + this.rand.nextInt(100);
        this.liftedDebris.add(new LiftedBlockDebris(state, this.getFunnelBaseRadius(), 0.5F + this.rand.nextFloat() * 2.5F, startAngle, angVel, life));

        // Scour soft foliage/sand/dirt on server side for EF-2+ tornadoes with low block update rate
        if (!this.world.isRemote && this.efScale >= 2 && (this.ticksExisted % 20 == 0) && this.world.getGameRules().getBoolean("mobGriefing"))
        {
            Material mat = state.getMaterial();

            if (mat == Material.LEAVES || mat == Material.PLANTS || mat == Material.VINE || mat == Material.SNOW || (this.efScale >= 4 && (block == Blocks.SAND || block == Blocks.GRAVEL || block == Blocks.DIRT || block == Blocks.GRASS)))
            {
                this.world.setBlockToAir(surfacePos);
            }
        }
    }

    private boolean canTornadoLiftBlock(IBlockState state, BlockPos pos)
    {
        Block block = state.getBlock();

        if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.WEATHER_RADAR || block.hasTileEntity() || state.getMaterial().isLiquid())
        {
            return false;
        }

        float hardness = state.getBlockHardness(this.world, pos);

        if (hardness < 0.0F)
        {
            return false;
        }

        float maxHardness = 0.3F + (float)this.efScale * 0.45F;

        if (this.efScale >= 6)
        {
            maxHardness = 6.0F;
        }

        return hardness <= maxHardness;
    }

    private void applyVortexPull(float radius, float funnelH)
    {
        AxisAlignedBB pullBox = new AxisAlignedBB(
            this.posX - (double)radius,
            this.posY - 6.0D,
            this.posZ - (double)radius,
            this.posX + (double)radius,
            this.posY + (double)funnelH,
            this.posZ + (double)radius
        );

        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, pullBox);

        for (int i = 0; i < list.size(); ++i)
        {
            Entity entity = list.get(i);

            if (entity instanceof EntityWeatherEffect)
            {
                continue;
            }

            if (entity instanceof EntityPlayer)
            {
                EntityPlayer player = (EntityPlayer)entity;

                if (player.isSpectator() || (player.capabilities.isCreativeMode && player.capabilities.isFlying))
                {
                    continue;
                }
            }

            double dx = this.posX - entity.posX;
            double dz = this.posZ - entity.posZ;
            double horizDist = Math.sqrt(dx * dx + dz * dz);

            if (horizDist < 0.001D || horizDist > (double)radius)
            {
                continue;
            }

            double normDist = 1.0D - (horizDist / (double)radius);
            double pullStrength = (0.025D + (double)this.efScale * 0.012D) * normDist;

            // Radial inward pull + strong tangential swirl
            double nx = dx / horizDist;
            double nz = dz / horizDist;
            double tx = -nz;
            double tz = nx;

            entity.motionX += (nx * 0.55D + tx * 0.85D) * pullStrength;
            entity.motionZ += (nz * 0.55D + tz * 0.85D) * pullStrength;

            if (horizDist < (double)(this.getFunnelBaseRadius() * 2.5F))
            {
                double lift = (0.045D + (double)this.efScale * 0.018D) * normDist;
                entity.motionY = MathHelper.clamp(entity.motionY + lift, -0.5D, 0.42D + (double)this.efScale * 0.05D);
                entity.fallDistance = 0.0F;
            }
        }
    }

    private void updateClientEffects(int quality)
    {
        int particleStep = quality == 1 ? 6 : (quality == 2 ? 3 : 1);

        if (this.ticksExisted % particleStep == 0)
        {
            float baseR = this.getFunnelBaseRadius();
            int count = quality == 1 ? 1 : (quality == 2 ? 2 : 4);

            for (int i = 0; i < count; ++i)
            {
                float angle = this.rand.nextFloat() * ((float)Math.PI * 2.0F);
                double px = this.posX + (double)(MathHelper.cos(angle) * baseR * (0.8F + this.rand.nextFloat() * 0.8F));
                double pz = this.posZ + (double)(MathHelper.sin(angle) * baseR * (0.8F + this.rand.nextFloat() * 0.8F));
                double py = this.posY + (double)(this.rand.nextFloat() * 6.0F);
                double vx = (double)(-MathHelper.sin(angle) * 0.35F);
                double vz = (double)(MathHelper.cos(angle) * 0.35F);
                this.world.spawnParticle(this.efScale >= 3 ? EnumParticleTypes.SMOKE_LARGE : EnumParticleTypes.CLOUD, px, py, pz, vx, 0.18D, vz);
            }
        }

        if (this.ticksExisted % 35 == 0)
        {
            float vol = 0.6F + (float)this.efScale * 0.22F;
            float pitch = 0.55F + (float)this.efScale * 0.04F;
            this.world.playSound(this.posX, this.posY + 8.0D, this.posZ, SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.WEATHER, vol, pitch, true);
        }
    }

    @Override
    public boolean isInRangeToRender3d(double x, double y, double z)
    {
        double dx = this.posX - x;
        double dz = this.posZ - z;
        return dx * dx + dz * dz <= 65536.0D;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound)
    {
        this.setEfScale(compound.getInteger("EfScale"));
        this.windHeading = compound.getFloat("WindHeading");
        this.ticksExisted = compound.getInteger("StormAge");

        if (compound.hasKey("MaxAge"))
        {
            this.maxAgeTicks = compound.getInteger("MaxAge");
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound)
    {
        compound.setInteger("EfScale", this.efScale);
        compound.setFloat("WindHeading", this.windHeading);
        compound.setInteger("StormAge", this.ticksExisted);
        compound.setInteger("MaxAge", this.maxAgeTicks);
    }
}
