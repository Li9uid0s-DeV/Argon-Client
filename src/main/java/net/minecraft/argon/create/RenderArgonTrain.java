package net.minecraft.argon.create;

import java.util.List;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class RenderArgonTrain extends Render<EntityArgonTrain>
{
    public RenderArgonTrain(RenderManager renderManagerIn)
    {
        super(renderManagerIn);
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntityArgonTrain entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        List<EntityArgonTrain.Carriage> cars = entity.getCarriages();

        if (cars.isEmpty())
        {
            return;
        }

        double trainX = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTicks;
        double trainY = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTicks;
        double trainZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTicks;

        for (int i = 0; i < cars.size(); ++i)
        {
            EntityArgonTrain.Carriage car = cars.get(i);
            double cx = car.prevX + (car.x - car.prevX) * (double)partialTicks;
            double cy = car.prevY + (car.y - car.prevY) * (double)partialTicks;
            double cz = car.prevZ + (car.z - car.prevZ) * (double)partialTicks;
            float cyaw = car.prevYaw + (car.yaw - car.prevYaw) * partialTicks;

            double relX = x + (cx - trainX);
            double relY = y + (cy - trainY);
            double relZ = z + (cz - trainZ);

            BlockPos samplePos = new BlockPos(cx, cy + 0.5D, cz);
            float light = 0.9F;

            if (entity.world != null && entity.world.isBlockLoaded(samplePos))
            {
                light = Math.max(0.35F, entity.world.getLightBrightness(samplePos));
            }

            // 1 cached VBO draw call per carriage -> 60 FPS for full 10-car trains!
            car.meshCache.render(car.structure, relX, relY, relZ, -cyaw, light);
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityArgonTrain entity)
    {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
