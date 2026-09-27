package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The numbers the abilities take from their recorded lore. */
class ClassAbilitiesTest {
    @Test
    void archer() {
        // "&8Cooldown: &a34s" and "for &a5 seconds" at Archer 16.
        assertEquals(34_000, ClassAbilities.explosiveShotCooldown(16));
        assertEquals(40_000, ClassAbilities.explosiveShotCooldown(0));
        assertEquals(20_000, ClassAbilities.explosiveShotCooldown(50));
        assertEquals(5, ClassAbilities.rapidFireSeconds(16));
        assertEquals(4, ClassAbilities.rapidFireSeconds(9));
    }

    @Test
    void tank() {
        assertEquals(20_000, ClassAbilities.seismicWaveDamage(49), 1e-6);
        assertEquals(22_000, ClassAbilities.seismicWaveDamage(50), 1e-6);
        assertEquals(40_000, ClassAbilities.seismicWaveDamage(500), 1e-6);
    }

    @Test
    void wish() {
        assertEquals(120_000, ClassAbilities.wishCooldown(0));
        assertEquals(100_000, ClassAbilities.wishCooldown(2));
        assertEquals(0, ClassAbilities.wishCooldown(20));
    }
}
