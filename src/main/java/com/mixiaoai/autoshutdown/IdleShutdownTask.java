package com.mixiaoai.autoshutdown;

import com.mixiaoai.autoshutdown.util.ServerUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Calendar;

/**
 * Monitors server for idle players during specified time period and shuts down after timeout
 */
public class IdleShutdownTask extends AutoShutdownTask<IdleShutdownTask>
{
    private int idleMinutes = 0;
    private boolean wasEmpty = false;

    public static void create(MinecraftServer server)
    {
        IdleShutdownTask task = AutoShutdownTask.create(IdleShutdownTask.class, new IdleShutdownTask(), server);

        int intervalMs = Config.idleCheckInterval.get() * 60 * 1000;
        task.start("Auto Shutdown idle checker", intervalMs, intervalMs);
        ShutdownMod.LOGGER.info("Idle shutdown monitor started. Active from {}:{:02d} to {}:{:02d}, timeout: {} minutes",
            Config.idleCheckStartHour.get(),
            Config.idleCheckStartMinute.get(),
            Config.idleCheckEndHour.get(),
            Config.idleCheckEndMinute.get(),
            Config.idleTimeout.get());
    }

    @Override
    public void run()
    {
        if (!isWithinActiveTime())
        {
            // Reset idle counter when outside active time
            if (idleMinutes > 0)
            {
                ShutdownMod.LOGGER.debug("Outside active time period, resetting idle counter");
                idleMinutes = 0;
                wasEmpty = false;
            }
            return;
        }

        boolean isEmpty = !ServerUtil.hasRealPlayers(server);

        if (isEmpty)
        {
            if (!wasEmpty)
            {
                // Server just became empty
                wasEmpty = true;
                idleMinutes = Config.idleCheckInterval.get();
                ShutdownMod.LOGGER.info("Server is now empty. Will shutdown after {} minutes of idle time", Config.idleTimeout.get());
            }
            else
            {
                // Server continues to be empty
                idleMinutes += Config.idleCheckInterval.get();
                ShutdownMod.LOGGER.debug("Server idle for {} minutes (timeout: {})", idleMinutes, Config.idleTimeout.get());

                if (idleMinutes >= Config.idleTimeout.get())
                {
                    ShutdownMod.LOGGER.info("Server has been idle for {} minutes. Initiating shutdown...", idleMinutes);
                    ServerUtil.shutdown(server, ServerUtil.localized("auto_shutdown.msg.idleshutdown",
                        "Server shutdown due to inactivity"));
                }
            }
        }
        else
        {
            // Server has players
            if (wasEmpty && idleMinutes > 0)
            {
                ShutdownMod.LOGGER.info("Players detected, resetting idle counter (was idle for {} minutes)", idleMinutes);
            }
            idleMinutes = 0;
            wasEmpty = false;
        }
    }

    private boolean isWithinActiveTime()
    {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        int startHour = Config.idleCheckStartHour.get();
        int startMinute = Config.idleCheckStartMinute.get();
        int endHour = Config.idleCheckEndHour.get();
        int endMinute = Config.idleCheckEndMinute.get();

        int currentTime = currentHour * 60 + currentMinute;
        int startTime = startHour * 60 + startMinute;
        int endTime = endHour * 60 + endMinute;

        if (startTime <= endTime)
        {
            // Normal case: start time is before end time (e.g., 08:00 to 23:00)
            return currentTime >= startTime && currentTime <= endTime;
        }
        else
        {
            // Crosses midnight (e.g., 22:00 to 02:00)
            return currentTime >= startTime || currentTime <= endTime;
        }
    }

    private IdleShutdownTask() { }
}