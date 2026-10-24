package net.icxd.dungeons.quest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bukkit.Material;

import net.icxd.dungeons.quest.Place.Island;
import net.icxd.dungeons.quest.Quest.Objective;

/**
 * The quests there are, and a profile's progress in them, as plain functions on its document (no
 * server needed). A profile keeps each quest it has started under {@code quests.<id>}:
 *
 * <pre>
 * started:   when (the Quest Log's "Started:")
 * done:      how many of its objectives are done
 * completed: when the last one was (absent while it's going)
 * </pre>
 *
 * <p>The quests are the recording's (2026-10-24, a new profile's first ten minutes) and the names
 * the Quest Log and chat gave them: Getting Started on the island, then Marco the Artist, which it
 * leads to, Time to Strike in the Hub, and the Banker's Saving Up. Marco and the Bartender aren't in
 * the Hub yet (their places weren't recorded), so those two stay going, as they still were on the
 * recorded older profile, years in.
 */
public final class Quests {
    public static final String KEY = "quests";

    public static final Quest GETTING_STARTED = new Quest("GETTING_STARTED", "Getting Started", Material.PAPER, Home.on(Island.PRIVATE_ISLAND),
            List.of(new Objective("Break a log", true), new Objective("Craft a workbench", true), new Objective("Craft a Wooden Pickaxe", true),
                    new Objective("Talk to Jerry", false), new Objective("Use the teleporter", false)), "MARCO_THE_ARTIST");
    public static final int BREAK_A_LOG = 0;
    public static final int CRAFT_A_WORKBENCH = 1;
    public static final int CRAFT_A_WOODEN_PICKAXE = 2;
    public static final int TALK_TO_JERRY = 3;
    public static final int USE_THE_TELEPORTER = 4;

    public static final Quest MARCO_THE_ARTIST = new Quest("MARCO_THE_ARTIST", "Marco the Artist", Material.PAPER, Home.anywhere(),
            List.of(new Objective("Talk to Marco", true)), null);
    /** Started when the Village is found (the boss bar went to it then). */
    public static final Quest TIME_TO_STRIKE = new Quest("TIME_TO_STRIKE", "Time to Strike", Material.PAPER, Home.on(Island.HUB),
            List.of(new Objective("Talk to the Bartender", true)), null);

    /**
     * Started by talking to the Banker, which is its first objective (+5 SkyBlock XP); a deposit is the
     * second (+10 Coins). At home in the Bank: the boss bar went to it there, and only there.
     */
    public static final Quest SAVING_UP = new Quest("SAVING_UP", "Saving Up", Material.PAPER, Home.in(Island.HUB, "Bank"),
            List.of(new Objective("Talk to the Banker", true, Quest.Reward.skyBlockXp(5)),
                    new Objective("Deposit Coins in the Bank", true, Quest.Reward.coins(10))), null);
    public static final int TALK_TO_THE_BANKER = 0;
    public static final int DEPOSIT_COINS = 1;

    public static final List<Quest> ALL = List.of(GETTING_STARTED, MARCO_THE_ARTIST, TIME_TO_STRIKE, SAVING_UP);

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
        if (objective + 1 < quest.objectives().size()) return new Step(quest, false, null);
        entry.put("completed", new Date(now));
        Quest next = quest.next() == null ? null : byId(quest.next());
        return new Step(quest, true, next != null && start(profile, next, now) ? next : null);
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
            int score = quest.home().score(place);
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
        if (!moved && following != null && active(profile, following) && following.home().score(place) > 0) return following;
        return best(profile, place);
    }
}
