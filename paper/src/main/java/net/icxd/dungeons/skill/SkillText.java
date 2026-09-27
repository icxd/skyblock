package net.icxd.dungeons.skill;

import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonLevels;
import net.icxd.dungeons.utils.Utils;
import org.bson.Document;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * How skills and their XP are written: the progress bars and numbers of the menus, the action bar's
 * "+60.2 Combat (35.46%)", the tab list's lines and the level-up message (research skills.md 1.2,
 * 2.1-2.3). No server needed.
 */
public final class SkillText {
    /** The menus' bars are 25 struck-through spaces. */
    public static final int BAR = 25;
    public static final String RULE = "&3&l" + "▬".repeat(64);

    private static final ThreadLocal<DecimalFormat> ONE_DECIMAL = format("#,##0.#");
    private static final ThreadLocal<DecimalFormat> TWO_DECIMALS = format("#,##0.##");

    private SkillText() {
    }

    /**
     * Rounded to the nearest, a tie to the even digit: the recorded percentages are rounded, not cut
     * (8,199.8 of 15k is 54.7%), and Hunting's 85 of 2k shows 4.2% (half-up would make it 4.3%).
     */
    private static ThreadLocal<DecimalFormat> format(String pattern) {
        return ThreadLocal.withInitial(() -> {
            DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT));
            format.setRoundingMode(RoundingMode.HALF_EVEN);
            return format;
        });
    }

    /** "272,514.2", "82,575": thousands grouped, one decimal when there is one. */
    public static String number(double value) {
        return ONE_DECIMAL.get().format(value);
    }

    /** "35.46", "35.9", "36": to two decimals, trailing zeros dropped (the action bar's percentage). */
    public static String twoDecimals(double value) {
        return TWO_DECIMALS.get().format(value);
    }

    /** "38.9", "85": a fraction as a percentage to one decimal (the menus' and the tab list's). */
    public static String percent(double fraction) {
        return number(fraction * 100);
    }

    /**
     * "785", "2k", "15k", "700k", "1.2M", "192.7k": a bar's maximum, one decimal at most. How a second
     * decimal (2,750,000) is shown is UNKNOWN (no recorded bar had one): it's rounded.
     */
    public static String shortNumber(double value) {
        if (value < 1_000) return number(value);
        if (value < 1_000_000) return number(value / 1_000) + "k";
        if (value < 1_000_000_000) return number(value / 1_000_000) + "M";
        return number(value / 1_000_000_000) + "B";
    }

    /** How many of the bar's 25 are filled: a started part counts (4.2% fills 2, 36% 9, 95.5% 24). */
    public static int filled(double fraction) {
        // Less a hair, so 36% (which isn't exact in binary) doesn't round up past 9.
        return (int) Math.max(0, Math.min(BAR, Math.ceil(fraction * BAR - 1e-9)));
    }

    /** "&2&l&m          &f&l&m               &r &e272,514.2&6/&e700k". */
    public static String bar(double fraction, double current, double max) {
        return strip(fraction) + " &e" + number(current) + "&6/&e" + shortNumber(max);
    }

    /** The struck-through part, ending in "&r": dark green for what's done, white for the rest (none of a part that's empty). */
    public static String strip(double fraction) {
        int filled = filled(fraction);
        StringBuilder out = new StringBuilder();
        if (filled > 0) out.append("&2&l&m").append(" ".repeat(filled));
        if (filled < BAR) out.append("&f&l&m").append(" ".repeat(BAR - filled));
        return out.append("&r").toString();
    }

    /** "Combat XXIV"; a skill at level 0 is only its name (as mods read the menu: no number, level 0). */
    public static String named(Skill skill, int level) {
        return level > 0 ? skill.getName() + " " + Utils.getRomanNumeral(level) : skill.getName();
    }

    /**
     * The action bar's part for a gain, in place of Defense: "&3+60.2 Combat (35.46%)", the last gain
     * and how far into the level the skill is now. At the cap, XP past it and "/0" (SkyHanni's test line
     * "+207.2 Hunting (5,183,244/0)"; not recorded with the percentage setting, so this is assumed).
     */
    public static String actionBar(Skills.Gain gain) {
        Skill skill = gain.skill();
        String progress = skill.maxed(gain.after()) ? number(skill.xpIntoLevel(gain.after())) + "/0"
                : twoDecimals(skill.progress(gain.after()) * 100) + "%";
        return "&3+" + number(gain.amount()) + " " + skill.getName() + " (" + progress + ")";
    }

    /**
     * A skill's tab list line: " Combat 24: &a36.5%" on the Hub (in the dungeon it's after "&e&lSkills: "
     * with {@code &3}). At the cap "MAX" (mods read " Farming 60: MAX"; its colour UNKNOWN: the percentage's).
     */
    public static String tab(Skill skill, double xp, String color) {
        int level = skill.level(xp);
        String name = level > 0 ? skill.getName() + " " + level : skill.getName();
        return name + ": " + color + (skill.maxed(xp) ? "MAX" : percent(skill.progress(xp)) + "%");
    }

    /** The Hub tab list's Skills widget: its header and these four, in the recorded order. */
    public static List<String> hubTab(Document profile) {
        List<String> lines = new ArrayList<>();
        lines.add("&e&lSkills:");
        for (Skill skill : List.of(Skill.FARMING, Skill.MINING, Skill.COMBAT, Skill.FORAGING)) {
            lines.add(" " + tab(skill, Skills.xp(profile, skill), "&a"));
        }
        return lines;
    }

    /** The dungeon tab list's one line: the skill that last gained XP ("&e&lSkills: &aCombat 24: &335.5%"). */
    public static String dungeonTab(Skill skill, double xp) {
        return "&e&lSkills: &a" + tab(skill, xp, "&3");
    }

    /** The Dungeon Hub tab list's Dungeons widget: " &fCatacombs 21: &a22%", " &aBerserk 20: 72.6%" (the selected class). */
    public static List<String> dungeonsTab(double catacombsXp, DungeonClass dungeonClass, double classXp) {
        return List.of("&6&lDungeons:",
                " &fCatacombs " + DungeonLevels.level(catacombsXp) + ": &a" + percent(DungeonLevels.progress(catacombsXp)) + "%",
                " &a" + dungeonClass.getDisplayName() + " " + DungeonLevels.level(classXp) + ": " + percent(DungeonLevels.progress(classXp)) + "%");
    }

    /**
     * The chat message for reaching {@code level}. Not recorded (research skills.md 2.3), so it's the
     * layout other remakes agree on, NOT verified against Hypixel: a dark aqua rule, the header, the
     * level's rewards as the menu lists them, the rule again.
     */
    public static List<String> levelUp(Skill skill, int level) {
        List<String> lines = new ArrayList<>();
        lines.add(RULE);
        String from = level > 1 ? "&8" + Utils.getRomanNumeral(level - 1) + "➜" : "";
        lines.add("  &b&lSKILL LEVEL UP &3" + skill.getName() + " " + from + "&3" + Utils.getRomanNumeral(level));
        lines.add("");
        lines.add("  &a&lREWARDS");
        for (String line : SkillRewards.lines(skill, level)) lines.add("    " + line);
        lines.add(RULE);
        return lines;
    }
}
