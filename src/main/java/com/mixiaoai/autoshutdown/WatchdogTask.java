package com.mixiaoai.autoshutdown;

import net.minecraft.server.MinecraftServer;

/**
 * Singleton that acts as a timer task for monitoring server stalls
 */
public class WatchdogTask extends AutoShutdownTask<WatchdogTask>
{
    public static void create(MinecraftServer server)
    {
        WatchdogTask task = AutoShutdownTask.create(WatchdogTask.class, new WatchdogTask(), server);

        int intervalMs = Config.watchdogInterval.get() * 1000;
        task.start("Auto Shutdown watchdog", intervalMs, intervalMs);
        ShutdownMod.LOGGER.debug("Watchdog timer running");
    }

    private int lastTick = 0;
    private int hungTicks = 0;
    private int lagTicks = 0;

    private boolean isHanging = false;

    @Override
    public void run()
    {
        if (isHanging)
            doHanging();
        else
            doMonitor();
    }

    /** Checks if server is hung on a tick, then if TPS is too low for too long */
    private void doMonitor()
    {
        double averageTickTimeNanos = Platform.get().getAverageTickTimeNanos(server);
        double tps = averageTickTimeNanos <= 0 ? 20.0 : Math.min(1_000_000_000.0 / averageTickTimeNanos, 20.0);

        if (ShutdownMod.LOGGER.isTraceEnabled())
        {
            ShutdownMod.LOGGER.trace("Watchdog: 100 tick avg. latency: {} / 50 ms", averageTickTimeNanos / 1_000_000.0);
            ShutdownMod.LOGGER.trace("Watchdog: 100 tick avg. TPS: {} / 20", String.format("%.2f", tps));
        }

        int serverTick = server.getTickCount();
        if (serverTick == lastTick)
        {
            ShutdownMod.LOGGER.debug("No advance in server ticks; server is hanging");
            isHanging = true;
            hungTicks = 1;
            return;
        }
        else
            lastTick = serverTick;

        if (tps < Config.lowTPSThreshold.get())
        {
            lagTicks++;
            int lagSec = lagTicks * Config.watchdogInterval.get();
            ShutdownMod.LOGGER.trace("TPS too low since {} seconds", lagSec);

            if (lagSec >= Config.lowTPSTimeout.get())
            {
                ShutdownMod.LOGGER.warn(
                    "TPS below {} since {} seconds",
                    Config.lowTPSThreshold.get(),
                    lagSec
                );

                if (Config.attemptSoftKill.get())
                    performSoftKill();
                else
                    performHardKill();
            }
        }
        else
            lagTicks = 0;
    }

    /** Regular check of a hanging server; kills if confirmed hung */
    private void doHanging()
    {
        int serverTick = server.getTickCount();
        if (serverTick != lastTick)
        {
            ShutdownMod.LOGGER.debug("Server no longer hanging");
            isHanging = false;
            return;
        }

        hungTicks++;
        int hangSec = hungTicks * Config.watchdogInterval.get();
        ShutdownMod.LOGGER.trace("Server hanging for {} seconds", hangSec);

        if (hangSec >= Config.maxTickTimeout.get())
        {
            ShutdownMod.LOGGER.warn("Server is hung on a tick after {} seconds", hangSec);

            if (Config.attemptSoftKill.get())
                performSoftKill();
            else
                performHardKill();
        }
    }

    private void performSoftKill()
    {
        ShutdownMod.LOGGER.warn("Attempting a soft kill of the server...");

        Thread hardKillCheck = new Thread("Shutdown watchdog")
        {
            public void run()
            {
                try
                {
                    Thread.sleep(10000);
                    System.out.println("Hung during soft kill; trying a hard kill..");
                    performHardKill();
                }
                catch (InterruptedException ignored) { }
            }
        };

        hardKillCheck.setDaemon(true);
        hardKillCheck.start();

        server.halt(false);
    }

    private void performHardKill()
    {
        ShutdownMod.LOGGER.warn("Attempting a hard kill of the server - data may be lost!");
        Runtime.getRuntime().halt(1);
    }

    private WatchdogTask() { }
}