package com.mixiaoai.autoshutdown;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Forge implementation of the platform abstraction.
 */
public class ForgePlatform implements Platform
{
    @Override
    public boolean isRealPlayer(ServerPlayer player)
    {
        return !(player instanceof FakePlayer);
    }

    @Override
    public void afterReload()
    {
        ForgeConfig.SPEC.afterReload();
    }

    @Override
    public void createShutdownTask(MinecraftServer server)
    {
        ForgeShutdownTask.create(server);
    }

    @Override
    public double getAverageTickTimeNanos(MinecraftServer server)
    {
        return server.getAverageTickTime() * 1_000_000.0;
    }
}