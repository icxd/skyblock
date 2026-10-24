package net.icxd.dungeons.quest;

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

    /** The objective they're at done, if it's this one (nothing otherwise). */
    public static void complete(Player player, Quest quest, int objective) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        if (Quests.complete(user.profile(), quest, objective, System.currentTimeMillis()) == null) return;
        user.save();
        QuestBar.progressed(player);
    }

    /** A quest started by something they found (an area): no word of it but the boss bar's. */
    public static void start(Player player, Quest quest) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || !Quests.start(user.profile(), quest, System.currentTimeMillis())) return;
        user.save();
        QuestBar.moved(player);
    }
}
