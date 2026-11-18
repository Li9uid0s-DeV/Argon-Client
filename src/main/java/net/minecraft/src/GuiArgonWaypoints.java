package net.minecraft.src;

import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;

public class GuiArgonWaypoints extends GuiScreen
{
    private final GuiScreen parentScreen;
    private GuiTextField nameField;
    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField zField;
    private int selectedColorIndex = 0;
    private int pageOffset = 0;
    private static final int ITEMS_PER_PAGE = 5;

    public GuiArgonWaypoints(GuiScreen parentScreenIn)
    {
        this.parentScreen = parentScreenIn;
    }

    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        int centerX = this.width / 2;
        int defaultX = 0;
        int defaultY = 64;
        int defaultZ = 0;

        if (this.mc != null && this.mc.player != null)
        {
            Entity view = this.mc.getRenderViewEntity() != null ? this.mc.getRenderViewEntity() : this.mc.player;
            defaultX = MathHelper.floor(view.posX);
            defaultY = MathHelper.floor(view.getEntityBoundingBox().minY);
            defaultZ = MathHelper.floor(view.posZ);
        }

        String prevName = this.nameField != null ? this.nameField.getText() : ("WP #" + (ArgonWaypointManager.getWaypoints().size() + 1));
        String prevX = this.xField != null ? this.xField.getText() : String.valueOf(defaultX);
        String prevY = this.yField != null ? this.yField.getText() : String.valueOf(defaultY);
        String prevZ = this.zField != null ? this.zField.getText() : String.valueOf(defaultZ);

        this.nameField = new GuiTextField(10, this.fontRenderer, centerX - 154, 34, 92, 18);
        this.nameField.setMaxStringLength(18);
        this.nameField.setText(prevName);

        this.xField = new GuiTextField(11, this.fontRenderer, centerX - 56, 34, 44, 18);
        this.xField.setMaxStringLength(8);
        this.xField.setText(prevX);

        this.yField = new GuiTextField(12, this.fontRenderer, centerX - 8, 34, 36, 18);
        this.yField.setMaxStringLength(5);
        this.yField.setText(prevY);

        this.zField = new GuiTextField(13, this.fontRenderer, centerX + 32, 34, 44, 18);
        this.zField.setMaxStringLength(8);
        this.zField.setText(prevZ);

        this.rebuildButtons();
    }

    private void rebuildButtons()
    {
        this.buttonList.clear();
        int centerX = this.width / 2;

        this.buttonList.add(new GuiButton(101, centerX + 80, 33, 75, 20, "Color: " + ArgonWaypointManager.COLOR_NAMES[this.selectedColorIndex]));
        this.buttonList.add(new GuiButton(102, centerX - 155, 58, 150, 20, "\u00a7b+ Add Waypoint"));
        this.buttonList.add(new GuiButton(103, centerX + 5, 58, 150, 20, "Use Player XYZ"));

        List<ArgonWaypointManager.Waypoint> list = ArgonWaypointManager.getWaypoints();
        int maxOffset = Math.max(0, list.size() - ITEMS_PER_PAGE);

        if (this.pageOffset > maxOffset)
        {
            this.pageOffset = maxOffset;
        }

        int listStartY = 92;

        for (int i = 0; i < ITEMS_PER_PAGE; ++i)
        {
            int wpIdx = this.pageOffset + i;

            if (wpIdx >= list.size())
            {
                break;
            }

            ArgonWaypointManager.Waypoint wp = list.get(wpIdx);
            int rowY = listStartY + i * 22;
            String stateLabel = wp.enabled ? "\u00a7aON" : "\u00a77OFF";
            this.buttonList.add(new GuiButton(1000 + wpIdx, centerX + 58, rowY, 46, 18, stateLabel));
            this.buttonList.add(new GuiButton(2000 + wpIdx, centerX + 108, rowY, 47, 18, "\u00a7cDelete"));
        }

        GuiButton prevBtn = new GuiButton(104, centerX - 155, this.height - 28, 65, 20, "< Prev");
        GuiButton nextBtn = new GuiButton(105, centerX - 85, this.height - 28, 65, 20, "Next >");
        prevBtn.enabled = this.pageOffset > 0;
        nextBtn.enabled = this.pageOffset + ITEMS_PER_PAGE < list.size();
        this.buttonList.add(prevBtn);
        this.buttonList.add(nextBtn);
        this.buttonList.add(new GuiButton(200, centerX + 5, this.height - 28, 150, 20, I18n.format("gui.done")));
    }

    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    public void updateScreen()
    {
        this.nameField.updateCursorCounter();
        this.xField.updateCursorCounter();
        this.yField.updateCursorCounter();
        this.zField.updateCursorCounter();
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (!button.enabled)
        {
            return;
        }

        if (button.id == 101)
        {
            this.selectedColorIndex = (this.selectedColorIndex + 1) % ArgonWaypointManager.COLOR_PALETTE.length;
            this.rebuildButtons();
        }
        else if (button.id == 102)
        {
            int x = this.parseCoord(this.xField.getText(), this.mc.player != null ? MathHelper.floor(this.mc.player.posX) : 0);
            int y = this.parseCoord(this.yField.getText(), this.mc.player != null ? MathHelper.floor(this.mc.player.posY) : 64);
            int z = this.parseCoord(this.zField.getText(), this.mc.player != null ? MathHelper.floor(this.mc.player.posZ) : 0);
            int dim = this.mc.player != null ? this.mc.player.dimension : 0;
            int color = ArgonWaypointManager.COLOR_PALETTE[this.selectedColorIndex];
            ArgonWaypointManager.addWaypoint(this.nameField.getText(), x, y, z, dim, color);
            this.selectedColorIndex = (this.selectedColorIndex + 1) % ArgonWaypointManager.COLOR_PALETTE.length;
            this.nameField.setText("WP #" + (ArgonWaypointManager.getWaypoints().size() + 1));
            this.rebuildButtons();
        }
        else if (button.id == 103)
        {
            if (this.mc.player != null)
            {
                Entity view = this.mc.getRenderViewEntity() != null ? this.mc.getRenderViewEntity() : this.mc.player;
                this.xField.setText(String.valueOf(MathHelper.floor(view.posX)));
                this.yField.setText(String.valueOf(MathHelper.floor(view.getEntityBoundingBox().minY)));
                this.zField.setText(String.valueOf(MathHelper.floor(view.posZ)));
            }
        }
        else if (button.id == 104)
        {
            this.pageOffset = Math.max(0, this.pageOffset - ITEMS_PER_PAGE);
            this.rebuildButtons();
        }
        else if (button.id == 105)
        {
            this.pageOffset += ITEMS_PER_PAGE;
            this.rebuildButtons();
        }
        else if (button.id == 200)
        {
            this.mc.displayGuiScreen(this.parentScreen);
        }
        else if (button.id >= 1000 && button.id < 2000)
        {
            ArgonWaypointManager.toggleWaypoint(button.id - 1000);
            this.rebuildButtons();
        }
        else if (button.id >= 2000 && button.id < 3000)
        {
            ArgonWaypointManager.removeWaypoint(button.id - 2000);
            this.rebuildButtons();
        }
    }

    private int parseCoord(String text, int fallback)
    {
        try
        {
            return Integer.parseInt(text.trim());
        }
        catch (NumberFormatException var4)
        {
            return fallback;
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.nameField.textboxKeyTyped(typedChar, keyCode))
        {
            return;
        }

        if (this.xField.textboxKeyTyped(typedChar, keyCode))
        {
            return;
        }

        if (this.yField.textboxKeyTyped(typedChar, keyCode))
        {
            return;
        }

        if (this.zField.textboxKeyTyped(typedChar, keyCode))
        {
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.nameField.mouseClicked(mouseX, mouseY, mouseButton);
        this.xField.mouseClicked(mouseX, mouseY, mouseButton);
        this.yField.mouseClicked(mouseX, mouseY, mouseButton);
        this.zField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        int centerX = this.width / 2;
        this.drawCenteredString(this.fontRenderer, "\u00a7bArgon \u00a79Minimap \u00a7fWaypoints", centerX, 10, 0xFFFFFF);

        this.fontRenderer.drawStringWithShadow("\u00a77Name", (float)(centerX - 154), 24.0F, 0xCCCCCC);
        this.fontRenderer.drawStringWithShadow("\u00a77X", (float)(centerX - 56), 24.0F, 0xCCCCCC);
        this.fontRenderer.drawStringWithShadow("\u00a77Y", (float)(centerX - 8), 24.0F, 0xCCCCCC);
        this.fontRenderer.drawStringWithShadow("\u00a77Z", (float)(centerX + 32), 24.0F, 0xCCCCCC);

        this.nameField.drawTextBox();
        this.xField.drawTextBox();
        this.yField.drawTextBox();
        this.zField.drawTextBox();

        List<ArgonWaypointManager.Waypoint> list = ArgonWaypointManager.getWaypoints();
        int listStartY = 92;

        if (list.isEmpty())
        {
            this.drawCenteredString(this.fontRenderer, "\u00a77No waypoints registered yet. Click '+ Add Waypoint' or press [B] in-game.", centerX, listStartY + 24, 0xAAAAAA);
        }
        else
        {
            for (int i = 0; i < ITEMS_PER_PAGE; ++i)
            {
                int wpIdx = this.pageOffset + i;

                if (wpIdx >= list.size())
                {
                    break;
                }

                ArgonWaypointManager.Waypoint wp = list.get(wpIdx);
                int rowY = listStartY + i * 22;
                drawRect(centerX - 155, rowY, centerX + 54, rowY + 18, 0x66000000);
                drawRect(centerX - 155, rowY, centerX - 151, rowY + 18, wp.color);

                String distStr = "";

                if (this.mc.player != null && this.mc.player.dimension == wp.dimension)
                {
                    double dx = (double)wp.x + 0.5D - this.mc.player.posX;
                    double dz = (double)wp.z + 0.5D - this.mc.player.posZ;
                    int dist = MathHelper.floor(Math.sqrt(dx * dx + dz * dz));
                    distStr = " \u00a7b(" + dist + "m)";
                }

                String label = "\u00a7f" + wp.name + " \u00a77[" + wp.x + ", " + wp.y + ", " + wp.z + "]" + distStr;
                this.fontRenderer.drawStringWithShadow(label, (float)(centerX - 146), (float)(rowY + 5), 0xFFFFFF);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
