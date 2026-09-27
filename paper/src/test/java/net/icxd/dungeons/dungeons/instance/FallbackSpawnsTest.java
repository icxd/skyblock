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
