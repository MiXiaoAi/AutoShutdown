package com.mixiaoai.autoshutdown;

import org.apache.logging.log4j.Logger;

/**
 * Static container class for the mod's configuration values.
 * The platform module binds each value to its own config spec.
 */
class Config
{
    private static final String SCHEDULE = "Schedule";
    private static final String VOTING = "Voting";
    private static final String WATCHDOG = "Watchdog";
    private static final String IDLE_SHUTDOWN = "IdleShutdown";
    private static final String MESSAGES = "Messages";

    static final ConfigVal<String> language = new ConfigVal<>("en_us");
    static final ConfigVal<Boolean> scheduleEnabled = new ConfigVal<>(true);
    static final ConfigVal<Boolean> scheduleWarning = new ConfigVal<>(true);
    static final ConfigVal<Integer> scheduleWarningCount = new ConfigVal<>(5);
    static final ConfigVal<Boolean> scheduleDelay = new ConfigVal<>(false);
    static final ConfigVal<Boolean> scheduleUptime = new ConfigVal<>(false);
    static final ConfigVal<Integer> scheduleHour = new ConfigVal<>(5);
    static final ConfigVal<Integer> scheduleMinute = new ConfigVal<>(0);
    static final ConfigVal<Integer> scheduleDelayBy = new ConfigVal<>(5);

    static final ConfigVal<Boolean> voteEnabled = new ConfigVal<>(false);
    static final ConfigVal<Integer> voteInterval = new ConfigVal<>(15);
    static final ConfigVal<Integer> minVoters = new ConfigVal<>(4);
    static final ConfigVal<Integer> maxNoVotes = new ConfigVal<>(2);

    static final ConfigVal<Boolean> watchdogEnabled = new ConfigVal<>(false);
    static final ConfigVal<Boolean> attemptSoftKill = new ConfigVal<>(true);
    static final ConfigVal<Integer> watchdogInterval = new ConfigVal<>(10);
    static final ConfigVal<Integer> maxTickTimeout = new ConfigVal<>(40);
    static final ConfigVal<Integer> lowTPSThreshold = new ConfigVal<>(10);
    static final ConfigVal<Integer> lowTPSTimeout = new ConfigVal<>(30);

    static final ConfigVal<Boolean> idleShutdownEnabled = new ConfigVal<>(false);
    static final ConfigVal<Integer> idleCheckStartHour = new ConfigVal<>(0);
    static final ConfigVal<Integer> idleCheckStartMinute = new ConfigVal<>(0);
    static final ConfigVal<Integer> idleCheckEndHour = new ConfigVal<>(23);
    static final ConfigVal<Integer> idleCheckEndMinute = new ConfigVal<>(59);
    static final ConfigVal<Integer> idleTimeout = new ConfigVal<>(30);
    static final ConfigVal<Integer> idleCheckInterval = new ConfigVal<>(1);

    static final ConfigVal<String> msgWarn = new ConfigVal<>("Server is shutting down in %m minute(s).");
    static final ConfigVal<String> msgKick = new ConfigVal<>("Scheduled server shutdown");

    private Config() { }

    /**
     * Checks the loaded configuration and makes adjustments based on other config
     */
    static void validate()
    {
        Logger logger = ShutdownMod.LOGGER;

        int hour = scheduleHour.get();
        int minute = scheduleMinute.get();

        if (!scheduleUptime.get() && hour >= 24)
        {
            logger.warn("Uptime shutdown is disabled, but the shutdown hour is more " +
                "than 23! Please fix this in the config. It will be set to 00 hours.");
            scheduleHour.set(0);
        }

        if (scheduleUptime.get() && hour == 0 && minute == 0)
        {
            logger.warn("Uptime shutdown is enabled, but is set to shutdown after " +
                "0 hours and 0 minutes of uptime! Please fix this in the config. " +
                "It will be set to 24 hours.");
            scheduleHour.set(24);
        }
    }

    static boolean isNothingEnabled()
    {
        return !scheduleEnabled.get() && !voteEnabled.get() && !watchdogEnabled.get() && !idleShutdownEnabled.get();
    }
}
