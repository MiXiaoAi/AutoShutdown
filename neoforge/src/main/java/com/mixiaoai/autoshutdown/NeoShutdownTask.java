package com.mixiaoai.autoshutdown;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge implementation of the scheduled shutdown task.
 */
public class NeoShutdownTask extends ShutdownTask
{
    public static void create(MinecraftServer server)
    {
        ShutdownTask.create(NeoShutdownTask.class, new NeoShutdownTask(), server);
    }

    @Override
    protected void registerTickListener()
    {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(ServerTickEvent.Pre event)
    {
        onTick();
    }

    private NeoShutdownTask() { }
}