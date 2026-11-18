package net.minecraft.src;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ArgonVoiceChatManager
{
    public static final String CHANNEL_ARGON_VC = "ARGON|VC";
    public static final String CHANNEL_SIMPLE_VC = "voicechat:audio";

    public static final byte PACKET_STATE = 0;
    public static final byte PACKET_AUDIO_FRAME = 1;

    private static final double MIN_FULL_VOLUME_DIST = 2.0D;

    /**
     * Bridge interface for future WebRTC / TeaVM / WASM browser audio transport.
     */
    public interface WebRtcVoiceBridge
    {
        void onClientConnected(String localUuid, String serverAddress);

        void onClientDisconnected();

        void onMicrophoneStateChanged(boolean muted, boolean pttActive, boolean whispering);

        void onSpatialPeerUpdated(UUID peerUuid, String peerName, double distance, float attenuatedGain, float stereoPan, float lowPassCutoff, boolean occluded);

        void onVoiceAudioFrameReceived(UUID peerUuid, byte[] audioData, float attenuatedGain, float stereoPan, float lowPassCutoff);

        byte[] pollLocalAudioFrame();

        boolean isWebRtcConnected();
    }

    private static class DefaultWebRtcBridge implements WebRtcVoiceBridge
    {
        public void onClientConnected(String localUuid, String serverAddress)
        {
        }

        public void onClientDisconnected()
        {
        }

        public void onMicrophoneStateChanged(boolean muted, boolean pttActive, boolean whispering)
        {
        }

        public void onSpatialPeerUpdated(UUID peerUuid, String peerName, double distance, float attenuatedGain, float stereoPan, float lowPassCutoff, boolean occluded)
        {
        }

        public void onVoiceAudioFrameReceived(UUID peerUuid, byte[] audioData, float attenuatedGain, float stereoPan, float lowPassCutoff)
        {
        }

        public byte[] pollLocalAudioFrame()
        {
            return null;
        }

        public boolean isWebRtcConnected()
        {
            return false;
        }
    }

    public static class VoicePeerState
    {
        public final UUID uuid;
        public String name;
        public double posX;
        public double posY;
        public double posZ;
        public double distance;
        public float proximityGain;
        public float stereoPan;
        public float lowPassCutoff;
        public boolean occluded;
        public boolean speaking;
        public boolean whispering;
        public float audioLevel;
        public long lastPacketTimeMs;

        public VoicePeerState(UUID uuidIn, String nameIn)
        {
            this.uuid = uuidIn;
            this.name = nameIn;
            this.lowPassCutoff = 1.0F;
        }
    }

    private static WebRtcVoiceBridge bridge = new DefaultWebRtcBridge();
    private static final Map<UUID, VoicePeerState> peerMap = Maps.<UUID, VoicePeerState>newHashMap();
    private static final List<VoicePeerState> activeHudPeers = Lists.<VoicePeerState>newArrayList();

    private static boolean micMuted = false;
    private static boolean pttActive = false;
    private static boolean lastSentPtt = false;
    private static float simulatedMicLevel = 0.0F;
    private static int tickCounter = 0;

    public static void setBridge(WebRtcVoiceBridge customBridge)
    {
        bridge = customBridge != null ? customBridge : new DefaultWebRtcBridge();
    }

    public static WebRtcVoiceBridge getBridge()
    {
        return bridge;
    }

    public static boolean isMicMuted()
    {
        return micMuted;
    }

    public static void toggleMicMuted()
    {
        micMuted = !micMuted;

        if (micMuted)
        {
            pttActive = false;
        }

        bridge.onMicrophoneStateChanged(micMuted, pttActive, false);
    }

    public static boolean isPttActive()
    {
        return pttActive && !micMuted;
    }

    public static float getLocalMicLevel()
    {
        return simulatedMicLevel;
    }

    public static List<VoicePeerState> getActiveHudPeers()
    {
        return activeHudPeers;
    }

    public static void onDisconnect()
    {
        peerMap.clear();
        activeHudPeers.clear();
        pttActive = false;
        lastSentPtt = false;
        simulatedMicLevel = 0.0F;
        bridge.onClientDisconnected();
    }

    /**
     * Computes 3D distance attenuation factor (0.0F to 1.0F).
     * Full volume within 2.0 blocks, smooth quadratic decay up to maxDistance, 0.0F beyond maxDistance.
     */
    public static float computeDistanceAttenuation(double distance, double maxDistance)
    {
        if (maxDistance <= MIN_FULL_VOLUME_DIST)
        {
            return distance <= maxDistance ? 1.0F : 0.0F;
        }

        if (distance <= MIN_FULL_VOLUME_DIST)
        {
            return 1.0F;
        }

        if (distance >= maxDistance)
        {
            return 0.0F;
        }

        double normalized = (distance - MIN_FULL_VOLUME_DIST) / (maxDistance - MIN_FULL_VOLUME_DIST);
        double linear = 1.0D - normalized;
        return MathHelper.clamp((float)(linear * linear), 0.0F, 1.0F);
    }

    /**
     * Computes stereo pan (-1.0F left to +1.0F right) relative to the local player's look direction.
     */
    public static float computeStereoPan(EntityPlayer listener, double targetX, double targetZ)
    {
        double dx = targetX - listener.posX;
        double dz = targetZ - listener.posZ;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDist < 0.001D)
        {
            return 0.0F;
        }

        double dirX = dx / horizontalDist;
        double dirZ = dz / horizontalDist;

        float yawRad = listener.rotationYaw * 0.017453292F;
        // Right-hand vector in Minecraft's coordinate system
        double rightX = -MathHelper.cos(yawRad);
        double rightZ = -MathHelper.sin(yawRad);

        double dotRight = dirX * rightX + dirZ * rightZ;
        return MathHelper.clamp((float)dotRight, -1.0F, 1.0F);
    }

    /**
     * Checks if opaque blocks obstruct the line of sight between listener and speaker.
     */
    public static boolean isLineOfSightOccluded(World world, EntityPlayer listener, EntityPlayer speaker)
    {
        if (world == null || listener == null || speaker == null)
        {
            return false;
        }

        Vec3d start = new Vec3d(listener.posX, listener.posY + (double)listener.getEyeHeight(), listener.posZ);
        Vec3d end = new Vec3d(speaker.posX, speaker.posY + (double)speaker.getEyeHeight(), speaker.posZ);
        RayTraceResult hit = world.rayTraceBlocks(start, end, false, true, false);
        return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK;
    }

    public static void updateClientTick(Minecraft mc)
    {
        if (!Config.isVoiceChat() || mc == null || mc.world == null || mc.player == null)
        {
            pttActive = false;
            simulatedMicLevel = 0.0F;
            activeHudPeers.clear();
            return;
        }

        boolean pttKeyDown = mc.currentScreen == null && mc.gameSettings.ofKeyBindVoicePTT != null && GameSettingsIsKeyDown(mc.gameSettings.ofKeyBindVoicePTT);
        pttActive = !micMuted && pttKeyDown;
        boolean whispering = mc.player.isSneaking();

        if (pttActive)
        {
            float wave = 0.65F + 0.35F * MathHelper.sin((float)mc.player.ticksExisted * 0.45F);
            simulatedMicLevel = MathHelper.clamp(wave, 0.0F, 1.0F);
        }
        else
        {
            simulatedMicLevel = 0.0F;
        }

        if (pttActive != lastSentPtt || (pttActive && mc.player.ticksExisted % 10 == 0))
        {
            lastSentPtt = pttActive;
            bridge.onMicrophoneStateChanged(micMuted, pttActive, whispering);

            if (mc.getConnection() != null)
            {
                sendVoiceStatePacket(mc.getConnection(), pttActive, whispering, simulatedMicLevel);
            }
        }

        // Poll WebRTC local frame if bridge provides one
        if (pttActive && mc.getConnection() != null)
        {
            byte[] encodedFrame = bridge.pollLocalAudioFrame();

            if (encodedFrame != null && encodedFrame.length > 0)
            {
                sendVoiceAudioPacket(mc.getConnection(), whispering, encodedFrame);
            }
        }

        // Update 3D spatial attenuation for nearby players every 4 ticks to keep CPU cost near zero
        ++tickCounter;

        if (tickCounter < 4)
        {
            return;
        }

        tickCounter = 0;
        long nowMs = Minecraft.getSystemTime();
        double baseMaxDist = (double)Config.getVoiceChatDistance();
        float masterVoiceVol = Config.getVoiceChatVolume();

        activeHudPeers.clear();
        List<EntityPlayer> players = mc.world.playerEntities;

        for (int i = 0; i < players.size(); ++i)
        {
            EntityPlayer other = players.get(i);

            if (other == null || other == mc.player || other.getUniqueID() == null)
            {
                continue;
            }

            UUID uuid = other.getUniqueID();
            VoicePeerState peer = peerMap.get(uuid);

            if (peer == null)
            {
                peer = new VoicePeerState(uuid, other.getName());
                peerMap.put(uuid, peer);
            }

            peer.name = other.getName();
            peer.posX = other.posX;
            peer.posY = other.posY;
            peer.posZ = other.posZ;

            double dx = other.posX - mc.player.posX;
            double dy = (other.posY + (double)other.getEyeHeight()) - (mc.player.posY + (double)mc.player.getEyeHeight());
            double dz = other.posZ - mc.player.posZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            peer.distance = dist;

            // Expire remote speaking flag if no packet received in last 1200ms
            if (nowMs - peer.lastPacketTimeMs > 1200L)
            {
                peer.speaking = false;
                peer.audioLevel = 0.0F;
            }

            double effectiveMaxDist = (peer.whispering || other.isSneaking()) ? baseMaxDist * 0.5D : baseMaxDist;
            float distAtten = computeDistanceAttenuation(dist, effectiveMaxDist);

            if (distAtten > 0.001F)
            {
                boolean occluded = Config.isSoundPhysics() && isLineOfSightOccluded(mc.world, mc.player, other);
                peer.occluded = occluded;
                peer.lowPassCutoff = occluded ? 0.45F : 1.0F;
                float occlusionMult = occluded ? 0.65F : 1.0F;
                peer.proximityGain = MathHelper.clamp(distAtten * occlusionMult * masterVoiceVol, 0.0F, 2.0F);
                peer.stereoPan = computeStereoPan(mc.player, other.posX, other.posZ);

                bridge.onSpatialPeerUpdated(peer.uuid, peer.name, peer.distance, peer.proximityGain, peer.stereoPan, peer.lowPassCutoff, peer.occluded);
                activeHudPeers.add(peer);
            }
            else
            {
                peer.proximityGain = 0.0F;
                peer.occluded = false;
                peer.lowPassCutoff = 1.0F;
            }
        }

        // Clean up disconnected peers and sort activeHudPeers by distance
        if (peerMap.size() > 64)
        {
            Iterator<Map.Entry<UUID, VoicePeerState>> it = peerMap.entrySet().iterator();

            while (it.hasNext())
            {
                Map.Entry<UUID, VoicePeerState> entry = it.next();

                if (nowMs - entry.getValue().lastPacketTimeMs > 30000L && !activeHudPeers.contains(entry.getValue()))
                {
                    it.remove();
                }
            }
        }

        if (activeHudPeers.size() > 1)
        {
            Collections.sort(activeHudPeers, new Comparator<VoicePeerState>()
            {
                public int compare(VoicePeerState a, VoicePeerState b)
                {
                    return Double.compare(a.distance, b.distance);
                }
            });
        }
    }

    private static boolean GameSettingsIsKeyDown(net.minecraft.client.settings.KeyBinding keyBind)
    {
        return net.minecraft.client.settings.GameSettings.isKeyDown(keyBind);
    }

    public static void handleCustomPayload(String channelName, PacketBuffer buf, WorldClient world)
    {
        if (!Config.isVoiceChat() || buf == null || world == null)
        {
            return;
        }

        if (!CHANNEL_ARGON_VC.equals(channelName) && !CHANNEL_SIMPLE_VC.equals(channelName))
        {
            return;
        }

        try
        {
            if (buf.readableBytes() < 17)
            {
                return;
            }

            byte packetType = buf.readByte();
            long mostSig = buf.readLong();
            long leastSig = buf.readLong();
            UUID senderUuid = new UUID(mostSig, leastSig);

            Minecraft mc = Minecraft.getMinecraft();

            if (mc.player != null && senderUuid.equals(mc.player.getUniqueID()))
            {
                return;
            }

            EntityPlayer speakerEntity = world.getPlayerEntityByUUID(senderUuid);
            String speakerName = speakerEntity != null ? speakerEntity.getName() : "Player";
            VoicePeerState peer = peerMap.get(senderUuid);

            if (peer == null)
            {
                peer = new VoicePeerState(senderUuid, speakerName);
                peerMap.put(senderUuid, peer);
            }

            peer.lastPacketTimeMs = Minecraft.getSystemTime();

            if (packetType == PACKET_STATE && buf.readableBytes() >= 6)
            {
                peer.speaking = buf.readBoolean();
                peer.whispering = buf.readBoolean();
                peer.audioLevel = MathHelper.clamp(buf.readFloat(), 0.0F, 1.0F);
            }
            else if (packetType == PACKET_AUDIO_FRAME && buf.readableBytes() >= 3)
            {
                peer.speaking = true;
                peer.whispering = buf.readBoolean();
                int len = buf.readUnsignedShort();

                if (len > 0 && len <= 4096 && buf.readableBytes() >= len)
                {
                    byte[] audioData = new byte[len];
                    buf.readBytes(audioData);

                    if (mc.player != null && speakerEntity != null)
                    {
                        double dx = speakerEntity.posX - mc.player.posX;
                        double dy = (speakerEntity.posY + (double)speakerEntity.getEyeHeight()) - (mc.player.posY + (double)mc.player.getEyeHeight());
                        double dz = speakerEntity.posZ - mc.player.posZ;
                        peer.distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                        double maxDist = (double)Config.getVoiceChatDistance() * (peer.whispering ? 0.5D : 1.0D);
                        float atten = computeDistanceAttenuation(peer.distance, maxDist);
                        boolean occluded = Config.isSoundPhysics() && isLineOfSightOccluded(world, mc.player, speakerEntity);
                        peer.occluded = occluded;
                        peer.lowPassCutoff = occluded ? 0.45F : 1.0F;
                        peer.proximityGain = MathHelper.clamp(atten * (occluded ? 0.65F : 1.0F) * Config.getVoiceChatVolume(), 0.0F, 2.0F);
                        peer.stereoPan = computeStereoPan(mc.player, speakerEntity.posX, speakerEntity.posZ);

                        if (peer.proximityGain > 0.001F)
                        {
                            bridge.onVoiceAudioFrameReceived(senderUuid, audioData, peer.proximityGain, peer.stereoPan, peer.lowPassCutoff);
                        }
                    }
                }
            }
        }
        catch (Exception var16)
        {
            ;
        }
    }

    public static void sendVoiceStatePacket(NetHandlerPlayClient connection, boolean speaking, boolean whispering, float audioLevel)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (connection == null || mc == null || mc.player == null || mc.player.getUniqueID() == null)
        {
            return;
        }

        UUID selfUuid = mc.player.getUniqueID();
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer(24));
        buf.writeByte(PACKET_STATE);
        buf.writeLong(selfUuid.getMostSignificantBits());
        buf.writeLong(selfUuid.getLeastSignificantBits());
        buf.writeBoolean(speaking);
        buf.writeBoolean(whispering);
        buf.writeFloat(audioLevel);
        connection.sendPacket(new CPacketCustomPayload(CHANNEL_ARGON_VC, buf));
    }

    public static void sendVoiceAudioPacket(NetHandlerPlayClient connection, boolean whispering, byte[] encodedAudio)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (connection == null || mc == null || mc.player == null || mc.player.getUniqueID() == null || encodedAudio == null || encodedAudio.length == 0 || encodedAudio.length > 4096)
        {
            return;
        }

        UUID selfUuid = mc.player.getUniqueID();
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer(20 + encodedAudio.length));
        buf.writeByte(PACKET_AUDIO_FRAME);
        buf.writeLong(selfUuid.getMostSignificantBits());
        buf.writeLong(selfUuid.getLeastSignificantBits());
        buf.writeBoolean(whispering);
        buf.writeShort(encodedAudio.length);
        buf.writeBytes(encodedAudio);
        connection.sendPacket(new CPacketCustomPayload(CHANNEL_ARGON_VC, buf));
    }
}
