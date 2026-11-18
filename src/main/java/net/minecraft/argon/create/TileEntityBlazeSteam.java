package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class TileEntityBlazeSteam extends TileEntityKinetic
{
    public static final int KIND_BLAZE_BURNER = 0;
    public static final int KIND_STEAM_ENGINE = 1;

    private int kind = KIND_BLAZE_BURNER;
    private int burnTicks = 0;
    private int heatLevel = 0; // 0=NONE, 1=HEATED, 2=SUPER_HEATED
    private int boilerLevel = 0;
    private int scanTimer = 0;

    public TileEntityBlazeSteam()
    {
    }

    public TileEntityBlazeSteam(int kind)
    {
        this.kind = kind;
    }

    public int getKind()
    {
        return this.kind;
    }

    public boolean isHeated()
    {
        return this.heatLevel > 0;
    }

    public int getHeatLevel()
    {
        return this.heatLevel;
    }

    public int getBoilerLevel()
    {
        return this.boilerLevel;
    }

    @Override
    public String getCreatePartId()
    {
        return this.kind == KIND_STEAM_ENGINE ? "steam_engine" : "blaze_burner";
    }

    @Override
    public float getGeneratedCapacitySU()
    {
        if (this.kind == KIND_STEAM_ENGINE && this.boilerLevel > 0)
        {
            return (float)(this.boilerLevel * 2048);
        }

        return 0.0F;
    }

    @Override
    public float getGeneratedSpeedRPM()
    {
        if (this.kind == KIND_STEAM_ENGINE && this.boilerLevel > 0)
        {
            return (float)Math.min(128, 32 + this.boilerLevel * 12) * (float)this.directionMultiplier;
        }

        return 0.0F;
    }

    @Override
    public void update()
    {
        super.update();

        if (this.world == null)
        {
            return;
        }

        if (this.kind == KIND_BLAZE_BURNER)
        {
            if (this.burnTicks > 0)
            {
                --this.burnTicks;

                if (this.burnTicks == 0)
                {
                    this.heatLevel = 0;
                    this.markDirty();
                }

                if (this.world.isRemote && (this.world.getTotalWorldTime() % 8L == 0L))
                {
                    this.world.spawnParticle(EnumParticleTypes.FLAME,
                        (double)this.pos.getX() + 0.5D,
                        (double)this.pos.getY() + 0.55D,
                        (double)this.pos.getZ() + 0.5D,
                        0.0D, 0.03D, 0.0D);
                }
            }
        }
        else
        {
            if (++this.scanTimer % 15 == 0 || this.scanTimer == 1)
            {
                int nextLevel = this.evaluateBoiler();

                if (nextLevel != this.boilerLevel)
                {
                    this.boilerLevel = nextLevel;
                    KineticNetwork.scheduleUpdate(this.world, this.pos);
                }
            }

            if (this.boilerLevel > 0 && this.world.isRemote && (this.world.getTotalWorldTime() % 6L == 0L))
            {
                this.world.spawnParticle(EnumParticleTypes.CLOUD,
                    (double)this.pos.getX() + 0.5D,
                    (double)this.pos.getY() + 1.05D,
                    (double)this.pos.getZ() + 0.5D,
                    0.0D, 0.06D, 0.0D);
            }
        }
    }

    private int evaluateBoiler()
    {
        // Steam Engine sits attached to (or directly above) a FluidTank column with Water + Blaze Burner underneath
        TileEntityFluidNode tank = this.findAttachedWaterTank();

        if (tank == null || tank.getFluidType() != CreateFluidType.WATER || tank.getFluidMb() < 50)
        {
            return 0;
        }

        // Walk down tank column to find burner
        BlockPos scan = tank.getPos();
        int tankBlocks = 0;

        for (int i = 0; i < 8; ++i)
        {
            TileEntity te = this.world.getTileEntity(scan);

            if (te instanceof TileEntityFluidNode && ((TileEntityFluidNode)te).getKind() == TileEntityFluidNode.KIND_TANK)
            {
                ++tankBlocks;
                scan = scan.down();
            }
            else
            {
                break;
            }
        }

        TileEntity baseTe = this.world.getTileEntity(scan);
        int heat = 0;

        if (baseTe instanceof TileEntityBlazeSteam && ((TileEntityBlazeSteam)baseTe).getKind() == KIND_BLAZE_BURNER)
        {
            heat = ((TileEntityBlazeSteam)baseTe).getHeatLevel();
        }
        else if (this.world.getBlockState(scan).getBlock() == Blocks.LAVA || this.world.getBlockState(scan).getBlock() == Blocks.MAGMA)
        {
            heat = 1;
        }

        if (heat <= 0)
        {
            return 0;
        }

        // Consume small boiler water tick
        tank.drainFluid(5 * heat, false);
        return Math.min(9, tankBlocks * heat + (heat > 1 ? 2 : 0));
    }

    private TileEntityFluidNode findAttachedWaterTank()
    {
        for (EnumFacing dir : EnumFacing.VALUES)
        {
            TileEntity te = this.world.getTileEntity(this.pos.offset(dir));

            if (te instanceof TileEntityFluidNode && ((TileEntityFluidNode)te).getKind() == TileEntityFluidNode.KIND_TANK)
            {
                return (TileEntityFluidNode)te;
            }
        }

        return null;
    }

    public boolean onPlayerInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);

        if (this.kind == KIND_BLAZE_BURNER)
        {
            int addedTicks = 0;
            int level = 1;

            if (!held.isEmpty())
            {
                Item it = held.getItem();

                if (it == Items.BLAZE_ROD || it == Items.BLAZE_POWDER)
                {
                    addedTicks = 2400;
                    level = 2;
                }
                else if (it == Items.COAL || it == Items.LAVA_BUCKET || it == Item.getItemFromBlock(Blocks.COAL_BLOCK))
                {
                    addedTicks = it == Item.getItemFromBlock(Blocks.COAL_BLOCK) ? 3600 : 1200;
                    level = 1;
                }
            }

            if (addedTicks > 0)
            {
                this.burnTicks += addedTicks;
                this.heatLevel = Math.max(this.heatLevel, level);

                if (!player.capabilities.isCreativeMode)
                {
                    held.shrink(1);
                }

                this.markDirty();

                if (this.world != null && this.world.isRemote)
                {
                    player.sendMessage(new TextComponentString("\u00a76[Blaze Burner] \u00a7fState: " + (this.heatLevel >= 2 ? "\u00a7bSuper-Heated" : "\u00a7eHeated") + " \u00a77(" + (this.burnTicks / 20) + "s)"));
                }

                return true;
            }
        }

        if (this.world != null && this.world.isRemote)
        {
            if (this.kind == KIND_STEAM_ENGINE)
            {
                player.sendMessage(new TextComponentString("\u00a76[Steam Engine] \u00a7fBoiler Lv.\u00a7e" + this.boilerLevel + " \u00a77| Capacity: \u00a7a" + (int)this.getGeneratedCapacitySU() + " SU"));
            }
            else
            {
                player.sendMessage(new TextComponentString("\u00a76[Blaze Burner] \u00a77Feed Coal / Lava / Blaze Rod to ignite."));
            }
        }

        return true;
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        if (this.kind == KIND_STEAM_ENGINE)
        {
            super.appendJadeInfo(lines);
            lines.add("\u00a76Boiler Level: \u00a7eLv." + this.boilerLevel + (this.boilerLevel == 0 ? " \u00a77(Attach to heated Water Tank)" : " \u00a7aActive"));
        }
        else
        {
            lines.add("\u00a76Heat: " + (this.heatLevel >= 2 ? "\u00a7bSuper-Heated" : (this.heatLevel == 1 ? "\u00a7eHeated" : "\u00a77Idle")) + (this.burnTicks > 0 ? " (" + (this.burnTicks / 20) + "s)" : ""));
        }
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        if (this.kind == KIND_BLAZE_BURNER)
        {
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.BLAZE_BURNER_BODY, this.pos, EnumFacing.Axis.Y, 0.0F, light);

            if (this.heatLevel > 0)
            {
                float r = this.heatLevel >= 2 ? 0.45F : 1.0F;
                float g = this.heatLevel >= 2 ? 0.85F : 0.72F;
                float b = this.heatLevel >= 2 ? 1.0F : 0.30F;
                float bob = (float)(this.world != null ? (this.world.getTotalWorldTime() + partialTicks) * 4.0D : 0.0D);
                FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.BLAZE_CORE, (float)this.pos.getX() + 0.5F, (float)this.pos.getY() + 0.5F, (float)this.pos.getZ() + 0.5F, EnumFacing.Axis.Y, bob, r, g, b, 1.0F);
            }
        }
        else
        {
            float ang = this.getInterpolatedAngle(partialTicks);
            float stroke = this.boilerLevel > 0 ? (float)Math.sin(ang * 0.08F) * 0.16F : 0.0F;
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.STEAM_ENGINE_BODY, this.pos, EnumFacing.Axis.Y, 0.0F, light);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.STEAM_PISTON, (float)this.pos.getX() + 0.5F, (float)this.pos.getY() + 0.5F + stroke, (float)this.pos.getZ() + 0.5F, EnumFacing.Axis.Y, 0.0F, 1.0F, 1.0F, 1.0F, light);
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.SHAFT, this.pos, this.axis, ang, light);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.kind = compound.getByte("BlazeSteamKind");
        this.burnTicks = compound.getInteger("BurnTicks");
        this.heatLevel = compound.getByte("HeatLevel");
        this.boilerLevel = compound.getByte("BoilerLevel");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("BlazeSteamKind", (byte)this.kind);
        compound.setInteger("BurnTicks", this.burnTicks);
        compound.setByte("HeatLevel", (byte)this.heatLevel);
        compound.setByte("BoilerLevel", (byte)this.boilerLevel);
        return compound;
    }
}
