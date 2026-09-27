package net.icxd.dungeons.combat;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ferocity's extra strikes, as the wiki's Ferocity page counts them. */
class FerocityTest {
    @Test
    void none() {
        assertEquals(0, Ferocity.extraStrikes(0, 0));
        assertEquals(0, Ferocity.extraStrikes(0, 0.999));
        assertEquals(0, Ferocity.extraStrikes(-20, 0));
    }

    /** 50: a 50% chance of one more strike. */
    @Test
    void fifty() {
        assertEquals(1, Ferocity.extraStrikes(50, 0));
        assertEquals(1, Ferocity.extraStrikes(50, 0.49));
        assertEquals(0, Ferocity.extraStrikes(50, 0.5));
        assertEquals(0, Ferocity.extraStrikes(50, 0.999));
        // The stats menu's "Ferocity 7": no sure strikes, a 7% chance of one.
        assertEquals(1, Ferocity.extraStrikes(7, 0.069));
        assertEquals(0, Ferocity.extraStrikes(7, 0.07));
    }

    /** 100: one more strike, always, and no chance of a second. */
    @Test
    void hundred() {
        assertEquals(1, Ferocity.extraStrikes(100, 0));
        assertEquals(1, Ferocity.extraStrikes(100, 0.999));
    }

    /** The wiki's example: 250 is a 100% chance of striking three times and a 50% chance of a fourth. */
    @Test
    void twoHundredFifty() {
        assertEquals(3, Ferocity.extraStrikes(250, 0.25));
        assertEquals(3, Ferocity.extraStrikes(250, 0.4999));
        assertEquals(2, Ferocity.extraStrikes(250, 0.5));
        assertEquals(2, Ferocity.extraStrikes(250, 0.999));
    }

    /** 500 is the cap: five more strikes, and more Ferocity adds nothing. */
    @Test
    void cap() {
        assertEquals(5, Ferocity.extraStrikes(500, 0));
        assertEquals(5, Ferocity.extraStrikes(500, 0.999));
        assertEquals(5, Ferocity.extraStrikes(749.385, 0));
    }

    /** A melee hit's extra strikes fail from more than 6 blocks away; an arrow's never do. */
    @Test
    void range() {
        assertTrue(Ferocity.inRange(false, 6));
        assertFalse(Ferocity.inRange(false, 6.01));
        assertTrue(Ferocity.inRange(true, 40));
    }

    /** The slash goes across the target, sideways to the hit, from its top on one side to its bottom on the other. */
    @Test
    void slash() {
        // Hit from the south (the attacker looks north, -z): the line runs along x.
        List<Vector> points = Ferocity.slash(new Vector(0.5, 65, 0.5), new Vector(0, 0, -3), 1, 2, true);
        assertEquals(Ferocity.SLASH_POINTS, points.size());
        Vector first = points.getFirst(), last = points.getLast();
        assertEquals(0.5, first.getZ(), 1e-9);
        assertEquals(0.5, last.getZ(), 1e-9);
        assertEquals(1, Math.abs(first.getX() - last.getX()), 1e-9);
        assertEquals(66, first.getY(), 1e-9);
        assertEquals(64, last.getY(), 1e-9);
        // The other way: from the bottom up.
        List<Vector> rising = Ferocity.slash(new Vector(0.5, 65, 0.5), new Vector(0, 0, -3), 1, 2, false);
        assertEquals(64, rising.getFirst().getY(), 1e-9);
        assertEquals(66, rising.getLast().getY(), 1e-9);
    }
}
