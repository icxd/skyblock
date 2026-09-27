package net.icxd.dungeons.skill;

import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;

/** A player's skill levels, from the skill XP their data holds (for now just Combat's). Main thread. */
public final class Skills {
    private Skills() {
    }

    /** Their Combat level on the profile they play on: 0 while their data isn't loaded. */
    public static int combatLevel(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        Number xp = user == null ? null : user.profileValue("skills.combat", Number.class);
        return xp == null ? 0 : level(xp.doubleValue());
    }

    /** The level this much XP reaches on the standard skill table ({@link Skill#XP_GOALS}), at most 60. */
    public static int level(double xp) {
        int level = 0;
        while (level + 1 < Skill.XP_GOALS.size() && xp >= Skill.XP_GOALS.get(level + 1)) level++;
        return level;
    }
}
