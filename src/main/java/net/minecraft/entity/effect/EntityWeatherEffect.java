package net.minecraft.entity.effect;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public abstract class EntityWeatherEffect extends Entity
{
    public EntityWeatherEffect(World worldIn)
    {
        super(worldIn);
    }

    public boolean isLocalizedStorm()
    {
        return false;
    }

    public boolean isTornado()
    {
        return this.getEfScale() >= 1;
    }

    public int getEfScale()
    {
        return 0;
    }

    public float getStormIntensity()
    {
        return 0.0F;
    }

    public float getStormRadius()
    {
        return 0.0F;
    }
}

