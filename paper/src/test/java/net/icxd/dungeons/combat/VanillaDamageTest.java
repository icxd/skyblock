package net.icxd.dungeons.combat;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Vanilla damage by its cause, and the factors on it. */
class VanillaDamageTest {
    static {
        // Registered once for the whole run: half of falls and three quarters of explosions (a test's), for anyone.
        VanillaDamage.addFactor((player, cause) -> cause == VanillaDamage.Cause.FALL ? 0.5 : 1);
        VanillaDamage.addFactor((player, cause) -> cause == VanillaDamage.Cause.EXPLOSION ? 0.75 : 1);
        VanillaDamage.addFactor((player, cause) -> cause == VanillaDamage.Cause.FALL ? 0.5 : 1);
    }

    @Test
    void causes() {
        assertEquals(VanillaDamage.Cause.FALL, VanillaDamage.Cause.of(DamageCause.FALL));
        assertEquals(VanillaDamage.Cause.FIRE, VanillaDamage.Cause.of(DamageCause.FIRE_TICK));
        assertEquals(VanillaDamage.Cause.FIRE, VanillaDamage.Cause.of(DamageCause.HOT_FLOOR));
        assertEquals(VanillaDamage.Cause.LAVA, VanillaDamage.Cause.of(DamageCause.LAVA));
        assertEquals(VanillaDamage.Cause.EXPLOSION, VanillaDamage.Cause.of(DamageCause.BLOCK_EXPLOSION));
        assertEquals(VanillaDamage.Cause.EXPLOSION, VanillaDamage.Cause.of(DamageCause.ENTITY_EXPLOSION));
        assertEquals(VanillaDamage.Cause.PROJECTILE, VanillaDamage.Cause.of(DamageCause.PROJECTILE));
        assertEquals(VanillaDamage.Cause.DROWNING, VanillaDamage.Cause.of(DamageCause.DROWNING));
        assertEquals(VanillaDamage.Cause.OTHER, VanillaDamage.Cause.of(DamageCause.ENTITY_ATTACK));
        assertEquals(VanillaDamage.Cause.OTHER, VanillaDamage.Cause.of(null));
    }

    /** The factors multiply, by cause; the void and /kill are never changed. */
    @Test
    void factors() {
        assertEquals(0.25, VanillaDamage.factor(null, DamageCause.FALL), 1e-9);
        assertEquals(0.75, VanillaDamage.factor(null, DamageCause.ENTITY_EXPLOSION), 1e-9);
        assertEquals(1, VanillaDamage.factor(null, DamageCause.LAVA), 1e-9);
        assertEquals(1, VanillaDamage.factor(null, DamageCause.VOID), 1e-9);
        assertEquals(1, VanillaDamage.factor(null, DamageCause.KILL), 1e-9);
    }
}
