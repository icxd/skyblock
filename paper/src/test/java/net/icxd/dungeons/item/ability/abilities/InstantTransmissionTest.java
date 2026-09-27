package net.icxd.dungeons.item.ability.abilities;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Where Instant Transmission lands, in made-up worlds: a floor at y 68 (so feet at 69, eyes at 70.62)
 * and whatever blocks each test adds. Eyes start in the middle of block (0, 70, 0).
 */
class InstantTransmissionTest {
    private static final Vector EYE = new Vector(0.5, 70.62, 0.5);

    private final Set<String> solid = new HashSet<>();

    private void block(int x, int y, int z) {
        solid.add(x + "," + y + "," + z);
    }

    private InstantTransmission.Blocks world() {
        return (x, y, z) -> y > 68 && !solid.contains(x + "," + y + "," + z);
    }

    /** Looking east and down by {@code pitch} degrees. */
    private static Vector east(double pitch) {
        double r = Math.toRadians(pitch);
        return new Vector(Math.cos(r), -Math.sin(r), 0);
    }

    private InstantTransmission.Landing land(Vector direction) {
        return InstantTransmission.landing(EYE, direction, 8, world());
    }

    @Test
    void straightAhead() {
        assertEquals(new InstantTransmission.Landing(8, 69, 0, false), land(east(0)));
    }

    /** Looking a little down doesn't dive into the floor: the line starts at the eyes. */
    @Test
    void aLittleDown() {
        assertEquals(new InstantTransmission.Landing(8, 69, 0, false), land(east(5)));
        assertEquals(new InstantTransmission.Landing(8, 69, 0, false), land(east(10)));
        // At 15 degrees the eyes' line meets the floor 6.26 blocks on: they land there, on it.
        assertEquals(new InstantTransmission.Landing(6, 69, 0, false), land(east(15)));
    }

    /** Looking well down, they land on the floor where the line meets it, and that isn't blocks in the way. */
    @Test
    void ontoTheFloor() {
        InstantTransmission.Landing landing = land(east(45));
        assertEquals(69, landing.y());
        assertEquals(2, landing.x());
        assertEquals(false, landing.blocked());
    }

    /** A block in front of the feet is gone over; a floor a block higher is landed on. */
    @Test
    void overAStep() {
        block(3, 69, 0);
        assertEquals(new InstantTransmission.Landing(8, 69, 0, false), land(east(0)));
        for (int x = 3; x <= 12; x++) block(x, 69, 0);
        assertEquals(new InstantTransmission.Landing(8, 70, 0, false), land(east(0)));
    }

    /** A wall stops them short of it, on the floor, and blocks were in the way. */
    @Test
    void wall() {
        for (int y = 69; y <= 75; y++) block(5, y, 0);
        assertEquals(new InstantTransmission.Landing(4, 69, 0, true), land(east(0)));
    }

    /** A wall right in front, or a ceiling too low to stand under with room above the eyes: nowhere to go. */
    @Test
    void noRoom() {
        for (int y = 69; y <= 75; y++) block(1, y, 0);
        assertNull(land(east(0)));
        solid.clear();
        for (int x = -2; x <= 12; x++) block(x, 71, 0);
        assertNull(land(east(0)));
    }

    /** Straight up they rise, a block below where the eyes' line ends; a ceiling stops them under it. */
    @Test
    void up() {
        assertEquals(new InstantTransmission.Landing(0, 77, 0, false), land(new Vector(0, 1, 0)));
        block(0, 76, 0);
        assertEquals(new InstantTransmission.Landing(0, 73, 0, true), land(new Vector(0, 1, 0)));
    }
}
