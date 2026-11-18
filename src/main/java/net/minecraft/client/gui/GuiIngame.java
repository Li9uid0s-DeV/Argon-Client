package net.minecraft.client.gui;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.IChatListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.chat.NormalChatListener;
import net.minecraft.client.gui.chat.OverlayChatListener;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.effect.EntityWeatherStorm;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.src.ArgonWaypointManager;
import net.minecraft.src.Config;
import net.minecraft.src.ArgonVoiceChatManager;
import net.minecraft.src.CustomColors;
import net.minecraft.src.CustomItems;
import net.minecraft.src.Reflector;
import net.minecraft.src.ReflectorForge;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.FoodStats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.border.WorldBorder;

public class GuiIngame extends Gui
{
    private static final ResourceLocation VIGNETTE_TEX_PATH = new ResourceLocation("textures/misc/vignette.png");
    private static final ResourceLocation WIDGETS_TEX_PATH = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation PUMPKIN_BLUR_TEX_PATH = new ResourceLocation("textures/misc/pumpkinblur.png");
    private final Random rand = new Random();
    private final Minecraft mc;
    private final RenderItem itemRenderer;

    /** ChatGUI instance that retains all previous chat data */
    private final GuiNewChat persistantChatGUI;
    private int updateCounter;

    /** The string specifying which record music is playing */
    private String overlayMessage = "";

    /** How many ticks the record playing message will be displayed */
    private int overlayMessageTime;
    private boolean animateOverlayMessageColor;

    /** Previous frame vignette brightness (slowly changes by 1% each frame) */
    public float prevVignetteBrightness = 1.0F;

    /** Remaining ticks the item highlight should be visible */
    private int remainingHighlightTicks;

    /** The ItemStack that is currently being highlighted */
    private ItemStack highlightingItemStack = ItemStack.EMPTY;
    private final GuiOverlayDebug overlayDebug;
    private final GuiSubtitleOverlay overlaySubtitle;

    /** The spectator GUI for this in-game GUI instance */
    private final GuiSpectator spectatorGui;
    private final GuiPlayerTabOverlay overlayPlayerList;
    private final GuiBossOverlay overlayBoss;

    /** A timer for the current title and subtitle displayed */
    private int titlesTimer;

    /** The current title displayed */
    private String displayedTitle = "";

    /** The current sub-title displayed */
    private String displayedSubTitle = "";

    /** The time that the title take to fade in */
    private int titleFadeIn;

    /** The time that the title is display */
    private int titleDisplayTime;

    /** The time that the title take to fade out */
    private int titleFadeOut;
    private int playerHealth;
    private int lastPlayerHealth;

    /** The last recorded system time */
    private long lastSystemTime;

    /** Used with updateCounter to make the heart bar flash */
    private long healthUpdateCounter;
    private final Map<ChatType, List<IChatListener>> chatListeners = Maps.<ChatType, List<IChatListener>>newHashMap();
    private static final String ARGON_HUD_TITLE = "\u00a7bArgon Client \u00a791.12.2";
    private int lastHudFps = -1;
    private int lastHudX = Integer.MIN_VALUE;
    private int lastHudY = Integer.MIN_VALUE;
    private int lastHudZ = Integer.MIN_VALUE;
    private String cachedFpsLine = "";
    private String cachedXyzLine = "";
    private int cachedHudBoxWidth = 0;
    private IBlockState lastJadeState = null;
    private BlockPos lastJadePos = null;
    private Item lastJadeHeldItem = null;
    private String cachedJadeTitle = "";
    private String cachedJadeSub = "";
    private ItemStack cachedJadeIcon = ItemStack.EMPTY;
    private ItemStack probePickaxe = ItemStack.EMPTY;
    private ItemStack probeAxe = ItemStack.EMPTY;
    private ItemStack probeShovel = ItemStack.EMPTY;
    private ItemStack probeShears = ItemStack.EMPTY;
    private ItemStack probeSword = ItemStack.EMPTY;
    private static final int MINIMAP_RADIUS = 32;
    private static final int MINIMAP_DIAMETER = 64;
    private DynamicTexture minimapTexture = null;
    private ResourceLocation minimapTextureLocation = null;
    private int minimapFrameCounter = 0;
    private int lastMinimapScanX = Integer.MIN_VALUE;
    private int lastMinimapScanZ = Integer.MIN_VALUE;
    private int lastMinimapDimension = Integer.MIN_VALUE;
    private final BlockPos.MutableBlockPos minimapMutablePos = new BlockPos.MutableBlockPos();

    public GuiIngame(Minecraft mcIn)
    {
        this.mc = mcIn;
        this.itemRenderer = mcIn.getRenderItem();
        this.overlayDebug = new GuiOverlayDebug(mcIn);
        this.spectatorGui = new GuiSpectator(mcIn);
        this.persistantChatGUI = new GuiNewChat(mcIn);
        this.overlayPlayerList = new GuiPlayerTabOverlay(mcIn, this);
        this.overlayBoss = new GuiBossOverlay(mcIn);
        this.overlaySubtitle = new GuiSubtitleOverlay(mcIn);

        for (ChatType chattype : ChatType.values())
        {
            this.chatListeners.put(chattype, Lists.newArrayList());
        }

        IChatListener ichatlistener = NarratorChatListener.INSTANCE;
        (this.chatListeners.get(ChatType.CHAT)).add(new NormalChatListener(mcIn));
        (this.chatListeners.get(ChatType.CHAT)).add(ichatlistener);
        (this.chatListeners.get(ChatType.SYSTEM)).add(new NormalChatListener(mcIn));
        (this.chatListeners.get(ChatType.SYSTEM)).add(ichatlistener);
        (this.chatListeners.get(ChatType.GAME_INFO)).add(new OverlayChatListener(mcIn));
        this.setDefaultTitlesTimes();
    }

    /**
     * Set the differents times for the titles to their default values
     */
    public void setDefaultTitlesTimes()
    {
        this.titleFadeIn = 10;
        this.titleDisplayTime = 70;
        this.titleFadeOut = 20;
    }

    public void renderGameOverlay(float partialTicks)
    {
        ScaledResolution scaledresolution = new ScaledResolution(this.mc);
        int i = scaledresolution.getScaledWidth();
        int j = scaledresolution.getScaledHeight();
        FontRenderer fontrenderer = this.getFontRenderer();
        GlStateManager.enableBlend();

        if (Config.isVignetteEnabled())
        {
            this.renderVignette(this.mc.player.getBrightness(), scaledresolution);
        }
        else
        {
            GlStateManager.enableDepth();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }

        if (Config.isEnhancedVisuals())
        {
            this.renderEnhancedVisuals(scaledresolution, partialTicks);
        }

        ItemStack itemstack = this.mc.player.inventory.armorItemInSlot(3);

        if (this.mc.gameSettings.thirdPersonView == 0 && itemstack.getItem() == Item.getItemFromBlock(Blocks.PUMPKIN))
        {
            this.renderPumpkinOverlay(scaledresolution);
        }

        if (!this.mc.player.isPotionActive(MobEffects.NAUSEA))
        {
            float f = this.mc.player.prevTimeInPortal + (this.mc.player.timeInPortal - this.mc.player.prevTimeInPortal) * partialTicks;

            if (f > 0.0F)
            {
                this.renderPortal(f, scaledresolution);
            }
        }

        if (this.mc.playerController.isSpectator())
        {
            this.spectatorGui.renderTooltip(scaledresolution, partialTicks);
        }
        else
        {
            this.renderHotbar(scaledresolution, partialTicks);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(ICONS);
        GlStateManager.enableBlend();
        this.renderAttackIndicator(partialTicks, scaledresolution);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        this.mc.mcProfiler.startSection("bossHealth");
        this.overlayBoss.renderBossHealth();
        this.mc.mcProfiler.endSection();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(ICONS);

        if (this.mc.playerController.shouldDrawHUD())
        {
            this.renderPlayerStats(scaledresolution);
        }

        this.renderMountHealth(scaledresolution);
        GlStateManager.disableBlend();

        if (this.mc.player.getSleepTimer() > 0)
        {
            this.mc.mcProfiler.startSection("sleep");
            GlStateManager.disableDepth();
            GlStateManager.disableAlpha();
            int j1 = this.mc.player.getSleepTimer();
            float f1 = (float)j1 / 100.0F;

            if (f1 > 1.0F)
            {
                f1 = 1.0F - (float)(j1 - 100) / 10.0F;
            }

            int k = (int)(220.0F * f1) << 24 | 1052704;
            drawRect(0, 0, i, j, k);
            GlStateManager.enableAlpha();
            GlStateManager.enableDepth();
            this.mc.mcProfiler.endSection();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int k1 = i / 2 - 91;

        if (this.mc.player.isRidingHorse())
        {
            this.renderHorseJumpBar(scaledresolution, k1);
        }
        else if (this.mc.playerController.gameIsSurvivalOrAdventure())
        {
            this.renderExpBar(scaledresolution, k1);
        }

        if (this.mc.gameSettings.heldItemTooltips && !this.mc.playerController.isSpectator())
        {
            this.renderSelectedItem(scaledresolution);
        }
        else if (this.mc.player.isSpectator())
        {
            this.spectatorGui.renderSelectedItem(scaledresolution);
        }

        if (this.mc.isDemo())
        {
            this.renderDemo(scaledresolution);
        }

        this.renderPotionEffects(scaledresolution);

        if (this.mc.gameSettings.showDebugInfo)
        {
            this.overlayDebug.renderDebugInfo(scaledresolution);
        }
        else
        {
            this.renderArgonHud(scaledresolution, fontrenderer);
        }

        if (this.overlayMessageTime > 0)
        {
            this.mc.mcProfiler.startSection("overlayMessage");
            float f2 = (float)this.overlayMessageTime - partialTicks;
            int l1 = (int)(f2 * 255.0F / 20.0F);

            if (l1 > 255)
            {
                l1 = 255;
            }

            if (l1 > 8)
            {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float)(i / 2), (float)(j - 68), 0.0F);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                int l = 16777215;

                if (this.animateOverlayMessageColor)
                {
                    l = MathHelper.hsvToRGB(f2 / 50.0F, 0.7F, 0.6F) & 16777215;
                }

                fontrenderer.drawString(this.overlayMessage, -fontrenderer.getStringWidth(this.overlayMessage) / 2, -4, l + (l1 << 24 & -16777216));
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }

            this.mc.mcProfiler.endSection();
        }

        this.overlaySubtitle.renderSubtitles(scaledresolution);

        if (this.titlesTimer > 0)
        {
            this.mc.mcProfiler.startSection("titleAndSubtitle");
            float f3 = (float)this.titlesTimer - partialTicks;
            int i2 = 255;

            if (this.titlesTimer > this.titleFadeOut + this.titleDisplayTime)
            {
                float f4 = (float)(this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut) - f3;
                i2 = (int)(f4 * 255.0F / (float)this.titleFadeIn);
            }

            if (this.titlesTimer <= this.titleFadeOut)
            {
                i2 = (int)(f3 * 255.0F / (float)this.titleFadeOut);
            }

            i2 = MathHelper.clamp(i2, 0, 255);

            if (i2 > 8)
            {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float)(i / 2), (float)(j / 2), 0.0F);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.pushMatrix();
                GlStateManager.scale(4.0F, 4.0F, 4.0F);
                int j2 = i2 << 24 & -16777216;
                fontrenderer.drawString(this.displayedTitle, (float)(-fontrenderer.getStringWidth(this.displayedTitle) / 2), -10.0F, 16777215 | j2, true);
                GlStateManager.popMatrix();
                GlStateManager.pushMatrix();
                GlStateManager.scale(2.0F, 2.0F, 2.0F);
                fontrenderer.drawString(this.displayedSubTitle, (float)(-fontrenderer.getStringWidth(this.displayedSubTitle) / 2), 5.0F, 16777215 | j2, true);
                GlStateManager.popMatrix();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }

            this.mc.mcProfiler.endSection();
        }

        Scoreboard scoreboard = this.mc.world.getScoreboard();
        ScoreObjective scoreobjective = null;
        ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(this.mc.player.getName());

        if (scoreplayerteam != null)
        {
            int i1 = scoreplayerteam.getColor().getColorIndex();

            if (i1 >= 0)
            {
                scoreobjective = scoreboard.getObjectiveInDisplaySlot(3 + i1);
            }
        }

        ScoreObjective scoreobjective1 = scoreobjective != null ? scoreobjective : scoreboard.getObjectiveInDisplaySlot(1);

        if (scoreobjective1 != null)
        {
            this.renderScoreboard(scoreobjective1, scaledresolution);
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableAlpha();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, (float)(j - 48), 0.0F);
        this.mc.mcProfiler.startSection("chat");
        this.persistantChatGUI.drawChat(this.updateCounter);
        this.mc.mcProfiler.endSection();
        GlStateManager.popMatrix();
        scoreobjective1 = scoreboard.getObjectiveInDisplaySlot(0);

        if (this.mc.gameSettings.keyBindPlayerList.isKeyDown() && (!this.mc.isIntegratedServerRunning() || this.mc.player.connection.getPlayerInfoMap().size() > 1 || scoreobjective1 != null))
        {
            this.overlayPlayerList.updatePlayerList(true);
            this.overlayPlayerList.renderPlayerlist(i, scoreboard, scoreobjective1);
        }
        else
        {
            this.overlayPlayerList.updatePlayerList(false);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
    }

    private void renderAttackIndicator(float p_184045_1_, ScaledResolution p_184045_2_)
    {
        GameSettings gamesettings = this.mc.gameSettings;

        if (gamesettings.thirdPersonView == 0)
        {
            if (this.mc.playerController.isSpectator() && this.mc.pointedEntity == null)
            {
                RayTraceResult raytraceresult = this.mc.objectMouseOver;

                if (raytraceresult == null || raytraceresult.typeOfHit != RayTraceResult.Type.BLOCK)
                {
                    return;
                }

                BlockPos blockpos = raytraceresult.getBlockPos();
                IBlockState iblockstate = this.mc.world.getBlockState(blockpos);

                if (!ReflectorForge.blockHasTileEntity(iblockstate) || !(this.mc.world.getTileEntity(blockpos) instanceof IInventory))
                {
                    return;
                }
            }

            int l = p_184045_2_.getScaledWidth();
            int i1 = p_184045_2_.getScaledHeight();

            if (gamesettings.showDebugInfo && !gamesettings.hideGUI && !this.mc.player.hasReducedDebug() && !gamesettings.reducedDebugInfo)
            {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float)(l / 2), (float)(i1 / 2), this.zLevel);
                Entity entity = this.mc.getRenderViewEntity();
                GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * p_184045_1_, -1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * p_184045_1_, 0.0F, 1.0F, 0.0F);
                GlStateManager.scale(-1.0F, -1.0F, -1.0F);
                OpenGlHelper.renderDirections(10);
                GlStateManager.popMatrix();
            }
            else
            {
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.enableAlpha();
                this.drawTexturedModalRect(l / 2 - 7, i1 / 2 - 7, 0, 0, 16, 16);

                if (this.mc.gameSettings.attackIndicator == 1)
                {
                    float f = this.mc.player.getCooledAttackStrength(0.0F);
                    boolean flag = false;

                    if (this.mc.pointedEntity != null && this.mc.pointedEntity instanceof EntityLivingBase && f >= 1.0F)
                    {
                        flag = this.mc.player.getCooldownPeriod() > 5.0F;
                        flag = flag & ((EntityLivingBase)this.mc.pointedEntity).isEntityAlive();
                    }

                    int i = i1 / 2 - 7 + 16;
                    int j = l / 2 - 8;

                    if (flag)
                    {
                        this.drawTexturedModalRect(j, i, 68, 94, 16, 16);
                    }
                    else if (f < 1.0F)
                    {
                        int k = (int)(f * 17.0F);
                        this.drawTexturedModalRect(j, i, 36, 94, 16, 4);
                        this.drawTexturedModalRect(j, i, 52, 94, k, 4);
                    }
                }
            }
        }
    }

    protected void renderPotionEffects(ScaledResolution resolution)
    {
        Collection<PotionEffect> collection = this.mc.player.getActivePotionEffects();

        if (!collection.isEmpty())
        {
            this.mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
            GlStateManager.enableBlend();
            int i = 0;
            int j = 0;
            Iterator iterator = Ordering.natural().reverse().sortedCopy(collection).iterator();

            while (true)
            {
                PotionEffect potioneffect;
                Potion potion;
                boolean flag;

                while (true)
                {
                    if (!iterator.hasNext())
                    {
                        return;
                    }

                    potioneffect = (PotionEffect)iterator.next();
                    potion = potioneffect.getPotion();
                    flag = potion.hasStatusIcon();

                    if (!Reflector.ForgePotion_shouldRenderHUD.exists())
                    {
                        break;
                    }

                    if (Reflector.callBoolean(potion, Reflector.ForgePotion_shouldRenderHUD, potioneffect))
                    {
                        this.mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
                        flag = true;
                        break;
                    }
                }

                if (flag && potioneffect.doesShowParticles())
                {
                    int k = resolution.getScaledWidth();
                    int l = 1;

                    if (this.mc.isDemo())
                    {
                        l += 15;
                    }

                    int i1 = potion.getStatusIconIndex();

                    if (potion.isBeneficial())
                    {
                        ++i;
                        k = k - 25 * i;
                    }
                    else
                    {
                        ++j;
                        k = k - 25 * j;
                        l += 26;
                    }

                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    float f = 1.0F;

                    if (potioneffect.getIsAmbient())
                    {
                        this.drawTexturedModalRect(k, l, 165, 166, 24, 24);
                    }
                    else
                    {
                        this.drawTexturedModalRect(k, l, 141, 166, 24, 24);

                        if (potioneffect.getDuration() <= 200)
                        {
                            int j1 = 10 - potioneffect.getDuration() / 20;
                            f = MathHelper.clamp((float)potioneffect.getDuration() / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F) + MathHelper.cos((float)potioneffect.getDuration() * (float)Math.PI / 5.0F) * MathHelper.clamp((float)j1 / 10.0F * 0.25F, 0.0F, 0.25F);
                        }
                    }

                    GlStateManager.color(1.0F, 1.0F, 1.0F, f);

                    if (Reflector.ForgePotion_renderHUDEffect.exists())
                    {
                        if (potion.hasStatusIcon())
                        {
                            this.drawTexturedModalRect(k + 3, l + 3, i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18);
                        }

                        Reflector.call(potion, Reflector.ForgePotion_renderHUDEffect, k, l, potioneffect, this.mc, f);
                    }
                    else
                    {
                        this.drawTexturedModalRect(k + 3, l + 3, i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18);
                    }
                }
            }
        }
    }

    protected void renderHotbar(ScaledResolution sr, float partialTicks)
    {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer)
        {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(WIDGETS_TEX_PATH);
            EntityPlayer entityplayer = (EntityPlayer)this.mc.getRenderViewEntity();
            ItemStack itemstack = entityplayer.getHeldItemOffhand();
            EnumHandSide enumhandside = entityplayer.getPrimaryHand().opposite();
            int i = sr.getScaledWidth() / 2;
            float f = this.zLevel;
            int j = 182;
            int k = 91;
            this.zLevel = -90.0F;
            this.drawTexturedModalRect(i - 91, sr.getScaledHeight() - 22, 0, 0, 182, 22);
            this.drawTexturedModalRect(i - 91 - 1 + entityplayer.inventory.currentItem * 20, sr.getScaledHeight() - 22 - 1, 0, 22, 24, 22);

            if (!itemstack.isEmpty())
            {
                if (enumhandside == EnumHandSide.LEFT)
                {
                    this.drawTexturedModalRect(i - 91 - 29, sr.getScaledHeight() - 23, 24, 22, 29, 24);
                }
                else
                {
                    this.drawTexturedModalRect(i + 91, sr.getScaledHeight() - 23, 53, 22, 29, 24);
                }
            }

            this.zLevel = f;
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderHelper.enableGUIStandardItemLighting();
            CustomItems.setRenderOffHand(false);

            for (int l = 0; l < 9; ++l)
            {
                int i1 = i - 90 + l * 20 + 2;
                int j1 = sr.getScaledHeight() - 16 - 3;
                this.renderHotbarItem(i1, j1, partialTicks, entityplayer, entityplayer.inventory.mainInventory.get(l));
            }

            if (!itemstack.isEmpty())
            {
                CustomItems.setRenderOffHand(true);
                int l1 = sr.getScaledHeight() - 16 - 3;

                if (enumhandside == EnumHandSide.LEFT)
                {
                    this.renderHotbarItem(i - 91 - 26, l1, partialTicks, entityplayer, itemstack);
                }
                else
                {
                    this.renderHotbarItem(i + 91 + 10, l1, partialTicks, entityplayer, itemstack);
                }

                CustomItems.setRenderOffHand(false);
            }

            if (this.mc.gameSettings.attackIndicator == 2)
            {
                float f1 = this.mc.player.getCooledAttackStrength(0.0F);

                if (f1 < 1.0F)
                {
                    int i2 = sr.getScaledHeight() - 20;
                    int j2 = i + 91 + 6;

                    if (enumhandside == EnumHandSide.RIGHT)
                    {
                        j2 = i - 91 - 22;
                    }

                    this.mc.getTextureManager().bindTexture(Gui.ICONS);
                    int k1 = (int)(f1 * 19.0F);
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    this.drawTexturedModalRect(j2, i2, 0, 94, 18, 18);
                    this.drawTexturedModalRect(j2, i2 + 18 - k1, 18, 112 - k1, 18, k1);
                }
            }

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
        }
    }

    public void renderHorseJumpBar(ScaledResolution scaledRes, int x)
    {
        this.mc.mcProfiler.startSection("jumpBar");
        this.mc.getTextureManager().bindTexture(Gui.ICONS);
        float f = this.mc.player.getHorseJumpPower();
        int i = 182;
        int j = (int)(f * 183.0F);
        int k = scaledRes.getScaledHeight() - 32 + 3;
        this.drawTexturedModalRect(x, k, 0, 84, 182, 5);

        if (j > 0)
        {
            this.drawTexturedModalRect(x, k, 0, 89, j, 5);
        }

        this.mc.mcProfiler.endSection();
    }

    public void renderExpBar(ScaledResolution scaledRes, int x)
    {
        this.mc.mcProfiler.startSection("expBar");
        this.mc.getTextureManager().bindTexture(Gui.ICONS);
        int i = this.mc.player.xpBarCap();

        if (i > 0)
        {
            int j = 182;
            int k = (int)(this.mc.player.experience * 183.0F);
            int l = scaledRes.getScaledHeight() - 32 + 3;
            this.drawTexturedModalRect(x, l, 0, 64, 182, 5);

            if (k > 0)
            {
                this.drawTexturedModalRect(x, l, 0, 69, k, 5);
            }
        }

        this.mc.mcProfiler.endSection();

        if (this.mc.player.experienceLevel > 0)
        {
            this.mc.mcProfiler.startSection("expLevel");
            int j1 = 8453920;

            if (Config.isCustomColors())
            {
                j1 = CustomColors.getExpBarTextColor(j1);
            }

            String s = "" + this.mc.player.experienceLevel;
            int k1 = (scaledRes.getScaledWidth() - this.getFontRenderer().getStringWidth(s)) / 2;
            int i1 = scaledRes.getScaledHeight() - 31 - 4;
            this.getFontRenderer().drawString(s, k1 + 1, i1, 0);
            this.getFontRenderer().drawString(s, k1 - 1, i1, 0);
            this.getFontRenderer().drawString(s, k1, i1 + 1, 0);
            this.getFontRenderer().drawString(s, k1, i1 - 1, 0);
            this.getFontRenderer().drawString(s, k1, i1, j1);
            this.mc.mcProfiler.endSection();
        }
    }

    public void renderSelectedItem(ScaledResolution scaledRes)
    {
        this.mc.mcProfiler.startSection("selectedItemName");

        if (this.remainingHighlightTicks > 0 && !this.highlightingItemStack.isEmpty())
        {
            String s = this.highlightingItemStack.getDisplayName();

            if (this.highlightingItemStack.hasDisplayName())
            {
                s = TextFormatting.ITALIC + s;
            }

            int i = (scaledRes.getScaledWidth() - this.getFontRenderer().getStringWidth(s)) / 2;
            int j = scaledRes.getScaledHeight() - 59;

            if (!this.mc.playerController.shouldDrawHUD())
            {
                j += 14;
            }

            int k = (int)((float)this.remainingHighlightTicks * 256.0F / 10.0F);

            if (k > 255)
            {
                k = 255;
            }

            if (k > 0)
            {
                GlStateManager.pushMatrix();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                this.getFontRenderer().drawStringWithShadow(s, (float)i, (float)j, 16777215 + (k << 24));
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        }

        this.mc.mcProfiler.endSection();
    }

    public void renderDemo(ScaledResolution scaledRes)
    {
        this.mc.mcProfiler.startSection("demo");
        String s;

        if (this.mc.world.getTotalWorldTime() >= 120500L)
        {
            s = I18n.format("demo.demoExpired");
        }
        else
        {
            s = I18n.format("demo.remainingTime", StringUtils.ticksToElapsedTime((int)(120500L - this.mc.world.getTotalWorldTime())));
        }

        int i = this.getFontRenderer().getStringWidth(s);
        this.getFontRenderer().drawStringWithShadow(s, (float)(scaledRes.getScaledWidth() - i - 10), 5.0F, 16777215);
        this.mc.mcProfiler.endSection();
    }

    private void renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes)
    {
        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> collection = scoreboard.getSortedScores(objective);
        List<Score> list = Lists.newArrayList(Iterables.filter(collection, new Predicate<Score>()
        {
            public boolean apply(@Nullable Score p_apply_1_)
            {
                return p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#");
            }
        }));

        if (list.size() > 15)
        {
            collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
        }
        else
        {
            collection = list;
        }

        int i = this.getFontRenderer().getStringWidth(objective.getDisplayName());

        for (Score score : collection)
        {
            ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
            String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + TextFormatting.RED + score.getScorePoints();
            i = Math.max(i, this.getFontRenderer().getStringWidth(s));
        }

        int i1 = collection.size() * this.getFontRenderer().FONT_HEIGHT;
        int j1 = scaledRes.getScaledHeight() / 2 + i1 / 3;
        int k1 = 3;
        int l1 = scaledRes.getScaledWidth() - i - 3;
        int j = 0;

        for (Score score1 : collection)
        {
            ++j;
            ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
            String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
            String s2 = TextFormatting.RED + "" + score1.getScorePoints();
            int k = j1 - j * this.getFontRenderer().FONT_HEIGHT;
            int l = scaledRes.getScaledWidth() - 3 + 2;
            drawRect(l1 - 2, k, l, k + this.getFontRenderer().FONT_HEIGHT, 1342177280);
            this.getFontRenderer().drawString(s1, l1, k, 553648127);
            this.getFontRenderer().drawString(s2, l - this.getFontRenderer().getStringWidth(s2), k, 553648127);

            if (j == collection.size())
            {
                String s3 = objective.getDisplayName();
                drawRect(l1 - 2, k - this.getFontRenderer().FONT_HEIGHT - 1, l, k - 1, 1610612736);
                drawRect(l1 - 2, k - 1, l, k, 1342177280);
                this.getFontRenderer().drawString(s3, l1 + i / 2 - this.getFontRenderer().getStringWidth(s3) / 2, k - this.getFontRenderer().FONT_HEIGHT, 553648127);
            }
        }
    }

    private void renderPlayerStats(ScaledResolution scaledRes)
    {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer)
        {
            EntityPlayer entityplayer = (EntityPlayer)this.mc.getRenderViewEntity();
            int i = MathHelper.ceil(entityplayer.getHealth());
            boolean flag = this.healthUpdateCounter > (long)this.updateCounter && (this.healthUpdateCounter - (long)this.updateCounter) / 3L % 2L == 1L;

            if (i < this.playerHealth && entityplayer.hurtResistantTime > 0)
            {
                this.lastSystemTime = Minecraft.getSystemTime();
                this.healthUpdateCounter = (long)(this.updateCounter + 20);
            }
            else if (i > this.playerHealth && entityplayer.hurtResistantTime > 0)
            {
                this.lastSystemTime = Minecraft.getSystemTime();
                this.healthUpdateCounter = (long)(this.updateCounter + 10);
            }

            if (Minecraft.getSystemTime() - this.lastSystemTime > 1000L)
            {
                this.playerHealth = i;
                this.lastPlayerHealth = i;
                this.lastSystemTime = Minecraft.getSystemTime();
            }

            this.playerHealth = i;
            int j = this.lastPlayerHealth;
            this.rand.setSeed((long)(this.updateCounter * 312871));
            FoodStats foodstats = entityplayer.getFoodStats();
            int k = foodstats.getFoodLevel();
            IAttributeInstance iattributeinstance = entityplayer.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
            int l = scaledRes.getScaledWidth() / 2 - 91;
            int i1 = scaledRes.getScaledWidth() / 2 + 91;
            int j1 = scaledRes.getScaledHeight() - 39;
            float f = (float)iattributeinstance.getAttributeValue();
            int k1 = MathHelper.ceil(entityplayer.getAbsorptionAmount());
            int l1 = MathHelper.ceil((f + (float)k1) / 2.0F / 10.0F);
            int i2 = Math.max(10 - (l1 - 2), 3);
            int j2 = j1 - (l1 - 1) * i2 - 10;
            int k2 = j1 - 10;
            int l2 = k1;
            int i3 = entityplayer.getTotalArmorValue();
            int j3 = -1;

            if (entityplayer.isPotionActive(MobEffects.REGENERATION))
            {
                j3 = this.updateCounter % MathHelper.ceil(f + 5.0F);
            }

            this.mc.mcProfiler.startSection("armor");

            for (int k3 = 0; k3 < 10; ++k3)
            {
                if (i3 > 0)
                {
                    int l3 = l + k3 * 8;

                    if (k3 * 2 + 1 < i3)
                    {
                        this.drawTexturedModalRect(l3, j2, 34, 9, 9, 9);
                    }

                    if (k3 * 2 + 1 == i3)
                    {
                        this.drawTexturedModalRect(l3, j2, 25, 9, 9, 9);
                    }

                    if (k3 * 2 + 1 > i3)
                    {
                        this.drawTexturedModalRect(l3, j2, 16, 9, 9, 9);
                    }
                }
            }

            this.mc.mcProfiler.endStartSection("health");

            for (int j5 = MathHelper.ceil((f + (float)k1) / 2.0F) - 1; j5 >= 0; --j5)
            {
                int k5 = 16;

                if (entityplayer.isPotionActive(MobEffects.POISON))
                {
                    k5 += 36;
                }
                else if (entityplayer.isPotionActive(MobEffects.WITHER))
                {
                    k5 += 72;
                }

                int i4 = 0;

                if (flag)
                {
                    i4 = 1;
                }

                int j4 = MathHelper.ceil((float)(j5 + 1) / 10.0F) - 1;
                int k4 = l + j5 % 10 * 8;
                int l4 = j1 - j4 * i2;

                if (i <= 4)
                {
                    l4 += this.rand.nextInt(2);
                }

                if (l2 <= 0 && j5 == j3)
                {
                    l4 -= 2;
                }

                int i5 = 0;

                if (entityplayer.world.getWorldInfo().isHardcoreModeEnabled())
                {
                    i5 = 5;
                }

                this.drawTexturedModalRect(k4, l4, 16 + i4 * 9, 9 * i5, 9, 9);

                if (flag)
                {
                    if (j5 * 2 + 1 < j)
                    {
                        this.drawTexturedModalRect(k4, l4, k5 + 54, 9 * i5, 9, 9);
                    }

                    if (j5 * 2 + 1 == j)
                    {
                        this.drawTexturedModalRect(k4, l4, k5 + 63, 9 * i5, 9, 9);
                    }
                }

                if (l2 > 0)
                {
                    if (l2 == k1 && k1 % 2 == 1)
                    {
                        this.drawTexturedModalRect(k4, l4, k5 + 153, 9 * i5, 9, 9);
                        --l2;
                    }
                    else
                    {
                        this.drawTexturedModalRect(k4, l4, k5 + 144, 9 * i5, 9, 9);
                        l2 -= 2;
                    }
                }
                else
                {
                    if (j5 * 2 + 1 < i)
                    {
                        this.drawTexturedModalRect(k4, l4, k5 + 36, 9 * i5, 9, 9);
                    }

                    if (j5 * 2 + 1 == i)
                    {
                        this.drawTexturedModalRect(k4, l4, k5 + 45, 9 * i5, 9, 9);
                    }
                }
            }

            Entity entity = entityplayer.getRidingEntity();

            if (entity == null || !(entity instanceof EntityLivingBase))
            {
                this.mc.mcProfiler.endStartSection("food");

                for (int l5 = 0; l5 < 10; ++l5)
                {
                    int j6 = j1;
                    int l6 = 16;
                    int j7 = 0;

                    if (entityplayer.isPotionActive(MobEffects.HUNGER))
                    {
                        l6 += 36;
                        j7 = 13;
                    }

                    if (entityplayer.getFoodStats().getSaturationLevel() <= 0.0F && this.updateCounter % (k * 3 + 1) == 0)
                    {
                        j6 = j1 + (this.rand.nextInt(3) - 1);
                    }

                    int l7 = i1 - l5 * 8 - 9;
                    this.drawTexturedModalRect(l7, j6, 16 + j7 * 9, 27, 9, 9);

                    if (l5 * 2 + 1 < k)
                    {
                        this.drawTexturedModalRect(l7, j6, l6 + 36, 27, 9, 9);
                    }

                    if (l5 * 2 + 1 == k)
                    {
                        this.drawTexturedModalRect(l7, j6, l6 + 45, 27, 9, 9);
                    }
                }
            }

            this.mc.mcProfiler.endStartSection("air");

            if (entityplayer.isInsideOfMaterial(Material.WATER))
            {
                int i6 = this.mc.player.getAir();
                int k6 = MathHelper.ceil((double)(i6 - 2) * 10.0D / 300.0D);
                int i7 = MathHelper.ceil((double)i6 * 10.0D / 300.0D) - k6;

                for (int k7 = 0; k7 < k6 + i7; ++k7)
                {
                    if (k7 < k6)
                    {
                        this.drawTexturedModalRect(i1 - k7 * 8 - 9, k2, 16, 18, 9, 9);
                    }
                    else
                    {
                        this.drawTexturedModalRect(i1 - k7 * 8 - 9, k2, 25, 18, 9, 9);
                    }
                }
            }

            this.mc.mcProfiler.endSection();
        }
    }

    private void renderMountHealth(ScaledResolution p_184047_1_)
    {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer)
        {
            EntityPlayer entityplayer = (EntityPlayer)this.mc.getRenderViewEntity();
            Entity entity = entityplayer.getRidingEntity();

            if (entity instanceof EntityLivingBase)
            {
                this.mc.mcProfiler.endStartSection("mountHealth");
                EntityLivingBase entitylivingbase = (EntityLivingBase)entity;
                int i = (int)Math.ceil((double)entitylivingbase.getHealth());
                float f = entitylivingbase.getMaxHealth();
                int j = (int)(f + 0.5F) / 2;

                if (j > 30)
                {
                    j = 30;
                }

                int k = p_184047_1_.getScaledHeight() - 39;
                int l = p_184047_1_.getScaledWidth() / 2 + 91;
                int i1 = k;
                int j1 = 0;

                for (boolean flag = false; j > 0; j1 += 20)
                {
                    int k1 = Math.min(j, 10);
                    j -= k1;

                    for (int l1 = 0; l1 < k1; ++l1)
                    {
                        int i2 = 52;
                        int j2 = 0;
                        int k2 = l - l1 * 8 - 9;
                        this.drawTexturedModalRect(k2, i1, 52 + j2 * 9, 9, 9, 9);

                        if (l1 * 2 + 1 + j1 < i)
                        {
                            this.drawTexturedModalRect(k2, i1, 88, 9, 9, 9);
                        }

                        if (l1 * 2 + 1 + j1 == i)
                        {
                            this.drawTexturedModalRect(k2, i1, 97, 9, 9, 9);
                        }
                    }

                    i1 -= 10;
                }
            }
        }
    }

    private void renderPumpkinOverlay(ScaledResolution scaledRes)
    {
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableAlpha();
        this.mc.getTextureManager().bindTexture(PUMPKIN_BLUR_TEX_PATH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(0.0D, (double)scaledRes.getScaledHeight(), -90.0D).tex(0.0D, 1.0D).endVertex();
        bufferbuilder.pos((double)scaledRes.getScaledWidth(), (double)scaledRes.getScaledHeight(), -90.0D).tex(1.0D, 1.0D).endVertex();
        bufferbuilder.pos((double)scaledRes.getScaledWidth(), 0.0D, -90.0D).tex(1.0D, 0.0D).endVertex();
        bufferbuilder.pos(0.0D, 0.0D, -90.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Renders a Vignette arount the entire screen that changes with light level.
     */
    private void renderVignette(float lightLevel, ScaledResolution scaledRes)
    {
        if (!Config.isVignetteEnabled())
        {
            GlStateManager.enableDepth();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }
        else
        {
            lightLevel = 1.0F - lightLevel;
            lightLevel = MathHelper.clamp(lightLevel, 0.0F, 1.0F);
            WorldBorder worldborder = this.mc.world.getWorldBorder();
            float f = (float)worldborder.getClosestDistance(this.mc.player);
            double d0 = Math.min(worldborder.getResizeSpeed() * (double)worldborder.getWarningTime() * 1000.0D, Math.abs(worldborder.getTargetSize() - worldborder.getDiameter()));
            double d1 = Math.max((double)worldborder.getWarningDistance(), d0);

            if ((double)f < d1)
            {
                f = 1.0F - (float)((double)f / d1);
            }
            else
            {
                f = 0.0F;
            }

            this.prevVignetteBrightness = (float)((double)this.prevVignetteBrightness + (double)(lightLevel - this.prevVignetteBrightness) * 0.01D);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

            if (f > 0.0F)
            {
                GlStateManager.color(0.0F, f, f, 1.0F);
            }
            else
            {
                GlStateManager.color(this.prevVignetteBrightness, this.prevVignetteBrightness, this.prevVignetteBrightness, 1.0F);
            }

            this.mc.getTextureManager().bindTexture(VIGNETTE_TEX_PATH);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos(0.0D, (double)scaledRes.getScaledHeight(), -90.0D).tex(0.0D, 1.0D).endVertex();
            bufferbuilder.pos((double)scaledRes.getScaledWidth(), (double)scaledRes.getScaledHeight(), -90.0D).tex(1.0D, 1.0D).endVertex();
            bufferbuilder.pos((double)scaledRes.getScaledWidth(), 0.0D, -90.0D).tex(1.0D, 0.0D).endVertex();
            bufferbuilder.pos(0.0D, 0.0D, -90.0D).tex(0.0D, 0.0D).endVertex();
            tessellator.draw();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }
    }

    private void renderEnhancedVisuals(ScaledResolution scaledRes, float partialTicks)
    {
        if (this.mc.player == null)
        {
            return;
        }

        float dmgIntensity = this.mc.player.prevEnhancedDamageIntensity + (this.mc.player.enhancedDamageIntensity - this.mc.player.prevEnhancedDamageIntensity) * partialTicks;
        float expIntensity = this.mc.player.prevEnhancedExplosionIntensity + (this.mc.player.enhancedExplosionIntensity - this.mc.player.prevEnhancedExplosionIntensity) * partialTicks;
        dmgIntensity = MathHelper.clamp(dmgIntensity, 0.0F, 1.0F);
        expIntensity = MathHelper.clamp(expIntensity, 0.0F, 1.0F);

        if (dmgIntensity <= 0.005F && expIntensity <= 0.005F)
        {
            return;
        }

        double width = scaledRes.getScaledWidth_double();
        double height = scaledRes.getScaledHeight_double();
        float time = ((float)this.mc.player.ticksExisted + partialTicks) * 0.65F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();

        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();

        float blurStrength = MathHelper.clamp(expIntensity * 0.9F + Math.max(0.0F, dmgIntensity - 0.25F) * 0.45F, 0.0F, 1.0F);

        if (blurStrength > 0.01F && OpenGlHelper.isFramebufferEnabled() && this.mc.getFramebuffer() != null)
        {
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            this.mc.getFramebuffer().bindFramebufferTexture();

            double offset1X = (double)(MathHelper.sin(time * 2.4F) * 6.5F * blurStrength);
            double offset1Y = (double)(MathHelper.cos(time * 2.9F) * 5.0F * blurStrength);
            GlStateManager.color(1.0F, 0.92F, 0.92F, 0.36F * blurStrength);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos(offset1X, height + offset1Y, -90.0D).tex(0.0D, 0.0D).endVertex();
            bufferbuilder.pos(width + offset1X, height + offset1Y, -90.0D).tex(1.0D, 0.0D).endVertex();
            bufferbuilder.pos(width + offset1X, offset1Y, -90.0D).tex(1.0D, 1.0D).endVertex();
            bufferbuilder.pos(offset1X, offset1Y, -90.0D).tex(0.0D, 1.0D).endVertex();
            tessellator.draw();

            double offset2X = -offset1X * 0.85D;
            double offset2Y = -offset1Y * 0.85D;
            GlStateManager.color(1.0F, 0.86F, 0.86F, 0.26F * blurStrength);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos(offset2X, height + offset2Y, -90.0D).tex(0.0D, 0.0D).endVertex();
            bufferbuilder.pos(width + offset2X, height + offset2Y, -90.0D).tex(1.0D, 0.0D).endVertex();
            bufferbuilder.pos(width + offset2X, offset2Y, -90.0D).tex(1.0D, 1.0D).endVertex();
            bufferbuilder.pos(offset2X, offset2Y, -90.0D).tex(0.0D, 1.0D).endVertex();
            tessellator.draw();
        }

        float vignettePower = MathHelper.clamp(dmgIntensity * 0.92F + expIntensity * 0.45F, 0.0F, 0.95F);

        if (vignettePower > 0.01F)
        {
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.color(0.0F, vignettePower, vignettePower, 1.0F);
            this.mc.getTextureManager().bindTexture(VIGNETTE_TEX_PATH);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos(0.0D, height, -90.0D).tex(0.0D, 1.0D).endVertex();
            bufferbuilder.pos(width, height, -90.0D).tex(1.0D, 1.0D).endVertex();
            bufferbuilder.pos(width, 0.0D, -90.0D).tex(1.0D, 0.0D).endVertex();
            bufferbuilder.pos(0.0D, 0.0D, -90.0D).tex(0.0D, 0.0D).endVertex();
            tessellator.draw();

            GlStateManager.disableTexture2D();
            GlStateManager.shadeModel(7425);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            float borderAlpha = MathHelper.clamp(vignettePower * 0.58F, 0.0F, 0.65F);
            double borderW = Math.min(width, height) * (0.14D + (double)vignettePower * 0.10D);

            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
            // Top edge blood gradient
            bufferbuilder.pos(0.0D, borderW, -90.0D).color(0.65F, 0.02F, 0.02F, 0.0F).endVertex();
            bufferbuilder.pos(width, borderW, -90.0D).color(0.65F, 0.02F, 0.02F, 0.0F).endVertex();
            bufferbuilder.pos(width, 0.0D, -90.0D).color(0.55F, 0.0F, 0.0F, borderAlpha).endVertex();
            bufferbuilder.pos(0.0D, 0.0D, -90.0D).color(0.55F, 0.0F, 0.0F, borderAlpha).endVertex();
            // Bottom edge blood gradient
            bufferbuilder.pos(0.0D, height, -90.0D).color(0.55F, 0.0F, 0.0F, borderAlpha).endVertex();
            bufferbuilder.pos(width, height, -90.0D).color(0.55F, 0.0F, 0.0F, borderAlpha).endVertex();
            bufferbuilder.pos(width, height - borderW, -90.0D).color(0.65F, 0.02F, 0.02F, 0.0F).endVertex();
            bufferbuilder.pos(0.0D, height - borderW, -90.0D).color(0.65F, 0.02F, 0.02F, 0.0F).endVertex();
            // Left edge blood gradient
            bufferbuilder.pos(0.0D, height, -90.0D).color(0.55F, 0.0F, 0.0F, borderAlpha).endVertex();
            bufferbuilder.pos(borderW, height, -90.0D).color(0.65F, 0.02F, 0.02F, 0.0F).endVertex();
            bufferbuilder.pos(borderW, 0.0D, -90.0D).color(0.65F, 0.02F, 0.02F, 0.0F).endVertex();
            bufferbuilder.pos(0.0D, 0.0D, -90.0D).color(0.55F, 0.0F, 0.0F, borderAlpha).endVertex();
            // Right edge blood gradient
            bufferbuilder.pos(width - borderW, height, -90.0D).color(0.65F, 0.02F, 0.02F, 0.0F).endVertex();
            bufferbuilder.pos(width, height, -90.0D).color(0.55F, 0.0F, 0.0F, borderAlpha).endVertex();
            bufferbuilder.pos(width, 0.0D, -90.0D).color(0.55F, 0.0F, 0.0F, borderAlpha).endVertex();
            bufferbuilder.pos(width - borderW, 0.0D, -90.0D).color(0.65F, 0.02F, 0.02F, 0.0F).endVertex();

            // Blood-splatter droplets around screen perimeter
            int seed = this.mc.player.enhancedVisualsSeed;
            float splatterAlpha = MathHelper.clamp(vignettePower * 0.52F, 0.0F, 0.6F);

            for (int idx = 0; idx < 12; ++idx)
            {
                int hash = (seed + idx * 7919) * 1103515245 + 12345;
                float normA = (float)((hash >>> 8) & 255) / 255.0F;
                float normB = (float)((hash >>> 16) & 255) / 255.0F;
                double dropRadius = 6.0D + (double)((hash >>> 24) & 15) * 1.4D * (double)vignettePower;
                double cx;
                double cy;
                int edge = idx & 3;

                if (edge == 0)
                {
                    cx = normA * width;
                    cy = normB * borderW * 0.85D;
                }
                else if (edge == 1)
                {
                    cx = normA * width;
                    cy = height - normB * borderW * 0.85D;
                }
                else if (edge == 2)
                {
                    cx = normB * borderW * 0.85D;
                    cy = normA * height;
                }
                else
                {
                    cx = width - normB * borderW * 0.85D;
                    cy = normA * height;
                }

                bufferbuilder.pos(cx, cy + dropRadius, -90.0D).color(0.48F, 0.0F, 0.0F, splatterAlpha * 0.85F).endVertex();
                bufferbuilder.pos(cx + dropRadius, cy, -90.0D).color(0.58F, 0.02F, 0.02F, splatterAlpha * 0.7F).endVertex();
                bufferbuilder.pos(cx, cy - dropRadius, -90.0D).color(0.48F, 0.0F, 0.0F, splatterAlpha * 0.85F).endVertex();
                bufferbuilder.pos(cx - dropRadius, cy, -90.0D).color(0.58F, 0.02F, 0.02F, splatterAlpha * 0.7F).endVertex();
            }

            tessellator.draw();
            GlStateManager.shadeModel(7424);
            GlStateManager.enableTexture2D();
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    private void renderPortal(float timeInPortal, ScaledResolution scaledRes)
    {
        if (timeInPortal < 1.0F)
        {
            timeInPortal = timeInPortal * timeInPortal;
            timeInPortal = timeInPortal * timeInPortal;
            timeInPortal = timeInPortal * 0.8F + 0.2F;
        }

        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, timeInPortal);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite textureatlassprite = this.mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.PORTAL.getDefaultState());
        float f = textureatlassprite.getMinU();
        float f1 = textureatlassprite.getMinV();
        float f2 = textureatlassprite.getMaxU();
        float f3 = textureatlassprite.getMaxV();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(0.0D, (double)scaledRes.getScaledHeight(), -90.0D).tex((double)f, (double)f3).endVertex();
        bufferbuilder.pos((double)scaledRes.getScaledWidth(), (double)scaledRes.getScaledHeight(), -90.0D).tex((double)f2, (double)f3).endVertex();
        bufferbuilder.pos((double)scaledRes.getScaledWidth(), 0.0D, -90.0D).tex((double)f2, (double)f1).endVertex();
        bufferbuilder.pos(0.0D, 0.0D, -90.0D).tex((double)f, (double)f1).endVertex();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderHotbarItem(int p_184044_1_, int p_184044_2_, float p_184044_3_, EntityPlayer player, ItemStack stack)
    {
        if (!stack.isEmpty())
        {
            float f = (float)stack.getAnimationsToGo() - p_184044_3_;

            if (f > 0.0F)
            {
                GlStateManager.pushMatrix();
                float f1 = 1.0F + f / 5.0F;
                GlStateManager.translate((float)(p_184044_1_ + 8), (float)(p_184044_2_ + 12), 0.0F);
                GlStateManager.scale(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
                GlStateManager.translate((float)(-(p_184044_1_ + 8)), (float)(-(p_184044_2_ + 12)), 0.0F);
            }

            this.itemRenderer.renderItemAndEffectIntoGUI(player, stack, p_184044_1_, p_184044_2_);

            if (f > 0.0F)
            {
                GlStateManager.popMatrix();
            }

            this.itemRenderer.renderItemOverlays(this.mc.fontRenderer, stack, p_184044_1_, p_184044_2_);
        }
    }

    /**
     * The update tick for the ingame UI
     */
    public void updateTick()
    {
        if (this.overlayMessageTime > 0)
        {
            --this.overlayMessageTime;
        }

        if (this.titlesTimer > 0)
        {
            --this.titlesTimer;

            if (this.titlesTimer <= 0)
            {
                this.displayedTitle = "";
                this.displayedSubTitle = "";
            }
        }

        ++this.updateCounter;

        if (this.mc.player != null)
        {
            ItemStack itemstack = this.mc.player.inventory.getCurrentItem();

            if (itemstack.isEmpty())
            {
                this.remainingHighlightTicks = 0;
            }
            else if (!this.highlightingItemStack.isEmpty() && itemstack.getItem() == this.highlightingItemStack.getItem() && ItemStack.areItemStackTagsEqual(itemstack, this.highlightingItemStack) && (itemstack.isItemStackDamageable() || itemstack.getMetadata() == this.highlightingItemStack.getMetadata()))
            {
                if (this.remainingHighlightTicks > 0)
                {
                    --this.remainingHighlightTicks;
                }
            }
            else
            {
                this.remainingHighlightTicks = 40;
            }

            this.highlightingItemStack = itemstack;
        }
    }

    public void setRecordPlayingMessage(String recordName)
    {
        this.setOverlayMessage(I18n.format("record.nowPlaying", recordName), true);
    }

    public void setOverlayMessage(String message, boolean animateColor)
    {
        this.overlayMessage = message;
        this.overlayMessageTime = 60;
        this.animateOverlayMessageColor = animateColor;
    }

    public void displayTitle(String title, String subTitle, int timeFadeIn, int displayTime, int timeFadeOut)
    {
        if (title == null && subTitle == null && timeFadeIn < 0 && displayTime < 0 && timeFadeOut < 0)
        {
            this.displayedTitle = "";
            this.displayedSubTitle = "";
            this.titlesTimer = 0;
        }
        else if (title != null)
        {
            this.displayedTitle = title;
            this.titlesTimer = this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut;
        }
        else if (subTitle != null)
        {
            this.displayedSubTitle = subTitle;
        }
        else
        {
            if (timeFadeIn >= 0)
            {
                this.titleFadeIn = timeFadeIn;
            }

            if (displayTime >= 0)
            {
                this.titleDisplayTime = displayTime;
            }

            if (timeFadeOut >= 0)
            {
                this.titleFadeOut = timeFadeOut;
            }

            if (this.titlesTimer > 0)
            {
                this.titlesTimer = this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut;
            }
        }
    }

    public void setOverlayMessage(ITextComponent component, boolean animateColor)
    {
        this.setOverlayMessage(component.getUnformattedText(), animateColor);
    }

    /**
     * Forwards the given chat message to all listeners.
     */
    public void addChatMessage(ChatType chatTypeIn, ITextComponent message)
    {
        for (IChatListener ichatlistener : this.chatListeners.get(chatTypeIn))
        {
            ichatlistener.say(chatTypeIn, message);
        }
    }

    /**
     * returns a pointer to the persistant Chat GUI, containing all previous chat messages and such
     */
    public GuiNewChat getChatGUI()
    {
        return this.persistantChatGUI;
    }

    public int getUpdateCounter()
    {
        return this.updateCounter;
    }

    public FontRenderer getFontRenderer()
    {
        return this.mc.fontRenderer;
    }

    public GuiSpectator getSpectatorGui()
    {
        return this.spectatorGui;
    }

    public GuiPlayerTabOverlay getTabList()
    {
        return this.overlayPlayerList;
    }

    /**
     * Reset the GuiPlayerTabOverlay's message header and footer
     */
    public void resetPlayersOverlayFooterHeader()
    {
        this.overlayPlayerList.resetFooterHeader();
        this.overlayBoss.clearBossInfos();
        this.mc.getToastGui().clear();
    }

    /**
     * Accessor for the GuiBossOverlay
     */
    public GuiBossOverlay getBossOverlay()
    {
        return this.overlayBoss;
    }

    private int getArgonHudScaleFactor()
    {
        int width = Math.max(1, this.mc.displayWidth);
        int height = Math.max(1, this.mc.displayHeight);
        int factor = 1;

        while (factor < 2 && width / (factor + 1) >= 320 && height / (factor + 1) >= 240)
        {
            ++factor;
        }

        if (width >= 1920 && height >= 1080 && width / 3 >= 320 && height / 3 >= 240)
        {
            factor = 3;
        }

        if (this.mc.isUnicode() && factor % 2 != 0 && factor != 1)
        {
            --factor;
        }

        return Math.max(1, factor);
    }

    private void renderArgonHud(ScaledResolution scaledRes, FontRenderer fontRenderer)
    {
        Entity viewEntity = this.mc.getRenderViewEntity();

        if (viewEntity == null)
        {
            viewEntity = this.mc.player;
        }

        if (viewEntity == null || fontRenderer == null)
        {
            return;
        }

        this.mc.mcProfiler.startSection("argonHud");
        int fps = Minecraft.getDebugFPS();
        int posX = MathHelper.floor(viewEntity.posX);
        int posY = MathHelper.floor(viewEntity.getEntityBoundingBox().minY);
        int posZ = MathHelper.floor(viewEntity.posZ);
        boolean dirty = false;

        if (fps != this.lastHudFps)
        {
            this.lastHudFps = fps;
            this.cachedFpsLine = "\u00a79[\u00a7bFPS\u00a79] \u00a7f" + fps + " fps";
            dirty = true;
        }

        if (posX != this.lastHudX || posY != this.lastHudY || posZ != this.lastHudZ)
        {
            this.lastHudX = posX;
            this.lastHudY = posY;
            this.lastHudZ = posZ;
            this.cachedXyzLine = "\u00a79[\u00a7bXYZ\u00a79] \u00a7f" + posX + "\u00a77, \u00a7f" + posY + "\u00a77, \u00a7f" + posZ;
            dirty = true;
        }

        if (dirty || this.cachedHudBoxWidth <= 0)
        {
            int titleWidth = fontRenderer.getStringWidth(ARGON_HUD_TITLE);
            int fpsWidth = fontRenderer.getStringWidth(this.cachedFpsLine);
            int xyzWidth = fontRenderer.getStringWidth(this.cachedXyzLine);
            this.cachedHudBoxWidth = Math.max(titleWidth, Math.max(fpsWidth, xyzWidth)) + 10;
        }

        int hudScaleFactor = this.getArgonHudScaleFactor();
        double hudWidth = (double)Math.max(1, this.mc.displayWidth) / (double)hudScaleFactor;
        double hudHeight = (double)Math.max(1, this.mc.displayHeight) / (double)hudScaleFactor;

        int x = 4;
        int y = 4;

        if (this.mc.gameSettings.ofShowFps)
        {
            int ofFpsBottom = MathHelper.ceil(13.0F * (float)Math.max(1, scaledRes.getScaleFactor()) / (float)hudScaleFactor);
            y = Math.max(14, ofFpsBottom);
        }

        int lineHeight = fontRenderer.FONT_HEIGHT + 2;
        int boxHeight = lineHeight * 3 + 4;

        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, hudWidth, hudHeight, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);

        drawRect(x, y, x + this.cachedHudBoxWidth, y + boxHeight, 0x66000000);
        drawRect(x, y, x + 2, y + boxHeight, 0xCC3399FF);

        fontRenderer.drawStringWithShadow(ARGON_HUD_TITLE, (float)(x + 5), (float)(y + 3), 0xFFFFFF);
        fontRenderer.drawStringWithShadow(this.cachedFpsLine, (float)(x + 5), (float)(y + 3 + lineHeight), 0xFFFFFF);
        fontRenderer.drawStringWithShadow(this.cachedXyzLine, (float)(x + 5), (float)(y + 3 + lineHeight * 2), 0xFFFFFF);

        if (Config.isJadeHud())
        {
            this.renderJadeHud(hudWidth, hudHeight, fontRenderer);
        }

        if (Config.isMinimap())
        {
            this.renderMinimapHud(hudWidth, hudHeight, viewEntity, fontRenderer);
        }

        if (Config.isVoiceChat())
        {
            this.renderVoiceChatHud(hudWidth, hudHeight, fontRenderer);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        this.mc.mcProfiler.endSection();
    }

    private void renderJadeHud(double hudWidth, double hudHeight, FontRenderer fontRenderer)
    {
        RayTraceResult ray = this.mc.getObjectMouseOver();

        if (ray == null || ray.typeOfHit == RayTraceResult.Type.MISS || this.mc.world == null || this.mc.player == null)
        {
            return;
        }

        String titleLine = null;
        String subLine = null;
        ItemStack iconStack = ItemStack.EMPTY;
        float healthRatio = -1.0F;

        if (ray.typeOfHit == RayTraceResult.Type.ENTITY && ray.entityHit != null)
        {
            Entity target = ray.entityHit;

            if (target instanceof EntityItem)
            {
                ItemStack dropStack = ((EntityItem)target).getItem();

                if (!dropStack.isEmpty())
                {
                    iconStack = dropStack;
                    titleLine = "\u00a7f" + dropStack.getDisplayName() + (dropStack.getCount() > 1 ? " \u00a7bx" + dropStack.getCount() : "");
                    subLine = "\u00a77Dropped Item";
                }
            }
            else
            {
                titleLine = "\u00a7f" + target.getDisplayName().getFormattedText();

                if (target instanceof EntityLivingBase)
                {
                    EntityLivingBase living = (EntityLivingBase)target;
                    float hp = Math.max(0.0F, living.getHealth());
                    float maxHp = Math.max(1.0F, living.getMaxHealth());
                    healthRatio = MathHelper.clamp(hp / maxHp, 0.0F, 1.0F);
                    int hpInt = MathHelper.ceil(hp);
                    int maxHpInt = MathHelper.ceil(maxHp);
                    subLine = "\u00a7c\u2764 \u00a7f" + hpInt + " \u00a77/ \u00a7f" + maxHpInt + " HP";
                }
                else
                {
                    subLine = "\u00a77Entity";
                }
            }
        }
        else if (ray.typeOfHit == RayTraceResult.Type.BLOCK && ray.getBlockPos() != null)
        {
            BlockPos pos = ray.getBlockPos();
            IBlockState state = this.mc.world.getBlockState(pos);

            if (state.getMaterial() == Material.AIR)
            {
                return;
            }

            Item heldItem = this.mc.player.getHeldItemMainhand().getItem();

            if (state != this.lastJadeState || !pos.equals(this.lastJadePos) || heldItem != this.lastJadeHeldItem)
            {
                this.lastJadeState = state;
                this.lastJadePos = pos;
                this.lastJadeHeldItem = heldItem;
                Block block = state.getBlock();

                try
                {
                    this.cachedJadeIcon = block.getItem(this.mc.world, pos, state);
                }
                catch (Throwable var18)
                {
                    this.cachedJadeIcon = ItemStack.EMPTY;
                }

                if (!this.cachedJadeIcon.isEmpty())
                {
                    this.cachedJadeTitle = "\u00a7f" + this.cachedJadeIcon.getDisplayName();
                }
                else
                {
                    this.cachedJadeTitle = "\u00a7f" + block.getLocalizedName();
                }

                this.cachedJadeSub = this.computeJadeToolInfo(state, pos);
            }

            titleLine = this.cachedJadeTitle;
            subLine = this.cachedJadeSub;
            iconStack = this.cachedJadeIcon;
        }

        if (titleLine == null || subLine == null)
        {
            return;
        }

        boolean hasIcon = !iconStack.isEmpty();
        int iconPad = hasIcon ? 20 : 0;
        int textWidth = Math.max(fontRenderer.getStringWidth(titleLine), fontRenderer.getStringWidth(subLine));
        int boxWidth = Math.max(88, textWidth + iconPad + 12);
        int lineHeight = fontRenderer.FONT_HEIGHT + 2;
        int boxHeight = lineHeight * 2 + (healthRatio >= 0.0F ? 8 : 4);
        int boxX = (int)(hudWidth / 2.0D) - boxWidth / 2;
        int boxY = 5;

        drawRect(boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, 0x44000000);
        drawRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x88000000);
        drawRect(boxX, boxY, boxX + boxWidth, boxY + 2, 0xCC3399FF);

        int textX = boxX + 5 + iconPad;
        fontRenderer.drawStringWithShadow(titleLine, (float)textX, (float)(boxY + 4), 0xFFFFFF);
        fontRenderer.drawStringWithShadow(subLine, (float)textX, (float)(boxY + 4 + lineHeight), 0xCCCCCC);

        if (healthRatio >= 0.0F)
        {
            int barX = boxX + 5;
            int barY = boxY + boxHeight - 5;
            int barW = boxWidth - 10;
            int fillW = (int)((float)barW * healthRatio);
            int barColor = healthRatio > 0.5F ? 0xFF33CC66 : (healthRatio > 0.25F ? 0xFFCCAA22 : 0xFFDD3344);
            drawRect(barX, barY, barX + barW, barY + 3, 0x88220000);

            if (fillW > 0)
            {
                drawRect(barX, barY, barX + fillW, barY + 3, barColor);
            }
        }

        if (hasIcon)
        {
            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            this.itemRenderer.renderItemAndEffectIntoGUI(iconStack, boxX + 4, boxY + (boxHeight - 16) / 2 + 1);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.popMatrix();
        }
    }

    private String computeJadeToolInfo(IBlockState state, BlockPos pos)
    {
        float hardness = state.getBlockHardness(this.mc.world, pos);

        if (hardness < 0.0F)
        {
            return "\u00a7c\u2716 \u00a77Tool: \u00a7cUnbreakable";
        }

        if (this.probePickaxe.isEmpty())
        {
            this.probePickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            this.probeAxe = new ItemStack(Items.DIAMOND_AXE);
            this.probeShovel = new ItemStack(Items.DIAMOND_SHOVEL);
            this.probeShears = new ItemStack(Items.SHEARS);
            this.probeSword = new ItemStack(Items.DIAMOND_SWORD);
        }

        Block block = state.getBlock();
        float pickStr = Items.DIAMOND_PICKAXE.getStrVsBlock(this.probePickaxe, state);
        float axeStr = Items.DIAMOND_AXE.getStrVsBlock(this.probeAxe, state);
        float shovelStr = Items.DIAMOND_SHOVEL.getStrVsBlock(this.probeShovel, state);
        float shearsStr = Items.SHEARS.getStrVsBlock(this.probeShears, state);
        float swordStr = Items.DIAMOND_SWORD.getStrVsBlock(this.probeSword, state);

        String bestTool = "Any";

        if (pickStr > 1.0F || Items.DIAMOND_PICKAXE.canHarvestBlock(state))
        {
            if (!state.getMaterial().isToolNotRequired())
            {
                if (Items.WOODEN_PICKAXE.canHarvestBlock(state))
                {
                    bestTool = "Pickaxe (Wood+)";
                }
                else if (Items.STONE_PICKAXE.canHarvestBlock(state))
                {
                    bestTool = "Pickaxe (Stone+)";
                }
                else if (Items.IRON_PICKAXE.canHarvestBlock(state))
                {
                    bestTool = "Pickaxe (Iron+)";
                }
                else
                {
                    bestTool = "Pickaxe (Diamond)";
                }
            }
            else
            {
                bestTool = "Pickaxe";
            }
        }
        else if (axeStr > 1.0F)
        {
            bestTool = "Axe";
        }
        else if (shovelStr > 1.0F || Items.DIAMOND_SHOVEL.canHarvestBlock(state))
        {
            bestTool = "Shovel";
        }
        else if (shearsStr > 1.0F || Items.SHEARS.canHarvestBlock(state))
        {
            bestTool = "Shears";
        }
        else if (swordStr > 1.5F || block == Blocks.WEB)
        {
            bestTool = "Sword";
        }

        boolean canHarvest = this.mc.player.canHarvestBlock(state);
        String prefix = canHarvest ? "\u00a7a\u2714 " : "\u00a7c\u2716 ";
        return prefix + "\u00a77Tool: \u00a7b" + bestTool;
    }

    private void renderMinimapHud(double hudWidth, double hudHeight, Entity viewEntity, FontRenderer fontRenderer)
    {
        if (this.mc.world == null || viewEntity == null)
        {
            return;
        }

        if (this.minimapTexture == null)
        {
            this.minimapTexture = new DynamicTexture(MINIMAP_DIAMETER, MINIMAP_DIAMETER);
            this.minimapTextureLocation = this.mc.getTextureManager().getDynamicTextureLocation("argon_minimap", this.minimapTexture);
        }

        int centerX = MathHelper.floor(viewEntity.posX);
        int centerY = MathHelper.floor(viewEntity.getEntityBoundingBox().minY);
        int centerZ = MathHelper.floor(viewEntity.posZ);
        int currentDim = viewEntity.dimension;
        ++this.minimapFrameCounter;

        boolean movedEnough = Math.abs(centerX - this.lastMinimapScanX) >= 2 || Math.abs(centerZ - this.lastMinimapScanZ) >= 2;

        if (this.minimapFrameCounter >= 10 || movedEnough || currentDim != this.lastMinimapDimension)
        {
            this.minimapFrameCounter = 0;
            this.lastMinimapScanX = centerX;
            this.lastMinimapScanZ = centerZ;
            this.lastMinimapDimension = currentDim;
            int[] pixels = this.minimapTexture.getTextureData();
            this.mc.world.scanMinimapColors(centerX, centerY, centerZ, MINIMAP_RADIUS, pixels, this.minimapMutablePos);
            this.minimapTexture.updateDynamicTexture();
        }

        int mapSize = 68;
        int mapX = (int)hudWidth - mapSize - 6;
        int mapY = 6;

        if (this.mc.player != null && !this.mc.player.getActivePotionEffects().isEmpty())
        {
            mapY = 30;
        }

        // Background frame & Argon accent header
        drawRect(mapX - 3, mapY - 3, mapX + mapSize + 3, mapY + mapSize + 14, 0x77000000);
        drawRect(mapX - 3, mapY - 3, mapX + mapSize + 3, mapY - 1, 0xCC3399FF);
        drawRect(mapX - 1, mapY - 1, mapX + mapSize + 1, mapY + mapSize + 1, 0xFF0B0F19);

        // Draw cached 64x64 surface terrain texture in 1 quad
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.minimapTextureLocation);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos((double)mapX, (double)(mapY + mapSize), 0.0D).tex(0.0D, 1.0D).endVertex();
        buffer.pos((double)(mapX + mapSize), (double)(mapY + mapSize), 0.0D).tex(1.0D, 1.0D).endVertex();
        buffer.pos((double)(mapX + mapSize), (double)mapY, 0.0D).tex(1.0D, 0.0D).endVertex();
        buffer.pos((double)mapX, (double)mapY, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        int mapCenterX = mapX + mapSize / 2;
        int mapCenterY = mapY + mapSize / 2;

        // Subtle crosshair grid lines
        drawRect(mapCenterX, mapY, mapCenterX + 1, mapY + mapSize, 0x22FFFFFF);
        drawRect(mapX, mapCenterY, mapX + mapSize, mapCenterY + 1, 0x22FFFFFF);

        // Render Waypoint Pins (座標ピン)
        List<ArgonWaypointManager.Waypoint> waypoints = ArgonWaypointManager.getWaypoints();
        double scale = (double)mapSize / (double)MINIMAP_DIAMETER;
        double halfSpan = (double)(mapSize / 2 - 3);
        ArgonWaypointManager.Waypoint nearestWp = null;
        int nearestDist = Integer.MAX_VALUE;

        for (int i = 0; i < waypoints.size(); ++i)
        {
            ArgonWaypointManager.Waypoint wp = waypoints.get(i);

            if (!wp.enabled || wp.dimension != currentDim)
            {
                continue;
            }

            double dx = ((double)wp.x + 0.5D) - viewEntity.posX;
            double dz = ((double)wp.z + 0.5D) - viewEntity.posZ;
            int distBlocks = MathHelper.floor(Math.sqrt(dx * dx + dz * dz));

            if (distBlocks < nearestDist)
            {
                nearestDist = distBlocks;
                nearestWp = wp;
            }

            double px = dx * scale;
            double pz = dz * scale;
            double maxAbs = Math.max(Math.abs(px), Math.abs(pz));
            boolean clamped = false;

            if (maxAbs > halfSpan && maxAbs > 0.001D)
            {
                px = px / maxAbs * halfSpan;
                pz = pz / maxAbs * halfSpan;
                clamped = true;
            }

            int pinX = mapCenterX + MathHelper.floor(px + 0.5D);
            int pinY = mapCenterY + MathHelper.floor(pz + 0.5D);

            drawRect(pinX - 2, pinY - 2, pinX + 3, pinY + 3, 0xDD000000);
            drawRect(pinX - 1, pinY - 1, pinX + 2, pinY + 2, wp.color);

            if (!clamped && wp.name != null && !wp.name.isEmpty())
            {
                String shortLabel = wp.name.length() > 8 ? wp.name.substring(0, 8) : wp.name;
                int lw = fontRenderer.getStringWidth(shortLabel);
                int lx = MathHelper.clamp(pinX - lw / 2, mapX + 2, mapX + mapSize - lw - 2);
                int ly = pinY - 10 < mapY + 2 ? pinY + 4 : pinY - 10;
                fontRenderer.drawStringWithShadow(shortLabel, (float)lx, (float)ly, wp.color);
            }
        }

        // Render Weather 2 Doppler Tornado Radar blips on Minimap
        EntityWeatherStorm nearestStorm = null;
        int nearestStormDist = Integer.MAX_VALUE;

        if (Config.getWeather2Quality() > 0 && this.mc.world != null)
        {
            List<Entity> weatherList = this.mc.world.weatherEffects;

            for (int i = 0; i < weatherList.size(); ++i)
            {
                Entity we = weatherList.get(i);

                if (we instanceof EntityWeatherStorm && !we.isDead)
                {
                    EntityWeatherStorm storm = (EntityWeatherStorm)we;
                    double sdx = storm.posX - viewEntity.posX;
                    double sdz = storm.posZ - viewEntity.posZ;
                    int sDist = MathHelper.floor(Math.sqrt(sdx * sdx + sdz * sdz));

                    if (sDist < nearestStormDist)
                    {
                        nearestStormDist = sDist;
                        nearestStorm = storm;
                    }

                    double spx = sdx * scale;
                    double spz = sdz * scale;
                    double sMax = Math.max(Math.abs(spx), Math.abs(spz));

                    if (sMax > halfSpan && sMax > 0.001D)
                    {
                        spx = spx / sMax * halfSpan;
                        spz = spz / sMax * halfSpan;
                    }

                    int sx = mapCenterX + MathHelper.floor(spx + 0.5D);
                    int sy = mapCenterY + MathHelper.floor(spz + 0.5D);
                    int ef = storm.getEfScale();
                    int radarCol = ef >= 5 ? 0xFFFF1122 : (ef >= 3 ? 0xFFFF5511 : 0xFFFFAA00);

                    drawRect(sx - 3, sy - 3, sx + 4, sy + 4, 0xAA000000);
                    drawRect(sx - 2, sy - 2, sx + 3, sy + 3, radarCol);
                    String efTag = ef >= 1 ? "EF" + ef : "STM";
                    fontRenderer.drawStringWithShadow("\u00a7c" + efTag, (float)(sx - 6), (float)(sy - 10), 0xFFFFFF);
                }
            }
        }

        // Player center position & direction arrow
        float yawRad = (viewEntity.rotationYaw + 180.0F) * 0.017453292F;
        int tipX = mapCenterX + Math.round(-MathHelper.sin(yawRad) * 3.0F);
        int tipY = mapCenterY + Math.round(MathHelper.cos(yawRad) * 3.0F);
        drawRect(mapCenterX - 2, mapCenterY - 2, mapCenterX + 3, mapCenterY + 3, 0xFF000000);
        drawRect(mapCenterX - 1, mapCenterY - 1, mapCenterX + 2, mapCenterY + 2, 0xFFFFFFFF);
        drawRect(tipX - 1, tipY - 1, tipX + 2, tipY + 2, 0xFF33CCFF);

        // Cardinal directions (N / S / E / W)
        fontRenderer.drawStringWithShadow("\u00a7cN", (float)(mapCenterX - 2), (float)(mapY + 1), 0xFFFFFF);
        fontRenderer.drawStringWithShadow("\u00a77S", (float)(mapCenterX - 2), (float)(mapY + mapSize - 9), 0xCCCCCC);
        fontRenderer.drawStringWithShadow("\u00a77W", (float)(mapX + 2), (float)(mapCenterY - 4), 0xCCCCCC);
        fontRenderer.drawStringWithShadow("\u00a77E", (float)(mapX + mapSize - 7), (float)(mapCenterY - 4), 0xCCCCCC);

        // Footer bar: active tornado alert, nearest waypoint, or [B] Waypoint hint
        String footerText;

        if (nearestStorm != null && nearestStorm.getEfScale() >= 1)
        {
            footerText = "\u00a7c\u26a0 EF-" + nearestStorm.getEfScale() + " \u00a77(" + nearestStormDist + "m)";
        }
        else if (nearestWp != null)
        {
            String wpLabel = nearestWp.name.length() > 7 ? nearestWp.name.substring(0, 7) : nearestWp.name;
            footerText = "\u00a7b\u25c6 \u00a7f" + wpLabel + " \u00a77(" + nearestDist + "m)";
        }
        else
        {
            footerText = "\u00a79[\u00a7bB\u00a79] \u00a77Waypoints";
        }

        int fw = fontRenderer.getStringWidth(footerText);
        fontRenderer.drawStringWithShadow(footerText, (float)(mapCenterX - fw / 2), (float)(mapY + mapSize + 3), 0xFFFFFF);
    }

    private void renderVoiceChatHud(double hudWidth, double hudHeight, FontRenderer fontRenderer)
    {
        if (this.mc.player == null || this.mc.world == null)
        {
            return;
        }

        boolean muted = ArgonVoiceChatManager.isMicMuted();
        boolean speaking = ArgonVoiceChatManager.isPttActive();
        boolean whispering = this.mc.player.isSneaking();
        float micLevel = ArgonVoiceChatManager.getLocalMicLevel();

        int baseX = 5;
        int baseY = (int)hudHeight - 24;

        String statusText;
        int accentColor;

        if (muted)
        {
            statusText = "\u00a7cMIC MUTED \u00a77[M]";
            accentColor = 0xFFDD3344;
        }
        else if (speaking)
        {
            statusText = whispering ? "\u00a7eWHISPERING \u00a77[V]" : "\u00a7aSPEAKING \u00a77[V]";
            accentColor = whispering ? 0xFFFFCC33 : 0xFF00E5FF;
        }
        else
        {
            statusText = "\u00a77VOICE READY \u00a78[V]";
            accentColor = 0xFF3399FF;
        }

        int textW = fontRenderer.getStringWidth(statusText);
        int boxW = 20 + textW + (speaking ? 16 : 6);
        int boxH = 18;

        drawRect(baseX, baseY, baseX + boxW, baseY + boxH, 0x77000000);
        drawRect(baseX, baseY, baseX + 2, baseY + boxH, accentColor);

        // Procedural Microphone Icon (10x12 at baseX + 5, baseY + 3)
        int micX = baseX + 6;
        int micY = baseY + 3;
        int micColor = muted ? 0xFFDD3344 : (speaking ? 0xFF00E5FF : 0xFFCCCCCC);
        drawRect(micX + 2, micY, micX + 6, micY + 6, micColor);       // Capsule
        drawRect(micX + 1, micY + 4, micX + 7, micY + 8, micColor);   // U-cradle
        drawRect(micX + 3, micY + 8, micX + 5, micY + 10, micColor);  // Stem
        drawRect(micX + 1, micY + 10, micX + 7, micY + 11, micColor); // Base

        if (muted)
        {
            // Diagonal mute strike
            drawRect(micX, micY + 1, micX + 3, micY + 3, 0xFFFF2222);
            drawRect(micX + 3, micY + 4, micX + 6, micY + 6, 0xFFFF2222);
            drawRect(micX + 6, micY + 7, micX + 8, micY + 9, 0xFFFF2222);
        }

        fontRenderer.drawStringWithShadow(statusText, (float)(baseX + 18), (float)(baseY + 5), 0xFFFFFF);

        if (speaking)
        {
            int waveX = baseX + 20 + textW;
            int bar1H = Math.max(2, Math.round(micLevel * 8.0F));
            int bar2H = Math.max(3, Math.round((0.5F + 0.5F * micLevel) * 10.0F));
            int bar3H = Math.max(2, Math.round(micLevel * 6.0F));
            int bottomY = baseY + 14;
            drawRect(waveX, bottomY - bar1H, waveX + 2, bottomY, accentColor);
            drawRect(waveX + 3, bottomY - bar2H, waveX + 5, bottomY, accentColor);
            drawRect(waveX + 6, bottomY - bar3H, waveX + 8, bottomY, accentColor);
        }

        // Render Nearby Proximity Voice Peers right above the mic widget
        List<ArgonVoiceChatManager.VoicePeerState> peers = ArgonVoiceChatManager.getActiveHudPeers();
        int maxShown = Math.min(4, peers.size());

        for (int i = 0; i < maxShown; ++i)
        {
            ArgonVoiceChatManager.VoicePeerState peer = peers.get(i);
            int rowY = baseY - (i + 1) * 16 - 2;
            int gainPct = Math.round(MathHelper.clamp(peer.proximityGain, 0.0F, 1.0F) * 100.0F);
            int distInt = MathHelper.floor(peer.distance);
            String occTag = peer.occluded ? " \u00a78[Wall]" : "";
            String peerLabel = (peer.speaking ? "\u00a7a\u25b6 \u00a7f" : "\u00a7b\u2022 \u00a77") + peer.name + " \u00a78(" + distInt + "m)" + occTag;
            int labelW = fontRenderer.getStringWidth(peerLabel);
            int rowW = Math.max(115, labelW + 38);

            drawRect(baseX, rowY, baseX + rowW, rowY + 14, 0x66000000);
            drawRect(baseX, rowY, baseX + 2, rowY + 14, peer.speaking ? 0xFF33CC66 : 0xFF3399FF);
            fontRenderer.drawStringWithShadow(peerLabel, (float)(baseX + 5), (float)(rowY + 3), 0xFFFFFF);

            // Proximity Attenuation Bar (grows as player gets closer, shrinks with distance)
            int barW = 26;
            int barX = baseX + rowW - barW - 4;
            int barY = rowY + 5;
            int fillW = MathHelper.clamp(Math.round((float)barW * MathHelper.clamp(peer.proximityGain, 0.0F, 1.0F)), 0, barW);
            int barColor = gainPct >= 60 ? 0xFF00E5FF : (gainPct >= 30 ? 0xFF3399FF : 0xFF557799);

            drawRect(barX, barY, barX + barW, barY + 4, 0x88111824);

            if (fillW > 0)
            {
                drawRect(barX, barY, barX + fillW, barY + 4, barColor);
            }
        }
    }
}
