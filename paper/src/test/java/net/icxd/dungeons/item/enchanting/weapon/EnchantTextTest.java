package net.icxd.dungeons.item.enchanting.weapon;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The numbers in enchantments' texts, as the weapon enchantments read them. */
class EnchantTextTest {
    @AfterEach
    void reset() {
        WeaponTexts.reset();
    }

    @Test
    void numbersInOrderWithoutColours() {
        assertArrayEquals(new double[] {5, 3.3}, EnchantText.numbers("&7Hits &a5% &7of it to others within &a3.3 &7blocks."));
        // "2nd" and "10th" are their numbers; "1.1x" is 1.1; the "+" isn't a sign to keep.
        assertArrayEquals(new double[] {3, 2, 1.4, 7}, EnchantText.numbers("&7Gain &a3% &7XP. The 2nd hit: &6+1.4 coins &7& &3+7 &7orbs."));
        assertArrayEquals(new double[] {10, 1.1}, EnchantText.numbers("&7Every &c10th &7hit, &c1.1x&7."));
        assertArrayEquals(new double[0], EnchantText.numbers(null));
        assertArrayEquals(new double[0], EnchantText.numbers("&7Arrows travel through enemies."));
    }

    @Test
    void thousandsAndMillions() {
        assertArrayEquals(new double[] {50_000}, EnchantText.numbers("&850k Combat XP to tier up!"));
        assertArrayEquals(new double[] {1_500_000}, EnchantText.numbers("&81.5m Combat XP to tier up!"));
        assertArrayEquals(new double[] {2, 1_000_000}, EnchantText.numbers("&c2x &8(Max 1M outside Dungeons)."));
        assertArrayEquals(new double[] {1000, 5000}, EnchantText.numbers("&a1,000% or &a5,000%"));
        // A word going on isn't a thousand or a million: "5 more", "3 monsters".
        assertArrayEquals(new double[] {5, 3}, EnchantText.numbers("5 more and 3 monsters"));
        assertEquals(50_000, EnchantText.tierUp("&850k Combat XP to tier up!"));
        assertEquals(0, EnchantText.tierUp(null));
    }

    @Test
    void levelsFromTheTable() {
        WeaponTexts.use();
        assertEquals(5, EnchantText.at(WeaponEnchant.CLEAVE, 1, 0));
        assertEquals(3.6, EnchantText.at(WeaponEnchant.CLEAVE, 2, 1));
        // No such number, level or table: nothing.
        assertEquals(0, EnchantText.at(WeaponEnchant.CLEAVE, 2, 5));
        assertEquals(0, EnchantText.at(WeaponEnchant.CLEAVE, 3, 0));
        assertEquals(0, EnchantText.at(WeaponEnchant.VENOMOUS, 1, 0));
        assertEquals(50_000, EnchantText.tierUp(WeaponEnchant.CHAMPION, 1));
        assertEquals(0, EnchantText.tierUp(WeaponEnchant.CHAMPION, 3));
        assertTrue(EnchantText.hasLevel(WeaponEnchant.CHAMPION, 3));
        assertFalse(EnchantText.hasLevel(WeaponEnchant.CHAMPION, 4));
        WeaponTexts.reset();
        // Another table in use is read anew.
        assertEquals(0, EnchantText.at(WeaponEnchant.CLEAVE, 1, 0));
    }

    /** Past the texts, an amount that grows by the level goes on growing (the Blood-Soaked reforge's one more level). */
    @Test
    void linearPastTheTexts() {
        WeaponTexts.use();
        assertEquals(4.8, EnchantText.linear(WeaponEnchant.LIFE_STEAL, 2, 0), 1e-9);
        assertEquals(7.2, EnchantText.linear(WeaponEnchant.LIFE_STEAL, 3, 0), 1e-9);
        assertEquals(1, EnchantText.linear(WeaponEnchant.DRAIN, 2, 0), 1e-9);
        assertEquals(0, EnchantText.linear(WeaponEnchant.DRAIN, 0, 0));
    }
}
