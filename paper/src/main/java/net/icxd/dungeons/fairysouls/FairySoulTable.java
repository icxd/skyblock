package net.icxd.dungeons.fairysouls;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Where the Fairy Souls are, island by island: {@code npcs/fairy_souls.json} in the data checkout, which is NEU's
 * {@code constants/fairy_souls.json} as it is ({@code "hub": ["-233,86,84", ...]}, each the block the soul's head
 * is in; its other keys, a note and "Max Souls", aren't islands). Checked against the recordings: every soul
 * stand they passed (58 of the Hub's 80, all 7 of the Dungeon Hub's) stands where it says and none anywhere else,
 * and the Hub's other 22 each sit on the captured map's ground. Hypixel's data, so it stays in that private
 * checkout. No server needed.
 *
 * @param islands  each island's souls, by NEU's key ("hub", "dungeon_hub", "combat_1")
 * @param problems what was wrong with it (it's missing, or souls that couldn't be read)
 */
public record FairySoulTable(Map<String, List<Soul>> islands, List<String> problems) {
    public static final String FILE = "fairy_souls.json";
    public static final String HUB = "hub";
    public static final String DUNGEON_HUB = "dungeon_hub";

    /** A soul: its island (NEU's key) and the block its head is in. */
    public record Soul(String island, int x, int y, int z) {
        /** How a profile remembers it: "hub:-233,86,84". */
        public String id() {
            return island + ":" + x + "," + y + "," + z;
        }
    }

    public FairySoulTable {
        islands = Map.copyOf(islands);
        problems = List.copyOf(problems);
    }

    /** Reads it from this folder (npcs/ of the data folder); what's wrong is in {@link #problems}. */
    public static FairySoulTable load(Path folder) {
        Path file = folder.resolve(FILE);
        if (!Files.isRegularFile(file)) return new FairySoulTable(Map.of(), List.of("no " + file + ", so there are no Fairy Souls"));
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            return new FairySoulTable(Map.of(), List.of("couldn't read " + file + ": " + e));
        }
    }

    static FairySoulTable parse(JsonObject json) {
        Map<String, List<Soul>> islands = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, JsonElement> island : json.entrySet()) {
            if (!island.getValue().isJsonArray()) continue;
            List<Soul> souls = new ArrayList<>();
            for (JsonElement spot : island.getValue().getAsJsonArray()) {
                try {
                    String[] xyz = spot.getAsString().split(",");
                    if (xyz.length != 3) throw new IllegalArgumentException("not x,y,z");
                    souls.add(new Soul(island.getKey(), Integer.parseInt(xyz[0].trim()), Integer.parseInt(xyz[1].trim()), Integer.parseInt(xyz[2].trim())));
                } catch (RuntimeException e) {
                    problems.add(island.getKey() + ": " + spot + " left out (" + e.getMessage() + ")");
                }
            }
            islands.put(island.getKey(), List.copyOf(souls));
        }
        return new FairySoulTable(islands, problems);
    }

    /** An island's souls; none for one it doesn't have. */
    public List<Soul> on(String island) {
        return islands.getOrDefault(island, List.of());
    }
}
