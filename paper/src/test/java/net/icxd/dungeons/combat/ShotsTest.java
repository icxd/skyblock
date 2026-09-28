package net.icxd.dungeons.combat;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An arrow's recorded shot as it goes through mobs (Piercing-like numbers). */
class ShotsTest {
    private static Shots.Shot shot() {
        Damage.Attacker attacker = new Damage.Attacker(100, 50, 30, 50, 10, 1_000, Map.of("power", 5), true, 0, 2);
        return new Shots.Shot(attacker, true, false, 40, new Location(null, 0, 0, 0), null);
    }

    /** It goes through so many more mobs, each hit with a share of the shot's damage (not of the one before). */
    @Test
    void pierces() {
        Shots.Shot first = shot().piercing(2, 0.25);
        Shots.Shot second = first.pierced();
        assertEquals(0.5, second.launched().multiplier(), 1e-9);
        assertTrue(second.critical());
        assertEquals(40, second.ferocity(), 1e-9);
        assertEquals(5, second.launched().enchantments().get("power"));
        Shots.Shot third = second.pierced();
        assertEquals(0.5, third.launched().multiplier(), 1e-9);
        assertNull(third.pierced());
    }

    /** One that doesn't pierce is gone after its hit; a scaled one keeps how many it goes through. */
    @Test
    void doesntPierce() {
        assertNull(shot().pierced());
        assertNull(shot().piercing(-3, 0.25).pierced());
        Shots.Shot doubled = shot().piercing(1, 0.25).times(2);
        assertEquals(4, doubled.launched().multiplier(), 1e-9);
        assertEquals(1, doubled.pierces());
        assertEquals(1, doubled.pierced().launched().multiplier(), 1e-9);
    }
}
