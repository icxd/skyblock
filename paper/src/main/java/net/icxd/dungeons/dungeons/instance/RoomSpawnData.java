package net.icxd.dungeons.dungeons.instance;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.icxd.dungeons.dungeons.paste.PastePlan;

/**
 * What each room has in it besides its blocks, per room id (the scanner's, as in {@code rooms/<id>}): its mobs,
 * skeleton skulls, crypts and weak walls, in the room's capture frame (see {@link RoomFrame}). Hypixel's, so
 * kept with the captures in the private data ({@code rooms/_mobs/<id>.json}, which servermgr links into
 * {@code dungeon-rooms/rooms/_mobs}): recorded on Hypixel where there's a recording of the room, crypts and
 * walls also from Skyblocker's waypoints. A room with no file (or no recorded mobs) gets planned mobs
 * ({@link FallbackSpawns}).
 */
final class RoomSpawnData {
    /** Where the data is, next to the captures. */
    static final String FOLDER = "_mobs";

    /**
     * A mob to spawn.
     *
     * @param kind     a {@link net.icxd.dungeons.mob.MobKinds} id
     * @param modifier as recorded (null for none); one sample, so a run rolls its own (see {@link RoomMobs})
     * @param level    for a miniboss, its level; null for the floor's first
     * @param inferred whether its star is inferred (its name tag was never seen)
     */
    record Mob(String kind, boolean starred, String modifier, Integer level, double x, double y, double z, boolean inferred) {
    }

    record Point(double x, double y, double z) {
    }

    /** A tomb: its blocks, and where its Crypt Undead comes out. */
    record Crypt(Point undead, List<PastePlan.Block> blocks, String source) {
        Crypt {
            blocks = List.copyOf(blocks);
        }
    }

    /** A weak wall: its blocks, and what some of them become when it's blown up (air for the rest). */
    record Wall(List<PastePlan.Block> blocks, Map<PastePlan.Block, String> after, String source) {
        Wall {
            blocks = List.copyOf(blocks);
            after = Map.copyOf(after);
        }
    }

    /** @param recorded whether its mobs were recorded on Hypixel (else it only has crypts or walls, and planned mobs) */
    record Room(String id, boolean recorded, List<Mob> mobs, List<Point> skulls, List<Crypt> crypts, List<Wall> walls) {
        Room {
            mobs = List.copyOf(mobs);
            skulls = List.copyOf(skulls);
            crypts = List.copyOf(crypts);
            walls = List.copyOf(walls);
        }
    }

    /** The file with the heads room loot lies as: name to texture hash. */
    static final String HEADS = "_heads.json";

    /** What loading found: the rooms by id, the loot's heads by name, and what couldn't be read. */
    record Loaded(Map<String, Room> rooms, Map<String, String> heads, List<String> problems) {
        static final Loaded NONE = new Loaded(Map.of(), Map.of(), List.of());

        Room room(String id) {
            return id == null ? null : rooms.get(id);
        }
    }

    private RoomSpawnData() {
    }

    /** Every {@code <id>.json} in the folder, and {@link #HEADS}; none if there's no folder. */
    static Loaded load(Path folder) {
        if (!Files.isDirectory(folder)) return new Loaded(Map.of(), Map.of(), List.of("no " + folder));
        Map<String, Room> rooms = new TreeMap<>();
        Map<String, String> heads = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                String id = name.substring(0, name.length() - ".json".length());
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                    if (name.equals(HEADS)) {
                        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                            if (!e.getKey().startsWith("_")) heads.put(e.getKey(), e.getValue().getAsString());
                        }
                    } else if (!name.startsWith("_")) {
                        rooms.put(id, parse(id, o));
                    }
                } catch (IOException | RuntimeException e) {
                    problems.add(name + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            problems.add(folder + ": " + e.getMessage());
        }
        return new Loaded(Collections.unmodifiableMap(new LinkedHashMap<>(rooms)), Map.copyOf(heads), List.copyOf(problems));
    }

    /** One room's file. */
    static Room parse(String id, JsonObject o) {
        List<Mob> mobs = new ArrayList<>();
        for (JsonElement e : array(o, "mobs")) {
            JsonObject m = e.getAsJsonObject();
            mobs.add(new Mob(m.get("kind").getAsString(), bool(m, "starred"), string(m, "modifier"),
                    m.has("level") && !m.get("level").isJsonNull() ? m.get("level").getAsInt() : null,
                    m.get("x").getAsDouble(), m.get("y").getAsDouble(), m.get("z").getAsDouble(), bool(m, "inferred")));
        }
        List<Point> skulls = new ArrayList<>();
        for (JsonElement e : array(o, "skulls")) skulls.add(point(e.getAsJsonObject()));
        List<Crypt> crypts = new ArrayList<>();
        for (JsonElement e : array(o, "crypts")) {
            JsonObject c = e.getAsJsonObject();
            crypts.add(new Crypt(point(c), blocks(c), string(c, "source")));
        }
        List<Wall> walls = new ArrayList<>();
        for (JsonElement e : array(o, "weakWalls")) {
            JsonObject w = e.getAsJsonObject();
            Map<PastePlan.Block, String> after = new LinkedHashMap<>();
            if (w.has("after")) {
                for (Map.Entry<String, JsonElement> a : w.getAsJsonObject("after").entrySet()) {
                    String[] xyz = a.getKey().split(",");
                    after.put(new PastePlan.Block(Integer.parseInt(xyz[0].trim()), Integer.parseInt(xyz[1].trim()), Integer.parseInt(xyz[2].trim())),
                            a.getValue().getAsString());
                }
            }
            walls.add(new Wall(blocks(w), after, string(w, "source")));
        }
        return new Room(id, bool(o, "recorded"), mobs, skulls, crypts, walls);
    }

    private static JsonArray array(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }

    private static boolean bool(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() && o.get(key).getAsBoolean();
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static Point point(JsonObject o) {
        return new Point(o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble());
    }

    private static List<PastePlan.Block> blocks(JsonObject o) {
        List<PastePlan.Block> out = new ArrayList<>();
        for (JsonElement e : array(o, "blocks")) {
            JsonArray b = e.getAsJsonArray();
            out.add(new PastePlan.Block(b.get(0).getAsInt(), b.get(1).getAsInt(), b.get(2).getAsInt()));
        }
        return out;
    }
}
