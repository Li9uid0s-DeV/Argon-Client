package net.minecraft.argon.create;

import net.minecraft.world.World;

public final class CreateWorldManager
{
    private CreateWorldManager()
    {
    }

    public static void tickWorld(World world)
    {
        if (world != null)
        {
            KineticNetwork.processPendingUpdates(world);
        }
    }
}
