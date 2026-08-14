package com.mixiaoai.autoshutdown;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Platform abstraction, implemented by the loader-specific module.
 */
public interface Platform
{
    static Platform get()
    {
        return Holder.INSTANCE;
    }

    static void set(Platform platform)
    {
        Holder.INSTANCE = platform;
    }

    final class Holder
    {
        private static Platform INSTANCE;
    }

    /** Whether the given player is a real (non-fake) player */
    boolean isRealPlayer(ServerPlayer player);

    /** Clears the platform config's caches after a reload */
    void afterReload();

    /** Creates the platform's scheduled shutdown task */
    void createShutdownTask(MinecraftServer server);

    /** Average server tick time in nanoseconds */
    double getAverageTickTimeNanos(MinecraftServer server);
}
