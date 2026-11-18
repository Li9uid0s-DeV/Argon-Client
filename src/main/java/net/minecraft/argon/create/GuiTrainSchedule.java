package net.minecraft.argon.create;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

public class GuiTrainSchedule extends GuiScreen
{
    private final EntityPlayer player;
    private final EnumHand hand;
    private final ItemStack stack;
    private final TrainScheduleData schedule;
    private GuiTextField stationNameField;
    private int selectedWaitTicks = 100;
    private GuiButton waitButton;
    private GuiButton loopButton;

    public GuiTrainSchedule(EntityPlayer player, EnumHand hand, ItemStack stack)
    {
        this.player = player;
        this.hand = hand;
        this.stack = stack;
        this.schedule = ItemTrainSchedule.readSchedule(stack);
    }

    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.stationNameField = new GuiTextField(0, this.fontRenderer, cx - 120, cy - 68, 150, 20);
        this.stationNameField.setMaxStringLength(32);
        this.stationNameField.setText("Station");
        this.stationNameField.setFocused(true);

        this.buttonList.add(new GuiButton(1, cx + 36, cy - 68, 84, 20, "+ Add Stop"));
        this.waitButton = new GuiButton(2, cx - 120, cy - 42, 118, 20, this.getWaitLabel());
        this.loopButton = new GuiButton(3, cx + 2, cy - 42, 118, 20, this.getLoopLabel());
        this.buttonList.add(this.waitButton);
        this.buttonList.add(this.loopButton);

        this.buttonList.add(new GuiButton(4, cx - 120, cy + 56, 118, 20, "Clear Stops"));
        this.buttonList.add(new GuiButton(5, cx + 2, cy + 56, 118, 20, "Save & Close"));
    }

    private String getWaitLabel()
    {
        return "Wait: " + (this.selectedWaitTicks / 20) + "s";
    }

    private String getLoopLabel()
    {
        return "Loop: " + (this.schedule.loop ? "ON" : "OFF");
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
        ItemTrainSchedule.writeSchedule(this.stack, this.schedule);
        this.player.setHeldItem(this.hand, this.stack);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 1)
        {
            String name = this.stationNameField != null ? this.stationNameField.getText().trim() : "Station";

            if (name.length() == 0)
            {
                name = "Station";
            }

            double tx = 0.0D;
            double ty = 0.0D;
            double tz = 0.0D;
            boolean found = false;

            if (this.mc.world != null)
            {
                for (int i = 0; i < this.mc.world.loadedTileEntityList.size(); ++i)
                {
                    TileEntity te = this.mc.world.loadedTileEntityList.get(i);

                    if (te instanceof TileEntityTrainStation && ((TileEntityTrainStation)te).getStationName().equalsIgnoreCase(name))
                    {
                        BlockPos tp = ((TileEntityTrainStation)te).findNearbyTrackPos();
                        BlockPos usePos = tp != null ? tp : te.getPos();
                        tx = (double)usePos.getX() + 0.5D;
                        ty = (double)usePos.getY() + 0.19D;
                        tz = (double)usePos.getZ() + 0.5D;
                        found = true;
                        break;
                    }
                }
            }

            this.schedule.stops.add(new TrainScheduleData.StopEntry(name, this.selectedWaitTicks, tx, ty, tz, found));
            ItemTrainSchedule.writeSchedule(this.stack, this.schedule);
        }
        else if (button.id == 2)
        {
            if (this.selectedWaitTicks == 100) this.selectedWaitTicks = 200;
            else if (this.selectedWaitTicks == 200) this.selectedWaitTicks = 600;
            else this.selectedWaitTicks = 100;
            this.waitButton.displayString = this.getWaitLabel();
        }
        else if (button.id == 3)
        {
            this.schedule.loop = !this.schedule.loop;
            this.loopButton.displayString = this.getLoopLabel();
        }
        else if (button.id == 4)
        {
            this.schedule.stops.clear();
            ItemTrainSchedule.writeSchedule(this.stack, this.schedule);
        }
        else if (button.id == 5)
        {
            ItemTrainSchedule.writeSchedule(this.stack, this.schedule);
            this.mc.displayGuiScreen((GuiScreen)null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.stationNameField != null && this.stationNameField.textboxKeyTyped(typedChar, keyCode))
        {
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (this.stationNameField != null)
        {
            this.stationNameField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen()
    {
        if (this.stationNameField != null)
        {
            this.stationNameField.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        int cx = this.width / 2;
        int cy = this.height / 2;
        drawRect(cx - 135, cy - 95, cx + 135, cy + 86, 0xCC1A1D24);
        drawRect(cx - 135, cy - 95, cx + 135, cy - 93, 0xFFE6B84D);

        this.drawCenteredString(this.fontRenderer, "\u00a76Create Train Schedule", cx, cy - 86, 0xFFFFFF);

        if (this.stationNameField != null)
        {
            this.stationNameField.drawTextBox();
        }

        int listTop = cy - 14;
        int maxShow = Math.min(5, this.schedule.stops.size());

        if (maxShow == 0)
        {
            this.drawCenteredString(this.fontRenderer, "\u00a77No stops added yet.", cx, listTop + 20, 0xAAAAAA);
        }
        else
        {
            for (int i = 0; i < maxShow; ++i)
            {
                TrainScheduleData.StopEntry e = this.schedule.stops.get(i);
                String line = "\u00a7e" + (i + 1) + ". \u00a7f" + e.stationName + " \u00a77(" + (e.waitTicks / 20) + "s)";
                this.fontRenderer.drawStringWithShadow(line, cx - 118, listTop + i * 13, 0xFFFFFF);
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
