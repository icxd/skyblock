package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;

class FloorMobsTest {
    /** On Floor I a skeleton's spot is one of Floor I's skeletons, in the recorded shares (72, 102, 61, 10 of 245). */
    @Test
    void floorOneSkeletons() {
        SplittableRandom random = new SplittableRandom(3);
        Map<MobKind, Integer> seen = new HashMap<>();
        int runs = 49_000;
        for (int i = 0; i < runs; i++) seen.merge(FloorMobs.kind(MobKinds.SCARED_SKELETON, DungeonFloor.FLOOR_1, random), 1, Integer::sum);
        assertEquals(Set.of(MobKinds.SKELETON_GRUNT, MobKinds.SKELETON_SOLDIER, MobKinds.SCARED_SKELETON, MobKinds.SNIPER), seen.keySet());
        assertEquals(102 / 245.0, seen.get(MobKinds.SKELETON_SOLDIER) / (double) runs, 0.01);
        assertEquals(10 / 245.0, seen.get(MobKinds.SNIPER) / (double) runs, 0.005);
    }

    @Test
    void othersStay() {
        SplittableRandom random = new SplittableRandom(3);
        assertSame(MobKinds.ZOMBIE_GRUNT, FloorMobs.kind(MobKinds.ZOMBIE_GRUNT, DungeonFloor.FLOOR_1, random));
        assertSame(MobKinds.CRYPT_DREADLORD, FloorMobs.kind(MobKinds.CRYPT_DREADLORD, DungeonFloor.FLOOR_1, random));
        for (int i = 0; i < 100; i++) assertSame(MobKinds.SKELETON_GRUNT, FloorMobs.kind(MobKinds.SKELETON_GRUNT, DungeonFloor.ENTRANCE, random));
    }

    /** The data's Entrance levels: kept where the floor has them, else the floor's first. */
    @Test
    void levels() {
        assertEquals(90, FloorMobs.level(MobKinds.LOST_ADVENTURER, DungeonFloor.FLOOR_1, 90));
        assertNull(FloorMobs.level(MobKinds.ANGRY_ARCHAEOLOGIST, DungeonFloor.FLOOR_1, 80));
        assertEquals(90, MobKinds.ANGRY_ARCHAEOLOGIST.variant(DungeonFloor.FLOOR_1, null).level());
        assertNull(FloorMobs.level(MobKinds.ZOMBIE_GRUNT, DungeonFloor.FLOOR_1, null));
    }

    /** One welcome line on Floor I (all three recorded runs), and the first head 5.2 s after the welcome's last. */
    @Test
    void watcherOnFloorOne() {
        assertEquals(java.util.List.of("Ah, we meet again. As I foresaw..."), Watcher.intro(DungeonFloor.FLOOR_1));
        assertEquals(396, Watcher.firstRelease(Watcher.intro(DungeonFloor.ENTRANCE).size()));
        assertEquals(108, Watcher.firstRelease(1));
    }

    /** Health plus 1,000 times -2 to 6: 12k to 20k on the Entrance, 15k to 23k on Floor I, as recorded. */
    @Test
    void undeadHealth() {
        double eMin = Double.MAX_VALUE, eMax = 0, fMin = Double.MAX_VALUE, fMax = 0;
        for (int i = 0; i < 2_000; i++) {
            double e = UndeadType.CANNIBAL.rollHealth(DungeonFloor.ENTRANCE);
            double f = UndeadType.CANNIBAL.rollHealth(DungeonFloor.FLOOR_1);
            assertTrue(e % 1_000 == 0 && f % 1_000 == 0);
            eMin = Math.min(eMin, e);
            eMax = Math.max(eMax, e);
            fMin = Math.min(fMin, f);
            fMax = Math.max(fMax, f);
        }
        assertEquals(12_000, eMin);
        assertEquals(20_000, eMax);
        assertEquals(15_000, fMin);
        assertEquals(23_000, fMax);
    }
}
