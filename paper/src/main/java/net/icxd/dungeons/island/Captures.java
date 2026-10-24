package net.icxd.dungeons.island;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Worlds saved from Hypixel with the dungeon scanner's {@code /dscan world}: the data checkout's
 * {@code worlds/}, which servermgr links into every server as {@code plugins/dungeons/worlds}. Each
 * is a folder of region files with a {@code capture.json} naming the areas the sidebar showed there,
 * which is how one is found: the starter island is the capture of "Your Island". Captures made
 * before the scanner's fix keep Hypixel's made-up colour codes in those names ("Your Isla§tnd"),
 * which are taken out here.
 */
public final class Captures {
    private static final Pattern CODES = Pattern.compile("§.?");

    private Captures() {
    }

    /** The newest capture (by when its saving ended) whose sidebar named this area; null for none. */
    public static Path find(Path worlds, String area) {
        if (!Files.isDirectory(worlds)) return null;
        Path best = null;
        String bestTime = null;
        try (Stream<Path> folders = Files.list(worlds)) {
            for (Path folder : folders.sorted().toList()) {
                Path json = folder.resolve("capture.json");
                if (!Files.isRegularFile(json) || !Files.isDirectory(folder.resolve("region"))) continue;
                JsonObject capture;
                try {
                    capture = JsonParser.parseString(Files.readString(json)).getAsJsonObject();
                } catch (IOException | JsonParseException | IllegalStateException e) {
                    continue;
                }
                if (areas(capture).stream().noneMatch(area::equalsIgnoreCase)) continue;
                String time = capture.has("to") ? capture.get("to").getAsString() : "";
                if (best == null || time.compareTo(bestTime) > 0) {
                    best = folder;
                    bestTime = time;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return best;
    }

    /** The areas a capture's sidebar named, as the player saw them. */
    static List<String> areas(JsonObject capture) {
        List<String> areas = new ArrayList<>();
        if (!(capture.get("areas") instanceof JsonArray list)) return areas;
        for (JsonElement area : list) {
            if (area.isJsonPrimitive()) areas.add(clean(area.getAsString()));
        }
        return areas;
    }

    /** Without colour codes, real or made up. */
    static String clean(String text) {
        return CODES.matcher(text).replaceAll("").trim();
    }
}
