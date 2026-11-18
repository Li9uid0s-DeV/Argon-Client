package net.minecraft.src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class ArgonWaypointManager
{
    public static final int[] COLOR_PALETTE = new int[]
    {
        0xFF33CCFF,
        0xFF33EE66,
        0xFFFFCC33,
        0xFFFF4455,
        0xFFCC66FF,
        0xFFFF8833
    };

    public static final String[] COLOR_NAMES = new String[]
    {
        "\u00a7bCyan",
        "\u00a7aGreen",
        "\u00a7eGold",
        "\u00a7cRed",
        "\u00a7dPurple",
        "\u00a76Orange"
    };

    private static final List<Waypoint> WAYPOINTS = new ArrayList<Waypoint>();
    private static boolean loaded = false;

    public static class Waypoint
    {
        public String name;
        public int x;
        public int y;
        public int z;
        public int dimension;
        public int color;
        public boolean enabled;

        public Waypoint(String name, int x, int y, int z, int dimension, int color, boolean enabled)
        {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.color = color;
            this.enabled = enabled;
        }
    }

    public static List<Waypoint> getWaypoints()
    {
        if (!loaded)
        {
            loadWaypoints();
        }

        return WAYPOINTS;
    }

    public static Waypoint addWaypoint(String name, int x, int y, int z, int dimension, int color)
    {
        if (!loaded)
        {
            loadWaypoints();
        }

        String cleanName = (name == null || name.trim().isEmpty()) ? ("WP #" + (WAYPOINTS.size() + 1)) : name.trim().replace(':', '_');
        Waypoint wp = new Waypoint(cleanName, x, y, z, dimension, color, true);
        WAYPOINTS.add(wp);
        saveWaypoints();
        return wp;
    }

    public static Waypoint addQuickWaypointAtPlayer(Minecraft mc)
    {
        if (mc == null || mc.player == null)
        {
            return null;
        }

        if (!loaded)
        {
            loadWaypoints();
        }

        Entity view = mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.player;
        int px = MathHelper.floor(view.posX);
        int py = MathHelper.floor(view.getEntityBoundingBox().minY);
        int pz = MathHelper.floor(view.posZ);
        int dim = mc.player.dimension;
        int color = COLOR_PALETTE[WAYPOINTS.size() % COLOR_PALETTE.length];
        String name = "WP #" + (WAYPOINTS.size() + 1);
        Waypoint wp = addWaypoint(name, px, py, pz, dim, color);

        if (mc.ingameGUI != null)
        {
            mc.ingameGUI.setOverlayMessage("\u00a7b[Argon] \u00a7fPinned Waypoint \u00a7b" + wp.name + " \u00a77(" + px + ", " + py + ", " + pz + ")", false);
        }

        return wp;
    }

    public static void removeWaypoint(int index)
    {
        if (!loaded)
        {
            loadWaypoints();
        }

        if (index >= 0 && index < WAYPOINTS.size())
        {
            WAYPOINTS.remove(index);
            saveWaypoints();
        }
    }

    public static void toggleWaypoint(int index)
    {
        if (!loaded)
        {
            loadWaypoints();
        }

        if (index >= 0 && index < WAYPOINTS.size())
        {
            Waypoint wp = WAYPOINTS.get(index);
            wp.enabled = !wp.enabled;
            saveWaypoints();
        }
    }

    public static void clearWaypoints()
    {
        WAYPOINTS.clear();
        saveWaypoints();
    }

    private static File getWaypointFile()
    {
        Minecraft mc = Minecraft.getMinecraft();
        File dir = (mc != null && mc.mcDataDir != null) ? mc.mcDataDir : new File(".");
        return new File(dir, "options_waypoints.txt");
    }

    public static void loadWaypoints()
    {
        loaded = true;
        WAYPOINTS.clear();
        File file = getWaypointFile();

        if (!file.exists())
        {
            return;
        }

        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;

            while ((line = reader.readLine()) != null)
            {
                String[] parts = line.split(":");

                if (parts.length >= 7)
                {
                    String name = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    int dim = Integer.parseInt(parts[4]);
                    int color = (int)Long.parseLong(parts[5], 16);
                    boolean enabled = Boolean.parseBoolean(parts[6]);
                    WAYPOINTS.add(new Waypoint(name, x, y, z, dim, color, enabled));
                }
            }

            reader.close();
        }
        catch (Exception var11)
        {
            Config.warn("Failed to load Argon waypoints");
        }
    }

    public static void saveWaypoints()
    {
        File file = getWaypointFile();

        try
        {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

            for (int i = 0; i < WAYPOINTS.size(); ++i)
            {
                Waypoint wp = WAYPOINTS.get(i);
                writer.println(wp.name + ":" + wp.x + ":" + wp.y + ":" + wp.z + ":" + wp.dimension + ":" + Integer.toHexString(wp.color) + ":" + wp.enabled);
            }

            writer.close();
        }
        catch (Exception var4)
        {
            Config.warn("Failed to save Argon waypoints");
        }
    }
}
