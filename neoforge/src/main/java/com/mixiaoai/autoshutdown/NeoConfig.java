package com.mixiaoai.autoshutdown;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Builds the NeoForge config spec and binds its values to the common Config holders.
 */
class NeoConfig
{
    private static final String SCHEDULE = "Schedule";
    private static final String VOTING = "Voting";
    private static final String WATCHDOG = "Watchdog";
    private static final String IDLE_SHUTDOWN = "IdleShutdown";
    private static final String MESSAGES = "Messages";

    static final ModConfigSpec SPEC;

    static final ModConfigSpec.BooleanValue scheduleEnabled;
    static final ModConfigSpec.BooleanValue scheduleWarning;
    static final ModConfigSpec.IntValue scheduleWarningCount;
    static final ModConfigSpec.BooleanValue scheduleDelay;
    static final ModConfigSpec.BooleanValue scheduleUptime;
    static final ModConfigSpec.IntValue scheduleHour;
    static final ModConfigSpec.IntValue scheduleMinute;
    static final ModConfigSpec.IntValue scheduleDelayBy;

    static final ModConfigSpec.BooleanValue voteEnabled;
    static final ModConfigSpec.IntValue voteInterval;
    static final ModConfigSpec.IntValue minVoters;
    static final ModConfigSpec.IntValue maxNoVotes;

    static final ModConfigSpec.BooleanValue watchdogEnabled;
    static final ModConfigSpec.BooleanValue attemptSoftKill;
    static final ModConfigSpec.IntValue watchdogInterval;
    static final ModConfigSpec.IntValue maxTickTimeout;
    static final ModConfigSpec.IntValue lowTPSThreshold;
    static final ModConfigSpec.IntValue lowTPSTimeout;

    static final ModConfigSpec.BooleanValue idleShutdownEnabled;
    static final ModConfigSpec.IntValue idleCheckStartHour;
    static final ModConfigSpec.IntValue idleCheckStartMinute;
    static final ModConfigSpec.IntValue idleCheckEndHour;
    static final ModConfigSpec.IntValue idleCheckEndMinute;
    static final ModConfigSpec.IntValue idleTimeout;
    static final ModConfigSpec.IntValue idleCheckInterval;

    static final ModConfigSpec.ConfigValue<String> msgWarn;
    static final ModConfigSpec.ConfigValue<String> msgKick;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("All times are 24 hour (military) format, relative to machine's local time")
            .push(SCHEDULE);

        scheduleEnabled = builder.define("Enabled", true);
        scheduleWarning = builder.define("Warnings", true);
        scheduleWarningCount = builder.defineInRange("WarningCount", 5, 1, 60);
        scheduleDelay = builder.define("Delay", false);
        scheduleUptime = builder.define("Uptime", false);
        scheduleHour = builder.defineInRange("Hour", 5, 0, 720);
        scheduleMinute = builder.defineInRange("Minute", 0, 0, 59);
        scheduleDelayBy = builder.defineInRange("DelayBy", 5, 1, 1440);
        builder.pop();

        builder.comment("Allows players to shut down the server without admin intervention")
            .push(VOTING);

        voteEnabled = builder.define("Enabled", false);
        voteInterval = builder.defineInRange("VoteInterval", 15, 0, 1440);
        minVoters = builder.defineInRange("MinVoters", 4, 1, 999);
        maxNoVotes = builder.defineInRange("MaxNoVotes", 2, 1, 999);
        builder.pop();

        builder.comment(
            "Monitors the server and tries to kill it if unresponsive. " +
            "USE AT RISK: May corrupt data if killed before or during save"
        ).push(WATCHDOG);

        watchdogEnabled = builder.define("Enabled", false);
        attemptSoftKill = builder.define("AttemptSoftKill", true);
        watchdogInterval = builder.defineInRange("Interval", 10, 1, 3600);
        maxTickTimeout = builder.defineInRange("Timeout", 40, 1, 3600);
        lowTPSThreshold = builder.defineInRange("LowTPSThreshold", 10, 0, 19);
        lowTPSTimeout = builder.defineInRange("LowTPSTimeout", 30, 1, 3600);
        builder.pop();

        builder.comment(
            "Automatically shuts down the server when no players are online during specified time period"
        ).push(IDLE_SHUTDOWN);

        idleShutdownEnabled = builder.define("Enabled", false);
        idleCheckStartHour = builder.defineInRange("StartHour", 0, 0, 23);
        idleCheckStartMinute = builder.defineInRange("StartMinute", 0, 0, 59);
        idleCheckEndHour = builder.defineInRange("EndHour", 23, 0, 23);
        idleCheckEndMinute = builder.defineInRange("EndMinute", 59, 0, 59);
        idleTimeout = builder.defineInRange("IdleTimeout", 30, 1, 1440);
        idleCheckInterval = builder.defineInRange("CheckInterval", 1, 1, 60);
        builder.pop();

        builder.comment("Customizable messages for the shutdown process")
            .push(MESSAGES);

        msgWarn = builder.define("Warn", "Server is shutting down in %m minute(s).");
        msgKick = builder.define("Kick", "Scheduled server shutdown");
        builder.pop();

        SPEC = builder.build();
    }

    /** Binds the spec values to the common Config holders */
    static void init()
    {
        Config.scheduleEnabled.bind(scheduleEnabled, scheduleEnabled::set);
        Config.scheduleWarning.bind(scheduleWarning, scheduleWarning::set);
        Config.scheduleWarningCount.bind(scheduleWarningCount, scheduleWarningCount::set);
        Config.scheduleDelay.bind(scheduleDelay, scheduleDelay::set);
        Config.scheduleUptime.bind(scheduleUptime, scheduleUptime::set);
        Config.scheduleHour.bind(scheduleHour, scheduleHour::set);
        Config.scheduleMinute.bind(scheduleMinute, scheduleMinute::set);
        Config.scheduleDelayBy.bind(scheduleDelayBy, scheduleDelayBy::set);

        Config.voteEnabled.bind(voteEnabled, voteEnabled::set);
        Config.voteInterval.bind(voteInterval, voteInterval::set);
        Config.minVoters.bind(minVoters, minVoters::set);
        Config.maxNoVotes.bind(maxNoVotes, maxNoVotes::set);

        Config.watchdogEnabled.bind(watchdogEnabled, watchdogEnabled::set);
        Config.attemptSoftKill.bind(attemptSoftKill, attemptSoftKill::set);
        Config.watchdogInterval.bind(watchdogInterval, watchdogInterval::set);
        Config.maxTickTimeout.bind(maxTickTimeout, maxTickTimeout::set);
        Config.lowTPSThreshold.bind(lowTPSThreshold, lowTPSThreshold::set);
        Config.lowTPSTimeout.bind(lowTPSTimeout, lowTPSTimeout::set);

        Config.idleShutdownEnabled.bind(idleShutdownEnabled, idleShutdownEnabled::set);
        Config.idleCheckStartHour.bind(idleCheckStartHour, idleCheckStartHour::set);
        Config.idleCheckStartMinute.bind(idleCheckStartMinute, idleCheckStartMinute::set);
        Config.idleCheckEndHour.bind(idleCheckEndHour, idleCheckEndHour::set);
        Config.idleCheckEndMinute.bind(idleCheckEndMinute, idleCheckEndMinute::set);
        Config.idleTimeout.bind(idleTimeout, idleTimeout::set);
        Config.idleCheckInterval.bind(idleCheckInterval, idleCheckInterval::set);

        Config.msgWarn.bind(msgWarn, msgWarn::set);
        Config.msgKick.bind(msgKick, msgKick::set);
    }

    private NeoConfig() { }
}