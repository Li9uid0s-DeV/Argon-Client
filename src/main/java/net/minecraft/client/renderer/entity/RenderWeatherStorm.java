package net.minecraft.client.renderer.entity;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.effect.EntityWeatherStorm;
import net.minecraft.src.Config;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class RenderWeatherStorm extends Render<EntityWeatherStorm>
{
    public RenderWeatherStorm(RenderManager renderManagerIn)
    {
        super(renderManagerIn);
    }

    @Override
    public void doRender(EntityWeatherStorm entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        int quality = Config.getWeather2Quality();

        if (quality <= 0)
        {
            return;
        }

        int efScale = entity.getEfScale();
        float funnelH = entity.getFunnelHeight();
        float baseR = entity.getFunnelBaseRadius();
        float topR = entity.getFunnelTopRadius();
        float rotDeg = entity.getInterpolatedRotation(partialTicks);
        float timeSec = ((float)entity.ticksExisted + partialTicks) * 0.05F;

        int ringLayers = quality == 1 ? 6 : (quality == 2 ? 10 : 16);
        int segsPerRing = quality == 1 ? 8 : (quality == 2 ? 12 : 16);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y, (float)z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 1. Batched Tornado Funnel & Supercell Cloud Wall (Single Draw Call)
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        if (efScale >= 1)
        {
            float baseTone = Math.max(0.10F, 0.34F - (float)efScale * 0.032F);
            float baseAlpha = Math.min(0.82F, 0.44F + (float)efScale * 0.05F);

            for (int layer = 0; layer < ringLayers; ++layer)
            {
                float t0 = (float)layer / (float)ringLayers;
                float t1 = (float)(layer + 1) / (float)ringLayers;
                float y0 = t0 * funnelH;
                float y1 = t1 * funnelH;

                float r0 = baseR + (topR - baseR) * (t0 * t0);
                float r1 = baseR + (topR - baseR) * (t1 * t1);

                // Natural snake-like tornado bend
                float swayX0 = MathHelper.sin(timeSec * 1.7F + t0 * 4.2F) * (1.2F + (1.0F - t0) * 2.4F);
                float swayZ0 = MathHelper.cos(timeSec * 1.4F + t0 * 3.8F) * (1.2F + (1.0F - t0) * 2.4F);
                float swayX1 = MathHelper.sin(timeSec * 1.7F + t1 * 4.2F) * (1.2F + (1.0F - t1) * 2.4F);
                float swayZ1 = MathHelper.cos(timeSec * 1.4F + t1 * 3.8F) * (1.2F + (1.0F - t1) * 2.4F);

                float twist0 = (rotDeg * 0.017453292F) * (1.3F - t0 * 0.5F);
                float twist1 = (rotDeg * 0.017453292F) * (1.3F - t1 * 0.5F);

                float c0 = MathHelper.clamp(baseTone + t0 * 0.12F, 0.08F, 0.55F);
                float c1 = MathHelper.clamp(baseTone + t1 * 0.12F, 0.08F, 0.55F);

                for (int s = 0; s < segsPerRing; ++s)
                {
                    float a0 = (float)s / (float)segsPerRing * ((float)Math.PI * 2.0F);
                    float a1 = (float)(s + 1) / (float)segsPerRing * ((float)Math.PI * 2.0F);

                    float x00 = swayX0 + MathHelper.cos(a0 + twist0) * r0;
                    float z00 = swayZ0 + MathHelper.sin(a0 + twist0) * r0;
                    float x01 = swayX0 + MathHelper.cos(a1 + twist0) * r0;
                    float z01 = swayZ0 + MathHelper.sin(a1 + twist0) * r0;

                    float x11 = swayX1 + MathHelper.cos(a1 + twist1) * r1;
                    float z11 = swayZ1 + MathHelper.sin(a1 + twist1) * r1;
                    float x10 = swayX1 + MathHelper.cos(a0 + twist1) * r1;
                    float z10 = swayZ1 + MathHelper.sin(a0 + twist1) * r1;

                    float shade = 0.88F + 0.12F * MathHelper.sin(a0 * 2.0F + twist0);
                    float rCol0 = c0 * shade;
                    float gCol0 = c0 * shade * 1.02F;
                    float bCol0 = c0 * shade * 1.08F;
                    float rCol1 = c1 * shade;
                    float gCol1 = c1 * shade * 1.02F;
                    float bCol1 = c1 * shade * 1.08F;

                    buffer.pos((double)x00, (double)y0, (double)z00).color(rCol0, gCol0, bCol0, baseAlpha).endVertex();
                    buffer.pos((double)x01, (double)y0, (double)z01).color(rCol0, gCol0, bCol0, baseAlpha).endVertex();
                    buffer.pos((double)x11, (double)y1, (double)z11).color(rCol1, gCol1, bCol1, baseAlpha * 0.92F).endVertex();
                    buffer.pos((double)x10, (double)y1, (double)z10).color(rCol1, gCol1, bCol1, baseAlpha * 0.92F).endVertex();
                }
            }
        }

        // Supercell rotating wall cloud / anvil deck at top of storm
        float cloudBaseY = efScale >= 1 ? funnelH * 0.88F : 32.0F;
        float cloudHeight = 9.0F + (float)efScale * 1.5F;
        float cloudInnerR = efScale >= 1 ? topR * 0.45F : 3.0F;
        float cloudOuterR = topR * 1.65F;
        float cloudTwist = -rotDeg * 0.008F;

        for (int s = 0; s < segsPerRing; ++s)
        {
            float a0 = (float)s / (float)segsPerRing * ((float)Math.PI * 2.0F) + cloudTwist;
            float a1 = (float)(s + 1) / (float)segsPerRing * ((float)Math.PI * 2.0F) + cloudTwist;

            float ix0 = MathHelper.cos(a0) * cloudInnerR;
            float iz0 = MathHelper.sin(a0) * cloudInnerR;
            float ix1 = MathHelper.cos(a1) * cloudInnerR;
            float iz1 = MathHelper.sin(a1) * cloudInnerR;

            float ox0 = MathHelper.cos(a0) * cloudOuterR;
            float oz0 = MathHelper.sin(a0) * cloudOuterR;
            float ox1 = MathHelper.cos(a1) * cloudOuterR;
            float oz1 = MathHelper.sin(a1) * cloudOuterR;

            buffer.pos((double)ix0, (double)cloudBaseY, (double)iz0).color(0.16F, 0.17F, 0.20F, 0.78F).endVertex();
            buffer.pos((double)ix1, (double)cloudBaseY, (double)iz1).color(0.16F, 0.17F, 0.20F, 0.78F).endVertex();
            buffer.pos((double)ox1, (double)(cloudBaseY + cloudHeight), (double)oz1).color(0.24F, 0.26F, 0.30F, 0.25F).endVertex();
            buffer.pos((double)ox0, (double)(cloudBaseY + cloudHeight), (double)oz0).color(0.24F, 0.26F, 0.30F, 0.25F).endVertex();
        }

        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();

        // 2. Render Lifted Block Debris (Capped by Low / Medium / High setting)
        List<EntityWeatherStorm.LiftedBlockDebris> debrisList = entity.getLiftedDebris();
        int maxBlocks = Math.min(debrisList.size(), EntityWeatherStorm.getMaxLiftedBlocks(quality));

        if (maxBlocks > 0)
        {
            this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();

            for (int i = 0; i < maxBlocks; ++i)
            {
                EntityWeatherStorm.LiftedBlockDebris debris = debrisList.get(i);
                float interpAngle = debris.orbitAngle + debris.angularVelocity * partialTicks;
                float interpY = debris.heightOffset + 0.18F * partialTicks;
                float tNorm = MathHelper.clamp(interpY / funnelH, 0.0F, 1.0F);
                float swayX = MathHelper.sin(timeSec * 1.7F + tNorm * 4.2F) * (1.2F + (1.0F - tNorm) * 2.4F);
                float swayZ = MathHelper.cos(timeSec * 1.4F + tNorm * 3.8F) * (1.2F + (1.0F - tNorm) * 2.4F);

                float bx = swayX + MathHelper.cos(interpAngle) * debris.orbitRadius;
                float bz = swayZ + MathHelper.sin(interpAngle) * debris.orbitRadius;

                GlStateManager.pushMatrix();
                GlStateManager.translate(bx, interpY, bz);
                GlStateManager.rotate(debris.tumbleYaw + partialTicks * 10.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(debris.tumblePitch + partialTicks * 7.0F, 1.0F, 0.0F, 1.0F);
                GlStateManager.scale(0.75F, 0.75F, 0.75F);
                GlStateManager.translate(-0.5F, -0.5F, -0.5F);
                blockRenderer.renderBlockBrightness(debris.blockState, 0.85F);
                GlStateManager.popMatrix();
            }
        }

        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityWeatherStorm entity)
    {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
