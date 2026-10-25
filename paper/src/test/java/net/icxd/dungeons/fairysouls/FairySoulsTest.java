package net.icxd.dungeons.fairysouls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;

import net.icxd.dungeons.fairysouls.FairySoulTable.Soul;
import net.icxd.dungeons.menu.Icon;

/** The souls' table (NEU's file), what a profile keeps of them, and Tia's exchange. */
class FairySoulsTest {
    private static final Soul FIRST = new Soul(FairySoulTable.HUB, -233, 86, 84);
    private static final Soul SECOND = new Soul(FairySoulTable.HUB, 18, 68, -3);
    private static final Soul DUNGEON_HUB = new Soul(FairySoulTable.DUNGEON_HUB, 17, 124, -58);

    @Test
    void readsNeusFile() {
        FairySoulTable table = FairySoulTable.parse(JsonParser.parseString("""
                {"//": "a note", "Max Souls": 289, "hub": ["-233,86,84", " 18, 68, -3", "1,2", "a,b,c"], "dungeon_hub": ["17,124,-58"]}
                """).getAsJsonObject());
        assertEquals(List.of(FIRST, SECOND), table.on(FairySoulTable.HUB));
        assertEquals(List.of(DUNGEON_HUB), table.on(FairySoulTable.DUNGEON_HUB));
        assertEquals(List.of(), table.on("combat_1"));
        assertEquals(Map.of(FairySoulTable.HUB, 2, FairySoulTable.DUNGEON_HUB, 1).keySet(), table.islands().keySet());
        assertEquals(2, table.problems().size(), table.problems().toString());
        assertEquals("hub:-233,86,84", FIRST.id());
    }

    @Test
    void aMissingFileIsAProblem(@TempDir Path folder) {
        FairySoulTable table = FairySoulTable.load(folder);
        assertTrue(table.islands().isEmpty());
        assertEquals(1, table.problems().size());
    }

    /** The data checkout's, when it's there: as many as the recorded Fairy Souls Guide says each island has. */
    @Test
    void theDataCheckoutsMatchesTheRecordedGuide() {
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        Path folder = repository.resolveSibling("skyblock-dungeon-data").resolve("npcs");
        assumeTrue(Files.isRegularFile(folder.resolve(FairySoulTable.FILE)), "no " + folder.resolve(FairySoulTable.FILE));
        FairySoulTable table = FairySoulTable.load(folder);
        assertEquals(List.of(), table.problems());
        assertEquals(80, table.on(FairySoulTable.HUB).size());
        assertEquals(7, table.on(FairySoulTable.DUNGEON_HUB).size());
        // The guide's islands and the table's: the same counts (2026-09-27's guide); all but the Rift's and the miscellaneous.
        assertEquals(Map.of("safari", 4, "mining_2", 21, "winter", 5, "foraging_1", 12, "mining_3", 15, "hub", 80, "mining_1", 12,
                "dungeon_hub", 7, "crimson_isle", 29, "combat_3", 12), counts(table, "safari", "mining_2", "winter", "foraging_1",
                "mining_3", "hub", "mining_1", "dungeon_hub", "crimson_isle", "combat_3"));
        assertEquals(Map.of("lotus_atoll", 6, "farming_1", 20, "foraging_3", 12, "combat_1", 19, "fishing_1", 5, "foraging_2", 12),
                counts(table, "lotus_atoll", "farming_1", "foraging_3", "combat_1", "fishing_1", "foraging_2"));
        int all = table.islands().values().stream().mapToInt(List::size).sum();
        assertEquals(FairySouls.TOTAL - 17 - 1, all);
    }

    private static Map<String, Integer> counts(FairySoulTable table, String... islands) {
        Map<String, Integer> counts = new java.util.HashMap<>();
        for (String island : islands) counts.put(island, table.on(island).size());
        return counts;
    }

    @Test
    void eachSoulIsFoundOnce() {
        Document profile = new Document();
        assertEquals(0, FairySouls.found(profile));
        assertFalse(FairySouls.found(profile, FIRST));
        assertTrue(FairySouls.find(profile, FIRST));
        assertFalse(FairySouls.find(profile, FIRST));
        assertTrue(FairySouls.find(profile, DUNGEON_HUB));
        assertTrue(FairySouls.found(profile, FIRST));
        assertEquals(2, FairySouls.found(profile));
        assertEquals(1, FairySouls.found(profile, FairySoulTable.HUB));
        assertEquals(1, FairySouls.found(profile, FairySoulTable.DUNGEON_HUB));
        assertEquals(List.of("hub:-233,86,84", "dungeon_hub:17,124,-58"), profile.get(FairySouls.FIELD, Document.class).get("found"));
    }

    @Test
    void tiaTakesFiveAtATime() {
        Document profile = new Document();
        assertFalse(FairySouls.exchange(profile));
        for (int i = 0; i < 12; i++) FairySouls.find(profile, new Soul(FairySoulTable.HUB, i, 70, 0));
        assertEquals(12, FairySouls.unspent(profile));
        assertTrue(FairySouls.exchange(profile));
        assertTrue(FairySouls.exchange(profile));
        assertFalse(FairySouls.exchange(profile), "two left");
        assertEquals(2, FairySouls.exchanges(profile));
        assertEquals(2, FairySouls.unspent(profile));
        // Exchanged ones stay found.
        assertEquals(12, FairySouls.found(profile));
    }

    /** The wiki's exchanges #1, #2, #4 ... #44 unlock slots 2 to 18. */
    @Test
    void theBackpackSlotsExchangesUnlock() {
        assertEquals(2, FairySouls.backpackSlot(1));
        assertEquals(3, FairySouls.backpackSlot(2));
        assertEquals(0, FairySouls.backpackSlot(3));
        assertEquals(4, FairySouls.backpackSlot(4));
        assertEquals(8, FairySouls.backpackSlot(13));
        assertEquals(18, FairySouls.backpackSlot(44));
        assertEquals(0, FairySouls.backpackSlot(45));
    }

    @Test
    void yoinkedOnesAreOnNoIsland() {
        Document profile = new Document();
        FairySouls.find(profile, FIRST);
        FairySouls.yoinked(profile, 246, 49);
        assertEquals(246, FairySouls.found(profile));
        assertEquals(0, FairySouls.found(profile, FairySoulTable.HUB), "replaced, and the API doesn't say which");
        assertEquals(49, FairySouls.exchanges(profile));
        assertEquals(1, FairySouls.unspent(profile));
        // Found here again on top, but never more than there are; never more exchanges than the souls allow.
        FairySouls.yoinked(profile, 289, 60);
        assertEquals(57, FairySouls.exchanges(profile));
        FairySouls.find(profile, FIRST);
        assertEquals(FairySouls.TOTAL, FairySouls.found(profile));
    }

    /** Tia's exchange as the wiki's UI has it with none, and with enough. */
    @Test
    void tiasExchange() {
        Document profile = new Document();
        Icon none = TiaMenu.exchange(profile);
        assertEquals("&aExchange Fairy Souls", none.name());
        assertEquals(List.of("&7Find &dFairy Souls &7around the", "&7world and bring them back to me", "&7and I will reward you with",
                "&7SkyBlock XP and Backpack Slots!", "", "&7Fairy Souls: &e0&7/&d5", "", "&7Next Reward:", "&8+&b10 SkyBlock XP",
                "&6Backpack Slot #2", "", "&cYou don't have enough Fairy Souls!"), none.lore());
        assertEquals(FairySouls.HEAD, none.texture());
        for (int i = 0; i < 17; i++) FairySouls.find(profile, new Soul(FairySoulTable.HUB, i, 70, 0));
        FairySouls.exchange(profile);
        FairySouls.exchange(profile);
        // The third exchange unlocks no slot.
        Icon enough = TiaMenu.exchange(profile);
        assertEquals("&7Fairy Souls: &e7&7/&d5", enough.lore().get(5));
        assertEquals(List.of("&7Next Reward:", "&8+&b10 SkyBlock XP", "", "&eClick to exchange!"), enough.lore().subList(7, 11));
    }

    @Test
    void tiaSaysTheirName() {
        assertEquals("&e[NPC] &dTia the Fairy&f: Welcome to the &dFairy Pond&f, Tester! I am Tia.", Tia.firstTalk("Tester").getFirst());
        assertEquals(Tia.FIRST_TALK.subList(1, 4), Tia.firstTalk("Tester").subList(1, 4));
        assertEquals(Tia.FIRST_TALK.size(), Tia.LINE_TICKS.length);
    }

    /** Where a recorded stand stood (18.5 66.53125 -2.5, its spawn; 66.55 when sent again), its soul's head is: 18,68,-3. */
    @Test
    void theBlockAStandsSoulIsIn() {
        assertEquals("18,68,-3", SoulStands.spot(18.5, 66.53125, -2.5));
        assertEquals("18,68,-3", SoulStands.spot(18.5, 66.55, -2.5));
        assertEquals("-34,86,-35", SoulStands.spot(-33.5, 84.53125, -34.5));
    }
}
