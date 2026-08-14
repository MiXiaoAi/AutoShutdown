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

    private static final SimpleCommandExceptionType PLAYERS_ONLY =
        new SimpleCommandExceptionType(Component.translatable("auto_shutdown.error.playersonly"));
    private static final SimpleCommandExceptionType NO_VOTE_IN_PROGRESS =
        new SimpleCommandExceptionType(Component.translatable("auto_shutdown.error.novoteinprogress"));
    private static final SimpleCommandExceptionType VOTE_IN_PROGRESS =
        new SimpleCommandExceptionType(Component.translatable("auto_shutdown.error.voteinprogress"));
    private static final DynamicCommandExceptionType TOO_SOON =
        new DynamicCommandExceptionType(seconds ->
            Component.translatable("auto_shutdown.error.toosoon", seconds)
        );
    private static final DynamicCommandExceptionType NOT_ENOUGH_PLAYERS =
        new DynamicCommandExceptionType(required ->
            Component.translatable("auto_shutdown.error.notenoughplayers", required)
        );

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
                        source.sendSuccess(() -> Component.literal("§e[AutoShutdown] Reloading configuration..."), true);

                        // Reload configuration
                        boolean success = ShutdownMod.reload(source.getServer());

                        if (success)
                        {
                            source.sendSuccess(() -> Component.literal("§a[AutoShutdown] Configuration reloaded successfully!"), true);
                            LOGGER.info("Configuration reloaded successfully");
                        }
                        else
                        {
                            source.sendFailure(Component.literal("§c[AutoShutdown] Failed to reload configuration. Check server logs for details."));
                            LOGGER.error("Failed to reload configuration");
                        }

                        return success ? 1 : 0;
                    }
                    catch (Exception e)
                    {
                        LOGGER.error("Error reloading configuration", e);
                        source.sendFailure(Component.literal("§c[AutoShutdown] Error: " + e.getMessage()));
                        return 0;
                    }
                })
            )
            .then(Commands.literal("status")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();

                    source.sendSuccess(() -> Component.literal("§6[AutoShutdown] Current Status:"), false);
                    source.sendSuccess(() -> Component.literal("§7- Schedule: " + (Config.scheduleEnabled.get() ? "§aEnabled" : "§cDisabled")), false);
                    source.sendSuccess(() -> Component.literal("§7- Voting: " + (Config.voteEnabled.get() ? "§aEnabled" : "§cDisabled")), false);
                    source.sendSuccess(() -> Component.literal("§7- Watchdog: " + (Config.watchdogEnabled.get() ? "§aEnabled" : "§cDisabled")), false);
                    source.sendSuccess(() -> Component.literal("§7- Idle Shutdown: " + (Config.idleShutdownEnabled.get() ? "§aEnabled" : "§cDisabled")), false);

                    if (Config.idleShutdownEnabled.get())
                    {
                        source.sendSuccess(() -> Component.literal(String.format("§7  Active: %02d:%02d - %02d:%02d, Timeout: %d min",
                            Config.idleCheckStartHour.get(),
                            Config.idleCheckStartMinute.get(),
                            Config.idleCheckEndHour.get(),
                            Config.idleCheckEndMinute.get(),
                            Config.idleTimeout.get())), false);
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
            .requires(source -> Config.voteEnabled.get())
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

    private int initiateVote(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = getPlayer(source);

        if (voting)
            throw VOTE_IN_PROGRESS.create();

        long now = System.currentTimeMillis();
        long interval = (long) Config.voteInterval.get() * 60 * 1000;
        long difference = now - lastVoteMillis;

        if (difference < interval)
            throw TOO_SOON.create((interval - difference) / 1000);

        MinecraftServer server = source.getServer();
        int players = server.getPlayerList().getPlayers().size();

        if (players < Config.minVoters.get())
            throw NOT_ENOUGH_PLAYERS.create(Config.minVoters.get());

        votes.clear();
        voting = true;

        ServerUtil.toAll(server, "auto_shutdown.msg.votebegun");
        LOGGER.info("AutoShutdown: {} called for a shutdown vote", player.getScoreboardName());
        return 1;
    }

    private int processVote(CommandSourceStack source, boolean vote) throws CommandSyntaxException
    {
        ServerPlayer player = getPlayer(source);

        if (!voting)
            throw NO_VOTE_IN_PROGRESS.create();

        UUID id = player.getUUID();

        if (votes.containsKey(id))
            ServerUtil.to(source, "auto_shutdown.msg.votecleared");

        votes.put(id, vote);
        ServerUtil.to(source, "auto_shutdown.msg.voterecorded");

        LOGGER.info("AutoShutdown: {} voted {}", player.getScoreboardName(), vote ? "yes" : "no");
        checkVotes(source.getServer());
        return 1;
    }

    private void checkVotes(MinecraftServer server)
    {
        int players = server.getPlayerList().getPlayers().size();

        if (players < Config.minVoters.get())
        {
            voteFailure(server, "auto_shutdown.fail.notenoughplayers");
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
            voteFailure(server, "auto_shutdown.fail.maxnovotes");
            return;
        }

        if (yes + no == players)
            voteSuccess(server);
    }

    private void voteSuccess(MinecraftServer server)
    {
        LOGGER.info("Server shutdown initiated by vote");
        ServerUtil.shutdown(server, Component.translatable("auto_shutdown.msg.usershutdown"));
    }

    private void voteFailure(MinecraftServer server, String reason)
    {
        ServerUtil.toAll(server, reason);
        votes.clear();

        lastVoteMillis = System.currentTimeMillis();
        voting = false;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) throws CommandSyntaxException
    {
        if (source.getEntity() instanceof ServerPlayer player)
            return player;

        throw PLAYERS_ONLY.create();
    }
}