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
    void aFailedRunLosesThirtyPercent() {
        Score score = DeathRules.failedScore(new Score(59, 57, 70, 3));
        assertEquals(new Score(41, 40, 49, 2), score);
        assertEquals(132, score.total());
    }
}
