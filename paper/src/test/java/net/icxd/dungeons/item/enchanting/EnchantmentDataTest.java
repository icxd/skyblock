package net.icxd.dungeons.item.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.icxd.dungeons.hex.enchant.EnchantRules;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** The enchantments' table: reading it, finding one by any name, what goes on what; and the private one as it is. */
class EnchantmentDataTest {
    @AfterEach
    void noTable() {
        FakeEnchantments.reset();
    }

    private static SkyBlockItem item(String name, SpecificItemType type, String typeKey) {
        return new SkyBlockItem() {
            @Override
            public String id() {
                return "TEST";
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public Material material() {
                return Material.STONE;
            }

            @Override
            public SpecificItemType specificItemType() {
                return type;
            }

            @Override
            public String typeKey() {
                return typeKey != null ? typeKey : SkyBlockItem.super.typeKey();
            }
        };
    }

    @Test
    void anyName() {
        EnchantmentData data = FakeEnchantments.table();
        // Hypixel's id, the plugin's, any case.
        assertEquals("one_for_all", data.get("ultimate_one_for_all").id());
        assertEquals("one_for_all", data.get("ONE_FOR_ALL").id());
        assertEquals("wise", data.get("ULTIMATE_WISE").id());
        assertNull(data.get("nothing"));
        assertNull(data.get(null));
        assertEquals("one_for_all", EnchantmentData.id("Ultimate_One_For_All"));
        assertEquals("syphon", EnchantmentData.id("syphon"));
        assertTrue(data.conflict("syphon", "life_steal"));
        assertTrue(data.conflict("life_steal", "syphon"));
        assertFalse(data.conflict("sharpness", "smite"));
        assertFalse(data.conflict("sharpness", "sharpness"));
    }

    @Test
    void entries() {
        EnchantmentData data = FakeEnchantments.table();
        EnchantmentData.Entry sharpness = data.get("sharpness");
        assertEquals(Integer.valueOf(30), sharpness.xp(5));
        assertNull(sharpness.xp(8));
        assertTrue(sharpness.fromTable(5));
        assertFalse(sharpness.fromTable(6));
        assertEquals(Rarity.RARE, sharpness.level(6).rarity());
        assertNull(sharpness.level(6).lines());
        assertEquals(List.of("&7Gain &a3% &7extra", "&7Combat XP."), data.get("champion").level(1).lines());
        assertTrue(data.get("telekinesis").removed());
        assertFalse(data.get("mana_steal").fromTable(1));
        // The table's order first, then the rest in the file's.
        assertEquals(List.of("sharpness", "champion", "smite", "critical", "life_steal", "syphon", "mana_steal", "one_for_all", "wise",
                "telekinesis", "overload", "growth", "bank"), data.order());
    }

    @Test
    void typesByTheBooksWords() {
        assertTrue(EnchantmentData.is(item("Test", SpecificItemType.HELMET, null), "Armor"));
        assertTrue(EnchantmentData.is(item("Test", SpecificItemType.NONE, "CARNIVAL_MASK"), "Chestplate"));
        assertTrue(EnchantmentData.is(item("Test", SpecificItemType.NONE, "FARMING_TOOL"), "Farming Tool"));
        assertTrue(EnchantmentData.is(item("Test", SpecificItemType.NONE, "FARMING_TOOL"), "Hoe"));
        assertTrue(EnchantmentData.is(item("Test", SpecificItemType.NONE, "VACUUM"), "Vacuum"));
        assertTrue(EnchantmentData.is(item("Test", SpecificItemType.SPADE, null), "Shovel"));
        assertTrue(EnchantmentData.is(item("Test", SpecificItemType.DRILL, null), "Tools"));
        assertFalse(EnchantmentData.is(item("Test", SpecificItemType.SWORD, null), "Tools"));
        assertTrue(EnchantmentData.is(item("Precursor Eye", SpecificItemType.NONE, null), "Precursor Eye"));
        assertFalse(EnchantmentData.is(item("Test", SpecificItemType.NONE, null), "Precursor Eye"));
    }

    @Test
    void readingIt(@TempDir Path folder) throws IOException {
        List<String> problems = new ArrayList<>();
        assertNull(EnchantmentData.read(folder, problems));
        assertTrue(problems.getFirst().startsWith("no "), problems.toString());
        Files.writeString(folder.resolve(EnchantmentData.FILE), FakeEnchantments.json());
        problems.clear();
        EnchantmentData data = EnchantmentData.read(folder, problems);
        assertEquals(List.of(), problems);
        assertEquals(13, data.size());
        Files.writeString(folder.resolve(EnchantmentData.FILE), "{\"format\":2}");
        assertNull(EnchantmentData.read(folder, problems));
    }

    @Test
    void theTypeAsksTheTable() {
        assertNull(EnchantmentType.getByNamespace("sharpness"));
        assertEquals("Scavenger", EnchantmentType.SCAVENGER.getName());
        FakeEnchantments.use();
        EnchantmentType drain = EnchantmentType.getByNamespace("SYPHON");
        assertEquals("syphon", drain.getNamespace());
        assertEquals("Drain", drain.getName());
        assertEquals(EnchantmentType.getByNamespace("ultimate_wise"), EnchantmentType.getByNamespace("wise"));
        assertTrue(EnchantmentType.getByNamespace("wise").isUltimate());
        assertEquals(13, EnchantmentType.all().size());
    }

    /** Plain stats read from the text: one, two, and not those that depend on something. */
    @Test
    void statsFromTheText() {
        Stats absorb = EnchantmentType.stats("&7Grants &3+1☯ Foraging Wisdom &7and &6+2☘ Foraging Fortune&7.");
        assertEquals(1, absorb.get(Stat.FORAGING_WISDOM), 1e-9);
        assertEquals(2, absorb.get(Stat.FORAGING_FORTUNE), 1e-9);
        assertEquals(6, EnchantmentType.stats("&7Grants &b+6✯ Magic Find.").get(Stat.MAGIC_FIND), 1e-9);
        assertEquals(new Stats(), EnchantmentType.stats("&7Grants &c+1❤ Health &7and &f+0.5❂ True Defense &7per digit in your &6Accessory Power&7."));
        assertEquals(new Stats(), EnchantmentType.stats("&7Grants &60.25☘ Farming Fortune &7per unique visitor served."));
        assertEquals(new Stats(), EnchantmentType.stats(null));
    }

    // The private table as it is

    /** Every enchantment Hypixel has, and each one the plugin had before (the 76 with Java constants). */
    @Test
    void everyOne() {
        EnchantmentData data = FakeEnchantments.real();
        assertEquals(156, data.size());
        for (String id : List.of("bane_of_arthropods", "champion", "cleave", "critical", "cubism", "dragon_hunter", "ender_slayer", "execute",
                "fire_aspect", "first_strike", "giant_killer", "lethality", "life_steal", "looting", "luck", "mana_steal", "prosecute",
                "scavenger", "sharpness", "smite", "smoldering", "syphon", "thunderbolt", "thunderlord", "titan_killer", "triple_strike",
                "vampirism", "venomous", "vicious", "fortune", "pristine", "cultivating", "delicate", "harvesting", "replenish", "chance",
                "infinite_quiver", "overload", "power", "snipe", "angler", "blessing", "caster", "charm", "corruption", "expertise", "frail",
                "luck_of_the_sea", "lure", "magnet", "piscary", "spiked_hook", "big_brain", "hecatomb", "counter_strike", "true_protection",
                "smarty_pants", "feather_falling", "sugar_rush", "blast_protection", "ferocious_mana", "fire_protection", "growth",
                "hardened_mana", "mana_vampire", "projectile_protection", "protection", "rejuvenate", "respite", "strong_mana", "compact",
                "experience", "cayenne", "prosperity", "tabasco", "one_for_all")) {
            assertNotNull(data.get(id), id);
        }
        assertEquals(26, data.all().stream().filter(EnchantmentData.Entry::ultimate).count());
        // Hypixel's names now.
        assertEquals("Drain", data.get("syphon").name());
        assertEquals("Gravity", data.get("dragon_hunter").name());
        assertEquals("Duplex", data.get("ultimate_reiterate").name());
    }

    @Test
    void eachOneWhole() {
        EnchantmentData data = FakeEnchantments.real();
        Set<String> kinds = Set.of("Sword", "Longsword", "Fishing Weapon", "Gauntlet", "Bow", "Fishing Rod", "Wand", "Armor", "Helmet",
                "Chestplate", "Leggings", "Boots", "Necklace", "Belt", "Bracelet", "Cloak", "Gloves", "Pickaxe", "Drill", "Axe", "Shovel",
                "Shears", "Hoe", "Farming Tool", "Tools", "Vacuum", "Precursor Eye", "Aspect of the Jerry");
        for (EnchantmentData.Entry e : data.all()) {
            assertTrue(e.min() >= 1 && e.min() <= e.max(), e.id());
            assertNotNull(e.level(e.min()), e.id() + " has no text at its lowest level");
            assertEquals(e.max(), e.xp().size(), e.id());
            assertTrue(kinds.containsAll(e.applies()), e.id() + ": " + e.applies());
            for (String other : e.conflicts()) {
                assertNotNull(data.get(other), e.id() + ": " + other);
                assertTrue(data.get(other).conflicts().contains(e.id()), e.id() + " and " + other);
            }
        }
    }

    /**
     * A sword's list: the wiki's 34 (its first page as the wiki shows it) and Pyroclasm, newer; Telekinesis is out. Its
     * 35 make 29 groups: Life Steal, Drain and Mana Steal; Giant and Titan Killer; Execute and Prosecute; First Strike
     * and Triple-Strike; Thunderlord and Thunderbolt (Sharpness, Smite and Bane of Arthropods don't conflict now).
     */
    @Test
    void aSword() {
        EnchantmentData data = FakeEnchantments.real();
        SkyBlockItem sword = item("Test", SpecificItemType.SWORD, null);
        List<String> offered = EnchantRules.offered(data, sword, false).stream().map(EnchantmentData.Entry::id).toList();
        assertEquals(35, offered.size());
        assertEquals(List.of("sharpness", "champion", "tabasco", "smite", "bane_of_arthropods", "divine_gift", "knockback", "fire_aspect",
                "experience", "looting", "scavenger", "smoldering", "luck", "cubism", "cleave"), offered.subList(0, 15));
        assertEquals("magmarizer", offered.getLast());
        assertEquals(29, EnchantRules.groups(offered, data::conflict));
        assertEquals(List.of("one_for_all", "wise", "combo", "chimera", "fatal_tempo", "inferno", "soul_eater", "swarm"),
                EnchantRules.offered(data, sword, true).stream().map(EnchantmentData.Entry::id).toList());
    }
}
