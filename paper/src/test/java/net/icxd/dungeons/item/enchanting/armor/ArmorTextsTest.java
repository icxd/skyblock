package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.hex.HexData;
import net.icxd.dungeons.hex.PrivateHex;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enchanting.FakeEnchantments;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.reforge.Reforge;
import net.icxd.dungeons.reforge.ReforgeTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The private data's texts (skipped without them) have the numbers where the armor enchantments and reforges read
 * them: how many each level's text has, and the few the code relies on being in their place.
 */
class ArmorTextsTest {
    @AfterEach
    void reset() {
        FakeEnchantments.reset();
    }

    /** How many numbers each level's text has, for every level with text (the effect's comment says which is which). */
    @Test
    void theEnchantmentsTexts() {
        EnchantmentData data = FakeEnchantments.real();
        EnchantmentData.use(data);
        Map<String, Integer> counts = Map.ofEntries(Map.entry("thorns", 2), Map.entry("reflection", 3), Map.entry("counter_strike", 2),
                Map.entry("last_stand", 5), Map.entry("no_pain_no_gain", 2), Map.entry("projectile_protection", 1),
                Map.entry("blast_protection", 1), Map.entry("fire_protection", 1), Map.entry("feather_falling", 2),
                Map.entry("depth_strider", 1), Map.entry("frost_walker", 1), Map.entry("stealth", 1), Map.entry("hardened_mana", 3),
                Map.entry("strong_mana", 3), Map.entry("ferocious_mana", 3), Map.entry("mana_vampire", 1), Map.entry("refrigerate", 3),
                Map.entry("respite", 1), Map.entry("transylvanian", 3), Map.entry("cayenne", 2), Map.entry("the_one", 3),
                Map.entry("quantum", 2), Map.entry("wisdom", 3), Map.entry("legion", 3), Map.entry("habanero_tactics", 4),
                Map.entry("hecatomb", 4));
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            EnchantmentData.Entry entry = data.get(e.getKey());
            assertNotNull(entry, e.getKey());
            int levels = 0;
            for (int level = entry.min(); level <= entry.max(); level++) {
                if (entry.level(level) == null) continue;
                levels++;
                assertEquals(e.getValue(), EnchantNumbers.of(e.getKey(), level).length, e.getKey() + " " + level);
            }
            assertTrue(levels > 0, e.getKey());
        }
        // Last Stand's share of health is its first number; Hecatomb's tiers go up by S runs in all.
        assertEquals(40, EnchantNumbers.get("last_stand", 1, 0));
        double runs = 0;
        for (int level = 1; level < data.get("hecatomb").max(); level++) {
            double[] tierUp = EnchantNumbers.tierUp("hecatomb", level);
            assertEquals(1, tierUp.length, "hecatomb " + level);
            assertTrue(tierUp[0] > runs, "hecatomb " + level);
            runs = tierUp[0];
        }
        assertEquals(0, EnchantNumbers.tierUp("hecatomb", data.get("hecatomb").max()).length);
    }

    /** The reforges' bonus texts at every rarity: a percent each, Blood-Soaked's one level, and Ridiculous's six numbers. */
    @Test
    void theReforgesTexts() {
        List<String> problems = new ArrayList<>();
        ReforgeTable table = ReforgeTable.read(HexData.json(PrivateHex.folder(), ReforgeTable.FILE, problems), problems);
        assertNotNull(table, problems.toString());
        Map<String, Integer> counts = Map.of("renowned", 1, "perfect", 1, "undead", 1, "cubic", 1, "blood_soaked", 1, "ridiculous", 6);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Reforge reforge = table.reforge(e.getKey());
            assertNotNull(reforge, e.getKey());
            for (Rarity rarity : Rarity.values()) {
                List<String> lines = reforge.bonusLines(rarity);
                if (!lines.isEmpty()) assertEquals(e.getValue(), EnchantNumbers.numbers(String.join(" ", lines)).length, e.getKey() + " " + rarity);
            }
        }
    }
}
