package com.mixiaoai.autoshutdown;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * NeoForge implementation of the platform abstraction.
 */
public class NeoPlatform implements Platform
{
    @Override
    public boolean isRealPlayer(ServerPlayer player)
    {
        return !(player instanceof FakePlayer);
    }

    @Override
    public void afterReload()
    {
        NeoConfig.SPEC.afterReload();
    }

    @Override
    public void createShutdownTask(MinecraftServer server)
    {
        NeoShutdownTask.create(server);
    }

    @Override
    public double getAverageTickTimeNanos(MinecraftServer server)
    {
        return server.getAverageTickTimeNanos();
    }
}