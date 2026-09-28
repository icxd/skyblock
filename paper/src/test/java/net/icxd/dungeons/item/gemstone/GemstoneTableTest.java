package net.icxd.dungeons.item.gemstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;

/** Reading the gemstone table: a made-up one, and the private one against what live items and recordings show. */
class GemstoneTableTest {
    @Test
    void readsAMadeUpTable() {
        List<String> problems = new ArrayList<>();
        GemstoneTable table = GemstoneTable.read(JsonParser.parseString("""
                {"format": 1,
                 "gems": {"JASPER": {"stat": "STRENGTH", "values": {"FINE": {"RARE": 4, "MYTHIC": 7, "SUPREME": 9}}},
                          "COMBAT": {"stat": "STRENGTH", "values": {}},
                          "RUBY": {"stat": "NOT_A_STAT", "values": {}}},
                 "removal_costs": {"ROUGH": 2, "FLAWED": 20, "FINE": 200, "FLAWLESS": 2000},
                 "chisel_percentages": {"FINE": 7},
                 "chisel_perks": {"PERIDOT": "§7Gain §a+{}%"},
                 "armor_sets": {"DIVAN": {"name": "Divan Armor", "pieces": ["DIVAN_HELMET", "DIVAN_BOOTS"]}}}"""), problems);
        assertNotNull(table);
        assertEquals(Stat.STRENGTH, table.stat(GemstoneType.JASPER));
        assertEquals(7, table.value(new Gem(GemstoneType.JASPER, GemstoneQuality.FINE), Rarity.MYTHIC));
        assertNull(table.stat(GemstoneType.RUBY));
        assertEquals(200, table.removalCost(GemstoneQuality.FINE));
        assertEquals(7, table.chiselPercentages().get(GemstoneQuality.FINE));
        assertEquals(List.of("DIVAN_HELMET", "DIVAN_BOOTS"), table.armorSets().get("DIVAN").pieces());
        // What it doesn't know: a rarity, a slot type as a gem, a stat, and Perfect's fee.
        assertEquals(4, problems.size(), problems.toString());
        assertNull(GemstoneTable.read(JsonParser.parseString("{\"format\": 2}"), problems));
        assertNull(GemstoneTable.read(null, problems));
    }

    @Test
    void thePrivateTable() {
        GemstoneTable table = PrivateGemstones.table();
        assertEquals(12, table.gems().size());
        // The recordings (grinder report 7): Fine Jasper M 7, Flawless Jasper M 12, Fine Sapphire E 10, Fine Peridot M 6.
        assertEquals(7, value(table, "FINE_JASPER_GEM", Rarity.MYTHIC));
        assertEquals(12, value(table, "FLAWLESS_JASPER_GEM", Rarity.MYTHIC));
        assertEquals(10, value(table, "FINE_SAPPHIRE_GEM", Rarity.EPIC));
        assertEquals(6, value(table, "FINE_PERIDOT_GEM", Rarity.MYTHIC));
        // Live Citrine, twice NEU's: Fine R6 E8 L10, Flawless R8 E10 L12, Perfect R10 E12 L16, Rough L5.
        assertEquals(List.of(6.0, 8.0, 10.0), values(table, "FINE_CITRINE_GEM"));
        assertEquals(List.of(8.0, 10.0, 12.0), values(table, "FLAWLESS_CITRINE_GEM"));
        assertEquals(List.of(10.0, 12.0, 16.0), values(table, "PERFECT_CITRINE_GEM"));
        assertEquals(5, value(table, "ROUGH_CITRINE_GEM", Rarity.LEGENDARY));
        // Live Flawed Sapphire at Legendary (the wiki's 10 is wrong), and Amber's Divine ("SUPREME" on old items).
        assertEquals(8, value(table, "FLAWED_SAPPHIRE_GEM", Rarity.LEGENDARY));
        assertEquals(54, value(table, "FINE_AMBER_GEM", Rarity.DIVINE));
        assertEquals(Stat.FORAGING_FORTUNE, table.stat(GemstoneType.CITRINE));
        assertEquals(Stat.CRIT_DAMAGE, table.stat(GemstoneType.ONYX));
        // A Fine gem's fee as the Ring of Power screenshot shows it ("Cost to remove - 10.0k"); every quality has one.
        assertEquals(10000, table.removalCost(GemstoneQuality.FINE));
        assertEquals(GemstoneQuality.values().length, table.removalCosts().size());
        assertEquals(GemstoneQuality.values().length, table.chiselPercentages().size());
        assertTrue(table.armorSets().get("DIVAN").pieces().contains("DIVAN_HELMET"));
    }

    private static double value(GemstoneTable table, String id, Rarity rarity) {
        return table.value(Gem.of(id), rarity);
    }

    private static List<Double> values(GemstoneTable table, String id) {
        return List.of(value(table, id, Rarity.RARE), value(table, id, Rarity.EPIC), value(table, id, Rarity.LEGENDARY));
    }
}
