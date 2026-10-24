package net.icxd.dungeons.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.utils.SkyBlockTime;

/** Against the recording (2026-10-24): the chat, the boss bar, the sidebar and the Quest Log as Hypixel wrote them. */
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
        assertEquals("&fObjective: &eBreak a log", QuestText.bossBar(log));
        assertEquals(List.of("&fObjective", "&eBreak a log"), QuestText.sidebar(log));
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
