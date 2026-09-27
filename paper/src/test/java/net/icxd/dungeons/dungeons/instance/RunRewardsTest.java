package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;

/** Experience and Bits for finishing a floor (research score_rewards.md 4; the constants are unverified). */
class RunRewardsTest {
    @Test
    void baseAndBits() {
        assertEquals(55, RunRewards.baseXp(DungeonFloor.ENTRANCE));
        assertEquals(110, RunRewards.baseXp(DungeonFloor.FLOOR_1));
        assertEquals(28_000, RunRewards.baseXp(DungeonFloor.FLOOR_7));
        assertEquals(300_000, RunRewards.baseXp(DungeonFloor.MASTER_FLOOR_7));
        assertEquals(3, RunRewards.bits(DungeonFloor.ENTRANCE));
        assertEquals(25, RunRewards.bits(DungeonFloor.FLOOR_7));
        assertEquals(10, RunRewards.bits(DungeonFloor.MASTER_FLOOR_1));
        assertEquals(100, RunRewards.bits(DungeonFloor.MASTER_FLOOR_7));
    }

    /** Linear in the score, and 300 more of it for the first five completions of a floor. */
    @Test
    void scoreMultiplier() {
        assertEquals(482 / 300.0, RunRewards.scoreMultiplier(182, 0), 1e-12);
        assertEquals(482 / 300.0, RunRewards.scoreMultiplier(182, 4), 1e-12);
        assertEquals(109 / 300.0, RunRewards.scoreMultiplier(109, 5), 1e-12);
        // Class experience scales the same way; the recorded R1 and R2 fit it (research 4.3).
        assertEquals(RunRewards.classXp(DungeonFloor.ENTRANCE, 109, 5) * 482 / 109,
                RunRewards.classXp(DungeonFloor.ENTRANCE, 182, 3), 1e-9);
    }

    @Test
    void entranceRewards() {
        // A first Entrance of the day with the recorded R1 chat score, solo Berserk.
        RunRewards.Reward first = RunRewards.reward(DungeonFloor.ENTRANCE, 182, 0, 0, 19.05, DungeonClass.BERSERK, List.of());
        assertEquals(127.3, first.catacombs());
        assertEquals(Map.of(DungeonClass.BERSERK, 136.1), first.classes());
        assertEquals(3, first.bits());
        // Four completions earlier: the frequent flier factor has grown, class experience hasn't.
        assertEquals(135.1, RunRewards.reward(DungeonFloor.ENTRANCE, 182, 3, 0, 0, DungeonClass.BERSERK, List.of()).catacombs());
        // R2's score after five completions: no +300; with and without the daily 40%.
        RunRewards.Reward daily = RunRewards.reward(DungeonFloor.ENTRANCE, 109, 5, 4, 0, DungeonClass.BERSERK, List.of());
        assertEquals(31.7, daily.catacombs());
        assertEquals(30.8, daily.classes().get(DungeonClass.BERSERK));
        RunRewards.Reward later = RunRewards.reward(DungeonFloor.ENTRANCE, 109, 5, 5, 0, DungeonClass.BERSERK, List.of());
        assertEquals(22.7, later.catacombs());
        assertEquals(22.0, later.classes().get(DungeonClass.BERSERK));
    }

    /** The frequent flier factor stops growing at 76 completions below Floor VI. */
    @Test
    void frequentFlierCap() {
        double capped = RunRewards.catacombsXp(DungeonFloor.ENTRANCE, 300, 76, 0);
        assertEquals(55 * 2.625, capped, 1e-9);
        assertEquals(capped, RunRewards.catacombsXp(DungeonFloor.ENTRANCE, 300, 500, 0), 1e-9);
    }

    /** Half a percent more Catacombs experience for every percent of secrets over the requirement, up to Floor VI. */
    @Test
    void overExploration() {
        double base = RunRewards.catacombsXp(DungeonFloor.ENTRANCE, 200, 10, 30);
        assertEquals(base * 1.1, RunRewards.catacombsXp(DungeonFloor.ENTRANCE, 200, 10, 50), 1e-9);
        assertEquals(base, RunRewards.catacombsXp(DungeonFloor.ENTRANCE, 200, 10, 10), 1e-9);
        double f7 = RunRewards.catacombsXp(DungeonFloor.FLOOR_7, 300, 10, 100);
        assertEquals(f7, RunRewards.catacombsXp(DungeonFloor.FLOOR_7, 300, 10, 50), 1e-9);
    }

    /** The others' classes get a quarter, once each; the member's own class comes first. */
    @Test
    void teamBonus() {
        RunRewards.Reward reward = RunRewards.reward(DungeonFloor.ENTRANCE, 300, 10, 10, 0, DungeonClass.BERSERK,
                List.of(DungeonClass.MAGE, DungeonClass.BERSERK, DungeonClass.HEALER, DungeonClass.MAGE));
        assertEquals(List.of(DungeonClass.BERSERK, DungeonClass.HEALER, DungeonClass.MAGE), List.copyOf(reward.classes().keySet()));
        assertEquals(60.5, reward.classes().get(DungeonClass.BERSERK));
        assertEquals(15.1, reward.classes().get(DungeonClass.HEALER));
        assertEquals(15.1, reward.classes().get(DungeonClass.MAGE));
    }

    @Test
    void format() {
        assertEquals("131.1", RunRewards.format(131.1));
        assertEquals("30.8", RunRewards.format(30.8));
        assertEquals("33", RunRewards.format(33));
        assertEquals("33", RunRewards.format(32.96));
        assertEquals("1,001,246.4", RunRewards.format(1_001_246.4));
        assertEquals("846,720", RunRewards.format(846_720));
    }
}
