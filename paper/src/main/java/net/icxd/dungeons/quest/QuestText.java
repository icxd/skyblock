package net.icxd.dungeons.quest;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.quest.Quest.Objective;
import net.icxd.dungeons.skill.Skills;
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

    /** "&fObjective: &eBreak a log", "&fObjective: &eKill Zombies  &7(&e0&7/&a10&7)": with what it has counted, as recorded. */
    public static String bossBar(Objective objective, double progress) {
        String counted = counted(objective.goal(), progress);
        return "&fObjective: &e" + objective.text() + (counted.isEmpty() ? "" : "  " + counted);
    }

    /**
     * The sidebar's lines, between the purse and the address: "&fObjective", the objective, and under
     * it what it has counted ("  &7(&e0&7/&a10&7)") or what to bring ("&f Rose Red &8x5"), as recorded.
     */
    public static List<String> sidebar(Objective objective, double progress) {
        List<String> lines = new ArrayList<>(List.of("&fObjective", "&e" + objective.text()));
        Quest.Goal goal = objective.goal();
        if (goal != null && goal.kind() == Quest.Goal.Kind.ITEMS) lines.add(itemLine(goal));
        else if (goal != null) lines.add("  " + counted(goal, progress));
        return lines;
    }

    /** "&7(&e3&7/&a10&7)" for a count, "&8(&b150&8/&350 XP&8)" for a skill's XP; nothing for what to bring, or no goal. */
    static String counted(Quest.Goal goal, double progress) {
        if (goal == null) return "";
        return switch (goal.kind()) {
            case COUNT -> "&7(&e" + whole(progress) + "&7/&a" + goal.target() + "&7)";
            case SKILL_XP -> "&8(&b" + whole(progress) + "&8/&3" + goal.target() + " XP&8)";
            case ITEMS -> "";
        };
    }

    /** How full the boss bar is: what's counted of the goal (full for one with nothing to count). */
    public static float fill(Quest.Goal goal, double progress) {
        if (goal == null || goal.kind() == Quest.Goal.Kind.ITEMS) return 1f;
        return (float) Math.clamp(progress / goal.target(), 0, 1);
    }

    /** "&f Rose Red &8x5": the item in its rarity's colour (white while the item data isn't there). */
    static String itemLine(Quest.Goal goal) {
        SkyBlockItem item = ItemRegistry.get(goal.item());
        String name = item == null ? goal.item() : item.name();
        String color = item == null ? "&f" : item.rarity().getColor().replace('§', '&');
        return color + " " + name + " &8x" + goal.target();
    }

    private static String whole(double value) {
        return Long.toString((long) Math.floor(value));
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
     * The Quest Log's progress: as {@link #progress}, each objective that counts with its count, as
     * recorded (2026-10-25): done in white, " &a✔ &fKill Zombies. (&b10&f/&b10&f)"; the one they're at in
     * yellow, " &c✖ &eExplore the Hub. (&b2&e/&b13&e)"; a skill's XP as it is now, even once done ("Reach
     * Combat Level I. (&b165&f/&b50&f)").
     */
    static List<String> logProgress(Document profile, Quest quest) {
        int done = Quests.done(profile, quest);
        List<String> lines = progress(quest, done);
        for (int i = 0; i < lines.size(); i++) {
            Quest.Goal goal = quest.objective(i).goal();
            if (goal == null || goal.kind() == Quest.Goal.Kind.ITEMS) continue;
            double value = goal.kind() == Quest.Goal.Kind.SKILL_XP ? Skills.xp(profile, goal.skill())
                    : i < done ? goal.target() : Quests.count(profile, quest);
            String color = i < done ? "&f" : "&e";
            lines.set(i, lines.get(i) + " (&b" + whole(value) + color + "/&b" + goal.target() + color + ")");
        }
        return lines;
    }

    /** What an objective that counts has counted: Time to Strike's zombies, or a skill's XP now (0 for one that doesn't count). */
    public static double progress(Document profile, Quest quest, int objective) {
        Quest.Goal goal = objective < 0 ? null : quest.objective(objective).goal();
        if (goal == null) return 0;
        return switch (goal.kind()) {
            case COUNT -> Quests.count(profile, quest);
            case SKILL_XP -> Skills.xp(profile, goal.skill());
            case ITEMS -> goal.target();
        };
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
        lore.addAll(logProgress(profile, quest));
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
