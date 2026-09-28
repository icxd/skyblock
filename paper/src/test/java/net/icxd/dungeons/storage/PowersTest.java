package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Accessory Powers' stats at an Accessory Power (the wiki's Module:Power), against what Hypixel showed: the
 * recorded Select Power Stone menu at 571 (the Loadouts and Storage tour, 01:07.4), the recorded loadout
 * pages at 500 (01:02.8 and 01:09.8), and a Power Stone's own lore at 250 (items.json).
 */
class PowersTest {
    private static final StorageTables.Power MADE_UP = new StorageTables.Power("Made Up", "Starter Power", null, "STONE", 0,
            Map.of(Stat.HEALTH, 50.0, Stat.STRENGTH, 50.0), Map.of(Stat.FEROCITY, 2.0));

    @Test
    void multiplier() {
        assertEquals(0, Powers.multiplier(0), 1e-9);
        assertEquals(0, Powers.multiplier(-5), 1e-9);
        // 29.97 * ln(0.0019 * 250 + 1) ^ 1.2
        assertEquals(9.642, Powers.multiplier(250), 0.001);
        assertTrue(Powers.multiplier(571) > Powers.multiplier(500));
    }

    @Test
    void formula() {
        StorageTables tables = PrivateTables.withPowers(MADE_UP);
        Stats stats = Powers.stats(MADE_UP, 250, tables);
        // base / 100 * multiplier (Health's is 2 here, Strength's 1 by default) * 24 * M
        assertEquals(0.5 * 2 * 24 * Powers.multiplier(250), stats.get(Stat.HEALTH), 1e-9);
        assertEquals(0.5 * 24 * Powers.multiplier(250), stats.get(Stat.STRENGTH), 1e-9);
        // The Unique Power Bonus doesn't grow, and isn't in the stats.
        assertEquals(0, stats.get(Stat.FEROCITY), 1e-9);
        assertEquals(2, Powers.bonus(MADE_UP).get(Stat.FEROCITY), 1e-9);
        assertEquals(List.of("&c+2 Ferocity"), Powers.bonusLines(MADE_UP));
        // In Hypixel's order, with at most two decimals and no symbol.
        assertEquals(List.of("&c+231.41 Health", "&c+115.7 Strength"), Powers.statLines(MADE_UP, 250, tables));
    }

    @Test
    void recordedAt571() {
        StorageTables tables = PrivateTables.load();
        assertEquals(List.of("&9+472.03 Crit Damage", "&f+12.42 Speed"), Powers.statLines(tables.power("Silky"), 571, tables));
        assertEquals(List.of("&e+5 Attack Speed"), Powers.bonusLines(tables.power("Silky")));
        assertEquals(List.of("&b+745.31 Intelligence"), Powers.statLines(tables.power("Sighted"), 571, tables));
        assertEquals(List.of("&c+3 Ability Damage"), Powers.bonusLines(tables.power("Sighted")));
        assertEquals(List.of("&c+34.78 Health", "&c+372.65 Strength", "&9+99.37 Crit Damage"), Powers.statLines(tables.power("Forceful"), 571,
                tables));
        assertEquals(List.of("&c+4 Ferocity"), Powers.bonusLines(tables.power("Forceful")));
        assertEquals(List.of("&c+69.56 Health", "&a+24.84 Defense", "&c+173.91 Strength", "&9+49.69 Crit Chance", "&9+124.22 Crit Damage"),
                Powers.statLines(tables.power("Warrior"), 571, tables));
        assertEquals(List.of("&c+104.34 Health", "&c+74.53 Strength", "&9+29.81 Crit Chance", "&9+74.53 Crit Damage", "&e+29.81 Attack Speed",
                "&b+89.44 Intelligence", "&f+19.87 Speed"), Powers.statLines(tables.power("Ominous"), 571, tables));
        assertEquals(List.of(), Powers.bonusLines(tables.power("Ominous")));
    }

    @Test
    void recordedAt500() {
        StorageTables tables = PrivateTables.load();
        assertEquals(List.of("&9+420.94 Crit Damage", "&f+11.08 Speed"), Powers.statLines(tables.power("Silky"), 500, tables));
        assertEquals(List.of("&b+664.64 Intelligence"), Powers.statLines(tables.power("Sighted"), 500, tables));
    }

    @Test
    void stoneLoreAt250() {
        StorageTables tables = PrivateTables.load();
        assertEquals(List.of("&c+16.2 Health", "&c+173.56 Strength", "&9+46.28 Crit Damage"), Powers.statLines(tables.power("Forceful"), 250,
                tables));
    }

    @Test
    void recordedOrder() {
        StorageTables tables = PrivateTables.load();
        List<String> recorded = List.of("Forceful", "Sighted", "Silky", "Sweet", "Sanguisuge", "Commando", "Disciplined", "Inspired", "Ominous",
                "Prepared", "Fortuitous", "Pretty", "Protected", "Simple", "Warrior");
        List<String> sorted = tables.powers().values().stream().filter(p -> recorded.contains(p.name())).sorted(Powers.ORDER)
                .map(StorageTables.Power::name).toList();
        assertEquals(recorded, sorted);
    }

    @Test
    void unlocked() {
        StorageTables tables = PrivateTables.load();
        List<String> starter = List.of("Fortuitous", "Pretty", "Protected", "Simple", "Warrior");
        List<String> intermediate = List.of("Commando", "Disciplined", "Inspired", "Ominous", "Prepared");
        // Every Stone Power, as if learned from Maxwell (who isn't here), best first; then the Starter Powers.
        List<String> none = names(Powers.unlocked(0, tables));
        assertEquals(22 + starter.size(), none.size());
        assertEquals("Scorching", none.getFirst());
        assertEquals(starter, none.subList(22, none.size()));
        assertTrue(none.contains("Silky"));
        // The Intermediate Powers come with Combat XV (the wiki's Powers).
        assertFalse(names(Powers.unlocked(14, tables)).contains("Commando"));
        List<String> fifteen = names(Powers.unlocked(15, tables));
        assertEquals(tables.powers().size(), fifteen.size());
        assertEquals(intermediate, fifteen.subList(22, 27));
    }

    @Test
    void tables() {
        StorageTables tables = PrivateTables.load();
        assertEquals(32, tables.powers().size());
        for (StorageTables.Power power : tables.powers().values()) {
            assertFalse(power.stats().isEmpty(), power.name());
            // A Stone Power shows as its stone; the others as their material.
            if (!power.stonePower()) assertNotNull(org.bukkit.Material.matchMaterial(power.icon()), power.name());
        }
        // Each rarity gives more Accessory Power than the one below it.
        List<Rarity> rarities = List.of(Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY, Rarity.MYTHIC, Rarity.DIVINE);
        for (int i = 1; i < rarities.size(); i++) {
            assertTrue(tables.accessoryPower(rarities.get(i)) > tables.accessoryPower(rarities.get(i - 1)), rarities.get(i).name());
        }
        assertTrue(tables.accessoryPower(Rarity.COMMON) > 0);
        // A stat the table doesn't scale counts as it is.
        assertEquals(1, tables.multiplier(Stat.FEROCITY), 1e-9);
    }

    @Test
    void missingFolder(@TempDir Path empty) {
        StorageTables tables = StorageTables.load(empty.resolve("storage"));
        assertEquals(1, tables.problems().size());
        assertTrue(tables.powers().isEmpty());
        assertEquals(0, tables.accessoryPower(Rarity.LEGENDARY));
    }

    @Test
    void brokenFile(@TempDir Path folder) throws Exception {
        java.nio.file.Files.writeString(folder.resolve("powers.json"), "{\"accessory_power\": {\"NOT_A_RARITY\": 3}}");
        java.nio.file.Files.writeString(folder.resolve("bags.json"), "{\"bags\": {\"POTION_BAG\": {\"collection\": \"NETHER_STALK\", \"unlock\": 2,"
                + " \"base\": 9, \"slots\": {\"5\": 9}}}}");
        StorageTables tables = StorageTables.load(folder);
        // The broken one and the missing one are problems; the good one is read.
        assertEquals(2, tables.problems().size(), tables.problems().toString());
        assertEquals(18, tables.bag("POTION_BAG").at(5));
    }

    private static List<String> names(List<StorageTables.Power> powers) {
        return powers.stream().map(StorageTables.Power::name).toList();
    }
}
