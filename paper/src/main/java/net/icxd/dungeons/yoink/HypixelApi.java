package net.icxd.dungeons.yoink;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Hypixel's public API (v2) and Mojang's name lookup, for /yoinkprofile: a player's name to their id, and their
 * SkyBlock profiles ({@code skyblock/profiles}, which needs an API key: developer.hypixel.net). Each call runs off the
 * main thread; what goes wrong comes back as a {@link Failed} with a line to tell the player.
 */
public final class HypixelApi {
    static final String HYPIXEL = "https://api.hypixel.net/v2/";
    /** Mojang's lookup: {@code name/<name>} or {@code <id>}, each answering {@code {id, name}}. */
    static final String MOJANG = "https://api.minecraftservices.com/minecraft/profile/lookup/";
    /** A Minecraft name: 1 to 16 letters, digits and underscores. */
    private static final Pattern NAME = Pattern.compile("\\w{1,16}");
    /** An id with or without its dashes. */
    private static final Pattern ID = Pattern.compile("[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}");
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    /** What's said without a key. */
    public static final String NO_KEY = "&cThere's no Hypixel API key: set hypixel.api-key in this server's config.yml"
            + " (or the HYPIXEL_API_KEY environment variable).";

    private final HttpClient http;
    private final String key;
    private final String hypixel;
    private final String mojang;

    /** With the key (blank for none); {@code hypixel} and {@code mojang} are the APIs' addresses, for a test's own. */
    public HypixelApi(String key, String hypixel, String mojang) {
        this.http = HttpClient.newBuilder().proxy(ProxySelector.getDefault()).connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        this.key = key == null ? "" : key.trim();
        this.hypixel = hypixel;
        this.mojang = mojang;
    }

    public HypixelApi(String key) {
        this(key, HYPIXEL, MOJANG);
    }

    public boolean hasKey() {
        return !key.isEmpty();
    }

    /** What went wrong, said as a line for the player. */
    public static final class Failed extends RuntimeException {
        public Failed(String message) {
            super(message);
        }
    }

    /** A player: their id without dashes, and their name as it's written. */
    public record Player(String id, String name) {
    }

    /** Who has this name, or this id (with or without its dashes). */
    public CompletableFuture<Player> player(String nameOrId) {
        String query = nameOrId == null ? "" : nameOrId.trim();
        boolean id = ID.matcher(query).matches();
        if (!id && !NAME.matcher(query).matches()) return CompletableFuture.failedFuture(new Failed("&c" + query + " isn't a Minecraft name!"));
        String path = id ? query.replace("-", "").toLowerCase(Locale.ROOT) : "name/" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        String none = id ? "&cThere's no player with the id " + query + "!" : "&cThere's no player called " + query + "!";
        HttpRequest request = HttpRequest.newBuilder(URI.create(mojang + path)).timeout(TIMEOUT).GET().build();
        return send(request).thenApply(response -> {
            if (response.statusCode() == 404 || response.statusCode() == 204) throw new Failed(none);
            if (response.statusCode() != 200) throw new Failed("&cCouldn't look up " + query + " (Mojang said " + response.statusCode() + ")!");
            JsonObject body = json(response.body(), "Mojang");
            if (!body.has("id")) throw new Failed(none);
            return new Player(body.get("id").getAsString(), body.has("name") ? body.get("name").getAsString() : query);
        });
    }

    /** Their SkyBlock profiles, as Hypixel gives them: {@code {success, profiles: [...]}}. */
    public CompletableFuture<JsonObject> profiles(String playerId) {
        if (!hasKey()) return CompletableFuture.failedFuture(new Failed(NO_KEY));
        HttpRequest request = HttpRequest.newBuilder(URI.create(hypixel + "skyblock/profiles?uuid=" + playerId)).timeout(TIMEOUT)
                .header("API-Key", key).GET().build();
        return send(request).thenApply(response -> {
            // An error page needn't be an answer: what it said is in its status.
            JsonObject body = response.statusCode() == 200 ? json(response.body(), "Hypixel") : lenient(response.body());
            switch (response.statusCode()) {
                case 200 -> {
                }
                case 403 -> throw new Failed("&cHypixel says the API key isn't valid (it may have expired).");
                case 429 -> throw new Failed("&cHypixel's API says too many requests: try again in a minute.");
                default -> throw new Failed("&cHypixel's API said " + response.statusCode() + cause(body) + ".");
            }
            if (body.has("success") && !body.get("success").getAsBoolean()) throw new Failed("&cHypixel's API said no" + cause(body) + ".");
            return body;
        });
    }

    private CompletableFuture<HttpResponse<String>> send(HttpRequest request) {
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).exceptionally(e -> {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            if (cause instanceof Failed failed) throw failed;
            throw new Failed("&cCouldn't reach " + request.uri().getHost() + ": " + (cause instanceof IOException ? cause.getClass().getSimpleName()
                    : cause.toString()));
        });
    }

    private static JsonObject json(String body, String from) {
        try {
            JsonElement element = JsonParser.parseString(body == null || body.isBlank() ? "{}" : body);
            if (!element.isJsonObject()) throw new Failed("&c" + from + " sent something that isn't an answer.");
            return element.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new Failed("&c" + from + " sent something that isn't an answer.");
        }
    }

    private static JsonObject lenient(String body) {
        try {
            return json(body, "");
        } catch (Failed e) {
            return new JsonObject();
        }
    }

    private static String cause(JsonObject body) {
        return body.has("cause") && body.get("cause").isJsonPrimitive() ? ": " + body.get("cause").getAsString() : "";
    }

    /** The line to tell the player, whatever went wrong. */
    public static String reason(Throwable e) {
        Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
        return cause instanceof Failed failed ? failed.getMessage() : "&cSomething went wrong: " + cause;
    }
}
