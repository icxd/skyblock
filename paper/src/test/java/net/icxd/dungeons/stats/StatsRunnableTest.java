package net.icxd.dungeons.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsRunnableTest {
    /** The recorded action bar: "§c5,238/5,238", "§a2,446§a Defense", "§b1,201/1,201 Mana"; health's fractions dropped. */
    @Test
    void actionBarNumbers() {
        assertEquals("5,238/5,238", StatsRunnable.ofMax(5238.67, 5238.67));
        assertEquals("3,944/5,238", StatsRunnable.ofMax(3944.9, 5238.67));
        assertEquals("2,446", StatsRunnable.number(2446.4));
        assertEquals("940/940", StatsRunnable.ofMax(940, 940));
        assertEquals("104/104", StatsRunnable.ofMax(104, 104));
    }

    /** Vitality after mana: the numbers and its symbol, no word (as Skyblocker reads it), in its own colour. */
    @Test
    void vitality() {
        assertEquals("&4104/104♨", StatsRunnable.vitality(104, 104));
        assertEquals("&479/104♨", StatsRunnable.vitality(79.2, 104));
    }

    /** Health in red; with absorption, health and absorption together over the max, in gold ("§66,171/4,422❤"). */
    @Test
    void health() {
        assertEquals("&c5,238/5,238❤", StatsRunnable.health(5238.67, 0, 5238.67));
        assertEquals("&66,171/4,422❤", StatsRunnable.health(4422, 1749.5, 4422));
        assertEquals("&61,500/4,422❤", StatsRunnable.health(900, 600, 4422));
    }

    /** "Regen mana 10x slower": a tenth of a second's regeneration, rounded up as the base is. */
    @Test
    void manaRegenSlowed() {
        assertEquals(20, StatsRunnable.slowed(20, 1));
        assertEquals(2, StatsRunnable.slowed(20, 0.1));
        assertEquals(3, StatsRunnable.slowed(21, 0.1));
        assertEquals(0, StatsRunnable.slowed(20, 0));
    }
}
