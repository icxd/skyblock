package net.icxd.dungeons.hex.enchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.hex.HexFakes;
import net.icxd.dungeons.hex.enchant.EnchantRules.Action;
import net.icxd.dungeons.hex.enchant.EnchantRules.Change;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enchanting.FakeEnchantments;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;

/** The Hex's enchanting rules, on a made-up table. */
class EnchantRulesTest {
    private static final SkyBlockItem SWORD = HexFakes.item("TEST_SWORD", "Test Sword", Rarity.LEGENDARY, SpecificItemType.SWORD);
    private static final SkyBlockItem WAND = HexFakes.item("TEST_WAND", "Test Wand", Rarity.EPIC, SpecificItemType.WAND);
    private static final SkyBlockItem PET_ITEM = HexFakes.item("TEST_PET_ITEM", "Test Pet Item", Rarity.EPIC, SpecificItemType.PET_ITEM);
    private EnchantmentData data;

    @BeforeEach
    void table() {
        data = FakeEnchantments.use();
    }

    @AfterEach
    void noTable() {
        FakeEnchantments.reset();
    }

    private List<String> ids(List<EnchantmentData.Entry> entries) {
        return entries.stream().map(EnchantmentData.Entry::id).toList();
    }

    @Test
    void offeredInTheHexsOrder() {
        // The table's order; not Telekinesis (taken out of the game) or Overload (bows) or Growth (armor).
        assertEquals(List.of("sharpness", "champion", "smite", "critical", "life_steal", "syphon", "mana_steal"),
                ids(EnchantRules.offered(data, SWORD, false)));
        assertEquals(List.of("one_for_all", "wise"), ids(EnchantRules.offered(data, SWORD, true)));
        assertEquals(List.of(), ids(EnchantRules.offered(data, WAND, false)));
        assertEquals(List.of("wise"), ids(EnchantRules.offered(data, WAND, true)));
        assertEquals(List.of(), ids(EnchantRules.offered(data, PET_ITEM, false)));
    }

    /**
     * The wiki's Weapon tab: a sword's 34 enchantments then made 26 groups, Sharpness, Smite and Bane of Arthropods
     * conflicting then. The books today have them together (and 933 live items), so today's count differs.
     */
    @Test
    void groupsAsTheWikisSword() {
        List<String> sword = List.of("sharpness", "champion", "tabasco", "smite", "bane_of_arthropods", "divine_gift", "knockback",
                "fire_aspect", "experience", "looting", "scavenger", "smoldering", "luck", "cubism", "cleave", "life_steal", "giant_killer",
                "critical", "first_strike", "ender_slayer", "impaling", "execute", "thunderlord", "lethality", "syphon", "vampirism",
                "dragon_hunter", "venomous", "triple_strike", "mana_steal", "thunderbolt", "prosecute", "vicious", "titan_killer");
        List<List<String>> then = List.of(List.of("sharpness", "smite", "bane_of_arthropods"), List.of("life_steal", "syphon", "mana_steal"),
                List.of("giant_killer", "titan_killer"), List.of("execute", "prosecute"), List.of("first_strike", "triple_strike"),
                List.of("thunderlord", "thunderbolt"));
        BiPredicate<String, String> conflict = (a, b) -> then.stream().anyMatch(pool -> pool.contains(a) && pool.contains(b));
        assertEquals(34, sword.size());
        assertEquals(26, EnchantRules.atOnce(sword, conflict));
        assertEquals(28, EnchantRules.atOnce(sword, (a, b) -> conflict.test(a, b) && !then.getFirst().contains(a)));
        assertEquals(0, EnchantRules.atOnce(List.of(), conflict));
        // Silk Touch conflicts with Fortune and with Smelting Touch, which go together: two at once.
        BiPredicate<String, String> silk = (a, b) -> a.equals("silk_touch") != b.equals("silk_touch");
        assertEquals(2, EnchantRules.atOnce(List.of("silk_touch", "fortune", "smelting_touch"), silk));
        assertEquals(3, EnchantRules.atOnce(List.of("silk_touch", "fortune", "smelting_touch", "efficiency"), silk));
        // Told one way round, a conflict is a conflict.
        assertEquals(1, EnchantRules.atOnce(List.of("thunderlord", "thunderbolt"), (a, b) -> a.equals("thunderbolt")));
    }

    @Test
    void summaries() {
        // 7 listed, Life Steal, Drain and Mana Steal one group: 5.
        assertEquals("  &7Enchantments &e0&7/&a5", EnchantRules.summary(data, SWORD, Map.of()));
        assertEquals("  &7Enchantments &e2&7/&a5", EnchantRules.summary(data, SWORD, Map.of("sharpness", 5, "syphon", 3, "growth", 1)));
        assertEquals("  &7Enchantments &a5&7/&a5",
                EnchantRules.summary(data, SWORD, Map.of("sharpness", 5, "champion", 1, "smite", 1, "critical", 1, "syphon", 1)));
        assertEquals("  &7Ultimate Enchantments &e0&7/&a1", EnchantRules.ultimateSummary(data, Map.of("sharpness", 5)));
        assertEquals("  &7Ultimate Enchantments &a1&7/&a1", EnchantRules.ultimateSummary(data, Map.of("wise", 5)));
    }

    private static Map<String, Integer> on(Object... pairs) {
        Map<String, Integer> on = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) on.put((String) pairs[i], (Integer) pairs[i + 1]);
        return on;
    }

    @Test
    void choosingALevel() {
        EnchantmentData.Entry sharpness = data.get("sharpness");
        assertEquals(new Change(Action.APPLY, on("smite", 7, "sharpness", 5), List.of()), EnchantRules.choose(data, on("smite", 7), sharpness, 5));
        assertEquals(new Change(Action.UPGRADE, on("sharpness", 6, "smite", 7), List.of()),
                EnchantRules.choose(data, on("sharpness", 5, "smite", 7), sharpness, 6));
        // Its own level takes it off.
        assertEquals(new Change(Action.REMOVE, on("smite", 7), List.of()), EnchantRules.choose(data, on("sharpness", 5, "smite", 7), sharpness, 5));
        assertEquals(Action.LOWER, EnchantRules.choose(data, on("sharpness", 5), sharpness, 4).action());
        assertEquals(on("sharpness", 5), EnchantRules.choose(data, on("sharpness", 5), sharpness, 4).after());
    }

    @Test
    void conflictsAreReplaced() {
        // Drain takes Life Steal's place (the wiki's own example).
        Change drain = EnchantRules.choose(data, on("life_steal", 3, "critical", 5), data.get("syphon"), 3);
        assertEquals(on("critical", 5, "syphon", 3), drain.after());
        assertEquals(List.of("life_steal"), drain.replaced());
        // One ultimate: Ultimate Wise takes One For All's place.
        Change wise = EnchantRules.choose(data, on("one_for_all", 1, "champion", 1), data.get("wise"), 2);
        assertEquals(on("champion", 1, "wise", 2), wise.after());
        // One For All goes with nothing: it takes every other off, and any other takes it off (Champion too, which
        // NEU's pools spare; no live item has the two).
        Change ofa = EnchantRules.choose(data, on("sharpness", 5, "champion", 1, "wise", 1), data.get("one_for_all"), 1);
        assertEquals(on("one_for_all", 1), ofa.after());
        assertEquals(List.of("sharpness", "champion", "wise"), ofa.replaced());
        assertEquals(on("sharpness", 5), EnchantRules.choose(data, on("one_for_all", 1), data.get("sharpness"), 5).after());
        Change champion = EnchantRules.choose(data, on("one_for_all", 1), data.get("champion"), 1);
        assertEquals(on("champion", 1), champion.after());
        assertEquals(List.of("one_for_all"), champion.replaced());
        // Stored under Hypixel's id, it's still One For All.
        assertEquals(on("growth", 5), EnchantRules.choose(data, EnchantRules.on(tag("ultimate_one_for_all", "1")), data.get("growth"), 5).after());
    }

    private static NBTTagCompound tag(String... enchantments) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "TEST_SWORD");
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < enchantments.length; i += 2) {
            NBTTagCompound e = new NBTTagCompound();
            e.setString("name", enchantments[i]);
            e.setShort("lvl", Short.parseShort(enchantments[i + 1]));
            list.add(e);
        }
        tag.set("enchantments", list);
        return tag;
    }

    @Test
    void theItemsData() {
        // Hypixel's id is read as the plugin's.
        NBTTagCompound tag = tag("smite", "7", "ultimate_wise", "3", "critical", "5");
        assertEquals(on("smite", 7, "wise", 3, "critical", 5), EnchantRules.on(tag));
        // Kept as stored, a level changed in place, one gone, a new one last.
        NBTTagCompound after = EnchantRules.with(tag, on("smite", 7, "wise", 4, "sharpness", 5));
        NBTTagList list = after.getList("enchantments", 10);
        assertEquals(3, list.size());
        assertEquals("smite", list.get(0).getString("name"));
        assertEquals(7, list.get(0).getInt("lvl"));
        assertEquals("ultimate_wise", list.get(1).getString("name"));
        assertEquals(4, list.get(1).getInt("lvl"));
        assertEquals("sharpness", list.get(2).getString("name"));
        assertEquals(5, list.get(2).getInt("lvl"));
        assertEquals("TEST_SWORD", after.getString("id"));
        // The data given isn't changed.
        assertEquals(3, tag.getList("enchantments", 10).size());
        assertEquals("critical", tag.getList("enchantments", 10).get(2).getString("name"));
        assertTrue(EnchantRules.with(tag, Map.of()).getList("enchantments", 10).isEmpty());
    }

    @Test
    void names() {
        assertEquals("&aSharpness VI", EnchantRules.bookName(data.get("sharpness"), 6));
        assertEquals("&d&lOne For All I", EnchantRules.bookName(data.get("one_for_all"), 1));
        assertEquals("&9Drain III", EnchantRules.displayName(data.get("syphon"), 3));
    }

    /** The wiki's Enchanting: 3.5 X^1.5 for X levels. */
    @Test
    void enchantingXp() {
        assertEquals(3.5, EnchantRules.enchantingXp(1), 1e-9);
        assertEquals(3.5 * 1000, EnchantRules.enchantingXp(100), 1e-9);
        assertEquals(0, EnchantRules.enchantingXp(0), 1e-9);
    }
}
