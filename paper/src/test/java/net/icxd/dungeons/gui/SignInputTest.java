package net.icxd.dungeons.gui;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Where the search sign goes, and where there's nowhere the client would let them edit it. */
class SignInputTest {
    private static final int MIN = -64;
    private static final int MAX = 320;
    private static final double SURVIVAL = 4.5;
    private static final double CREATIVE = 5;

    @Test
    void justAboveTheHead() {
        // Standing on y=64: eyes at 65.62, in block 65.
        assertEquals(OptionalInt.of(66), SignInput.signY(65.62, MIN, MAX, SURVIVAL));
        assertEquals(OptionalInt.of(-63), SignInput.signY(-63.5, MIN, MAX, SURVIVAL));
        // At the top of the world it's the eye's own block.
        assertEquals(OptionalInt.of(319), SignInput.signY(319.5, MIN, MAX, SURVIVAL));
    }

    @Test
    void notOutOfReachAboveTheWorld() {
        // Flying at y=335: the nearest block is 319, about 16 below the eyes.
        assertEquals(OptionalInt.empty(), SignInput.signY(336.62, MIN, MAX, SURVIVAL));
        assertEquals(OptionalInt.empty(), SignInput.signY(336.62, MIN, MAX, CREATIVE));
        assertEquals(OptionalInt.empty(), SignInput.signY(1e12, MIN, MAX, CREATIVE));
        // A little above it is still in reach: up to reach + 4, less a block to spare.
        assertEquals(OptionalInt.of(319), SignInput.signY(320 + 7.4, MIN, MAX, SURVIVAL));
        assertEquals(OptionalInt.empty(), SignInput.signY(320 + 7.6, MIN, MAX, SURVIVAL));
        assertEquals(OptionalInt.of(319), SignInput.signY(320 + 7.9, MIN, MAX, CREATIVE));
        assertEquals(OptionalInt.empty(), SignInput.signY(320 + 8.1, MIN, MAX, CREATIVE));
    }

    @Test
    void notOutOfReachBelowTheWorld() {
        assertEquals(OptionalInt.of(-64), SignInput.signY(-70.5, MIN, MAX, SURVIVAL));
        assertEquals(OptionalInt.empty(), SignInput.signY(-80, MIN, MAX, SURVIVAL));
        assertEquals(OptionalInt.empty(), SignInput.signY(-1e12, MIN, MAX, SURVIVAL));
    }
}
