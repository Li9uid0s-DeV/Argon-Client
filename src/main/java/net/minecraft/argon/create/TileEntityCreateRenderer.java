package net.minecraft.argon.create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class TileEntityCreateRenderer extends TileEntitySpecialRenderer<TileEntityCreateBase>
{
    @Override
    public void render(TileEntityCreateBase te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
    {
        if (te == null || te.getWorld() == null)
        {
            return;
        }

        // Distance culling at 64 blocks to keep Kompanio 500 at 60 FPS
        double distSq = x * x + y * y + z * z;
        if (distSq > 64.0D * 64.0D)
        {
            return;
        }

        BlockPos pos = te.getPos();
        float light = Math.max(0.35F, te.getWorld().getLightBrightness(pos));

        if (te instanceof TileEntityKinetic)
        {
            ((TileEntityKinetic)te).queueFlywheelInstances(partialTicks, light);

            if (te instanceof TileEntityBelt)
            {
                this.renderBeltItem((TileEntityBelt)te, x, y, z);
            }
            else if (te instanceof TileEntityMechanicalPiston)
            {
                TileEntityMechanicalPiston piston = (TileEntityMechanicalPiston)te;
                ContraptionStructure payload = piston.getActivePayload();

                if (payload != null && !payload.entries.isEmpty())
                {
                    float ext = piston.getInterpolatedOffset(partialTicks);
                    EnumFacing dir = piston.getFacing();
                    double offX = (double)dir.getFrontOffsetX() * (double)ext;
                    double offY = (double)dir.getFrontOffsetY() * (double)ext;
                    double offZ = (double)dir.getFrontOffsetZ() * (double)ext;
                    piston.getMeshCache().render(
                        payload,
                        x + 0.5D + (double)dir.getFrontOffsetX() + offX,
                        y + (double)dir.getFrontOffsetY() + offY,
                        z + 0.5D + (double)dir.getFrontOffsetZ() + offZ,
                        0.0F,
                        light);
                }
            }
            else if (te instanceof TileEntityGantryCarriage)
            {
                TileEntityGantryCarriage gantry = (TileEntityGantryCarriage)te;
                ContraptionStructure payload = gantry.getActivePayload();

                if (payload != null && !payload.entries.isEmpty())
                {
                    float s = gantry.getInterpolatedSlide(partialTicks);
                    EnumFacing sd = gantry.getSlideDirection();
                    gantry.getMeshCache().render(
                        payload,
                        x + 0.5D + (double)sd.getFrontOffsetX() * (double)s,
                        y + (double)sd.getFrontOffsetY() * (double)s,
                        z + 0.5D + (double)sd.getFrontOffsetZ() * (double)s,
                        0.0F,
                        light);
                }
            }

            return;
        }

        if (te instanceof TileEntityFunnelChute)
        {
            TileEntityFunnelChute fc = (TileEntityFunnelChute)te;
            if (fc.getKind() == TileEntityFunnelChute.KIND_CHUTE)
            {
                FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.CHUTE, pos, EnumFacing.Axis.Y, 0.0F, light);
            }
            else
            {
                EnumFacing face = fc.getFacing();
                float yaw = face != null ? face.getHorizontalAngle() : 0.0F;
                FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.FUNNEL, pos, EnumFacing.Axis.Y, yaw, light);
            }
        }
        else if (te instanceof TileEntityBasin)
        {
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.BASIN, pos, EnumFacing.Axis.Y, 0.0F, light);
            this.renderBasinContents((TileEntityBasin)te, x, y, z);
        }
        else if (te instanceof TileEntityTrainTrack)
        {
            ((TileEntityTrainTrack)te).queueRailBatches();
        }
        else if (te instanceof TileEntityTrainStation)
        {
            FlywheelBatchRenderer.enqueue(FlywheelBatchRenderer.ModelId.TRAIN_STATION, pos, EnumFacing.Axis.Y, 0.0F, light);
        }
        else if (te instanceof TileEntityTrainDecor)
        {
            TileEntityTrainDecor td = (TileEntityTrainDecor)te;
            float yaw = td.getFacing() != null ? td.getFacing().getHorizontalAngle() : 0.0F;
            FlywheelBatchRenderer.enqueue(
                td.getKind() == TileEntityTrainDecor.KIND_CONTROLS
                    ? FlywheelBatchRenderer.ModelId.TRAIN_CONTROLS
                    : FlywheelBatchRenderer.ModelId.TRAIN_BOGEY,
                pos, EnumFacing.Axis.Y, yaw, light);
        }
    }

    private void renderBeltItem(TileEntityBelt belt, double x, double y, double z)
    {
        ItemStack stack = belt.getStoredStack();

        if (stack.isEmpty())
        {
            return;
        }

        EnumFacing moveDir = belt.getSpeedRPM() >= 0.0F ? belt.getFacing() : belt.getFacing().getOpposite();
        double offset = (double)belt.getItemProgress() - 0.5D;
        double ix = x + 0.5D + (double)moveDir.getFrontOffsetX() * offset;
        double iy = y + 0.62D;
        double iz = z + 0.5D + (double)moveDir.getFrontOffsetZ() * offset;

        GlStateManager.pushMatrix();
        GlStateManager.translate(ix, iy, iz);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void renderBasinContents(TileEntityBasin basin, double x, double y, double z)
    {
        int shown = 0;

        for (int i = 0; i < basin.getItems().size() && shown < 4; ++i)
        {
            ItemStack s = basin.getItems().get(i);

            if (!s.isEmpty())
            {
                double ox = ((shown & 1) == 0 ? -0.16D : 0.16D);
                double oz = ((shown & 2) == 0 ? -0.16D : 0.16D);
                GlStateManager.pushMatrix();
                GlStateManager.translate(x + 0.5D + ox, y + 0.35D, z + 0.5D + oz);
                GlStateManager.scale(0.35F, 0.35F, 0.35F);
                RenderHelper.enableStandardItemLighting();
                Minecraft.getMinecraft().getRenderItem().renderItem(s, ItemCameraTransforms.TransformType.FIXED);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.popMatrix();
                ++shown;
            }
        }
    }

    @Override
    public boolean isGlobalRenderer(TileEntityCreateBase te)
    {
        return te instanceof TileEntityTrainTrack
            || te instanceof TileEntityMechanicalPiston
            || te instanceof TileEntityGantryCarriage
            || te instanceof TileEntityWindmillBearing;
    }
}
