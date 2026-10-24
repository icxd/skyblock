package net.icxd.dungeons.quest;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import net.icxd.dungeons.quest.Quest.Objective;
import net.icxd.dungeons.utils.SkyBlockTime;
import net.icxd.dungeons.utils.Text;

/** How quests are written: the boss bar, the sidebar, the chat and the Quest Log, as recorded (2026-10-24). */
public final class QuestText {
    /** The rule above and below the welcome, 64 bold yellow bars. */
    static final String RULE = "&e&l" + "▬".repeat(64);
    /** Where Hypixel's centred chat lines are centred (see {@link #centered}). */
    private static final int CHAT_MIDDLE = 160;

    private QuestText() {
    }

    /** "&fObjective: &eBreak a log". */
    public static String bossBar(Objective objective) {
        return "&fObjective: &e" + objective.text();
    }

    /** The sidebar's two lines, between the purse and the address: "&fObjective", then the objective. */
    public static List<String> sidebar(Objective objective) {
        return List.of("&fObjective", "&e" + objective.text());
    }

    /**
     * The quest as far as it's got: "&eQuest: &fGetting Started", a blank, " &a✔ &fBreak a log." for each
     * done, " &c✖ &eCraft a workbench." for the one they're at, a blank, and the click line. The
     * chat's lines carry it as their hover (recorded on every linked line).
     */
    static List<String> hover(Quest quest, int done) {
        List<String> lines = new ArrayList<>();
        lines.add("&eQuest: &f" + quest.name());
        lines.add("");
        lines.addAll(progress(quest, done));
        lines.add("");
        lines.add("&eClick to view your Quest Log!");
        return lines;
    }

    /** " &a✔ &fBreak a log." for each objective done, then " &c✖ &eCraft a workbench." for the one they're at (none once all are done). */
    static List<String> progress(Quest quest, int done) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(done + 1, quest.objectives().size()); i++) {
            String text = quest.objective(i).logText();
            lines.add(i < done ? " &a✔ &f" + text : " &c✖ &e" + text);
        }
        return lines;
    }

    /**
     * NEW OBJECTIVE, as it came two seconds after each step (blank, "  &6&lNEW OBJECTIVE", "  &f" and the
     * objective, blank). The two middle lines take you to the Quest Log, with the quest's progress as
     * their hover, for objectives that are linked.
     */
    static List<String> newObjective(Objective objective) {
        return List.of("", "  &6&lNEW OBJECTIVE", "  &f" + objective.text(), "");
    }

    /**
     * The Quest Log's item lore for a quest going: a blank, its progress, a blank, and when it started
     * ("&7Started:", "  &fWinter 17th, Year 521", "  &73:08am").
     */
    public static List<String> logLore(Document profile, Quest quest) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(progress(quest, Quests.done(profile, quest)));
        lore.add("");
        lore.add("&7Started:");
        SkyBlockTime started = SkyBlockTime.at(Quests.startedAt(profile, quest));
        lore.add("  &f" + started.seasonDate());
        lore.add("  &7" + started.exactClock());
        return lore;
    }

    /**
     * The welcome on a new profile's island, as recorded on arriving there: rules of bars around
     * "Welcome to SkyBlock, NAME!" (the name in their rank's colour) and Hypixel's words, centred as
     * Hypixel centred them.
     */
    public static List<String> welcome(String name, String rankColor) {
        return List.of(
                RULE,
                centered("&f&lWelcome to SkyBlock, " + rankColor + name + "&f&l!"),
                "",
                "&f           &eThis is your island! The SkyBlock universe has",
                "&f            &emany lands to discover, secrets to uncover,",
                "&f            &eand people to meet. Collect resources, craft",
                "&f              &eitems, and complete objectives to advance",
                "&f                       &eyour way through SkyBlock.",
                "",
                "&f                                   &eHave fun!",
                "",
                RULE);
    }

    /** Spaces in front so it sits in the middle of the chat, as Hypixel centres it (the recorded welcome's 20 for "ICoding"). */
    static String centered(String text) {
        int spaces = (int) Math.ceil((CHAT_MIDDLE - Text.width(text) / 2.0) / 4.0);
        return "&f" + " ".repeat(Math.max(0, spaces)) + text;
    }
}
