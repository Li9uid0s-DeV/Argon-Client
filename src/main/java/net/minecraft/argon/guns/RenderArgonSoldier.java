package net.minecraft.argon.guns;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Renders Techguns-style NPC Soldiers with aimed rifle arm pose and procedural
 * tactical helmet, visor, chestplate, and variant colors.
 */
public class RenderArgonSoldier extends RenderBiped<EntityArgonSoldier>
{
    private static final ResourceLocation SOLDIER_BASE_TEX = new ResourceLocation("textures/entity/steve.png");

    public RenderArgonSoldier(RenderManager renderManagerIn)
    {
        super(renderManagerIn, new ModelBiped(0.0F, 0.0F, 64, 64), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityArgonSoldier entity)
    {
        return SOLDIER_BASE_TEX;
    }

    @Override
    public void doRender(EntityArgonSoldier entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        ModelBiped model = (ModelBiped)this.getMainModel();
        model.rightArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
        model.leftArmPose = ModelBiped.ArmPose.EMPTY;
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected void renderLayers(EntityArgonSoldier entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn)
    {
        super.renderLayers(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scaleIn);
        this.renderTacticalArmorOverlay(entitylivingbaseIn);
    }

    private void renderTacticalArmorOverlay(EntityArgonSoldier soldier)
    {
        int variant = soldier.getSoldierVariant();
        float armorR = variant == EntityArgonSoldier.VARIANT_HEAVY ? 0.22F : (variant == EntityArgonSoldier.VARIANT_BREACHER ? 0.28F : 0.27F);
        float armorG = variant == EntityArgonSoldier.VARIANT_HEAVY ? 0.24F : (variant == EntityArgonSoldier.VARIANT_BREACHER ? 0.31F : 0.35F);
        float armorB = variant == EntityArgonSoldier.VARIANT_HEAVY ? 0.29F : (variant == EntityArgonSoldier.VARIANT_BREACHER ? 0.36F : 0.23F);

        float visorR = variant == EntityArgonSoldier.VARIANT_HEAVY ? 0.98F : (variant == EntityArgonSoldier.VARIANT_BREACHER ? 0.95F : 0.22F);
        float visorG = variant == EntityArgonSoldier.VARIANT_HEAVY ? 0.25F : (variant == EntityArgonSoldier.VARIANT_BREACHER ? 0.68F : 0.85F);
        float visorB = variant == EntityArgonSoldier.VARIANT_HEAVY ? 0.18F : (variant == EntityArgonSoldier.VARIANT_BREACHER ? 0.18F : 0.95F);

        ModelBiped model = (ModelBiped)this.getMainModel();

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();

        // Tactical helmet & glowing visor attached to head transform
        GlStateManager.pushMatrix();
        model.bipedHead.postRender(0.0625F);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GunModelRenderer.addBox(buf, -0.285F, -0.535F, -0.285F, 0.285F, -0.32F, 0.285F, armorR, armorG, armorB);
        GunModelRenderer.addBox(buf, -0.22F, -0.34F, -0.295F, 0.22F, -0.20F, -0.24F, visorR, visorG, visorB);
        tess.draw();
        GlStateManager.popMatrix();

        // Ballistic chest vest + tactical backpack attached to torso transform
        GlStateManager.pushMatrix();
        model.bipedBody.postRender(0.0625F);
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        GunModelRenderer.addBox(buf, -0.285F, 0.04F, -0.175F, 0.285F, 0.64F, 0.175F, armorR * 0.92F, armorG * 0.92F, armorB * 0.92F);
        GunModelRenderer.addBox(buf, -0.20F, 0.10F, 0.175F, 0.20F, 0.56F, 0.34F, armorR * 0.72F, armorG * 0.72F, armorB * 0.72F);
        tess.draw();
        GlStateManager.popMatrix();

        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
