package net.icxd.dungeons.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsTest {
    @Test
    void baseIsHypixels() {
        Stats base = Stats.base();
        assertEquals(100, base.get(Stat.HEALTH));
        // Base intelligence is 0 (a bare player's mana pool is 100), swing range 3.
        assertEquals(0, base.get(Stat.INTELLIGENCE));
        assertEquals(3, base.get(Stat.SWING_RANGE));
        assertEquals(100, base.get(Stat.SPEED));
        assertEquals(30, base.get(Stat.CRIT_CHANCE));
        assertEquals(50, base.get(Stat.CRIT_DAMAGE));
        assertEquals(0, base.get(Stat.STRENGTH));
        assertEquals(30, base.get(Stat.RESPIRATION));
    }

    @Test
    void addsPerStat() {
        Stats a = new Stats().set(Stat.STRENGTH, 100).set(Stat.DAMAGE, 120);
        Stats b = new Stats().set(Stat.STRENGTH, 50).add(Stat.STRENGTH, 5);
        a.add(b);
        assertEquals(155, a.get(Stat.STRENGTH));
        assertEquals(120, a.get(Stat.DAMAGE));
        assertTrue(a.has(Stat.DAMAGE));
        assertFalse(a.has(Stat.HEALTH));
    }

    @Test
    void copiesAreSeparate() {
        Stats a = new Stats().set(Stat.DEFENSE, 10);
        Stats copy = a.copy();
        assertEquals(a, copy);
        copy.add(Stat.DEFENSE, 1);
        assertNotEquals(a, copy);
        assertEquals(10, a.get(Stat.DEFENSE));
    }
}
