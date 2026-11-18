package net.minecraft.argon.guns;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**
 * Automated Defense Turret TileEntity.
 * Scans for hostile mobs every 5 ticks (throttled for Kompanio 500 60 FPS),
 * smoothly rotates twin barrels toward the nearest visible target, and fires tracer rounds.
 */
public class TileEntityAutoTurret extends TileEntity implements ITickable
{
    public static final int MAX_AMMO = 300;
    private static final double RANGE = 24.0D;

    private String ownerName = "";
    private int ammoCount = 120;
    private boolean enabled = true;

    public float turretYaw = 0.0F;
    public float prevTurretYaw = 0.0F;
    public float turretPitch = 0.0F;
    public float prevTurretPitch = 0.0F;
    public boolean hasLockedTarget = false;
    public int muzzleFlashTicks = 0;

    private int scanCooldown = 0;
    private int fireCooldown = 0;
    private EntityLivingBase currentTarget = null;

    public void setOwnerName(String name)
    {
        this.ownerName = name != null ? name : "";
        this.markDirty();
    }

    public String getOwnerName()
    {
        return this.ownerName;
    }

    public int getAmmoCount()
    {
        return this.ammoCount;
    }

    public int addAmmo(int amount)
    {
        int before = this.ammoCount;
        this.ammoCount = Math.min(MAX_AMMO, this.ammoCount + amount);
        this.markDirty();
        return this.ammoCount - before;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void toggleEnabled()
    {
        this.enabled = !this.enabled;
        this.markDirty();
    }

    @Override
    public void update()
    {
        this.prevTurretYaw = this.turretYaw;
        this.prevTurretPitch = this.turretPitch;

        if (this.muzzleFlashTicks > 0)
        {
            this.muzzleFlashTicks--;
        }

        if (this.world == null)
        {
            return;
        }

        if (!this.enabled || this.ammoCount <= 0)
        {
            this.hasLockedTarget = false;
            this.currentTarget = null;
            this.turretPitch *= 0.85F;
            return;
        }

        if (this.fireCooldown > 0)
        {
            this.fireCooldown--;
        }

        // Throttled target acquisition (every 5 ticks) for low CPU overhead
        if (this.scanCooldown <= 0 || this.currentTarget == null || !this.isValidTarget(this.currentTarget))
        {
            this.currentTarget = this.findNearestHostile();
            this.scanCooldown = 5;
        }
        else
        {
            this.scanCooldown--;
        }

        if (this.currentTarget != null)
        {
            this.hasLockedTarget = true;
            double ox = (double)this.pos.getX() + 0.5D;
            double oy = (double)this.pos.getY() + 0.72D;
            double oz = (double)this.pos.getZ() + 0.5D;

            double tx = this.currentTarget.posX;
            double ty = this.currentTarget.posY + (double)this.currentTarget.getEyeHeight() * 0.75D;
            double tz = this.currentTarget.posZ;

            double dx = tx - ox;
            double dy = ty - oy;
            double dz = tz - oz;
            double horiz = Math.sqrt(dx * dx + dz * dz);

            float targetYaw = (float)(MathHelper.atan2(-dx, dz) * (180.0D / Math.PI));
            float targetPitch = (float)(-MathHelper.atan2(dy, horiz) * (180.0D / Math.PI));

            this.turretYaw = approachAngle(this.turretYaw, targetYaw, 18.0F);
            this.turretPitch = approachAngle(this.turretPitch, MathHelper.clamp(targetPitch, -45.0F, 45.0F), 14.0F);

            float yawError = Math.abs(MathHelper.wrapDegrees(targetYaw - this.turretYaw));

            if (yawError < 14.0F && this.fireCooldown <= 0)
            {
                this.fireAtTarget(ox, oy, oz, dx, dy, dz);
            }
        }
        else
        {
            this.hasLockedTarget = false;
            // Slow idle radar sweep
            this.turretYaw = MathHelper.wrapDegrees(this.turretYaw + 1.2F);
            this.turretPitch *= 0.85F;
        }
    }

    private void fireAtTarget(double ox, double oy, double oz, double dx, double dy, double dz)
    {
        this.fireCooldown = 5;
        this.muzzleFlashTicks = 2;

        if (this.world.isRemote)
        {
            return;
        }

        if (this.ammoCount <= 0)
        {
            return;
        }

        this.ammoCount--;
        this.markDirty();

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.001D)
        {
            return;
        }

        double nx = dx / dist;
        double ny = dy / dist;
        double nz = dz / dist;

        EntityArgonBullet bullet = new EntityArgonBullet(
            this.world,
            ox + nx * 0.55D,
            oy + ny * 0.55D,
            oz + nz * 0.55D,
            EntityArgonBullet.BulletKind.TURRET_ROUND,
            6.0F,
            true
        );
        bullet.setThrowableHeading(nx, ny, nz, 3.6F, 1.6F);
        this.world.spawnEntity(bullet);

        this.world.playSound((EntityPlayer)null, ox, oy, oz, SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS, 0.75F, 1.7F);
    }

    private EntityLivingBase findNearestHostile()
    {
        double ox = (double)this.pos.getX() + 0.5D;
        double oy = (double)this.pos.getY() + 0.72D;
        double oz = (double)this.pos.getZ() + 0.5D;

        AxisAlignedBB scanBox = new AxisAlignedBB(
            ox - RANGE, oy - 12.0D, oz - RANGE,
            ox + RANGE, oy + 12.0D, oz + RANGE
        );

        List<EntityLivingBase> list = this.world.getEntitiesWithinAABB(EntityLivingBase.class, scanBox);
        EntityLivingBase best = null;
        double bestDistSq = RANGE * RANGE;

        for (int i = 0; i < list.size(); ++i)
        {
            EntityLivingBase candidate = list.get(i);

            if (!this.isValidTarget(candidate))
            {
                continue;
            }

            double dSq = candidate.getDistanceSq(ox, oy, oz);

            if (dSq < bestDistSq)
            {
                bestDistSq = dSq;
                best = candidate;
            }
        }

        return best;
    }

    private boolean isValidTarget(EntityLivingBase entity)
    {
        if (entity == null || !entity.isEntityAlive() || entity instanceof EntityPlayer)
        {
            return false;
        }

        if (!(entity instanceof IMob))
        {
            return false;
        }

        double ox = (double)this.pos.getX() + 0.5D;
        double oy = (double)this.pos.getY() + 0.72D;
        double oz = (double)this.pos.getZ() + 0.5D;

        if (entity.getDistanceSq(ox, oy, oz) > RANGE * RANGE)
        {
            return false;
        }

        Vec3d start = new Vec3d(ox, oy, oz);
        Vec3d end = new Vec3d(entity.posX, entity.posY + (double)entity.getEyeHeight() * 0.75D, entity.posZ);
        RayTraceResult hit = this.world.rayTraceBlocks(start, end, false, true, false);
        return hit == null;
    }

    private static float approachAngle(float current, float target, float maxStep)
    {
        float diff = MathHelper.wrapDegrees(target - current);

        if (diff > maxStep) diff = maxStep;
        if (diff < -maxStep) diff = -maxStep;

        return MathHelper.wrapDegrees(current + diff);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setString("Owner", this.ownerName);
        compound.setInteger("Ammo", this.ammoCount);
        compound.setBoolean("Enabled", this.enabled);
        compound.setFloat("TurretYaw", this.turretYaw);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.ownerName = compound.getString("Owner");
        if (compound.hasKey("Ammo"))
        {
            this.ammoCount = compound.getInteger("Ammo");
        }
        if (compound.hasKey("Enabled"))
        {
            this.enabled = compound.getBoolean("Enabled");
        }
        this.turretYaw = compound.getFloat("TurretYaw");
        this.prevTurretYaw = this.turretYaw;
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
    }
}
