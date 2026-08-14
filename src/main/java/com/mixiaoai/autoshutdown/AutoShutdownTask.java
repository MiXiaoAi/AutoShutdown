package com.mixiaoai.autoshutdown;

import net.minecraft.server.MinecraftServer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Base class for the mod's background timer tasks.
 * Owns the singleton lifecycle, the daemon timer and the cancellation plumbing;
 * subclasses run their own logic on each timer tick.
 */
public abstract class AutoShutdownTask<T extends AutoShutdownTask<T>> extends TimerTask
{
    private static final Map<Class<?>, AutoShutdownTask<?>> ACTIVE = new HashMap<>();

    protected MinecraftServer server;
    private Timer timer;

    /**
     * Creates or replaces the singleton instance of type T and registers it as the active task.
     * @return the newly created task instance
     */
    protected static <T extends AutoShutdownTask<?>> T create(Class<T> type, T instance, MinecraftServer server)
    {
        AutoShutdownTask<?> existing = ACTIVE.get(type);
        if (existing != null)
        {
            ShutdownMod.LOGGER.warn("{} already exists, stopping old instance", type.getSimpleName());
            existing.dispose();
        }

        instance.server = server;
        ACTIVE.put(type, instance);
        return instance;
    }

    /** Stops and unregisters the active task of the given type, if any */
    protected static void stop(Class<?> type)
    {
        AutoShutdownTask<?> existing = ACTIVE.get(type);
        if (existing != null)
        {
            ShutdownMod.LOGGER.debug("{} stopped", type.getSimpleName());
            existing.dispose();
        }
    }

    /** Starts this task on a daemon timer after delayMs, repeating every periodMs */
    protected void start(String timerName, long delayMs, long periodMs)
    {
        timer = new Timer(timerName, true);
        timer.schedule(this, delayMs, periodMs);
    }

    /** Starts this task on a daemon timer at firstTime, repeating every periodMs */
    protected void start(String timerName, Date firstTime, long periodMs)
    {
        timer = new Timer(timerName, true);
        timer.schedule(this, firstTime, periodMs);
    }

    /** Cancels the daemon timer and this task, then unregisters it */
    public void dispose()
    {
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }

        cancel();
        ACTIVE.remove(getClass());
    }
}