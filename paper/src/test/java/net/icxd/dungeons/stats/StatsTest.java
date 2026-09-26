package net.icxd.dungeons.stats;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.reforge.Reforge;
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
        assertEquals(100, base.get(Stat.INTELLIGENCE));
        assertEquals(100, base.get(Stat.SPEED));
        assertEquals(30, base.get(Stat.CRIT_CHANCE));
        assertEquals(50, base.get(Stat.CRIT_DAMAGE));
        assertEquals(0, base.get(Stat.STRENGTH));
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

    /** Heroic on the recorded epic Aspect of the Void: (+32) strength, (+80) intelligence, (+3%) attack speed. */
    @Test
    void reforgeAtRarity() {
        Stats heroic = Reforge.HEROIC.getStats().at(Rarity.EPIC);
        assertEquals(32, heroic.get(Stat.STRENGTH));
        assertEquals(80, heroic.get(Stat.INTELLIGENCE));
        assertEquals(3, heroic.get(Stat.ATTACK_SPEED));
        assertEquals(0, Reforge.HEROIC.getStats().at(Rarity.DIVINE).get(Stat.STRENGTH));
    }
}
