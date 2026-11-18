package net.minecraft.argon.portals;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.argon.create.ContraptionStructure;
import net.minecraft.argon.create.EntityArgonTrain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.src.Config;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

/**
 * BetterPortals-style seamless portal renderer and teleporter for Argon Client 1.12.2.
 * Uses stencil buffer masking + 0.5x resolution destination FBO + 3-chunk distance cap
 * + recursion depth 1 + frame-sliced cached chunk VBOs for 60 FPS on Kompanio 500.
 */
public final class ArgonBetterPortals
{
    public static final int MAX_PORTAL_DEPTH = 1;
    public static final float PORTAL_RESOLUTION_SCALE = 0.5F;
    public static final int PORTAL_CHUNK_RADIUS = 3;
    public static final float PORTAL_MAX_RENDER_DIST = 48.0F;
    private static final int PORTAL_SCAN_RADIUS = 22;
    private static final int MAX_PORTALS_PER_FRAME = 2;
    private static final int MAX_CHUNKS_BUILT_PER_FRAME = 4;

    private static int renderDepth = 0;
    private static boolean seamlessTeleportFlag = false;

    private static Framebuffer portalFramebuffer = null;
    private static int cachedFbWidth = -1;
    private static int cachedFbHeight = -1;

    private static final List<PortalOpening> scannedOpenings = new ArrayList<PortalOpening>();
    private static long lastScanTick = -100L;
    private static int lastScanDim = Integer.MIN_VALUE;

    private static final Map<Long, PortalDestination> portalLinkCache = new HashMap<Long, PortalDestination>();
    private static final Map<Long, CachedChunkSlice> chunkSliceCache = new HashMap<Long, CachedChunkSlice>();
    private static final Map<Long, SnapshotRegion> mpSnapshots = new HashMap<Long, SnapshotRegion>();

    private static final FloatBuffer FOG_COLOR_BUF = GLAllocation.createDirectFloatBuffer(4);

    private ArgonBetterPortals()
    {
    }

    public static final class PortalOpening
    {
        public final BlockPos anchor;
        public final EnumFacing.Axis axis;
        public final double minX;
        public final double minY;
        public final double minZ;
        public final double maxX;
        public final double maxY;
        public final double maxZ;
        public final double centerX;
        public final double centerY;
        public final double centerZ;
        public double distSq;

        public PortalOpening(BlockPos anchor, EnumFacing.Axis axis, double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
        {
            this.anchor = anchor;
            this.axis = axis;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.centerX = (minX + maxX) * 0.5D;
            this.centerY = (minY + maxY) * 0.5D;
            this.centerZ = (minZ + maxZ) * 0.5D;
        }
    }

    public static final class PortalDestination
    {
        public final int destDim;
        public final double exitX;
        public final double exitY;
        public final double exitZ;
        public final EnumFacing.Axis exitAxis;
        public long lastResolvedTick;

        public PortalDestination(int destDim, double exitX, double exitY, double exitZ, EnumFacing.Axis exitAxis, long tick)
        {
            this.destDim = destDim;
            this.exitX = exitX;
            this.exitY = exitY;
            this.exitZ = exitZ;
            this.exitAxis = exitAxis;
            this.lastResolvedTick = tick;
        }
    }

    private static final class CachedChunkSlice
    {
        public final long key;
        public VertexBuffer vbo;
        public BufferBuilder.State fallbackState;
        public long builtTick;
        public boolean empty;

        public CachedChunkSlice(long key)
        {
            this.key = key;
        }

        public void dispose()
        {
            if (this.vbo != null)
            {
                this.vbo.deleteGlBuffers();
                this.vbo = null;
            }

            this.fallbackState = null;
            this.empty = true;
        }
    }

    private static final class SnapshotRegion
    {
        public final int dim;
        public final int originX;
        public final int originY;
        public final int originZ;
        public final int sizeX = 48;
        public final int sizeY = 32;
        public final int sizeZ = 48;
        public final IBlockState[] states;

        public SnapshotRegion(int dim, int originX, int originY, int originZ)
        {
            this.dim = dim;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.states = new IBlockState[this.sizeX * this.sizeY * this.sizeZ];
        }

        public void capture(World world)
        {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

            for (int x = 0; x < this.sizeX; ++x)
            {
                for (int z = 0; z < this.sizeZ; ++z)
                {
                    for (int y = 0; y < this.sizeY; ++y)
                    {
                        m.setPos(this.originX + x, this.originY + y, this.originZ + z);
                        int idx = (x * this.sizeZ + z) * this.sizeY + y;
                        this.states[idx] = world.getBlockState(m);
                    }
                }
            }
        }
    }

    public static boolean isRenderingMainWorld()
    {
        return renderDepth == 0;
    }

    public static boolean isInsidePortalPass()
    {
        return renderDepth > 0;
    }

    public static boolean isSeamlessActiveForEntity(Entity entity)
    {
        if (entity == null || !Config.isBetterPortals())
        {
            return false;
        }

        return entity.dimension == 0 || entity.dimension == -1;
    }

    public static void markSeamlessTeleport()
    {
        seamlessTeleportFlag = true;
    }

    public static void markSeamlessTeleportFor(Entity entity)
    {
        if (entity instanceof EntityPlayer && Config.isBetterPortals())
        {
            seamlessTeleportFlag = true;
        }
    }

    public static boolean consumeSeamlessTeleport()
    {
        boolean val = seamlessTeleportFlag;
        seamlessTeleportFlag = false;
        return val && Config.isBetterPortals();
    }

    public static void clearCaches()
    {
        for (CachedChunkSlice slice : chunkSliceCache.values())
        {
            slice.dispose();
        }

        chunkSliceCache.clear();
        portalLinkCache.clear();
        scannedOpenings.clear();
        lastScanTick = -100L;
        lastScanDim = Integer.MIN_VALUE;

        if (portalFramebuffer != null)
        {
            portalFramebuffer.deleteFramebuffer();
            portalFramebuffer = null;
            cachedFbWidth = -1;
            cachedFbHeight = -1;
        }
    }

    public static void captureCurrentWorldSnapshotAround(World world, BlockPos centerPos)
    {
        if (world == null || centerPos == null || !Config.isBetterPortals())
        {
            return;
        }

        int dim = world.provider.getDimensionType().getId();
        int ox = centerPos.getX() - 24;
        int oy = MathHelper.clamp(centerPos.getY() - 12, 0, 224);
        int oz = centerPos.getZ() - 24;
        long key = packPortalKey(dim, centerPos.getX() >> 3, centerPos.getY() >> 3, centerPos.getZ() >> 3);
        SnapshotRegion region = new SnapshotRegion(dim, ox, oy, oz);
        region.capture(world);
        mpSnapshots.put(Long.valueOf(key), region);
    }

    public static void renderPortals(EntityRenderer entityRenderer, Entity viewEntity, float partialTicks)
    {
        if (!Config.isBetterPortals() || renderDepth >= MAX_PORTAL_DEPTH || shadersActive())
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.world == null || viewEntity == null)
        {
            return;
        }

        int srcDim = mc.world.provider.getDimensionType().getId();

        if (srcDim != 0 && srcDim != -1)
        {
            return;
        }

        double camX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * (double)partialTicks;
        double camY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * (double)partialTicks + (double)viewEntity.getEyeHeight();
        double camZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * (double)partialTicks;

        updateNearbyPortalScan(mc.world, camX, camY, camZ);

        if (scannedOpenings.isEmpty())
        {
            return;
        }

        for (int i = 0; i < scannedOpenings.size(); ++i)
        {
            PortalOpening op = scannedOpenings.get(i);
            double dx = op.centerX - camX;
            double dy = op.centerY - camY;
            double dz = op.centerZ - camZ;
            op.distSq = dx * dx + dy * dy + dz * dz;
        }

        Collections.sort(scannedOpenings, new Comparator<PortalOpening>()
        {
            @Override
            public int compare(PortalOpening a, PortalOpening b)
            {
                return Double.compare(a.distSq, b.distSq);
            }
        });

        Framebuffer mainFb = mc.getFramebuffer();

        if (mainFb == null)
        {
            return;
        }

        ensurePortalFramebuffer(mainFb);

        if (portalFramebuffer == null)
        {
            return;
        }

        int count = Math.min(MAX_PORTALS_PER_FRAME, scannedOpenings.size());
        List<PortalOpening> rendered = new ArrayList<PortalOpening>(count);

        for (int i = 0; i < count; ++i)
        {
            PortalOpening opening = scannedOpenings.get(i);

            if (opening.distSq <= (double)(PORTAL_SCAN_RADIUS * PORTAL_SCAN_RADIUS))
            {
                rendered.add(opening);
            }
        }

        if (rendered.isEmpty())
        {
            return;
        }

        // Currently render closest portal seamlessly into 0.5x FBO and composite via stencil buffer.
        // If two portals are visible, render each with its own stencil ID cleanly.
        boolean useStencil = mainFb.isStencilActive();

        if (useStencil)
        {
            GL11.glClearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        }

        for (int i = 0; i < rendered.size(); ++i)
        {
            PortalOpening opening = rendered.get(i);
            PortalDestination dest = resolveDestination(mc, srcDim, opening);

            renderDestinationToPortalFbo(mc, entityRenderer, opening, dest, camX, camY, camZ, partialTicks);
            mainFb.bindFramebuffer(true);

            if (useStencil)
            {
                compositeWithStencil(mc, mainFb, opening, i + 1);
            }
            else
            {
                drawPortalFallbackOverlay(mc, opening);
            }
        }
    }

    private static boolean shadersActive()
    {
        return Config.isShaders();
    }

    private static void ensurePortalFramebuffer(Framebuffer mainFb)
    {
        int targetW = Math.max(1, mainFb.framebufferWidth);
        int targetH = Math.max(1, mainFb.framebufferHeight);

        if (portalFramebuffer == null || cachedFbWidth != targetW || cachedFbHeight != targetH)
        {
            if (portalFramebuffer != null)
            {
                portalFramebuffer.deleteFramebuffer();
            }

            portalFramebuffer = new Framebuffer(targetW, targetH, true);
            portalFramebuffer.setResolutionScale(PORTAL_RESOLUTION_SCALE);
            portalFramebuffer.createBindFramebuffer(targetW, targetH);
            cachedFbWidth = targetW;
            cachedFbHeight = targetH;
        }
    }

    private static void updateNearbyPortalScan(World world, double camX, double camY, double camZ)
    {
        long tick = world.getTotalWorldTime();
        int dim = world.provider.getDimensionType().getId();

        if (dim == lastScanDim && Math.abs(tick - lastScanTick) < 4L && !scannedOpenings.isEmpty())
        {
            Iterator<PortalOpening> it = scannedOpenings.iterator();

            while (it.hasNext())
            {
                PortalOpening op = it.next();

                if (world.getBlockState(op.anchor).getBlock() != Blocks.PORTAL)
                {
                    it.remove();
                }
            }

            if (!scannedOpenings.isEmpty())
            {
                return;
            }
        }

        lastScanTick = tick;
        lastScanDim = dim;
        scannedOpenings.clear();

        int cx = MathHelper.floor(camX);
        int cy = MathHelper.floor(camY);
        int cz = MathHelper.floor(camZ);
        int minY = Math.max(1, cy - 10);
        int maxY = Math.min(254, cy + 10);

        Set<BlockPos> visited = new HashSet<BlockPos>();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        for (int x = cx - PORTAL_SCAN_RADIUS; x <= cx + PORTAL_SCAN_RADIUS; ++x)
        {
            for (int z = cz - PORTAL_SCAN_RADIUS; z <= cz + PORTAL_SCAN_RADIUS; ++z)
            {
                Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);

                if (!chunk.isLoaded())
                {
                    continue;
                }

                for (int y = minY; y <= maxY; ++y)
                {
                    probe.setPos(x, y, z);

                    if (chunk.getBlockState(x & 15, y, z & 15).getBlock() == Blocks.PORTAL)
                    {
                        BlockPos start = probe.toImmutable();

                        if (!visited.contains(start))
                        {
                            PortalOpening group = floodPortalGroup(world, start, visited);

                            if (group != null)
                            {
                                scannedOpenings.add(group);
                            }
                        }
                    }
                }
            }
        }
    }

    private static PortalOpening floodPortalGroup(World world, BlockPos start, Set<BlockPos> visited)
    {
        IBlockState baseState = world.getBlockState(start);

        if (baseState.getBlock() != Blocks.PORTAL)
        {
            return null;
        }

        EnumFacing.Axis axis = (EnumFacing.Axis)baseState.getValue(BlockPortal.AXIS);
        List<BlockPos> queue = new ArrayList<BlockPos>(24);
        queue.add(start);
        visited.add(start);

        int minX = start.getX();
        int maxX = start.getX();
        int minY = start.getY();
        int maxY = start.getY();
        int minZ = start.getZ();
        int maxZ = start.getZ();

        int idx = 0;

        while (idx < queue.size() && queue.size() < 256)
        {
            BlockPos cur = queue.get(idx++);

            if (cur.getX() < minX) minX = cur.getX();
            if (cur.getX() > maxX) maxX = cur.getX();
            if (cur.getY() < minY) minY = cur.getY();
            if (cur.getY() > maxY) maxY = cur.getY();
            if (cur.getZ() < minZ) minZ = cur.getZ();
            if (cur.getZ() > maxZ) maxZ = cur.getZ();

            for (EnumFacing dir : EnumFacing.VALUES)
            {
                if (axis == EnumFacing.Axis.X && dir.getAxis() == EnumFacing.Axis.Z)
                {
                    continue;
                }

                if (axis == EnumFacing.Axis.Z && dir.getAxis() == EnumFacing.Axis.X)
                {
                    continue;
                }

                BlockPos next = cur.offset(dir);

                if (!visited.contains(next))
                {
                    IBlockState ns = world.getBlockState(next);

                    if (ns.getBlock() == Blocks.PORTAL && ns.getValue(BlockPortal.AXIS) == axis)
                    {
                        visited.add(next);
                        queue.add(next);
                    }
                }
            }
        }

        if (axis == EnumFacing.Axis.X)
        {
            double planeZ = (double)minZ + 0.5D;
            return new PortalOpening(start, axis, (double)minX, (double)minY, planeZ - 0.02D, (double)(maxX + 1), (double)(maxY + 1), planeZ + 0.02D);
        }
        else
        {
            double planeX = (double)minX + 0.5D;
            return new PortalOpening(start, axis, planeX - 0.02D, (double)minY, (double)minZ, planeX + 0.02D, (double)(maxY + 1), (double)(maxZ + 1));
        }
    }

    private static PortalDestination resolveDestination(Minecraft mc, int srcDim, PortalOpening opening)
    {
        int destDim = srcDim == -1 ? 0 : -1;
        long linkKey = packPortalKey(srcDim, opening.anchor.getX() >> 2, opening.anchor.getY() >> 2, opening.anchor.getZ() >> 2);
        long now = mc.world.getTotalWorldTime();

        PortalDestination cached = portalLinkCache.get(Long.valueOf(linkKey));

        if (cached != null && now - cached.lastResolvedTick < 100L)
        {
            return cached;
        }

        double scaledX = srcDim == -1 ? opening.centerX * 8.0D : opening.centerX / 8.0D;
        double scaledZ = srcDim == -1 ? opening.centerZ * 8.0D : opening.centerZ / 8.0D;
        double targetY = MathHelper.clamp(opening.centerY, 16.0D, destDim == -1 ? 116.0D : 220.0D);

        if (mc.isSingleplayer() && mc.getIntegratedServer() != null)
        {
            WorldServer destWorld = mc.getIntegratedServer().getWorld(destDim);

            if (destWorld != null)
            {
                BlockPos found = findExistingPortalNear(destWorld, MathHelper.floor(scaledX), MathHelper.floor(targetY), MathHelper.floor(scaledZ), 40);

                if (found == null && opening.distSq <= 324.0D)
                {
                    EntityArmorStand probe = new EntityArmorStand(destWorld, scaledX, targetY, scaledZ);
                    destWorld.getDefaultTeleporter().placeInPortal(probe, 0.0F);
                    found = findExistingPortalNear(destWorld, MathHelper.floor(probe.posX), MathHelper.floor(probe.posY), MathHelper.floor(probe.posZ), 24);
                }

                if (found != null)
                {
                    IBlockState st = destWorld.getBlockState(found);
                    EnumFacing.Axis exitAxis = st.getBlock() == Blocks.PORTAL ? (EnumFacing.Axis)st.getValue(BlockPortal.AXIS) : opening.axis;
                    Set<BlockPos> tmpVisited = new HashSet<BlockPos>();
                    PortalOpening exitOpening = floodPortalGroup(destWorld, found, tmpVisited);
                    double ex = exitOpening != null ? exitOpening.centerX : ((double)found.getX() + 0.5D);
                    double ey = exitOpening != null ? exitOpening.centerY : ((double)found.getY() + 1.0D);
                    double ez = exitOpening != null ? exitOpening.centerZ : ((double)found.getZ() + 0.5D);
                    PortalDestination pd = new PortalDestination(destDim, ex, ey, ez, exitAxis, now);
                    portalLinkCache.put(Long.valueOf(linkKey), pd);
                    return pd;
                }
            }
        }

        PortalDestination fallback = new PortalDestination(destDim, scaledX, targetY, scaledZ, opening.axis, now);
        portalLinkCache.put(Long.valueOf(linkKey), fallback);
        return fallback;
    }

    private static BlockPos findExistingPortalNear(World world, int cx, int cy, int cz, int radius)
    {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int minY = Math.max(4, cy - 36);
        int maxY = Math.min(250, cy + 36);

        for (int x = cx - radius; x <= cx + radius; x += 2)
        {
            for (int z = cz - radius; z <= cz + radius; z += 2)
            {
                Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);

                if (!chunk.isLoaded())
                {
                    continue;
                }

                for (int y = minY; y <= maxY; ++y)
                {
                    m.setPos(x, y, z);

                    if (chunk.getBlockState(x & 15, y, z & 15).getBlock() == Blocks.PORTAL)
                    {
                        double dx = (double)(x - cx);
                        double dy = (double)(y - cy);
                        double dz = (double)(z - cz);
                        double dSq = dx * dx + dy * dy + dz * dz;

                        if (dSq < bestDistSq)
                        {
                            bestDistSq = dSq;
                            best = m.toImmutable();
                        }
                    }
                }
            }
        }

        return best;
    }

    private static void renderDestinationToPortalFbo(
        Minecraft mc,
        EntityRenderer entityRenderer,
        PortalOpening opening,
        PortalDestination dest,
        double camX,
        double camY,
        double camZ,
        float partialTicks)
    {
        renderDepth = 1;

        try
        {
            portalFramebuffer.bindFramebuffer(true);

            boolean isNetherDest = dest.destDim == -1;
            float fogR = isNetherDest ? 0.22F : 0.56F;
            float fogG = isNetherDest ? 0.03F : 0.75F;
            float fogB = isNetherDest ? 0.03F : 0.96F;

            GlStateManager.clearColor(fogR, fogG, fogB, 1.0F);
            GlStateManager.clearDepth(1.0D);
            GlStateManager.clear(16640);

            double distPlane = opening.axis == EnumFacing.Axis.X ? Math.abs(camZ - opening.centerZ) : Math.abs(camX - opening.centerX);
            float nearClip = (float)Math.max(0.05D, distPlane - 0.14D);

            GlStateManager.matrixMode(5889);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            float fov = entityRenderer.getPortalFOV(partialTicks);
            Project.gluPerspective(fov, (float)mc.displayWidth / (float)Math.max(1, mc.displayHeight), nearClip, PORTAL_MAX_RENDER_DIST);

            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();

            // Translate so exit portal center coincides with entry portal center in viewer space
            double relX = opening.centerX - camX;
            double relY = opening.centerY - camY;
            double relZ = opening.centerZ - camZ;
            GlStateManager.translate(relX, relY, relZ);

            if (opening.axis != dest.exitAxis)
            {
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            }

            // Configure 3-chunk (48-block) capped linear fog matching destination dimension
            GlStateManager.enableFog();
            GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
            GlStateManager.setFogStart(24.0F);
            GlStateManager.setFogEnd(PORTAL_MAX_RENDER_DIST);
            FOG_COLOR_BUF.clear();
            FOG_COLOR_BUF.put(fogR).put(fogG).put(fogB).put(1.0F);
            FOG_COLOR_BUF.flip();
            GlStateManager.glFog(2918, FOG_COLOR_BUF);

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1F);
            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            RenderHelper.disableStandardItemLighting();
            entityRenderer.enableLightmap();
            GlStateManager.shadeModel(7425);

            World destWorld = resolveClientAccessibleDestWorld(mc, dest.destDim);

            if (destWorld != null)
            {
                renderDestinationChunks(mc, destWorld, dest);
                renderDestinationEntities(mc, destWorld, dest, partialTicks);
            }
            else
            {
                renderAtmosphereChamber(isNetherDest);
            }

            entityRenderer.disableLightmap();
            GlStateManager.disableFog();
            GlStateManager.shadeModel(7424);

            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
        finally
        {
            portalFramebuffer.unbindFramebuffer();
            renderDepth = 0;
        }
    }

    private static World resolveClientAccessibleDestWorld(Minecraft mc, int destDim)
    {
        if (mc.isSingleplayer() && mc.getIntegratedServer() != null)
        {
            return mc.getIntegratedServer().getWorld(destDim);
        }

        return null;
    }

    private static void renderDestinationChunks(Minecraft mc, World destWorld, PortalDestination dest)
    {
        int centerChunkX = MathHelper.floor(dest.exitX) >> 4;
        int centerChunkZ = MathHelper.floor(dest.exitZ) >> 4;
        int originBlockX = MathHelper.floor(dest.exitX);
        int originBlockY = MathHelper.floor(dest.exitY);
        int originBlockZ = MathHelper.floor(dest.exitZ);
        long now = destWorld.getTotalWorldTime();

        List<ChunkPos> sortedChunks = new ArrayList<ChunkPos>(49);

        for (int dx = -PORTAL_CHUNK_RADIUS; dx <= PORTAL_CHUNK_RADIUS; ++dx)
        {
            for (int dz = -PORTAL_CHUNK_RADIUS; dz <= PORTAL_CHUNK_RADIUS; ++dz)
            {
                if (dx * dx + dz * dz <= PORTAL_CHUNK_RADIUS * PORTAL_CHUNK_RADIUS + 1)
                {
                    sortedChunks.add(new ChunkPos(centerChunkX + dx, centerChunkZ + dz));
                }
            }
        }

        Collections.sort(sortedChunks, new Comparator<ChunkPos>()
        {
            @Override
            public int compare(ChunkPos a, ChunkPos b)
            {
                int da = (a.x - centerChunkX) * (a.x - centerChunkX) + (a.z - centerChunkZ) * (a.z - centerChunkZ);
                int db = (b.x - centerChunkX) * (b.x - centerChunkX) + (b.z - centerChunkZ) * (b.z - centerChunkZ);
                return Integer.compare(da, db);
            }
        });

        int builtThisFrame = 0;

        for (int i = 0; i < sortedChunks.size(); ++i)
        {
            ChunkPos cp = sortedChunks.get(i);
            long sliceKey = packPortalKey(dest.destDim, cp.x, originBlockY >> 4, cp.z);
            CachedChunkSlice slice = chunkSliceCache.get(Long.valueOf(sliceKey));

            boolean needsBuild = slice == null || (now - slice.builtTick > 100L);

            if (needsBuild && builtThisFrame < MAX_CHUNKS_BUILT_PER_FRAME)
            {
                Chunk chunk = destWorld.getChunkFromChunkCoords(cp.x, cp.z);

                if (chunk.isLoaded())
                {
                    if (slice == null)
                    {
                        slice = new CachedChunkSlice(sliceKey);
                        chunkSliceCache.put(Long.valueOf(sliceKey), slice);
                    }

                    buildChunkSliceMesh(mc, destWorld, chunk, slice, originBlockX, originBlockY, originBlockZ, now);
                    ++builtThisFrame;
                }
            }

            if (slice != null && !slice.empty)
            {
                drawChunkSlice(slice, (double)originBlockX - dest.exitX, (double)originBlockY - dest.exitY, (double)originBlockZ - dest.exitZ);
            }
        }
    }

    private static void buildChunkSliceMesh(
        Minecraft mc,
        World destWorld,
        Chunk chunk,
        CachedChunkSlice slice,
        int originX,
        int originY,
        int originZ,
        long now)
    {
        slice.dispose();
        slice.builtTick = now;

        int minY = Math.max(1, originY - 22);
        int maxY = Math.min(254, originY + 24);
        int baseX = chunk.x << 4;
        int baseZ = chunk.z << 4;

        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        BufferBuilder buf = new BufferBuilder(32768);
        buf.begin(7, DefaultVertexFormats.BLOCK);
        buf.setTranslation((double)(-originX), (double)(-originY), (double)(-originZ));

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int renderedBlocks = 0;

        for (int lx = 0; lx < 16; ++lx)
        {
            int wx = baseX + lx;

            for (int lz = 0; lz < 16; ++lz)
            {
                int wz = baseZ + lz;

                for (int wy = minY; wy <= maxY; ++wy)
                {
                    IBlockState state = chunk.getBlockState(lx, wy, lz);
                    Block block = state.getBlock();

                    if (state.getMaterial() == Material.AIR || block == Blocks.PORTAL)
                    {
                        continue;
                    }

                    pos.setPos(wx, wy, wz);

                    if (isCompletelyOccluded(destWorld, pos))
                    {
                        continue;
                    }

                    if (dispatcher.renderBlock(state, pos, (IBlockAccess)destWorld, buf))
                    {
                        ++renderedBlocks;
                    }
                }
            }
        }

        buf.setTranslation(0.0D, 0.0D, 0.0D);
        buf.finishDrawing();

        if (renderedBlocks == 0 || buf.getVertexCount() == 0)
        {
            slice.empty = true;
            return;
        }

        slice.empty = false;

        if (OpenGlHelper.useVbo())
        {
            slice.vbo = new VertexBuffer(DefaultVertexFormats.BLOCK);
            slice.vbo.bufferData(buf.getByteBuffer());
        }
        else
        {
            slice.fallbackState = buf.getVertexState();
        }
    }

    private static boolean isCompletelyOccluded(World world, BlockPos pos)
    {
        for (EnumFacing f : EnumFacing.VALUES)
        {
            IBlockState adj = world.getBlockState(pos.offset(f));

            if (!adj.isOpaqueCube())
            {
                return false;
            }
        }

        return true;
    }

    private static void drawChunkSlice(CachedChunkSlice slice, double offX, double offY, double offZ)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate(offX, offY, offZ);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (slice.vbo != null && OpenGlHelper.useVbo())
        {
            slice.vbo.bindBuffer();
            GlStateManager.glEnableClientState(32884);
            GlStateManager.glVertexPointer(3, 5126, 28, 0);
            GlStateManager.glEnableClientState(32886);
            GlStateManager.glColorPointer(4, 5121, 28, 12);
            GlStateManager.glEnableClientState(32888);
            GlStateManager.glTexCoordPointer(2, 5126, 28, 16);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.glEnableClientState(32888);
            GlStateManager.glTexCoordPointer(2, 5122, 28, 24);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);

            slice.vbo.drawArrays(7);
            slice.vbo.unbindBuffer();

            GlStateManager.glDisableClientState(32884);
            GlStateManager.glDisableClientState(32886);
            GlStateManager.glDisableClientState(32888);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.glDisableClientState(32888);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        }
        else if (slice.fallbackState != null)
        {
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            buf.begin(7, DefaultVertexFormats.BLOCK);
            buf.setVertexState(slice.fallbackState);
            tess.draw();
        }

        GlStateManager.popMatrix();
    }

    private static void renderDestinationEntities(Minecraft mc, World destWorld, PortalDestination dest, float partialTicks)
    {
        RenderManager rm = mc.getRenderManager();
        double maxDistSq = (double)(PORTAL_MAX_RENDER_DIST * PORTAL_MAX_RENDER_DIST);
        List<Entity> list = destWorld.loadedEntityList;
        int rendered = 0;

        RenderHelper.enableStandardItemLighting();

        for (int i = 0; i < list.size() && rendered < 48; ++i)
        {
            Entity ent = list.get(i);

            if (ent == null || ent.isDead || ent instanceof EntityPlayer)
            {
                continue;
            }

            double dx = ent.posX - dest.exitX;
            double dy = ent.posY - dest.exitY;
            double dz = ent.posZ - dest.exitZ;

            if (dx * dx + dy * dy + dz * dz <= maxDistSq)
            {
                double ex = ent.lastTickPosX + (ent.posX - ent.lastTickPosX) * (double)partialTicks;
                double ey = ent.lastTickPosY + (ent.posY - ent.lastTickPosY) * (double)partialTicks;
                double ez = ent.lastTickPosZ + (ent.posZ - ent.lastTickPosZ) * (double)partialTicks;
                float yaw = ent.prevRotationYaw + (ent.rotationYaw - ent.prevRotationYaw) * partialTicks;

                int bright = ent.getBrightnessForRender();
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)(bright % 65536), (float)(bright / 65536));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                rm.doRenderEntity(ent, ex - dest.exitX, ey - dest.exitY, ez - dest.exitZ, yaw, partialTicks, false);
                ++rendered;
            }
        }

        RenderHelper.disableStandardItemLighting();
    }

    private static void renderAtmosphereChamber(boolean isNetherDest)
    {
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);

        float r = isNetherDest ? 0.34F : 0.45F;
        float g = isNetherDest ? 0.06F : 0.65F;
        float b = isNetherDest ? 0.08F : 0.88F;
        double sz = 36.0D;

        buf.pos(-sz, -2.0D, -sz).color(r, g, b, 1.0F).endVertex();
        buf.pos(sz, -2.0D, -sz).color(r, g, b, 1.0F).endVertex();
        buf.pos(sz, -2.0D, sz).color(r, g, b, 1.0F).endVertex();
        buf.pos(-sz, -2.0D, sz).color(r, g, b, 1.0F).endVertex();
        tess.draw();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
    }

    private static void compositeWithStencil(Minecraft mc, Framebuffer mainFb, PortalOpening opening, int stencilRef)
    {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.colorMask(false, false, false, false);
        GlStateManager.depthMask(false);
        GlStateManager.enableDepth();

        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, stencilRef, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        // Stamp portal opening into stencil buffer where world depth test passes
        drawPortalQuad(mc, opening, 1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.colorMask(true, true, true, true);
        GL11.glStencilMask(0x00);
        GL11.glStencilFunc(GL11.GL_EQUAL, stencilRef, 0xFF);

        // Composite 0.5x destination FBO full-screen masked strictly by stencil
        GlStateManager.disableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();

        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, (double)mainFb.framebufferWidth, (double)mainFb.framebufferHeight, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        portalFramebuffer.bindFramebufferTexture();
        float w = (float)mainFb.framebufferWidth;
        float h = (float)mainFb.framebufferHeight;
        float uMax = (float)portalFramebuffer.framebufferWidth / (float)Math.max(1, portalFramebuffer.framebufferTextureWidth);
        float vMax = (float)portalFramebuffer.framebufferHeight / (float)Math.max(1, portalFramebuffer.framebufferTextureHeight);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        buf.pos(0.0D, (double)h, 0.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
        buf.pos((double)w, (double)h, 0.0D).tex((double)uMax, 0.0D).color(255, 255, 255, 255).endVertex();
        buf.pos((double)w, 0.0D, 0.0D).tex((double)uMax, (double)vMax).color(255, 255, 255, 255).endVertex();
        buf.pos(0.0D, 0.0D, 0.0D).tex(0.0D, (double)vMax).color(255, 255, 255, 255).endVertex();
        tess.draw();
        portalFramebuffer.unbindFramebufferTexture();

        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);

        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);

        // Subtle purple portal shimmer membrane on portal frame
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        drawPortalQuad(mc, opening, 0.62F, 0.24F, 0.96F, 0.14F);

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawPortalFallbackOverlay(Minecraft mc, PortalOpening opening)
    {
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        drawPortalQuad(mc, opening, 0.58F, 0.22F, 0.92F, 0.35F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawPortalQuad(Minecraft mc, PortalOpening op, float r, float g, float b, float a)
    {
        RenderManager rm = mc.getRenderManager();
        double ox = rm.viewerPosX;
        double oy = rm.viewerPosY;
        double oz = rm.viewerPosZ;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);

        if (op.axis == EnumFacing.Axis.X)
        {
            double z = op.centerZ - oz;
            buf.pos(op.minX - ox, op.minY - oy, z).color(r, g, b, a).endVertex();
            buf.pos(op.maxX - ox, op.minY - oy, z).color(r, g, b, a).endVertex();
            buf.pos(op.maxX - ox, op.maxY - oy, z).color(r, g, b, a).endVertex();
            buf.pos(op.minX - ox, op.maxY - oy, z).color(r, g, b, a).endVertex();
        }
        else
        {
            double x = op.centerX - ox;
            buf.pos(x, op.minY - oy, op.minZ - oz).color(r, g, b, a).endVertex();
            buf.pos(x, op.minY - oy, op.maxZ - oz).color(r, g, b, a).endVertex();
            buf.pos(x, op.maxY - oy, op.maxZ - oz).color(r, g, b, a).endVertex();
            buf.pos(x, op.maxY - oy, op.minZ - oz).color(r, g, b, a).endVertex();
        }

        tess.draw();
    }

    /**
     * Seamless Create Train portal crossing: transfers EntityArgonTrain + all carriages + schedule
     * + seated passengers across dimensions with zero loading screen.
     */
    public static boolean teleportTrainThroughPortal(EntityArgonTrain train, BlockPos portalPos)
    {
        if (!Config.isBetterPortals() || train == null || train.world == null || train.world.isRemote || train.isDead)
        {
            return false;
        }

        if (train.timeUntilPortal > 0)
        {
            return false;
        }

        MinecraftServer server = train.getServer();

        if (server == null || !server.getAllowNether())
        {
            return false;
        }

        int srcDim = train.dimension;
        if (srcDim != 0 && srcDim != -1)
        {
            return false;
        }

        int destDim = srcDim == -1 ? 0 : -1;
        WorldServer oldWorld = server.getWorld(srcDim);
        WorldServer destWorld = server.getWorld(destDim);

        if (oldWorld == null || destWorld == null)
        {
            return false;
        }

        // Snapshot source 3-chunk neighborhood before leaving
        captureCurrentWorldSnapshotAround(oldWorld, portalPos);

        double scaledX = destDim == -1 ? train.posX / 8.0D : train.posX * 8.0D;
        double scaledZ = destDim == -1 ? train.posZ / 8.0D : train.posZ * 8.0D;
        double clampedY = MathHelper.clamp(train.posY, 16.0D, destDim == -1 ? 116.0D : 220.0D);

        EntityArmorStand probe = new EntityArmorStand(destWorld, scaledX, clampedY, scaledZ);
        probe.rotationYaw = train.rotationYaw;
        Teleporter teleporter = destWorld.getDefaultTeleporter();
        teleporter.placeInPortal(probe, train.rotationYaw);

        BlockPos destPortalPos = findExistingPortalNear(destWorld, MathHelper.floor(probe.posX), MathHelper.floor(probe.posY), MathHelper.floor(probe.posZ), 16);
        double exitX = destPortalPos != null ? (double)destPortalPos.getX() + 0.5D : probe.posX;
        double exitY = destPortalPos != null ? (double)destPortalPos.getY() + 0.19D : probe.posY;
        double exitZ = destPortalPos != null ? (double)destPortalPos.getZ() + 0.5D : probe.posZ;

        // Nudge train lead forward 2.4 blocks clear of portal plane along exit heading
        float destYaw = probe.rotationYaw;
        double rad = Math.toRadians((double)destYaw);
        double fwdX = -Math.sin(rad);
        double fwdZ = Math.cos(rad);
        double dirSign = train.getTrainSpeed() < 0.0D ? -1.0D : 1.0D;
        double spawnX = exitX + fwdX * 2.4D * dirSign;
        double spawnZ = exitZ + fwdZ * 2.4D * dirSign;

        List<Entity> riders = new ArrayList<Entity>(train.getPassengers());
        for (int i = 0; i < riders.size(); ++i)
        {
            riders.get(i).dismountRidingEntity();
        }

        EntityArgonTrain newTrain = new EntityArgonTrain(destWorld);
        newTrain.copyTrainStateForPortal(train, spawnX, exitY, spawnZ, destYaw);
        newTrain.timeUntilPortal = 60;
        newTrain.forceSpawn = true;
        destWorld.spawnEntity(newTrain);
        destWorld.updateEntityWithOptionalForce(newTrain, false);

        for (int i = 0; i < riders.size(); ++i)
        {
            Entity passenger = riders.get(i);

            if (passenger instanceof EntityPlayerMP)
            {
                EntityPlayerMP mp = (EntityPlayerMP)passenger;
                markSeamlessTeleport();
                mp.timeUntilPortal = 60;
                server.getPlayerList().changePlayerDimension(mp, destDim);
                mp.connection.setPlayerLocation(spawnX, exitY + 1.0D, spawnZ, destYaw, mp.rotationPitch);
                mp.timeUntilPortal = 60;
                mp.startRiding(newTrain, true);
            }
            else if (!passenger.isDead)
            {
                passenger.timeUntilPortal = 60;
                Entity transferred = passenger.changeDimension(destDim);

                if (transferred != null)
                {
                    transferred.setLocationAndAngles(spawnX, exitY + 1.0D, spawnZ, destYaw, transferred.rotationPitch);
                    transferred.timeUntilPortal = 60;
                    transferred.startRiding(newTrain, true);
                }
            }
        }

        train.timeUntilPortal = 60;
        oldWorld.removeEntity(train);
        train.isDead = true;
        return true;
    }

    private static long packPortalKey(int dim, int x, int y, int z)
    {
        long h = ((long)(dim & 15) << 60);
        h |= ((long)(x & 0xFFFFF) << 40);
        h |= ((long)(y & 0xFFFFF) << 20);
        h |= ((long)(z & 0xFFFFF));
        return h;
    }
}
