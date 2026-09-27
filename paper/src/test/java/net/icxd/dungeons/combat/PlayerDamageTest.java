package net.icxd.dungeons.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Hits on players and the numbers they show (research damage.md 1.5 and 3). */
class PlayerDamageTest {
    @Test
    void kinds() {
        // Normal: through Defense; true: through True Defense only; a share of max health: neither.
        assertEquals(100 * 100 / 844.5, PlayerDamage.taken(100, PlayerDamage.Kind.NORMAL, 744.5, 33, 2206), 1e-9);
        assertEquals(100 * 100 / 133.0, PlayerDamage.taken(100, PlayerDamage.Kind.TRUE, 744.5, 33, 2206), 1e-9);
        // The recorded Arrow Trap: 15% of a 5,238.67 max with 2,434 Defense is 785.8.
        assertEquals(785.8, PlayerDamage.taken(0.15, PlayerDamage.Kind.MAX_HEALTH, 2434, 0, 5238.67), 0.001);
    }

    @Test
    void trapMessage() {
        assertEquals("&cThe Arrow Trap hit you for 785.8 damage!", PlayerDamage.trapMessage("Arrow Trap", 0.15 * 5238.67));
        assertEquals("&cThe Flamethrower hit you for 785.8 damage!", PlayerDamage.trapMessage("Flamethrower", 785.8));
        assertEquals("&cThe Arrow Trap hit you for 1,500 damage!", PlayerDamage.trapMessage("Arrow Trap", 1500));
    }

    /** "&71,047"; "✧16,485,463✧" in white, white, yellow, gold, red, red from the first ✧, commas too. */
    @Test
    void indicators() {
        assertEquals("&748", DamageIndicators.text(48.9, false));
        assertEquals("&71,047", DamageIndicators.text(1047, false));
        assertEquals("&714,689,667", DamageIndicators.text(14_689_667.3, false));
        assertEquals("§f✧§f1§e6§6,§c4§c8§f5§f,§e4§66§c3§c✧", DamageIndicators.text(16_485_463, true));
        assertEquals("§f✧§f7§e1§64§c,§c9§f1§f9§e✧", DamageIndicators.text(714_919, true));
        assertEquals("&72,147,483,647", DamageIndicators.text(1e12, false));
    }
}
