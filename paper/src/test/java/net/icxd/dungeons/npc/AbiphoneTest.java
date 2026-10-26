package net.icxd.dungeons.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Kat's call as recorded (2026-10-25, 02:05.4), less the [PICK UP] after each ring. */
class AbiphoneTest {
    @Test
    void itsLines() {
        assertEquals("&e✆ &bKat&e ✆ ", Abiphone.header("&bKat"));
        // "&a✆ RING...  &2&l[PICK UP]": the line's own space before [PICK UP] makes the two.
        assertEquals("&a✆ RING... ", Abiphone.ringing(1));
        assertEquals("&a✆ RING... RING... RING... ", Abiphone.ringing(3));
    }
}
