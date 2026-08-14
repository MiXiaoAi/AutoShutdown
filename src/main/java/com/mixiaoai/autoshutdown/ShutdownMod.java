package com.mixiaoai.autoshutdown;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Common entry point and static state for the mod.
 * Platform modules (@Mod classes) drive this class at load time.
 */
public class ShutdownMod
{
    public static final String MODID = "auto_shutdown";
    public static final Logger LOGGER = LogManager.getLogger();

    private ShutdownMod() { }

    /** Called when the server starts; validates config and starts enabled tasks */
    public static void onServerStarting(MinecraftServer server)
    {
        Config.validate();

        if (Config.isNothingEnabled())
        {
            LOGGER.warn("It appears no Auto Shutdown features are enabled.");
            LOGGER.warn("Please check the config at `world/serverconfig/auto_shutdown-server.toml`.");
            return;
        }

        startAllTasks(server);
    }

    /**
     * Reloads the configuration and restarts all tasks.
     * @param server The Minecraft server instance
     * @return true if reload was successful
     */
    public static boolean reload(MinecraftServer server)
    {
        try
        {
            // Stop existing tasks
            stopAllTasks();

            // Force reload config spec
            LOGGER.info("Reloading configuration from file...");
            Platform.get().afterReload();

            // Validate new config
            Config.validate();

            // Check if anything is enabled
            if (Config.isNothingEnabled())
            {
                LOGGER.warn("No Auto Shutdown features are enabled after reload.");
                return true; // Not an error, just a warning
            }

            // Restart tasks based on new config
            startAllTasks(server);
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to reload configuration", e);
            return false;
        }
    }

    /** Starts every task whose feature is enabled in the configuration */
    private static void startAllTasks(MinecraftServer server)
    {
        if (Config.scheduleEnabled.get())
            Platform.get().createShutdownTask(server);

        if (Config.watchdogEnabled.get())
            WatchdogTask.create(server);

        if (Config.idleShutdownEnabled.get())
            IdleShutdownTask.create(server);
    }

    /** Stops all running tasks */
    private static void stopAllTasks()
    {
        LOGGER.info("Stopping all tasks...");

        stopTask(ShutdownTask.class);
        stopTask(WatchdogTask.class);
        stopTask(IdleShutdownTask.class);
    }

    /** Stops a task type, tolerating failure of an individual task */
    private static void stopTask(Class<?> type)
    {
        try
        {
            AutoShutdownTask.stop(type);
        }
        catch (Exception e)
        {
            LOGGER.warn("Error stopping " + type.getSimpleName(), e);
        }
    }
}
