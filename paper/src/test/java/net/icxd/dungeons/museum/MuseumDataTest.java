package net.icxd.dungeons.museum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;

import net.icxd.dungeons.museum.MuseumData.Category;
import net.icxd.dungeons.museum.MuseumData.Entry;

/** museum.json (format 1): reading it, what it says is wrong, and the recorded data. */
class MuseumDataTest {
    @Test
    void readsEntriesSetsAndMilestones() {
        MuseumData data = MuseumFixtures.fixture();
        assertEquals(List.of("COMBAT", "MINING"), data.categories().stream().map(Category::id).toList());
        assertEquals(9, data.count());
        assertEquals(22, data.totalXp());
        Entry belt = data.entry("DOJO_WHITE_BELT");
        assertEquals("DOJO_YELLOW_BELT", belt.parent());
        assertEquals(List.of("&7Donating a &eYellow Belt &7will count as", "&7donating this item."), belt.parentLines());
        assertEquals(List.of(List.of("DOJO_WHITE_BELT")), belt.pieces());
        assertEquals("COMBAT", belt.category());
        Entry raider = data.entry("MONSTER_RAIDER");
        assertTrue(raider.set());
        assertEquals(List.of("SPIDER_BOOTS", "TARANTULA_BOOTS"), raider.pieces().get(3));
        // A piece in two sets is both's, in order; a starred item is its own entry's.
        assertEquals(List.of("MONSTER_RAIDER", "MONSTER_HUNTER"), data.entriesOf("SKELETON_HELMET").stream().map(Entry::id).toList());
        assertEquals(List.of(data.entry("REVENANT_SWORD")), data.entriesOf("STARRED_REVENANT_SWORD"));
        assertEquals(List.of(), data.entriesOf("DIRT"));
        assertEquals(List.of(), data.entriesOf(null));
        assertEquals(List.of("CAKE_SOUL"), data.special());
        assertEquals(3, data.milestones().size());
        assertEquals(List.of("&8+&dDeluxe Bank Upgrade", "&8+&fMuseum Appraisal Service", "&8+&b1% Bits Multiplier", "&8+&62% Bank Interest"),
                data.milestones().get(2).rewards());
    }

    @Test
    void saysWhatsWrong() {
        MuseumData data = MuseumData.parse(JsonParser.parseString("""
                {"format": 1, "categories": [{"id": "COMBAT", "name": "Combat", "entries": [
                  {"id": "A", "name": "A", "xp": 1, "items": ["A"], "parent": "NOWHERE", "parent_lines": ["x"]},
                  {"id": "A", "name": "A again", "xp": 1, "items": ["A"]},
                  {"id": "B", "name": "B", "xp": 1, "items": []},
                  {"id": "C", "name": "C", "xp": 2, "set": true, "pieces": [["C_HELMET"]]}]}]}
                """).getAsJsonObject());
        assertEquals(List.of("A", "C"), data.categories().getFirst().entries().stream().map(Entry::id).toList());
        assertNull(data.entry("A").parent(), "a higher tier that isn't an entry is left off");
        assertEquals(List.of(), data.entry("A").parentLines());
        assertEquals(3, data.problems().size(), data.problems().toString());
        assertEquals(List.of("format 2, not 1"), MuseumData.parse(JsonParser.parseString("{\"format\": 2}").getAsJsonObject()).problems());
    }

    @Test
    void withoutTheFile(@TempDir Path folder) {
        MuseumData data = MuseumData.load(folder);
        assertEquals(0, data.count());
        assertEquals(1, data.problems().size());
    }

    /** The data checkout's, from the recording (2026-10-25): what Your Museum and its pages said. */
    @Test
    void asRecorded() {
        MuseumData data = MuseumFixtures.recorded();
        assertEquals(636, data.count());
        assertEquals(3646, data.totalXp());
        assertEquals(List.of("COMBAT", "FARMING", "MINING", "FISHING", "FORAGING", "DUNGEONEERING", "HUNTING"),
                data.categories().stream().map(Category::id).toList());
        assertEquals(List.of(245, 101, 74, 66, 27, 98, 25), data.categories().stream().map(c -> c.entries().size()).toList());
        // Their pages: "(1/9) Museum ➜ Combat", "(1/4) ... Farming", 3, 3, one for Foraging, 4, one for Hunting.
        assertEquals(List.of(9, 4, 3, 3, 1, 4, 1), data.categories().stream().map(c -> MuseumCategoryMenu.pages(c.entries().size())).toList());
        List<Entry> combat = data.category("COMBAT").entries();
        assertEquals(List.of("Rogue Sword", "White Belt", "Creeper Hat", "Skeleton Hat", "Fancy Sword", "Undead Sword", "Spider Sword"),
                combat.subList(0, 7).stream().map(Entry::name).toList());
        assertEquals(List.of("&7Donating a &eYellow Belt &7will count as", "&7donating this item."), combat.get(1).parentLines());
        assertTrue(data.category("COMBAT").entries().get(14).set(), "Celeste Armor");
        assertEquals(40, data.milestones().size());
        assertEquals(List.of("&8+&cDust the Fish", "&8+&cPremier Bank Upgrade", "&8+&9Shen's Auction", "&8+&9Flower Minion XII &7Recipe",
                "&8+&b1% Bits Multiplier", "&8+&62% Bank Interest"), data.milestones().get(9).rewards());
        for (int i = 0; i < 40; i++) assertEquals(100 * (i + 1), data.milestones().get(i).xp());
    }
}
