package net.minecraft.argon.tnt;

import javax.annotation.Nullable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

/**
 * Primed TNT entity for Argon Lucky / Too Much / Unlucky TNT.
 * Extends EntityTNTPrimed and routes detonation through ArgonExplosionScheduler
 * via TntEffects so low-spec devices maintain 60 FPS even during massive blasts.
 */
public class EntityArgonTNTPrimed extends EntityTNTPrimed
{
    private static final DataParameter<Integer> TNT_TYPE = EntityDataManager.<Integer>createKey(EntityArgonTNTPrimed.class, DataSerializers.VARINT);

    private int argonFuse;

    public EntityArgonTNTPrimed(World worldIn)
    {
        super(worldIn);
        this.argonFuse = 80;
    }

    public EntityArgonTNTPrimed(World worldIn, double x, double y, double z, @Nullable EntityLivingBase igniter, TntType type)
    {
        super(worldIn, x, y, z, igniter);
        TntType resolved = type != null ? type : TntType.LUCKY_TNT;
        this.setTntType(resolved);
        this.setFuse(resolved.fuseTicks);
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(TNT_TYPE, Integer.valueOf(0));
    }

    public TntType getTntType()
    {
        Integer val = this.dataManager.get(TNT_TYPE);
        return TntType.byOrdinal(val != null ? val.intValue() : 0);
    }

    public void setTntType(TntType type)
    {
        TntType resolved = type != null ? type : TntType.LUCKY_TNT;
        this.dataManager.set(TNT_TYPE, Integer.valueOf(resolved.ordinal()));
    }

    @Override
    public void setFuse(int fuseIn)
    {
        super.setFuse(fuseIn);
        this.argonFuse = fuseIn;
    }

    @Override
    public int getFuse()
    {
        return this.argonFuse;
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key)
    {
        super.notifyDataManagerChange(key);
        this.argonFuse = super.getFuse();
    }

    @Override
    public void onUpdate()
    {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (!this.hasNoGravity())
        {
            this.motionY -= 0.03999999910593033D;
        }

        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9800000190734863D;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= 0.9800000190734863D;

        if (this.onGround)
        {
            this.motionX *= 0.699999988079071D;
            this.motionZ *= 0.699999988079071D;
            this.motionY *= -0.5D;
        }

        this.argonFuse--;

        if (this.argonFuse <= 0)
        {
            this.setDead();

            if (!this.world.isRemote)
            {
                TntEffects.detonate(this.world, this, this.getTntType());
            }
            else
            {
                TntEffects.spawnClientDetonationBurst(this.posX, this.posY + 0.5D, this.posZ, this.getTntType());
            }
        }
        else
        {
            this.handleWaterMovement();
            TntType type = this.getTntType();

            if (type.category == TntType.Category.LUCKY)
            {
                this.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, this.posX, this.posY + 0.7D, this.posZ, 0.0D, 0.05D, 0.0D);
            }
            else if (type.category == TntType.Category.UNLUCKY)
            {
                this.world.spawnParticle(EnumParticleTypes.PORTAL, this.posX, this.posY + 0.6D, this.posZ, (this.rand.nextDouble() - 0.5D) * 0.2D, 0.08D, (this.rand.nextDouble() - 0.5D) * 0.2D);
            }
            else
            {
                this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX, this.posY + 0.6D, this.posZ, 0.0D, 0.04D, 0.0D);
            }
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setShort("Fuse", (short)this.argonFuse);
        compound.setInteger("ArgonTntType", this.getTntType().ordinal());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.setFuse(compound.getShort("Fuse"));

        if (compound.hasKey("ArgonTntType"))
        {
            this.setTntType(TntType.byOrdinal(compound.getInteger("ArgonTntType")));
        }
    }
}
