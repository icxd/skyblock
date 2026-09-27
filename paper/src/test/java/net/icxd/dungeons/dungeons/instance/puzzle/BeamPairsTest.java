package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.instance.puzzle.BeamPairs.Pick;

/** Creeper Beams' picks, with a made-up room: the creeper stands in the middle of block (10, 50, 10). */
class BeamPairsTest {
    private static final double[] FEET = {10.5, 50, 10.5};
    private static final UUID STEVE = UUID.randomUUID();
    private static final UUID ALEX = UUID.randomUUID();

    // Each of these pairs goes through the creeper, and A-C does too.
    private static final int[] A = {0, 50, 10};
    private static final int[] B = {20, 51, 10};
    private static final int[] C = {20, 50, 11};
    private static final int[] D = {0, 51, 11};
    private static final int[] E = {10, 40, 10};
    private static final int[] F = {10, 60, 10};
    private static final int[] G = {10, 50, 0};
    private static final int[] H = {10, 51, 20};
    /** Wide of it with anything above. */
    private static final int[] WIDE = {0, 50, 14};
    /** The lantern it stands on. */
    private static final int[] UNDER = {10, 49, 10};
    /** Level with that one, off to the side. */
    private static final int[] LOW = {0, 49, 10};

    private static BeamPairs pairs() {
        return new BeamPairs(FEET, CreeperBeamsPuzzle.BEAMS);
    }

    @Test
    void twoPicksMakeABeam() {
        BeamPairs pairs = pairs();
        assertEquals(Pick.FIRST, pairs.pick(STEVE, A));
        assertEquals(Pick.FIRST, pairs.pick(STEVE, A), "the same one again is still the first");
        assertEquals(Pick.BEAM, pairs.pick(STEVE, B));
        assertArrayEquals(new int[][]{A, B}, pairs.last());
        assertEquals(Pick.NONE, pairs.pick(STEVE, A), "in a beam already");
        assertEquals(Pick.NONE, pairs.pick(ALEX, B), "in a beam already");
    }

    @Test
    void aMissDropsTheFirst() {
        BeamPairs pairs = pairs();
        assertEquals(Pick.FIRST, pairs.pick(STEVE, A));
        assertEquals(Pick.MISSED, pairs.pick(STEVE, WIDE));
        assertEquals(Pick.FIRST, pairs.pick(STEVE, B), "A was dropped");
        assertTrue(pairs.beams().isEmpty());
    }

    @Test
    void everyoneHasTheirOwnPicks() {
        BeamPairs pairs = pairs();
        assertEquals(Pick.FIRST, pairs.pick(STEVE, A));
        assertEquals(Pick.FIRST, pairs.pick(ALEX, E));
        assertEquals(Pick.BEAM, pairs.pick(STEVE, B));
        assertEquals(Pick.BEAM, pairs.pick(ALEX, F));
        assertEquals(2, pairs.beams().size());
    }

    /** Steve picks A, Alex makes a beam with A: Steve's pick is gone, and A isn't in a second beam with C. */
    @Test
    void aLanternIsInOneBeam() {
        BeamPairs pairs = pairs();
        assertTrue(BeamGeometry.throughCreeper(A, C, FEET), "A-C would count if A were free");
        assertEquals(Pick.FIRST, pairs.pick(STEVE, A));
        assertEquals(Pick.FIRST, pairs.pick(ALEX, A));
        assertEquals(Pick.BEAM, pairs.pick(ALEX, B));
        assertEquals(Pick.FIRST, pairs.pick(STEVE, C), "Steve starts again from C");
        assertEquals(1, pairs.beams().size());
        assertEquals(Pick.BEAM, pairs.pick(STEVE, D));
        assertArrayEquals(new int[][]{C, D}, pairs.last());
    }

    @Test
    void theLanternUnderIt() {
        BeamPairs pairs = pairs();
        assertEquals(Pick.FIRST, pairs.pick(STEVE, UNDER));
        assertEquals(Pick.MISSED, pairs.pick(STEVE, LOW), "not through the creeper");
        assertEquals(Pick.FIRST, pairs.pick(STEVE, UNDER));
        assertEquals(Pick.BEAM, pairs.pick(STEVE, F), "straight up through it");
    }

    @Test
    void fourAndItsDone() {
        BeamPairs pairs = pairs();
        int[][][] beams = {{A, B}, {C, D}, {E, F}, {G, H}};
        for (int i = 0; i < beams.length; i++) {
            assertFalse(pairs.isDone());
            assertEquals(Pick.FIRST, pairs.pick(STEVE, beams[i][0]));
            assertEquals(Pick.BEAM, pairs.pick(STEVE, beams[i][1]), "beam " + i);
        }
        assertTrue(pairs.isDone());
        assertEquals(Pick.NONE, pairs.pick(ALEX, UNDER));
        assertEquals(Pick.NONE, pairs.pick(ALEX, WIDE));
        assertEquals(4, pairs.beams().size());
    }
}
