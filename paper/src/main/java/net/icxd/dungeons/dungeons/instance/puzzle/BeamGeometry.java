package net.icxd.dungeons.dungeons.instance.puzzle;

/**
 * Whether a beam between two sea lanterns goes through the creeper. How Hypixel decides is UNKNOWN:
 * the wiki says the four beams must "pass through the Creeper", the mods either list the pairs that
 * work (Odin, 11 pairs) or pair the lanterns whose line passes closest to it (Skyblocker). None of
 * Odin's pairs misses the creeper's hitbox by more than 0.76 of a block (lantern centre to lantern
 * centre), so a beam counts if that line goes through the hitbox grown by {@link #MARGIN}: to the
 * sides and over its head, not under its feet. There is the lantern it stands on, whose middle is
 * only half a block down; grown below too, the box would hold it, and a beam from that lantern to
 * any other would count (Odin pairs it with just one, the lantern straight over it).
 */
public final class BeamGeometry {
    /** How much bigger than the creeper's hitbox the beam may pass through, on every side but the bottom. */
    static final double MARGIN = 0.8;
    /** A creeper's hitbox. */
    static final double WIDTH = 0.6;
    static final double HEIGHT = 1.7;

    private BeamGeometry() {
    }

    /**
     * @param a      a lantern block
     * @param b      the other lantern block
     * @param feet   where the creeper stands (the middle of its feet)
     */
    public static boolean throughCreeper(int[] a, int[] b, double[] feet) {
        double half = WIDTH / 2 + MARGIN;
        double[] min = {feet[0] - half, feet[1], feet[2] - half};
        double[] max = {feet[0] + half, feet[1] + HEIGHT + MARGIN, feet[2] + half};
        return segmentHitsBox(new double[]{a[0] + 0.5, a[1] + 0.5, a[2] + 0.5}, new double[]{b[0] + 0.5, b[1] + 0.5, b[2] + 0.5}, min, max);
    }

    /** Whether the segment from p to q goes through the box (slab test). */
    static boolean segmentHitsBox(double[] p, double[] q, double[] min, double[] max) {
        double from = 0;
        double to = 1;
        for (int i = 0; i < 3; i++) {
            double d = q[i] - p[i];
            if (Math.abs(d) < 1e-9) {
                if (p[i] < min[i] || p[i] > max[i]) return false;
                continue;
            }
            double t1 = (min[i] - p[i]) / d;
            double t2 = (max[i] - p[i]) / d;
            from = Math.max(from, Math.min(t1, t2));
            to = Math.min(to, Math.max(t1, t2));
            if (from > to) return false;
        }
        return true;
    }
}
