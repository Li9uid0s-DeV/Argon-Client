package net.minecraft.argon.guns;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import org.lwjgl.opengl.GL11;

/**
 * Procedural 3D firearm & attachment renderer built strictly on Minecraft 1.12's
 * GlStateManager, Tessellator, and BufferBuilder pipeline (no external 3D libs).
 * Batches all solid gun geometry into a single GL_QUADS draw call per render pass
 * for high frame rates on MediaTek Kompanio 500 hardware.
 */
public final class GunModelRenderer
{
    private GunModelRenderer()
    {
    }

    /**
     * Renders a firearm in first-person view with smooth ADS alignment,
     * recoil kick, reload tilt animation, barrel spin, and muzzle flash.
     */
    public static void renderFirstPerson(ItemStack stack, EnumHandSide handSide, float partialTicks)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArgonGun))
        {
            return;
        }

        ItemArgonGun gun = (ItemArgonGun)stack.getItem();
        GunType type = gun.getGunType();

        float ads = GunClientState.getAdsProgress(partialTicks);

        // When zoomed into a full-screen 4x/8x sniper scope, hide the gun mesh so it never clips the viewport
        if (ads > 0.88F && GunAttachment.usesFullScopeOverlay(type, stack))
        {
            return;
        }

        float side = handSide == EnumHandSide.RIGHT ? 1.0F : -1.0F;
        float recoilKick = GunClientState.getRecoilKick(partialTicks);
        float recoilPitch = GunClientState.getRecoilPitch(partialTicks);
        float reloadFrac = GunClientState.getReloadAnimFraction(stack, partialTicks);
        float spinAngle = GunClientState.getMinigunSpinAngle(partialTicks);

        // Interpolate from hip-fire position toward exact center-eye ADS position
        float hipX = side * 0.42F;
        float hipY = -0.34F;
        float hipZ = -0.78F;

        float adsX = 0.0F;
        float adsY = getAdsEyeHeightOffset(type, stack);
        float adsZ = -0.56F;

        float tx = hipX + (adsX - hipX) * ads;
        float ty = hipY + (adsY - hipY) * ads;
        float tz = hipZ + (adsZ - hipZ) * ads + recoilKick * 0.14F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(tx, ty, tz);

        // Reload dip & roll animation
        if (reloadFrac > 0.0F)
        {
            float dip = (float)Math.sin((double)reloadFrac * Math.PI);
            GlStateManager.translate(side * 0.08F * dip, -0.22F * dip, 0.04F * dip);
            GlStateManager.rotate(-28.0F * dip, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(side * 34.0F * dip, 0.0F, 0.0F, 1.0F);
        }

        // Subtle hip yaw toe-in that straightens out during ADS
        GlStateManager.rotate(side * 3.5F * (1.0F - ads), 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(recoilPitch * 2.4F, 1.0F, 0.0F, 0.0F);

        float scale = type == GunType.PISTOL_9MM ? 0.62F : (type == GunType.MINIGUN || type == GunType.ROCKET_LAUNCHER ? 0.85F : 0.72F);
        GlStateManager.scale(scale, scale, scale);

        boolean showFlash = GunClientState.getMuzzleFlashTicks() > 0 && !GunAttachment.getMuzzle(stack).hidesMuzzleFlash;
        renderGunGeometry(stack, type, spinAngle, showFlash, reloadFrac);

        GlStateManager.popMatrix();
    }

    private static float getAdsEyeHeightOffset(GunType type, ItemStack stack)
    {
        GunAttachment.ScopeType scope = GunAttachment.getScope(stack);

        if (scope == GunAttachment.ScopeType.REFLEX)
        {
            return -0.255F;
        }

        if (scope == GunAttachment.ScopeType.SCOPE_4X || scope == GunAttachment.ScopeType.SCOPE_8X)
        {
            return -0.275F;
        }

        if (type == GunType.PISTOL_9MM)
        {
            return -0.19F;
        }

        return -0.215F;
    }

    /**
     * Renders a firearm held in third-person (player or NPC soldier hand).
     */
    public static void renderThirdPerson(ItemStack stack, boolean leftHanded, float spinAngle)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArgonGun))
        {
            return;
        }

        ItemArgonGun gun = (ItemArgonGun)stack.getItem();
        GunType type = gun.getGunType();

        GlStateManager.pushMatrix();
        GlStateManager.translate(leftHanded ? 0.04F : -0.04F, 0.06F, 0.08F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        float scale = type == GunType.PISTOL_9MM ? 0.55F : 0.72F;
        GlStateManager.scale(scale, scale, scale);
        renderGunGeometry(stack, type, spinAngle, false, 0.0F);
        GlStateManager.popMatrix();
    }

    /**
     * Renders a rotating 3D gun preview inside GuiGunWorkbench or TileEntityGunWorkbench.
     */
    public static void renderPreviewModel(ItemStack stack, float spinAngle)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArgonGun))
        {
            return;
        }

        ItemArgonGun gun = (ItemArgonGun)stack.getItem();
        renderGunGeometry(stack, gun.getGunType(), spinAngle, false, 0.0F);
    }

    /**
     * Core procedural 3D weapon & attachment geometry builder.
     * Local coordinate frame: +X right, +Y up, -Z forward toward muzzle.
     */
    public static void renderGunGeometry(ItemStack stack, GunType type, float spinAngle, boolean muzzleFlash, float reloadFrac)
    {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableCull();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float r1 = ((type.primaryColor >> 16) & 255) / 255.0F;
        float g1 = ((type.primaryColor >> 8) & 255) / 255.0F;
        float b1 = (type.primaryColor & 255) / 255.0F;

        float r2 = ((type.accentColor >> 16) & 255) / 255.0F;
        float g2 = ((type.accentColor >> 8) & 255) / 255.0F;
        float b2 = (type.accentColor & 255) / 255.0F;

        float muzzleZ = -0.70F;
        float railY = 0.24F;
        float underY = 0.06F;
        float magDrop = reloadFrac > 0.15F && reloadFrac < 0.75F ? -0.18F * (float)Math.sin((double)((reloadFrac - 0.15F) / 0.60F) * Math.PI) : 0.0F;
        GunAttachment.MagType magType = GunAttachment.getMag(stack);
        boolean extendedMag = magType == GunAttachment.MagType.EXTENDED;

        switch (type)
        {
            case PISTOL_9MM:
                // Slide + frame
                addBox(buf, -0.055F, 0.09F, -0.46F, 0.055F, 0.21F, 0.12F, r1, g1, b1);
                addBox(buf, -0.048F, 0.04F, -0.42F, 0.048F, 0.09F, 0.10F, r2, g2, b2);
                // Grip + magazine
                addBox(buf, -0.045F, -0.22F, -0.04F, 0.045F, 0.06F, 0.11F, r2, g2, b2);
                addBox(buf, -0.036F, (extendedMag ? -0.34F : -0.24F) + magDrop, -0.02F, 0.036F, -0.14F + magDrop, 0.09F, 0.16F, 0.18F, 0.20F);
                // Iron sights
                addBox(buf, -0.03F, 0.21F, 0.07F, -0.015F, 0.24F, 0.10F, 0.85F, 0.85F, 0.85F);
                addBox(buf, 0.015F, 0.21F, 0.07F, 0.03F, 0.24F, 0.10F, 0.85F, 0.85F, 0.85F);
                addBox(buf, -0.01F, 0.21F, -0.44F, 0.01F, 0.245F, -0.41F, 0.2F, 1.0F, 0.4F);
                muzzleZ = -0.48F;
                railY = 0.21F;
                underY = 0.04F;
                break;

            case ASSAULT_RIFLE:
                // Stock
                addBox(buf, -0.05F, 0.02F, 0.14F, 0.05F, 0.18F, 0.56F, r2, g2, b2);
                // Receiver
                addBox(buf, -0.065F, 0.04F, -0.36F, 0.065F, 0.22F, 0.16F, r1, g1, b1);
                // Handguard + rails
                addBox(buf, -0.058F, 0.06F, -0.82F, 0.058F, 0.20F, -0.36F, r2, g2, b2);
                // Barrel
                addBox(buf, -0.028F, 0.105F, -1.06F, 0.028F, 0.16F, -0.82F, 0.18F, 0.20F, 0.23F);
                // Pistol grip
                addBox(buf, -0.042F, -0.18F, -0.04F, 0.042F, 0.05F, 0.09F, 0.16F, 0.17F, 0.19F);
                // Magazine
                addBox(buf, -0.042F, (extendedMag ? -0.36F : -0.22F) + magDrop, -0.34F, 0.042F, 0.05F + magDrop, -0.18F, 0.19F, 0.21F, 0.24F);
                // Front sight post
                addBox(buf, -0.012F, 0.20F, -0.80F, 0.012F, 0.27F, -0.76F, 0.16F, 0.18F, 0.20F);
                muzzleZ = -1.06F;
                railY = 0.22F;
                underY = 0.06F;
                break;

            case SHOTGUN_12G:
                // Wood stock
                addBox(buf, -0.055F, -0.02F, 0.12F, 0.055F, 0.17F, 0.58F, r2, g2, b2);
                // Receiver
                addBox(buf, -0.068F, 0.04F, -0.34F, 0.068F, 0.21F, 0.14F, r1, g1, b1);
                // Heavy barrel + under-barrel shell tube
                addBox(buf, -0.036F, 0.12F, -1.08F, 0.036F, 0.195F, -0.34F, 0.22F, 0.24F, 0.27F);
                addBox(buf, -0.032F, 0.05F, extendedMag ? -1.02F : -0.88F, 0.032F, 0.115F, -0.34F, 0.26F, 0.28F, 0.31F);
                // Pump forend
                addBox(buf, -0.058F, 0.035F, -0.76F + magDrop * 0.6F, 0.058F, 0.135F, -0.46F + magDrop * 0.6F, r2, g2, b2);
                muzzleZ = -1.08F;
                railY = 0.21F;
                underY = 0.035F;
                break;

            case SNIPER_50:
                // Heavy tactical stock + cheek rest
                addBox(buf, -0.055F, 0.01F, 0.18F, 0.055F, 0.19F, 0.68F, r2, g2, b2);
                addBox(buf, -0.045F, 0.19F, 0.28F, 0.045F, 0.24F, 0.52F, 0.18F, 0.20F, 0.22F);
                // Heavy .50 cal receiver
                addBox(buf, -0.072F, 0.04F, -0.44F, 0.072F, 0.23F, 0.18F, r1, g1, b1);
                // Fluted long barrel + muzzle brake
                addBox(buf, -0.032F, 0.11F, -1.42F, 0.032F, 0.175F, -0.44F, 0.17F, 0.19F, 0.22F);
                addBox(buf, -0.058F, 0.095F, -1.52F, 0.058F, 0.19F, -1.42F, 0.26F, 0.28F, 0.30F);
                // Box magazine + grip + bipod legs
                addBox(buf, -0.048F, (extendedMag ? -0.26F : -0.15F) + magDrop, -0.36F, 0.048F, 0.04F + magDrop, -0.12F, 0.18F, 0.20F, 0.22F);
                addBox(buf, -0.042F, -0.18F, 0.02F, 0.042F, 0.04F, 0.14F, 0.16F, 0.18F, 0.20F);
                // Built-in optics rail & default scope if none attached
                if (GunAttachment.getScope(stack) == GunAttachment.ScopeType.NONE)
                {
                    addBox(buf, -0.042F, 0.27F, -0.46F, 0.042F, 0.355F, 0.08F, 0.15F, 0.17F, 0.19F);
                    addBox(buf, -0.054F, 0.258F, -0.58F, 0.054F, 0.368F, -0.46F, 0.12F, 0.14F, 0.16F);
                    addBox(buf, -0.032F, 0.282F, -0.585F, 0.032F, 0.344F, -0.578F, 0.18F, 0.58F, 0.88F);
                }
                muzzleZ = -1.52F;
                railY = 0.23F;
                underY = 0.04F;
                break;

            case MINIGUN:
                // Heavy motor housing + top carry handle + ammo drum
                addBox(buf, -0.13F, -0.04F, -0.34F, 0.13F, 0.24F, 0.24F, r1, g1, b1);
                addBox(buf, -0.03F, 0.24F, -0.28F, 0.03F, 0.34F, 0.16F, r2, g2, b2);
                addBox(buf, -0.14F, -0.26F + magDrop, -0.18F, 0.14F, -0.02F + magDrop, 0.14F, 0.28F, 0.34F, 0.24F);
                // Barrel clamp rings
                addBox(buf, -0.115F, -0.015F, -0.68F, 0.115F, 0.215F, -0.62F, r2, g2, b2);
                addBox(buf, -0.115F, -0.015F, -1.16F, 0.115F, 0.215F, -1.10F, r2, g2, b2);
                // 6 rotating barrels around center (0, 0.10)
                for (int b = 0; b < 6; ++b)
                {
                    double ang = (double)spinAngle * (Math.PI / 180.0D) + (double)b * (Math.PI / 3.0D);
                    float bx = (float)Math.cos(ang) * 0.075F;
                    float by = 0.10F + (float)Math.sin(ang) * 0.075F;
                    addBox(buf, bx - 0.02F, by - 0.02F, -1.24F, bx + 0.02F, by + 0.02F, -0.34F, 0.34F, 0.37F, 0.41F);
                }
                muzzleZ = -1.25F;
                railY = 0.34F;
                underY = -0.04F;
                break;

            case ROCKET_LAUNCHER:
                // Olive-drab main launch tube
                addBox(buf, -0.11F, 0.00F, -1.06F, 0.11F, 0.22F, 0.62F, r1, g1, b1);
                // Flared front & rear venturi cones
                addBox(buf, -0.135F, -0.025F, -1.16F, 0.135F, 0.245F, -1.02F, r2, g2, b2);
                addBox(buf, -0.135F, -0.025F, 0.56F, 0.135F, 0.245F, 0.72F, r2, g2, b2);
                // Warning stripe + grip + side iron sight
                addBox(buf, -0.114F, 0.02F, -0.84F, 0.114F, 0.20F, -0.74F, 0.90F, 0.74F, 0.14F);
                addBox(buf, -0.042F, -0.20F, -0.18F, 0.042F, 0.02F, -0.04F, 0.18F, 0.18F, 0.18F);
                // Loaded warhead tip visible if ammo > 0
                if (((ItemArgonGun)stack.getItem()).getAmmoInMag(stack) > 0)
                {
                    addBox(buf, -0.075F, 0.035F, -1.28F, 0.075F, 0.185F, -1.12F, 0.72F, 0.22F, 0.14F);
                }
                muzzleZ = -1.28F;
                railY = 0.22F;
                underY = 0.00F;
                break;

            case PLASMA_RIFLE:
                // Sci-fi white/titanium shell + stock
                addBox(buf, -0.065F, 0.02F, 0.12F, 0.065F, 0.20F, 0.52F, r1, g1, b1);
                addBox(buf, -0.078F, 0.03F, -0.62F, 0.078F, 0.24F, 0.14F, r1, g1, b1);
                // Twin magnetic accelerator rails
                addBox(buf, -0.068F, 0.165F, -1.06F, 0.068F, 0.225F, -0.60F, 0.24F, 0.28F, 0.34F);
                addBox(buf, -0.068F, 0.055F, -1.06F, 0.068F, 0.115F, -0.60F, 0.24F, 0.28F, 0.34F);
                // Glowing cyan plasma core & coil ribs
                addBox(buf, -0.042F, 0.118F, -1.02F, 0.042F, 0.162F, -0.58F, r2, g2, b2);
                addBox(buf, -0.084F, 0.10F, -0.46F, 0.084F, 0.19F, -0.18F, r2, g2, b2);
                // Energy cell magazine + grip
                addBox(buf, -0.044F, -0.18F, -0.04F, 0.044F, 0.04F, 0.10F, 0.20F, 0.23F, 0.28F);
                addBox(buf, -0.048F, (extendedMag ? -0.26F : -0.16F) + magDrop, -0.40F, 0.048F, 0.04F + magDrop, -0.20F, 0.10F, 0.78F, 0.96F);
                muzzleZ = -1.08F;
                railY = 0.24F;
                underY = 0.03F;
                break;
        }

        // Dynamic 3D Attachments rendered directly on weapon rails
        if (type.supportsAttachments())
        {
            GunAttachment.ScopeType scope = GunAttachment.getScope(stack);

            if (scope == GunAttachment.ScopeType.REFLEX)
            {
                addBox(buf, -0.048F, railY, -0.26F, 0.048F, railY + 0.03F, -0.04F, 0.14F, 0.16F, 0.18F);
                addBox(buf, -0.048F, railY + 0.03F, -0.24F, -0.036F, railY + 0.11F, -0.08F, 0.16F, 0.18F, 0.20F);
                addBox(buf, 0.036F, railY + 0.03F, -0.24F, 0.048F, railY + 0.11F, -0.08F, 0.16F, 0.18F, 0.20F);
                addBox(buf, -0.048F, railY + 0.10F, -0.24F, 0.048F, railY + 0.115F, -0.08F, 0.16F, 0.18F, 0.20F);
                // Red holographic center emitter dot
                addBox(buf, -0.008F, railY + 0.062F, -0.16F, 0.008F, railY + 0.078F, -0.14F, 1.0F, 0.15F, 0.15F);
            }
            else if (scope == GunAttachment.ScopeType.SCOPE_4X)
            {
                addBox(buf, -0.032F, railY, -0.26F, 0.032F, railY + 0.035F, -0.02F, 0.15F, 0.17F, 0.19F);
                addBox(buf, -0.042F, railY + 0.035F, -0.34F, 0.042F, railY + 0.115F, 0.04F, 0.18F, 0.20F, 0.23F);
                addBox(buf, -0.030F, railY + 0.047F, -0.345F, 0.030F, railY + 0.103F, -0.338F, 0.20F, 0.72F, 0.92F);
            }
            else if (scope == GunAttachment.ScopeType.SCOPE_8X)
            {
                addBox(buf, -0.028F, railY, -0.32F, 0.028F, railY + 0.04F, -0.24F, 0.14F, 0.16F, 0.18F);
                addBox(buf, -0.028F, railY, -0.08F, 0.028F, railY + 0.04F, 0.00F, 0.14F, 0.16F, 0.18F);
                addBox(buf, -0.038F, railY + 0.04F, -0.44F, 0.038F, railY + 0.12F, 0.10F, 0.14F, 0.16F, 0.18F);
                addBox(buf, -0.052F, railY + 0.028F, -0.58F, 0.052F, railY + 0.132F, -0.44F, 0.11F, 0.13F, 0.15F);
                addBox(buf, -0.036F, railY + 0.045F, -0.586F, 0.036F, railY + 0.115F, -0.578F, 0.18F, 0.65F, 0.95F);
            }

            GunAttachment.MuzzleType muzzle = GunAttachment.getMuzzle(stack);

            if (muzzle == GunAttachment.MuzzleType.SILENCER)
            {
                addBox(buf, -0.042F, 0.085F, muzzleZ - 0.34F, 0.042F, 0.175F, muzzleZ, 0.12F, 0.13F, 0.15F);
                muzzleZ -= 0.34F;
            }
            else if (muzzle == GunAttachment.MuzzleType.COMPENSATOR)
            {
                addBox(buf, -0.046F, 0.090F, muzzleZ - 0.14F, 0.046F, 0.170F, muzzleZ, 0.32F, 0.35F, 0.38F);
                muzzleZ -= 0.14F;
            }

            GunAttachment.GripType grip = GunAttachment.getGrip(stack);

            if (grip == GunAttachment.GripType.FOREGRIP)
            {
                addBox(buf, -0.034F, underY - 0.18F, -0.62F, 0.034F, underY + 0.01F, -0.52F, 0.15F, 0.17F, 0.19F);
            }
            else if (grip == GunAttachment.GripType.LASER)
            {
                addBox(buf, 0.055F, underY + 0.02F, -0.68F, 0.105F, underY + 0.075F, -0.48F, 0.16F, 0.18F, 0.20F);
                // Laser beam segment extending forward
                addBox(buf, 0.076F, underY + 0.043F, -3.20F, 0.084F, underY + 0.051F, -0.68F, 1.0F, 0.12F, 0.12F);
            }
        }

        // Animated muzzle flash star when firing
        if (muzzleFlash)
        {
            float fr = type == GunType.PLASMA_RIFLE ? 0.25F : 1.0F;
            float fg = type == GunType.PLASMA_RIFLE ? 0.92F : 0.84F;
            float fb = type == GunType.PLASMA_RIFLE ? 1.0F : 0.25F;
            addBox(buf, -0.14F, 0.095F, muzzleZ - 0.20F, 0.14F, 0.165F, muzzleZ, fr, fg, fb);
            addBox(buf, -0.035F, 0.00F, muzzleZ - 0.20F, 0.035F, 0.26F, muzzleZ, fr, fg, fb);
        }

        tess.draw();

        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Appends an axis-aligned 3D shaded box into the current POSITION_COLOR quad batch.
     */
    public static void addBox(
        BufferBuilder buf,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float r, float g, float b)
    {
        float topR = Math.min(1.0F, r * 1.12F);
        float topG = Math.min(1.0F, g * 1.12F);
        float topB = Math.min(1.0F, b * 1.12F);

        float botR = r * 0.68F;
        float botG = g * 0.68F;
        float botB = b * 0.68F;

        float sideR = r * 0.86F;
        float sideG = g * 0.86F;
        float sideB = b * 0.86F;

        float frontR = r * 0.94F;
        float frontG = g * 0.94F;
        float frontB = b * 0.94F;

        // Top (+Y)
        buf.pos(x1, y2, z1).color(topR, topG, topB, 1.0F).endVertex();
        buf.pos(x1, y2, z2).color(topR, topG, topB, 1.0F).endVertex();
        buf.pos(x2, y2, z2).color(topR, topG, topB, 1.0F).endVertex();
        buf.pos(x2, y2, z1).color(topR, topG, topB, 1.0F).endVertex();

        // Bottom (-Y)
        buf.pos(x1, y1, z2).color(botR, botG, botB, 1.0F).endVertex();
        buf.pos(x1, y1, z1).color(botR, botG, botB, 1.0F).endVertex();
        buf.pos(x2, y1, z1).color(botR, botG, botB, 1.0F).endVertex();
        buf.pos(x2, y1, z2).color(botR, botG, botB, 1.0F).endVertex();

        // North / Forward (-Z)
        buf.pos(x2, y1, z1).color(frontR, frontG, frontB, 1.0F).endVertex();
        buf.pos(x1, y1, z1).color(frontR, frontG, frontB, 1.0F).endVertex();
        buf.pos(x1, y2, z1).color(frontR, frontG, frontB, 1.0F).endVertex();
        buf.pos(x2, y2, z1).color(frontR, frontG, frontB, 1.0F).endVertex();

        // South / Rear (+Z)
        buf.pos(x1, y1, z2).color(frontR, frontG, frontB, 1.0F).endVertex();
        buf.pos(x2, y1, z2).color(frontR, frontG, frontB, 1.0F).endVertex();
        buf.pos(x2, y2, z2).color(frontR, frontG, frontB, 1.0F).endVertex();
        buf.pos(x1, y2, z2).color(frontR, frontG, frontB, 1.0F).endVertex();

        // West (-X)
        buf.pos(x1, y1, z1).color(sideR, sideG, sideB, 1.0F).endVertex();
        buf.pos(x1, y1, z2).color(sideR, sideG, sideB, 1.0F).endVertex();
        buf.pos(x1, y2, z2).color(sideR, sideG, sideB, 1.0F).endVertex();
        buf.pos(x1, y2, z1).color(sideR, sideG, sideB, 1.0F).endVertex();

        // East (+X)
        buf.pos(x2, y1, z2).color(sideR, sideG, sideB, 1.0F).endVertex();
        buf.pos(x2, y1, z1).color(sideR, sideG, sideB, 1.0F).endVertex();
        buf.pos(x2, y2, z1).color(sideR, sideG, sideB, 1.0F).endVertex();
        buf.pos(x2, y2, z2).color(sideR, sideG, sideB, 1.0F).endVertex();
    }
}
