package com.mixiaoai.autoshutdown;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mixiaoai.autoshutdown.util.ServerUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registers and handles all commands provided by the mod:
 * `/auto_shutdown` (reload & status) and `/shutdown` (player voting).
 */
public class ModCommands
{
    private static final Logger LOGGER = ShutdownMod.LOGGER;

    private static SimpleCommandExceptionType playersOnly()
    {
        return new SimpleCommandExceptionType(ServerUtil.localized(
            "auto_shutdown.error.playersonly", "*** Only players may use this command; try '/stop' instead"));
    }

    private static SimpleCommandExceptionType noVoteInProgress()
    {
        return new SimpleCommandExceptionType(ServerUtil.localized(
            "auto_shutdown.error.novoteinprogress", "*** No vote is in progress; try '/shutdown'"));
    }

    private static SimpleCommandExceptionType voteInProgress()
    {
        return new SimpleCommandExceptionType(ServerUtil.localized(
            "auto_shutdown.error.voteinprogress", "*** Vote in progress; try '/shutdown yes' or '/shutdown no'"));
    }

    private static DynamicCommandExceptionType tooSoon()
    {
        return new DynamicCommandExceptionType(seconds ->
            ServerUtil.localized("auto_shutdown.error.toosoon",
                "*** It is too soon since the last vote to initiate another one. Try again in %d seconds.", seconds)
        );
    }

    private static DynamicCommandExceptionType notEnoughPlayers()
    {
        return new DynamicCommandExceptionType(required ->
            ServerUtil.localized("auto_shutdown.error.notenoughplayers",
                "*** Need at least %d players online to initiate a vote", required)
        );
    }

    private static final ModCommands INSTANCE = new ModCommands();

    private final Map<UUID, Boolean> votes = new HashMap<>();
    private long lastVoteMillis = 0L;
    private boolean voting = false;

    /** Registers every command of the mod */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        registerAdmin(dispatcher);
        registerVoting(dispatcher);
        LOGGER.debug("Commands registered");
    }

    /** Registers `/auto_shutdown` (reload & status), requires OP level 3 */
    private static void registerAdmin(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("auto_shutdown")
            .requires(source -> source.hasPermission(3)) // Requires OP level 3
            .then(Commands.literal("reload")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();

                    try
                    {
                        LOGGER.info("Reloading Auto Shutdown configuration...");
                        source.sendSuccess(() -> ServerUtil.localized(
                            "auto_shutdown.cmd.reload.begin", "§e[AutoShutdown] Reloading configuration..."), true);

                        // Reload configuration
                        boolean success = ShutdownMod.reload(source.getServer());

                        if (success)
                        {
                            source.sendSuccess(() -> ServerUtil.localized(
                                "auto_shutdown.cmd.reload.success", "§a[AutoShutdown] Configuration reloaded successfully!"), true);
                            LOGGER.info("Configuration reloaded successfully");
                        }
                        else
                        {
                            source.sendFailure(ServerUtil.localized(
                                "auto_shutdown.cmd.reload.failed", "§c[AutoShutdown] Failed to reload configuration. Check server logs for details."));
                            LOGGER.error("Failed to reload configuration");
                        }

                        return success ? 1 : 0;
                    }
                    catch (Exception e)
                    {
                        LOGGER.error("Error reloading configuration", e);
                        source.sendFailure(ServerUtil.localized(
                            "auto_shutdown.cmd.reload.error", "§c[AutoShutdown] Error: %s", e.getMessage()));
                        return 0;
                    }
                })
            )
            .then(Commands.literal("status")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();

                    source.sendSuccess(() -> ServerUtil.localized(
                        "auto_shutdown.cmd.status.header", "§6[AutoShutdown] Current Status:"), false);
                    source.sendSuccess(() -> ServerUtil.localized(
                        "auto_shutdown.cmd.status.schedule", "§7- Schedule: %s",
                        statusText(Config.scheduleEnabled.get())), false);
                    source.sendSuccess(() -> ServerUtil.localized(
                        "auto_shutdown.cmd.status.voting", "§7- Voting: %s",
                        statusText(Config.voteEnabled.get())), false);
                    source.sendSuccess(() -> ServerUtil.localized(
                        "auto_shutdown.cmd.status.watchdog", "§7- Watchdog: %s",
                        statusText(Config.watchdogEnabled.get())), false);
                    source.sendSuccess(() -> ServerUtil.localized(
                        "auto_shutdown.cmd.status.idle", "§7- Idle Shutdown: %s",
                        statusText(Config.idleShutdownEnabled.get())), false);

                    if (Config.idleShutdownEnabled.get())
                    {
                        source.sendSuccess(() -> ServerUtil.localized(
                            "auto_shutdown.cmd.status.idle_active", "§7  Active: %s - %s, Timeout: %s min",
                            String.format("%02d:%02d", Config.idleCheckStartHour.get(), Config.idleCheckStartMinute.get()),
                            String.format("%02d:%02d", Config.idleCheckEndHour.get(), Config.idleCheckEndMinute.get()),
                            Config.idleTimeout.get()), false);
                    }

                    return 1;
                })
            )
        );
    }

    /** Registers `/shutdown` (player voting) */
    private static void registerVoting(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("shutdown")
            .executes(context -> INSTANCE.initiateVote(context.getSource()))
            .then(Commands.literal("yes")
                .executes(context -> INSTANCE.processVote(context.getSource(), true))
            )
            .then(Commands.literal("no")
                .executes(context -> INSTANCE.processVote(context.getSource(), false))
            )
        );
    }

    private ModCommands() { }

    private static String statusText(boolean enabled)
    {
        return enabled
            ? LangLoader.get("auto_shutdown.cmd.status.enabled", "§aEnabled")
            : LangLoader.get("auto_shutdown.cmd.status.disabled", "§cDisabled");
    }

    private int initiateVote(CommandSourceStack source) throws CommandSyntaxException
    {
        if (!Config.voteEnabled.get())
        {
            source.sendFailure(ServerUtil.localized(
                "auto_shutdown.error.votenotenabled", "§c[AutoShutdown] Voting is not enabled by the server administrator."));
            return 0;
        }

        ServerPlayer player = getPlayer(source);

        if (voting)
            throw voteInProgress().create();

        long now = System.currentTimeMillis();
        long interval = (long) Config.voteInterval.get() * 60 * 1000;
        long difference = now - lastVoteMillis;

        if (difference < interval)
            throw tooSoon().create((interval - difference) / 1000);

        MinecraftServer server = source.getServer();
        int players = server.getPlayerList().getPlayers().size();

        if (players < Config.minVoters.get())
            throw notEnoughPlayers().create(Config.minVoters.get());

        votes.clear();
        voting = true;

        ServerUtil.toAll(server, ServerUtil.localized("auto_shutdown.msg.votebegun",
            "*** A vote has begun to shutdown the server; please do '/shutdown yes' or '/shutdown no' to cast your vote"));
        LOGGER.info("AutoShutdown: {} called for a shutdown vote", player.getScoreboardName());
        return 1;
    }

    private int processVote(CommandSourceStack source, boolean vote) throws CommandSyntaxException
    {
        if (!Config.voteEnabled.get())
        {
            source.sendFailure(ServerUtil.localized(
                "auto_shutdown.error.votenotenabled", "§c[AutoShutdown] Voting is not enabled by the server administrator."));
            return 0;
        }

        ServerPlayer player = getPlayer(source);

        if (!voting)
            throw noVoteInProgress().create();

        UUID id = player.getUUID();

        if (votes.containsKey(id))
            ServerUtil.to(source, ServerUtil.localized("auto_shutdown.msg.votecleared",
                "*** Your previous vote has been cleared"));

        votes.put(id, vote);
        ServerUtil.to(source, ServerUtil.localized("auto_shutdown.msg.voterecorded",
            "*** Your vote has been recorded"));

        LOGGER.info("AutoShutdown: {} voted {}", player.getScoreboardName(), vote ? "yes" : "no");
        checkVotes(source.getServer());
        return 1;
    }

    private void checkVotes(MinecraftServer server)
    {
        int players = server.getPlayerList().getPlayers().size();

        if (players < Config.minVoters.get())
        {
            voteFailure(server, ServerUtil.localized("auto_shutdown.fail.notenoughplayers",
                "*** Vote to shutdown the server failed; not enough players online"));
            return;
        }

        int yes = 0;
        int no = 0;
        for (boolean vote : votes.values())
        {
            if (vote)
                yes++;
            else
                no++;
        }

        if (no >= Config.maxNoVotes.get())
        {
            voteFailure(server, ServerUtil.localized("auto_shutdown.fail.maxnovotes",
                "*** Vote to shutdown the server failed; too many 'no' votes"));
            return;
        }

        if (yes + no == players)
            voteSuccess(server);
    }

    private void voteSuccess(MinecraftServer server)
    {
        LOGGER.info("Server shutdown initiated by vote");
        ServerUtil.shutdown(server, ServerUtil.localized("auto_shutdown.msg.usershutdown",
            "Server shutting down by user vote"));
    }

    private void voteFailure(MinecraftServer server, Component message)
    {
        ServerUtil.toAll(server, message);
        votes.clear();

        lastVoteMillis = System.currentTimeMillis();
        voting = false;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) throws CommandSyntaxException
    {
        if (source.getEntity() instanceof ServerPlayer player)
            return player;

        throw playersOnly().create();
    }
}