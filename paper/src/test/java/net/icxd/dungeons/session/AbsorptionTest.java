package net.icxd.dungeons.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Absorption: health on top of health, source by source, taken before health and gone when it runs out. */
class AbsorptionTest {
    @Test
    void takesAHitFirst() {
        Absorption absorption = new Absorption();
        absorption.give("Golem", 60, 20_000);
        assertEquals(60, absorption.total(0), 1e-9);
        // 100 through their Defense: 60 of it absorbed, 40 left for their health.
        assertEquals(40, absorption.absorb(100, 1_000), 1e-9);
        assertEquals(0, absorption.total(1_000), 1e-9);
        assertEquals(100, absorption.absorb(100, 1_000), 1e-9);
    }

    /** Sources add up; a hit takes from the one that runs out soonest first. */
    @Test
    void sources() {
        Absorption absorption = new Absorption();
        absorption.give("Wither Shield", 1_400, 5_000);
        absorption.give("Potion", 300, 180_000);
        assertEquals(1_700, absorption.total(0), 1e-9);
        assertEquals(0, absorption.absorb(1_000, 100), 1e-9);
        assertEquals(400, absorption.of("Wither Shield", 100), 1e-9);
        assertEquals(300, absorption.of("Potion", 100), 1e-9);
        assertEquals(0, absorption.absorb(500, 200), 1e-9);
        assertEquals(0, absorption.of("Wither Shield", 200), 1e-9);
        assertEquals(200, absorption.of("Potion", 200), 1e-9);
    }

    /** It runs out with its time; the same source again replaces what it gave; none of nothing. */
    @Test
    void timeAndReplacing() {
        Absorption absorption = new Absorption();
        absorption.give("Golem", 60, 20_000);
        assertEquals(0, absorption.total(20_000), 1e-9);
        assertEquals(50, absorption.absorb(50, 20_000), 1e-9);
        absorption.give("Golem", 60, 40_000);
        absorption.absorb(30, 21_000);
        absorption.give("Golem", 60, 60_000);
        assertEquals(60, absorption.of("Golem", 21_000), 1e-9);
        absorption.give("Golem", 0, 60_000);
        assertEquals(0, absorption.total(21_000), 1e-9);
        absorption.give("Golem", 60, 60_000);
        absorption.clear();
        assertEquals(0, absorption.total(0), 1e-9);
        assertEquals(0, absorption.absorb(-5, 0), 1e-9);
    }
}
