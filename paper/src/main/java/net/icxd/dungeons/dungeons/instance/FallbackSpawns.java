package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;

/**
 * Mobs for a room nobody has recorded, as research mobs.md 7.3 has them: not Hypixel's algorithm (which
 * isn't known), only fitted to the two recorded runs. Starred mobs on the main floor (y 67 to 74), in
 * groups of 2 to 6 standing 1 to 3 blocks apart, at least 3 blocks from the doorways; 5 to 16 of them a
 * square (the recorded rooms had 7 to 16 in a 1x1, 32 in a 1x2, 16 and 24 in an L); kinds as often as
 * recorded; 1 to 7 skeleton skulls a square; a champion room's miniboss near its middle. Only where
 * someone can walk to from the room's door ({@link #reachable}): a starred mob sealed in a hidden part
 * would keep the room from ever clearing. The unstarred extras Hypixel hides in a room's secret areas
 * are left out.
 */
final class FallbackSpawns {
    /** The main floor, where the recorded starred mobs stood. */
    static final int FLOOR_MIN_Y = 67;
    static final int FLOOR_MAX_Y = 74;
    static final int PER_SQUARE_MIN = 5;
    static final int PER_SQUARE_MAX = 16;
    static final int GROUP_MIN = 2;
    static final int GROUP_MAX = 6;
    /** How far a group's mobs stand from its first. */
    static final int GROUP_SPREAD = 3;
    static final int SKULLS_MIN = 1;
    static final int SKULLS_MAX = 7;
    /** No closer to a doorway than this. */
    static final int FROM_DOORS = 3;

    /** The kinds' shares of the 392 recorded room mobs, in percent (research mobs.md 0.2). */
    private static final List<MobKind> KINDS = List.of(MobKinds.ZOMBIE_GRUNT, MobKinds.CRYPT_LURKER, MobKinds.SKELETON_GRUNT,
            MobKinds.SCARED_SKELETON, MobKinds.TANK_ZOMBIE, MobKinds.CRYPT_DREADLORD, MobKinds.CRYPT_SOULEATER);
    private static final double[] WEIGHTS = {23, 22, 14, 13.5, 13, 9, 5};

    /** How far above a recorded spot that's inside something its mob is moved, at most. */
    static final int RAISE = 2;

    /** What the world has at a block. */
    interface Blocks {
        /** Something to stand on. */
        boolean solid(int x, int y, int z);

        /** Room to stand in (air and the like). */
        boolean passable(int x, int y, int z);

        /** Taller than a block (fences, walls): nobody steps up onto it. */
        default boolean tall(int x, int y, int z) {
            return false;
        }

        /** Something to climb or swim up (ladders, vines, water). */
        default boolean climbable(int x, int y, int z) {
            return false;
        }
    }

    /** Which block columns are a room's. */
    interface Area {
        boolean contains(int x, int z);
    }

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    record Spot(int x, int y, int z) {
        int distance(Spot other) {
            return Math.max(Math.abs(x - other.x), Math.max(Math.abs(y - other.y), Math.abs(z - other.z)));
        }
    }

    private FallbackSpawns() {
    }

    /**
     * Where a mob could stand in these columns ({x, z}): something solid under it and two free blocks, at
     * a height from {@code minY} to {@code maxY}; every such height in a column, lowest first.
     */
    static List<Spot> spots(Blocks blocks, List<int[]> columns, int minY, int maxY) {
        List<Spot> out = new ArrayList<>();
        for (int[] c : columns) {
            for (int y = minY; y <= maxY; y++) {
                if (blocks.solid(c[0], y - 1, c[1]) && blocks.passable(c[0], y, c[1]) && blocks.passable(c[0], y + 1, c[1])) {
                    out.add(new Spot(c[0], y, c[1]));
                }
            }
        }
        return out;
    }

    /**
     * Where a recorded mob stands: at its recorded height, unless that's inside something (Default's Angry
     * Archaeologist was recorded at y 69, where the capture has its dirt floor, whose top is 70); then on
     * the first spot at most {@link #RAISE} blocks up with something under it and room for it, or where
     * it was if there's none.
     */
    static double standY(Blocks blocks, int x, double y, int z) {
        int feet = (int) Math.floor(y);
        if (blocks.passable(x, feet, z) && blocks.passable(x, feet + 1, z)) return y;
        for (int up = feet + 1; up <= feet + RAISE; up++) {
            if (blocks.solid(x, up - 1, z) && blocks.passable(x, up, z) && blocks.passable(x, up + 1, z)) return up;
        }
        return y;
    }

    /**
     * A room's columns: its cells ({x, z} min corners, {@code cellSize} wide), the gaps between two of them
     * (and where four meet), and the columns of these blocks ({x, y, z}: its doorways).
     */
    static Area area(List<int[]> cellMins, int cellSize, List<int[]> extra) {
        Set<Long> extras = new HashSet<>();
        for (int[] b : extra) extras.add(column(b[0], b[2]));
        Area cells = (x, z) -> {
            for (int[] min : cellMins) {
                if (x >= min[0] && x < min[0] + cellSize && z >= min[1] && z < min[1] + cellSize) return true;
            }
            return false;
        };
        return (x, z) -> cells.contains(x, z) || extras.contains(column(x, z))
                || (cells.contains(x - 1, z) && cells.contains(x + 1, z))
                || (cells.contains(x, z - 1) && cells.contains(x, z + 1))
                || (cells.contains(x - 1, z - 1) && cells.contains(x + 1, z - 1) && cells.contains(x - 1, z + 1) && cells.contains(x + 1, z + 1));
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /**
     * Where someone can walk to from these spots (inside a door), in {@code area} and from {@code minY} to
     * {@code maxY}: along the ground, a block up where there's room to jump (not onto a fence or a wall),
     * any way down, and up ladders, vines and water. Nothing is opened on the way (doors, levers, weak
     * walls), so a room's hidden parts aren't in it. Spots are where the feet are, as {@link #spots}'.
     */
    static Set<Spot> reachable(Blocks blocks, Area area, Collection<Spot> starts, int minY, int maxY) {
        Set<Spot> seen = new HashSet<>(starts);
        Deque<Spot> queue = new ArrayDeque<>(seen);
        while (!queue.isEmpty()) {
            Spot at = queue.poll();
            if (at.y < maxY && blocks.climbable(at.x, at.y, at.z) && stand(blocks, at.x, at.y + 1, at.z)) {
                Spot up = new Spot(at.x, at.y + 1, at.z);
                if (seen.add(up)) queue.add(up);
            }
            for (int[] side : SIDES) {
                int x = at.x + side[0];
                int z = at.z + side[1];
                if (!area.contains(x, z)) continue;
                Spot to = step(blocks, at, x, z, minY, maxY);
                if (to != null && seen.add(to)) queue.add(to);
            }
        }
        return seen;
    }

    /** From a spot to the next column: level, a block up, or down to wherever that ends; null if it can't. */
    private static Spot step(Blocks blocks, Spot from, int x, int z, int minY, int maxY) {
        int y = from.y;
        if (stand(blocks, x, y, z)) return new Spot(x, y, z);
        if (room(blocks, x, y, z) && room(blocks, x, y + 1, z)) {
            for (int down = y - 1; down >= minY; down--) {
                if (!room(blocks, x, down, z)) return null;
                if (stand(blocks, x, down, z)) return new Spot(x, down, z);
            }
            return null;
        }
        if (y + 1 <= maxY && room(blocks, from.x, y + 2, from.z) && !blocks.tall(x, y, z) && stand(blocks, x, y + 1, z)) {
            return new Spot(x, y + 1, z);
        }
        return null;
    }

    /** Feet here: something under them (anything that isn't air and the like), or something to hold on to, and room for the head. */
    private static boolean stand(Blocks blocks, int x, int y, int z) {
        return (!blocks.passable(x, y - 1, z) || blocks.climbable(x, y, z)) && room(blocks, x, y, z) && room(blocks, x, y + 1, z);
    }

    private static boolean room(Blocks blocks, int x, int y, int z) {
        return blocks.passable(x, y, z) || blocks.climbable(x, y, z);
    }

    /**
     * The columns of a room's cells, less the outer wall and anything within {@link #FROM_DOORS} of a door
     * (given as its blocks, {x, y, z}).
     *
     * @param cellMins each cell's min corner {x, z}; cells are {@link net.icxd.dungeons.dungeons.paste.PastePlan#CELL} wide
     */
    static List<int[]> columns(List<int[]> cellMins, int cellSize, List<int[]> doorBlocks) {
        List<int[]> out = new ArrayList<>();
        for (int[] min : cellMins) {
            for (int x = min[0] + 1; x < min[0] + cellSize - 1; x++) {
                for (int z = min[1] + 1; z < min[1] + cellSize - 1; z++) {
                    boolean nearDoor = false;
                    for (int[] d : doorBlocks) {
                        if (Math.max(Math.abs(d[0] - x), Math.abs(d[2] - z)) < FROM_DOORS) {
                            nearDoor = true;
                            break;
                        }
                    }
                    if (!nearDoor) out.add(new int[]{x, z});
                }
            }
        }
        return out;
    }

    /** How many starred mobs a room of this many squares gets. */
    static int starredCount(int squares, RandomGenerator random) {
        int count = 0;
        for (int i = 0; i < squares; i++) count += random.nextInt(PER_SQUARE_MIN, PER_SQUARE_MAX + 1);
        return count;
    }

    /** How many skeleton skulls. */
    static int skullCount(int squares, RandomGenerator random) {
        int count = 0;
        for (int i = 0; i < squares; i++) count += random.nextInt(SKULLS_MIN, SKULLS_MAX + 1);
        return count;
    }

    /**
     * {@code count} spots in groups: each group starts at a random free spot and takes 1 to 5 more free
     * ones within {@link #GROUP_SPREAD} of it. Fewer if the room runs out of spots.
     */
    static List<Spot> groups(List<Spot> spots, int count, RandomGenerator random) {
        List<Spot> free = new ArrayList<>(spots);
        List<Spot> out = new ArrayList<>();
        while (out.size() < count && !free.isEmpty()) {
            Spot first = free.remove(random.nextInt(free.size()));
            out.add(first);
            int size = Math.min(count - out.size() + 1, random.nextInt(GROUP_MIN, GROUP_MAX + 1));
            List<Spot> near = new ArrayList<>();
            for (Spot s : free) {
                if (s.distance(first) <= GROUP_SPREAD && s.distance(first) >= 1) near.add(s);
            }
            for (int i = 1; i < size && !near.isEmpty(); i++) {
                Spot s = near.remove(random.nextInt(near.size()));
                free.remove(s);
                out.add(s);
            }
            // Groups stand apart: the next doesn't start among this one.
            Set<Spot> taken = new HashSet<>(out);
            free.removeIf(s -> !taken.contains(s) && s.distance(first) <= GROUP_SPREAD);
        }
        return out;
    }

    /** A spot at random (for skulls, which stand alone); null for none. */
    static Spot any(List<Spot> spots, RandomGenerator random) {
        return spots.isEmpty() ? null : spots.get(random.nextInt(spots.size()));
    }

    /** The spot nearest this point (the miniboss's, near the room's middle); null for none. */
    static Spot nearest(List<Spot> spots, double x, double y, double z) {
        Spot best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Spot s : spots) {
            double dx = s.x + 0.5 - x;
            double dy = s.y - y;
            double dz = s.z + 0.5 - z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestDistance) {
                best = s;
                bestDistance = d;
            }
        }
        return best;
    }

    /** A kind for a room mob, as often as each was recorded. */
    static MobKind kind(RandomGenerator random) {
        double total = 0;
        for (double w : WEIGHTS) total += w;
        double pick = random.nextDouble() * total;
        for (int i = 0; i < WEIGHTS.length; i++) {
            pick -= WEIGHTS[i];
            if (pick < 0) return KINDS.get(i);
        }
        return KINDS.get(0);
    }

    /**
     * A champion room's miniboss: a Lost Adventurer or an Angry Archaeologist (one each was recorded in
     * the Entrance's champion rooms, both Lv90; which one a room gets is UNKNOWN, so either as often).
     */
    static MobKind miniboss(RandomGenerator random) {
        return random.nextBoolean() ? MobKinds.LOST_ADVENTURER : MobKinds.ANGRY_ARCHAEOLOGIST;
    }

    /** The level both recorded champion room minibosses had. */
    static final int MINIBOSS_LEVEL = 90;
}
