package net.minecraft.argon.create;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.BlockPos;

/**
 * Combined Mesh Cache for Contraptions and 10-Car Trains.
 * Bakes all blocks in a contraption or train carriage into ONE static GPU VertexBuffer
 * (with hidden internal face culling + BlockColors/directional shading), so rendering an entire
 * 10-car train costs only 1 draw call per car at 60 FPS on Kompanio 500.
 */
public class ContraptionMeshCache
{
    private VertexBuffer gpuVbo;
    private BufferBuilder.State fallbackState;
    private boolean built = false;
    private boolean empty = true;

    public void render(ContraptionStructure structure, double relX, double relY, double relZ, float yawDeg, float lightFactor)
    {
        if (structure == null || structure.entries.isEmpty())
        {
            return;
        }

        if (!this.built)
        {
            this.buildCombinedMesh(structure);
        }

        if (this.empty)
        {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(relX, relY, relZ);

        if (Math.abs(yawDeg) > 0.01F)
        {
            GlStateManager.rotate(yawDeg, 0.0F, 1.0F, 0.0F);
        }

        GlStateManager.enableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(lightFactor, lightFactor, lightFactor, 1.0F);

        if (this.gpuVbo != null && OpenGlHelper.useVbo())
        {
            this.gpuVbo.bindBuffer();
            GlStateManager.glEnableClientState(32884);
            GlStateManager.glVertexPointer(3, 5126, 24, 0);
            GlStateManager.glEnableClientState(32888);
            GlStateManager.glTexCoordPointer(2, 5126, 24, 12);
            GlStateManager.glEnableClientState(32886);
            GlStateManager.glColorPointer(4, 5121, 24, 20);
            this.gpuVbo.drawArrays(7);
            this.gpuVbo.unbindBuffer();
            GlStateManager.glDisableClientState(32884);
            GlStateManager.glDisableClientState(32888);
            GlStateManager.glDisableClientState(32886);
        }
        else if (this.fallbackState != null)
        {
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            buf.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            buf.setVertexState(this.fallbackState);
            tess.draw();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        RenderHelper.enableStandardItemLighting();
    }

    private void buildCombinedMesh(ContraptionStructure structure)
    {
        this.built = true;
        Set<BlockPos> occupied = new HashSet<BlockPos>(structure.entries.size() * 2);

        for (int i = 0; i < structure.entries.size(); ++i)
        {
            occupied.add(structure.entries.get(i).relPos);
        }

        Minecraft mc = Minecraft.getMinecraft();
        BlockModelShapes shapes = mc.getBlockRendererDispatcher().getBlockModelShapes();
        BufferBuilder buf = new BufferBuilder(Math.max(4096, structure.entries.size() * 256));
        buf.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

        int emittedQuads = 0;

        for (int i = 0; i < structure.entries.size(); ++i)
        {
            ContraptionStructure.BlockEntry e = structure.entries.get(i);
            BlockPos rp = e.relPos;
            IBlockState st = e.state;
            Block b = st.getBlock();

            TextureAtlasSprite sp = shapes.getTexture(st);

            if (sp == null)
            {
                sp = CreateMeshTemplate.resolveSprite(CreateMeshTemplate.SpriteKey.PLANKS_OAK);
            }

            float u0 = sp.getMinU();
            float u1 = sp.getMaxU();
            float v0 = sp.getMinV();
            float v1 = sp.getMaxV();

            float minX = (float)rp.getX() - 0.5F;
            float maxX = (float)rp.getX() + 0.5F;
            float minY = (float)rp.getY();
            float maxY = (float)rp.getY() + 1.0F;
            float minZ = (float)rp.getZ() - 0.5F;
            float maxZ = (float)rp.getZ() + 0.5F;

            // Slight brass/dark tint for Create train bogeys/controls inside carriage mesh
            float tintR = 1.0F;
            float tintG = 1.0F;
            float tintB = 1.0F;

            if (b == CreateBlocks.TRAIN_BOGEY)
            {
                minY += 0.1F;
                maxY -= 0.25F;
                tintR = 0.45F; tintG = 0.47F; tintB = 0.52F;
            }
            else if (b == CreateBlocks.TRAIN_CONTROLS)
            {
                maxY -= 0.15F;
                tintR = 0.92F; tintG = 0.76F; tintB = 0.36F;
            }

            // Cull internal hidden faces between solid neighbor blocks inside the carriage!
            if (!occupied.contains(rp.down()))
            {
                emitQuad(buf, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, u0, u1, v0, v1, tintR * 0.60F, tintG * 0.60F, tintB * 0.60F);
                ++emittedQuads;
            }

            if (!occupied.contains(rp.up()))
            {
                emitQuad(buf, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, u0, u1, v0, v1, tintR, tintG, tintB);
                ++emittedQuads;
            }

            if (!occupied.contains(rp.north()))
            {
                emitQuad(buf, maxX, maxY, minZ, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, u0, u1, v0, v1, tintR * 0.85F, tintG * 0.85F, tintB * 0.85F);
                ++emittedQuads;
            }

            if (!occupied.contains(rp.south()))
            {
                emitQuad(buf, minX, maxY, maxZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, u0, u1, v0, v1, tintR * 0.85F, tintG * 0.85F, tintB * 0.85F);
                ++emittedQuads;
            }

            if (!occupied.contains(rp.west()))
            {
                emitQuad(buf, minX, maxY, minZ, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, u0, u1, v0, v1, tintR * 0.75F, tintG * 0.75F, tintB * 0.75F);
                ++emittedQuads;
            }

            if (!occupied.contains(rp.east()))
            {
                emitQuad(buf, maxX, maxY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, u0, u1, v0, v1, tintR * 0.75F, tintG * 0.75F, tintB * 0.75F);
                ++emittedQuads;
            }
        }

        buf.finishDrawing();

        if (emittedQuads == 0)
        {
            this.empty = true;
            return;
        }

        this.empty = false;

        if (OpenGlHelper.useVbo())
        {
            this.gpuVbo = new VertexBuffer(DefaultVertexFormats.POSITION_TEX_COLOR);
            this.gpuVbo.bufferData(buf.getByteBuffer());
        }
        else
        {
            this.fallbackState = buf.getVertexState();
        }
    }

    private static void emitQuad(
        BufferBuilder buf,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float u0, float u1, float v0, float v1,
        float r, float g, float b)
    {
        buf.pos((double)x0, (double)y0, (double)z0).tex((double)u0, (double)v0).color(r, g, b, 1.0F).endVertex();
        buf.pos((double)x1, (double)y1, (double)z1).tex((double)u0, (double)v1).color(r, g, b, 1.0F).endVertex();
        buf.pos((double)x2, (double)y2, (double)z2).tex((double)u1, (double)v1).color(r, g, b, 1.0F).endVertex();
        buf.pos((double)x3, (double)y3, (double)z3).tex((double)u1, (double)v0).color(r, g, b, 1.0F).endVertex();
    }

    public void dispose()
    {
        if (this.gpuVbo != null)
        {
            this.gpuVbo.deleteGlBuffers();
            this.gpuVbo = null;
        }

        this.fallbackState = null;
        this.built = false;
        this.empty = true;
    }
}
