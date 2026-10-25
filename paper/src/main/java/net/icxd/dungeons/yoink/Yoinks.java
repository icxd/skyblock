package net.icxd.dungeons.yoink;

import org.bukkit.configuration.file.FileConfiguration;

import net.icxd.dungeons.Dungeons;

/**
 * /yoinkprofile's API client, with the key from this server's config.yml ({@code hypixel.api-key}), or else the
 * {@code HYPIXEL_API_KEY} environment variable (one for every server servermgr starts), and the API's address
 * ({@code hypixel.api-url}, only changed to test against a stand-in).
 */
public final class Yoinks {
    public static final String KEY_PATH = "hypixel.api-key";
    public static final String KEY_VARIABLE = "HYPIXEL_API_KEY";
    public static final String URL_PATH = "hypixel.api-url";

    private static HypixelApi api;
    private static String apiKey;
    private static String apiUrl;

    private Yoinks() {
    }

    /** The client for the key and address there are now. */
    public static synchronized HypixelApi api() {
        FileConfiguration config = Dungeons.getInstance().getConfig();
        String key = key(config, System.getenv(KEY_VARIABLE));
        String url = url(config);
        if (api == null || !key.equals(apiKey) || !url.equals(apiUrl)) {
            api = new HypixelApi(key, url, HypixelApi.MOJANG);
            apiKey = key;
            apiUrl = url;
        }
        return api;
    }

    /** The config's key, else the environment's; blank for none. */
    static String key(FileConfiguration config, String environment) {
        String configured = config == null ? null : config.getString(KEY_PATH, "");
        if (configured != null && !configured.isBlank()) return configured.trim();
        return environment == null ? "" : environment.trim();
    }

    /** The API's address, ending in a slash. */
    static String url(FileConfiguration config) {
        String url = config == null ? null : config.getString(URL_PATH, "");
        if (url == null || url.isBlank()) return HypixelApi.HYPIXEL;
        return url.trim().endsWith("/") ? url.trim() : url.trim() + "/";
    }
}
