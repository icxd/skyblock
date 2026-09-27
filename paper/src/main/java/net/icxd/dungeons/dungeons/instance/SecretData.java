package net.icxd.dungeons.dungeons.instance;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.icxd.dungeons.dungeons.generation.utils.Direction;

/**
 * Where each room's secrets are: {@code rooms/_secrets/<room id>.json} in the private data folder, one file
 * per room the scanner knows (converted from Skyblocker's and BetterMap's waypoints, with how many secrets
 * Hypixel counts for the room from Odin; research secrets_puzzles.md 1.1-1.3). Hypixel's data, so it stays
 * out of this repository, the way the captured rooms do.
 *
 * <p>Positions are in the room's capture frame (the roof marker in the north-west corner, see
 * {@link net.icxd.dungeons.dungeons.paste.RoomCapture}) with y as the world's: the rooms are pasted at the
 * height they were captured at, and {@link RoomFrame} puts them in the world. Chest facings, where given,
 * are in that frame too. No Bukkit in it.
 */
final class SecretData {
    /** The folder under {@code rooms/} (a leading underscore: not a room, see RoomLibrary). */
    static final String FOLDER = "_secrets";

    /**
     * What the plugin does with a waypoint; the others (routes, crypts, Superboom walls) are hints. A
     * Redstone Key is two: the head you take ({@code KEY}, "Redstone Skull (right click)") and the Redstone
     * Node you put it on ({@code NODE}, "Place Skull"), which is the secret (the wiki's Redstone Key: "will
     * reward 1 secret"; Skyblocker counts it when the node's redstone block is clicked).
     */
    enum Kind {
        CHEST, ITEM, BAT, WITHER, LEVER, KEY, NODE, OTHER;

        /** Counts as a secret. */
        boolean secret() {
            return this != LEVER && this != KEY && this != OTHER;
        }

        static Kind of(String category, String name) {
            return switch (category) {
                case "chest" -> CHEST;
                case "item" -> ITEM;
                case "bat" -> BAT;
                case "wither" -> WITHER;
                case "lever" -> LEVER;
                case "key" -> name.contains("Place") ? NODE : KEY;
                default -> OTHER;
            };
        }
    }

    /**
     * One waypoint.
     *
     * @param category Skyblocker's ("chest", "lever", "superboom", "entrance", ...)
     * @param start    a chest Hypixel puts out when the run starts: one behind one of the room's levers (the
     *                 same number in its name; the wiki's "locked Secret Chests") or recorded so; the rest
     *                 come when someone walks into the room
     * @param facing   a chest's facing as recorded on Hypixel (capture frame); null if never seen
     * @param wall     a lever's wall: the blocks that move away when it's pulled (capture frame); empty if
     *                 never recorded
     */
    record Waypoint(String name, String category, int x, int y, int z, boolean start, Direction facing, List<int[]> wall) {
        Waypoint {
            wall = List.copyOf(wall);
        }

        Kind kind() {
            return Kind.of(category, name);
        }
    }

    /** A room's waypoints, and how many secrets Hypixel counts in it (the action bar's "0/5 Secrets"). */
    record Room(String id, int secrets, List<Waypoint> waypoints) {
        Room {
            waypoints = List.copyOf(waypoints);
        }
    }

    private final Map<String, Room> rooms;
    private final List<String> problems;

    private SecretData(Map<String, Room> rooms, List<String> problems) {
        this.rooms = rooms;
        this.problems = problems;
    }

    static SecretData empty() {
        return new SecretData(Map.of(), List.of());
    }

    /** Reads {@code rooms/_secrets} under the dungeon rooms folder (the one with {@code rooms/} in it). */
    static SecretData load(Path root) {
        Path folder = root.resolve("rooms").resolve(FOLDER);
        List<String> problems = new ArrayList<>();
        Map<String, Room> rooms = new HashMap<>();
        if (!Files.isDirectory(folder)) {
            problems.add("no " + folder + ", so no room has secrets");
            return new SecretData(Map.of(), problems);
        }
        List<Path> files;
        try (Stream<Path> list = Files.list(folder)) {
            files = list.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            problems.add("couldn't list " + folder + ": " + e.getMessage());
            return new SecretData(Map.of(), problems);
        }
        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Room room = room(JsonParser.parseReader(reader).getAsJsonObject());
                rooms.put(room.id(), room);
            } catch (IOException | RuntimeException e) {
                problems.add(file.getFileName() + ": " + e.getMessage());
            }
        }
        return new SecretData(Collections.unmodifiableMap(rooms), List.copyOf(problems));
    }

    static Room room(JsonObject o) {
        List<Waypoint> waypoints = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("waypoints")) {
            JsonObject w = e.getAsJsonObject();
            List<int[]> wall = new ArrayList<>();
            if (w.has("wall")) {
                for (JsonElement b : w.getAsJsonArray("wall")) {
                    JsonArray a = b.getAsJsonArray();
                    wall.add(new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()});
                }
            }
            waypoints.add(new Waypoint(w.get("name").getAsString(), w.get("category").getAsString(), w.get("x").getAsInt(),
                    w.get("y").getAsInt(), w.get("z").getAsInt(), w.has("start") && w.get("start").getAsBoolean(),
                    w.has("facing") ? Direction.valueOf(w.get("facing").getAsString()) : null, wall));
        }
        return new Room(o.get("id").getAsString(), o.get("secrets").getAsInt(), waypoints);
    }

    /** The room's secrets; null if it has none (or no file). */
    Room room(String id) {
        return rooms.get(id);
    }

    int size() {
        return rooms.size();
    }

    /** Files that couldn't be read, and why. */
    List<String> problems() {
        return problems;
    }
}
