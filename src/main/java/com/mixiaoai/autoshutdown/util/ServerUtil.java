package com.mixiaoai.autoshutdown.util;

import com.mixiaoai.autoshutdown.LangLoader;
import com.mixiaoai.autoshutdown.Platform;
import com.mixiaoai.autoshutdown.ShutdownMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.util.IllegalFormatException;

/**
 * Static utility class for chat and server functions
 */
public class ServerUtil
{
    private static final Logger LOGGER = ShutdownMod.LOGGER;

    private ServerUtil() { }

    /**
     * Resolves a language key on the server and returns it as a literal component.
     * The text is rendered server-side from the mod's own language files (selected
     * via config), so clients without the mod's language files never see the raw key.
     * @param key Language key to look up
     * @param fallback English text used when the key is missing from the active language
     * @param parts Optional objects to format into the message
     */
    public static Component localized(String key, String fallback, Object... parts)
    {
        String text = LangLoader.get(key, fallback);

        if (parts.length > 0)
        {
            try
            {
                text = String.format(text, parts);
            }
            catch (IllegalFormatException e)
            {
                ShutdownMod.LOGGER.warn("Failed to format localized message '{}': {}", key, e.getMessage());
            }
        }

        return Component.literal(text);
    }

    /**
     * Broadcasts an auto translated message to all players
     * @param server Server instance to broadcast to
     * @param message Component to broadcast
     */
    public static void toAll(MinecraftServer server, Component message)
    {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /**
     * Sends a message to a command source
     * @param source Target to send message to
     * @param message Component to send
     */
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
