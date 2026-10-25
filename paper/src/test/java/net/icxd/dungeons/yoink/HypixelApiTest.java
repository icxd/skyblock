package net.icxd.dungeons.yoink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** The API client against a stand-in for Mojang's lookup and Hypixel's {@code skyblock/profiles}, as they answer. */
class HypixelApiTest {
    private HttpServer server;
    private String base;
    /** Each lookup that reached the stand-in: the name, or the key profiles were asked with. */
    private final List<String> asked = new CopyOnWriteArrayList<>();

    @BeforeEach
    void start() throws IOException {
        byte[] profiles;
        try (InputStream in = HypixelApiTest.class.getResourceAsStream("/yoink/profiles.json")) {
            profiles = in.readAllBytes();
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mojang/", exchange -> {
            // name/<name>, or <id>
            String path = exchange.getRequestURI().getPath().substring("/mojang/".length());
            asked.add(path);
            if (path.equalsIgnoreCase("name/technoblade") || path.equals(HypixelProfileTest.PLAYER)) {
                respond(exchange, 200, "{\"id\":\"" + HypixelProfileTest.PLAYER + "\",\"name\":\"Technoblade\"}");
            } else {
                respond(exchange, 404, "{\"path\":\"/minecraft/profile/lookup/" + path + "\",\"error\":\"NOT_FOUND\",\"errorMessage\":\"Not Found\"}");
            }
        });
        server.createContext("/hypixel/skyblock/profiles", exchange -> {
            String key = exchange.getRequestHeaders().getFirst("API-Key");
            asked.add(key);
            switch (key == null ? "" : key) {
                case "good" -> respond(exchange, 200, new String(profiles, StandardCharsets.UTF_8));
                case "busy" -> respond(exchange, 429, "{\"success\":false,\"cause\":\"Key throttle\",\"throttle\":true}");
                case "odd" -> respond(exchange, 200, "{\"success\":false,\"cause\":\"Malformed UUID\"}");
                case "down" -> respond(exchange, 502, "<html>Bad Gateway</html>");
                default -> respond(exchange, 403, "{\"success\":false,\"cause\":\"Invalid API key\"}");
            }
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private HypixelApi api(String key) {
        return new HypixelApi(key, base + "hypixel/", base + "mojang/");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** What it failed with, as the player is told. */
    private static String failure(CompletableFuture<?> future) {
        CompletionException e = assertThrows(CompletionException.class, future::join);
        return HypixelApi.reason(e);
    }

    @Test
    void namesToIds() {
        HypixelApi api = api("good");
        HypixelApi.Player techno = new HypixelApi.Player(HypixelProfileTest.PLAYER, "Technoblade");
        assertEquals(techno, api.player("technoblade").join());
        assertEquals("&cThere's no player called Nobody_Here!", failure(api.player("Nobody_Here")));
        // An id is looked up for its name, with or without its dashes.
        assertEquals(techno, api.player("B876EC32-E396-476B-A115-8438D83C67D4").join());
        assertEquals("&cThere's no player with the id 00000000000000000000000000000b16!", failure(api.player("00000000000000000000000000000b16")));
        assertEquals(List.of("name/technoblade", "name/Nobody_Here", HypixelProfileTest.PLAYER, "00000000000000000000000000000b16"), asked);
        // What can't be either isn't asked.
        assertEquals("&cnot a name! isn't a Minecraft name!", failure(api.player("not a name!")));
        assertEquals(4, asked.size());
    }

    @Test
    void profilesWithTheKey() {
        JsonObject answer = api("good").profiles(HypixelProfileTest.PLAYER).join();
        assertEquals(3, answer.getAsJsonArray("profiles").size());
        assertEquals(List.of("good"), asked, "the key goes in its header");
        assertEquals("&cHypixel says the API key isn't valid (it may have expired).", failure(api("expired").profiles(HypixelProfileTest.PLAYER)));
        assertEquals("&cHypixel's API says too many requests: try again in a minute.", failure(api("busy").profiles(HypixelProfileTest.PLAYER)));
        assertEquals("&cHypixel's API said no: Malformed UUID.", failure(api("odd").profiles(HypixelProfileTest.PLAYER)));
        assertEquals("&cHypixel's API said 502.", failure(api("down").profiles(HypixelProfileTest.PLAYER)));
        // Without a key nothing is asked.
        HypixelApi none = api("  ");
        assertFalse(none.hasKey());
        assertEquals(HypixelApi.NO_KEY, failure(none.profiles(HypixelProfileTest.PLAYER)));
        assertEquals(5, asked.size());
        assertTrue(api(" good ").hasKey());
    }

    @Test
    void unreachable() {
        server.stop(0);
        assertTrue(failure(api("good").player("technoblade")).startsWith("&cCouldn't reach 127.0.0.1: "));
    }
}
