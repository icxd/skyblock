package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.List;

import net.icxd.dungeons.leveling.LevelingData.Reward;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * What each SkyBlock level gives: +5 Health every level and +1 Strength every fifth, as the recorded Stat Rewards
 * item says ("For every level: +5 Health", "For every 5 levels: +1 Strength"; Level 90's rewards are +1 Strength and
 * +5 Health, and level 88's Health breakdown is +440, so not the wiki's +10 Health every tenth level), and each
 * level's other rewards from the data. No server needed.
 */
public final class LevelRewards {
    public static final int HEALTH = 5;
    public static final int STRENGTH = 1;
    public static final int STRENGTH_EVERY = 5;

    private LevelRewards() {
    }

    /** What every level up to this one gives together. */
    public static Stats stats(int level) {
        Stats stats = new Stats();
        if (level <= 0) return stats;
        stats.add(Stat.HEALTH, HEALTH * level);
        stats.add(Stat.STRENGTH, STRENGTH * (level / STRENGTH_EVERY));
        return stats;
    }

    /** Adds what every level up to this one gives to someone's stats (each tick, so it makes nothing new). */
    public static void add(Stats stats, int level) {
        if (level <= 0) return;
        stats.add(Stat.HEALTH, HEALTH * level);
        stats.add(Stat.STRENGTH, STRENGTH * (level / STRENGTH_EVERY));
    }

    /** "&8+&a5 &c❤ Health". */
    static String healthLine(int health) {
        return "&8+&a" + health + " " + Stat.HEALTH.label();
    }

    /** "&8+&a1 &c❁ Strength". */
    static String strengthLine(int strength) {
        return "&8+&a" + strength + " " + Stat.STRENGTH.label();
    }

    /** A level's own stat lines: Strength (every fifth level), then Health; none for level 0. */
    static List<String> statLines(int level) {
        List<String> lines = new ArrayList<>();
        if (level <= 0) return lines;
        if (level % STRENGTH_EVERY == 0) lines.add(strengthLine(STRENGTH));
        lines.add(healthLine(HEALTH));
        return lines;
    }

    /**
     * A level's rewards as the SkyBlock Leveling menu lists them (" &fBoxes Emblem &7⧉", " &8+&a1 &c❁ Strength", " &8+&a5 &c❤
     * Health"), unindented: its other rewards first, then its stats.
     */
    public static List<String> lines(LevelingData data, int level) {
        List<String> lines = new ArrayList<>();
        if (level <= 0) return lines;
        for (Reward reward : data.rewards(level)) lines.add(reward.name());
        lines.addAll(statLines(level));
        return lines;
    }

    /** Whether a level has a reward other than its stats: the menus draw it as a milestone (a glass block, not a pane). */
    public static boolean milestone(LevelingData data, int level) {
        return level > 0 && !data.rewards(level).isEmpty();
    }

    /** The first milestone level after this one; -1 if there's none left. */
    public static int nextMilestone(LevelingData data, int level) {
        for (Reward reward : data.rewards()) if (reward.level() > level) return reward.level();
        return -1;
    }

    /** "Reward:" over one line, "Rewards:" over none or several (level 0's recorded "Rewards:" with nothing under it). */
    static String heading(int lines) {
        return lines == 1 ? "&7Reward:" : "&7Rewards:";
    }

    /**
     * What going from one level to a higher one gives, as the level-up message lists it: the levels' other rewards,
     * then their Strength and Health together ("Level 287 ➡ [289]" gave "+10 ❤ Health", in a real chat log).
     */
    static List<String> gained(LevelingData data, int from, int to) {
        List<String> lines = new ArrayList<>();
        for (Reward reward : data.rewards()) if (reward.level() > from && reward.level() <= to) lines.add(reward.name());
        Stats before = stats(from), after = stats(to);
        int strength = (int) (after.get(Stat.STRENGTH) - before.get(Stat.STRENGTH));
        int health = (int) (after.get(Stat.HEALTH) - before.get(Stat.HEALTH));
        if (strength > 0) lines.add(strengthLine(strength));
        if (health > 0) lines.add(healthLine(health));
        return lines;
    }
}
