package net.icxd.dungeons.item.behaviour;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

class TerminatorTest {
    /** "Divides your ☣ Crit Chance by 4!": the base 30 and what gear adds, together. */
    @Test
    void dividesCritChanceByFour() {
        Stats stats = new Stats().set(Stat.CRIT_CHANCE, 30 + 50).set(Stat.CRIT_DAMAGE, 300);
        new Terminator().whileHeld(stats);
        assertEquals(20, stats.get(Stat.CRIT_CHANCE));
        assertEquals(300, stats.get(Stat.CRIT_DAMAGE));
    }
}
