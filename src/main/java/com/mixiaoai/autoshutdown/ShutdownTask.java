package com.mixiaoai.autoshutdown;

import com.mixiaoai.autoshutdown.util.ServerUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Singleton that acts as a timer task and an event handler for daily shutdown.
 *
 * The use of a tick handler ensures the shutdown process is run in the main thread,
 * to prevent issues with cross-thread contamination. As the handler runs 20 times a
 * second, the event is just a boolean check. This means the scheduled task's role is
 * to unlock the tick handler. Platform modules provide the tick wiring.
 */
public abstract class ShutdownTask extends AutoShutdownTask<ShutdownTask>
{
    static final Format DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private boolean registered = false;

    /** Creates a timer task to run at the configured time of day */
    protected static <T extends ShutdownTask> T create(Class<T> type, T task, MinecraftServer server)
    {
        AutoShutdownTask.create(type, task, server);

        Calendar shutdownAt = Calendar.getInstance();

        if (Config.scheduleUptime.get())
        {
            shutdownAt.add(Calendar.HOUR_OF_DAY, Config.scheduleHour.get());
            shutdownAt.add(Calendar.MINUTE, Config.scheduleMinute.get());
        }
        else
        {
            shutdownAt.set(Calendar.HOUR_OF_DAY, Config.scheduleHour.get());
            shutdownAt.set(Calendar.MINUTE, Config.scheduleMinute.get());
            shutdownAt.set(Calendar.SECOND, 0);

            if (shutdownAt.before(Calendar.getInstance()))
                shutdownAt.add(Calendar.DAY_OF_MONTH, 1);
        }

        Date shutdownAtDate = shutdownAt.getTime();

        task.start("Auto Shutdown timer", shutdownAtDate, 60 * 1000);
        ShutdownMod.LOGGER.info("Next automatic shutdown: {}", DATE.format(shutdownAtDate));
        return task;
    }

    boolean executeTick = false;
    byte warningsLeft = (byte) Config.scheduleWarningCount.get().intValue();
    int delayMinutes = 0;

    /** Runs from the timer thread */
    @Override
    public void run()
    {
        if (!registered)
        {
            registerTickListener();
            registered = true;
        }

        executeTick = true;
        ShutdownMod.LOGGER.debug("Timer called; next ShutdownTask tick will run");
    }

    /** Registers this task to receive server-tick callbacks from the platform */
    protected abstract void registerTickListener();

    /** Runs from the main server thread, called by the platform tick handler */
    protected void onTick()
    {
        if (!executeTick)
            return;
        else
            executeTick = false;

        if (Config.scheduleDelay.get() && performDelay())
        {
            ShutdownMod.LOGGER.debug("ShutdownTask ticked; {} minute(s) of delay to go", delayMinutes);
            delayMinutes--;
            return;
        }

        if (Config.scheduleWarning.get() && warningsLeft > 0)
        {
            performWarning();
            ShutdownMod.LOGGER.debug("ShutdownTask ticked; {} warning(s) to go", warningsLeft);
        }
        else
        {
            ServerUtil.shutdown(server, Component.literal(Config.msgKick.get()));
        }
    }

    private boolean performDelay()
    {
        if (delayMinutes > 0)
            return true;

        if (!ServerUtil.hasRealPlayers(server))
            return false;

        warningsLeft = (byte) Config.scheduleWarningCount.get().intValue();
        delayMinutes += Config.scheduleDelayBy.get();
        ShutdownMod.LOGGER.info("Shutdown delayed by {} minutes; server is not empty", delayMinutes);
        return true;
    }

    private void performWarning()
    {
        String warning = Config.msgWarn.get().replace("%m", Byte.toString(warningsLeft));

        ServerUtil.toAll(server, Component.literal("*** " + warning));
        ShutdownMod.LOGGER.info(warning);
        warningsLeft--;
    }
}