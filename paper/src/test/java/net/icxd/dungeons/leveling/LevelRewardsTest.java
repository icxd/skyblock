package net.icxd.dungeons.leveling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** What each SkyBlock level gives: its stats (as the recorded Stat Rewards item says) and its other rewards. */
class LevelRewardsTest {
    @Test
    void stats() {
        assertEquals(new Stats(), LevelRewards.stats(0));
        Stats one = LevelRewards.stats(1);
        assertEquals(5, one.get(Stat.HEALTH));
        assertEquals(0, one.get(Stat.STRENGTH));
        // Level 88: +440 Health (the recorded Health breakdown's SkyBlock Level line) and +17 Strength.
        Stats banana = LevelRewards.stats(88);
        assertEquals(440, banana.get(Stat.HEALTH));
        assertEquals(17, banana.get(Stat.STRENGTH));
        assertEquals(18, LevelRewards.stats(90).get(Stat.STRENGTH));
        // Added to someone's stats, the same.
        Stats stats = new Stats();
        LevelRewards.add(stats, 88);
        assertEquals(banana, stats);
        LevelRewards.add(stats, 0);
        assertEquals(banana, stats);
    }

    /** Level 90's lines as the recorded SkyBlock Leveling menu lists them: its emblem, then Strength and Health. */
    @Test
    void lines() {
        LevelingData data = Fixtures.fixture();
        assertEquals(List.of(), LevelRewards.lines(data, 0));
        assertEquals(List.of("&8+&a5 &c❤ Health"), LevelRewards.lines(data, 1));
        assertEquals(List.of("&bAccess to Something", "&8+&a5 &c❤ Health"), LevelRewards.lines(data, 2));
        assertEquals(List.of("&fDot Emblem &7•", "&9Some Bonus", "&8+&a5 &c❤ Health"), LevelRewards.lines(data, 3));
        assertEquals(List.of("&8+&a1 &c❁ Strength", "&8+&a5 &c❤ Health"), LevelRewards.lines(data, 5));
        assertEquals("&7Reward:", LevelRewards.heading(1));
        assertEquals("&7Rewards:", LevelRewards.heading(0));
        assertEquals("&7Rewards:", LevelRewards.heading(3));
    }

    @Test
    void milestones() {
        LevelingData data = Fixtures.fixture();
        assertFalse(LevelRewards.milestone(data, 1));
        assertTrue(LevelRewards.milestone(data, 2));
        assertEquals(2, LevelRewards.nextMilestone(data, 0));
        assertEquals(3, LevelRewards.nextMilestone(data, 2));
        assertEquals(40, LevelRewards.nextMilestone(data, 3));
        assertEquals(-1, LevelRewards.nextMilestone(data, 40));
    }

    /** A jump of levels is one message with all they gave ("Level 287 ➡ [289]" gave "+10 ❤ Health"). */
    @Test
    void gained() {
        LevelingData data = Fixtures.fixture();
        assertEquals(List.of("&8+&a10 &c❤ Health"), LevelRewards.gained(data, 287, 289));
        assertEquals(List.of("&bAccess to Something", "&fDot Emblem &7•", "&9Some Bonus", "&8+&a1 &c❁ Strength", "&8+&a25 &c❤ Health"),
                LevelRewards.gained(data, 0, 5));
    }

    /** The recorded levels: 90 has the Boxes Emblem, and the next milestone after 88 is 90. */
    @Test
    void recordedLevels() {
        LevelingData data = Fixtures.recorded();
        assertEquals(List.of("&fBoxes Emblem &7⧉", "&8+&a1 &c❁ Strength", "&8+&a5 &c❤ Health"), LevelRewards.lines(data, 90));
        assertEquals(90, LevelRewards.nextMilestone(data, 88));
        assertEquals(3, LevelRewards.nextMilestone(data, 0));
        assertEquals(List.of("&bAccess to Community Shop", "&8+&a5 &c❤ Health"), LevelRewards.lines(data, 3));
    }
}
