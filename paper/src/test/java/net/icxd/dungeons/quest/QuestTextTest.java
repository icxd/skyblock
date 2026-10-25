package net.icxd.dungeons.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.collection.PrivateData;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.utils.SkyBlockTime;

/** Against the recordings (2026-10-24 and 10-25): the chat, the boss bar, the sidebar and the Quest Log as Hypixel wrote them. */
class QuestTextTest {
    @Test
    void theWelcome() {
        List<String> welcome = QuestText.welcome("ICoding", "&b");
        String rule = "&e&l" + "▬".repeat(64);
        assertEquals(List.of(rule,
                "&f                    &f&lWelcome to SkyBlock, &bICoding&f&l!",
                "",
                "&f           &eThis is your island! The SkyBlock universe has",
                "&f            &emany lands to discover, secrets to uncover,",
                "&f            &eand people to meet. Collect resources, craft",
                "&f              &eitems, and complete objectives to advance",
                "&f                       &eyour way through SkyBlock.",
                "",
                "&f                                   &eHave fun!",
                "",
                rule), welcome);
    }

    /** The centring that put "ICoding"'s welcome where it was puts Hypixel's fixed lines where they were too. */
    @Test
    void centredAsHypixelCentres() {
        assertEquals("&f           &eThis is your island! The SkyBlock universe has", QuestText.centered("&eThis is your island! The SkyBlock universe has"));
        assertEquals("&f              &eitems, and complete objectives to advance", QuestText.centered("&eitems, and complete objectives to advance"));
        assertEquals("&f                                   &eHave fun!", QuestText.centered("&eHave fun!"));
    }

    @Test
    void theBossBarAndSidebar() {
        Quest.Objective log = Quests.GETTING_STARTED.objective(Quests.BREAK_A_LOG);
        assertEquals("&fObjective: &eBreak a log", QuestText.bossBar(log, 0));
        assertEquals(List.of("&fObjective", "&eBreak a log"), QuestText.sidebar(log, 0));
    }

    /** 10-25: what an objective counts, on the bar (02:21.7, 03:30.8) and the sidebar, and how full the bar is. */
    @Test
    void countedObjectives() {
        Quest.Objective zombies = Quests.TIME_TO_STRIKE.objective(Quests.KILL_ZOMBIES);
        Quest.Objective combat = Quests.TIME_TO_STRIKE.objective(Quests.REACH_COMBAT_I);
        Quest.Objective dye = Quests.MARCO_THE_ARTIST.objective(Quests.GIVE_ROSE_RED);
        assertEquals("&fObjective: &eKill Zombies  &7(&e0&7/&a10&7)", QuestText.bossBar(zombies, 0));
        assertEquals("&fObjective: &eReach Combat Level I  &8(&b150&8/&350 XP&8)", QuestText.bossBar(combat, 150));
        assertEquals("&fObjective: &eGive Rose Red Dye to Marco", QuestText.bossBar(dye, 5));
        assertEquals(List.of("&fObjective", "&eKill Zombies", "  &7(&e0&7/&a10&7)"), QuestText.sidebar(zombies, 0));
        assertEquals(0.1f, QuestText.fill(zombies.goal(), 1));
        assertEquals(1f, QuestText.fill(combat.goal(), 150));
        assertEquals(1f, QuestText.fill(dye.goal(), 5));
        assertEquals(1f, QuestText.fill(null, 0));
    }

    /** 00:44.7: what Marco wants, under the objective, in its rarity's colour. */
    @Test
    void whatToBring() {
        PrivateData.load();
        try {
            assertEquals(List.of("&fObjective", "&eGive Rose Red Dye to Marco", "&f Rose Red &8x5"),
                    QuestText.sidebar(Quests.MARCO_THE_ARTIST.objective(Quests.GIVE_ROSE_RED), 5));
        } finally {
            PrivateData.unload();
        }
    }

    /** 04:17.9: Time to Strike in the Quest Log, its counts done (Combat's XP as it is now, 165). */
    @Test
    void theLogCountsThem() {
        Document profile = new Document();
        long started = SkyBlockTime.EPOCH + 520 * SkyBlockTime.YEAR_MS + 11 * SkyBlockTime.MONTH_MS + 21 * SkyBlockTime.DAY_MS
                + (17 * 60 + 16) * SkyBlockTime.DAY_MS / (24 * 60) + 1;
        Quests.start(profile, Quests.TIME_TO_STRIKE, started);
        for (int step = Quests.TALK_TO_THE_BARTENDER; step <= Quests.REACH_COMBAT_I; step++) Quests.complete(profile, Quests.TIME_TO_STRIKE, step, started);
        Skills.add(profile, Skill.COMBAT, 165);
        assertEquals(List.of("", " &a✔ &fTalk to the Bartender.", " &a✔ &fKill Zombies. (&b10&f/&b10&f)", " &a✔ &fTalk to the Bartender.",
                " &a✔ &fReach Combat Level I. (&b165&f/&b50&f)", " &c✖ &eTravel to the Spider's Den.", "", "&7Started:",
                "  &fWinter 22nd, Year 521", "  &75:16pm"), QuestText.logLore(profile, Quests.TIME_TO_STRIKE));
    }

    @Test
    void newObjectiveAndItsHover() {
        assertEquals(List.of("", "  &6&lNEW OBJECTIVE", "  &fCraft a workbench", ""),
                QuestText.newObjective(Quests.GETTING_STARTED.objective(Quests.CRAFT_A_WORKBENCH)));
        // 00:48.6: two done, the Wooden Pickaxe next.
        assertEquals(List.of("&eQuest: &fGetting Started", "", " &a✔ &fBreak a log.", " &a✔ &fCraft a workbench.",
                " &c✖ &eCraft a Wooden Pickaxe.", "", "&eClick to view your Quest Log!"), QuestText.hover(Quests.GETTING_STARTED, 2));
        // 03:18.5, Marco's.
        assertEquals(List.of("&eQuest: &fMarco the Artist", "", " &c✖ &eTalk to Marco.", "", "&eClick to view your Quest Log!"),
                QuestText.hover(Quests.MARCO_THE_ARTIST, 0));
    }

    /** The Quest Log's Getting Started at 00:18.0 (its start time as a SkyBlock time). */
    @Test
    void theLogsEntry() {
        Document profile = new Document();
        long started = SkyBlockTime.EPOCH + 520 * SkyBlockTime.YEAR_MS + 11 * SkyBlockTime.MONTH_MS + 16 * SkyBlockTime.DAY_MS
                + (3 * 60 + 8) * SkyBlockTime.DAY_MS / (24 * 60) + 1;
        Quests.start(profile, Quests.GETTING_STARTED, started);
        assertEquals(List.of("", " &c✖ &eBreak a log.", "", "&7Started:", "  &fWinter 17th, Year 521", "  &73:08am"),
                QuestText.logLore(profile, Quests.GETTING_STARTED));
    }
}
