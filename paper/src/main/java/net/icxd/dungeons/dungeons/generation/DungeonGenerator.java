package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Generates a dungeon layout. See {@code DUNGEON_GEN.md} for the reasoning behind each stage.
 *
 * <ol>
 *   <li>{@link SpecialPlacer}: put the 1x1 special rooms on the grid (entrance, blood, fairy,
 *       puzzles, trap, miniboss).
 *   <li>{@link Tiler}: fill every other cell with regular rooms of random shapes.
 *   <li>{@link Connector}: pick which walls become doors so the rooms form a tree, with the fairy on
 *       the entrance-to-blood path, while respecting every template's door rules.
 *   <li>{@link Merger}: a 1x1 regular room left with a single door is merged into the room it
 *       hangs off when that makes a valid shape, otherwise it becomes a rare room.
 *   <li>{@link TemplateAssigner}: pick the concrete template + rotation for each room.
 *   <li>{@link DoorTyper}: entrance, wither and blood doors along the critical path.
 * </ol>
 *
 * Any stage can fail on an unlucky roll (e.g. a puzzle sealing off a corner); the whole thing is
 * then re-rolled. Generation is deterministic for a given seed.
 */
public final class DungeonGenerator {
    public static final int MAX_ATTEMPTS = 500;

    private final DungeonConfig config;
    private final RoomPool pool;
    /** Why attempts were thrown away, for tuning. */
    final Map<String, Integer> failures = new TreeMap<>();

    public DungeonGenerator(DungeonConfig config, RoomPool pool) {
        this.config = config;
        this.pool = pool;
    }

    public DungeonLayout generate() {
        return generate(new Random().nextLong());
    }

    public DungeonLayout generate(long seed) {
        Random random = new Random(seed);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            DungeonLayout layout = tryGenerate(random, seed, attempt);
            if (layout != null) return layout;
        }
        throw new IllegalStateException("Could not generate a dungeon for " + config + " (seed " + seed + ")");
    }

    private DungeonLayout tryGenerate(Random random, long seed, int attempt) {
        int w = config.width();
        int h = config.height();
        int[][] grid = new int[w][h];
        for (int[] column : grid) Arrays.fill(column, -1);

        // 1. Special rooms.
        List<RoomNode> rooms = new ArrayList<>();
        SpecialPlacer specials = new SpecialPlacer(config, w, h, random);
        if (!specials.place()) return fail("specials");
        for (SpecialPlacer.Special s : specials.placed()) {
            int id = rooms.size();
            grid[s.cell().x()][s.cell().y()] = id;
            rooms.add(new RoomNode(id, s.type(), RoomShape.ONE_BY_ONE, List.of(s.cell())));
        }
        for (Position p : specials.voidCells()) grid[p.x()][p.y()] = DungeonLayout.EMPTY;

        // 2. Regular rooms everywhere else.
        Set<Integer> blocked = new HashSet<>();
        for (RoomNode r : rooms) if (r.type == RoomType.START || r.type == RoomType.BLOOD) blocked.add(r.id);
        Map<Tiler.Piece, Integer> capacityCache = new HashMap<>();
        Tiler tiler = new Tiler(grid, random, config.shapeWeights(), pool.shapeLimits(config.floor()), blocked,
                piece -> capacityCache.computeIfAbsent(piece, pc -> doorCapacity(pc, grid)));
        for (Tiler.Piece piece : tiler.fill(rooms.size())) {
            rooms.add(new RoomNode(rooms.size(), RoomType.REGULAR, piece.shape(), piece.cells()));
        }

        for (RoomNode room : rooms) {
            room.options = new ArrayList<>();
            for (Room template : pool.templates(room.type, room.shape, config.floor())) {
                room.options.addAll(Placement.enumerate(template, room.cells, grid));
            }
            if (room.options.isEmpty()) return fail("no template for " + room.type + " " + room.shape);
            room.allOptions = room.options;
        }

        // 3. Doors.
        RoomNode entrance = first(rooms, RoomType.START);
        RoomNode fairy = first(rooms, RoomType.FAIRY);
        RoomNode blood = first(rooms, RoomType.BLOOD);
        Connector connector = new Connector(rooms, grid, random, config.newestBias(), config.pathNoise());
        if (!connector.connect(entrance, fairy, blood)) return fail(connector.failure);

        // A 1x1 regular room that ended up with a single door has no matching template (Hypixel has
        // no dead-end regular 1x1s). Merge it into its parent if that makes a valid shape, otherwise
        // it becomes one of Hypixel's "rare" rooms, which are exactly that: 1x1 brown dead ends.
        List<Integer> path = new ArrayList<>(connector.criticalPath());
        while (Merger.mergeStranded(rooms, grid, path, pool, config)) {
            // A merge can turn the parent into a shape another stranded neighbour can merge with.
        }
        for (RoomNode room : rooms) {
            if (room.satisfied()) continue;
            if (room.type == RoomType.REGULAR && room.shape == RoomShape.ONE_BY_ONE && room.doors.size() == 1) {
                List<Placement> rare = new ArrayList<>();
                for (Room template : pool.templates(RoomType.RARE, RoomShape.ONE_BY_ONE, config.floor())) {
                    for (Placement p : Placement.enumerate(template, room.cells, grid)) if (p.fits(room.doors)) rare.add(p);
                }
                if (!rare.isEmpty()) {
                    room.type = RoomType.RARE;
                    room.options = rare;
                    continue;
                }
            }
            return fail("no template fits " + room.type + " " + room.shape + " with " + room.doors.size() + " doors");
        }

        // 4. Templates.
        // A repeat is fine when the pool simply has fewer templates of that kind than the map has
        // rooms of it; otherwise it came from unlucky door choices, so re-roll.
        int[] repeats = new int[1];
        Placement[] chosen = new TemplateAssigner(rooms, random).assign(repeats);
        if (pool.canAvoidRepeats() && repeats[0] > unavoidableRepeats(rooms) && attempt < MAX_ATTEMPTS / 2) {
            return fail("repeated template");
        }

        // 5. Door types.
        Map<Edge, DoorType> types = DoorTyper.type(rooms, path);

        List<DungeonLayout.Door> doors = new ArrayList<>();
        List<List<DungeonLayout.Door>> doorsPerRoom = new ArrayList<>();
        for (int i = 0; i < rooms.size(); i++) doorsPerRoom.add(new ArrayList<>());
        for (RoomNode room : rooms) {
            if (room.parent < 0) continue;
            Edge edge = room.doors.get(0); // a room's first door is always the one it was entered through
            DungeonLayout.Door door = new DungeonLayout.Door(edge, room.parent, room.id, types.getOrDefault(edge, DoorType.NORMAL));
            doors.add(door);
            doorsPerRoom.get(room.id).add(door);
            doorsPerRoom.get(room.parent).add(door);
        }

        List<DungeonLayout.PlacedRoom> placed = new ArrayList<>();
        for (RoomNode room : rooms) {
            Placement p = chosen[room.id];
            placed.add(new DungeonLayout.PlacedRoom(
                    room.id, room.type, room.shape, p.template(), p.rotation(), room.cells,
                    room.parent, room.depth, List.copyOf(doorsPerRoom.get(room.id))));
        }
        return new DungeonLayout(grid, placed, doors, path, seed, attempt, config.specialColumn());
    }

    private int doorCapacity(Tiler.Piece piece, int[][] grid) {
        int best = 0;
        for (Room t : pool.templates(RoomType.REGULAR, piece.shape(), config.floor())) {
            for (Placement p : Placement.enumerate(t, piece.cells(), grid)) {
                best = Math.max(best, Math.min(p.doorLimit(), p.allowedDoors().size()));
            }
        }
        return best;
    }

    private int unavoidableRepeats(List<RoomNode> rooms) {
        Map<String, Integer> counts = new HashMap<>();
        for (RoomNode r : rooms) counts.merge(r.type + "/" + r.shape, 1, Integer::sum);
        int unavoidable = 0;
        for (RoomNode r : rooms) {
            String key = r.type + "/" + r.shape;
            Integer n = counts.remove(key);
            if (n != null) unavoidable += Math.max(0, n - pool.templates(r.type, r.shape, config.floor()).size());
        }
        return unavoidable;
    }

    private DungeonLayout fail(String reason) {
        failures.merge(reason, 1, Integer::sum);
        return null;
    }

    private static RoomNode first(List<RoomNode> rooms, RoomType type) {
        for (RoomNode r : rooms) if (r.type == type) return r;
        return null;
    }

    /**
     * Default shape weights (tuning). Nudged towards the shape mix of captured F6 runs: per dungeon
     * about 4 regular 1x1s, 2 1x2s, 1 1x3, 1-2 1x4s and 1-2 2x2s or Ls. Big rooms end up rarer than
     * their weight suggests because they need free space.
     */
    public static Map<RoomShape, Double> defaultShapeWeights() {
        Map<RoomShape, Double> weights = new EnumMap<>(RoomShape.class);
        weights.put(RoomShape.ONE_BY_ONE, 4.0);
        weights.put(RoomShape.ONE_BY_TWO, 2.0);
        weights.put(RoomShape.ONE_BY_THREE, 2.0);
        weights.put(RoomShape.ONE_BY_FOUR, 4.0);
        weights.put(RoomShape.TWO_BY_TWO, 6.0);
        weights.put(RoomShape.L_SHAPE, 2.0);
        return weights;
    }
}
