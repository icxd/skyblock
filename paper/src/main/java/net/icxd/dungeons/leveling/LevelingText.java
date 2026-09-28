package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.List;

import net.icxd.dungeons.skill.SkillText;
import net.icxd.dungeons.utils.Text;

/**
 * How SkyBlock Leveling writes things: its bars and percentages (the recorded menus'), the rewards' states, the
 * level-up message and the action bar's XP. No server needed.
 */
final class LevelingText {
    /**
     * How wide Hypixel lets these menus' generated lines get: the guide's "Reach 50% completion of Intermediate"
     * (188 pixels) stays on one line, "... of Professional" (190) breaks, and so do the ranking's "You have completed
     * 15.2% of the total" (191) and the bonuses' "When you are attacked, take 0.9% less" (191).
     */
    static final int WIDTH = 188;
    /** Hypixel centres the level-up message's lines around this many pixels (every line of the real chat logs'). */
    private static final int CHAT_MIDDLE = 160;

    private LevelingText() {
    }

    /** The text without its colour and format codes. */
    static String plain(String text) {
        return text.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
    }

    /** A bar's struck-through part, done in {@code color} ("&3"), the rest white: 25 in all, as the skills' bars. */
    static String strip(String color, double fraction) {
        return SkillText.strip(fraction).replace("&2&l&m", color + "&l&m");
    }

    /** "44.4", "0", "88.3": a fraction as a percentage, one decimal at most (every recorded one is rounded). */
    static String percent(double fraction) {
        return SkillText.percent(Math.max(0, fraction));
    }

    /** Text wrapped the way these menus' generated lines are, each line after the first in the colour carried over. */
    static List<String> wrap(String text) {
        return Text.wrap(text, WIDTH);
    }

    /**
     * Where a level's reward stands for someone with this much XP: unlocked; within five levels, the levels left and
     * the XP from their level to it ("Levels left to Unlock: 2", "34/200 XP"); further, how far they are in levels
     * ("Progress to Unlock: 88.3%", "88/100"). The recorded menus and the wiki's (a new profile's Level 5 and Level 6
     * rewards) all fit that five.
     */
    static List<String> state(int rewardLevel, int xp) {
        int level = SkyBlockXp.level(xp);
        if (level >= rewardLevel) return List.of("&a&lUNLOCKED");
        int left = rewardLevel - level;
        if (left <= 5) {
            int into = SkyBlockXp.intoLevel(xp), needed = left * SkyBlockXp.PER_LEVEL;
            return List.of("&7Levels left to Unlock: &3" + left,
                    strip("&3", (double) into / needed) + " &3" + into + "&b/&3" + needed + " XP");
        }
        double levels = Math.max(0, xp) / (double) SkyBlockXp.PER_LEVEL;
        return List.of("&7Progress to Unlock: &3" + percent(levels / rewardLevel) + "%",
                strip("&3", (double) level / rewardLevel) + " &3" + level + "&b/&3" + rewardLevel);
    }

    /** "Rewards Unlocked: 75%" and its bar, "9/12". */
    static List<String> unlocked(String label, int unlocked, int total) {
        double fraction = total == 0 ? 0 : (double) unlocked / total;
        return List.of("&7" + label + ": &3" + percent(fraction) + "%", strip("&3", fraction) + " &3" + unlocked + "&b/&3" + total);
    }

    /** Spaces in front so it sits in the middle of the chat, as Hypixel's level-up lines do. */
    static String centered(String text) {
        int spaces = (int) Math.ceil((CHAT_MIDDLE - Text.width(text) / 2) / 4.0);
        return "&f" + " ".repeat(Math.max(0, spaces)) + text;
    }

    /**
     * The chat when they reach a new level, as real chat logs have Hypixel's (2024-25): a blank line, then centred
     * "SKYBLOCK LEVEL UP", "Level 166 ➡ [167]" (a jump of several levels is one message, "287 ➡ [289]"), REWARDS and
     * what the levels gave, and a blank line. How a reward other than stats is written there is UNKNOWN (no log has
     * one): as the menus name it.
     */
    static List<String> levelUp(int from, int to, List<String> rewards) {
        String color = SkyBlockXp.color(to);
        List<String> lines = new ArrayList<>();
        lines.add(" ");
        lines.add(centered("&r&3&lSKYBLOCK LEVEL UP"));
        lines.add(centered("&r" + color + "Level &r&8" + from + " ➡ &r&8[&r" + color + to + "&r&8]"));
        lines.add(" ");
        lines.add(centered("&r&6&lREWARDS"));
        for (String reward : rewards) lines.add(centered("&r" + reward));
        lines.add(" ");
        return lines;
    }

    /**
     * The action bar's part for XP a task gave, in place of Defense: "&b+20 SkyBlock XP &7(Skill Level Up&7)&b
     * (34/100)", with how far into their level they are now (SkyHanni's copies of it into chat, from real play).
     */
    static String actionBar(int xp, String task, int total) {
        return "&b+" + xp + " SkyBlock XP &7(" + task + "&7)&b (" + SkyBlockXp.intoLevel(total) + "/" + SkyBlockXp.PER_LEVEL + ")";
    }
}
