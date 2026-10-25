package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stat;

/** Reading pets.json, and the XP each level takes: the table from a rarity's offset, the pet's own past it. */
class PetDataTest {
    private static final PetData DATA = PetFixtures.fixture();

    @Test
    void read() {
        assertEquals(6, DATA.count());
        Pet bee = DATA.pet("bee");
        assertEquals(List.of("Bee", "Farming Pet", Skill.FARMING), List.of(bee.name(), bee.kind(), bee.skill()));
        assertEquals(0.25, bee.variant(Rarity.COMMON).stats().get(Stat.STRENGTH).perLevel());
        assertTrue(bee.variant(Rarity.COMMON).kat());
        assertFalse(bee.variant(Rarity.UNCOMMON).kat());
        assertTrue(DATA.pet("BINGO").allSkills());
        assertNull(DATA.pet("DROPLET_WISP").skill(), "fed, not levelled by a skill");
        assertEquals(2, DATA.pet("REINDEER").xpMultiplier());
        assertEquals(List.of(new PetData.Reward(1, 1), new PetData.Reward(5, 2), new PetData.Reward(10, 3)), DATA.scoreRewards());
        // A rarity it doesn't have: the highest it has below.
        assertEquals(Rarity.UNCOMMON, bee.variant(Rarity.LEGENDARY).rarity());
    }

    @Test
    void levels() {
        Pet bee = DATA.pet("BEE");
        // Common starts the table at its first, Uncommon at its second.
        assertEquals(100, DATA.xpFor(bee, Rarity.COMMON, 2));
        assertEquals(110, DATA.xpFor(bee, Rarity.UNCOMMON, 2));
        assertEquals(0, DATA.xpFor(bee, Rarity.COMMON, 1));
        assertEquals(0, DATA.xpFor(bee, Rarity.COMMON, 6), "past its most");
        assertEquals(100 + 110 + 120 + 130, DATA.totalXp(bee, Rarity.COMMON, 5));
        assertEquals(1, DATA.level(bee, Rarity.COMMON, 99.9));
        assertEquals(2, DATA.level(bee, Rarity.COMMON, 100));
        assertEquals(5, DATA.level(bee, Rarity.COMMON, 1_000_000), "its most, XP past it kept");
        assertEquals(10, DATA.xpIntoLevel(bee, Rarity.COMMON, 110), 1e-9);
        assertEquals(0, DATA.xpIntoLevel(bee, Rarity.COMMON, 1_000_000), 1e-9);
    }

    @Test
    void ownLevels() {
        Pet dragon = DATA.pet("GOLDEN_DRAGON");
        assertEquals(9, DATA.maxLevel(dragon));
        // Legendary's offset is 4: levels 2 to 7 are the table's last six, 8 and 9 its own.
        assertEquals(230, DATA.xpFor(dragon, Rarity.LEGENDARY, 7));
        assertEquals(1000, DATA.xpFor(dragon, Rarity.LEGENDARY, 8));
        assertEquals(2000, DATA.xpFor(dragon, Rarity.LEGENDARY, 9));
        // The Bingo pet's every rarity starts at the table's first.
        assertEquals(100, DATA.xpFor(DATA.pet("BINGO"), Rarity.UNCOMMON, 2));
    }

    @Test
    void notOurs() {
        PetData data = PetData.parse(JsonParser.parseString("""
                {"format": 1, "levels": [100], "rarity_offset": {"COMMON": 0}, "max_level": 2, "score": {},
                 "pets": {"ORCHID_MANTIS": {"name": "Orchid Mantis", "kind": "Foraging Pet", "skill": "FORAGING", "rarities": {
                  "COMMON": {"texture": "x", "stats": {"OVERBLOOM": {"base": 0, "per_level": 1}}, "numbers": [], "blocks": []},
                  "SUPREME": {"texture": "y", "stats": {}, "numbers": [], "blocks": []}}}}}
                """).getAsJsonObject());
        assertEquals(List.of("ORCHID_MANTIS COMMON: stat OVERBLOOM isn't one of the plugin's (left out)",
                "ORCHID_MANTIS: rarity SUPREME isn't one of the plugin's (left out)"), data.problems());
        assertEquals(1, data.pet("ORCHID_MANTIS").variants().size());
        assertEquals(List.of("format 2, not 1"), PetData.parse(JsonParser.parseString("{\"format\": 2}").getAsJsonObject()).problems());
    }

    /** The private data's tables give the totals Hypixel's pets take (the owner's numbers, and the wiki's for the dragons). */
    @Test
    void privateTotals() {
        assumeTrue(Files.isRegularFile(PetFixtures.dataFolder().resolve("pets/pets.json")), "no private pets.json");
        PetData data = PetData.load(PetFixtures.dataFolder());
        assertTrue(data.count() > 80);
        Pet bee = data.pet("BEE");
        assertEquals(5_624_785, data.totalXp(bee, Rarity.COMMON, 100));
        assertEquals(8_644_220, data.totalXp(bee, Rarity.UNCOMMON, 100));
        assertEquals(12_626_665, data.totalXp(bee, Rarity.RARE, 100));
        assertEquals(18_608_500, data.totalXp(bee, Rarity.EPIC, 100));
        assertEquals(25_353_230, data.totalXp(bee, Rarity.LEGENDARY, 100));
        assertEquals(25_353_230, data.totalXp(bee, Rarity.MYTHIC, 100));
        Pet dragon = data.pet("GOLDEN_DRAGON");
        assertEquals(200, data.maxLevel(dragon));
        assertEquals(214_023_230, data.totalXp(dragon, Rarity.LEGENDARY, 200));
    }
}
