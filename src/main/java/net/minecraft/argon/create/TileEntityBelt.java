package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityBelt extends TileEntityKinetic
{
    public static final int MAX_ITEMS = 4;
    private ItemStack storedStack = ItemStack.EMPTY;
    private float itemProgress = 0.0F;

    @Override
    public String getCreatePartId()
    {
        return "belt";
    }

    @Override
    public float getStressImpactFactor()
    {
        return 0.5F;
    }

    public BlockPos getNextBeltPos()
    {
        if (this.facing == null) return null;
        return this.pos.offset(this.facing);
    }

    public BlockPos getPrevBeltPos()
    {
        if (this.facing == null) return null;
        return this.pos.offset(this.facing.getOpposite());
    }

    public ItemStack getStoredStack()
    {
        return this.storedStack;
    }

    public float getItemProgress()
    {
        return this.itemProgress;
    }

    public boolean tryInsertItem(ItemStack stack)
    {
        if (stack.isEmpty() || !this.storedStack.isEmpty())
        {
            return false;
        }

        this.storedStack = stack.copy();
        this.itemProgress = 0.0F;
        this.markDirty();
        return true;
    }

    @Override
    public void update()
    {
        super.update();

        if (this.world == null || this.overstressed || Math.abs(this.speedRPM) < 0.1F)
        {
            return;
        }

        EnumFacing moveDir = this.speedRPM >= 0.0F ? this.facing : this.facing.getOpposite();
        double beltSpeed = Math.min(0.28D, Math.abs(this.speedRPM) * 0.0022D);

        // 1. Move entities standing on the belt
        AxisAlignedBB topBox = new AxisAlignedBB(
            this.pos.getX(), (double)this.pos.getY() + 0.5D, this.pos.getZ(),
            this.pos.getX() + 1.0D, (double)this.pos.getY() + 1.35D, this.pos.getZ() + 1.0D);
        List<Entity> entities = this.world.getEntitiesWithinAABB(Entity.class, topBox);

        for (int i = 0; i < entities.size(); ++i)
        {
            Entity e = entities.get(i);

            if (e instanceof EntityItem)
            {
                if (this.storedStack.isEmpty())
                {
                    EntityItem ei = (EntityItem)e;
                    ItemStack s = ei.getItem();

                    if (!s.isEmpty() && ei.ticksExisted > 3)
                    {
                        this.storedStack = s.splitStack(1);
                        this.itemProgress = 0.25F;

                        if (s.isEmpty())
                        {
                            ei.setDead();
                        }

                        this.markDirty();
                    }
                }
            }
            else if (!e.isSneaking())
            {
                e.motionX = e.motionX * 0.5D + (double)moveDir.getFrontOffsetX() * beltSpeed;
                e.motionZ = e.motionZ * 0.5D + (double)moveDir.getFrontOffsetZ() * beltSpeed;
            }
        }

        // 2. Advance carried item along belt
        if (!this.storedStack.isEmpty())
        {
            this.itemProgress += (float)beltSpeed;

            if (this.itemProgress >= 1.0F)
            {
                BlockPos targetPos = this.pos.offset(moveDir);
                TileEntity targetTe = this.world.getTileEntity(targetPos);

                if (targetTe instanceof TileEntityBelt && ((TileEntityBelt)targetTe).tryInsertItem(this.storedStack))
                {
                    this.storedStack = ItemStack.EMPTY;
                    this.itemProgress = 0.0F;
                    this.markDirty();
                }
                else if (targetTe instanceof TileEntityBasin && ((TileEntityBasin)targetTe).insertItem(this.storedStack))
                {
                    this.storedStack = ItemStack.EMPTY;
                    this.itemProgress = 0.0F;
                    this.markDirty();
                }
                else if (targetTe instanceof IInventory && insertIntoInventory((IInventory)targetTe, this.storedStack))
                {
                    this.storedStack = ItemStack.EMPTY;
                    this.itemProgress = 0.0F;
                    this.markDirty();
                }
                else if (this.world.isAirBlock(targetPos))
                {
                    if (!this.world.isRemote)
                    {
                        EntityItem drop = new EntityItem(this.world,
                            (double)targetPos.getX() + 0.5D,
                            (double)targetPos.getY() + 0.6D,
                            (double)targetPos.getZ() + 0.5D,
                            this.storedStack);
                        drop.motionX = (double)moveDir.getFrontOffsetX() * 0.06D;
                        drop.motionY = 0.05D;
                        drop.motionZ = (double)moveDir.getFrontOffsetZ() * 0.06D;
                        this.world.spawnEntity(drop);
                    }

                    this.storedStack = ItemStack.EMPTY;
                    this.itemProgress = 0.0F;
                    this.markDirty();
                }
                else
                {
                    this.itemProgress = 1.0F;
                }
            }
        }
    }

    public static boolean insertIntoInventory(IInventory inv, ItemStack stack)
    {
        if (inv == null || stack.isEmpty())
        {
            return false;
        }

        for (int i = 0; i < inv.getSizeInventory(); ++i)
        {
            ItemStack slot = inv.getStackInSlot(i);

            if (slot.isEmpty() && inv.isItemValidForSlot(i, stack))
            {
                inv.setInventorySlotContents(i, stack.copy());
                inv.markDirty();
                return true;
            }
            else if (!slot.isEmpty() && slot.getItem() == stack.getItem() && slot.getMetadata() == stack.getMetadata() && ItemStack.areItemStackTagsEqual(slot, stack))
            {
                int space = Math.min(inv.getInventoryStackLimit(), slot.getMaxStackSize()) - slot.getCount();

                if (space >= stack.getCount())
                {
                    slot.grow(stack.getCount());
                    inv.markDirty();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void appendJadeInfo(List<String> lines)
    {
        super.appendJadeInfo(lines);

        if (!this.storedStack.isEmpty())
        {
            lines.add("\u00a77Belt Item: \u00a7f" + this.storedStack.getDisplayName() + " x" + this.storedStack.getCount());
        }
    }

    @Override
    public void queueFlywheelInstances(float partialTicks, float light)
    {
        EnumFacing.Axis beltAxis = this.facing != null ? this.facing.getAxis() : EnumFacing.Axis.Z;
        // Frame orientation perpendicular to shaft rotation axis
        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.BELT_FRAME, (float)this.pos.getX() + 0.5F, (float)this.pos.getY() + 0.35F, (float)this.pos.getZ() + 0.5F, EnumFacing.Axis.Y, this.facing != null && this.facing.getAxis() == EnumFacing.Axis.X ? 90.0F : 0.0F, 1.0F, 1.0F, 1.0F, light);
        EnumFacing.Axis shaftAx = beltAxis == EnumFacing.Axis.X ? EnumFacing.Axis.Z : EnumFacing.Axis.X;
        FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.SHAFT, (float)this.pos.getX() + 0.5F, (float)this.pos.getY() + 0.35F, (float)this.pos.getZ() + 0.5F, shaftAx, this.getInterpolatedAngle(partialTicks), 1.0F, 1.0F, 1.0F, light);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.itemProgress = compound.getFloat("BeltProg");
        this.storedStack = compound.hasKey("BeltItem") ? new ItemStack(compound.getCompoundTag("BeltItem")) : ItemStack.EMPTY;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setFloat("BeltProg", this.itemProgress);

        if (!this.storedStack.isEmpty())
        {
            compound.setTag("BeltItem", this.storedStack.writeToNBT(new NBTTagCompound()));
        }

        return compound;
    }
}
