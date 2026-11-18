package net.minecraft.src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class ArgonSkinManager
{
    public static final int SKIN_WIDTH = 64;
    public static final int SKIN_HEIGHT = 64;
    private static final int[] skinPixels = new int[SKIN_WIDTH * SKIN_HEIGHT];
    private static boolean loaded = false;
    private static boolean customSkinEnabled = false;
    private static boolean textureDirty = true;
    private static DynamicTexture dynamicTexture = null;
    private static ResourceLocation customSkinLocation = null;

    public static void ensureLoaded()
    {
        if (loaded)
        {
            return;
        }

        loaded = true;
        initDefaultSteveTemplate();

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.mcDataDir == null)
        {
            return;
        }

        File skinFile = new File(mc.mcDataDir, "options_skin.txt");

        if (!skinFile.exists())
        {
            return;
        }

        BufferedReader reader = null;

        try
        {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(skinFile), StandardCharsets.UTF_8));
            String line;
            int pixelIndex = 0;

            while ((line = reader.readLine()) != null)
            {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue;
                }

                if (line.startsWith("enabled:"))
                {
                    customSkinEnabled = Boolean.parseBoolean(line.substring("enabled:".length()).trim());
                }
                else if (line.startsWith("row:"))
                {
                    String[] hexTokens = line.substring("row:".length()).split(",");

                    for (int i = 0; i < hexTokens.length && pixelIndex < skinPixels.length; ++i)
                    {
                        String token = hexTokens[i].trim();

                        if (!token.isEmpty())
                        {
                            skinPixels[pixelIndex++] = (int)Long.parseLong(token, 16);
                        }
                    }
                }
            }

            textureDirty = true;
        }
        catch (Exception ex)
        {
            Config.warn("Failed to load Argon custom skin: " + ex.getMessage());
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (Exception var12)
                {
                    ;
                }
            }
        }
    }

    public static void saveSkin()
    {
        ensureLoaded();
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.mcDataDir == null)
        {
            return;
        }

        File skinFile = new File(mc.mcDataDir, "options_skin.txt");
        PrintWriter writer = null;

        try
        {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(skinFile), StandardCharsets.UTF_8));
            writer.println("# Argon Client 64x64 Skin Data");
            writer.println("enabled:" + customSkinEnabled);

            for (int y = 0; y < SKIN_HEIGHT; ++y)
            {
                StringBuilder sb = new StringBuilder("row:");

                for (int x = 0; x < SKIN_WIDTH; ++x)
                {
                    if (x > 0)
                    {
                        sb.append(',');
                    }

                    sb.append(Integer.toHexString(skinPixels[y * SKIN_WIDTH + x]));
                }

                writer.println(sb.toString());
            }
        }
        catch (Exception ex)
        {
            Config.warn("Failed to save Argon custom skin: " + ex.getMessage());
        }
        finally
        {
            if (writer != null)
            {
                writer.close();
            }
        }
    }

    public static boolean hasCustomSkin()
    {
        ensureLoaded();
        return customSkinEnabled;
    }

    public static void setCustomSkinEnabled(boolean enabled)
    {
        ensureLoaded();
        customSkinEnabled = enabled;
        textureDirty = true;
    }

    public static boolean isLocalPlayer(AbstractClientPlayer player)
    {
        if (player == null)
        {
            return false;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null)
        {
            return false;
        }

        if (player == mc.player)
        {
            return true;
        }

        if (mc.getSession() != null && player.getGameProfile() != null && player.getGameProfile().getName() != null)
        {
            return player.getGameProfile().getName().equalsIgnoreCase(mc.getSession().getUsername());
        }

        return false;
    }

    public static ResourceLocation getCustomSkinLocation()
    {
        ensureLoaded();
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.getTextureManager() == null)
        {
            return null;
        }

        if (dynamicTexture == null || customSkinLocation == null)
        {
            dynamicTexture = new DynamicTexture(SKIN_WIDTH, SKIN_HEIGHT);
            customSkinLocation = mc.getTextureManager().getDynamicTextureLocation("argon_custom_skin", dynamicTexture);
            textureDirty = true;
        }

        if (textureDirty)
        {
            int[] texData = dynamicTexture.getTextureData();
            System.arraycopy(skinPixels, 0, texData, 0, SKIN_WIDTH * SKIN_HEIGHT);
            dynamicTexture.updateDynamicTexture();
            textureDirty = false;
        }

        return customSkinLocation;
    }

    public static void markTextureDirty()
    {
        textureDirty = true;
        getCustomSkinLocation();
    }

    public static int getPixel(int x, int y)
    {
        ensureLoaded();

        if (x < 0 || x >= SKIN_WIDTH || y < 0 || y >= SKIN_HEIGHT)
        {
            return 0;
        }

        return skinPixels[y * SKIN_WIDTH + x];
    }

    public static void setPixel(int x, int y, int argb)
    {
        ensureLoaded();

        if (x >= 0 && x < SKIN_WIDTH && y >= 0 && y < SKIN_HEIGHT)
        {
            if (skinPixels[y * SKIN_WIDTH + x] != argb)
            {
                skinPixels[y * SKIN_WIDTH + x] = argb;
                textureDirty = true;
            }
        }
    }

    public static void floodFill(int startX, int startY, int newColor)
    {
        ensureLoaded();

        if (startX < 0 || startX >= SKIN_WIDTH || startY < 0 || startY >= SKIN_HEIGHT)
        {
            return;
        }

        int targetColor = skinPixels[startY * SKIN_WIDTH + startX];

        if (targetColor == newColor)
        {
            return;
        }

        int[] queueX = new int[SKIN_WIDTH * SKIN_HEIGHT];
        int[] queueY = new int[SKIN_WIDTH * SKIN_HEIGHT];
        boolean[] visited = new boolean[SKIN_WIDTH * SKIN_HEIGHT];
        int head = 0;
        int tail = 0;

        queueX[tail] = startX;
        queueY[tail] = startY;
        visited[startY * SKIN_WIDTH + startX] = true;
        ++tail;

        while (head < tail)
        {
            int cx = queueX[head];
            int cy = queueY[head];
            ++head;

            skinPixels[cy * SKIN_WIDTH + cx] = newColor;

            int[] dx = new int[] {-1, 1, 0, 0};
            int[] dy = new int[] {0, 0, -1, 1};

            for (int d = 0; d < 4; ++d)
            {
                int nx = cx + dx[d];
                int ny = cy + dy[d];

                if (nx >= 0 && nx < SKIN_WIDTH && ny >= 0 && ny < SKIN_HEIGHT)
                {
                    int idx = ny * SKIN_WIDTH + nx;

                    if (!visited[idx] && skinPixels[idx] == targetColor)
                    {
                        visited[idx] = true;
                        queueX[tail] = nx;
                        queueY[tail] = ny;
                        ++tail;
                    }
                }
            }
        }

        textureDirty = true;
    }

    private static void fillRectPixel(int x0, int y0, int w, int h, int argb)
    {
        for (int dy = 0; dy < h; ++dy)
        {
            int py = y0 + dy;

            if (py >= 0 && py < SKIN_HEIGHT)
            {
                for (int dx = 0; dx < w; ++dx)
                {
                    int px = x0 + dx;

                    if (px >= 0 && px < SKIN_WIDTH)
                    {
                        skinPixels[py * SKIN_WIDTH + px] = argb;
                    }
                }
            }
        }
    }

    public static void initDefaultSteveTemplate()
    {
        for (int i = 0; i < skinPixels.length; ++i)
        {
            skinPixels[i] = 0x00000000;
        }

        // Head base (8x8x8)
        fillRectPixel(8, 0, 8, 8, 0xFF4A3423);   // Top hair
        fillRectPixel(16, 0, 8, 8, 0xFFA87D5B);  // Bottom chin/neck
        fillRectPixel(0, 8, 8, 8, 0xFFB58662);   // Right head
        fillRectPixel(8, 8, 8, 8, 0xFFC69672);   // Front face
        fillRectPixel(16, 8, 8, 8, 0xFFB58662);  // Left head
        fillRectPixel(24, 8, 8, 8, 0xFF4A3423);  // Back hair

        // Hairline on head front & sides
        fillRectPixel(8, 8, 8, 2, 0xFF4A3423);
        fillRectPixel(8, 10, 1, 1, 0xFF4A3423);
        fillRectPixel(15, 10, 1, 1, 0xFF4A3423);
        fillRectPixel(0, 8, 8, 3, 0xFF4A3423);
        fillRectPixel(16, 8, 8, 3, 0xFF4A3423);

        // Eyes & Mouth on Front Face (8..15, 8..15)
        setPixelDirect(9, 12, 0xFFFFFFFF);
        setPixelDirect(10, 12, 0xFF4B3D8F);
        setPixelDirect(13, 12, 0xFF4B3D8F);
        setPixelDirect(14, 12, 0xFFFFFFFF);
        fillRectPixel(11, 13, 2, 1, 0xFFA97755); // Nose
        fillRectPixel(10, 14, 4, 1, 0xFF7B4A31); // Mouth

        // Body / Torso (8x12x4)
        fillRectPixel(20, 16, 8, 4, 0xFF0099AA); // Top torso
        fillRectPixel(28, 16, 8, 4, 0xFF007788); // Bottom torso
        fillRectPixel(16, 20, 4, 12, 0xFF008899); // Right torso
        fillRectPixel(20, 20, 8, 12, 0xFF00AABB); // Front torso
        fillRectPixel(28, 20, 4, 12, 0xFF008899); // Left torso
        fillRectPixel(32, 20, 8, 12, 0xFF0099AA); // Back torso
        fillRectPixel(22, 20, 4, 2, 0xFFB58662);  // Neck V-collar

        // Right Arm (4x12x4 at 40,16)
        fillRectPixel(44, 16, 4, 4, 0xFF0099AA);  // Shoulder top
        fillRectPixel(48, 16, 4, 4, 0xFFA87D5B);  // Hand bottom
        fillRectPixel(40, 20, 16, 4, 0xFF0099AA); // Sleeve
        fillRectPixel(40, 24, 16, 8, 0xFFB58662); // Arm skin

        // Left Arm (4x12x4 at 32,48)
        fillRectPixel(36, 48, 4, 4, 0xFF0099AA);
        fillRectPixel(40, 48, 4, 4, 0xFFA87D5B);
        fillRectPixel(32, 52, 16, 4, 0xFF0099AA);
        fillRectPixel(32, 56, 16, 8, 0xFFB58662);

        // Right Leg (4x12x4 at 0,16)
        fillRectPixel(4, 16, 4, 4, 0xFF3A3A7A);
        fillRectPixel(8, 16, 4, 4, 0xFF555555);
        fillRectPixel(0, 20, 16, 10, 0xFF3F3F88);
        fillRectPixel(0, 30, 16, 2, 0xFF5A5A5A);

        // Left Leg (4x12x4 at 16,48)
        fillRectPixel(20, 48, 4, 4, 0xFF3A3A7A);
        fillRectPixel(24, 48, 4, 4, 0xFF555555);
        fillRectPixel(16, 52, 16, 10, 0xFF3F3F88);
        fillRectPixel(16, 62, 16, 2, 0xFF5A5A5A);

        textureDirty = true;
    }

    public static void initArgonCyberTemplate()
    {
        for (int i = 0; i < skinPixels.length; ++i)
        {
            skinPixels[i] = 0x00000000;
        }

        // Argon Cyber Helmet / Head
        fillRectPixel(8, 0, 8, 8, 0xFF141B2D);
        fillRectPixel(16, 0, 8, 8, 0xFF0E1320);
        fillRectPixel(0, 8, 8, 8, 0xFF182238);
        fillRectPixel(8, 8, 8, 8, 0xFF1B263B);
        fillRectPixel(16, 8, 8, 8, 0xFF182238);
        fillRectPixel(24, 8, 8, 8, 0xFF141B2D);

        // Glowing Argon Plasma Visor on Face
        fillRectPixel(8, 11, 8, 2, 0xFF00E5FF);
        fillRectPixel(9, 11, 6, 1, 0xFF80F8FF);
        fillRectPixel(10, 9, 4, 1, 0xFF5C6BC0);

        // Argon Cyber Suit Torso
        fillRectPixel(20, 16, 8, 4, 0xFF1B263B);
        fillRectPixel(28, 16, 8, 4, 0xFF101726);
        fillRectPixel(16, 20, 16, 12, 0xFF162032);
        fillRectPixel(32, 20, 8, 12, 0xFF141B2D);
        // Plasma Core on Chest
        fillRectPixel(22, 22, 4, 4, 0xFF3399FF);
        fillRectPixel(23, 23, 2, 2, 0xFF00FFFF);
        fillRectPixel(23, 27, 2, 5, 0xFF283593);

        // Right & Left Arms
        fillRectPixel(44, 16, 4, 4, 0xFF1B263B);
        fillRectPixel(48, 16, 4, 4, 0xFF00E5FF);
        fillRectPixel(40, 20, 16, 12, 0xFF182238);
        fillRectPixel(40, 25, 16, 1, 0xFF00E5FF);

        fillRectPixel(36, 48, 4, 4, 0xFF1B263B);
        fillRectPixel(40, 48, 4, 4, 0xFF00E5FF);
        fillRectPixel(32, 52, 16, 12, 0xFF182238);
        fillRectPixel(32, 57, 16, 1, 0xFF00E5FF);

        // Right & Left Legs
        fillRectPixel(4, 16, 8, 4, 0xFF101726);
        fillRectPixel(0, 20, 16, 12, 0xFF141B2D);
        fillRectPixel(0, 29, 16, 1, 0xFF3399FF);

        fillRectPixel(20, 48, 8, 4, 0xFF101726);
        fillRectPixel(16, 52, 16, 12, 0xFF141B2D);
        fillRectPixel(16, 61, 16, 1, 0xFF3399FF);

        textureDirty = true;
    }

    private static void setPixelDirect(int x, int y, int argb)
    {
        if (x >= 0 && x < SKIN_WIDTH && y >= 0 && y < SKIN_HEIGHT)
        {
            skinPixels[y * SKIN_WIDTH + x] = argb;
        }
    }

    public static String getRegionLabel(int x, int y)
    {
        if (y >= 0 && y < 16)
        {
            if (x >= 0 && x < 32)
            {
                if (x >= 8 && x < 16 && y >= 8 && y < 16)
                {
                    return "Head (Front Face)";
                }

                return "Head (Base Layer)";
            }

            if (x >= 32 && x < 64)
            {
                return "Hat / Helmet Overlay";
            }
        }
        else if (y >= 16 && y < 32)
        {
            if (x >= 0 && x < 16)
            {
                return "Right Leg (Base)";
            }

            if (x >= 16 && x < 40)
            {
                return "Body / Torso (Base)";
            }

            if (x >= 40 && x < 56)
            {
                return "Right Arm (Base)";
            }
        }
        else if (y >= 32 && y < 48)
        {
            if (x >= 0 && x < 16)
            {
                return "Right Pants Overlay";
            }

            if (x >= 16 && x < 40)
            {
                return "Jacket Overlay";
            }

            if (x >= 40 && x < 56)
            {
                return "Right Sleeve Overlay";
            }
        }
        else if (y >= 48 && y < 64)
        {
            if (x >= 0 && x < 16)
            {
                return "Left Pants Overlay";
            }

            if (x >= 16 && x < 32)
            {
                return "Left Leg (Base)";
            }

            if (x >= 32 && x < 48)
            {
                return "Left Arm (Base)";
            }

            if (x >= 48 && x < 64)
            {
                return "Left Sleeve Overlay";
            }
        }

        return "Unused UV Space";
    }
}
