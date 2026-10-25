package net.icxd.dungeons.quest;

import java.util.List;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.user.User;

/** Quests moving on for a player who is here: saved, and the boss bar and chat told (see {@link QuestBar}). Main thread. */
public final class QuestProgress {
    /** On a profile until its island has welcomed it. */
    public static final String WELCOME = "welcome";

    private QuestProgress() {
    }

    /** A new profile's start: Getting Started, and the welcome when it first gets to its island. */
    public static void newProfile(Document profile, long now) {
        Quests.start(profile, Quests.GETTING_STARTED, now);
        profile.put(WELCOME, true);
    }

    /** Whether they're at this objective of this quest. */
    public static boolean at(Player player, Quest quest, int objective) {
        User user = User.ifLoaded(player.getUniqueId());
        return user != null && Quests.current(user.profile(), quest) == objective;
    }

    /** The objective they're at done, if it's this one (nothing otherwise), with what it gives (see {@link QuestRewards}). */
    public static void complete(Player player, Quest quest, int objective) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Quests.Step step = Quests.complete(user.profile(), quest, objective, System.currentTimeMillis());
        if (step == null) return;
        Quest.Reward reward = quest.objective(objective).reward();
        if (reward != null) QuestRewards.give(player, user, quest, Quests.done(user.profile(), quest), step.completed(), reward);
        user.save();
        QuestBar.progressed(player);
    }

    /**
     * An objective done by talking to the one whose quest it is, which starts the quest (the Banker's
     * Saving Up). The boss bar stays on the quest it follows, as it did: starting this way isn't
     * arriving somewhere.
     */
    public static void startedBy(Player player, Quest quest, int objective) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Quests.start(user.profile(), quest, System.currentTimeMillis());
        complete(player, quest, objective);
    }

    /** A quest started by something they found (an area): no word of it but the boss bar's. */
    public static void start(Player player, Quest quest) {
        startAll(player, List.of(quest));
    }

    /** Quests started together by something they found (the Village starts the Hub's), as {@link #start}. */
    public static void startAll(Player player, List<Quest> quests) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        long now = System.currentTimeMillis();
        boolean any = false;
        for (Quest quest : quests) any |= Quests.start(user.profile(), quest, now);
        if (!any) return;
        user.save();
        QuestBar.moved(player);
    }

    /**
     * Counts this many more for the objective they're at, if it's this one and it counts (Time to Strike's
     * zombies): done once it reaches its goal, with what it gives; till then the boss bar shows the count.
     */
    public static void count(Player player, Quest quest, int objective, int more) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        int now = Quests.addCount(user.profile(), quest, objective, more);
        if (now < 0) return;
        if (now >= quest.objective(objective).goal().target()) {
            complete(player, quest, objective);
            return;
        }
        user.save();
        QuestBar.counted(player);
    }
}
