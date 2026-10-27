package net.icxd.dungeons.dungeons.paste;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;

class BossArenaTest {
    @Test
    void floors() {
        assertSame(BossArena.BONZO, BossArena.of(DungeonFloor.FLOOR_1));
        assertNull(BossArena.of(DungeonFloor.ENTRANCE));
        assertNull(BossArena.of(DungeonFloor.FLOOR_2));
    }

    /** Bonzo's ring is clear of Floor I's cells (4x5 from -200, so up to x -73 and z -41), and the arrival and middle are in it. */
    @Test
    void besideTheFloor() {
        BossArena arena = BossArena.BONZO;
        int lastCellX = PastePlan.HYPIXEL_BASE + PastePlan.PITCH * DungeonFloor.FLOOR_1.getMaxXSize() - 2;
        int lastCellZ = PastePlan.HYPIXEL_BASE + PastePlan.PITCH * DungeonFloor.FLOOR_1.getMaxZSize() - 2;
        assertTrue(arena.box().min().x() > lastCellX || arena.box().min().z() > lastCellZ);
        assertTrue(arena.contains(arena.arrive()[0], arena.arrive()[1], arena.arrive()[2]));
        assertTrue(arena.contains(arena.center()[0], arena.center()[1], arena.center()[2]));
        assertFalse(arena.contains(-80, 71, 20));
        assertFalse(arena.contains(-42, 148, 20));
    }
}
