package net.icxd.dungeons.skill;

import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * A profile's skill XP and levels: XP is a double under {@code skills.<key>} ({@link Skill#key}), and
 * older profiles' whole numbers read the same (the next gain writes a double). New profiles have every
 * skill's key at 0.0; older ones may lack some, which read as no XP. Functions on the profile's
 * document, with no server needed, and a few on the profile a player plays on. Main thread.
 */
public final class Skills {
    private Skills() {
    }

    /** What adding XP did: the XP before and after, and the levels they reach. */
    public record Gain(Skill skill, double amount, double before, double after) {
        public int oldLevel() {
            return skill.level(before);
        }

        public int newLevel() {
            return skill.level(after);
        }

        public boolean leveledUp() {
            return newLevel() > oldLevel();
        }
    }

    /** The skill's XP on this profile (0 for none, or no profile). */
    public static double xp(Document profile, Skill skill) {
        Document skills = profile == null ? null : profile.get("skills") instanceof Document d ? d : null;
        return skills != null && skills.get(skill.key()) instanceof Number n ? n.doubleValue() : 0;
    }

    public static int level(Document profile, Skill skill) {
        return skill.level(xp(profile, skill));
    }

    /** Every skill's level on this profile. */
    public static Map<Skill, Integer> levels(Document profile) {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, level(profile, skill));
        return levels;
    }

    /** Adds XP to a skill on this profile (nothing for none or less). */
    public static Gain add(Document profile, Skill skill, double amount) {
        double before = xp(profile, skill);
        if (!(amount > 0)) return new Gain(skill, 0, before, before);
        Document skills = profile.get("skills") instanceof Document d ? d : null;
        if (skills == null) {
            skills = new Document();
            profile.put("skills", skills);
        }
        double after = before + amount;
        skills.put(skill.key(), after);
        return new Gain(skill, amount, before, after);
    }

    /** The skill's level on the profile they play on: 0 while their data isn't loaded. */
    public static int level(Player player, Skill skill) {
        User user = User.ifLoaded(player.getUniqueId());
        return user == null ? 0 : level(user.profile(), skill);
    }

    /** Their Combat level, for the Warrior bonus on their hits (see {@link net.icxd.dungeons.combat.Damage#warrior}). */
    public static int combatLevel(Player player) {
        return level(player, Skill.COMBAT);
    }

    /**
     * The mean of the non-cosmetic skills' whole levels (research skills.md 1.1: the recorded 21.6 is
     * the ten skills but Runecrafting and Social, Hunting included).
     */
    public static double average(Map<Skill, Integer> levels) {
        int sum = 0, count = 0;
        for (Skill skill : Skill.values()) {
            if (skill.cosmetic()) continue;
            sum += levels.getOrDefault(skill, 0);
            count++;
        }
        return (double) sum / count;
    }

    public static double average(Document profile) {
        return average(levels(profile));
    }

    /** The SkyBlock XP the profile's skill levels have given (each level's, see {@link Skill#skyBlockXp}). */
    public static int skyBlockXp(Document profile) {
        int xp = 0;
        for (Skill skill : Skill.values()) {
            int level = level(profile, skill);
            for (int l = 1; l <= level; l++) xp += skill.skyBlockXp(l);
        }
        return xp;
    }

    /** What the profile's skill levels add to its stats (see {@link SkillRewards#stats}). */
    public static Stats stats(Document profile) {
        Stats stats = new Stats();
        if (profile == null) return stats;
        for (Skill skill : Skill.values()) stats.add(SkillRewards.stats(skill, level(profile, skill)));
        return stats;
    }
}
