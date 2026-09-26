package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Stage 1a: picks cells for the special rooms. They're all 1x1, so they go down before the
 * regular rooms are tiled around them.
 *
 * <p>Every special room except the fairy has exactly one door, so they can't be walked through.
 * Each placement is checked to keep every other cell connected and every dead end touching a free
 * cell. With that invariant the connector can always build a tree unless templates' door rules
 * forbid it.
 *
 * <p>On floors with a {@linkplain DungeonConfig#specialColumn() special column} the map is one
 * column narrower than its grid, and the last column is empty apart from the odd puzzle, trap or
 * miniboss room sticking out of the east side (seen on real F6 captures: 5x6 plus 0-1 rooms in the
 * 6th column).
 */
final class SpecialPlacer {
    record Special(RoomType type, Position cell) {
    }

    private static final int FREE = 0;
    private static final int WALKABLE_SPECIAL = 1;
    private static final int DEAD_END = 2;
    /** Special-column cell with nothing in it. */
    private static final int VOID = 3;

    private final DungeonConfig config;
    private final int width;
    private final int height;
    /** Width of the part that's always filled. */
    private final int baseWidth;
    private final Random random;
    private final List<Special> placed = new ArrayList<>();
    private final int[][] state;

    SpecialPlacer(DungeonConfig config, int width, int height, Random random) {
        this.config = config;
        this.width = width;
        this.height = height;
        this.baseWidth = config.specialColumn() ? width - 1 : width;
        this.random = random;
        this.state = new int[width][height];
        if (config.specialColumn()) {
            for (int y = 0; y < height; y++) state[width - 1][y] = VOID;
        }
    }

    List<Special> placed() {
        return placed;
    }

    /** Special-column cells left empty; they stay empty in the dungeon. */
    List<Position> voidCells() {
        List<Position> out = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) if (state[x][y] == VOID) out.add(new Position(x, y));
        }
        return out;
    }

    boolean place() {
        Position entrance = pick(FREE, cell -> config.entranceOnEdge() && !onEdge(cell) ? 0 : 1);
        if (entrance == null || !add(RoomType.START, entrance, DEAD_END)) return false;

        // Blood: far away from the entrance, usually (not always) on the edge.
        int far = Math.max(2, (int) Math.ceil((baseWidth + height - 2) * config.bloodDistance()));
        Position blood = pick(FREE, cell -> {
            if (cell.manhattan(entrance) < far) return 0;
            return onEdge(cell) ? config.bloodEdgeWeight() : 1;
        });
        if (blood == null || !add(RoomType.BLOOD, blood, DEAD_END)) return false;

        if (config.fairy()) {
            // Roughly between the two, and never next to either so each has a regular room in front of it.
            int direct = entrance.manhattan(blood);
            Position fairy = pick(FREE, cell -> {
                int a = cell.manhattan(entrance);
                int b = cell.manhattan(blood);
                return a >= 2 && b >= 2 && a + b <= direct + 2 ? 1 : 0;
            });
            if (fairy == null || !add(RoomType.FAIRY, fairy, WALKABLE_SPECIAL)) return false;
        }

        // Puzzles & co. hang off the ends of branches, which is mostly the edge of the map.
        List<RoomType> deadEnds = new ArrayList<>();
        int puzzles = config.minPuzzles() + random.nextInt(config.maxPuzzles() - config.minPuzzles() + 1);
        for (int i = 0; i < puzzles; i++) deadEnds.add(RoomType.PUZZLE);
        for (int i = 0; i < config.traps(); i++) deadEnds.add(RoomType.TRAP);
        for (int i = 0; i < config.minibosses(); i++) deadEnds.add(RoomType.MINIBOSS);
        Collections.shuffle(deadEnds, random);
        for (RoomType type : deadEnds) {
            boolean column = config.specialColumn() && random.nextDouble() < config.specialColumnChance();
            if (!(column && placeDeadEnd(type, true)) && !placeDeadEnd(type, false)) return false;
        }
        return true;
    }

    private boolean placeDeadEnd(RoomType type, boolean column) {
        for (int tries = 0; tries < 20; tries++) {
            Position cell = column
                    ? pick(VOID, c -> 1)
                    : pick(FREE, c -> onEdge(c) ? config.deadEndEdgeWeight() : 1);
            if (cell != null && add(type, cell, DEAD_END)) return true;
        }
        return false;
    }

    private interface Weight {
        double of(Position cell);
    }

    /** Random cell in state {@code from}, weighted. */
    private Position pick(int from, Weight weight) {
        List<Position> cells = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (state[x][y] != from) continue;
                Position p = new Position(x, y);
                double w = weight.of(p);
                if (w <= 0) continue;
                cells.add(p);
                weights.add(w);
                total += w;
            }
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < cells.size(); i++) {
            roll -= weights.get(i);
            if (roll <= 0) return cells.get(i);
        }
        return cells.isEmpty() ? null : cells.get(cells.size() - 1);
    }

    /** Places the room if the map stays solvable, otherwise leaves everything as it was. */
    private boolean add(RoomType type, Position cell, int kind) {
        int before = state[cell.x()][cell.y()];
        state[cell.x()][cell.y()] = kind;
        if (!solvable()) {
            state[cell.x()][cell.y()] = before;
            return false;
        }
        placed.add(new Special(type, cell));
        return true;
    }

    /** Walkable cells form one region and every dead end has a free neighbour. */
    private boolean solvable() {
        Position start = null;
        int walkable = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!walkable(state[x][y])) continue;
                walkable++;
                if (start == null) start = new Position(x, y);
            }
        }
        if (start == null) return false;

        boolean[][] seen = new boolean[width][height];
        Deque<Position> queue = new ArrayDeque<>();
        queue.add(start);
        seen[start.x()][start.y()] = true;
        int reached = 0;
        while (!queue.isEmpty()) {
            Position p = queue.poll();
            reached++;
            for (Direction d : Direction.values()) {
                Position n = p.offset(d);
                if (inside(n) && !seen[n.x()][n.y()] && walkable(state[n.x()][n.y()])) {
                    seen[n.x()][n.y()] = true;
                    queue.add(n);
                }
            }
        }
        if (reached != walkable) return false;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (state[x][y] != DEAD_END) continue;
                boolean touches = false;
                for (Direction d : Direction.values()) {
                    Position n = new Position(x, y).offset(d);
                    // Must touch a free cell; don't rely on the fairy, it needs its doors for the path.
                    if (inside(n) && state[n.x()][n.y()] == FREE) touches = true;
                }
                if (!touches) return false;
            }
        }
        return true;
    }

    private static boolean walkable(int s) {
        return s == FREE || s == WALKABLE_SPECIAL;
    }

    /** On the border of the always-filled part of the map. */
    private boolean onEdge(Position p) {
        return p.x() == 0 || p.y() == 0 || p.x() == baseWidth - 1 || p.y() == height - 1;
    }

    private boolean inside(Position p) {
        return p.x() >= 0 && p.y() >= 0 && p.x() < width && p.y() < height;
    }
}
