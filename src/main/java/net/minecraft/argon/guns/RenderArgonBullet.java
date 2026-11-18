package net.minecraft.argon.guns;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Lightweight 3D projectile renderer for tracers, rockets, and glowing plasma bolts.
 */
public class RenderArgonBullet extends Render<EntityArgonBullet>
{
    public RenderArgonBullet(RenderManager renderManager)
    {
        super(renderManager);
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntityArgonBullet entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        EntityArgonBullet.BulletKind kind = entity.getBulletKind();
        int rgb = kind.tracerColor;
        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-pitch, 1.0F, 0.0F, 0.0F);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        if (kind == EntityArgonBullet.BulletKind.ROCKET)
        {
            // Olive warhead body + glowing thruster tail
            GunModelRenderer.addBox(buf, -0.085F, -0.085F, -0.28F, 0.085F, 0.085F, 0.24F, 0.34F, 0.42F, 0.28F);
            GunModelRenderer.addBox(buf, -0.065F, -0.065F, 0.24F, 0.065F, 0.065F, 0.42F, 0.78F, 0.22F, 0.16F);
            GunModelRenderer.addBox(buf, -0.055F, -0.055F, -0.44F, 0.055F, 0.055F, -0.28F, 1.0F, 0.65F, 0.18F);
        }
        else if (kind == EntityArgonBullet.BulletKind.PLASMA)
        {
            GunModelRenderer.addBox(buf, -0.06F, -0.06F, -0.38F, 0.06F, 0.06F, 0.38F, r, g, b);
            GunModelRenderer.addBox(buf, -0.032F, -0.032F, -0.46F, 0.032F, 0.032F, 0.46F, 0.90F, 0.98F, 1.0F);
        }
        else
        {
            float half = kind == EntityArgonBullet.BulletKind.SNIPER ? 0.042F : 0.032F;
            float len = kind == EntityArgonBullet.BulletKind.SNIPER ? 0.52F : 0.32F;
            GunModelRenderer.addBox(buf, -half, -half, -len, half, half, len, r, g, b);
        }

        tess.draw();

        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityArgonBullet entity)
    {
        return null;
    }
}
