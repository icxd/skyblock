package net.icxd.dungeons.museum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonParser;

/**
 * What the Museum tests count with: a small made-up data file (its entries are Hypixel's, the numbers partly made up),
 * and the private data where it's there (-Dmuseum.dir, the folder with museum.json; else the data checkout next to
 * this repository).
 */
final class MuseumFixtures {
    /** Combat: belts three tiers deep, a sword with a higher tier, two sets sharing pieces. Mining: one item. */
    static final String DATA = """
            {"format": 1, "source": {"api": 1, "recording": "test"},
             "categories": [
              {"id": "COMBAT", "name": "Combat", "entries": [
               {"id": "DOJO_WHITE_BELT", "name": "White Belt", "xp": 1, "items": ["DOJO_WHITE_BELT"], "parent": "DOJO_YELLOW_BELT",
                "parent_name": "&eYellow Belt", "parent_lines": ["&7Donating a &eYellow Belt &7will count as", "&7donating this item."]},
               {"id": "UNDEAD_SWORD", "name": "Undead Sword", "xp": 1, "items": ["UNDEAD_SWORD"], "parent": "REVENANT_SWORD",
                "parent_name": "&9Revenant Falchion", "parent_lines": ["&7Donating a &9Revenant Falchion &7will count", "&7as donating this item."]},
               {"id": "CELESTE", "set": true, "name": "Celeste Armor", "xp": 1,
                "pieces": [["CELESTE_BOOTS"], ["CELESTE_CHESTPLATE"], ["CELESTE_HELMET"], ["CELESTE_LEGGINGS"]]},
               {"id": "DOJO_YELLOW_BELT", "name": "Yellow Belt", "xp": 2, "items": ["DOJO_YELLOW_BELT"], "parent": "DOJO_GREEN_BELT",
                "parent_name": "&9Green Belt", "parent_lines": ["&7Donating a &9Green Belt &7will count as", "&7donating this item."]},
               {"id": "DOJO_GREEN_BELT", "name": "Green Belt", "xp": 3, "items": ["DOJO_GREEN_BELT"]},
               {"id": "REVENANT_SWORD", "name": "Revenant Falchion", "xp": 4, "items": ["REVENANT_SWORD", "STARRED_REVENANT_SWORD"]},
               {"id": "MONSTER_RAIDER", "set": true, "name": "Monster Raider Armor", "xp": 4,
                "pieces": [["CREEPER_LEGGINGS"], ["GUARDIAN_CHESTPLATE"], ["SKELETON_HELMET"], ["SPIDER_BOOTS", "TARANTULA_BOOTS"]]},
               {"id": "MONSTER_HUNTER", "set": true, "name": "Monster Hunter Armor", "xp": 5,
                "pieces": [["CREEPER_LEGGINGS"], ["SKELETON_HELMET"], ["SPIDER_BOOTS"]]}]},
              {"id": "MINING", "name": "Mining", "entries": [
               {"id": "FLINT_SHOVEL", "name": "Flint Shovel", "xp": 1, "items": ["FLINT_SHOVEL"]}]}],
             "special": ["CAKE_SOUL"],
             "milestones": [
              {"milestone": 1, "xp": 100, "rewards": ["&8+&b1% Bits Multiplier", "&8+&62% Bank Interest"]},
              {"milestone": 2, "xp": 200, "rewards": ["&8+&b1% Bits Multiplier", "&8+&62% Bank Interest"]},
              {"milestone": 3, "xp": 300, "rewards": ["&8+&dDeluxe Bank Upgrade", "&8+&fMuseum Appraisal Service", "&8+&b1% Bits Multiplier",
                                                     "&8+&62% Bank Interest"]}]}
            """;

    private MuseumFixtures() {
    }

    static MuseumData fixture() {
        MuseumData data = MuseumData.parse(JsonParser.parseString(DATA).getAsJsonObject());
        assertEquals(List.of(), data.problems());
        return data;
    }

    /** The private data's plugin data folder (the one with museum/): -Dmuseum.dir's parent, else the data checkout. */
    static Path dataFolder() {
        String property = System.getProperty("museum.dir");
        if (property != null) return Path.of(property).toAbsolutePath().getParent();
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        return repository.resolveSibling("skyblock-dungeon-data");
    }

    /** The private data, or the test is skipped without it. */
    static MuseumData recorded() {
        Path file = dataFolder().resolve(MuseumData.FOLDER).resolve(MuseumData.FILE);
        assumeTrue(Files.isRegularFile(file), "no " + file);
        MuseumData data = MuseumData.load(dataFolder());
        assertEquals(List.of(), data.problems());
        return data;
    }
}
