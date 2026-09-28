package net.icxd.dungeons.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A percent buff on a stat ("+12.5% Defense for 10s"), on what the rest makes it; several on one stat add up. */
class PercentBuffTest {
    @Test
    void percent() {
        assertEquals(1125, PlayerSession.percentBuffed(1000, 12.5), 1e-9);
        assertEquals(1250, PlayerSession.percentBuffed(1000, 12.5 + 12.5), 1e-9);
        assertEquals(500, PlayerSession.percentBuffed(1000, -50), 1e-9);
        assertEquals(0, PlayerSession.percentBuffed(1000, -150), 1e-9);
    }
}
