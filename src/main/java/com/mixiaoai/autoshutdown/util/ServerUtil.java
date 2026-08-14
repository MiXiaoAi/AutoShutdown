package com.mixiaoai.autoshutdown.util;

import com.mixiaoai.autoshutdown.Platform;
import com.mixiaoai.autoshutdown.ShutdownMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

/**
 * Static utility class for chat and server functions
 */
public class ServerUtil
{
    private static final Logger LOGGER = ShutdownMod.LOGGER;

    private ServerUtil() { }

    /**
     * Broadcasts an auto translated message to all players
     * @param server Server instance to broadcast to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void toAll(MinecraftServer server, String msg, Object... parts)
    {
        toAll(server, Component.translatable(msg, parts));
    }

    public static void toAll(MinecraftServer server, Component message)
    {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /**
     * Sends an automatically translated, formatted & encapsulated message to a command source
     * @param source Target to send message to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void to(CommandSourceStack source, String msg, Object... parts)
    {
        to(source, Component.translatable(msg, parts));
    }

    public static void to(CommandSourceStack source, Component message)
    {
        source.sendSystemMessage(message);
    }

    /** Kicks all players from the server with given reason, then shuts server down */
    public static void shutdown(MinecraftServer server, Component message)
    {
        if (server == null)
            return;

        for (ServerPlayer player : server.getPlayerList().getPlayers())
            player.connection.disconnect(message);

        LOGGER.info("Shutdown initiated because: {}", message.getString());
        server.halt(false);
    }

    /** Checks if any non-fake player is present on the server */
    public static boolean hasRealPlayers(MinecraftServer server)
    {
        if (server == null)
            return false;

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (Platform.get().isRealPlayer(player))
                return true;
        }

        return false;
    }
}
