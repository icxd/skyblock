package net.icxd.dungeons.item.enchanting.weapon;

import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.item.enchanting.FakeEnchantments;
import net.icxd.dungeons.stats.Stat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every weapon enchantment's level texts in the private data (skipped without it), read as the effects read them: each
 * has its numbers where the code takes them (how many, and in range), and a few are what the books say. So a changed
 * text shows here, not as a wrong number in a fight.
 */
class PrivateWeaponTextTest {
    /** How many numbers each level's text has, in the order the code reads them (ENCHANTS_WEAPONS.md). */
    private static final Map<WeaponEnchant, Integer> COUNTS = Map.ofEntries(
            Map.entry(WeaponEnchant.CLEAVE, 2), Map.entry(WeaponEnchant.LIFE_STEAL, 1), Map.entry(WeaponEnchant.MANA_STEAL, 1),
            Map.entry(WeaponEnchant.DRAIN, 2), Map.entry(WeaponEnchant.THUNDERLORD, 2), Map.entry(WeaponEnchant.THUNDERBOLT, 4),
            Map.entry(WeaponEnchant.LETHALITY, 3), Map.entry(WeaponEnchant.VENOMOUS, 4), Map.entry(WeaponEnchant.FIRE_ASPECT, 2),
            Map.entry(WeaponEnchant.KNOCKBACK, 1), Map.entry(WeaponEnchant.VAMPIRISM, 2), Map.entry(WeaponEnchant.TABASCO, 1),
            Map.entry(WeaponEnchant.CHAMPION, 4), Map.entry(WeaponEnchant.FLAME, 1), Map.entry(WeaponEnchant.PUNCH, 1),
            Map.entry(WeaponEnchant.PIERCING, 1), Map.entry(WeaponEnchant.INFINITE_QUIVER, 1), Map.entry(WeaponEnchant.TOXOPHILITE, 2),
            Map.entry(WeaponEnchant.INFERNO, 2), Map.entry(WeaponEnchant.FATAL_TEMPO, 3), Map.entry(WeaponEnchant.COMBO, 3),
            Map.entry(WeaponEnchant.SOUL_EATER, 2), Map.entry(WeaponEnchant.SWARM, 3), Map.entry(WeaponEnchant.REND, 3),
            Map.entry(WeaponEnchant.DUPLEX, 3), Map.entry(WeaponEnchant.WISE, 1), Map.entry(WeaponEnchant.JERRY, 1));

    private EnchantmentData data;

    @BeforeEach
    void real() {
        data = FakeEnchantments.real();
        EnchantmentData.use(data);
    }

    @AfterEach
    void reset() {
        FakeEnchantments.reset();
    }

    @Test
    void everyLevelHasItsNumbers() {
        assertEquals(WeaponEnchant.values().length, COUNTS.size());
        for (WeaponEnchant enchant : WeaponEnchant.values()) {
            EnchantmentData.Entry entry = data.get(enchant.id());
            assertNotNull(entry, enchant.id());
            for (int level : entry.levels().keySet()) {
                double[] numbers = EnchantText.of(enchant, level);
                assertEquals(COUNTS.get(enchant), numbers.length, enchant.id() + " " + level + ": " + entry.level(level).text());
                for (double number : numbers) assertTrue(number > 0, enchant.id() + " " + level);
            }
        }
    }

    /** What the numbers are, where the code relies on what they mean: times in seconds, radii in blocks, every Nth hit. */
    @Test
    void whatTheNumbersMean() {
        for (int level = 1; level <= 6; level++) {
            // Cleave's share is a percent, its radius a few blocks.
            assertTrue(EnchantText.at(WeaponEnchant.CLEAVE, level, 1) < 10);
            // Lethality lasts seconds and stacks a few times.
            assertEquals(4, EnchantText.at(WeaponEnchant.LETHALITY, level, 1));
            assertEquals(4, EnchantText.at(WeaponEnchant.LETHALITY, level, 2));
        }
        for (int level = 1; level <= 7; level++) {
            assertEquals(3, EnchantText.at(WeaponEnchant.THUNDERLORD, level, 0));
            assertEquals(3, EnchantText.at(WeaponEnchant.THUNDERBOLT, level, 0));
            assertEquals(10, EnchantText.at(WeaponEnchant.THUNDERBOLT, level, 2));
            assertEquals(40, EnchantText.at(WeaponEnchant.VENOMOUS, level, 2));
            assertEquals(5, EnchantText.at(WeaponEnchant.VENOMOUS, level, 3));
        }
        for (int level = 1; level <= 5; level++) {
            assertEquals(10, EnchantText.at(WeaponEnchant.INFERNO, level, 0));
            assertEquals(200, EnchantText.at(WeaponEnchant.FATAL_TEMPO, level, 1));
            assertEquals(3, EnchantText.at(WeaponEnchant.FATAL_TEMPO, level, 2));
            assertEquals(2 * level, EnchantText.at(WeaponEnchant.COMBO, level, 1));
            assertEquals(1_000_000, EnchantText.at(WeaponEnchant.SOUL_EATER, level, 1));
            assertEquals(10, EnchantText.at(WeaponEnchant.SWARM, level, 1));
            assertEquals(5, EnchantText.at(WeaponEnchant.REND, level, 1));
            assertEquals(2, EnchantText.at(WeaponEnchant.REND, level, 2));
            assertEquals(60, EnchantText.at(WeaponEnchant.DUPLEX, level, 2));
            assertEquals(10 * level, EnchantText.at(WeaponEnchant.WISE, level, 0));
        }
        for (int level = 1; level <= 10; level++) assertEquals(2, EnchantText.at(WeaponEnchant.CHAMPION, level, 1));
    }

    /** A few as the books have them (the wiki agrees). */
    @Test
    void theBooks() {
        assertEquals(5, EnchantText.at(WeaponEnchant.CLEAVE, 1, 0));
        assertEquals(3.3, EnchantText.at(WeaponEnchant.CLEAVE, 1, 1));
        assertEquals(60, EnchantText.at(WeaponEnchant.THUNDERLORD, 7, 1));
        assertEquals(1.4, EnchantText.at(WeaponEnchant.CHAMPION, 1, 2));
        assertEquals(50_000, EnchantText.tierUp(WeaponEnchant.CHAMPION, 1));
        assertEquals(0, EnchantText.tierUp(WeaponEnchant.CHAMPION, 10));
        // Life Steal VII (VI and the Blood-Soaked reforge's one more, past the books' VI): 16.8, the wiki's history.
        assertEquals(16.8, EnchantText.linear(WeaponEnchant.LIFE_STEAL, 7, 0), 1e-9);
        assertEquals(2, EnchantmentType.getByNamespace("tabasco").getStats(2).get(Stat.DAMAGE));
        assertEquals(10, EnchantmentType.getByNamespace("toxophilite").getStats(10).get(Stat.CRIT_CHANCE), 1e-9);
    }
}
