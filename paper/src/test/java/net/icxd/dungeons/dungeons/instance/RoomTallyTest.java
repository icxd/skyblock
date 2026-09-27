package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** When a room counts as cleared: its last starred mob dead, all of them spawned, and only once. */
class RoomTallyTest {
    private static RoomTally planned(int starred) {
        RoomTally tally = new RoomTally();
        for (int i = 0; i < starred; i++) tally.plan();
        return tally;
    }

    @Test
    void clearedByTheLastStarredDeath() {
        RoomTally tally = planned(3);
        for (int i = 0; i < 3; i++) tally.spawned();
        assertFalse(tally.open());
        assertFalse(tally.died());
        assertFalse(tally.died());
        assertFalse(tally.cleared());
        assertTrue(tally.died(), "the last one");
        assertTrue(tally.cleared());
    }

    /** A second death after it's cleared (or opening it again) doesn't clear it again. */
    @Test
    void clearedOnce() {
        RoomTally tally = planned(1);
        tally.spawned();
        tally.open();
        assertTrue(tally.died());
        assertFalse(tally.died());
        assertFalse(tally.open());
        assertTrue(tally.cleared());
    }

    /** Nothing starred: cleared when it opens, not before. */
    @Test
    void nothingStarredClearsOnOpening() {
        RoomTally tally = planned(0);
        assertFalse(tally.cleared());
        assertTrue(tally.open());
        assertFalse(tally.open());
    }

    /** Not while some are still to spawn, even with every spawned one dead. */
    @Test
    void waitsForTheOnesStillToSpawn() {
        RoomTally tally = planned(2);
        tally.spawned();
        tally.open();
        assertFalse(tally.died());
        assertFalse(tally.cleared());
        tally.spawned();
        assertTrue(tally.died());
    }

    /** The last one to spawn failing, after the others were killed, clears it then. */
    @Test
    void aFailedSpawnAfterTheKillsClears() {
        RoomTally tally = planned(3);
        tally.spawned();
        tally.spawned();
        tally.open();
        assertFalse(tally.died());
        assertFalse(tally.died());
        assertTrue(tally.failed());
        assertTrue(tally.cleared());
        assertEquals(2, tally.planned());
    }

    /** All of them failing: cleared once it's open (then with no loot, the keys put in the room). */
    @Test
    void allFailing() {
        RoomTally tally = planned(2);
        assertFalse(tally.failed());
        assertFalse(tally.failed(), "not open yet");
        assertTrue(tally.open());

        RoomTally open = planned(1);
        open.open();
        assertTrue(open.failed());
    }

    /** A key room's key drops with its loot when it has starred mobs; else it's waiting in the room from the start. */
    @Test
    void keys() {
        assertTrue(RoomMobs.keyWithLoot(true, planned(4)));
        assertFalse(RoomMobs.keyWithLoot(true, planned(0)));
        assertFalse(RoomMobs.keyWithLoot(false, planned(0)), "a puzzle, say");
    }
}
