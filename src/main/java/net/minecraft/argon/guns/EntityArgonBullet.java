package net.minecraft.argon.guns;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * High-velocity firearm projectile extending EntityThrowable.
 * Supports ballistic bullets, shotgun pellets, .50 cal armor-piercing rounds,
 * explosive rockets, Techguns plasma bolts, and Auto Turret tracer rounds.
 * Includes global active projectile cap for 60 FPS on Kompanio 500 hardware.
 */
public class EntityArgonBullet extends EntityThrowable
{
    public enum BulletKind
    {
        BALLISTIC(0.008F, 60, false, false, 0xFFE680),
        PELLET(0.015F, 28, false, false, 0xFFBB55),
        SNIPER(0.003F, 90, false, true, 0xFFF3B0),
        ROCKET(0.004F, 110, true, false, 0xFF6B2B),
        PLASMA(0.001F, 70, false, true, 0x2CE8FF),
        TURRET_ROUND(0.006F, 50, false, false, 0xFF3B3B);

        public final float gravity;
        public final int maxLifeTicks;
        public final boolean explosive;
        public final boolean armorPiercing;
        public final int tracerColor;

        BulletKind(float gravity, int maxLifeTicks, boolean explosive, boolean armorPiercing, int tracerColor)
        {
            this.gravity = gravity;
            this.maxLifeTicks = maxLifeTicks;
            this.explosive = explosive;
            this.armorPiercing = armorPiercing;
            this.tracerColor = tracerColor;
        }

        public static BulletKind byId(int id)
        {
            BulletKind[] vals = values();
            return id >= 0 && id < vals.length ? vals[id] : BALLISTIC;
        }
    }

    private static final DataParameter<Integer> BULLET_KIND = EntityDataManager.<Integer>createKey(EntityArgonBullet.class, DataSerializers.VARINT);
    private static final int MAX_ACTIVE_BULLETS = 96;
    private static int activeBulletCount = 0;

    private float damage = 6.0F;
    private boolean fromTurret = false;
    private boolean countedActive = false;

    public EntityArgonBullet(World worldIn)
    {
        super(worldIn);
        this.setSize(0.18F, 0.18F);
        this.registerActiveSlot();
    }

    public EntityArgonBullet(World worldIn, double x, double y, double z)
    {
        super(worldIn, x, y, z);
        this.setSize(0.18F, 0.18F);
        this.registerActiveSlot();
    }

    public EntityArgonBullet(World worldIn, double x, double y, double z, BulletKind kind, float damage, boolean fromTurret)
    {
        super(worldIn, x, y, z);
        this.setSize(kind == BulletKind.ROCKET ? 0.32F : 0.18F, kind == BulletKind.ROCKET ? 0.32F : 0.18F);
        this.setBulletKind(kind);
        this.damage = damage;
        this.fromTurret = fromTurret;
        this.registerActiveSlot();
    }

    public EntityArgonBullet(World worldIn, EntityLivingBase throwerIn, BulletKind kind, float damage)
    {
        super(worldIn, throwerIn);
        this.setSize(kind == BulletKind.ROCKET ? 0.32F : 0.18F, kind == BulletKind.ROCKET ? 0.32F : 0.18F);
        this.setBulletKind(kind);
        this.damage = damage;
        this.registerActiveSlot();
    }

    private void registerActiveSlot()
    {
        if (activeBulletCount >= MAX_ACTIVE_BULLETS)
        {
            this.setDead();
            return;
        }

        activeBulletCount++;
        this.countedActive = true;
    }

    @Override
    public void setDead()
    {
        if (this.countedActive)
        {
            this.countedActive = false;

            if (activeBulletCount > 0)
            {
                activeBulletCount--;
            }
        }

        super.setDead();
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(BULLET_KIND, Integer.valueOf(BulletKind.BALLISTIC.ordinal()));
    }

    public BulletKind getBulletKind()
    {
        return BulletKind.byId(this.dataManager.get(BULLET_KIND).intValue());
    }

    public void setBulletKind(BulletKind kind)
    {
        this.dataManager.set(BULLET_KIND, Integer.valueOf(kind != null ? kind.ordinal() : 0));
    }

    public void setDamage(float damage)
    {
        this.damage = damage;
    }

    public float getDamage()
    {
        return this.damage;
    }

    @Override
    protected float getGravityVelocity()
    {
        return this.getBulletKind().gravity;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance)
    {
        return distance < 128.0D * 128.0D;
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        if (this.isDead)
        {
            return;
        }

        BulletKind kind = this.getBulletKind();

        if (this.ticksExisted > kind.maxLifeTicks)
        {
            this.setDead();
            return;
        }

        // Throttled lightweight trail particles to maintain 60 FPS on Kompanio 500
        if (this.world.isRemote)
        {
            if (kind == BulletKind.ROCKET)
            {
                this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX, this.posY, this.posZ, -this.motionX * 0.08D, -this.motionY * 0.08D, -this.motionZ * 0.08D);

                if ((this.ticksExisted & 1) == 0)
                {
                    this.world.spawnParticle(EnumParticleTypes.FLAME, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
                }
            }
            else if (kind == BulletKind.PLASMA && (this.ticksExisted & 1) == 0)
            {
                this.world.spawnParticle(EnumParticleTypes.END_ROD, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
            }
            else if (kind == BulletKind.SNIPER && (this.ticksExisted & 1) == 0)
            {
                this.world.spawnParticle(EnumParticleTypes.CRIT, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onImpact(RayTraceResult result)
    {
        if (this.isDead)
        {
            return;
        }

        BulletKind kind = this.getBulletKind();
        EntityLivingBase shooter = this.getThrower();

        if (result.entityHit != null)
        {
            if (result.entityHit == shooter)
            {
                return;
            }

            if (this.fromTurret && result.entityHit instanceof EntityPlayer)
            {
                return;
            }

            if (!this.world.isRemote)
            {
                float dealtDamage = this.damage;
                boolean headshot = false;

                if (result.entityHit instanceof EntityLivingBase && kind != BulletKind.ROCKET)
                {
                    EntityLivingBase target = (EntityLivingBase)result.entityHit;
                    double headThreshold = target.posY + (double)target.getEyeHeight() - 0.35D;

                    if (this.posY >= headThreshold)
                    {
                        dealtDamage *= 1.5F;
                        headshot = true;
                    }
                }

                DamageSource src = new GunDamageSource("argon_gun", this, shooter != null ? shooter : this, kind.armorPiercing);

                result.entityHit.hurtResistantTime = 0;
                result.entityHit.attackEntityFrom(src, dealtDamage);

                if (kind == BulletKind.PLASMA)
                {
                    result.entityHit.setFire(3);
                }

                if (headshot && shooter instanceof EntityPlayer)
                {
                    this.world.playSound((EntityPlayer)null, shooter.posX, shooter.posY, shooter.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.45F, 1.85F);
                }
            }
        }
        else if (result.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            BlockPos hitPos = result.getBlockPos();
            IBlockState state = this.world.getBlockState(hitPos);
            Block block = state.getBlock();

            // Break fragile glass panes/blocks on high-energy impact
            if (!this.world.isRemote && (block == Blocks.GLASS || block == Blocks.GLASS_PANE || block == Blocks.STAINED_GLASS || block == Blocks.STAINED_GLASS_PANE))
            {
                this.world.destroyBlock(hitPos, false);
            }

            if (this.world.isRemote)
            {
                double hx = result.hitVec != null ? result.hitVec.x : this.posX;
                double hy = result.hitVec != null ? result.hitVec.y : this.posY;
                double hz = result.hitVec != null ? result.hitVec.z : this.posZ;
                EnumParticleTypes pType = kind == BulletKind.PLASMA ? EnumParticleTypes.CRIT_MAGIC : EnumParticleTypes.SMOKE_NORMAL;

                for (int i = 0; i < 3; ++i)
                {
                    this.world.spawnParticle(pType, hx, hy, hz, (this.rand.nextDouble() - 0.5D) * 0.15D, this.rand.nextDouble() * 0.12D, (this.rand.nextDouble() - 0.5D) * 0.15D);
                }
            }

            if (state.getMaterial() == Material.IRON && !this.world.isRemote)
            {
                this.world.playSound((EntityPlayer)null, this.posX, this.posY, this.posZ, SoundEvents.BLOCK_METAL_HIT, SoundCategory.BLOCKS, 0.4F, 1.6F);
            }
        }

        if (kind.explosive)
        {
            this.detonateRocket(shooter);
        }

        this.setDead();
    }

    private void detonateRocket(EntityLivingBase shooter)
    {
        if (this.world.isRemote)
        {
            this.world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
            return;
        }

        this.world.playSound((EntityPlayer)null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.2F, 0.85F + this.rand.nextFloat() * 0.2F);

        // Tactical blast splash damage without tearing up chunk meshes (Kompanio 500 friendly)
        double radius = 4.75D;
        AxisAlignedBB blastBox = new AxisAlignedBB(
            this.posX - radius, this.posY - radius, this.posZ - radius,
            this.posX + radius, this.posY + radius, this.posZ + radius
        );
        List<Entity> targets = this.world.getEntitiesWithinAABBExcludingEntity(this, blastBox);

        for (int i = 0; i < targets.size(); ++i)
        {
            Entity e = targets.get(i);

            if (e instanceof EntityLivingBase)
            {
                double dist = Math.sqrt(e.getDistanceSq(this.posX, this.posY, this.posZ));

                if (dist <= radius)
                {
                    float falloff = (float)(1.0D - (dist / radius) * 0.72D);
                    float blastDmg = 18.0F * falloff;

                    if (e == shooter)
                    {
                        blastDmg *= 0.45F;
                    }

                    DamageSource src = new EntityDamageSourceIndirect("argon_rocket", this, shooter != null ? shooter : this).setExplosion();
                    e.attackEntityFrom(src, blastDmg);

                    double dx = e.posX - this.posX;
                    double dz = e.posZ - this.posZ;
                    double len = MathHelper.sqrt(dx * dx + dz * dz);

                    if (len > 0.001D)
                    {
                        e.motionX += (dx / len) * 0.55D * (double)falloff;
                        e.motionY += 0.32D * (double)falloff;
                        e.motionZ += (dz / len) * 0.55D * (double)falloff;
                    }
                }
            }
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("ArgonBulletKind", this.getBulletKind().ordinal());
        compound.setFloat("ArgonBulletDamage", this.damage);
        compound.setBoolean("ArgonFromTurret", this.fromTurret);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.setBulletKind(BulletKind.byId(compound.getInteger("ArgonBulletKind")));

        if (compound.hasKey("ArgonBulletDamage"))
        {
            this.damage = compound.getFloat("ArgonBulletDamage");
        }

        this.fromTurret = compound.getBoolean("ArgonFromTurret");
    }

    private static final class GunDamageSource extends EntityDamageSourceIndirect
    {
        GunDamageSource(String damageTypeIn, Entity source, Entity indirectEntityIn, boolean armorPiercing)
        {
            super(damageTypeIn, source, indirectEntityIn);
            this.setProjectile();
            if (armorPiercing)
            {
                this.setDamageBypassesArmor();
            }
        }
    }
}
