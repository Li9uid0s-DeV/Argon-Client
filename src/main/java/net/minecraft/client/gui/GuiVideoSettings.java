package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.src.Config;
import net.minecraft.src.GuiAnimationSettingsOF;
import net.minecraft.src.GuiArgonSettings;
import net.minecraft.src.GuiDetailSettingsOF;
import net.minecraft.src.GuiOptionButtonOF;
import net.minecraft.src.GuiOptionSliderOF;
import net.minecraft.src.GuiOtherSettingsOF;
import net.minecraft.src.GuiPerformanceSettingsOF;
import net.minecraft.src.GuiQualitySettingsOF;
import net.minecraft.src.GuiScreenOF;
import net.minecraft.src.Lang;
import net.minecraft.src.TooltipManager;
import shadersmod.client.GuiShaders;

public class GuiVideoSettings extends GuiScreenOF
{
    private GuiScreen parentGuiScreen;
    protected String screenTitle = "Video Settings";
    private GameSettings guiGameSettings;
    private static GameSettings.Options[] videoOptions = new GameSettings.Options[] {GameSettings.Options.GRAPHICS, GameSettings.Options.RENDER_DISTANCE, GameSettings.Options.AMBIENT_OCCLUSION, GameSettings.Options.FRAMERATE_LIMIT, GameSettings.Options.AO_LEVEL, GameSettings.Options.VIEW_BOBBING, GameSettings.Options.GUI_SCALE, GameSettings.Options.USE_VBO, GameSettings.Options.GAMMA, GameSettings.Options.ATTACK_INDICATOR, GameSettings.Options.DYNAMIC_LIGHTS, GameSettings.Options.DYNAMIC_FOV, GameSettings.Options.RESOLUTION_SCALE, GameSettings.Options.CONNECTED_TEXTURES};
    private static final String __OBFID = "CL_00000718";
    private TooltipManager tooltipManager = new TooltipManager(this);

    public GuiVideoSettings(GuiScreen parentScreenIn, GameSettings gameSettingsIn)
    {
        this.parentGuiScreen = parentScreenIn;
        this.guiGameSettings = gameSettingsIn;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui()
    {
        this.screenTitle = I18n.format("options.videoTitle");
        this.buttonList.clear();

        int optionRows = (videoOptions.length + 1) / 2;
        int totalRows = optionRows + 4;
        int rowStep = 21;
        int topY = this.height / 6 - 12;

        if (topY + totalRows * rowStep + 24 > this.height)
        {
            rowStep = 18;
            topY = Math.max(16, (this.height - (totalRows * rowStep + 22)) / 2);
        }

        for (int i = 0; i < videoOptions.length; ++i)
        {
            GameSettings.Options gamesettings$options = videoOptions[i];

            if (gamesettings$options != null)
            {
                int j = this.width / 2 - 155 + i % 2 * 160;
                int k = topY + rowStep * (i / 2);

                if (gamesettings$options.getEnumFloat())
                {
                    this.buttonList.add(new GuiOptionSliderOF(gamesettings$options.returnEnumOrdinal(), j, k, gamesettings$options));
                }
                else
                {
                    this.buttonList.add(new GuiOptionButtonOF(gamesettings$options.returnEnumOrdinal(), j, k, gamesettings$options, this.guiGameSettings.getKeyBinding(gamesettings$options)));
                }
            }
        }

        int l = topY + rowStep * optionRows;
        int i1 = 0;
        i1 = this.width / 2 - 155 + 0;
        this.buttonList.add(new GuiOptionButton(231, i1, l, Lang.get("of.options.shaders")));
        i1 = this.width / 2 - 155 + 160;
        this.buttonList.add(new GuiOptionButton(202, i1, l, Lang.get("of.options.quality")));
        l = l + rowStep;
        i1 = this.width / 2 - 155 + 0;
        this.buttonList.add(new GuiOptionButton(201, i1, l, Lang.get("of.options.details")));
        i1 = this.width / 2 - 155 + 160;
        this.buttonList.add(new GuiOptionButton(212, i1, l, Lang.get("of.options.performance")));
        l = l + rowStep;
        i1 = this.width / 2 - 155 + 0;
        this.buttonList.add(new GuiOptionButton(211, i1, l, Lang.get("of.options.animations")));
        i1 = this.width / 2 - 155 + 160;
        this.buttonList.add(new GuiOptionButton(222, i1, l, Lang.get("of.options.other")));
        l = l + rowStep;
        this.buttonList.add(new GuiButton(240, this.width / 2 - 155, l, 310, 20, "\u00a7bArgon \u00a79Client \u00a7fSettings..."));
        l = l + rowStep;
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, Math.min(this.height - 24, Math.max(this.height / 6 + 168 + 11, l)), I18n.format("gui.done")));
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException
    {
        this.actionPerformed(button, 1);
    }

    protected void actionPerformedRightClick(GuiButton p_actionPerformedRightClick_1_)
    {
        if (p_actionPerformedRightClick_1_.id == GameSettings.Options.GUI_SCALE.ordinal())
        {
            this.actionPerformed(p_actionPerformedRightClick_1_, -1);
        }
    }

    private void actionPerformed(GuiButton p_actionPerformed_1_, int p_actionPerformed_2_)
    {
        if (p_actionPerformed_1_.enabled)
        {
            int i = this.guiGameSettings.guiScale;

            if (p_actionPerformed_1_.id < 200 && p_actionPerformed_1_ instanceof GuiOptionButton)
            {
                this.guiGameSettings.setOptionValue(((GuiOptionButton)p_actionPerformed_1_).getOption(), p_actionPerformed_2_);
                p_actionPerformed_1_.displayString = this.guiGameSettings.getKeyBinding(GameSettings.Options.getEnumOptions(p_actionPerformed_1_.id));
            }

            if (p_actionPerformed_1_.id == 200)
            {
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(this.parentGuiScreen);
            }

            if (this.guiGameSettings.guiScale != i)
            {
                ScaledResolution scaledresolution = new ScaledResolution(this.mc);
                int j = scaledresolution.getScaledWidth();
                int k = scaledresolution.getScaledHeight();
                this.setWorldAndResolution(this.mc, j, k);
            }

            if (p_actionPerformed_1_.id == 201)
            {
                this.mc.gameSettings.saveOptions();
                GuiDetailSettingsOF guidetailsettingsof = new GuiDetailSettingsOF(this, this.guiGameSettings);
                this.mc.displayGuiScreen(guidetailsettingsof);
            }

            if (p_actionPerformed_1_.id == 202)
            {
                this.mc.gameSettings.saveOptions();
                GuiQualitySettingsOF guiqualitysettingsof = new GuiQualitySettingsOF(this, this.guiGameSettings);
                this.mc.displayGuiScreen(guiqualitysettingsof);
            }

            if (p_actionPerformed_1_.id == 211)
            {
                this.mc.gameSettings.saveOptions();
                GuiAnimationSettingsOF guianimationsettingsof = new GuiAnimationSettingsOF(this, this.guiGameSettings);
                this.mc.displayGuiScreen(guianimationsettingsof);
            }

            if (p_actionPerformed_1_.id == 212)
            {
                this.mc.gameSettings.saveOptions();
                GuiPerformanceSettingsOF guiperformancesettingsof = new GuiPerformanceSettingsOF(this, this.guiGameSettings);
                this.mc.displayGuiScreen(guiperformancesettingsof);
            }

            if (p_actionPerformed_1_.id == 222)
            {
                this.mc.gameSettings.saveOptions();
                GuiOtherSettingsOF guiothersettingsof = new GuiOtherSettingsOF(this, this.guiGameSettings);
                this.mc.displayGuiScreen(guiothersettingsof);
            }

            if (p_actionPerformed_1_.id == 240)
            {
                this.mc.gameSettings.saveOptions();
                GuiArgonSettings guiargonsettings = new GuiArgonSettings(this, this.guiGameSettings);
                this.mc.displayGuiScreen(guiargonsettings);
            }

            if (p_actionPerformed_1_.id == 231)
            {
                if (Config.isAntialiasing() || Config.isAntialiasingConfigured())
                {
                    Config.showGuiMessage(Lang.get("of.message.shaders.aa1"), Lang.get("of.message.shaders.aa2"));
                    return;
                }

                if (Config.isAnisotropicFiltering())
                {
                    Config.showGuiMessage(Lang.get("of.message.shaders.af1"), Lang.get("of.message.shaders.af2"));
                    return;
                }

                if (Config.isFastRender())
                {
                    Config.showGuiMessage(Lang.get("of.message.shaders.fr1"), Lang.get("of.message.shaders.fr2"));
                    return;
                }

                if (Config.getGameSettings().anaglyph)
                {
                    Config.showGuiMessage(Lang.get("of.message.shaders.an1"), Lang.get("of.message.shaders.an2"));
                    return;
                }

                this.mc.gameSettings.saveOptions();
                GuiShaders guishaders = new GuiShaders(this, this.guiGameSettings);
                this.mc.displayGuiScreen(guishaders);
            }
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 15, 16777215);
        String s = Config.getVersion();
        String s1 = "HD_U";

        if (s1.equals("HD"))
        {
            s = "OptiFine HD C4";
        }

        if (s1.equals("HD_U"))
        {
            s = "OptiFine HD C4 Ultra";
        }

        if (s1.equals("L"))
        {
            s = "OptiFine C4 Light";
        }

        this.drawString(this.fontRenderer, s, 2, this.height - 10, 8421504);
        String s2 = "\u00a7bArgon Client \u00a791.12.2";
        int i = this.fontRenderer.getStringWidth(s2);
        this.drawString(this.fontRenderer, s2, this.width - i - 2, this.height - 10, 8421504);
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttonList);
    }

    public static int getButtonWidth(GuiButton p_getButtonWidth_0_)
    {
        return p_getButtonWidth_0_.width;
    }

    public static int getButtonHeight(GuiButton p_getButtonHeight_0_)
    {
        return p_getButtonHeight_0_.height;
    }

    public static void drawGradientRect(GuiScreen p_drawGradientRect_0_, int p_drawGradientRect_1_, int p_drawGradientRect_2_, int p_drawGradientRect_3_, int p_drawGradientRect_4_, int p_drawGradientRect_5_, int p_drawGradientRect_6_)
    {
        p_drawGradientRect_0_.drawGradientRect(p_drawGradientRect_1_, p_drawGradientRect_2_, p_drawGradientRect_3_, p_drawGradientRect_4_, p_drawGradientRect_5_, p_drawGradientRect_6_);
    }
}
