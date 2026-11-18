package net.minecraft.argon.guns;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Single-threaded client<->server payload bridge on channel "ARGON|GUN"
 * using standard Minecraft 1.12 CPacketCustomPayload.
 */
public final class GunNetworkHandler
{
    public static final String CHANNEL = "ARGON|GUN";

    public static final byte ACTION_FIRE = 1;
    public static final byte ACTION_RELOAD = 2;
    public static final byte ACTION_MODIFY_ATTACHMENT = 3;

    private GunNetworkHandler()
    {
    }

    public static void sendFirePacket(boolean isAds)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.getConnection() == null)
        {
            return;
        }

        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeByte(ACTION_FIRE);
        buf.writeBoolean(isAds);
        mc.getConnection().sendPacket(new CPacketCustomPayload(CHANNEL, buf));
    }

    public static void sendReloadPacket()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.getConnection() == null)
        {
            return;
        }

        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeByte(ACTION_RELOAD);
        mc.getConnection().sendPacket(new CPacketCustomPayload(CHANNEL, buf));
    }

    public static void sendModifyAttachmentPacket(BlockPos workbenchPos, int slotCategory, int newAttachmentIndex)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.getConnection() == null)
        {
            return;
        }

        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeByte(ACTION_MODIFY_ATTACHMENT);
        buf.writeBlockPos(workbenchPos != null ? workbenchPos : BlockPos.ORIGIN);
        buf.writeByte(slotCategory);
        buf.writeByte(newAttachmentIndex);
        mc.getConnection().sendPacket(new CPacketCustomPayload(CHANNEL, buf));
    }

    public static void handleServerPayload(EntityPlayerMP player, PacketBuffer buf)
    {
        if (player == null || buf == null || !buf.isReadable())
        {
            return;
        }

        byte action = buf.readByte();
        World world = player.world;
        ItemStack held = player.getHeldItemMainhand();

        if (held.isEmpty() || !(held.getItem() instanceof ItemArgonGun))
        {
            return;
        }

        ItemArgonGun gun = (ItemArgonGun)held.getItem();

        if (action == ACTION_FIRE)
        {
            boolean isAds = buf.isReadable() && buf.readBoolean();
            gun.fireServer(world, player, held, isAds);
        }
        else if (action == ACTION_RELOAD)
        {
            gun.startReloadServer(world, player, held);
        }
        else if (action == ACTION_MODIFY_ATTACHMENT)
        {
            BlockPos pos = buf.readBlockPos();
            int slotCategory = buf.readByte();
            int newIndex = buf.readByte();
            handleWorkbenchAttachment(world, player, held, gun, pos, slotCategory, newIndex);
        }
    }

    private static void handleWorkbenchAttachment(
        World world,
        EntityPlayerMP player,
        ItemStack held,
        ItemArgonGun gun,
        BlockPos workbenchPos,
        int slotCategory,
        int newIndex)
    {
        if (!gun.getGunType().supportsAttachments())
        {
            return;
        }

        if (player.getDistanceSq((double)workbenchPos.getX() + 0.5D, (double)workbenchPos.getY() + 0.5D, (double)workbenchPos.getZ() + 0.5D) > 64.0D)
        {
            return;
        }

        if (world.getBlockState(workbenchPos).getBlock() != GunBlocks.GUN_WORKBENCH)
        {
            return;
        }

        int currentIndex = GunAttachment.getAttachmentIndex(held, slotCategory);

        if (currentIndex == newIndex)
        {
            return;
        }

        Item neededItem = GunAttachment.getItemForAttachment(slotCategory, newIndex);
        Item oldItem = GunAttachment.getItemForAttachment(slotCategory, currentIndex);

        if (!player.capabilities.isCreativeMode && neededItem != null)
        {
            int foundSlot = -1;

            for (int i = 0; i < player.inventory.getSizeInventory(); ++i)
            {
                ItemStack inv = player.inventory.getStackInSlot(i);

                if (!inv.isEmpty() && inv.getItem() == neededItem)
                {
                    foundSlot = i;
                    break;
                }
            }

            if (foundSlot < 0)
            {
                return;
            }

            player.inventory.getStackInSlot(foundSlot).shrink(1);
        }

        if (!player.capabilities.isCreativeMode && oldItem != null)
        {
            ItemStack refund = new ItemStack(oldItem, 1);

            if (!player.inventory.addItemStackToInventory(refund))
            {
                player.dropItem(refund, false);
            }
        }

        GunAttachment.setAttachmentIndex(held, slotCategory, newIndex);

        // Clamp current ammo in magazine if Extended Mag was removed
        int maxMag = gun.getMaxMagazine(held);
        NBTTagCompound tag = ItemArgonGun.getOrInitTag(held, gun.getGunType());

        if (tag.getInteger(ItemArgonGun.NBT_AMMO) > maxMag)
        {
            tag.setInteger(ItemArgonGun.NBT_AMMO, maxMag);
        }

        player.inventoryContainer.detectAndSendChanges();
        world.playSound((EntityPlayerMP)null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.55F, 1.55F);
    }
}
