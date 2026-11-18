package net.minecraft.client.renderer.tileentity;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelShulker;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.src.Config;
import net.minecraft.src.Reflector;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.tileentity.TileEntityStructure;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class TileEntityRendererDispatcher
{
    public final Map <Class, TileEntitySpecialRenderer> renderers = Maps.newHashMap();
    public static TileEntityRendererDispatcher instance = new TileEntityRendererDispatcher();
    public FontRenderer fontRenderer;

    /** The player's current X position (same as playerX) */
    public static double staticPlayerX;

    /** The player's current Y position (same as playerY) */
    public static double staticPlayerY;

    /** The player's current Z position (same as playerZ) */
    public static double staticPlayerZ;
    public TextureManager renderEngine;
    public World world;
    public Entity entity;
    public float entityYaw;
    public float entityPitch;
    public RayTraceResult cameraHitResult;
    public double entityX;
    public double entityY;
    public double entityZ;
    public TileEntity tileEntityRendered;
    public ICamera currentCamera;
    private Tessellator batchBuffer = new Tessellator(2097152);
    private boolean drawingBatch = false;

    private TileEntityRendererDispatcher()
    {
        this.renderers.put(TileEntitySign.class, new TileEntitySignRenderer());
        this.renderers.put(TileEntityMobSpawner.class, new TileEntityMobSpawnerRenderer());
        this.renderers.put(TileEntityPiston.class, new TileEntityPistonRenderer());
        this.renderers.put(TileEntityChest.class, new TileEntityChestRenderer());
        this.renderers.put(TileEntityEnderChest.class, new TileEntityEnderChestRenderer());
        this.renderers.put(TileEntityEnchantmentTable.class, new TileEntityEnchantmentTableRenderer());
        this.renderers.put(TileEntityEndPortal.class, new TileEntityEndPortalRenderer());
        this.renderers.put(TileEntityEndGateway.class, new TileEntityEndGatewayRenderer());
        this.renderers.put(TileEntityBeacon.class, new TileEntityBeaconRenderer());
        this.renderers.put(TileEntitySkull.class, new TileEntitySkullRenderer());
        this.renderers.put(TileEntityBanner.class, new TileEntityBannerRenderer());
        this.renderers.put(TileEntityStructure.class, new TileEntityStructureRenderer());
        this.renderers.put(TileEntityShulkerBox.class, new TileEntityShulkerBoxRenderer(new ModelShulker()));
        this.renderers.put(TileEntityBed.class, new TileEntityBedRenderer());
        this.renderers.put(net.minecraft.argon.create.TileEntityCreateBase.class, new net.minecraft.argon.create.TileEntityCreateRenderer());
        net.minecraft.argon.guns.TileEntityGunRenderer gunTesr = new net.minecraft.argon.guns.TileEntityGunRenderer();
        this.renderers.put(net.minecraft.argon.guns.TileEntityGunWorkbench.class, gunTesr);
        this.renderers.put(net.minecraft.argon.guns.TileEntityAutoTurret.class, gunTesr);

        for (TileEntitySpecialRenderer<?> tileentityspecialrenderer : this.renderers.values())
        {
            tileentityspecialrenderer.setRendererDispatcher(this);
        }
    }

    public <T extends TileEntity> TileEntitySpecialRenderer<T> getRenderer(Class <? extends TileEntity > teClass)
    {
        TileEntitySpecialRenderer<T> tileentityspecialrenderer = (TileEntitySpecialRenderer)this.renderers.get(teClass);

        if (tileentityspecialrenderer == null && teClass != TileEntity.class)
        {
            tileentityspecialrenderer = this.getRenderer((Class<? extends TileEntity>) teClass.getSuperclass());
            this.renderers.put(teClass, tileentityspecialrenderer);
        }

        return tileentityspecialrenderer;
    }

    @Nullable
    public <T extends TileEntity> TileEntitySpecialRenderer<T> getRenderer(@Nullable TileEntity tileEntityIn)
    {
        return tileEntityIn == null ? null : this.getRenderer(tileEntityIn.getClass());
    }

    public void prepare(World worldIn, TextureManager renderEngineIn, FontRenderer fontRendererIn, Entity entityIn, RayTraceResult cameraHitResultIn, float p_190056_6_)
    {
        if (this.world != worldIn)
        {
            this.setWorld(worldIn);
        }

        this.renderEngine = renderEngineIn;
        this.entity = entityIn;
        this.fontRenderer = fontRendererIn;
        this.cameraHitResult = cameraHitResultIn;
        this.entityYaw = entityIn.prevRotationYaw + (entityIn.rotationYaw - entityIn.prevRotationYaw) * p_190056_6_;
        this.entityPitch = entityIn.prevRotationPitch + (entityIn.rotationPitch - entityIn.prevRotationPitch) * p_190056_6_;
        this.entityX = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * (double)p_190056_6_;
        this.entityY = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * (double)p_190056_6_;
        this.entityZ = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * (double)p_190056_6_;
    }

    public void setCamera(@Nullable ICamera cameraIn)
    {
        this.currentCamera = cameraIn;
    }

    public boolean isTileEntityInFrustum(TileEntity tileentityIn, @Nullable ICamera cameraIn)
    {
        if (cameraIn == null || tileentityIn instanceof TileEntityStructure)
        {
            return true;
        }

        BlockPos blockpos = tileentityIn.getPos();
        double minX = (double)blockpos.getX();
        double minY = (double)blockpos.getY();
        double minZ = (double)blockpos.getZ();
        double maxX = minX + 1.0D;
        double maxY = minY + 1.0D;
        double maxZ = minZ + 1.0D;

        if (tileentityIn instanceof TileEntityBeacon || tileentityIn instanceof TileEntityEndGateway)
        {
            maxY = 256.0D;
        }
        else if (tileentityIn instanceof TileEntityChest || tileentityIn instanceof TileEntityBed || tileentityIn instanceof TileEntityBanner || tileentityIn instanceof TileEntityPiston)
        {
            minX -= 1.0D;
            minY -= 0.5D;
            minZ -= 1.0D;
            maxX += 1.0D;
            maxY += 1.5D;
            maxZ += 1.0D;
        }

        if (cameraIn instanceof Frustum)
        {
            return ((Frustum)cameraIn).isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
        }

        return cameraIn.isBoundingBoxInFrustum(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    public boolean shouldRenderTileEntity(TileEntity tileentityIn, @Nullable ICamera cameraIn)
    {
        if (tileentityIn == null || tileentityIn.isInvalid())
        {
            return false;
        }

        if (tileentityIn.getDistanceSq(this.entityX, this.entityY, this.entityZ) >= tileentityIn.getMaxRenderDistanceSquared())
        {
            return false;
        }

        if (Config.isEntityCulling() && cameraIn != null && !this.isTileEntityInFrustum(tileentityIn, cameraIn))
        {
            return false;
        }

        return true;
    }

    public void render(TileEntity tileentityIn, float partialTicks, int destroyStage)
    {
        if (this.shouldRenderTileEntity(tileentityIn, destroyStage < 0 ? this.currentCamera : null))
        {
            RenderHelper.enableStandardItemLighting();
            boolean flag = true;

            if (Reflector.ForgeTileEntity_hasFastRenderer.exists())
            {
                flag = !this.drawingBatch || !Reflector.callBoolean(tileentityIn, Reflector.ForgeTileEntity_hasFastRenderer);
            }

            if (flag)
            {
                int i = this.world.getCombinedLight(tileentityIn.getPos(), 0);
                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j, (float)k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            BlockPos blockpos = tileentityIn.getPos();
            this.render(tileentityIn, (double)blockpos.getX() - staticPlayerX, (double)blockpos.getY() - staticPlayerY, (double)blockpos.getZ() - staticPlayerZ, partialTicks, destroyStage, 1.0F);
        }
    }

    /**
     * Render this TileEntity at a given set of coordinates
     */
    public void render(TileEntity tileEntityIn, double x, double y, double z, float partialTicks)
    {
        this.render(tileEntityIn, x, y, z, partialTicks, 1.0F);
    }

    public void render(TileEntity p_192855_1_, double p_192855_2_, double p_192855_4_, double p_192855_6_, float p_192855_8_, float p_192855_9_)
    {
        this.render(p_192855_1_, p_192855_2_, p_192855_4_, p_192855_6_, p_192855_8_, -1, p_192855_9_);
    }

    public void render(TileEntity tileEntityIn, double x, double y, double z, float partialTicks, int destroyStage, float p_192854_10_)
    {
        TileEntitySpecialRenderer<TileEntity> tileentityspecialrenderer = this.<TileEntity>getRenderer(tileEntityIn);

        if (tileentityspecialrenderer != null)
        {
            try
            {
                this.tileEntityRendered = tileEntityIn;

                if (this.drawingBatch && Reflector.callBoolean(tileEntityIn, Reflector.ForgeTileEntity_hasFastRenderer))
                {
                    tileentityspecialrenderer.renderTileEntityFast(tileEntityIn, x, y, z, partialTicks, destroyStage, p_192854_10_, this.batchBuffer.getBuffer());
                }
                else
                {
                    tileentityspecialrenderer.render(tileEntityIn, x, y, z, partialTicks, destroyStage, p_192854_10_);
                }

                this.tileEntityRendered = null;
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Rendering Block Entity");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Block Entity Details");
                tileEntityIn.addInfoToCrashReport(crashreportcategory);
                throw new ReportedException(crashreport);
            }
        }
    }

    public void setWorld(@Nullable World worldIn)
    {
        this.world = worldIn;

        if (worldIn == null)
        {
            this.entity = null;
        }
    }

    public FontRenderer getFontRenderer()
    {
        return this.fontRenderer;
    }

    public void preDrawBatch()
    {
        this.batchBuffer.getBuffer().begin(7, DefaultVertexFormats.BLOCK);
        this.drawingBatch = true;
    }

    public void drawBatch(int p_drawBatch_1_)
    {
        this.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();

        if (Minecraft.isAmbientOcclusionEnabled())
        {
            GlStateManager.shadeModel(7425);
        }
        else
        {
            GlStateManager.shadeModel(7424);
        }

        if (p_drawBatch_1_ > 0)
        {
            this.batchBuffer.getBuffer().sortVertexData((float)staticPlayerX, (float)staticPlayerY, (float)staticPlayerZ);
        }

        this.batchBuffer.draw();
        RenderHelper.enableStandardItemLighting();
        this.drawingBatch = false;
    }
}
