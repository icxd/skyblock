package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** The sidebar's "(N)": every 10 seconds from the start, 0 before the first (research score_rewards.md 1.6). */
class SidebarScoreTest {
    @Test
    void everyTenSeconds() {
        SidebarScore sidebar = new SidebarScore();
        AtomicInteger score = new AtomicInteger(44);
        // Before the start, nothing.
        sidebar.tick(5_000, score::get);
        assertEquals(0, sidebar.shown());
        sidebar.start(1_000);
        sidebar.tick(10_999, score::get);
        assertEquals(0, sidebar.shown());
        sidebar.tick(11_000, score::get);
        assertEquals(44, sidebar.shown());
        // A change shows at the next update, not before.
        score.set(76);
        sidebar.tick(20_500, score::get);
        assertEquals(44, sidebar.shown());
        sidebar.tick(21_300, score::get);
        assertEquals(76, sidebar.shown());
        // A late tick doesn't shift the cadence: the next is still due at 31,000.
        score.set(83);
        sidebar.tick(30_999, score::get);
        assertEquals(76, sidebar.shown());
        sidebar.tick(31_000, score::get);
        assertEquals(83, sidebar.shown());
        // Missed updates are skipped, not caught up one by one.
        score.set(99);
        sidebar.tick(75_000, score::get);
        score.set(100);
        sidebar.tick(76_000, score::get);
        assertEquals(99, sidebar.shown());
        sidebar.tick(81_000, score::get);
        assertEquals(100, sidebar.shown());
    }
}
