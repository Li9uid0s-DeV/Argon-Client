package net.minecraft.src;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

public class GuiArgonSettings extends GuiScreen
{
    private GuiScreen prevScreen;
    protected String title;
    private GameSettings settings;
    private static GameSettings.Options[] enumOptions = new GameSettings.Options[]
    {
        GameSettings.Options.CAMERA_OVERHAUL,
        GameSettings.Options.CAMERA_OVERHAUL_INTENSITY,
        GameSettings.Options.BARREL_ROLL,
        GameSettings.Options.BARREL_ROLL_SENSITIVITY,
        GameSettings.Options.ENHANCED_VISUALS,
        GameSettings.Options.SOUND_PHYSICS,
        GameSettings.Options.ITEM_PHYSICS,
        GameSettings.Options.AMBIENT_SOUNDS,
        GameSettings.Options.JADE_HUD,
        GameSettings.Options.MINIMAP,
        GameSettings.Options.VOICE_CHAT,
        GameSettings.Options.VOICE_CHAT_DISTANCE,
        GameSettings.Options.VOICE_CHAT_VOLUME,
        GameSettings.Options.WEATHER2_QUALITY,
        GameSettings.Options.RESOLUTION_SCALE
    };
    private TooltipManager tooltipManager = new TooltipManager(this);

    public GuiArgonSettings(GuiScreen prevScreenIn, GameSettings settingsIn)
    {
        this.prevScreen = prevScreenIn;
        this.settings = settingsIn;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui()
    {
        this.title = "\u00a7bArgon \u00a79Client \u00a7fSettings";
        this.buttonList.clear();

        for (int i = 0; i < enumOptions.length; ++i)
        {
            GameSettings.Options gamesettings$options = enumOptions[i];
            int j = this.width / 2 - 155 + i % 2 * 160;
            int k = this.height / 6 + 21 * (i / 2) - 18;

            if (!gamesettings$options.getEnumFloat())
            {
                this.buttonList.add(new GuiOptionButtonOF(gamesettings$options.returnEnumOrdinal(), j, k, gamesettings$options, this.settings.getKeyBinding(gamesettings$options)));
            }
            else
            {
                this.buttonList.add(new GuiOptionSliderOF(gamesettings$options.returnEnumOrdinal(), j, k, gamesettings$options));
            }
        }

        int extraRowY = this.height / 6 + 21 * ((enumOptions.length + 1) / 2) - 18;
        this.buttonList.add(new GuiButton(201, this.width / 2 - 155, extraRowY, 150, 20, "\u00a7bWaypoints Manager..."));
        this.buttonList.add(new GuiButton(202, this.width / 2 + 5, extraRowY, 150, 20, "\u00a7bSkin Maker..."));
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height / 6 + 168 + 11, I18n.format("gui.done")));
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.enabled)
        {
            if (button.id < 200 && button instanceof GuiOptionButton)
            {
                this.settings.setOptionValue(((GuiOptionButton)button).getOption(), 1);
                button.displayString = this.settings.getKeyBinding(GameSettings.Options.getEnumOptions(button.id));
            }

            if (button.id == 201)
            {
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiArgonWaypoints(this));
            }

            if (button.id == 202)
            {
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiArgonSkinMaker(this));
            }

            if (button.id == 200)
            {
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(this.prevScreen);
            }
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, 15, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttonList);
    }
}
