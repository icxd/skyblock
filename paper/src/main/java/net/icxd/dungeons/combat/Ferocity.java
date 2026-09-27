package net.icxd.dungeons.combat;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Ferocity, as the wiki's Ferocity page has it ("Mechanics"): each point is a 1% chance for a hit to strike
 * an extra time, and every full 100 is an extra strike for sure, so 250 is two more strikes and a 50% chance
 * of a third. At most 500. It works on melee hits and arrows, not on abilities; a melee hit's extra strikes
 * fail from more than 6 blocks away; they don't trigger Life Steal or Drain (neither is in the plugin yet).
 * No Bukkit here but the vector maths; {@link Combat} does the strikes.
 */
public final class Ferocity {
    /** The stat's cap (the wiki's infobox, "max_value=500"). */
    public static final double CAP = 500;
    /** "When using melee weapons, Ferocity fails to activate when the player is more than 6 blocks away from the target." */
    public static final double MELEE_RANGE = 6;
    /** The wiki gives no delay between the strikes; a short one, so each shows as its own number. */
    public static final int STRIKE_DELAY_TICKS = 3;
    /** Dust points in the red slash each strike draws across the target (the owner's description; how many is ours). */
    static final int SLASH_POINTS = 12;

    private Ferocity() {
    }

    /**
     * How many extra times a hit strikes with this much Ferocity: one for each full 100 (up to the cap),
     * and one more if {@code random} (0 inclusive to 1 exclusive) falls within the rest as a percentage.
     */
    public static int extraStrikes(double ferocity, double random) {
        double capped = Math.max(0, Math.min(ferocity, CAP));
        int sure = (int) Math.floor(capped / 100);
        double chance = capped - sure * 100.0;
        return sure + (random * 100 < chance ? 1 : 0);
    }

    /** Whether a hit's extra strikes happen from where the player is: an arrow's always, a melee hit's within 6 blocks. */
    public static boolean inRange(boolean ranged, double distance) {
        return ranged || distance <= MELEE_RANGE;
    }

    /**
     * The points of a strike's red slash: a straight line through {@code center} across the target, as wide
     * as {@code width} and as tall as {@code height}, sideways to the way it was hit from ({@code from}, the
     * horizontal direction from the attacker to the target), top left to bottom right or, when
     * {@code falling} is false, bottom left to top right.
     */
    static List<Vector> slash(Vector center, Vector from, double width, double height, boolean falling) {
        Vector flat = new Vector(from.getX(), 0, from.getZ());
        if (flat.lengthSquared() < 1e-9) flat = new Vector(0, 0, 1);
        flat.normalize();
        // Sideways to the hit: the attacker sees the line go across the target.
        Vector side = new Vector(-flat.getZ(), 0, flat.getX()).multiply(width / 2);
        Vector up = new Vector(0, height / 2, 0);
        Vector start = center.clone().subtract(side).add(falling ? up : up.clone().multiply(-1));
        Vector end = center.clone().add(side).add(falling ? up.clone().multiply(-1) : up);
        List<Vector> points = new ArrayList<>(SLASH_POINTS);
        for (int i = 0; i < SLASH_POINTS; i++) {
            double t = i / (double) (SLASH_POINTS - 1);
            points.add(start.clone().multiply(1 - t).add(end.clone().multiply(t)));
        }
        return points;
    }
}
