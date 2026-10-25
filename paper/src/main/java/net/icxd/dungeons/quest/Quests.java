package net.icxd.dungeons.quest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bukkit.Material;

import net.icxd.dungeons.quest.Place.Island;
import net.icxd.dungeons.quest.Quest.Goal;
import net.icxd.dungeons.quest.Quest.Objective;
import net.icxd.dungeons.quest.Quest.Reward;
import net.icxd.dungeons.quest.Quest.Target;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;

/**
 * The quests there are, and a profile's progress in them, as plain functions on its document (no
 * server needed). A profile keeps each quest it has started under {@code quests.<id>}:
 *
 * <pre>
 * started:   when (the Quest Log's "Started:")
 * done:      how many of its objectives are done
 * count:     what the one it's at has counted (Time to Strike's zombies)
 * completed: when the last one was (absent while it's going)
 * </pre>
 *
 * <p>The quests are the recordings' (2026-10-24 and 10-25, a new profile's first minutes and the
 * Hub's quests) and the names the Quest Log and chat gave them: Getting Started on the island, then
 * the Hub's Marco the Artist, Time to Strike and Saving Up, which the Village's discovery starts (the
 * Quest Log had them started together; the wiki: "visiting the Hub Island").
 */
public final class Quests {
    public static final String KEY = "quests";

    /**
     * Where the Hub's sidebar arrows point (see QuestText#arrow), fitted to the recorded arrows. Here only
     * Time to Strike's objectives had them (Farmer Rigby's quest, not here, had one too); Getting
     * Started's, Marco's and Saving Up's had none. The Bartender is where the recorded NPC table has him.
     */
    static final Target BARTENDER = new Target(Island.HUB, -40.5, -72.5);
    /** UNKNOWN: the point in the Graveyard's north-east the arrows fit best (20 of 21), not its middle. */
    static final Target GRAVEYARD = new Target(Island.HUB, -104, -122);
    /** UNKNOWN: where the arrows fit best (all 187, their colour too); no recording went there. */
    static final Target SPIDERS_DEN = new Target(Island.HUB, -144, -188);

    public static final Quest GETTING_STARTED = new Quest("GETTING_STARTED", "Getting Started", Material.PAPER, Home.on(Island.PRIVATE_ISLAND),
            List.of(new Objective("Break a log", true), new Objective("Craft a workbench", true), new Objective("Craft a Wooden Pickaxe", true),
                    new Objective("Talk to Jerry", false), new Objective("Use the teleporter", false)), "MARCO_THE_ARTIST");
    public static final int BREAK_A_LOG = 0;
    public static final int CRAFT_A_WORKBENCH = 1;
    public static final int CRAFT_A_WOODEN_PICKAXE = 2;
    public static final int TALK_TO_JERRY = 3;
    public static final int USE_THE_TELEPORTER = 4;

    /**
     * Marco wants 5 Rose Red (from the flowers by his house) and gives a Spray Can. Followed anywhere,
     * and most of all in the Flower House: the boss bar went to it there over the Bartender's, and
     * showed it on arriving anywhere with nothing more at home.
     */
    public static final Quest MARCO_THE_ARTIST = new Quest("MARCO_THE_ARTIST", "Marco the Artist", Material.PAPER,
            Home.in(Island.HUB, "Flower House").orAnywhere(),
            List.of(new Objective("Talk to Marco", true),
                    new Objective("Give Rose Red Dye to Marco", true, Reward.item("artist:SPRAY_CAN")).counting(Goal.items("INK_SACK:1", 5))),
            null);
    public static final int TALK_TO_MARCO = 0;
    public static final int GIVE_ROSE_RED = 1;
    /** How many Rose Red Marco takes. */
    public static final int ROSE_RED = 5;

    /**
     * The Bartender's, at home in the Hub: an Undead Sword for the zombies, ten of them killed (+5
     * SkyBlock XP, +100 Combat XP), 100 coins, Combat Level I (done silently), then the Spider's Den,
     * which isn't here, so it stops there.
     */
    public static final Quest TIME_TO_STRIKE = new Quest("TIME_TO_STRIKE", "Time to Strike", Material.PAPER, Home.on(Island.HUB),
            List.of(new Objective("Talk to the Bartender", true, Reward.item("bartender:UNDEAD_SWORD")).toward(BARTENDER),
                    new Objective("Kill Zombies", true, Reward.skyBlockXp(5).andSkillXp(Skill.COMBAT, 100)).counting(Goal.count(10)).toward(GRAVEYARD),
                    new Objective("Talk to the Bartender", true, Reward.coins(100)).toward(BARTENDER),
                    new Objective("Reach Combat Level I", true).counting(Goal.skillXp(Skill.COMBAT, 50)),
                    new Objective("Travel to the Spider's Den", true).toward(SPIDERS_DEN)),
            null);
    public static final int TALK_TO_THE_BARTENDER = 0;
    public static final int KILL_ZOMBIES = 1;
    public static final int TALK_TO_THE_BARTENDER_AGAIN = 2;
    public static final int REACH_COMBAT_I = 3;
    public static final int TRAVEL_TO_SPIDERS_DEN = 4;

    /**
     * Talking to the Banker is its first objective (+5 SkyBlock XP), a deposit the second (+10 Coins).
     * The second is followed in the Bank, and the first nowhere: the boss bar never showed it, even on
     * entering the Bank with it going (2026-10-25).
     */
    public static final Quest SAVING_UP = new Quest("SAVING_UP", "Saving Up", Material.PAPER, Home.nowhere(),
            List.of(new Objective("Talk to the Banker", true, Reward.skyBlockXp(5)),
                    new Objective("Deposit Coins in the Bank", true, Reward.coins(10)).followed(Home.in(Island.HUB, "Bank"))), null);
    public static final int TALK_TO_THE_BANKER = 0;
    public static final int DEPOSIT_COINS = 1;

    public static final List<Quest> ALL = List.of(GETTING_STARTED, MARCO_THE_ARTIST, TIME_TO_STRIKE, SAVING_UP);

    /** What finding the Village starts (all of them "visiting the Hub Island"; Marco's may have begun already). */
    public static final List<Quest> HUB = List.of(MARCO_THE_ARTIST, TIME_TO_STRIKE, SAVING_UP);

    private Quests() {
    }

    public static Quest byId(String id) {
        for (Quest quest : ALL) {
            if (quest.id().equals(id)) return quest;
        }
        return null;
    }

    // A profile's progress

    private static Document entry(Document profile, Quest quest) {
        return profile.get(KEY) instanceof Document quests && quests.get(quest.id()) instanceof Document entry ? entry : null;
    }

    public static boolean started(Document profile, Quest quest) {
        return entry(profile, quest) != null;
    }

    public static boolean completed(Document profile, Quest quest) {
        Document entry = entry(profile, quest);
        return entry != null && entry.containsKey("completed");
    }

    /** Started and not completed. */
    public static boolean active(Document profile, Quest quest) {
        return started(profile, quest) && !completed(profile, quest);
    }

    /** How many of its objectives are done (0 for one not started). */
    public static int done(Document profile, Quest quest) {
        Document entry = entry(profile, quest);
        return entry != null && entry.get("done") instanceof Number n ? Math.min(n.intValue(), quest.objectives().size()) : 0;
    }

    /** The objective they're at (its index); -1 if the quest isn't going. */
    public static int current(Document profile, Quest quest) {
        return active(profile, quest) ? done(profile, quest) : -1;
    }

    /** What the objective they're at has counted so far (0 for none). */
    public static int count(Document profile, Quest quest) {
        Document entry = entry(profile, quest);
        return entry != null && entry.get("count") instanceof Number n ? n.intValue() : 0;
    }

    /** Counts this many more for the objective they're at, if it counts; what it has now (-1 if it doesn't count). */
    public static int addCount(Document profile, Quest quest, int objective, int more) {
        if (current(profile, quest) != objective || quest.objective(objective).goal() == null
                || quest.objective(objective).goal().kind() != Goal.Kind.COUNT) return -1;
        int now = Math.min(count(profile, quest) + more, quest.objective(objective).goal().target());
        entry(profile, quest).put("count", now);
        return now;
    }

    /** When they started it; 0 for one not started. */
    public static long startedAt(Document profile, Quest quest) {
        Document entry = entry(profile, quest);
        return entry != null && entry.get("started") instanceof Date date ? date.getTime() : 0;
    }

    /** Starts a quest at its first objective; false if it was started before (done or not). */
    public static boolean start(Document profile, Quest quest, long now) {
        if (started(profile, quest)) return false;
        Document quests = profile.get(KEY) instanceof Document d ? d : new Document();
        quests.put(quest.id(), new Document("started", new Date(now)).append("done", 0));
        profile.put(KEY, quests);
        return true;
    }

    /**
     * Marks the objective they're at done, if it's {@code objective}: the next one is theirs, or the quest is
     * completed and the one it leads to starts. What happened, or null if they weren't at that objective.
     */
    public static Step complete(Document profile, Quest quest, int objective, long now) {
        if (current(profile, quest) != objective) return null;
        Document entry = entry(profile, quest);
        entry.put("done", objective + 1);
        entry.remove("count");
        if (objective + 1 < quest.objectives().size()) return new Step(quest, false, null);
        entry.put("completed", new Date(now));
        Quest next = quest.next() == null ? null : byId(quest.next());
        return new Step(quest, true, next != null && start(profile, next, now) ? next : null);
    }

    /**
     * Marks done, without a word, every objective they're at whose goal is a skill's XP they now have and
     * that gives nothing ("Reach Combat Level I", done as they next arrived somewhere, as recorded).
     * Whether any was.
     */
    public static boolean reachGoals(Document profile, long now) {
        boolean any = false;
        for (Quest quest : ALL) {
            int at = current(profile, quest);
            if (at < 0 || !reached(profile, quest, at) || quest.objective(at).reward() != null) continue;
            complete(profile, quest, at, now);
            any = true;
        }
        return any;
    }

    /**
     * Whether this objective's goal is a skill's XP they already have: it's done as they next arrive
     * somewhere ({@link #reachGoals}), and NEW OBJECTIVE didn't say it ("Reach Combat Level I" at
     * 150/50 XP, recorded 2026-10-25).
     */
    public static boolean reached(Document profile, Quest quest, int objective) {
        Goal goal = quest.objective(objective).goal();
        return goal != null && goal.kind() == Goal.Kind.SKILL_XP && Skills.xp(profile, goal.skill()) >= goal.target();
    }

    /** An objective done: whether that completed the quest, and the quest that started because of it (null for none). */
    public record Step(Quest quest, boolean completed, Quest started) {
    }

    /** The SkyBlock XP the objectives it has done gave (SkyBlock Leveling's Complete Objectives). */
    public static int skyBlockXp(Document profile) {
        int xp = 0;
        for (Quest quest : ALL) {
            int done = done(profile, quest);
            for (int i = 0; i < done; i++) {
                Quest.Reward reward = quest.objective(i).reward();
                if (reward != null) xp += reward.skyBlockXp();
            }
        }
        return xp;
    }

    /** The quests going, oldest first (the Quest Log's order after the Fairy Souls). */
    public static List<Quest> active(Document profile) {
        List<Quest> going = new ArrayList<>();
        for (Quest quest : ALL) if (active(profile, quest)) going.add(quest);
        going.sort(Comparator.comparingLong(q -> startedAt(profile, q)));
        return going;
    }

    /** The quests done, oldest first. */
    public static List<Quest> completed(Document profile) {
        List<Quest> done = new ArrayList<>();
        for (Quest quest : ALL) if (completed(profile, quest)) done.add(quest);
        done.sort(Comparator.comparingLong(q -> startedAt(profile, q)));
        return done;
    }

    // Following one

    /**
     * The quest to follow at a place (the boss bar's and sidebar's): the one most at home there (see
     * {@link Home}), the latest started among equals; null if none is followed there.
     */
    public static Quest best(Document profile, Place place) {
        Quest best = null;
        int bestScore = 0;
        long bestStart = 0;
        for (Quest quest : ALL) {
            if (!active(profile, quest)) continue;
            int score = home(profile, quest).score(place);
            long start = startedAt(profile, quest);
            if (score > bestScore || (score == bestScore && score > 0 && start > bestStart)) {
                best = quest;
                bestScore = score;
                bestStart = start;
            }
        }
        return best;
    }

    /**
     * Which quest to follow after something happened, given the one followed till now ({@code
     * following}, null for none). Arriving somewhere picks the one most at home there; progress keeps
     * the one being followed while it's going (talking to the Banker started his quest without the
     * boss bar leaving "Talk to the Bartender"), and picks again once it's done or if there was none.
     */
    public static Quest follow(Document profile, Place place, Quest following, boolean moved) {
        if (!moved && following != null && active(profile, following) && home(profile, following).score(place) > 0) return following;
        return best(profile, place);
    }

    /** Where the objective they're at is followed: its own home, else its quest's. */
    static Home home(Document profile, Quest quest) {
        int at = current(profile, quest);
        Home own = at < 0 ? null : quest.objective(at).home();
        return own != null ? own : quest.home();
    }
}
