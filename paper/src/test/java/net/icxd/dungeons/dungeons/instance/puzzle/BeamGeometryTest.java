package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeamGeometryTest {
    /** A creeper standing in the middle of block (10, 50, 10). */
    private static final double[] FEET = {10.5, 50, 10.5};

    @Test
    void straightThrough() {
        assertTrue(BeamGeometry.throughCreeper(new int[]{0, 50, 10}, new int[]{20, 51, 10}, FEET));
        assertTrue(BeamGeometry.throughCreeper(new int[]{10, 40, 10}, new int[]{10, 60, 10}, FEET));
    }

    @Test
    void wideOfIt() {
        assertFalse(BeamGeometry.throughCreeper(new int[]{0, 50, 14}, new int[]{20, 50, 14}, FEET));
        // Over its head by more than the margin (head at 51.7, line at 53.5).
        assertFalse(BeamGeometry.throughCreeper(new int[]{0, 53, 10}, new int[]{20, 53, 10}, FEET));
    }

    /** The margin: a line 0.75 past the hitbox counts, one a block past doesn't. */
    @Test
    void margin() {
        // Hitbox x from 10.2 to 10.8; line centres at z + 0.5.
        double edge = 10.8;
        assertTrue(BeamGeometry.segmentHitsBox(new double[]{edge + 0.75, 50.5, 0}, new double[]{edge + 0.75, 50.5, 20},
                new double[]{10.2 - BeamGeometry.MARGIN, 50 - BeamGeometry.MARGIN, 10.2 - BeamGeometry.MARGIN},
                new double[]{edge + BeamGeometry.MARGIN, 51.7 + BeamGeometry.MARGIN, edge + BeamGeometry.MARGIN}));
        assertFalse(BeamGeometry.throughCreeper(new int[]{11, 50, 0}, new int[]{11, 50, 20}, new double[]{9.5, 50, 10.5}));
    }

    /** Only the part between the two lanterns counts, not the whole line. */
    @Test
    void segmentNotLine() {
        assertFalse(BeamGeometry.throughCreeper(new int[]{0, 50, 10}, new int[]{5, 50, 10}, FEET));
    }
}
