package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;

class DeathRulesTest {
    @Test
    void ghostsComeBackByThemselvesOnTheFirstFloors() {
        assertEquals(15, DeathRules.autoReviveSeconds(DungeonFloor.valueOf("ENTRANCE")));
        for (DungeonFloor floor : DungeonFloor.values()) {
            int seconds = DeathRules.autoReviveSeconds(floor);
            if (floor.isMasterMode() || floor.getNumber() > 2) assertEquals(-1, seconds, floor.name());
        }
    }

    @Test
    void failsWhenEveryoneHereIsAGhostOrAfterAnHour() {
        assertFalse(DeathRules.failed(3, 2, 0));
        assertTrue(DeathRules.failed(3, 3, 0));
        assertTrue(DeathRules.failed(1, 1, 1000));
        // Nobody here: the run manager closes an empty run by itself.
        assertFalse(DeathRules.failed(0, 0, 1000));
        assertFalse(DeathRules.failed(2, 0, 59 * 60 * 1000L));
        assertTrue(DeathRules.failed(2, 0, 60 * 60 * 1000L));
    }

    @Test
    void aFailedRunsSpeedIsTheRoomsCleared() {
        DungeonFloor entrance = DungeonFloor.valueOf("ENTRANCE");
        // 7 of 15: 46%, and 70% of that on the Entrance.
        assertEquals(32, DeathRules.failedSpeed(entrance, 7, 15));
        assertEquals(70, DeathRules.failedSpeed(entrance, 15, 15));
        assertEquals(0, DeathRules.failedSpeed(entrance, 0, 0));
        DungeonFloor first = floor(1);
        assertEquals(50, DeathRules.failedSpeed(first, 10, 20));
        assertEquals(100, DeathRules.failedSpeed(first, 25, 20));
    }

    @Test
    void aFailedRunLosesThirtyPercent() {
        // Speed 70 by the clock, 32 by the rooms.
        Score score = DeathRules.failedScore(new Score(59, 57, 70, 3), DungeonFloor.valueOf("ENTRANCE"), 7, 15);
        assertEquals(new Score(41, 40, 22, 2), score);
        assertEquals(105, score.total());
    }

    private static DungeonFloor floor(int number) {
        for (DungeonFloor floor : DungeonFloor.values()) if (!floor.isMasterMode() && floor.getNumber() == number) return floor;
        throw new IllegalArgumentException("no floor " + number);
    }
}
