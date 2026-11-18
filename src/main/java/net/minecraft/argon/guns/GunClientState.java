package net.minecraft.argon.guns;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Client-side controller for ADS interpolation, recoil kick/pitch recovery,
 * Minigun barrel spin-up, trigger input, reload key handling, FOV zoom,
 * scope reticle overlay, and tactical ammo HUD.
 */
public final class GunClientState
{
    private static float adsProgress = 0.0F;
    private static float prevAdsProgress = 0.0F;

    private static float recoilKick = 0.0F;
    private static float prevRecoilKick = 0.0F;
    private static float recoilPitch = 0.0F;
    private static float prevRecoilPitch = 0.0F;

    private static int clientFireCooldown = 0;
    private static int muzzleFlashTicks = 0;
    private static int minigunSpinTicks = 0;
    private static float minigunSpinAngle = 0.0F;
    private static float prevMinigunSpinAngle = 0.0F;
    private static boolean semiAutoTriggerConsumed = false;

    private GunClientState()
    {
    }

    public static boolean isHoldingGun(EntityPlayer player)
    {
        if (player == null)
        {
            return false;
        }

        ItemStack held = player.getHeldItemMainhand();
        return !held.isEmpty() && held.getItem() instanceof ItemArgonGun;
    }

    public static void onClientTick(Minecraft mc)
    {
        prevAdsProgress = adsProgress;
        prevRecoilKick = recoilKick;
        prevRecoilPitch = recoilPitch;
        prevMinigunSpinAngle = minigunSpinAngle;

        if (clientFireCooldown > 0)
        {
            clientFireCooldown--;
        }

        if (muzzleFlashTicks > 0)
        {
            muzzleFlashTicks--;
        }

        recoilKick *= 0.58F;
        recoilPitch *= 0.62F;

        if (recoilKick < 0.01F) recoilKick = 0.0F;
        if (recoilPitch < 0.01F) recoilPitch = 0.0F;

        if (mc == null || mc.player == null || mc.world == null || mc.currentScreen != null || !mc.inGameHasFocus)
        {
            adsProgress = Math.max(0.0F, adsProgress - 0.25F);
            minigunSpinTicks = 0;
            semiAutoTriggerConsumed = false;
            return;
        }

        ItemStack held = mc.player.getHeldItemMainhand();

        if (held.isEmpty() || !(held.getItem() instanceof ItemArgonGun))
        {
            adsProgress = Math.max(0.0F, adsProgress - 0.30F);
            minigunSpinTicks = 0;
            semiAutoTriggerConsumed = false;
            return;
        }

        ItemArgonGun gun = (ItemArgonGun)held.getItem();
        GunType type = gun.getGunType();
        boolean reloading = gun.isReloading(held);

        // Reload key [R]
        if (mc.gameSettings.ofKeyBindReload != null)
        {
            while (mc.gameSettings.ofKeyBindReload.isPressed())
            {
                if (!reloading)
                {
                    GunNetworkHandler.sendReloadPacket();
                }
            }
        }

        // Right-click ADS smooth interpolation
        boolean aiming = mc.gameSettings.keyBindUseItem.isKeyDown() && !reloading && mc.gameSettings.thirdPersonView == 0;

        if (aiming)
        {
            adsProgress = Math.min(1.0F, adsProgress + 0.22F);
        }
        else
        {
            adsProgress = Math.max(0.0F, adsProgress - 0.24F);
        }

        // Left-click trigger handling (full-auto, semi-auto, and Minigun spin-up)
        boolean attackDown = mc.gameSettings.keyBindAttack.isKeyDown();

        if (!attackDown)
        {
            semiAutoTriggerConsumed = false;
            if (minigunSpinTicks > 0)
            {
                minigunSpinTicks = Math.max(0, minigunSpinTicks - 1);
            }
        }
        else if (!reloading)
        {
            if (type.spinUpTicks > 0)
            {
                minigunSpinTicks = Math.min(type.spinUpTicks + 4, minigunSpinTicks + 1);
                minigunSpinAngle = (minigunSpinAngle + 28.0F) % 360.0F;
            }

            boolean canSpinFire = type.spinUpTicks <= 0 || minigunSpinTicks >= type.spinUpTicks;
            boolean triggerAllowed = type.fullAuto || !semiAutoTriggerConsumed;

            if (canSpinFire && triggerAllowed && clientFireCooldown <= 0)
            {
                tryFireClient(mc, gun, held);
                if (!type.fullAuto)
                {
                    semiAutoTriggerConsumed = true;
                }
            }
        }
    }

    public static boolean handleLeftClickPress(Minecraft mc)
    {
        if (mc == null || mc.player == null || !isHoldingGun(mc.player))
        {
            return false;
        }

        ItemStack held = mc.player.getHeldItemMainhand();
        ItemArgonGun gun = (ItemArgonGun)held.getItem();

        if (!gun.isReloading(held) && clientFireCooldown <= 0 && gun.getGunType().spinUpTicks <= 0)
        {
            tryFireClient(mc, gun, held);
            semiAutoTriggerConsumed = true;
        }

        return true;
    }

    private static void tryFireClient(Minecraft mc, ItemArgonGun gun, ItemStack held)
    {
        GunType type = gun.getGunType();
        int ammo = gun.getAmmoInMag(held);
        boolean isAds = adsProgress > 0.55F;

        GunNetworkHandler.sendFirePacket(isAds);
        clientFireCooldown = type.fireIntervalTicks;

        if (ammo > 0)
        {
            float effRecoil = GunAttachment.getEffectiveRecoil(type, held, isAds);
            recoilKick = Math.min(1.8F, recoilKick + 0.65F + effRecoil * 0.08F);
            recoilPitch = Math.min(14.0F, recoilPitch + effRecoil * 0.75F);
            muzzleFlashTicks = 2;

            // Apply tactile upward camera kick to player pitch
            mc.player.rotationPitch = MathHelper.clamp(mc.player.rotationPitch - effRecoil * 0.35F, -90.0F, 90.0F);
        }
    }

    public static float getAdsProgress(float partialTicks)
    {
        return prevAdsProgress + (adsProgress - prevAdsProgress) * partialTicks;
    }

    public static float getRecoilKick(float partialTicks)
    {
        return prevRecoilKick + (recoilKick - prevRecoilKick) * partialTicks;
    }

    public static float getRecoilPitch(float partialTicks)
    {
        return prevRecoilPitch + (recoilPitch - prevRecoilPitch) * partialTicks;
    }

    public static int getMuzzleFlashTicks()
    {
        return muzzleFlashTicks;
    }

    public static float getMinigunSpinAngle(float partialTicks)
    {
        float delta = minigunSpinAngle - prevMinigunSpinAngle;
        if (delta < -180.0F) delta += 360.0F;
        return prevMinigunSpinAngle + delta * partialTicks;
    }

    public static float getReloadAnimFraction(ItemStack stack, float partialTicks)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArgonGun))
        {
            return 0.0F;
        }

        ItemArgonGun gun = (ItemArgonGun)stack.getItem();
        int rem = gun.getReloadTicks(stack);
        int max = gun.getReloadMax(stack);

        if (rem <= 0 || max <= 0)
        {
            return 0.0F;
        }

        float elapsed = (float)(max - rem) + partialTicks;
        return MathHelper.clamp(elapsed / (float)max, 0.0F, 1.0F);
    }

    /**
     * Composes cleanly with OptiFine Config.zoomMode inside EntityRenderer.getFOVModifier.
     */
    public static float getAdsFovMultiplier(Minecraft mc, float partialTicks)
    {
        if (mc == null || mc.player == null || mc.gameSettings.thirdPersonView != 0)
        {
            return 1.0F;
        }

        ItemStack held = mc.player.getHeldItemMainhand();

        if (held.isEmpty() || !(held.getItem() instanceof ItemArgonGun))
        {
            return 1.0F;
        }

        float ads = getAdsProgress(partialTicks);

        if (ads <= 0.005F)
        {
            return 1.0F;
        }

        ItemArgonGun gun = (ItemArgonGun)held.getItem();
        float targetMult = GunAttachment.getEffectiveAdsFovMultiplier(gun.getGunType(), held);
        return 1.0F + (targetMult - 1.0F) * ads;
    }

    /**
     * Renders the ADS tactical scope overlay (when scoped) and the bottom-right weapon/ammo HUD.
     */
    public static void renderGunHud(Minecraft mc, ScaledResolution sr, FontRenderer font, float partialTicks)
    {
        if (mc == null || mc.player == null || mc.player.isSpectator())
        {
            return;
        }

        ItemStack held = mc.player.getHeldItemMainhand();

        if (held.isEmpty() || !(held.getItem() instanceof ItemArgonGun))
        {
            return;
        }

        ItemArgonGun gun = (ItemArgonGun)held.getItem();
        GunType type = gun.getGunType();
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();
        float ads = getAdsProgress(partialTicks);

        // 1. Tactical Sniper / 4x Scope Vignette & Mil-Dot Crosshair
        if (ads > 0.85F && mc.gameSettings.thirdPersonView == 0 && GunAttachment.usesFullScopeOverlay(type, held))
        {
            renderScopeOverlay(width, height, font, type, held);
        }
        else if (ads > 0.65F && mc.gameSettings.thirdPersonView == 0 && GunAttachment.getScope(held) == GunAttachment.ScopeType.REFLEX)
        {
            int cx = width / 2;
            int cy = height / 2;
            Gui.drawRect(cx - 1, cy - 1, cx + 2, cy + 2, 0xEEFF2A2A);
        }

        // 2. Compact Bottom-Right Tactical Ammo & Status Panel
        int panelW = 148;
        int panelH = 44;
        int px = width - panelW - 10;
        int py = height - panelH - 10;

        Gui.drawRect(px, py, px + panelW, py + panelH, 0xB811161C);
        Gui.drawRect(px, py, px + panelW, py + 1, 0xCC3FA9F5);
        Gui.drawRect(px, py + panelH - 1, px + panelW, py + panelH, 0x663FA9F5);

        int ammo = gun.getAmmoInMag(held);
        int maxMag = gun.getMaxMagazine(held);
        int reserve = gun.countReserveAmmo(mc.player);
        boolean reloading = gun.isReloading(held);

        font.drawStringWithShadow(type.displayName, (float)(px + 6), (float)(py + 5), 0xE6F2FF);

        if (reloading)
        {
            float frac = getReloadAnimFraction(held, partialTicks);
            font.drawStringWithShadow("RELOADING...", (float)(px + 6), (float)(py + 18), 0xFFCC44);
            int barX = px + 6;
            int barY = py + 31;
            int barW = panelW - 12;
            Gui.drawRect(barX, barY, barX + barW, barY + 5, 0xAA2A323D);
            Gui.drawRect(barX, barY, barX + (int)((float)barW * frac), barY + 5, 0xFF42C5F5);
        }
        else
        {
            int ammoColor = ammo == 0 ? 0xFF4A4A : (ammo <= Math.max(1, maxMag / 4) ? 0xFFB84D : 0xFFFFFF);
            String ammoStr = ammo + " / " + maxMag;
            String resStr = mc.player.capabilities.isCreativeMode ? "INF" : String.valueOf(reserve);
            font.drawStringWithShadow(ammoStr, (float)(px + 6), (float)(py + 18), ammoColor);
            font.drawStringWithShadow("RES: " + resStr, (float)(px + 72), (float)(py + 18), 0x99B4C8);

            String mode = type.fullAuto ? "AUTO" : "SEMI";
            GunAttachment.ScopeType scope = GunAttachment.getScope(held);
            String sub = mode + " | " + (scope != GunAttachment.ScopeType.NONE ? scope.shortLabel : type.caliberLabel);
            if (font.getStringWidth(sub) > panelW - 12)
            {
                sub = mode + " | " + type.caliberLabel;
            }
            font.drawStringWithShadow(sub, (float)(px + 6), (float)(py + 31), 0x7A90A6);
        }
    }

    private static void renderScopeOverlay(int width, int height, FontRenderer font, GunType type, ItemStack stack)
    {
        int cx = width / 2;
        int cy = height / 2;
        int radius = Math.min(width, height) * 42 / 100;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // Outer blackout surround using a 48-segment ring to a far outer radius
        int segments = 48;
        float outerR = (float)(Math.max(width, height));
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; ++i)
        {
            double a0 = (double)i * (Math.PI * 2.0D) / (double)segments;
            double a1 = (double)(i + 1) * (Math.PI * 2.0D) / (double)segments;
            float cos0 = (float)Math.cos(a0);
            float sin0 = (float)Math.sin(a0);
            float cos1 = (float)Math.cos(a1);
            float sin1 = (float)Math.sin(a1);

            buf.pos(cx + cos0 * radius, cy + sin0 * radius, 0.0D).color(0.02F, 0.03F, 0.04F, 0.98F).endVertex();
            buf.pos(cx + cos0 * outerR, cy + sin0 * outerR, 0.0D).color(0.02F, 0.03F, 0.04F, 0.98F).endVertex();
            buf.pos(cx + cos1 * outerR, cy + sin1 * outerR, 0.0D).color(0.02F, 0.03F, 0.04F, 0.98F).endVertex();
            buf.pos(cx + cos1 * radius, cy + sin1 * radius, 0.0D).color(0.02F, 0.03F, 0.04F, 0.98F).endVertex();
        }

        tess.draw();
        GlStateManager.enableTexture2D();

        // Crisp mil-dot crosshairs
        Gui.drawRect(cx - radius, cy, cx + radius, cy + 1, 0xCC0D1116);
        Gui.drawRect(cx, cy - radius, cx + 1, cy + radius, 0xCC0D1116);

        // Illuminated center dot & rangefinder ticks
        Gui.drawRect(cx - 1, cy - 1, cx + 2, cy + 2, 0xEEFF3333);

        for (int step = 1; step <= 4; ++step)
        {
            int d = step * 16;
            if (d < radius - 8)
            {
                Gui.drawRect(cx - d, cy - 3, cx - d + 1, cy + 4, 0xAA0D1116);
                Gui.drawRect(cx + d, cy - 3, cx + d + 1, cy + 4, 0xAA0D1116);
                Gui.drawRect(cx - 4, cy + d, cx + 5, cy + d + 1, 0xAA0D1116);
            }
        }

        GunAttachment.ScopeType scope = GunAttachment.getScope(stack);
        String zoomLabel = scope == GunAttachment.ScopeType.SCOPE_8X ? "8.0x OPTICS" : (scope == GunAttachment.ScopeType.SCOPE_4X ? "4.0x ACOG" : "6.0x SNIPER");
        font.drawStringWithShadow(zoomLabel, (float)(cx - radius + 14), (float)(cy + radius - 14), 0xAA66E0FF);
    }
}
