package net.minecraft.argon.guns;

import javax.annotation.Nullable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

/**
 * Techguns-style Hostile Armed NPC Soldier.
 * Variants:
 *   0 = Assault Trooper (M4A1 Assault Rifle, burst fire)
 *   1 = Breacher Commando (M870 Combat Shotgun, close-range pressure)
 *   2 = Heavy Trooper (M134 Minigun / AT4 Rocket Launcher, high armor)
 */
public class EntityArgonSoldier extends EntityMob
{
    public static final int VARIANT_ASSAULT = 0;
    public static final int VARIANT_BREACHER = 1;
    public static final int VARIANT_HEAVY = 2;

    private static final DataParameter<Integer> SOLDIER_VARIANT = EntityDataManager.<Integer>createKey(EntityArgonSoldier.class, DataSerializers.VARINT);

    public EntityArgonSoldier(World worldIn)
    {
        super(worldIn);
        this.setSize(0.6F, 1.95F);
        this.experienceValue = 12;
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(SOLDIER_VARIANT, Integer.valueOf(VARIANT_ASSAULT));
    }

    public int getSoldierVariant()
    {
        return this.dataManager.get(SOLDIER_VARIANT).intValue();
    }

    public void setSoldierVariant(int variant)
    {
        int clamped = variant < 0 ? 0 : (variant > 2 ? 2 : variant);
        this.dataManager.set(SOLDIER_VARIANT, Integer.valueOf(clamped));
        this.applyVariantLoadout(clamped);
    }

    @Override
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(26.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(6.0D);
    }

    @Override
    protected void initEntityAI()
    {
        this.tasks.addTask(1, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIGunCombat(this));
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.85D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 12.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata)
    {
        IEntityLivingData data = super.onInitialSpawn(difficulty, livingdata);
        int roll = this.rand.nextInt(10);
        int variant = roll < 5 ? VARIANT_ASSAULT : (roll < 8 ? VARIANT_BREACHER : VARIANT_HEAVY);
        this.setSoldierVariant(variant);
        return data;
    }

    private void applyVariantLoadout(int variant)
    {
        if (variant == VARIANT_BREACHER)
        {
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(28.0D);
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.31D);
            this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(8.0D);
            this.setHealth(28.0F);
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(GunItems.SHOTGUN_12G));
        }
        else if (variant == VARIANT_HEAVY)
        {
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(42.0D);
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23D);
            this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(12.0D);
            this.setHealth(42.0F);
            ItemArgonGun heavyWeapon = this.rand.nextBoolean() ? GunItems.MINIGUN : GunItems.PLASMA_RIFLE;
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(heavyWeapon));
        }
        else
        {
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(24.0D);
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28D);
            this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(6.0D);
            this.setHealth(24.0F);
            ItemStack rifle = new ItemStack(GunItems.ASSAULT_RIFLE);
            GunAttachment.setAttachmentIndex(rifle, GunAttachment.SLOT_SCOPE, GunAttachment.ScopeType.REFLEX.ordinal());
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, rifle);
        }
    }

    public void fireWeaponAt(EntityLivingBase target)
    {
        if (this.world.isRemote || target == null)
        {
            return;
        }

        ItemStack held = this.getHeldItemMainhand();
        GunType type = (held.getItem() instanceof ItemArgonGun) ? ((ItemArgonGun)held.getItem()).getGunType() : GunType.ASSAULT_RIFLE;

        double dx = target.posX - this.posX;
        double dy = (target.posY + (double)target.getEyeHeight() * 0.72D) - (this.posY + (double)this.getEyeHeight() - 0.1D);
        double dz = target.posZ - this.posZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.001D)
        {
            return;
        }

        float dmgScale = 0.55F;
        int pellets = type == GunType.SHOTGUN_12G ? 5 : 1;
        float spread = type == GunType.SHOTGUN_12G ? 9.5F : 4.2F;
        float speed = Math.min(3.2F, type.projectileSpeed);

        for (int i = 0; i < pellets; ++i)
        {
            EntityArgonBullet bullet = new EntityArgonBullet(this.world, this, type.bulletKind, type.baseDamage * dmgScale);
            bullet.setThrowableHeading(dx / dist, dy / dist, dz / dist, speed, spread);
            this.world.spawnEntity(bullet);
        }

        this.world.playSound(
            (EntityPlayer)null,
            this.posX,
            this.posY,
            this.posZ,
            type == GunType.PLASMA_RIFLE ? SoundEvents.ENTITY_WITHER_SHOOT : SoundEvents.ENTITY_FIREWORK_BLAST_FAR,
            SoundCategory.HOSTILE,
            0.95F,
            type == GunType.SHOTGUN_12G ? 0.75F : 1.25F
        );
    }

    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        super.dropFewItems(wasRecentlyHit, lootingModifier);

        int variant = this.getSoldierVariant();
        ItemGunAmmo ammoDrop = variant == VARIANT_BREACHER ? GunItems.AMMO_12G : (variant == VARIANT_HEAVY ? GunItems.AMMO_PLASMA_CELL : GunItems.AMMO_556);
        this.dropItem(ammoDrop, 1 + this.rand.nextInt(3 + lootingModifier));

        if (this.rand.nextInt(5) == 0)
        {
            ItemGunAttachment[] pool = new ItemGunAttachment[] {
                GunItems.ATTACH_SCOPE_REFLEX,
                GunItems.ATTACH_SCOPE_4X,
                GunItems.ATTACH_SILENCER,
                GunItems.ATTACH_COMPENSATOR,
                GunItems.ATTACH_EXTENDED_MAG,
                GunItems.ATTACH_FOREGRIP,
                GunItems.ATTACH_LASER
            };
            this.dropItem(pool[this.rand.nextInt(pool.length)], 1);
        }
    }

    @Override
    public void onDeath(DamageSource cause)
    {
        super.onDeath(cause);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("ArgonSoldierVariant", this.getSoldierVariant());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("ArgonSoldierVariant"))
        {
            this.dataManager.set(SOLDIER_VARIANT, Integer.valueOf(compound.getInteger("ArgonSoldierVariant")));
        }
    }

    /**
     * Tactical ranged combat AI: closes to optimal distance, strafes while sighting,
     * and fires controlled bursts with fair cooldowns.
     */
    private static final class EntityAIGunCombat extends EntityAIBase
    {
        private final EntityArgonSoldier soldier;
        private int attackCooldown = 20;
        private int burstRemaining = 0;
        private int strafeTimer = 0;
        private boolean strafeLeft = false;

        EntityAIGunCombat(EntityArgonSoldier soldier)
        {
            this.soldier = soldier;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute()
        {
            EntityLivingBase target = this.soldier.getAttackTarget();
            return target != null && target.isEntityAlive();
        }

        @Override
        public void resetTask()
        {
            this.burstRemaining = 0;
            this.attackCooldown = 16;
            this.soldier.getNavigator().clearPathEntity();
        }

        @Override
        public void updateTask()
        {
            EntityLivingBase target = this.soldier.getAttackTarget();

            if (target == null)
            {
                return;
            }

            double distSq = this.soldier.getDistanceSq(target.posX, target.getEntityBoundingBox().minY, target.posZ);
            boolean canSee = this.soldier.getEntitySenses().canSee(target);
            int variant = this.soldier.getSoldierVariant();
            double preferredDist = variant == VARIANT_BREACHER ? 9.0D : (variant == VARIANT_HEAVY ? 17.0D : 14.0D);

            this.soldier.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

            if (!canSee || distSq > (preferredDist + 4.0D) * (preferredDist + 4.0D))
            {
                this.soldier.getNavigator().tryMoveToEntityLiving(target, 1.05D);
            }
            else
            {
                this.soldier.getNavigator().clearPathEntity();
                this.strafeTimer++;

                if (this.strafeTimer >= 24)
                {
                    this.strafeTimer = 0;
                    this.strafeLeft = !this.strafeLeft;
                }

                float moveForward = distSq < (preferredDist * 0.5D) * (preferredDist * 0.5D) ? -0.45F : 0.0F;
                float moveStrafe = this.strafeLeft ? 0.55F : -0.55F;
                this.soldier.getMoveHelper().strafe(moveForward, moveStrafe);
                this.soldier.faceEntity(target, 30.0F, 30.0F);
            }

            if (this.attackCooldown > 0)
            {
                this.attackCooldown--;
                return;
            }

            if (canSee && distSq <= 28.0D * 28.0D)
            {
                if (this.burstRemaining <= 0)
                {
                    this.burstRemaining = variant == VARIANT_BREACHER ? 1 : (variant == VARIANT_HEAVY ? 5 : 3);
                }

                this.soldier.fireWeaponAt(target);
                this.burstRemaining--;

                if (this.burstRemaining > 0)
                {
                    this.attackCooldown = variant == VARIANT_HEAVY ? 3 : 4;
                }
                else
                {
                    this.attackCooldown = variant == VARIANT_BREACHER ? 28 : (variant == VARIANT_HEAVY ? 22 : 24);
                }
            }
        }
    }
}
