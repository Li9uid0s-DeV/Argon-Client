package net.minecraft.argon.guns;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Custom TESR for BlockGunWorkbench and BlockAutoTurret using Minecraft's
 * GlStateManager + Tessellator batched quad pipeline.
 */
public class TileEntityGunRenderer extends TileEntitySpecialRenderer<TileEntity>
{
    @Override
    public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
    {
        if (te instanceof TileEntityAutoTurret)
        {
            this.renderAutoTurret((TileEntityAutoTurret)te, x, y, z, partialTicks);
        }
        else if (te instanceof TileEntityGunWorkbench)
        {
            this.renderWorkbench(x, y, z);
        }
    }

    private void renderWorkbench(double x, double y, double z)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // Heavy steel legs
        GunModelRenderer.addBox(buf, -0.46F, 0.0F, -0.46F, -0.34F, 0.78F, -0.34F, 0.24F, 0.27F, 0.31F);
        GunModelRenderer.addBox(buf, 0.34F, 0.0F, -0.46F, 0.46F, 0.78F, -0.34F, 0.24F, 0.27F, 0.31F);
        GunModelRenderer.addBox(buf, -0.46F, 0.0F, 0.34F, -0.34F, 0.78F, 0.46F, 0.24F, 0.27F, 0.31F);
        GunModelRenderer.addBox(buf, 0.34F, 0.0F, 0.34F, 0.46F, 0.78F, 0.46F, 0.24F, 0.27F, 0.31F);

        // Lower ammo storage shelf
        GunModelRenderer.addBox(buf, -0.42F, 0.20F, -0.42F, 0.42F, 0.27F, 0.42F, 0.20F, 0.23F, 0.27F);
        GunModelRenderer.addBox(buf, -0.34F, 0.27F, -0.26F, 0.04F, 0.48F, 0.14F, 0.24F, 0.36F, 0.20F);

        // Tabletop + Blueprint mat + Vise clamp
        GunModelRenderer.addBox(buf, -0.49F, 0.78F, -0.49F, 0.49F, 0.90F, 0.49F, 0.32F, 0.36F, 0.42F);
        GunModelRenderer.addBox(buf, -0.40F, 0.901F, -0.36F, 0.36F, 0.915F, 0.36F, 0.14F, 0.42F, 0.72F);
        GunModelRenderer.addBox(buf, -0.32F, 0.915F, -0.10F, -0.18F, 1.04F, 0.10F, 0.48F, 0.50F, 0.54F);
        GunModelRenderer.addBox(buf, 0.16F, 0.915F, -0.10F, 0.28F, 1.04F, 0.10F, 0.48F, 0.50F, 0.54F);

        // Display rifle silhouette resting on workbench vise
        GunModelRenderer.addBox(buf, -0.36F, 0.97F, -0.03F, 0.36F, 1.06F, 0.03F, 0.16F, 0.18F, 0.21F);
        GunModelRenderer.addBox(buf, -0.10F, 1.06F, -0.025F, 0.10F, 1.12F, 0.025F, 0.12F, 0.62F, 0.88F);

        tess.draw();

        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void renderAutoTurret(TileEntityAutoTurret turret, double x, double y, double z, float partialTicks)
    {
        float yawDiff = MathHelper.wrapDegrees(turret.turretYaw - turret.prevTurretYaw);
        float yaw = turret.prevTurretYaw + yawDiff * partialTicks;
        float pitch = turret.prevTurretPitch + (turret.turretPitch - turret.prevTurretPitch) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // Stationary armored tripod pedestal
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GunModelRenderer.addBox(buf, -0.38F, 0.0F, -0.38F, 0.38F, 0.10F, 0.38F, 0.24F, 0.26F, 0.30F);
        GunModelRenderer.addBox(buf, -0.16F, 0.10F, -0.16F, 0.16F, 0.48F, 0.16F, 0.30F, 0.33F, 0.38F);
        GunModelRenderer.addBox(buf, -0.24F, 0.46F, -0.24F, 0.24F, 0.54F, 0.24F, 0.22F, 0.24F, 0.28F);
        tess.draw();

        // Rotating turret head & twin barrels
        GlStateManager.translate(0.0F, 0.72F, 0.0F);
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        // Armored receiver housing
        GunModelRenderer.addBox(buf, -0.22F, -0.16F, -0.24F, 0.22F, 0.18F, 0.26F, 0.28F, 0.34F, 0.26F);
        // Side ammo drum
        GunModelRenderer.addBox(buf, 0.22F, -0.14F, -0.12F, 0.36F, 0.10F, 0.14F, 0.36F, 0.34F, 0.22F);
        // Sensor optic pod + status LED (green active, red target locked, amber standby)
        float ledR = !turret.isEnabled() || turret.getAmmoCount() <= 0 ? 0.90F : (turret.hasLockedTarget ? 1.0F : 0.18F);
        float ledG = !turret.isEnabled() || turret.getAmmoCount() <= 0 ? 0.55F : (turret.hasLockedTarget ? 0.15F : 0.95F);
        float ledB = 0.18F;
        GunModelRenderer.addBox(buf, -0.10F, 0.18F, -0.10F, 0.10F, 0.28F, 0.16F, 0.18F, 0.20F, 0.24F);
        GunModelRenderer.addBox(buf, -0.06F, 0.20F, 0.16F, 0.06F, 0.26F, 0.185F, ledR, ledG, ledB);

        // Twin autocannon barrels extending along +Z (matches atan2(-dx, dz) yaw convention)
        GunModelRenderer.addBox(buf, -0.13F, -0.04F, 0.26F, -0.05F, 0.05F, 0.86F, 0.20F, 0.22F, 0.25F);
        GunModelRenderer.addBox(buf, 0.05F, -0.04F, 0.26F, 0.13F, 0.05F, 0.86F, 0.20F, 0.22F, 0.25F);

        // Targeting laser when locked on hostile mob
        if (turret.isEnabled() && turret.hasLockedTarget && turret.getAmmoCount() > 0)
        {
            GunModelRenderer.addBox(buf, -0.012F, 0.22F, 0.19F, 0.012F, 0.244F, 6.5F, 1.0F, 0.12F, 0.12F);
        }

        // Muzzle flash
        if (turret.muzzleFlashTicks > 0)
        {
            GunModelRenderer.addBox(buf, -0.18F, -0.08F, 0.86F, 0.18F, 0.09F, 1.06F, 1.0F, 0.82F, 0.24F);
        }

        tess.draw();

        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
