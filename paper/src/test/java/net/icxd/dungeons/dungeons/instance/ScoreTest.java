package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;

class ScoreTest {
    private static Score.Inputs inputs(int completed, int total, int secrets, int totalSecrets, int deaths, double seconds) {
        return new Score.Inputs(completed, total, secrets, totalSecrets, deaths, false, 0, 0, false, false, seconds);
    }

    /** An Entrance as recorded: 15 cells counted, 21 secrets. */
    private static Score.Inputs entrance(int cells, int secrets, int puzzlesNotDone, int crypts, double seconds) {
        return new Score.Inputs(cells, 15, secrets, 21, 0, false, puzzlesNotDone, crypts, false, false, seconds);
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
        // A full clear with the 30% of secrets the Entrance asks for: its best, 214 (B).
        Score score = Score.of(DungeonFloor.ENTRANCE, new Score.Inputs(8, 8, 3, 10, 0, false, 0, 5, false, false, 300));
        assertEquals(new Score(70, 70, 70, 4), score);
        assertEquals(214, score.total());
        assertEquals("B", score.grade());
        // With Paul, 221 (research score_rewards.md 1.4).
        assertEquals(221, Score.of(DungeonFloor.ENTRANCE, new Score.Inputs(8, 8, 3, 10, 0, false, 0, 5, false, true, 300)).total());
    }

    /** The four recorded numbers, each part as the score card showed it (research score_rewards.md 1.4). */
    @Test
    void recordedEntrances() {
        // R1: 13 of 15 cells in the chat (the Blood Room not counted yet), 14 on the card; 4 of 21
        // secrets, 4 crypts, Tic Tac Toe failed; 03m 55s.
        Score chat1 = Score.of(DungeonFloor.ENTRANCE, entrance(13, 4, 1, 4, 235));
        assertEquals(new Score(55, 54, 70, 3), chat1);
        assertEquals(182, chat1.total());
        assertEquals("B", chat1.grade());
        Score card1 = Score.of(DungeonFloor.ENTRANCE, entrance(14, 4, 1, 4, 235));
        assertEquals(new Score(59, 57, 70, 3), card1);
        assertEquals(189, card1.total());
        // R2: 6 then 7 cells, no secrets or crypts, both puzzles never found; 01m 49s.
        Score chat2 = Score.of(DungeonFloor.ENTRANCE, entrance(6, 0, 2, 0, 109));
        assertEquals(new Score(22, 17, 70, 0), chat2);
        assertEquals(109, chat2.total());
        assertEquals("C", chat2.grade());
        Score card2 = Score.of(DungeonFloor.ENTRANCE, entrance(7, 0, 2, 0, 109));
        assertEquals(new Score(26, 20, 70, 0), card2);
        assertEquals(116, card2.total());
    }

    @Test
    void penalties() {
        // Two deaths and an unfinished puzzle take 14 off skill; F1 allows 10 minutes, and this took 11.
        Score score = Score.of(DungeonFloor.FLOOR_1, new Score.Inputs(10, 10, 0, 20, 2, false, 1, 0, false, false, 10 * 60 + 60));
        assertEquals(86, score.skill());
        assertEquals(60, score.explore());
        assertEquals(95, score.speed());
        // The first death with a Spirit pet costs 1.
        assertEquals(87, Score.of(DungeonFloor.FLOOR_1, new Score.Inputs(10, 10, 0, 20, 2, true, 1, 0, false, false, 60)).skill());
        // Skill never goes under 20.
        assertEquals(20, Score.of(DungeonFloor.FLOOR_1, new Score.Inputs(1, 10, 0, 20, 5, false, 3, 0, false, false, 60)).skill());
    }

    /** The share of secrets by percentage, not "found out of the secrets needed" (which gives 22 here). */
    @Test
    void secretsByPercentage() {
        assertEquals(60 + 25, Score.of(DungeonFloor.FLOOR_1, inputs(10, 10, 4, 21, 0, 60)).explore());
        // Exactly the share asked for is all 40, with no rounding error.
        assertEquals(100, Score.of(DungeonFloor.FLOOR_1, inputs(10, 10, 3, 10, 0, 60)).explore());
        assertEquals(100, Score.of(DungeonFloor.FLOOR_7, inputs(10, 10, 50, 50, 0, 60)).explore());
        assertEquals(19.0476, entrance(13, 4, 1, 4, 235).secretPercent(), 1e-4);
    }

    @Test
    void speed() {
        assertEquals(100, Score.speed(DungeonFloor.ENTRANCE, 19 * 60 + 59));
        assertEquals(90, Score.speed(DungeonFloor.FLOOR_1, 12 * 60));
        assertEquals(80, Score.speed(DungeonFloor.FLOOR_1, 16 * 60));
        assertEquals(70, Score.speed(DungeonFloor.FLOOR_1, 21 * 60));
        assertEquals(0, Score.speed(DungeonFloor.FLOOR_1, 70 * 60));
        assertEquals(100, Score.speed(DungeonFloor.FLOOR_7, 13 * 60));
    }

    @Test
    void grades() {
        assertEquals("C", new Score(20, 30, 100, 0).grade());
        assertEquals("B", new Score(60, 60, 100, 0).grade());
        assertEquals("A", new Score(80, 80, 100, 0).grade());
        assertEquals("D", new Score(14, 0, 70, 0).grade());
        assertEquals("&e", Score.gradeColor("B"));
        assertEquals("&6", Score.gradeColor("C"));
        assertEquals("&c", Score.gradeColor("D"));
    }

    /** "Cleared: 87%": to the nearest, where 13 of 15 used to show 86. */
    @Test
    void cleared() {
        assertEquals(0, Score.cleared(0, 15));
        assertEquals(27, Score.cleared(4, 15));
        assertEquals(47, Score.cleared(7, 15));
        assertEquals(87, Score.cleared(13, 15));
        assertEquals(93, Score.cleared(14, 15));
        assertEquals(100, Score.cleared(15, 15));
        assertEquals(0, Score.cleared(3, 0));
        assertEquals("&c", Score.clearedColor(33));
        assertEquals("&6", Score.clearedColor(40));
        assertEquals("&6", Score.clearedColor(67));
        assertEquals("&a", Score.clearedColor(87));
    }

    /** Every recorded value of the sidebar's "(N)" (research score_rewards.md 1.6, srscore/check.py). */
    @Test
    void indicatorAsRecorded() {
        // {cells, puzzles not done, secrets of 21, crypts, Watcher's undead killed, shown}
        int[][] recorded = {
                // R1
                {4, 2, 0, 0, 0, 44}, {6, 2, 0, 0, 0, 76}, {6, 2, 1, 1, 0, 83}, {7, 2, 1, 1, 0, 99}, {8, 2, 1, 1, 0, 114},
                {9, 2, 1, 1, 0, 131}, {9, 2, 1, 2, 0, 132}, {9, 2, 2, 2, 0, 138}, {9, 2, 2, 3, 0, 139}, {9, 2, 2, 4, 0, 140},
                {9, 2, 3, 4, 0, 147}, {10, 1, 3, 4, 0, 173}, {13, 1, 3, 4, 0, 221}, {13, 1, 4, 4, 0, 227}, {13, 1, 4, 4, 9, 246},
                // R2
                {3, 2, 0, 0, 0, 32}, {4, 2, 0, 0, 0, 44}, {6, 2, 0, 0, 0, 76}, {6, 2, 0, 0, 3, 82}, {6, 2, 0, 0, 4, 84},
                {6, 2, 0, 0, 8, 93}};
        for (int[] r : recorded) {
            Score.Inputs in = entrance(r[0], r[2], r[1], r[3], 60);
            assertEquals(r[5], Score.indicator(DungeonFloor.ENTRANCE, in, r[4]), () -> java.util.Arrays.toString(r));
        }
        assertEquals(0, Score.indicator(DungeonFloor.ENTRANCE, entrance(0, 0, 2, 0, 0), 0));
    }
}
