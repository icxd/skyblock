package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

/** Which hitboxes an ability's ball, cone and line reach. */
class ShapesTest {
    /** A zombie-sized hitbox standing at x, y, z. */
    private static BoundingBox mob(double x, double y, double z) {
        return new BoundingBox(x - 0.3, y, z - 0.3, x + 0.3, y + 1.95, z + 0.3);
    }

    /** Any part of the hitbox in the ball counts: a mob whose side is 6 blocks off is in Implosion's 6. */
    @Test
    void ball() {
        Vector center = new Vector(0, 0, 0);
        assertTrue(Shapes.inBall(center, 6, mob(6.2, 0, 0)));
        assertFalse(Shapes.inBall(center, 6, mob(6.4, 0, 0)));
        assertTrue(Shapes.inBall(center, 6, mob(0, -7.9, 0)));
        assertTrue(Shapes.inBall(center, 0.1, mob(0, -1, 0)));
    }

    /** Ice Spray's cone: 7 long and 60° wide, so up to 30° to either side, by the hitbox's middle. */
    @Test
    void cone() {
        Vector eye = new Vector(0, 1, 0);
        Vector ahead = new Vector(1, 0, 0);
        assertTrue(Shapes.inCone(eye, ahead, 7, 60, mob(5, 0.025, 0)));
        // 29° and 31° off.
        assertTrue(Shapes.inCone(eye, ahead, 7, 60, mob(5 * Math.cos(Math.toRadians(29)), 0.025, 5 * Math.sin(Math.toRadians(29)))));
        assertFalse(Shapes.inCone(eye, ahead, 7, 60, mob(5 * Math.cos(Math.toRadians(31)), 0.025, 5 * Math.sin(Math.toRadians(31)))));
        assertFalse(Shapes.inCone(eye, ahead, 7, 60, mob(7.5, 0.025, 0)));
        assertFalse(Shapes.inCone(eye, ahead, 7, 60, mob(-3, 0.025, 0)));
    }

    /** A line hits what its width reaches, and says how far along it did. */
    @Test
    void line() {
        Vector start = new Vector(0, 1, 0);
        Vector ahead = new Vector(0, 0, 1);
        assertEquals(9.7, Shapes.along(start, ahead, 32, 0, mob(0, 0, 10)), 1e-9);
        assertEquals(-1, Shapes.along(start, ahead, 32, 0, mob(1, 0, 10)));
        assertEquals(9.4, Shapes.along(start, ahead, 32, 0.3, mob(0.5, 0, 10)), 1e-9);
        assertEquals(-1, Shapes.along(start, ahead, 5, 0.3, mob(0, 0, 10)));
        assertEquals(0, Shapes.along(start, ahead, 5, 0, mob(0, 0, 0)));
    }

    /** Minecraft's yaw: 0 faces +z, 90 faces -x. */
    @Test
    void ahead() {
        Vector feet = new Vector(10, 64, 10);
        Vector south = Shapes.ahead(feet, 0, 5);
        assertEquals(10, south.getX(), 1e-9);
        assertEquals(15, south.getZ(), 1e-9);
        assertEquals(64, south.getY(), 1e-9);
        Vector west = Shapes.ahead(feet, 90, 5);
        assertEquals(5, west.getX(), 1e-9);
        assertEquals(10, west.getZ(), 1e-9);
    }
}
