package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

/** A summoned pet's name and head by its holder: as recorded (2026-10-25, a Bee at 02:31.7), turned as it faces. */
class PetFollowersTest {
    private static final double EPSILON = 1e-9;

    @Test
    void theRecordedNameStand() {
        // At the summon, as for a holder at 8.5 71.8305 -51.5 facing +x: its name stand at 8.1 73.2805 -51.56.
        Location name = PetFollowers.nameSpot(new Location(null, 8.5, 71.8305, -51.5, -90, 0));
        assertEquals(8.1, name.getX(), EPSILON);
        assertEquals(73.2805, name.getY(), EPSILON);
        assertEquals(-51.56, name.getZ(), EPSILON);
    }

    @Test
    void turnedAsItFaces() {
        // Facing -x: its left is +z, its back +x.
        Location holder = new Location(null, 0, 0, 0, 90, 0);
        Location name = PetFollowers.nameSpot(holder);
        assertEquals(0.4, name.getX(), EPSILON);
        assertEquals(0.06, name.getZ(), EPSILON);
        assertEquals(0, name.getYaw());
        Location head = PetFollowers.headSpot(holder);
        assertEquals(0.425, head.getX(), EPSILON);
        assertEquals(1.03, head.getY(), EPSILON);
        assertEquals(0.2, head.getZ(), EPSILON);
    }

    @Test
    void lookingAtItsHead() {
        Location head = PetFollowers.headSpot(new Location(null, 0, 72, 0, 0, 0));
        // Two blocks south of it, looking north at it; then 30° off, the other way, and from too far.
        assertTrue(PetFollowers.aimedAt(eye(head, 2, 180), head, 3));
        assertFalse(PetFollowers.aimedAt(eye(head, 2, 210), head, 3));
        assertFalse(PetFollowers.aimedAt(eye(head, 2, 0), head, 3));
        assertFalse(PetFollowers.aimedAt(eye(head, 4, 180), head, 3));
    }

    private static Location eye(Location head, double south, float yaw) {
        Location eye = head.clone().add(0, 0, south);
        eye.setYaw(yaw);
        eye.setPitch(0);
        return eye;
    }
}
