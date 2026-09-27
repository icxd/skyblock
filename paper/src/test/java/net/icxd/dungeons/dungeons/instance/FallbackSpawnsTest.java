package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;

class FallbackSpawnsTest {
    /** A flat floor at y 68 (so mobs stand at 69) with a solid pillar at (5, 69..70, 5). */
    private static final FallbackSpawns.Blocks FLOOR = new FallbackSpawns.Blocks() {
        @Override
        public boolean solid(int x, int y, int z) {
            return y == 68 || (x == 5 && z == 5 && (y == 69 || y == 70));
        }

        @Override
        public boolean passable(int x, int y, int z) {
            return !solid(x, y, z);
        }
    };

    @Test
    void spotsStandOnSomethingWithRoomAbove() {
        List<int[]> columns = List.of(new int[]{1, 1}, new int[]{5, 5});
        List<FallbackSpawns.Spot> spots = FallbackSpawns.spots(FLOOR, columns, 67, 74);
        // On the floor at (1, 1); on top of the pillar at (5, 5), not inside it.
        assertEquals(List.of(new FallbackSpawns.Spot(1, 69, 1), new FallbackSpawns.Spot(5, 71, 5)), spots);
    }

    /** A recorded mob inside something stands on it instead (Default's Angry Archaeologist, recorded at the dirt floor's y 69). */
    @Test
    void recordedMobsInsideAFloorStandOnIt() {
        // In the floor (y 68) and in the pillar (y 69, 70): up onto them.
        assertEquals(69, FallbackSpawns.standY(FLOOR, 1, 68, 1), 1e-9);
        assertEquals(71, FallbackSpawns.standY(FLOOR, 5, 69.5, 5), 1e-9);
        // Where there's room, as recorded (half a block over the floor, say).
        assertEquals(69, FallbackSpawns.standY(FLOOR, 1, 69, 1), 1e-9);
        assertEquals(70.5, FallbackSpawns.standY(FLOOR, 1, 70.5, 1), 1e-9);
        // Under the floor by the pillar, nothing free within 2 blocks: left as it was.
        assertEquals(67.5, FallbackSpawns.standY(FLOOR, 5, 67.5, 5), 1e-9);
    }

    @Test
    void columnsKeepOffTheWallsAndDoors() {
        // One cell at the origin, a door block in the middle of its north wall.
        List<int[]> columns = FallbackSpawns.columns(List.<int[]>of(new int[]{0, 0}), 31, List.<int[]>of(new int[]{15, 69, 0}));
        Set<String> all = new HashSet<>();
        for (int[] c : columns) all.add(c[0] + "," + c[1]);
        assertFalse(all.contains("0,5"), "the outer wall");
        assertFalse(all.contains("30,5"), "the outer wall");
        assertFalse(all.contains("15,2"), "2 from the door");
        assertTrue(all.contains("15,3"), "3 from the door");
        assertTrue(all.contains("1,1"));
        assertEquals(29 * 29 - 5 * 2, columns.size());
    }

    /** Blocks made up for a test: solid ones, fences, ladders and water; air everywhere else. */
    private static final class Scene implements FallbackSpawns.Blocks {
        final Set<List<Integer>> solid = new HashSet<>();
        final Set<List<Integer>> fences = new HashSet<>();
        final Set<List<Integer>> ladders = new HashSet<>();
        final Set<List<Integer>> water = new HashSet<>();

        /** A floor with its top at y 69 (feet at 69) over x 0..sizeX-1, z 0..sizeZ-1. */
        Scene(int sizeX, int sizeZ) {
            fill(0, 68, 0, sizeX - 1, 68, sizeZ - 1, solid);
        }

        Scene fill(int x1, int y1, int z1, int x2, int y2, int z2, Set<List<Integer>> into) {
            for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) into.add(List.of(x, y, z));
            return this;
        }

        @Override
        public boolean solid(int x, int y, int z) {
            return solid.contains(List.of(x, y, z)) || fences.contains(List.of(x, y, z));
        }

        @Override
        public boolean passable(int x, int y, int z) {
            List<Integer> at = List.of(x, y, z);
            return !solid.contains(at) && !fences.contains(at) && !ladders.contains(at);
        }

        @Override
        public boolean tall(int x, int y, int z) {
            return fences.contains(List.of(x, y, z));
        }

        @Override
        public boolean climbable(int x, int y, int z) {
            return ladders.contains(List.of(x, y, z)) || water.contains(List.of(x, y, z));
        }
    }

    private static FallbackSpawns.Area box(int sizeX, int sizeZ) {
        return (x, z) -> x >= 0 && x < sizeX && z >= 0 && z < sizeZ;
    }

    private static Set<FallbackSpawns.Spot> fromTheDoor(Scene scene, int sizeX, int sizeZ) {
        return FallbackSpawns.reachable(scene, box(sizeX, sizeZ), List.of(new FallbackSpawns.Spot(0, 69, 0)), 60, 80);
    }

    private static FallbackSpawns.Spot spot(int x, int y, int z) {
        return new FallbackSpawns.Spot(x, y, z);
    }

    /**
     * A pocket walled off all round (a hidden room behind a weak wall, or behind nothing that opens) is out
     * of reach, however much room there is in it: mobs there couldn't be killed.
     */
    @Test
    void sealedPocketsAreOutOfReach() {
        Scene scene = new Scene(12, 12);
        // Walls along x 7 and z 7 up to y 73, fencing off x 8..11, z 8..11.
        scene.fill(7, 69, 7, 11, 73, 7, scene.solid).fill(7, 69, 7, 7, 73, 11, scene.solid);
        Set<FallbackSpawns.Spot> reach = fromTheDoor(scene, 12, 12);
        assertTrue(reach.contains(spot(5, 69, 5)));
        assertTrue(reach.contains(spot(11, 69, 0)));
        assertFalse(reach.contains(spot(9, 69, 9)));
        assertFalse(reach.contains(spot(11, 69, 11)));
        // Filtering the room's spots keeps only the ones outside it.
        List<int[]> columns = new ArrayList<>();
        for (int x = 1; x < 11; x++) for (int z = 1; z < 11; z++) columns.add(new int[]{x, z});
        List<FallbackSpawns.Spot> spots = new ArrayList<>(FallbackSpawns.spots(scene, columns, 67, 74));
        spots.retainAll(reach);
        assertFalse(spots.isEmpty());
        assertTrue(spots.stream().noneMatch(s -> s.x() >= 8 && s.z() >= 8), spots.toString());
        // Nor anywhere outside the room's area.
        assertTrue(reach.stream().allMatch(s -> s.x() >= 0 && s.x() < 12 && s.z() >= 0 && s.z() < 12));
    }

    /** A block up with room to jump, not two, not onto a fence; and down again, any way. */
    @Test
    void stepsAndDrops() {
        Scene step = new Scene(6, 1);
        step.solid.add(List.of(3, 69, 0));
        Set<FallbackSpawns.Spot> reach = fromTheDoor(step, 6, 1);
        assertTrue(reach.contains(spot(3, 70, 0)), "onto the block");
        assertTrue(reach.contains(spot(5, 69, 0)), "down the other side");

        Scene two = new Scene(6, 1);
        two.fill(3, 69, 0, 3, 70, 0, two.solid);
        assertFalse(fromTheDoor(two, 6, 1).contains(spot(5, 69, 0)), "two blocks up");

        Scene fence = new Scene(6, 1);
        fence.fences.add(List.of(3, 69, 0));
        assertFalse(fromTheDoor(fence, 6, 1).contains(spot(5, 69, 0)), "a fence");

        Scene low = new Scene(6, 1);
        low.solid.add(List.of(3, 69, 0));
        low.fill(0, 71, 0, 2, 71, 0, low.solid);
        assertFalse(fromTheDoor(low, 6, 1).contains(spot(3, 70, 0)), "no room to jump under a ceiling at y 71");

        // A pit: its floor 4 lower, dropped into.
        Scene pit = new Scene(3, 1);
        pit.fill(3, 64, 0, 5, 64, 0, pit.solid);
        Set<FallbackSpawns.Spot> down = fromTheDoor(pit, 6, 1);
        assertTrue(down.contains(spot(4, 65, 0)));
        assertFalse(down.contains(spot(4, 69, 0)), "nothing to stand on up there");
    }

    /** Up a ladder onto a floor 5 higher, which isn't reached without it; across water. */
    @Test
    void laddersAndWater() {
        Scene wall = new Scene(3, 1);
        wall.fill(3, 68, 0, 6, 73, 0, wall.solid);
        assertFalse(fromTheDoor(wall, 7, 1).contains(spot(5, 74, 0)));
        wall.fill(2, 69, 0, 2, 73, 0, wall.ladders);
        assertTrue(fromTheDoor(wall, 7, 1).contains(spot(5, 74, 0)), "up the ladder");

        // A pool 3 deep between two floors: swum across and climbed out of.
        Scene pool = new Scene(8, 1);
        pool.solid.removeIf(b -> b.get(0) >= 3 && b.get(0) <= 4);
        pool.fill(3, 65, 0, 4, 65, 0, pool.solid).fill(3, 66, 0, 4, 68, 0, pool.water);
        assertTrue(fromTheDoor(pool, 8, 1).contains(spot(7, 69, 0)));
    }

    /** A room's columns: its cells, the gap between two of them, its doorways; not the gap by a cell it hasn't. */
    @Test
    void theRoomsArea() {
        // An L: cells at (0, 0), (32, 0) and (32, 32), 31 wide; a door block out at (15, 69, -1).
        FallbackSpawns.Area area = FallbackSpawns.area(List.of(new int[]{0, 0}, new int[]{32, 0}, new int[]{32, 32}), 31,
                List.<int[]>of(new int[]{15, 69, -1}));
        assertTrue(area.contains(0, 0));
        assertTrue(area.contains(31, 10), "the gap between two of its cells");
        assertTrue(area.contains(40, 31), "and the other");
        assertFalse(area.contains(31, 31), "where its cells meet the one it hasn't");
        assertFalse(area.contains(10, 40), "the cell it hasn't");
        assertTrue(area.contains(15, -1), "its doorway");
        assertFalse(area.contains(16, -2));
        // A 2x2's middle, where four meet.
        FallbackSpawns.Area square = FallbackSpawns.area(List.of(new int[]{0, 0}, new int[]{32, 0}, new int[]{0, 32}, new int[]{32, 32}), 31,
                List.of());
        assertTrue(square.contains(31, 31));
    }

    /** 5 to 16 a square (research mobs.md 7.3). */
    @Test
    void countsPerSquare() {
        Random random = new Random(1);
        for (int i = 0; i < 200; i++) {
            int one = FallbackSpawns.starredCount(1, random);
            assertTrue(one >= 5 && one <= 16, "1x1: " + one);
            int four = FallbackSpawns.starredCount(4, random);
            assertTrue(four >= 20 && four <= 64, "2x2: " + four);
            int skulls = FallbackSpawns.skullCount(3, random);
            assertTrue(skulls >= 3 && skulls <= 21, "skulls: " + skulls);
        }
    }

    @Test
    void groupsStandTogetherAndApart() {
        List<FallbackSpawns.Spot> spots = new ArrayList<>();
        for (int x = 1; x < 30; x++) for (int z = 1; z < 30; z++) spots.add(new FallbackSpawns.Spot(x, 69, z));
        Random random = new Random(7);
        List<FallbackSpawns.Spot> picked = FallbackSpawns.groups(spots, 16, random);
        assertEquals(16, picked.size());
        assertEquals(16, new HashSet<>(picked).size(), "one mob a spot");
        // Every mob has another within 3 blocks (they come in groups of 2 or more), unless the count ran out on a group of one.
        int alone = 0;
        for (FallbackSpawns.Spot s : picked) {
            boolean near = picked.stream().anyMatch(o -> o != s && o.distance(s) <= FallbackSpawns.GROUP_SPREAD);
            if (!near) alone++;
        }
        assertTrue(alone <= 1, "alone: " + alone);
        // Not more than there's room for.
        assertEquals(2, FallbackSpawns.groups(spots.subList(0, 2), 10, random).size());
        assertTrue(FallbackSpawns.groups(List.of(), 10, random).isEmpty());
    }

    /** The kinds come about as often as recorded (ZG 23, CL 22, SG 14, SS 13.5, TZ 13, CD 9, CS 5 %). */
    @Test
    void kindsAsRecorded() {
        Random random = new Random(3);
        Map<MobKind, Integer> counts = new HashMap<>();
        int n = 20_000;
        for (int i = 0; i < n; i++) counts.merge(FallbackSpawns.kind(random), 1, Integer::sum);
        assertEquals(7, counts.size());
        assertEquals(0.23 / 0.995, counts.get(MobKinds.ZOMBIE_GRUNT) / (double) n, 0.02);
        assertEquals(0.05 / 0.995, counts.get(MobKinds.CRYPT_SOULEATER) / (double) n, 0.01);
        assertFalse(counts.containsKey(MobKinds.UNDEAD_SKELETON));
        MobKind miniboss = FallbackSpawns.miniboss(random);
        assertTrue(miniboss == MobKinds.LOST_ADVENTURER || miniboss == MobKinds.ANGRY_ARCHAEOLOGIST);
    }

    @Test
    void theMinibossStandsNearTheMiddle() {
        List<FallbackSpawns.Spot> spots = List.of(new FallbackSpawns.Spot(2, 69, 2), new FallbackSpawns.Spot(14, 69, 16),
                new FallbackSpawns.Spot(15, 80, 15));
        assertEquals(new FallbackSpawns.Spot(14, 69, 16), FallbackSpawns.nearest(spots, 15.5, 69, 15.5));
        assertNull(FallbackSpawns.nearest(List.of(), 0, 0, 0));
    }
}
