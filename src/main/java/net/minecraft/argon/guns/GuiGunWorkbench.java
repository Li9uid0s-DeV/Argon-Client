package net.minecraft.argon.guns;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Vanilla-styled Minecraft 1.12 GuiScreen for the Argon Gun Attachment Workbench.
 * Uses standard GuiButton controls and renders a live rotating 3D preview of the held gun
 * via GlStateManager + Tessellator (Rule 1 & Rule 4 compliant).
 */
public class GuiGunWorkbench extends GuiScreen
{
    private final BlockPos workbenchPos;
    private float previewYaw = 35.0F;

    private GuiButton btnScope;
    private GuiButton btnMuzzle;
    private GuiButton btnMag;
    private GuiButton btnGrip;
    private GuiButton btnReset;
    private GuiButton btnDone;

    public GuiGunWorkbench(BlockPos workbenchPos)
    {
        this.workbenchPos = workbenchPos;
    }

    @Override
    public void initGui()
    {
        this.buttonList.clear();

        int leftX = this.width / 2 - 154;
        int rightX = this.width / 2 + 6;
        int startY = this.height / 2 - 42;

        this.btnScope = this.addButton(new GuiButton(10, rightX, startY, 152, 20, "Scope: None"));
        this.btnMuzzle = this.addButton(new GuiButton(11, rightX, startY + 26, 152, 20, "Muzzle: None"));
        this.btnMag = this.addButton(new GuiButton(12, rightX, startY + 52, 152, 20, "Mag: Standard"));
        this.btnGrip = this.addButton(new GuiButton(13, rightX, startY + 78, 152, 20, "Underbarrel: None"));

        this.btnReset = this.addButton(new GuiButton(20, leftX, this.height / 2 + 72, 140, 20, "Strip All Attachments"));
        this.btnDone = this.addButton(new GuiButton(21, rightX, this.height / 2 + 72, 152, 20, "Done"));

        this.refreshButtons();
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        this.previewYaw = (this.previewYaw + 1.4F) % 360.0F;
        this.refreshButtons();
    }

    private ItemStack getHeldGun()
    {
        if (this.mc == null || this.mc.player == null)
        {
            return ItemStack.EMPTY;
        }

        ItemStack held = this.mc.player.getHeldItemMainhand();
        return !held.isEmpty() && held.getItem() instanceof ItemArgonGun ? held : ItemStack.EMPTY;
    }

    private boolean playerCanEquip(int slotCategory, int candidateIndex)
    {
        if (candidateIndex == 0 || this.mc == null || this.mc.player == null)
        {
            return true;
        }

        if (this.mc.player.capabilities.isCreativeMode)
        {
            return true;
        }

        Item needed = GunAttachment.getItemForAttachment(slotCategory, candidateIndex);

        if (needed == null)
        {
            return true;
        }

        for (int i = 0; i < this.mc.player.inventory.getSizeInventory(); ++i)
        {
            ItemStack inv = this.mc.player.inventory.getStackInSlot(i);

            if (!inv.isEmpty() && inv.getItem() == needed)
            {
                return true;
            }
        }

        return false;
    }

    private int findNextAvailableIndex(ItemStack held, int slotCategory, int totalCount)
    {
        int current = GunAttachment.getAttachmentIndex(held, slotCategory);

        for (int step = 1; step <= totalCount; ++step)
        {
            int next = (current + step) % totalCount;

            if (next == current || this.playerCanEquip(slotCategory, next))
            {
                return next;
            }
        }

        return current;
    }

    private void refreshButtons()
    {
        ItemStack held = this.getHeldGun();
        boolean canMod = !held.isEmpty() && ((ItemArgonGun)held.getItem()).getGunType().supportsAttachments();

        this.btnScope.enabled = canMod;
        this.btnMuzzle.enabled = canMod;
        this.btnMag.enabled = canMod;
        this.btnGrip.enabled = canMod;
        this.btnReset.enabled = canMod;

        if (!held.isEmpty())
        {
            this.btnScope.displayString = "Scope: " + GunAttachment.getScope(held).displayName;
            this.btnMuzzle.displayString = "Muzzle: " + GunAttachment.getMuzzle(held).displayName;
            this.btnMag.displayString = "Mag: " + GunAttachment.getMag(held).displayName;
            this.btnGrip.displayString = "Grip: " + GunAttachment.getGrip(held).displayName;
        }
        else
        {
            this.btnScope.displayString = "Scope: -";
            this.btnMuzzle.displayString = "Muzzle: -";
            this.btnMag.displayString = "Mag: -";
            this.btnGrip.displayString = "Grip: -";
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 21)
        {
            this.mc.displayGuiScreen((GuiScreen)null);
            return;
        }

        ItemStack held = this.getHeldGun();

        if (held.isEmpty() || !((ItemArgonGun)held.getItem()).getGunType().supportsAttachments())
        {
            return;
        }

        if (button.id == 10)
        {
            int next = this.findNextAvailableIndex(held, GunAttachment.SLOT_SCOPE, GunAttachment.ScopeType.values().length);
            GunNetworkHandler.sendModifyAttachmentPacket(this.workbenchPos, GunAttachment.SLOT_SCOPE, next);
            GunAttachment.setAttachmentIndex(held, GunAttachment.SLOT_SCOPE, next);
        }
        else if (button.id == 11)
        {
            int next = this.findNextAvailableIndex(held, GunAttachment.SLOT_MUZZLE, GunAttachment.MuzzleType.values().length);
            GunNetworkHandler.sendModifyAttachmentPacket(this.workbenchPos, GunAttachment.SLOT_MUZZLE, next);
            GunAttachment.setAttachmentIndex(held, GunAttachment.SLOT_MUZZLE, next);
        }
        else if (button.id == 12)
        {
            int next = this.findNextAvailableIndex(held, GunAttachment.SLOT_MAGAZINE, GunAttachment.MagType.values().length);
            GunNetworkHandler.sendModifyAttachmentPacket(this.workbenchPos, GunAttachment.SLOT_MAGAZINE, next);
            GunAttachment.setAttachmentIndex(held, GunAttachment.SLOT_MAGAZINE, next);
        }
        else if (button.id == 13)
        {
            int next = this.findNextAvailableIndex(held, GunAttachment.SLOT_UNDERBARREL, GunAttachment.GripType.values().length);
            GunNetworkHandler.sendModifyAttachmentPacket(this.workbenchPos, GunAttachment.SLOT_UNDERBARREL, next);
            GunAttachment.setAttachmentIndex(held, GunAttachment.SLOT_UNDERBARREL, next);
        }
        else if (button.id == 20)
        {
            for (int slot = 0; slot <= 3; ++slot)
            {
                GunNetworkHandler.sendModifyAttachmentPacket(this.workbenchPos, slot, 0);
                GunAttachment.setAttachmentIndex(held, slot, 0);
            }
        }

        this.refreshButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Vanilla-toned workbench panel frame
        drawRect(cx - 166, cy - 102, cx + 166, cy + 102, 0xCC181C22);
        drawRect(cx - 166, cy - 102, cx + 166, cy - 100, 0xFF42A8E8);
        drawRect(cx - 166, cy + 100, cx + 166, cy + 102, 0x8842A8E8);

        this.drawCenteredString(this.fontRenderer, "Argon Gun Attachment Workbench", cx, cy - 92, 0xFFFFFF);

        // Left 3D weapon preview box
        int boxX1 = cx - 154;
        int boxY1 = cy - 72;
        int boxX2 = cx - 6;
        int boxY2 = cy + 62;
        drawRect(boxX1, boxY1, boxX2, boxY2, 0xAA0E1217);
        drawRect(boxX1, boxY1, boxX2, boxY1 + 1, 0xFF34414F);
        drawRect(boxX1, boxY2 - 1, boxX2, boxY2, 0xFF34414F);

        ItemStack held = this.getHeldGun();

        if (held.isEmpty())
        {
            this.drawCenteredString(this.fontRenderer, "Hold an Argon firearm", (boxX1 + boxX2) / 2, cy - 18, 0xFFCC66);
            this.drawCenteredString(this.fontRenderer, "in your main hand", (boxX1 + boxX2) / 2, cy - 6, 0xAAAAAA);
        }
        else
        {
            ItemArgonGun gun = (ItemArgonGun)held.getItem();
            GunType type = gun.getGunType();

            // Render live rotating 3D gun preview inside the panel
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)(boxX1 + boxX2) * 0.5F, (float)(boxY1 + boxY2) * 0.5F + 4.0F, 120.0F);
            GlStateManager.scale(68.0F, -68.0F, 68.0F);
            GlStateManager.rotate(14.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(this.previewYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, -0.08F, 0.35F);
            GunModelRenderer.renderPreviewModel(held, this.previewYaw * 2.0F);
            GlStateManager.popMatrix();

            // Weapon header & live stats readout
            this.drawString(this.fontRenderer, type.displayName, cx + 6, cy - 72, 0x70D6FF);

            float dmg = GunAttachment.getEffectiveDamage(type, held);
            int mag = GunAttachment.getEffectiveMagazineSize(type, held);
            float recoil = GunAttachment.getEffectiveRecoil(type, held, true);
            float spread = GunAttachment.getEffectiveSpread(type, held, true);

            String statsLine1 = "DMG: " + String.format("%.1f", dmg) + "  MAG: " + mag;
            String statsLine2 = "REC: " + String.format("%.1f", recoil) + "  SPR: " + String.format("%.2f", spread);
            this.drawString(this.fontRenderer, statsLine1, cx + 6, cy - 60, 0xD8E2EC);
            this.drawString(this.fontRenderer, statsLine2, cx + 6, cy - 50, 0xA8BAC9);

            if (!type.supportsAttachments())
            {
                this.drawString(this.fontRenderer, "Heavy ordnance: fixed frame", cx + 6, cy + 56, 0xFF9966);
            }
            else if (this.mc.player != null && !this.mc.player.capabilities.isCreativeMode)
            {
                this.drawString(this.fontRenderer, "Uses attachment items in bag", cx + 6, cy + 56, 0x8EA3B5);
            }
            else
            {
                this.drawString(this.fontRenderer, "Creative Mode: all parts free", cx + 6, cy + 56, 0x6EEB83);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
