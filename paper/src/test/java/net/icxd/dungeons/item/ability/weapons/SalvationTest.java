package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** "Can be cast after landing 3 hits." */
class SalvationTest {
    @Test
    void threeHitsThenABeam() {
        UUID player = UUID.randomUUID();
        assertFalse(Salvation.charged(player));
        Salvation.landed(player);
        Salvation.landed(player);
        assertFalse(Salvation.charged(player));
        Salvation.landed(player);
        assertTrue(Salvation.charged(player));
        // More hits don't bank a second beam.
        Salvation.landed(player);
        Salvation.spend(player);
        assertFalse(Salvation.charged(player));
        Salvation.landed(player);
        Salvation.landed(player);
        Salvation.landed(player);
        assertTrue(Salvation.charged(player));
    }

    /** Each player's own. */
    @Test
    void perPlayer() {
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        for (int i = 0; i < 3; i++) Salvation.landed(one);
        assertTrue(Salvation.charged(one));
        assertFalse(Salvation.charged(other));
        Salvation.forget(one);
        assertFalse(Salvation.charged(one));
    }
}
