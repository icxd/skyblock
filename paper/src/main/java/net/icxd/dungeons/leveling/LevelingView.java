package net.icxd.dungeons.leveling;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.leveling.LevelingData.Unlock;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.user.User;

/**
 * What the leveling menus show of someone: the data, the profile they play on and its SkyBlock XP, its mode, their
 * rank and name (for the prefix previews) and whether they see levels in chat. Menus are built from this, so they
 * can be checked without a server.
 */
record LevelingView(LevelingData data, LevelingSources sources, Document profile, SkyBlockXp.Breakdown xp, ProfileMode mode,
                    Rank rank, String name, boolean levelsInChat) {
    static LevelingView of(Player viewer, User user) {
        Document profile = user.profile();
        return new LevelingView(SkyBlockLevels.data(), SkyBlockLevels.sources(), profile, SkyBlockLevels.breakdown(user), user.mode(),
                user.getRank(), viewer.getName(), SkyBlockLevels.levelsInChat(viewer.getUniqueId()));
    }

    int level() {
        return xp.level();
    }

    int total() {
        return xp.total();
    }

    /** Whether they've done what an unlock asks (never, for one the plugin can't tell). */
    boolean done(Unlock unlock) {
        return SkyBlockXp.done(unlock, profile, level(), sources);
    }
}
