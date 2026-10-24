package net.icxd.dungeons.npc;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A map's NPCs as recorded: {@code npcs/<map>.json} in the data checkout, which the replay tool's
 * {@code npcs} command makes from the recordings that went there (see tools/replay). Each stands where
 * it stood, facing the way it faced, under its name tags at their recorded heights, in its skin (a
 * player-shaped one) or as its kind (a villager's type and job, or another mob), holding what it held.
 * Hypixel's data, so it stays in that private checkout.
 *
 * @param npcs     what's in it, as it's laid out
 * @param problems what was wrong with it (it's missing, or entries that couldn't be read)
 */
public record NpcTable(List<Entry> npcs, List<String> problems) {
    public static final String FOLDER = "npcs";

    /**
     * One NPC. {@code type} is the entity type as the recording had it ("player", "villager", "witch");
     * {@code heights} go with {@code lines}, top line first; the skin (with its signature, if it had one)
     * only for a player-shaped one, the villager kind only for a villager (null otherwise); equipment by
     * slot as the replay tool names them ("mainhand"), each an item id ("stone_axe").
     */
    public record Entry(String name, String type, double x, double y, double z, float yaw, float pitch, List<String> lines, double[] heights,
                        String skin, String signature, String villagerType, String villagerProfession, Map<String, String> equipment) {
        /** What it's known as: its name in capitals ("Clerk Seraphine" is CLERK_SERAPHINE), as Ophelia's is OPHELIA. */
        public String id() {
            String id = name == null ? "" : name.replaceAll("[&§].", "").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
            return id.isEmpty() ? "NPC" : id;
        }
    }

    /** Reads {@code <map>.json} in this folder (npcs/ of the data folder); what's wrong is in {@link #problems}. */
    public static NpcTable load(Path folder, String map) {
        Path file = folder.resolve(map + ".json");
        if (!Files.isRegularFile(file)) return new NpcTable(List.of(), List.of("no " + file + ", so its NPCs aren't there"));
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            return new NpcTable(List.of(), List.of("couldn't read " + file + ": " + e));
        }
    }

    static NpcTable parse(JsonObject o) {
        List<Entry> npcs = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        int i = 0;
        for (JsonElement e : o.getAsJsonArray("npcs")) {
            i++;
            try {
                npcs.add(entry(e.getAsJsonObject()));
            } catch (RuntimeException ex) {
                problems.add("NPC " + i + " left out: " + ex.getMessage());
            }
        }
        return new NpcTable(List.copyOf(npcs), List.copyOf(problems));
    }

    private static Entry entry(JsonObject n) {
        List<String> lines = new ArrayList<>();
        for (JsonElement line : n.getAsJsonArray("lines")) lines.add(line.getAsString());
        double[] heights = new double[lines.size()];
        var given = n.getAsJsonArray("heights");
        if (given == null || given.size() != lines.size()) throw new IllegalArgumentException("its tags' heights don't go with its " + lines.size() + " lines");
        for (int h = 0; h < heights.length; h++) {
            // Null for a name it carries itself, which none of the recorded ones does: taken as at its feet.
            heights[h] = given.get(h).isJsonNull() ? 0 : given.get(h).getAsDouble();
        }
        JsonObject skin = n.has("skin") && n.get("skin").isJsonObject() ? n.getAsJsonObject("skin") : null;
        JsonObject villager = n.has("villager") && n.get("villager").isJsonObject() ? n.getAsJsonObject("villager") : null;
        Map<String, String> equipment = new LinkedHashMap<>();
        if (n.has("equipment") && n.get("equipment").isJsonObject()) {
            for (Map.Entry<String, JsonElement> slot : n.getAsJsonObject("equipment").entrySet()) {
                equipment.put(slot.getKey(), slot.getValue().getAsJsonObject().get("id").getAsString());
            }
        }
        return new Entry(
                n.has("name") && !n.get("name").isJsonNull() ? n.get("name").getAsString() : null,
                n.get("type").getAsString(),
                n.get("x").getAsDouble(), n.get("y").getAsDouble(), n.get("z").getAsDouble(),
                n.has("yaw") ? n.get("yaw").getAsFloat() : 0, n.has("pitch") ? n.get("pitch").getAsFloat() : 0,
                List.copyOf(lines), heights,
                skin == null ? null : skin.get("value").getAsString(),
                skin == null || !skin.has("signature") || skin.get("signature").isJsonNull() ? null : skin.get("signature").getAsString(),
                villager == null ? null : villager.get("type").getAsString(),
                villager == null ? null : villager.get("profession").getAsString(),
                Map.copyOf(equipment));
    }
}
