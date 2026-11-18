package net.minecraft.argon.tnt;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/**
 * Lightweight 3D renderer for EntityArgonTNTPrimed using only GlStateManager & Tessellator.
 * Renders each TntType's color-coded body, center stripe, fuse cap, detonation swell, and flash.
 */
public class RenderArgonTNTPrimed extends Render<EntityArgonTNTPrimed>
{
    public RenderArgonTNTPrimed(RenderManager renderManagerIn)
    {
        super(renderManagerIn);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(EntityArgonTNTPrimed entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        TntType type = entity.getTntType();
        int fuse = entity.getFuse();

        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y + 0.5F, (float)z);

        // Classic Minecraft pre-detonation swell during final 10 ticks
        if ((float)fuse - partialTicks + 1.0F < 10.0F)
        {
            float f = 1.0F - ((float)fuse - partialTicks + 1.0F) / 10.0F;
            f = MathHelper.clamp(f, 0.0F, 1.0F);
            f = f * f * f * f;
            float scale = 1.0F + f * 0.32F;
            GlStateManager.scale(scale, scale, scale);
        }

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );

        boolean whiteFlash = (fuse / 5) % 2 == 0;
        float flashBlend = whiteFlash ? MathHelper.clamp((1.0F - ((float)fuse - partialTicks + 1.0F) / 100.0F) * 0.85F, 0.35F, 0.9F) : 0.0F;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);

        // Lower body section
        drawBox(buf, -0.5F, -0.5F, -0.5F, 0.5F, -0.18F, 0.5F, blendFlash(type.bodyColor, flashBlend));
        // Center labeled stripe
        drawBox(buf, -0.503F, -0.18F, -0.503F, 0.503F, 0.18F, 0.503F, blendFlash(type.bandColor, flashBlend));
        // Upper body section
        drawBox(buf, -0.5F, 0.18F, -0.5F, 0.5F, 0.44F, 0.5F, blendFlash(type.bodyColor, flashBlend));
        // Top cap
        drawBox(buf, -0.5F, 0.44F, -0.5F, 0.5F, 0.5F, 0.5F, blendFlash(type.topColor, flashBlend));
        // Fuse stud on top
        drawBox(buf, -0.06F, 0.5F, -0.06F, 0.06F, 0.64F, 0.06F, whiteFlash ? 0xFFF4B0 : 0x333333);

        tess.draw();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private static int blendFlash(int rgb, float flashAmount)
    {
        if (flashAmount <= 0.01F)
        {
            return rgb;
        }

        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        r = Math.min(255, (int)(r + (255 - r) * flashAmount));
        g = Math.min(255, (int)(g + (255 - g) * flashAmount));
        b = Math.min(255, (int)(b + (255 - b) * flashAmount));
        return (r << 16) | (g << 8) | b;
    }

    private static void drawBox(BufferBuilder buf, float x0, float y0, float z0, float x1, float y1, float z1, int rgb)
    {
        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;

        // Top (+Y)
        addQuad(buf, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b);
        // Bottom (-Y)
        addQuad(buf, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, r * 0.55F, g * 0.55F, b * 0.55F);
        // North (-Z)
        addQuad(buf, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r * 0.82F, g * 0.82F, b * 0.82F);
        // South (+Z)
        addQuad(buf, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r * 0.82F, g * 0.82F, b * 0.82F);
        // West (-X)
        addQuad(buf, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r * 0.68F, g * 0.68F, b * 0.68F);
        // East (+X)
        addQuad(buf, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r * 0.68F, g * 0.68F, b * 0.68F);
    }

    private static void addQuad(
        BufferBuilder buf,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float r, float g, float b)
    {
        buf.pos(x0, y0, z0).color(r, g, b, 1.0F).endVertex();
        buf.pos(x1, y1, z1).color(r, g, b, 1.0F).endVertex();
        buf.pos(x2, y2, z2).color(r, g, b, 1.0F).endVertex();
        buf.pos(x3, y3, z3).color(r, g, b, 1.0F).endVertex();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityArgonTNTPrimed entity)
    {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
