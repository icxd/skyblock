package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.DungeonRecords;

/** The end-of-run summary and what it saves (research score_rewards.md 2.1, 4). */
class RunEndTest {
    private static final LocalDate DAY = LocalDate.of(2026, 9, 26);

    /** R1's summary (04:28.4), line for line and space for space, with its rewards put in. */
    @Test
    void recordedSummary() {
        Score chat = new Score(55, 54, 70, 3);
        RunRewards.Reward reward = new RunRewards.Reward(131.1, Map.of(DungeonClass.BERSERK, 126.2), 7);
        RunEnd.Outcome outcome = new RunEnd.Outcome(reward, new DungeonRecords.Completion(3, 1, false, false));
        List<String> lines = RunEnd.summary(DungeonFloor.ENTRANCE, chat, 235_000, outcome);
        assertEquals(List.of(
                "&a&l" + "▬".repeat(64),
                "&f                        &cThe Catacombs &8- &eEntrance",
                "",
                "&f                            Team Score: &a182 &f(&eB&f)",
                "&f                  &c☠ &eDefeated &cThe Watcher &ein &a03m 55s",
                "&f                             &6> &e&lEXTRA STATS &6<",
                "&f                                    &8+&b7 Bits",
                "&f                      &8+&3131.1 Catacombs Experience",
                "&f                       &8+&3126.2 Berserk Experience",
                "&a&l" + "▬".repeat(64)), lines);
        assertEquals(RunEnd.EXTRA_STATS, lines.get(5));
    }

    /** R2's (03:42.1): the C, and the shorter numbers one space further in. */
    @Test
    void recordedSummaryTwo() {
        RunRewards.Reward reward = new RunRewards.Reward(30.8, Map.of(DungeonClass.BERSERK, 28.5), 7);
        List<String> lines = RunEnd.summary(DungeonFloor.ENTRANCE, new Score(22, 17, 70, 0), 109_000,
                new RunEnd.Outcome(reward, new DungeonRecords.Completion(6, 3, false, false)));
        assertEquals("&f                            Team Score: &a109 &f(&6C&f)", lines.get(3));
        assertEquals("&f                  &c☠ &eDefeated &cThe Watcher &ein &a01m 49s", lines.get(4));
        assertEquals("&f                       &8+&330.8 Catacombs Experience", lines.get(7));
        assertEquals("&f                        &8+&328.5 Berserk Experience", lines.get(8));
    }

    @Test
    void recordsAndNoData() {
        RunRewards.Reward reward = new RunRewards.Reward(33, Map.of(DungeonClass.HEALER, 29.6), 3);
        List<String> lines = RunEnd.summary(DungeonFloor.ENTRANCE, new Score(22, 17, 59, 0), 1_942_000,
                new RunEnd.Outcome(reward, new DungeonRecords.Completion(0, 0, true, true)));
        assertEquals(true, lines.get(3).endsWith("Team Score: &a98 &f(&cD&f)" + RunEnd.NEW_RECORD));
        assertEquals(true, lines.get(4).endsWith("&a32m 22s" + RunEnd.NEW_RECORD));
        assertEquals(true, lines.get(7).endsWith(" &8+&333 Catacombs Experience"));
        // Nobody's data: the summary without rewards.
        assertEquals(7, RunEnd.summary(DungeonFloor.ENTRANCE, new Score(22, 17, 59, 0), 1_942_000, null).size());
    }

    /** What finishing saves on the profile they play on. */
    @Test
    void award() {
        Document profile = new Document("bits", 10);
        RunEnd.Outcome outcome = RunEnd.award(profile, DungeonFloor.ENTRANCE, new Score(55, 54, 70, 3), 235_000, 19.05,
                DungeonClass.BERSERK, List.of(DungeonClass.MAGE), DAY);
        assertEquals(127.3, DungeonProfile.catacombsXp(profile));
        assertEquals(136.1, DungeonProfile.classXp(profile, DungeonClass.BERSERK));
        assertEquals(34.0, DungeonProfile.classXp(profile, DungeonClass.MAGE));
        assertEquals(0, DungeonProfile.classXp(profile, DungeonClass.TANK));
        assertEquals(13, profile.get("bits"));
        assertEquals(1, DungeonRecords.completions(profile, DungeonFloor.ENTRANCE));
        assertEquals(182, DungeonRecords.bestScore(profile, DungeonFloor.ENTRANCE));
        assertEquals(new DungeonRecords.Completion(0, 0, true, true), outcome.completion());

        // The next one counts one earlier completion and run today.
        RunEnd.Outcome second = RunEnd.award(profile, DungeonFloor.ENTRANCE, new Score(55, 54, 70, 3), 235_000, 19.05,
                DungeonClass.BERSERK, List.of(), DAY);
        assertEquals(new DungeonRecords.Completion(1, 1, false, false), second.completion());
        // The frequent flier factor counted one more completion: 1.05 instead of 1.029.
        assertEquals(127.3 + 129.9, DungeonProfile.catacombsXp(profile), 1e-9);
        assertEquals(16, profile.get("bits"));
    }
}
