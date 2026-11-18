package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class TileEntityFluidNode extends TileEntityKinetic
{
    public static final int KIND_PIPE = 0;
    public static final int KIND_PUMP = 1;
    public static final int KIND_TANK = 2;

    private int kind = KIND_PIPE;
    private CreateFluidType fluidType = CreateFluidType.EMPTY;
    private int fluidMb = 0;

    public TileEntityFluidNode()
    {
    }

    public TileEntityFluidNode(int kind)
    {
        this.kind = kind;
    }

    public int getKind()
    {
        return this.kind;
    }

    public CreateFluidType getFluidType()
    {
        return this.fluidType;
    }

    public int getFluidMb()
    {
        return this.fluidMb;
    }

    public int getCapacityMb()
    {
        return this.kind == KIND_TANK ? 8000 : 1000;
    }

    @Override
    public String getCreatePartId()
    {
        if (this.kind == KIND_PUMP) return "mechanical_pump";
        if (this.kind == KIND_TANK) return "fluid_tank";
        return "fluid_pipe";
    }

    @Override
    public boolean hasShaftOnSide(EnumFacing side)
    {
        return this.kind == KIND_PUMP && side.getAxis() == this.axis;
    }

    @Override
    public float getStressImpactFactor()
    {
        return this.kind == KIND_PUMP ? 4.0F : 0.0F;
    }

    public int receiveFluid(CreateFluidType type, int amountMb, boolean simulate)
    {
        if (type == CreateFluidType.EMPTY || amountMb <= 0)
        {
            return 0;
        }

        if (this.fluidType != CreateFluidType.EMPTY && this.fluidType != type)
        {
            return 0;
        }

        int space = this.getCapacityMb() - this.fluidMb;
        int accepted = Math.min(space, amountMb);

        if (!simulate && accepted > 0)
        {
            this.fluidType = type;
            this.fluidMb += accepted;
            this.markDirty();
        }

        return accepted;
    }

    public int drainFluid(int maxMb, boolean simulate)
    {
        int drained = Math.min(this.fluidMb, maxMb);

        if (!simulate && drained > 0)
        {
            this.fluidMb -= drained;

            if (this.fluidMb <= 0)
            {
                this.fluidMb = 0;
                this.fluidType = CreateFluidType.EMPTY;
            }

            this.markDirty();
        }

        return drained;
    }

    @Override
    public void update()
    {
        super.update();

        if (this.world == null)
        {
            return;
        }

        if (this.kind == KIND_PUMP)
        {
            this.tickPump();
        }
        else if (this.kind == KIND_PIPE)
        {
            this.tickPipeEqualize();
        }
        else if (this.kind == KIND_TANK)
        {
            this.tickTankColumn();
        }
    }

    private void tickPump()
    {
        if (this.overstressed || Math.abs(this.speedRPM) < 0.1F)
        {
            return;
        }

        int rate = Math.max(20, Math.min(400, (int)(Math.abs(this.speedRPM) * 4.0F)));
        EnumFacing pushDir = this.speedRPM >= 0.0F ? this.facing : this.facing.getOpposite();
        EnumFacing pullDir = pushDir.getOpposite();

        BlockPos pullPos = this.pos.offset(pullDir);
        BlockPos pushPos = this.pos.offset(pushDir);

        // Pull from adjacent world water/lava block or fluid node
        if (this.fluidMb < this.getCapacityMb())
        {
            TileEntity pullTe = this.world.getTileEntity(pullPos);

            if (pullTe instanceof TileEntityFluidNode)
            {
                TileEntityFluidNode src = (TileEntityFluidNode)pullTe;

                if (src.fluidMb > 0 && (this.fluidType == CreateFluidType.EMPTY || this.fluidType == src.fluidType))
                {
                    CreateFluidType t = src.fluidType;
                    int canTake = Math.min(rate, this.getCapacityMb() - this.fluidMb);
                    int got = src.drainFluid(canTake, false);
                    this.receiveFluid(t, got, false);
                }
            }
            else if (this.world.isBlockLoaded(pullPos))
            {
                IBlockState st = this.world.getBlockState(pullPos);

                if (st.getMaterial() == Material.WATER)
                {
                    this.receiveFluid(CreateFluidType.WATER, rate, false);
                }
                else if (st.getMaterial() == Material.LAVA)
                {
                    this.receiveFluid(CreateFluidType.LAVA, rate, false);
                }
            }
        }

        // Push into front pipe/tank
        if (this.fluidMb > 0)
        {
            TileEntity pushTe = this.world.getTileEntity(pushPos);

            if (pushTe instanceof TileEntityFluidNode)
            {
                int sent = ((TileEntityFluidNode)pushTe).receiveFluid(this.fluidType, Math.min(rate, this.fluidMb), false);

                if (sent > 0)
                {
                    this.drainFluid(sent, false);
                }
            }
        }
    }

    private void tickPipeEqualize()
    {
        if (this.fluidMb <= 0 || this.world.getTotalWorldTime() % 2L != 0L)
        {
            return;
        }

        for (EnumFacing dir : EnumFacing.VALUES)
        {
            BlockPos np = this.pos.offset(dir);
            TileEntity te = this.world.getTileEntity(np);

            if (te instanceof TileEntityFluidNode)
            {
                TileEntityFluidNode target = (TileEntityFluidNode)te;

                if (target.kind == KIND_TANK || (target.kind == KIND_PIPE && target.fluidMb + 20 < this.fluidMb))
                {
                    int transfer = target.kind == KIND_TANK ? Math.min(200, this.fluidMb) : (this.fluidMb - target.fluidMb) / 2;
                    int acc = target.receiveFluid(this.fluidType, transfer, false);

                    if (acc > 0)
                    {
                        this.drainFluid(acc, false);

                        if (this.fluidMb <= 0)
                        {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void tickTankColumn()
    {
        if (this.fluidMb <= 0)
        {
            return;
        }

        // Multi-block column gravity balance: bottom tanks fill first
        TileEntity below = this.world.getTileEntity(this.pos.down());

        if (below instanceof TileEntityFluidNode && ((TileEntityFluidNode)below).kind == KIND_TANK)
        {
            TileEntityFluidNode lower = (TileEntityFluidNode)below;
            int acc = lower.receiveFluid(this.fluidType, Math.min(500, this.fluidMb), false);

            if (acc > 0)
            {
                this.drainFluid(acc, false);
            }
        }
    }

    public boolean onPlayerInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);

        if (!held.isEmpty() && held.getItem() == Items.WATER_BUCKET)
        {
            int acc = this.receiveFluid(CreateFluidType.WATER, 1000, false);

            if (acc > 0 && !player.capabilities.isCreativeMode)
            {
                player.setHeldItem(hand, new ItemStack(Items.BUCKET));
            }

            if (this.world != null && this.world.isRemote)
            {
                player.sendMessage(new TextComponentString("\u00a7b[Fluid Tank] \u00a7f" + this.fluidType.getDisplayName() + ": \u00a7b" + this.fluidMb + " / " + this.getCapacityMb() + " mB"));
            }

            return true;
        }

        if (!held.isEmpty() && held.getItem() == Items.LAVA_BUCKET)
        {
            int acc = this.receiveFluid(CreateFluidType.LAVA, 1000, false);

            if (acc > 0 && !player.capabilities.isCreativeMode)
            {
                player.setHeldItem(hand, new ItemStack(Items.BUCKET));
            }

            return true;
        }

        if (this.world != null && this.world.isRemote)
        {
            player.sendMessage(new TextComponentString("\u00a7b[Fluid] \u00a7f" + (this.fluidMb > 0 ? this.fluidType.getDisplayName() + " " + this.fluidMb + "/" + this.getCapacityMb() + " mB" : "Empty")));
        }

        return true;
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        if (this.kind == KIND_PUMP)
        {
            super.appendJadeInfo(lines);
        }

        lines.add("\u00a7bFluid: \u00a7f" + (this.fluidMb > 0 ? this.fluidType.getDisplayName() + " " + this.fluidMb + "/" + this.getCapacityMb() + "mB" : "Empty"));
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        if (this.kind == KIND_TANK)
        {
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.TANK_CASING, this.pos, EnumFacing.Axis.Y, 0.0F, light);
        }
        else if (this.kind == KIND_PUMP)
        {
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.PIPE_CORE, this.pos, EnumFacing.Axis.Y, 0.0F, light);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.PUMP_COLLAR, this.pos, this.facing.getAxis(), this.getInterpolatedAngle(partialTicks), light);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.COGWHEEL_SMALL, this.pos, this.axis, this.getInterpolatedAngle(partialTicks), light);
        }
        else
        {
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.PIPE_CORE, this.pos, EnumFacing.Axis.Y, 0.0F, light);

            if (this.world != null)
            {
                for (EnumFacing dir : EnumFacing.VALUES)
                {
                    TileEntity n = this.world.getTileEntity(this.pos.offset(dir));

                    if (n instanceof TileEntityFluidNode || n instanceof TileEntityBlazeSteam)
                    {
                        float ox = dir.getFrontOffsetX() * 0.25F;
                        float oy = dir.getFrontOffsetY() * 0.25F;
                        float oz = dir.getFrontOffsetZ() * 0.25F;
                        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.PIPE_ARM_POS_Y,
                            (float)this.pos.getX() + 0.5F + ox * 0.0F,
                            (float)this.pos.getY() + 0.5F + oy * 0.0F,
                            (float)this.pos.getZ() + 0.5F + oz * 0.0F,
                            dir.getAxis(),
                            dir.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? 180.0F : 0.0F,
                            1.0F, 1.0F, 1.0F, light);
                    }
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.kind = compound.getByte("FluidNodeKind");
        this.fluidType = CreateFluidType.byId(compound.getString("FluidType"));
        this.fluidMb = compound.getInteger("FluidMb");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("FluidNodeKind", (byte)this.kind);
        compound.setString("FluidType", this.fluidType.id);
        compound.setInteger("FluidMb", this.fluidMb);
        return compound;
    }
}
