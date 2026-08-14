package com.mixiaoai.autoshutdown;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge implementation of the scheduled shutdown task.
 */
public class ForgeShutdownTask extends ShutdownTask
{
    public static void create(MinecraftServer server)
    {
        ShutdownTask.create(ForgeShutdownTask.class, new ForgeShutdownTask(), server);
    }

    @Override
    protected void registerTickListener()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            return;

        onTick();
    }

    private ForgeShutdownTask() { }
}