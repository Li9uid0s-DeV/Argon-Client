package net.minecraft.argon.create;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;

public final class CreateMeshTemplate
{
    public enum SpriteKey
    {
        ANDESITE("minecraft:blocks/stone_andesite_smooth"),
        PLANKS_OAK("minecraft:blocks/planks_oak"),
        PLANKS_SPRUCE("minecraft:blocks/planks_spruce"),
        IRON("minecraft:blocks/iron_block"),
        GOLD("minecraft:blocks/gold_block"),
        CAULDRON("minecraft:blocks/cauldron_side"),
        OBSIDIAN("minecraft:blocks/obsidian"),
        GLASS("minecraft:blocks/glass"),
        WOOL_WHITE("minecraft:blocks/wool_colored_white"),
        ANVIL("minecraft:blocks/anvil_base"),
        RAIL("minecraft:blocks/rail_normal"),
        WATER("minecraft:blocks/water_still"),
        LAVA("minecraft:blocks/lava_still"),
        FURNACE("minecraft:blocks/furnace_top"),
        STONE("minecraft:blocks/stone");

        public final String location;

        private SpriteKey(String location)
        {
            this.location = location;
        }
    }

    public static final class Quad
    {
        public final float[] x = new float[4];
        public final float[] y = new float[4];
        public final float[] z = new float[4];
        public final float nx;
        public final float ny;
        public final float nz;
        public final SpriteKey sprite;
        public final float r;
        public final float g;
        public final float b;
        public final float a;

        public Quad(
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz,
            SpriteKey sprite, float r, float g, float b, float a)
        {
            this.x[0] = x0; this.y[0] = y0; this.z[0] = z0;
            this.x[1] = x1; this.y[1] = y1; this.z[1] = z1;
            this.x[2] = x2; this.y[2] = y2; this.z[2] = z2;
            this.x[3] = x3; this.y[3] = y3; this.z[3] = z3;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.sprite = sprite;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }

    public final Quad[] quads;

    private CreateMeshTemplate(List<Quad> list)
    {
        this.quads = list.toArray(new Quad[list.size()]);
    }

    public static class Builder
    {
        private final List<Quad> list = new ArrayList<Quad>();

        public Builder addBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, SpriteKey sprite, float r, float g, float b, float a)
        {
            // Bottom (-Y)
            this.list.add(new Quad(
                minX, minY, maxZ,
                minX, minY, minZ,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                0.0F, -1.0F, 0.0F, sprite, r * 0.65F, g * 0.65F, b * 0.65F, a));

            // Top (+Y)
            this.list.add(new Quad(
                minX, maxY, minZ,
                minX, maxY, maxZ,
                maxX, maxY, maxZ,
                maxX, maxY, minZ,
                0.0F, 1.0F, 0.0F, sprite, r, g, b, a));

            // North (-Z)
            this.list.add(new Quad(
                maxX, maxY, minZ,
                maxX, minY, minZ,
                minX, minY, minZ,
                minX, maxY, minZ,
                0.0F, 0.0F, -1.0F, sprite, r * 0.85F, g * 0.85F, b * 0.85F, a));

            // South (+Z)
            this.list.add(new Quad(
                minX, maxY, maxZ,
                minX, minY, maxZ,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                0.0F, 0.0F, 1.0F, sprite, r * 0.85F, g * 0.85F, b * 0.85F, a));

            // West (-X)
            this.list.add(new Quad(
                minX, maxY, minZ,
                minX, minY, minZ,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                -1.0F, 0.0F, 0.0F, sprite, r * 0.75F, g * 0.75F, b * 0.75F, a));

            // East (+X)
            this.list.add(new Quad(
                maxX, maxY, maxZ,
                maxX, minY, maxZ,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                1.0F, 0.0F, 0.0F, sprite, r * 0.75F, g * 0.75F, b * 0.75F, a));

            return this;
        }

        public Builder addRotatedBox(float cx, float cy, float cz, float hx, float hy, float hz, float angleYRad, SpriteKey sprite, float r, float g, float b, float a)
        {
            float cos = (float)Math.cos(angleYRad);
            float sin = (float)Math.sin(angleYRad);
            Builder temp = new Builder();
            temp.addBox(-hx, -hy, -hz, hx, hy, hz, sprite, r, g, b, a);

            for (int i = 0; i < temp.list.size(); ++i)
            {
                Quad q = temp.list.get(i);
                float[] rx = new float[4];
                float[] ry = new float[4];
                float[] rz = new float[4];

                for (int v = 0; v < 4; ++v)
                {
                    rx[v] = cx + q.x[v] * cos - q.z[v] * sin;
                    ry[v] = cy + q.y[v];
                    rz[v] = cz + q.x[v] * sin + q.z[v] * cos;
                }

                float rnx = q.nx * cos - q.nz * sin;
                float rnz = q.nx * sin + q.nz * cos;
                this.list.add(new Quad(
                    rx[0], ry[0], rz[0],
                    rx[1], ry[1], rz[1],
                    rx[2], ry[2], rz[2],
                    rx[3], ry[3], rz[3],
                    rnx, q.ny, rnz, q.sprite, q.r, q.g, q.b, q.a));
            }

            return this;
        }

        public CreateMeshTemplate build()
        {
            return new CreateMeshTemplate(this.list);
        }
    }

    public static TextureAtlasSprite resolveSprite(SpriteKey key)
    {
        TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite sprite = map.getAtlasSprite(key.location);
        return sprite != null ? sprite : map.getMissingSprite();
    }
}
