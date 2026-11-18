package net.minecraft.src;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class GuiArgonSkinMaker extends GuiScreen
{
    private final GuiScreen parentScreen;

    private static final int TOOL_PENCIL = 0;
    private static final int TOOL_BRUSH = 1;
    private static final int TOOL_FILL = 2;
    private static final int TOOL_ERASER = 3;
    private static final int TOOL_PICKER = 4;
    private static final String[] TOOL_NAMES = new String[]
    {
        "Pencil (1px)",
        "Brush (2x2)",
        "Fill Bucket",
        "Eraser",
        "Color Picker"
    };

    private static final int[] PALETTE_COLORS = new int[]
    {
        0xFF00E5FF, 0xFF3399FF, 0xFF3F51B5, 0xFF9C27B0, 0xFFE91E63, 0xFFF44336,
        0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF009688, 0xFFFFFFFF, 0xFFB0BEC5,
        0xFF607D8B, 0xFF263238, 0xFF141B2D, 0xFF000000, 0xFFF5D0B5, 0xFFD9A47E,
        0xFFC69672, 0xFFA87D5B, 0xFF7B4A31, 0xFF4A3423, 0xFF00AABB, 0xFF3F3F88
    };

    private int currentTool = TOOL_PENCIL;
    private int selectedColor = PALETTE_COLORS[0];
    private boolean showGrid = true;
    private boolean showUvGuides = true;
    private boolean mirrorHorizontal = false;
    private String statusMessage = "\u00a77Left-click to paint, Right-click to pick color";
    private int statusTimer = 0;

    private int canvasX;
    private int canvasY;
    private int cellSize;
    private int canvasSize;

    private int paletteX;
    private int paletteY;

    private GuiButton btnTool;
    private GuiButton btnGrid;
    private GuiButton btnUvGuides;
    private GuiButton btnMirror;
    private GuiButton btnToggleActive;

    public GuiArgonSkinMaker(GuiScreen parentScreenIn)
    {
        this.parentScreen = parentScreenIn;
    }

    public void initGui()
    {
        ArgonSkinManager.ensureLoaded();
        this.buttonList.clear();

        int availableH = this.height - 64;
        this.cellSize = MathHelper.clamp(availableH / 64, 2, 4);
        this.canvasSize = this.cellSize * 64;
        this.canvasX = Math.max(12, this.width / 2 - this.canvasSize - 10);
        this.canvasY = 30;

        int rightPanelX = this.canvasX + this.canvasSize + 14;
        this.paletteX = rightPanelX;
        this.paletteY = 30;

        int controlsY = this.paletteY + 62;
        int btnW = 104;
        int gapX = 108;

        this.btnTool = new GuiButton(10, rightPanelX, controlsY, btnW, 18, "Tool: " + TOOL_NAMES[this.currentTool]);
        this.btnMirror = new GuiButton(11, rightPanelX + gapX, controlsY, btnW, 18, "Mirror X: " + (this.mirrorHorizontal ? "\u00a7aON" : "\u00a7cOFF"));
        this.btnGrid = new GuiButton(12, rightPanelX, controlsY + 20, btnW, 18, "Grid: " + (this.showGrid ? "\u00a7aON" : "\u00a7cOFF"));
        this.btnUvGuides = new GuiButton(13, rightPanelX + gapX, controlsY + 20, btnW, 18, "UV Guide: " + (this.showUvGuides ? "\u00a7aON" : "\u00a7cOFF"));

        this.buttonList.add(this.btnTool);
        this.buttonList.add(this.btnMirror);
        this.buttonList.add(this.btnGrid);
        this.buttonList.add(this.btnUvGuides);

        this.buttonList.add(new GuiButton(20, rightPanelX, controlsY + 42, btnW, 18, "Preset: Steve"));
        this.buttonList.add(new GuiButton(21, rightPanelX + gapX, controlsY + 42, btnW, 18, "\u00a7bPreset: Argon"));

        this.btnToggleActive = new GuiButton(30, rightPanelX, controlsY + 64, btnW * 2 + 4, 18, this.getCustomSkinStatusLabel());
        this.buttonList.add(this.btnToggleActive);

        this.buttonList.add(new GuiButton(31, rightPanelX, controlsY + 86, btnW, 20, "\u00a7aApply & Save"));
        this.buttonList.add(new GuiButton(200, rightPanelX + gapX, controlsY + 86, btnW, 20, I18n.format("gui.done")));
    }

    private String getCustomSkinStatusLabel()
    {
        return "Player Skin Override: " + (ArgonSkinManager.hasCustomSkin() ? "\u00a7aACTIVE" : "\u00a77DEFAULT");
    }

    public void updateScreen()
    {
        if (this.statusTimer > 0)
        {
            --this.statusTimer;
        }
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (!button.enabled)
        {
            return;
        }

        if (button.id == 10)
        {
            this.currentTool = (this.currentTool + 1) % TOOL_NAMES.length;
            this.btnTool.displayString = "Tool: " + TOOL_NAMES[this.currentTool];
        }
        else if (button.id == 11)
        {
            this.mirrorHorizontal = !this.mirrorHorizontal;
            this.btnMirror.displayString = "Mirror X: " + (this.mirrorHorizontal ? "\u00a7aON" : "\u00a7cOFF");
        }
        else if (button.id == 12)
        {
            this.showGrid = !this.showGrid;
            this.btnGrid.displayString = "Grid: " + (this.showGrid ? "\u00a7aON" : "\u00a7cOFF");
        }
        else if (button.id == 13)
        {
            this.showUvGuides = !this.showUvGuides;
            this.btnUvGuides.displayString = "UV Guide: " + (this.showUvGuides ? "\u00a7aON" : "\u00a7cOFF");
        }
        else if (button.id == 20)
        {
            ArgonSkinManager.initDefaultSteveTemplate();
            ArgonSkinManager.markTextureDirty();
            this.setStatus("\u00a7eLoaded Classic Steve 64x64 template.");
        }
        else if (button.id == 21)
        {
            ArgonSkinManager.initArgonCyberTemplate();
            ArgonSkinManager.markTextureDirty();
            this.setStatus("\u00a7bLoaded Argon Plasma Cyber 64x64 template.");
        }
        else if (button.id == 30)
        {
            ArgonSkinManager.setCustomSkinEnabled(!ArgonSkinManager.hasCustomSkin());
            ArgonSkinManager.saveSkin();
            this.btnToggleActive.displayString = this.getCustomSkinStatusLabel();
        }
        else if (button.id == 31)
        {
            ArgonSkinManager.setCustomSkinEnabled(true);
            ArgonSkinManager.markTextureDirty();
            ArgonSkinManager.saveSkin();
            this.btnToggleActive.displayString = this.getCustomSkinStatusLabel();
            this.setStatus("\u00a7aCustom skin applied & saved to options_skin.txt!");
        }
        else if (button.id == 200)
        {
            ArgonSkinManager.saveSkin();
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    private void setStatus(String msg)
    {
        this.statusMessage = msg;
        this.statusTimer = 100;
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Check Palette Click
        int cols = 8;
        int swatchSize = 14;
        int swatchGap = 16;

        for (int i = 0; i < PALETTE_COLORS.length; ++i)
        {
            int col = i % cols;
            int row = i / cols;
            int sx = this.paletteX + col * swatchGap;
            int sy = this.paletteY + row * swatchGap;

            if (mouseX >= sx && mouseX < sx + swatchSize && mouseY >= sy && mouseY < sy + swatchSize)
            {
                this.selectedColor = PALETTE_COLORS[i];

                if (this.currentTool == TOOL_ERASER || this.currentTool == TOOL_PICKER)
                {
                    this.currentTool = TOOL_PENCIL;
                    this.btnTool.displayString = "Tool: " + TOOL_NAMES[this.currentTool];
                }

                return;
            }
        }

        // Check Canvas Click
        this.handleCanvasInteraction(mouseX, mouseY, mouseButton, true);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        this.handleCanvasInteraction(mouseX, mouseY, clickedMouseButton, false);
    }

    private void handleCanvasInteraction(int mouseX, int mouseY, int mouseButton, boolean isInitialClick)
    {
        if (mouseX < this.canvasX || mouseX >= this.canvasX + this.canvasSize || mouseY < this.canvasY || mouseY >= this.canvasY + this.canvasSize)
        {
            return;
        }

        int gx = (mouseX - this.canvasX) / this.cellSize;
        int gy = (mouseY - this.canvasY) / this.cellSize;

        if (mouseButton == 1 || this.currentTool == TOOL_PICKER)
        {
            int picked = ArgonSkinManager.getPixel(gx, gy);

            if ((picked >>> 24) != 0)
            {
                this.selectedColor = picked | 0xFF000000;
                this.setStatus("\u00a7bPicked color #" + String.format("%06X", this.selectedColor & 0xFFFFFF) + " at (" + gx + ", " + gy + ")");
            }

            return;
        }

        if (mouseButton != 0)
        {
            return;
        }

        if (this.currentTool == TOOL_FILL)
        {
            if (isInitialClick)
            {
                ArgonSkinManager.floodFill(gx, gy, this.selectedColor);
                ArgonSkinManager.markTextureDirty();
            }
        }
        else if (this.currentTool == TOOL_ERASER)
        {
            this.paintAt(gx, gy, 0x00000000, 1);
            ArgonSkinManager.markTextureDirty();
        }
        else if (this.currentTool == TOOL_BRUSH)
        {
            this.paintAt(gx, gy, this.selectedColor, 2);
            ArgonSkinManager.markTextureDirty();
        }
        else
        {
            this.paintAt(gx, gy, this.selectedColor, 1);
            ArgonSkinManager.markTextureDirty();
        }
    }

    private void paintAt(int gx, int gy, int color, int brushRadius)
    {
        for (int dy = 0; dy < brushRadius; ++dy)
        {
            for (int dx = 0; dx < brushRadius; ++dx)
            {
                int px = gx + dx;
                int py = gy + dy;
                ArgonSkinManager.setPixel(px, py, color);

                if (this.mirrorHorizontal)
                {
                    int mirrorX = 63 - px;
                    ArgonSkinManager.setPixel(mirrorX, py, color);
                }
            }
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, "\u00a7bArgon \u00a79Skin Maker \u00a77(64x64 DynamicTexture Editor)", this.width / 2, 10, 0xFFFFFF);

        // Draw Canvas Frame & Dark Checkerboard Backdrop
        drawRect(this.canvasX - 2, this.canvasY - 2, this.canvasX + this.canvasSize + 2, this.canvasY + this.canvasSize + 2, 0xCC3399FF);
        drawRect(this.canvasX, this.canvasY, this.canvasX + this.canvasSize, this.canvasY + this.canvasSize, 0xFF181C24);

        // Draw 8x8 Checkerboard Sub-blocks
        int blockStep = this.cellSize * 4;

        for (int by = 0; by < 16; ++by)
        {
            for (int bx = 0; bx < 16; ++bx)
            {
                if (((bx + by) & 1) == 1)
                {
                    int rx = this.canvasX + bx * blockStep;
                    int ry = this.canvasY + by * blockStep;
                    drawRect(rx, ry, rx + blockStep, ry + blockStep, 0xFF222834);
                }
            }
        }

        // Bind & Render 64x64 DynamicTexture in 1 Textured Quad
        ResourceLocation skinLoc = ArgonSkinManager.getCustomSkinLocation();

        if (skinLoc != null)
        {
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(skinLoc);
            this.drawTexturedQuad(this.canvasX, this.canvasY, this.canvasSize, this.canvasSize, 0.0F, 0.0F, 1.0F, 1.0F);
        }

        // Optional pixel grid lines
        if (this.showGrid && this.cellSize >= 3)
        {
            for (int i = 1; i < 64; ++i)
            {
                int lineAlpha = (i % 8 == 0) ? 0x44FFFFFF : 0x18FFFFFF;
                int px = this.canvasX + i * this.cellSize;
                int py = this.canvasY + i * this.cellSize;
                drawRect(px, this.canvasY, px + 1, this.canvasY + this.canvasSize, lineAlpha);
                drawRect(this.canvasX, py, this.canvasX + this.canvasSize, py + 1, lineAlpha);
            }
        }

        // Optional UV Region Guides
        if (this.showUvGuides)
        {
            this.drawUvBox(8, 0, 16, 8, 0x8800E5FF);   // Head Top/Bottom
            this.drawUvBox(0, 8, 32, 8, 0x8800E5FF);   // Head Sides/Front/Back
            this.drawUvBox(32, 0, 32, 16, 0x553399FF); // Hat Overlay
            this.drawUvBox(16, 16, 24, 16, 0x884CAF50);// Body
            this.drawUvBox(40, 16, 16, 16, 0x88FF9800);// Right Arm
            this.drawUvBox(0, 16, 16, 16, 0x88E91E63); // Right Leg
            this.drawUvBox(16, 48, 16, 16, 0x88E91E63);// Left Leg
            this.drawUvBox(32, 48, 16, 16, 0x88FF9800);// Left Arm
        }

        // Hover pixel highlight & UV Region inspection
        String hoverInfo = "Hover over the 64x64 UV grid to inspect region";

        if (mouseX >= this.canvasX && mouseX < this.canvasX + this.canvasSize && mouseY >= this.canvasY && mouseY < this.canvasY + this.canvasSize)
        {
            int hx = (mouseX - this.canvasX) / this.cellSize;
            int hy = (mouseY - this.canvasY) / this.cellSize;
            int px = this.canvasX + hx * this.cellSize;
            int py = this.canvasY + hy * this.cellSize;
            drawRect(px, py, px + this.cellSize, py + this.cellSize, 0x88FFFFFF);
            hoverInfo = "\u00a7bUV (" + hx + ", " + hy + ") \u00a77- \u00a7f" + ArgonSkinManager.getRegionLabel(hx, hy);
        }

        // Draw Color Palette Swatches
        int cols = 8;
        int swatchSize = 14;
        int swatchGap = 16;

        for (int i = 0; i < PALETTE_COLORS.length; ++i)
        {
            int col = i % cols;
            int row = i / cols;
            int sx = this.paletteX + col * swatchGap;
            int sy = this.paletteY + row * swatchGap;
            int c = PALETTE_COLORS[i];

            if (c == this.selectedColor)
            {
                drawRect(sx - 2, sy - 2, sx + swatchSize + 2, sy + swatchSize + 2, 0xFFFFFFFF);
            }
            else
            {
                drawRect(sx - 1, sy - 1, sx + swatchSize + 1, sy + swatchSize + 1, 0xFF000000);
            }

            drawRect(sx, sy, sx + swatchSize, sy + swatchSize, c);
        }

        // Draw Selected Color Preview & Hex
        int selBoxX = this.paletteX + cols * swatchGap + 6;
        int selBoxY = this.paletteY;
        drawRect(selBoxX - 1, selBoxY - 1, selBoxX + 42, selBoxY + 30, 0xFF3399FF);
        drawRect(selBoxX, selBoxY, selBoxX + 41, selBoxY + 29, this.selectedColor);
        this.drawString(this.fontRenderer, "#" + String.format("%06X", this.selectedColor & 0xFFFFFF), selBoxX, selBoxY + 34, 0xCCCCCC);

        // Draw Live Assembled Front & Back Character Preview
        if (skinLoc != null)
        {
            int previewBoxX = this.paletteX;
            int previewBoxY = this.paletteY + 175;
            int previewW = 212;
            int previewH = 74;

            if (previewBoxY + previewH < this.height - 16)
            {
                drawRect(previewBoxX, previewBoxY, previewBoxX + previewW, previewBoxY + previewH, 0x66000000);
                drawRect(previewBoxX, previewBoxY, previewBoxX + 2, previewBoxY + previewH, 0xCC3399FF);
                this.drawString(this.fontRenderer, "\u00a7bLive Preview \u00a77(Front / Back)", previewBoxX + 8, previewBoxY + 4, 0xFFFFFF);

                this.mc.getTextureManager().bindTexture(skinLoc);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.renderAssembledPreview(previewBoxX + 46, previewBoxY + 16, 2, false);
                this.renderAssembledPreview(previewBoxX + 136, previewBoxY + 16, 2, true);
            }
        }

        this.drawString(this.fontRenderer, hoverInfo, this.canvasX, this.canvasY + this.canvasSize + 5, 0xFFFFFF);
        this.drawString(this.fontRenderer, this.statusMessage, this.canvasX, this.canvasY + this.canvasSize + 16, 0xCCCCCC);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderAssembledPreview(int topLeftX, int topLeftY, int scale, boolean backView)
    {
        // Head (8x8)
        int headX = topLeftX + 4 * scale;
        int headY = topLeftY;
        this.drawSkinSubRect(headX, headY, 8 * scale, 8 * scale, backView ? 24 : 8, 8, 8, 8);
        this.drawSkinSubRect(headX, headY, 8 * scale, 8 * scale, backView ? 56 : 40, 8, 8, 8);

        // Body (8x12)
        int bodyX = topLeftX + 4 * scale;
        int bodyY = topLeftY + 8 * scale;
        this.drawSkinSubRect(bodyX, bodyY, 8 * scale, 12 * scale, backView ? 32 : 20, 20, 8, 12);

        // Arms (4x12 each)
        int leftArmX = topLeftX;
        int rightArmX = topLeftX + 12 * scale;
        int armY = bodyY;

        if (!backView)
        {
            this.drawSkinSubRect(leftArmX, armY, 4 * scale, 12 * scale, 44, 20, 4, 12);
            this.drawSkinSubRect(rightArmX, armY, 4 * scale, 12 * scale, 36, 52, 4, 12);
        }
        else
        {
            this.drawSkinSubRect(leftArmX, armY, 4 * scale, 12 * scale, 44, 52, 4, 12);
            this.drawSkinSubRect(rightArmX, armY, 4 * scale, 12 * scale, 52, 20, 4, 12);
        }

        // Legs (4x12 each)
        int legY = bodyY + 12 * scale;
        int leftLegX = topLeftX + 4 * scale;
        int rightLegX = topLeftX + 8 * scale;

        if (!backView)
        {
            this.drawSkinSubRect(leftLegX, legY, 4 * scale, 12 * scale, 4, 20, 4, 12);
            this.drawSkinSubRect(rightLegX, legY, 4 * scale, 12 * scale, 20, 52, 4, 12);
        }
        else
        {
            this.drawSkinSubRect(leftLegX, legY, 4 * scale, 12 * scale, 28, 52, 4, 12);
            this.drawSkinSubRect(rightLegX, legY, 4 * scale, 12 * scale, 12, 20, 4, 12);
        }
    }

    private void drawSkinSubRect(int x, int y, int w, int h, int u, int v, int uw, int vh)
    {
        float u0 = (float)u / 64.0F;
        float v0 = (float)v / 64.0F;
        float u1 = (float)(u + uw) / 64.0F;
        float v1 = (float)(v + vh) / 64.0F;
        this.drawTexturedQuad(x, y, w, h, u0, v0, u1, v1);
    }

    private void drawTexturedQuad(int x, int y, int w, int h, float u0, float v0, float u1, float v1)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos((double)x, (double)(y + h), (double)this.zLevel).tex((double)u0, (double)v1).endVertex();
        buffer.pos((double)(x + w), (double)(y + h), (double)this.zLevel).tex((double)u1, (double)v1).endVertex();
        buffer.pos((double)(x + w), (double)y, (double)this.zLevel).tex((double)u1, (double)v0).endVertex();
        buffer.pos((double)x, (double)y, (double)this.zLevel).tex((double)u0, (double)v0).endVertex();
        tessellator.draw();
    }

    private void drawUvBox(int ux, int uy, int uw, int uh, int color)
    {
        int x0 = this.canvasX + ux * this.cellSize;
        int y0 = this.canvasY + uy * this.cellSize;
        int x1 = x0 + uw * this.cellSize;
        int y1 = y0 + uh * this.cellSize;
        drawRect(x0, y0, x1, y0 + 1, color);
        drawRect(x0, y1 - 1, x1, y1, color);
        drawRect(x0, y0, x0 + 1, y1, color);
        drawRect(x1 - 1, y0, x1, y1, color);
    }
}
