package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** An ability's chat line, as recorded. */
class TallyTest {
    /** The recorded Giant's Slam: its three gray numbers, and the line that sums them. */
    @Test
    void giantsSlam() {
        Tally tally = new Tally();
        tally.add(2_097_307);
        tally.add(2_025_717);
        tally.add(3_980_779);
        assertEquals("&7Your Giant's Sword hit &c3 &7enemies for &c8,103,803 &7damage.", tally.message("Giant's Sword"));
    }

    /** One enemy, and a total that isn't whole: one decimal (the recorded Throwing Axe's). */
    @Test
    void oneEnemy() {
        assertEquals("&7Your Throwing Axe hit &c1 &7enemy for &c14,689,667.3 &7damage.", Tally.message("Throwing Axe", 1, 14_689_667.3));
    }

    @Test
    void nothingHit() {
        assertNull(new Tally().message("Implosion"));
    }
}
