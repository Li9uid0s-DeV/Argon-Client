package net.minecraft.argon.create;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class EntityArgonTrain extends Entity
{
    public static final int MAX_CARS = 10;
    private static final int PATH_CAPACITY = 512;

    public static final class Carriage
    {
        public ContraptionStructure structure;
        public final ContraptionMeshCache meshCache = new ContraptionMeshCache();
        public double x;
        public double y;
        public double z;
        public double prevX;
        public double prevY;
        public double prevZ;
        public float yaw;
        public float prevYaw;
        public double carSpacing = 5.5D;

        public Carriage(ContraptionStructure structure, double x, double y, double z, float yaw, double spacing)
        {
            this.structure = structure;
            this.x = x;
            this.y = y;
            this.z = z;
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.yaw = yaw;
            this.prevYaw = yaw;
            this.carSpacing = Math.max(3.0D, spacing);
        }

        public void dispose()
        {
            this.meshCache.dispose();
        }
    }

    private final List<Carriage> carriages = new ArrayList<Carriage>();
    private TrainScheduleData schedule = null;
    private boolean autopilot = false;
    private double trainSpeed = 0.0D;
    private int stationWaitTimer = 0;

    // Active Hermite spline curve traversal
    private TileEntityTrainTrack activeCurveTrack = null;
    private double activeCurveT = 0.0D;
    private double activeCurveLength = 1.0D;

    // Circular breadcrumb buffer for multi-car curved path following
    private final double[] pathX = new double[PATH_CAPACITY];
    private final double[] pathY = new double[PATH_CAPACITY];
    private final double[] pathZ = new double[PATH_CAPACITY];
    private final float[] pathYaw = new float[PATH_CAPACITY];
    private final double[] pathDist = new double[PATH_CAPACITY];
    private int pathHead = 0;
    private int pathCount = 0;
    private double totalTraveledDist = 0.0D;

    public EntityArgonTrain(World worldIn)
    {
        super(worldIn);
        this.setSize(2.6F, 2.8F);
        this.preventEntitySpawning = true;
        this.ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit()
    {
    }

    public List<Carriage> getCarriages()
    {
        return this.carriages;
    }

    public double getTrainSpeed()
    {
        return this.trainSpeed;
    }

    public boolean isAutopilot()
    {
        return this.autopilot;
    }

    public TrainScheduleData getSchedule()
    {
        return this.schedule;
    }

    public void setSchedule(TrainScheduleData scheduleData)
    {
        this.schedule = scheduleData;
        this.autopilot = scheduleData != null && !scheduleData.isEmpty();
        this.stationWaitTimer = 0;
    }

    public void addCarriage(ContraptionStructure structure, double x, double y, double z, float yaw, double spacing)
    {
        if (this.carriages.size() >= MAX_CARS || structure == null || structure.entries.isEmpty())
        {
            return;
        }

        Carriage car = new Carriage(structure, x, y, z, yaw, spacing);
        this.carriages.add(car);

        if (this.carriages.size() == 1)
        {
            this.setPosition(x, y, z);
            this.rotationYaw = yaw;
            this.prevRotationYaw = yaw;
            this.seedInitialPath(x, y, z, yaw);
        }
    }

    private void seedInitialPath(double leadX, double leadY, double leadZ, float yaw)
    {
        this.pathHead = 0;
        this.pathCount = 0;
        this.totalTraveledDist = 0.0D;
        double rad = Math.toRadians((double)yaw);
        double backX = Math.sin(rad);
        double backZ = -Math.cos(rad);

        for (int s = 80; s >= 0; --s)
        {
            double distBack = (double)s * 0.75D;
            this.pushBreadcrumb(leadX + backX * distBack, leadY, leadZ + backZ * distBack, yaw, 0.75D);
        }

        this.totalTraveledDist = 0.0D;
        this.pathDist[this.pathHead] = 0.0D;
    }

    private void pushBreadcrumb(double x, double y, double z, float yaw, double stepDist)
    {
        this.pathHead = (this.pathHead + 1) % PATH_CAPACITY;
        this.totalTraveledDist += Math.abs(stepDist);
        this.pathX[this.pathHead] = x;
        this.pathY[this.pathHead] = y;
        this.pathZ[this.pathHead] = z;
        this.pathYaw[this.pathHead] = yaw;
        this.pathDist[this.pathHead] = this.totalTraveledDist;

        if (this.pathCount < PATH_CAPACITY)
        {
            ++this.pathCount;
        }
    }

    private void samplePathAtOffset(double targetDistBack, double[] outPosYaw)
    {
        if (this.pathCount <= 1)
        {
            outPosYaw[0] = this.posX;
            outPosYaw[1] = this.posY;
            outPosYaw[2] = this.posZ;
            outPosYaw[3] = (double)this.rotationYaw;
            return;
        }

        double targetArc = this.totalTraveledDist - targetDistBack;
        int cur = this.pathHead;

        for (int i = 0; i < this.pathCount - 1; ++i)
        {
            int older = (cur - 1 + PATH_CAPACITY) % PATH_CAPACITY;
            double dCur = this.pathDist[cur];
            double dOlder = this.pathDist[older];

            if (targetArc >= dOlder && targetArc <= dCur && (dCur - dOlder) > 1.0E-5D)
            {
                double f = (targetArc - dOlder) / (dCur - dOlder);
                outPosYaw[0] = this.pathX[older] + (this.pathX[cur] - this.pathX[older]) * f;
                outPosYaw[1] = this.pathY[older] + (this.pathY[cur] - this.pathY[older]) * f;
                outPosYaw[2] = this.pathZ[older] + (this.pathZ[cur] - this.pathZ[older]) * f;
                float dyaw = MathHelper.wrapDegrees(this.pathYaw[cur] - this.pathYaw[older]);
                outPosYaw[3] = (double)(this.pathYaw[older] + dyaw * (float)f);
                return;
            }

            cur = older;
        }

        // Extrapolate backward beyond oldest breadcrumb using lead heading
        int oldest = (this.pathHead - (this.pathCount - 1) + PATH_CAPACITY) % PATH_CAPACITY;
        double extra = Math.max(0.0D, this.pathDist[oldest] - targetArc);
        double rad = Math.toRadians((double)this.pathYaw[oldest]);
        outPosYaw[0] = this.pathX[oldest] + Math.sin(rad) * extra;
        outPosYaw[1] = this.pathY[oldest];
        outPosYaw[2] = this.pathZ[oldest] - Math.cos(rad) * extra;
        outPosYaw[3] = (double)this.pathYaw[oldest];
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        if (this.carriages.isEmpty())
        {
            return;
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevRotationYaw = this.rotationYaw;

        for (int i = 0; i < this.carriages.size(); ++i)
        {
            Carriage c = this.carriages.get(i);
            c.prevX = c.x;
            c.prevY = c.y;
            c.prevZ = c.z;
            c.prevYaw = c.yaw;
        }

        Entity controller = this.getControllingPassenger();

        if (controller instanceof EntityLivingBase)
        {
            float forward = ((EntityLivingBase)controller).moveForward;

            if (Math.abs(forward) > 0.05F)
            {
                this.trainSpeed = MathHelper.clamp(this.trainSpeed + (double)forward * 0.032D, -0.45D, 0.55D);
            }
            else
            {
                this.trainSpeed *= 0.94D;

                if (Math.abs(this.trainSpeed) < 0.004D)
                {
                    this.trainSpeed = 0.0D;
                }
            }
        }
        else if (this.autopilot && this.schedule != null && !this.schedule.isEmpty())
        {
            this.updateAutopilot();
        }
        else
        {
            this.trainSpeed *= 0.92D;

            if (Math.abs(this.trainSpeed) < 0.004D)
            {
                this.trainSpeed = 0.0D;
            }
        }

        if (Math.abs(this.trainSpeed) > 0.001D)
        {
            this.advanceAlongTrack(this.trainSpeed);
        }

        // Position every carriage (up to 10 cars) along curved breadcrumb spline
        double accumOffset = 0.0D;
        double[] sample = new double[4];

        for (int i = 0; i < this.carriages.size(); ++i)
        {
            Carriage c = this.carriages.get(i);

            if (i == 0)
            {
                c.x = this.posX;
                c.y = this.posY;
                c.z = this.posZ;
                c.yaw = this.rotationYaw;
            }
            else
            {
                accumOffset += c.carSpacing;
                this.samplePathAtOffset(accumOffset, sample);
                c.x = sample[0];
                c.y = sample[1];
                c.z = sample[2];
                c.yaw = (float)sample[3];
            }
        }
    }

    private void updateAutopilot()
    {
        if (this.stationWaitTimer > 0)
        {
            --this.stationWaitTimer;
            this.trainSpeed = 0.0D;

            if (this.stationWaitTimer == 0)
            {
                this.schedule.advanceStop();
            }

            return;
        }

        TrainScheduleData.StopEntry stop = this.schedule.getCurrentStop();

        if (stop == null)
        {
            return;
        }

        if (!stop.hasCoordinates)
        {
            // Resolve named station within loaded world TileEntities
            List<TileEntity> loaded = this.world.loadedTileEntityList;

            for (int i = 0; i < loaded.size(); ++i)
            {
                TileEntity te = loaded.get(i);

                if (te instanceof TileEntityTrainStation)
                {
                    TileEntityTrainStation st = (TileEntityTrainStation)te;

                    if (st.getStationName().equalsIgnoreCase(stop.stationName))
                    {
                        BlockPos trackPos = st.findNearbyTrackPos();
                        BlockPos target = trackPos != null ? trackPos : st.getPos();
                        stop.targetX = (double)target.getX() + 0.5D;
                        stop.targetY = (double)target.getY() + 0.19D;
                        stop.targetZ = (double)target.getZ() + 0.5D;
                        stop.hasCoordinates = true;
                        break;
                    }
                }
            }
        }

        if (!stop.hasCoordinates)
        {
            this.trainSpeed *= 0.90D;
            return;
        }

        double dx = stop.targetX - this.posX;
        double dz = stop.targetZ - this.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 2.2D)
        {
            this.trainSpeed = 0.0D;
            this.stationWaitTimer = stop.waitTicks;
            return;
        }

        double forwardX = -Math.sin(Math.toRadians((double)this.rotationYaw));
        double forwardZ = Math.cos(Math.toRadians((double)this.rotationYaw));
        double dot = (dx * forwardX + dz * forwardZ) / Math.max(0.001D, dist);
        double desiredSpeed = (dot >= 0.0D ? 1.0D : -1.0D) * Math.min(0.40D, Math.max(0.12D, dist * 0.045D));
        this.trainSpeed += (desiredSpeed - this.trainSpeed) * 0.08D;
    }

    private void advanceAlongTrack(double step)
    {
        double absStep = Math.abs(step);
        double forwardSign = step >= 0.0D ? 1.0D : -1.0D;

        if (this.activeCurveTrack != null && !this.activeCurveTrack.isInvalid() && this.activeCurveTrack.hasCurvedLink())
        {
            double dt = (absStep / Math.max(2.0D, this.activeCurveLength)) * forwardSign;
            this.activeCurveT += dt;

            if (this.activeCurveT >= 1.0D || this.activeCurveT <= 0.0D)
            {
                BlockPos endPos = this.activeCurveT >= 1.0D ? this.activeCurveTrack.getLinkedPos() : this.activeCurveTrack.getPos();
                double[] endOut = new double[4];
                this.activeCurveTrack.evaluateSpline(this.activeCurveT >= 1.0D ? 1.0D : 0.0D, endOut);
                this.setPosition(endOut[0], endOut[1] + 0.1875D, endOut[2]);
                this.rotationYaw = (float)endOut[3];
                this.activeCurveTrack = null;
                this.activeCurveT = 0.0D;
                this.pushBreadcrumb(this.posX, this.posY, this.posZ, this.rotationYaw, absStep);
                return;
            }

            double[] out = new double[4];
            this.activeCurveTrack.evaluateSpline(this.activeCurveT, out);
            this.setPosition(out[0], out[1] + 0.1875D, out[2]);
            this.rotationYaw = (float)out[3];
            this.pushBreadcrumb(this.posX, this.posY, this.posZ, this.rotationYaw, absStep);
            return;
        }

        double rad = Math.toRadians((double)this.rotationYaw);
        double dirX = -Math.sin(rad) * step;
        double dirZ = Math.cos(rad) * step;
        double nx = this.posX + dirX;
        double ny = this.posY;
        double nz = this.posZ + dirZ;

        // Check for curved track entry or straight track alignment near new lead pos
        BlockPos probe = new BlockPos(nx, ny - 0.1D, nz);
        TileEntity te = this.world.getTileEntity(probe);

        if (!(te instanceof TileEntityTrainTrack))
        {
            probe = probe.down();
            te = this.world.getTileEntity(probe);
        }

        if (te instanceof TileEntityTrainTrack)
        {
            TileEntityTrainTrack track = (TileEntityTrainTrack)te;
            ny = (double)probe.getY() + 0.1875D;

            if (forwardSign > 0.0D && track.hasCurvedLink())
            {
                this.activeCurveTrack = track;
                this.activeCurveT = 0.05D;
                this.activeCurveLength = Math.max(2.0D, Math.sqrt(track.getPos().distanceSq(track.getLinkedPos())));
            }
            else
            {
                // Snap softly toward track axis tangent
                float trackYaw = track.getFacing().getHorizontalAngle();
                float dYawFwd = Math.abs(MathHelper.wrapDegrees(trackYaw - this.rotationYaw));
                float dYawRev = Math.abs(MathHelper.wrapDegrees((trackYaw + 180.0F) - this.rotationYaw));
                float targetYaw = dYawFwd <= dYawRev ? trackYaw : trackYaw + 180.0F;
                this.rotationYaw += MathHelper.wrapDegrees(targetYaw - this.rotationYaw) * 0.35F;
            }
        }

        this.setPosition(nx, ny, nz);
        this.pushBreadcrumb(nx, ny, nz, this.rotationYaw, absStep);
    }

    public void disassembleToWorld()
    {
        if (this.isDead || this.world == null)
        {
            return;
        }

        for (int i = 0; i < this.carriages.size(); ++i)
        {
            Carriage car = this.carriages.get(i);
            BlockPos anchor = new BlockPos(car.x, car.y + 0.2D, car.z);
            car.structure.placeIntoWorld(this.world, anchor);
            car.dispose();
        }

        this.carriages.clear();
        this.setDead();
    }

    @Override
    public boolean canBeCollidedWith()
    {
        return !this.isDead;
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand)
    {
        if (hand != EnumHand.MAIN_HAND)
        {
            return true;
        }

        ItemStack held = player.getHeldItem(hand);

        if (!held.isEmpty() && held.getItem() == CreateItems.TRAIN_SCHEDULE)
        {
            TrainScheduleData data = ItemTrainSchedule.readSchedule(held);

            if (!data.isEmpty())
            {
                this.setSchedule(data);

                if (this.world.isRemote)
                {
                    player.sendMessage(new TextComponentString("\u00a7a[Train] Assigned autopilot schedule (" + data.stops.size() + " stops)."));
                }
            }
            else if (this.world.isRemote)
            {
                player.sendMessage(new TextComponentString("\u00a7e[Train] Schedule has no stops yet. Right-click air to edit it."));
            }

            return true;
        }

        if (!held.isEmpty() && held.getItem() == CreateItems.WRENCH && player.isSneaking())
        {
            this.disassembleToWorld();

            if (this.world.isRemote)
            {
                player.sendMessage(new TextComponentString("\u00a76[Train] Disassembled carriage(s) into blocks."));
            }

            return true;
        }

        if (!player.isRiding())
        {
            player.startRiding(this);
        }

        return true;
    }

    @Override
    @Nullable
    public Entity getControllingPassenger()
    {
        return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
    }

    @Override
    public void updatePassenger(Entity passenger)
    {
        if (this.isPassenger(passenger))
        {
            double seatX = this.posX;
            double seatY = this.posY + 1.05D;
            double seatZ = this.posZ;

            if (!this.carriages.isEmpty())
            {
                Carriage lead = this.carriages.get(0);

                if (lead.structure.hasControlsSeat)
                {
                    double lx = (double)lead.structure.seatLocalPos.getX();
                    double ly = (double)lead.structure.seatLocalPos.getY() + 0.65D;
                    double lz = (double)lead.structure.seatLocalPos.getZ();
                    double rad = Math.toRadians((double)lead.yaw);
                    double cos = Math.cos(rad);
                    double sin = Math.sin(rad);
                    seatX = lead.x + (lx * cos - lz * sin);
                    seatY = lead.y + ly;
                    seatZ = lead.z + (lx * sin + lz * cos);
                }
            }

            passenger.setPosition(seatX, seatY, seatZ);
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        return this.getEntityBoundingBox().grow(48.0D, 8.0D, 48.0D);
    }

    @Override
    public void setDead()
    {
        super.setDead();

        for (int i = 0; i < this.carriages.size(); ++i)
        {
            this.carriages.get(i).dispose();
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound)
    {
        this.carriages.clear();
        this.trainSpeed = compound.getDouble("TrainSpeed");
        this.autopilot = compound.getBoolean("Autopilot");
        this.stationWaitTimer = compound.getInteger("WaitTimer");

        if (compound.hasKey("Schedule"))
        {
            this.schedule = TrainScheduleData.readFromNBT(compound.getCompoundTag("Schedule"));
        }

        NBTTagList cars = compound.getTagList("Carriages", 10);

        for (int i = 0; i < cars.tagCount(); ++i)
        {
            NBTTagCompound ct = cars.getCompoundTagAt(i);
            ContraptionStructure st = ContraptionStructure.readFromNBT(ct.getCompoundTag("Struct"));
            Carriage c = new Carriage(st,
                ct.getDouble("CX"), ct.getDouble("CY"), ct.getDouble("CZ"),
                ct.getFloat("CYaw"), ct.getDouble("CSpacing"));
            this.carriages.add(c);
        }

        this.seedInitialPath(this.posX, this.posY, this.posZ, this.rotationYaw);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound)
    {
        compound.setDouble("TrainSpeed", this.trainSpeed);
        compound.setBoolean("Autopilot", this.autopilot);
        compound.setInteger("WaitTimer", this.stationWaitTimer);

        if (this.schedule != null)
        {
            compound.setTag("Schedule", this.schedule.writeToNBT());
        }

        NBTTagList cars = new NBTTagList();

        for (int i = 0; i < this.carriages.size(); ++i)
        {
            Carriage c = this.carriages.get(i);
            NBTTagCompound ct = new NBTTagCompound();
            ct.setTag("Struct", c.structure.writeToNBT());
            ct.setDouble("CX", c.x);
            ct.setDouble("CY", c.y);
            ct.setDouble("CZ", c.z);
            ct.setFloat("CYaw", c.yaw);
            ct.setDouble("CSpacing", c.carSpacing);
            cars.appendTag(ct);
        }

        compound.setTag("Carriages", cars);
    }
}
