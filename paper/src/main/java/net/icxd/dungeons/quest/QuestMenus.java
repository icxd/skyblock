package net.icxd.dungeons.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.bson.Document;
import org.bukkit.Material;

import net.icxd.dungeons.menu.Icon;

/**
 * What the quest menus show, slot by slot, as data so they can be checked without a server; as
 * recorded on a new profile (2026-10-24: Quests & Chapters at 00:14.7, Chapters at 00:15.6, the Quest
 * Log at 00:18.2) and an old one (2026-09-27: the Quest Log with 21 quests and Completed Quests).
 */
public final class QuestMenus {
    public static final String QUESTS_AND_CHAPTERS = "Quests & Chapters";
    public static final String CHAPTERS = "Chapters";
    public static final String QUEST_LOG = "Quest Log";
    /** Not recorded: the button's name. */
    public static final String COMPLETED_QUESTS = "Completed Quests";

    static final int HEADER = 4;
    static final int CHAPTERS_SLOT = 20;
    static final int QUEST_LOG_SLOT = 24;
    static final int JOURNALS = 47;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int COMPLETED = 50;
    /** Where the Quest Log lists quests: the four middle rows, inside the glass. */
    static final int[] QUEST_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    /** Hypixel counts 289 Fairy Souls; finding them isn't here yet, so none are found. */
    static final int FAIRY_SOULS = 289;
    static final String FAIRY_SOUL_HEAD = "299ea120bd83d0c81a3c4627f5bce1b12fb03bcb57779c63dcc77e3f4ae8a793";

    /** A menu: its items by slot, and where its glass is. */
    public record Layout(Map<Integer, Icon> icons, Set<Integer> glass) {
    }

    private QuestMenus() {
    }

    static final List<String> ABOUT = List.of("&7Each island has its own series of", "&bChapters &7for you to complete!", "",
            "&7Complete tasks within a Chapter to", "&7earn small &6rewards&7, or complete", "&7entire Chapters to earn big ones!", "",
            "&7Some islands also have &aQuests &7for", "&7you to complete! Some items can only", "&7be obtained through Quests.");

    /** Quests & Chapters (the SkyBlock Menu's book): Chapters and the Quest Log. */
    public static Layout questsAndChapters() {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(HEADER, new Icon(Material.WRITABLE_BOOK, "&aQuests & Chapters", ABOUT));
        icons.put(CHAPTERS_SLOT, new Icon(Material.BOOK, "&bChapters", "&8/chapters", "", "&7View your progression through the",
                "&bChapters &7of each island!", "", "&eClick to view!"));
        icons.put(QUEST_LOG_SLOT, new Icon(Material.WRITABLE_BOOK, "&aQuest Log", "&8/quests", "", "&7View your active quests, progress,",
                "&7and rewards.", "", "&eClick to view!"));
        icons.put(BACK, back("&7To SkyBlock Menu"));
        icons.put(CLOSE, close());
        return new Layout(icons, everythingBut(icons.keySet()));
    }

    /** Chapters: the four islands' (Galatea's two and the fishing islands'), none started, each saying who starts it. */
    public static Layout chapters() {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(HEADER, new Icon(Material.BOOK, "&bChapters", "&8/chapters", "", "&7View your progression through the", "&bChapters &7of each island!"));
        icons.put(19, new Icon(Material.PLAYER_HEAD, "&2Backwater Bayou", List.of("&8/chapters bayou", "", "&7The &2Backwater Bayou &7is the first of",
                "&7the &bFishing Islands&7.", "", "&7Here you'll learn about fishing &2Junk&7,", "&7fishing in &dHotspots&7, and more!", "",
                "&cTalk to Jonesy on the Backwater", "&cBayou!"), "1c0cd33590f64d346d98cdd01606938742e715dda6737353306a44f81c8ba426"));
        icons.put(21, new Icon(Material.PLAYER_HEAD, "&3Lotus Atoll", List.of("&8/chapters lotus", "", "&2Researcher Betty &7is part of a team",
                "&7of researchers studying the", "&7various species of &2Trophy Frogs", "&7found throughout the &3Lotus Atoll&7.", "",
                "&7Progress through challenges on this", "&7island to gain &6rewards &7and ultimately", "&7earn a &6Golden Pearl&7!", "",
                "&cTalk to Researcher Betty on the", "&cLotus Atoll!"), "67bfd7f90468e5c6ac14ca14bfad982f90770d0a9efa1df9f66f91a05c81fef7"));
        icons.put(23, new Icon(Material.PLAYER_HEAD, "&2Moonglade Marsh", List.of("&8/chapters moonglade", "", "&7Welcome to the &2Moonglade Marsh&7, the",
                "&7first island of &2Galatea&7.", "", "&7Here you'll learn about &aHunting&7,", "&7advanced &aForaging&7, and maybe even",
                "&7discover the secrets of the island!", "", "&cTalk to Hina on Galatea!"), "a211ac81698c229d8ef2fae89f62a6a961b30d8b82b97161863090e90bff02a5"));
        icons.put(25, new Icon(Material.PLAYER_HEAD, "&6Torrhus Canyon", List.of("&8/chapters torrhus", "", "&6Torrhus Canyon &7is the second island",
                "&7of &2Galatea&7.", "", "&7Here you'll continue to hone your", "&aForaging &7and &aHunting &7skills as you",
                "&7partake in &bMiria's Contests&7, explore", "&7the &2Critter Safari&7, and dive deep into", "&bTorrhus Springs&7.", "",
                "&cTalk to Helia in the Torrhus Canyon!"), "d9a143ba380c167f976fd5f42397feb6aa46b602d3331df7c99b49aaf4a3e6e3"));
        icons.put(BACK, back("&7To Quests & Chapters"));
        icons.put(CLOSE, close());
        return new Layout(icons, everythingBut(icons.keySet()));
    }

    /**
     * The Quest Log: the Fairy Souls first, then the quests going, oldest first, in the middle four rows
     * (a page of 28; more than that isn't recorded). Completed Quests sits by Close once one is done.
     */
    public static Layout questLog(Document profile) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(HEADER, new Icon(Material.WRITABLE_BOOK, "&aQuest Log", "&8/quests", "", "&7View your active quests, progress,", "&7and rewards."));
        List<Icon> entries = new ArrayList<>();
        entries.add(fairySouls());
        for (Quest quest : Quests.active(profile)) entries.add(new Icon(quest.icon(), "&e" + quest.name(), QuestText.logLore(profile, quest)));
        list(icons, entries);
        icons.put(JOURNALS, new Icon(Material.WRITTEN_BOOK, "&aDungeon Journals", "&7Mysterious books written during the", "&7Ancient Era.", "",
                "&7View all the Dungeon Journals that", "&7you have found.", "", "&eClick to view!"));
        icons.put(BACK, back("&7To Quests & Chapters"));
        icons.put(CLOSE, close());
        int completed = Quests.completed(profile).size();
        if (completed > 0) {
            icons.put(COMPLETED, new Icon(Material.BOOK, "&aCompleted Quests", "&7Take a peek at the past and", "&7browse quests you've",
                    "&7already completed.", "", "&7Completed: &a" + completed, "", "&eClick to view!"));
        }
        return new Layout(icons, border(icons.keySet()));
    }

    /**
     * Completed Quests (UNKNOWN: never opened in a recording): the Quest Log's frame, each quest done
     * as the log writes one, every objective ticked.
     */
    public static Layout completedQuests(Document profile) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(HEADER, new Icon(Material.BOOK, "&aCompleted Quests", "&7Take a peek at the past and", "&7browse quests you've", "&7already completed."));
        List<Icon> entries = new ArrayList<>();
        for (Quest quest : Quests.completed(profile)) entries.add(new Icon(quest.icon(), "&e" + quest.name(), QuestText.logLore(profile, quest)));
        list(icons, entries);
        icons.put(BACK, back("&7To Quest Log"));
        icons.put(CLOSE, close());
        return new Layout(icons, border(icons.keySet()));
    }

    /** "&eFind all Fairy Souls": " &c✖ &eFound: &70/&d289", as recorded with none found. */
    static Icon fairySouls() {
        return new Icon(Material.PLAYER_HEAD, "&eFind all Fairy Souls", List.of("", " &c✖ &eFound: &70/&d" + FAIRY_SOULS, "",
                "&7Forever ongoing quest...", "", "&eClick to view details!"), FAIRY_SOUL_HEAD);
    }

    private static void list(Map<Integer, Icon> icons, List<Icon> entries) {
        for (int i = 0; i < entries.size() && i < QUEST_SLOTS.length; i++) icons.put(QUEST_SLOTS[i], entries.get(i));
    }

    static Icon back(String to) {
        return new Icon(Material.ARROW, "&aGo Back", to);
    }

    static Icon close() {
        return new Icon(Material.BARRIER, "&cClose");
    }

    /** Glass everywhere but these slots (the menus where it fills the gaps). */
    private static Set<Integer> everythingBut(Set<Integer> used) {
        Set<Integer> glass = new TreeSet<>();
        for (int slot = 0; slot < 54; slot++) if (!used.contains(slot)) glass.add(slot);
        return glass;
    }

    /** Glass round the edge only, the middle left empty for the quests (the recorded Quest Log). */
    private static Set<Integer> border(Set<Integer> used) {
        Set<Integer> glass = new TreeSet<>();
        for (int slot = 0; slot < 54; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if ((row == 0 || row == 5 || column == 0 || column == 8) && !used.contains(slot)) glass.add(slot);
        }
        return glass;
    }
}
