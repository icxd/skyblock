package net.icxd.dungeons.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a grant of experience comes to with bonuses, which add up. */
class ExpOrbsTest {
    static {
        // Registered once for the whole run: +25% on everything and +200% on ores (an Experience Artifact's and Lapis Armor's, a test's).
        ExpOrbs.addBonus((player, source) -> 25);
        ExpOrbs.addBonus((player, source) -> source == ExpOrbs.Source.ORE ? 200 : 0);
    }

    @Test
    void amounts() {
        assertEquals(30, ExpOrbs.amount(30, 0));
        assertEquals(38, ExpOrbs.amount(30, 25));
        // Double experience is +100.
        assertEquals(60, ExpOrbs.amount(30, 100));
        assertEquals(0, ExpOrbs.amount(0, 100));
        assertEquals(0, ExpOrbs.amount(30, -200));
    }

    /** Bonuses add up, by source. */
    @Test
    void bonuses() {
        assertEquals(25, ExpOrbs.bonus(null, ExpOrbs.Source.MOB), 1e-9);
        assertEquals(225, ExpOrbs.bonus(null, ExpOrbs.Source.ORE), 1e-9);
        // 45 x 3.25 = 146.25.
        assertEquals(146, ExpOrbs.amount(45, ExpOrbs.bonus(null, ExpOrbs.Source.ORE)));
    }
}
