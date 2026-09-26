package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RunTextTest {
    /** The end of a recorded Entrance run: each line and the spaces Hypixel put in front of it. */
    @Test
    void centresLikeHypixel() {
        assertEquals(24, spaces("&cThe Catacombs &8- &eEntrance"));
        assertEquals(28, spaces("Team Score: &a109 &f(&6C&f)"));
        assertEquals(18, spaces("&c☠ &eDefeated &cThe Watcher &ein &a01m 49s"));
        assertEquals(29, spaces("&6> &e&lEXTRA STATS &6<"));
        assertEquals(36, spaces("&8+&b7 Bits"));
        assertEquals(23, spaces("&8+&330.8 Catacombs Experience"));
        assertEquals(24, spaces("&8+&328.5 Berserk Experience"));
    }

    private static int spaces(String line) {
        String centered = RunText.centered(line);
        return centered.length() - 2 - line.length();
    }

    @Test
    void times() {
        assertEquals("01s", RunText.elapsed(1_400));
        assertEquals("01s", RunText.elapsed(999));
        assertEquals("45s", RunText.elapsed(45_000));
        assertEquals("01m 49s", RunText.elapsed(109_000));
        assertEquals("01h 00m 05s", RunText.elapsed(3_605_000));
        assertEquals("1:57", RunText.clock(116_200));
        assertEquals("0:04", RunText.clock(4_000));
        assertEquals("0:00", RunText.clock(-5));
    }

    @Test
    void compactNumbers() {
        assertEquals("0", RunText.compact(0));
        assertEquals("223", RunText.compact(223.6));
        assertEquals("852.7k", RunText.compact(852_790));
        assertEquals("1M", RunText.compact(1_000_000));
        assertEquals("1.3M", RunText.compact(1_345_000));
    }
}
