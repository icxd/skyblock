package net.icxd.dungeons.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The Experience enchantment's double experience: +100 when its chance comes up. */
class ExpBonusesTest {
    @Test
    void doubled() {
        assertEquals(100, ExpBonuses.doubled(12.5, 0.1249), 1e-9);
        assertEquals(0, ExpBonuses.doubled(12.5, 0.125), 1e-9);
        assertEquals(100, ExpBonuses.doubled(62.5, 0.6), 1e-9);
        assertEquals(0, ExpBonuses.doubled(0, 0), 1e-9);
    }
}
