package net.icxd.dungeons.dungeons.paste;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.RoomPool;
import net.icxd.dungeons.dungeons.generation.room.DoorSlot;
import net.icxd.dungeons.dungeons.generation.room.HypixelRooms;
import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * The rooms captured by the dungeon scanner, read from a folder laid out the way the scanner saves
 * them:
 * <pre>
 *   rooms/&lt;id&gt;/&lt;id&gt;_&lt;hash&gt;.json + .schem   one file pair per captured variant
 * </pre>
 * Every room id becomes one generator template; its captures are variants of it (e.g. the same
 * room captured with different doorways open, or Lower and Higher Blaze for the blaze puzzle).
 * Doors come out of the captures too (see {@link PastePlan}), so the scanner's {@code doors/}
 * folder isn't needed.
 */
public final class RoomLibrary {
    /** Scanner names (from Odin's room list) that differ from the generator's (IllegalMap's). */
    private static final Map<String, String> ALIASES = Map.of(
            "lower_blaze", "blaze",
            "higher_blaze", "blaze",
            "rare_pillars", "pillars",
            "silver_sword", "silvers_sword",
            "rail_track", "mini_rail_track"
    );

    /** A doorway that was open in a capture, and the door that was in it. */
    public record Doorway(RoomCapture capture, DoorSlot slot, DoorType type) {
    }

    private final Map<String, List<RoomCapture>> rooms;
    private final List<Doorway> doorways;
    private final List<Room> templates;
    private final List<String> problems;

    private RoomLibrary(Map<String, List<RoomCapture>> rooms, List<Doorway> doorways, List<Room> templates,
                                            List<String> problems) {
        this.rooms = rooms;
        this.doorways = doorways;
        this.templates = templates;
        this.problems = problems;
    }

    public static RoomLibrary load(Path root) throws IOException {
        Path roomDir = root.resolve("rooms");
        if (!Files.isDirectory(roomDir)) throw new IOException("No rooms folder in " + root);
        List<String> problems = new ArrayList<>();

        List<Path> jsons;
        try (Stream<Path> files = Files.walk(roomDir, FileVisitOption.FOLLOW_LINKS)) {
            jsons = files.filter(p -> p.toString().endsWith(".json")).sorted().collect(Collectors.toList());
        }
        Map<String, List<RoomCapture>> byTemplate = new TreeMap<>();
        for (Path json : jsons) {
            try {
                RoomCapture capture = read(json);
                if (capture == null) continue;
                byTemplate.computeIfAbsent(capture.templateId(), k -> new ArrayList<>()).add(capture);
            } catch (IOException | RuntimeException e) {
                problems.add(root.relativize(json) + ": " + e.getMessage());
            }
        }

        Map<String, List<RoomCapture>> rooms = new LinkedHashMap<>();
        List<Room> templates = new ArrayList<>();
        for (Map.Entry<String, List<RoomCapture>> e : byTemplate.entrySet()) {
            List<RoomCapture> usable = consistent(e.getKey(), e.getValue(), problems);
            rooms.put(e.getKey(), List.copyOf(usable));
            templates.add(template(e.getKey(), usable));
        }

        List<Doorway> doorways = new ArrayList<>();
        for (List<RoomCapture> list : rooms.values()) {
            for (RoomCapture c : list) {
                if (!hasDoorways(c)) continue;
                for (Map.Entry<DoorSlot, DoorType> d : c.doors().entrySet()) doorways.add(new Doorway(c, d.getKey(), d.getValue()));
            }
        }
        if (doorways.stream().noneMatch(d -> PastePlan.look(d.type()) == DoorType.NORMAL)) {
            problems.add("No capture has a normal door in it");
        }
        return new RoomLibrary(rooms, List.copyOf(doorways), List.copyOf(templates), List.copyOf(problems));
    }

    /** The capture reaches from the bottom to the top of the doorways. */
    static boolean hasDoorways(RoomCapture capture) {
        return capture.originY() <= PastePlan.DOOR_Y && capture.topY() >= PastePlan.DOOR_Y + PastePlan.DOOR_HEIGHT - 1;
    }

    /** Reads one capture's JSON; null (and no error) for types the generator has no use for. */
    static RoomCapture read(Path json) throws IOException {
        JsonObject o;
        try (Reader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            o = new JsonParser().parse(reader).getAsJsonObject();
        }
        String name = json.getFileName().toString();
        Path schematic = json.resolveSibling(name.substring(0, name.length() - ".json".length()) + ".schem");
        if (!Files.exists(schematic)) throw new IOException("missing " + schematic.getFileName());

        String id = o.get("id").getAsString();
        RoomType type = type(o.get("type").getAsString());
        RoomShape shape = shape(o.get("shape").getAsString());

        List<Position> cells = new ArrayList<>();
        for (JsonElement c : o.getAsJsonArray("cells")) cells.add(position(c.getAsJsonArray()));
        List<Integer> turns = shape.rotationsMatching(cells);
        if (turns.isEmpty()) throw new IOException("cells " + cells + " are not a " + shape);

        int[] size = new int[3];
        JsonArray s = o.getAsJsonArray("size");
        for (int i = 0; i < 3; i++) size[i] = s.get(i).getAsInt();
        Position max = new Position(0, 0);
        for (Position c : cells) max = new Position(Math.max(max.x(), c.x()), Math.max(max.y(), c.y()));
        if (size[0] != PastePlan.PITCH * (max.x() + 1) - 1 || size[2] != PastePlan.PITCH * (max.y() + 1) - 1) {
            throw new IOException("size " + size[0] + "x" + size[2] + " doesn't fit cells " + cells);
        }

        // Sorted, so that everything built from it comes out in the same order on every run.
        Map<DoorSlot, DoorType> doors = new TreeMap<>(Comparator.comparingInt((DoorSlot d) -> d.cell().y())
                .thenComparingInt(d -> d.cell().x()).thenComparing(DoorSlot::side));
        for (JsonElement d : o.getAsJsonArray("doors")) {
            JsonObject door = d.getAsJsonObject();
            DoorSlot slot = new DoorSlot(position(door.getAsJsonArray("cell")), Direction.valueOf(door.get("side").getAsString()));
            doors.put(slot, doorType(door.get("type").getAsString()));
        }

        return new RoomCapture(id, ALIASES.getOrDefault(id, id), type, shape, turns.get(0), o.get("originY").getAsInt(),
                size, List.copyOf(cells), Collections.unmodifiableMap(new LinkedHashMap<>(doors)), schematic);
    }

    /**
     * Keeps the captures that agree with the first on type and shape, and for rooms with fixed
     * doorways on where they are (a capture that disagrees was misread by the scanner).
     */
    private static List<RoomCapture> consistent(String id, List<RoomCapture> captures, List<String> problems) {
        RoomCapture first = captures.get(0);
        Set<DoorSlot> doorways = fixedDoorways(first) ? mostCommonDoorways(captures) : null;
        List<RoomCapture> out = new ArrayList<>();
        for (RoomCapture c : captures) {
            if (c.type() != first.type() || c.shape() != first.shape()) {
                problems.add(id + ": " + c.schematic().getFileName() + " is a " + c.type() + " " + c.shape() + ", expected "
                        + first.type() + " " + first.shape());
            } else if (doorways != null && !c.doors().keySet().equals(doorways)) {
                problems.add(id + ": " + c.schematic().getFileName() + " has doorways " + c.doors().keySet() + ", expected " + doorways);
            } else {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * 1x1 rooms other than the fairy room are built with a fixed set of doorways, so every capture
     * shows all of them. Everything else can have doors on any outside wall.
     */
    static boolean fixedDoorways(RoomCapture capture) {
        return capture.shape() == RoomShape.ONE_BY_ONE && capture.type() != RoomType.FAIRY;
    }

    private static Set<DoorSlot> mostCommonDoorways(List<RoomCapture> captures) {
        Map<Set<DoorSlot>, Integer> counts = new LinkedHashMap<>();
        for (RoomCapture c : captures) counts.merge(c.doors().keySet(), 1, Integer::sum);
        return Collections.max(counts.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private static Room template(String id, List<RoomCapture> captures) {
        RoomCapture first = captures.get(0);
        Room.RoomBuilder b = Room.builder()
                .id(id)
                .type(first.type())
                .shape(first.shape())
                .minimumFloor(minimumFloor(id))
                .rotations(HypixelRooms.rotations(first.shape()));
        // In the template frame; only 1x1 rooms have fixed doorways and there the frames are the same.
        if (fixedDoorways(first)) b.doorSlots(Set.copyOf(first.doors().keySet())).exactDoors(true);
        return b.build();
    }

    private static DungeonFloor minimumFloor(String id) {
        for (Room r : HypixelRooms.ALL) if (r.getId().equals(id)) return r.getMinimumFloor();
        return DungeonFloor.ENTRANCE;
    }

    private static RoomType type(String type) throws IOException {
        return switch (type) {
            case "NORMAL" -> RoomType.REGULAR;
            case "CHAMPION" -> RoomType.MINIBOSS;
            case "ENTRANCE" -> RoomType.START;
            case "RARE", "PUZZLE", "TRAP", "FAIRY", "BLOOD" -> RoomType.valueOf(type);
            default -> throw new IOException("unknown room type " + type);
        };
    }

    private static RoomShape shape(String shape) throws IOException {
        return switch (shape) {
            case "1x1" -> RoomShape.ONE_BY_ONE;
            case "1x2" -> RoomShape.ONE_BY_TWO;
            case "1x3" -> RoomShape.ONE_BY_THREE;
            case "1x4" -> RoomShape.ONE_BY_FOUR;
            case "2x2" -> RoomShape.TWO_BY_TWO;
            case "L" -> RoomShape.L_SHAPE;
            default -> throw new IOException("unknown shape " + shape);
        };
    }

    private static DoorType doorType(String type) {
        try {
            return DoorType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return DoorType.NORMAL;
        }
    }

    private static Position position(JsonArray a) {
        return new Position(a.get(0).getAsInt(), a.get(1).getAsInt());
    }

    /** Generator pool made of the captured rooms only, never repeating one in a dungeon. */
    public RoomPool pool() {
        return new RoomPool(templates, true);
    }

    public List<Room> templates() {
        return templates;
    }

    /** Captured variants of a template, empty if it was never captured. */
    public List<RoomCapture> captures(String templateId) {
        return rooms.getOrDefault(templateId, List.of());
    }

    /** Every doorway that was open in a capture. */
    public List<Doorway> doorways() {
        return doorways;
    }

    /** Lowest block of any room. */
    public int minY() {
        int y = PastePlan.DOOR_Y;
        for (List<RoomCapture> list : rooms.values()) for (RoomCapture c : list) y = Math.min(y, c.originY());
        return y;
    }

    /** Highest block of any room. */
    public int maxY() {
        int y = PastePlan.DOOR_Y + PastePlan.DOOR_HEIGHT - 1;
        for (List<RoomCapture> list : rooms.values()) for (RoomCapture c : list) y = Math.max(y, c.topY());
        return y;
    }

    /** Files that couldn't be used, and why. */
    public List<String> problems() {
        return problems;
    }

    /** e.g. "51 rooms (75 captures), 141 doorways". */
    public String summary() {
        int captures = 0;
        for (List<RoomCapture> list : rooms.values()) captures += list.size();
        return rooms.size() + " rooms (" + captures + " captures), " + doorways.size() + " doorways";
    }
}
