package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;

class ScoreTest {
    private static Score.Inputs inputs(int completed, int total, int secrets, int totalSecrets, int deaths, double seconds) {
        return new Score.Inputs(completed, total, secrets, totalSecrets, deaths, false, 0, 0, false, false, seconds);
    }

    @Test
    void nothingDoneYet() {
        Score score = Score.of(DungeonFloor.FLOOR_7, inputs(0, 30, 0, 50, 0, 60));
        assertEquals(new Score(20, 0, 100, 0), score);
        assertEquals("C", score.grade());
    }

    @Test
    void everythingDoneInTime() {
        Score score = Score.of(DungeonFloor.FLOOR_7, new Score.Inputs(30, 30, 50, 50, 0, false, 0, 5, true, false, 400));
        assertEquals(new Score(100, 100, 100, 7), score);
        assertEquals("S+", score.grade());
    }

    @Test
    void entranceCountsSeventyPercent() {
        // A full clear with the 30% of secrets the Entrance asks for.
        Score score = Score.of(DungeonFloor.ENTRANCE, new Score.Inputs(8, 8, 3, 10, 0, false, 0, 5, false, false, 300));
        assertEquals(new Score(70, 70, 70, 4), score);
        assertEquals("B", score.grade());
    }

    @Test
    void penalties() {
        // Two deaths and an unfinished puzzle take 14 off skill; F1 allows 10 minutes, and this took 11.
        Score score = Score.of(DungeonFloor.FLOOR_1, new Score.Inputs(10, 10, 0, 20, 2, false, 1, 0, false, false, 10 * 60 + 60));
        assertEquals(86, score.skill());
        assertEquals(60, score.explore());
        assertEquals(95, score.speed());
    }

    @Test
    void grades() {
        assertEquals("C", new Score(20, 30, 100, 0).grade());
        assertEquals("B", new Score(60, 60, 100, 0).grade());
        assertEquals("A", new Score(80, 80, 100, 0).grade());
        assertEquals("D", new Score(14, 0, 70, 0).grade());
    }
}
