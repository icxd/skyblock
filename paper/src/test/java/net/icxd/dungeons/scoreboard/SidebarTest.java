package net.icxd.dungeons.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.economy.Coins;

class SidebarTest {
    @Test
    void thePurseAsRecorded() {
        // A new profile (research coins.md 1.1).
        assertEquals("&fPurse: &60.0", ScoreboardRunnable.purseLine(0, null));
        // The profile menu said 79,208,878.1 at the same time.
        assertEquals("&fPurse: &679,208,878", ScoreboardRunnable.purseLine(79_208_878.1, null));
        assertEquals("&fPurse: &657,690,425", ScoreboardRunnable.purseLine(57_690_425, null));
        assertEquals("&fPurse: &6423,085,776 &e(+5)", ScoreboardRunnable.purseLine(423_085_776, 5.0));
    }

    @Test
    void wholeNumbersOnTheSidebarAndTenthsInMenus() {
        assertEquals("1", Coins.sidebar(1.9), "never more than they have");
        assertEquals("1,000", Coins.sidebar(1_000));
        assertEquals("0.3", Coins.sidebar(0.3));
        assertEquals("0.0", Coins.sidebar(0.05));
        assertEquals("79,208,878.1", Coins.format(79_208_878.1));
        assertEquals("557,306", Coins.format(557_306));
        assertEquals("0", Coins.format(0));
        assertEquals("19.2", Coins.format(19.2));
        assertEquals("+5", Coins.signed(5));
        assertEquals("-1,200", Coins.signed(-1_200));
        assertEquals("+0.3", Coins.signed(0.3));
    }

    @Test
    void bits() {
        assertEquals("&fBits: &b14,321", ScoreboardRunnable.bitsLine(14_321, null));
        assertEquals("&fBits: &b2,614 &3(+545)", ScoreboardRunnable.bitsLine(2_614, 545.0));
    }

    /** As recorded for bits: 2,069, then 2,614 (+545) for three updates, then 2,614. */
    @Test
    void aChangeShowsForThreeUpdates() {
        SidebarChange change = new SidebarChange();
        assertNull(change.update(2_069), "nothing to compare with yet");
        assertNull(change.update(2_069));
        assertEquals(545, change.update(2_614));
        assertEquals(545, change.update(2_614));
        assertEquals(545, change.update(2_614));
        assertNull(change.update(2_614));
        assertNull(change.update(2_614));
    }

    @Test
    void changesWhileOneShowsAddUp() {
        SidebarChange change = new SidebarChange();
        change.update(100);
        assertEquals(20, change.update(120));
        assertEquals(20, change.update(120));
        // Another kill: +20 more, and three updates from now.
        assertEquals(40, change.update(140));
        assertEquals(40, change.update(140));
        assertEquals(40, change.update(140));
        assertNull(change.update(140));
        // A new change after it's gone starts from nothing.
        assertEquals(-40, change.update(100));
    }
}
