package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonParser;

/**
 * What the pet tests count with: a small made-up data file (the pets are Hypixel's, their numbers partly made up, the
 * XP table short), and where the private data is (-Dpets.dir, the folder with pets.json; else the data checkout next
 * to this repository).
 */
final class PetFixtures {
    /** Levels to 5 (the Golden Dragon's to 9, its last two its own), the table's offsets 0 to 4. */
    static final String DATA = """
            {"format": 1, "source": {"recording": "test"},
             "levels": [100, 110, 120, 130, 145, 160, 175, 190, 210, 230],
             "rarity_offset": {"COMMON": 0, "UNCOMMON": 1, "RARE": 2, "EPIC": 3, "LEGENDARY": 4, "MYTHIC": 4},
             "max_level": 5,
             "score": {"rarity": {"COMMON": 1, "UNCOMMON": 2, "RARE": 3, "EPIC": 4, "LEGENDARY": 5, "MYTHIC": 6}, "max_level": 1,
                       "rewards": [[1, 1], [5, 2], [10, 3]]},
             "pets": {
              "BEE": {"name": "Bee", "kind": "Farming Pet", "skill": "FARMING", "rarities": {
               "COMMON": {"texture": "bee0", "stats": {"STRENGTH": {"base": 5, "per_level": 0.25}, "INTELLIGENCE": {"base": 0, "per_level": 0.5}},
                          "numbers": [{"base": 1, "per_level": 0.02, "decimals": 2}],
                          "blocks": [{"name": "&6Hive", "text": ["&7For each player within &a25 &7blocks:", " &b+{0}✎ Intelligence", "&8Max 15 players"]}],
                          "kat": true, "kat_upgrade": {"seconds": 60, "coins": 0, "items": [["SMALL_HONEY_DIPPER", 1]]}},
               "UNCOMMON": {"texture": "bee1", "george": 250, "stats": {"STRENGTH": {"base": 5, "per_level": 0.25}},
                            "numbers": [{"base": 1, "per_level": 0.05, "decimals": 2}],
                            "blocks": [{"name": "&6Hive", "text": ["&7For each player within &a25 &7blocks:", " &b+{0}✎ Intelligence"]}]}}},
              "GUARDIAN": {"name": "Guardian", "kind": "Enchanting Pet", "skill": "ENCHANTING", "rarities": {
               "MYTHIC": {"texture": "guardian5", "stats": {"DEFENSE": {"base": 0, "per_level": 0.5}},
                          "numbers": [{"base": 0, "per_level": 0.07, "decimals": 1}],
                          "blocks": [{"name": "&6Lucky Seven", "text": ["&7Gain &b+{0}% &7chance to find &5ultra rare books in &dSuperpairs&7."]}]}}},
              "GOLDEN_DRAGON": {"name": "Golden Dragon", "kind": "Combat Pet", "skill": "COMBAT", "max_level": 9, "extra_levels": [1000, 2000],
                                "stats_from": 6, "rarities": {
               "LEGENDARY": {"texture": "gd4", "stats": {"STRENGTH": {"base": 0, "per_level": 0.25}}, "numbers": [],
                             "blocks": [{"name": "&6Gold's Power", "text": ["&7Gold."]}]}}},
              "REINDEER": {"name": "Reindeer", "kind": "Fishing Pet", "skill": "FISHING", "xp_multiplier": 2, "rarities": {
               "LEGENDARY": {"texture": "reindeer4", "stats": {}, "numbers": [], "blocks": []}}},
              "BINGO": {"name": "Bingo", "kind": "All Skills", "skill": "ALL", "rarity_offset": {"COMMON": 0, "UNCOMMON": 0}, "rarities": {
               "COMMON": {"texture": "bingo0", "stats": {}, "numbers": [], "blocks": []},
               "UNCOMMON": {"texture": "bingo1", "stats": {}, "numbers": [], "blocks": []}}},
              "DROPLET_WISP": {"name": "Drop Wisp", "kind": "Gabagool Pet, feed to gain XP", "skill": null, "rarities": {
               "UNCOMMON": {"texture": "wisp1", "stats": {}, "numbers": [], "blocks": []}}}}}
            """;

    private PetFixtures() {
    }

    static PetData fixture() {
        PetData data = PetData.parse(JsonParser.parseString(DATA).getAsJsonObject());
        assertEquals(List.of(), data.problems());
        return data;
    }

    /** The private data's plugin data folder (the one with pets/): -Dpets.dir's parent, else the data checkout. */
    static Path dataFolder() {
        String property = System.getProperty("pets.dir");
        if (property != null) return Path.of(property).toAbsolutePath().getParent();
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        return repository.resolveSibling("skyblock-dungeon-data");
    }
}
