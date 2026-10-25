package net.icxd.dungeons.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.fairysouls.FairySoulTable;
import net.icxd.dungeons.fairysouls.FairySouls;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.quest.QuestMenus.Layout;

/** The quest menus against the recordings (2026-10-24's new profile, 2026-09-27's old one for Completed Quests). */
class QuestMenusTest {
    /** When the recorded profile was made (2026-10-24). */
    private static final long NOW = 1_792_832_228_000L;

    private static Set<Integer> range(int from, int to) {
        Set<Integer> slots = new TreeSet<>();
        for (int i = from; i <= to; i++) slots.add(i);
        return slots;
    }

    @Test
    void questsAndChapters() {
        Layout menu = QuestMenus.questsAndChapters();
        assertEquals(List.of(4, 20, 24, 48, 49), List.copyOf(menu.icons().keySet()));
        Set<Integer> glass = range(0, 53);
        glass.removeAll(menu.icons().keySet());
        assertEquals(glass, menu.glass());
        Icon chapters = menu.icons().get(20);
        assertEquals(new Icon(Material.BOOK, "&bChapters", "&8/chapters", "", "&7View your progression through the", "&bChapters &7of each island!", "",
                "&eClick to view!"), chapters);
        assertEquals(List.of("&8/quests", "", "&7View your active quests, progress,", "&7and rewards.", "", "&eClick to view!"),
                menu.icons().get(24).lore());
        assertEquals("&7To SkyBlock Menu", menu.icons().get(48).lore().getFirst());
        assertEquals(10, menu.icons().get(4).lore().size());
    }

    @Test
    void chapters() {
        Layout menu = QuestMenus.chapters();
        assertEquals(List.of(4, 19, 21, 23, 25, 48, 49), List.copyOf(menu.icons().keySet()));
        assertEquals("&2Backwater Bayou", menu.icons().get(19).name());
        assertEquals(List.of("&cTalk to Helia in the Torrhus Canyon!"), menu.icons().get(25).lore().subList(11, 12));
        assertEquals("d9a143ba380c167f976fd5f42397feb6aa46b602d3331df7c99b49aaf4a3e6e3", menu.icons().get(25).texture());
        assertEquals("&7To Quests & Chapters", menu.icons().get(48).lore().getFirst());
    }

    /** At 00:18.2: the glass round the edge, the Fairy Souls then Getting Started, the journals, back and close, nothing at 50. */
    @Test
    void aNewProfilesQuestLog() {
        Document profile = new Document();
        QuestProgress.newProfile(profile, NOW);
        Layout log = QuestMenus.questLog(profile);
        assertEquals(List.of(4, 10, 11, 47, 48, 49), List.copyOf(log.icons().keySet()));
        assertEquals(Set.of(0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 50, 51, 52, 53), log.glass());
        Icon souls = log.icons().get(10);
        assertEquals("&eFind all Fairy Souls", souls.name());
        assertEquals(List.of("", " &c✖ &eFound: &70/&d289", "", "&7Forever ongoing quest...", "", "&eClick to view details!"), souls.lore());
        Icon started = log.icons().get(11);
        assertEquals(Material.PAPER, started.material());
        assertEquals("&eGetting Started", started.name());
        assertEquals(" &c✖ &eBreak a log.", started.lore().get(1));
        assertEquals(List.of("&8/quests", "", "&7View your active quests, progress,", "&7and rewards."), log.icons().get(4).lore());
    }

    /** The Quest Log's Fairy Souls: none found as recorded on a new profile, 246 as on the old one, all with a tick. */
    @Test
    void theFairySoulsFound() {
        Document profile = new Document();
        assertEquals(" &c✖ &eFound: &70/&d289", QuestMenus.fairySouls(profile).lore().get(1));
        FairySouls.yoinked(profile, 246, 0);
        assertEquals(" &c✖ &eFound: &d246&7/&d289", QuestMenus.fairySouls(profile).lore().get(1));
        FairySouls.yoinked(profile, 289, 0);
        assertEquals(" &a✔ &eFound: &d289&7/&d289", QuestMenus.fairySouls(profile).lore().get(1));
    }

    /** At 05:35.4 (2026-09-27): each island's head and count, glass everywhere else, back to the Quest Log. */
    @Test
    void theFairySoulsGuide() {
        Document profile = new Document();
        FairySouls.find(profile, new FairySoulTable.Soul(FairySoulTable.HUB, -233, 86, 84));
        FairySouls.find(profile, new FairySoulTable.Soul(FairySoulTable.DUNGEON_HUB, 17, 124, -58));
        Layout guide = QuestMenus.fairySoulsGuide(profile);
        Set<Integer> items = range(10, 16);
        items.addAll(range(19, 25));
        items.addAll(range(28, 31));
        items.addAll(Set.of(48, 49));
        assertEquals(items, new TreeSet<>(guide.icons().keySet()));
        Set<Integer> glass = range(0, 53);
        glass.removeAll(items);
        assertEquals(glass, guide.glass());
        Icon misc = guide.icons().get(10);
        assertEquals("&dMiscellaneous", misc.name());
        assertEquals(List.of("&7Fairy Souls: &e0&7/&d17", " &7Dungeons: &d9", " &7Fishing: &d4", " &7Garden: &d2", " &7Placeable: &d1",
                " &7Glacite Mineshafts: &d1"), misc.lore());
        assertEquals("41b830eb4082acec836bc835e40a11282bb51193315f91184337e8d3555583", misc.texture());
        assertEquals("&dHub", guide.icons().get(19).name());
        assertEquals(List.of("&7Fairy Souls: &e1&7/&d80"), guide.icons().get(19).lore());
        assertEquals(List.of("&7Fairy Souls: &e1&7/&d7"), guide.icons().get(21).lore());
        assertEquals(List.of("&7Fairy Souls: &e0&7/&d12"), guide.icons().get(31).lore());
        assertEquals("&dMoonglade Marsh", guide.icons().get(31).name());
        assertEquals("&7To Quest Log", guide.icons().get(48).lore().getFirst());
        int all = QuestMenus.GUIDE.stream().mapToInt(QuestMenus.GuideIsland::souls).sum();
        assertEquals(FairySouls.TOTAL, all);
    }

    @Test
    void completedQuestsShowOnceOneIsDone() {
        Document profile = new Document();
        QuestProgress.newProfile(profile, NOW);
        for (int step = 0; step <= Quests.USE_THE_TELEPORTER; step++) Quests.complete(profile, Quests.GETTING_STARTED, step, NOW + 1000);
        Layout log = QuestMenus.questLog(profile);
        assertEquals("&eMarco the Artist", log.icons().get(11).name());
        Icon completed = log.icons().get(50);
        assertEquals(new Icon(Material.BOOK, "&aCompleted Quests", "&7Take a peek at the past and", "&7browse quests you've", "&7already completed.", "",
                "&7Completed: &a1", "", "&eClick to view!"), completed);
        assertFalse(log.glass().contains(50));
        Layout done = QuestMenus.completedQuests(profile);
        assertEquals("&eGetting Started", done.icons().get(10).name());
        assertEquals(" &a✔ &fUse the teleporter.", done.icons().get(10).lore().get(5));
        assertNull(done.icons().get(11));
    }
}
