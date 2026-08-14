package com.mixiaoai.autoshutdown;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the mod's language files from its own resources, independent of the
 * client-side language system. The active language is selected via config so
 * the server can render chat messages in the configured language.
 */
public class LangLoader
{
    private static final Logger LOGGER = ShutdownMod.LOGGER;

    private static final String LANG_DIR = "/assets/auto_shutdown/lang/";

    private static String loadedLang = "";
    private static Map<String, String> entries = new HashMap<>();

    private LangLoader() { }

    /** Returns the localized text for a key, or the fallback if the key is missing */
    public static String get(String key, String fallback)
    {
        String lang;

        try
        {
            lang = Config.language.get();
        }
        catch (IllegalStateException e)
        {
            lang = "en_us";
        }

        if (!lang.equals(loadedLang))
            reload(lang);

        return entries.getOrDefault(key, fallback);
    }

    private static void reload(String lang)
    {
        Map<String, String> map = new HashMap<>();

        String resource = LANG_DIR + lang + ".json";

        try (InputStream in = LangLoader.class.getResourceAsStream(resource))
        {
            if (in == null)
            {
                LOGGER.warn("Language file '{}' not found, falling back to en_us", resource);
                try (InputStream fallback = LangLoader.class.getResourceAsStream(LANG_DIR + "en_us.json"))
                {
                    if (fallback != null)
                        map = parse(fallback);
                }
            }
            else
            {
                map = parse(in);
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to load language file '{}'", resource, e);
        }

        entries = map;
        loadedLang = lang;
    }

    private static Map<String, String> parse(InputStream in) throws IOException
    {
        Map<String, String> map = new HashMap<>();

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet())
                map.put(entry.getKey(), entry.getValue().getAsString());
        }

        return map;
    }
}
