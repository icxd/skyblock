package net.icxd.dungeons.dungeons.classes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntToDoubleFunction;

import net.icxd.dungeons.dungeons.DungeonClass;

/**
 * What a dungeon class gives at a class level: a base, and what the levels add. HYPOTHESIS, fitted to
 * one recorded level per class (research critic.md 3.2): the Ready Up menu's lines at the recorded
 * levels (Healer 15, Mage 15, Berserk 20, Archer 16, Tank 14), the Berserk's solo chat ("55% -> 95%"
 * and so on), which says a class played alone doubles the base and not what the levels add, and the
 * wiki's per-level numbers (MCW Healer, Mage, Archer, Tank, Berserk) where they fit the recording.
 * Where they don't, the recording wins (Berserk's Walk Speed is +0.4 a level, not every 5 levels;
 * Bloodlust Damage +0.75 a level; the Lust For Blood cap +14 a level). Verify with another level
 * before trusting.
 *
 * <p>Only positive bonuses double ("All positive bonus stats are doubled if only 1 player uses this
 * class in a dungeon."): the Archer's -25% Melee Damage stays.
 */
public enum ClassBonus {
    // Healer: MCW +0.2 Vitality and +0.5 Mending a level; the bases fit level 15.
    HEALER_VITALITY(DungeonClass.HEALER, "Vitality", "&a", true, 10, l -> 0.2 * l, ""),
    HEALER_MENDING(DungeonClass.HEALER, "Mending", "&a", true, 10, l -> 0.5 * l, ""),
    /** Renew's "Grants 1.6x Mending": MCW +1% Renew Healing a level from 50%. */
    HEALER_RENEW(DungeonClass.HEALER, "Renew Healing", "&a", false, 50, l -> l, "%"),

    // Mage: MCW +5 Intelligence a level (+500 at 50), Ability Damage 5 and +1 every 5 levels, 25%
    // shorter cooldowns and 1% more every other level.
    MAGE_INTELLIGENCE(DungeonClass.MAGE, "Intelligence", "&a", true, 250, l -> 5.0 * l, ""),
    MAGE_ABILITY_DAMAGE(DungeonClass.MAGE, "Ability Damage", "&a", true, 5, l -> l / 5, "%"),
    MAGE_EFFICIENT_SPELLS(DungeonClass.MAGE, "Efficient Spells Cooldown Reduction", "&a", false, 25, l -> l / 2, "%"),

    // Berserk, in the order of the recorded solo chat.
    BERSERK_MELEE_DAMAGE(DungeonClass.BERSERK, "Melee Damage", "&c", true, 40, l -> 0.75 * l, "%"),
    BERSERK_WALK_SPEED(DungeonClass.BERSERK, "Walk Speed", "&a", true, 30, l -> 0.4 * l, ""),
    BERSERK_BLOODLUST_DAMAGE(DungeonClass.BERSERK, "Bloodlust Damage", "&a", false, 20, l -> 0.75 * l, "%"),
    BERSERK_LUST_CAP(DungeonClass.BERSERK, "Lust For Blood Damage Increase Cap", "&a", false, 250, l -> 14.0 * l, "%"),
    BERSERK_LUST_PER_HIT(DungeonClass.BERSERK, "Lust For Blood Damage Increase Per Hit", "&a", false, 15, l -> 3.0 * l, "%"),
    BERSERK_INDOMITABLE(DungeonClass.BERSERK, "Indomitable Strength to Defense", "&a", false, 5, l -> 0.1 * l, "%"),
    BERSERK_WEAPON_MASTER(DungeonClass.BERSERK, "Weapon Master Swing Range Increase", "&a", false, 0.5, l -> 0.135 * l, ""),
    BERSERK_BLOODLUST_HEAL(DungeonClass.BERSERK, "Bloodlust Heal Percent", "&a", false, 3, l -> 0, "%"),
    BERSERK_BLOODLUST_DURATION(DungeonClass.BERSERK, "Bloodlust Duration", "&a", false, 5, l -> 0, ""),

    // Archer: MCW +1.6% Arrow Damage a level (+230% at 50), Doubleshot 50% and +1% a level, Bouncy
    // Arrows +2% a level.
    ARCHER_ARROW_DAMAGE(DungeonClass.ARCHER, "Arrow Damage", "&c", true, 150, l -> 1.6 * l, "%"),
    ARCHER_MELEE_DAMAGE(DungeonClass.ARCHER, "Melee Damage", "&c", true, -25, l -> 0, "%"),
    ARCHER_DOUBLESHOT(DungeonClass.ARCHER, "Doubleshot Chance", "&a", false, 50, l -> l, "%"),

    // Tank: MCW base +100 Health, +50 Defense, +10 Vitality; +1 Defense, +0.1 Vitality and +0.2%
    // Protective Barrier Defense a level (from +25%; "+25% -> +50%" when it's the only tank).
    TANK_HEALTH(DungeonClass.TANK, "Health", "&a", true, 100, l -> 0, ""),
    TANK_DEFENSE(DungeonClass.TANK, "Defense", "&a", true, 50, l -> l, ""),
    TANK_VITALITY(DungeonClass.TANK, "Vitality", "&a", true, 10, l -> 0.1 * l, ""),
    TANK_PROTECTIVE_BARRIER(DungeonClass.TANK, "Protective Barrier Defense", "&a", false, 25, l -> 0.2 * l, "%");

    /** Levels past 50 are cosmetic. */
    public static final int MAX_LEVEL = 50;

    private static final ThreadLocal<DecimalFormat> NUMBER =
            ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.US)));

    private final DungeonClass dungeonClass;
    private final String name;
    private final String color;
    private final boolean readyUp;
    private final double base;
    private final IntToDoubleFunction levels;
    private final String unit;

    ClassBonus(DungeonClass dungeonClass, String name, String color, boolean readyUp, double base, IntToDoubleFunction levels, String unit) {
        this.dungeonClass = dungeonClass;
        this.name = name;
        this.color = color;
        this.readyUp = readyUp;
        this.base = base;
        this.levels = levels;
        this.unit = unit;
    }

    public DungeonClass dungeonClass() {
        return dungeonClass;
    }

    /** As the Ready Up menu and the solo chat name it. */
    public String displayName() {
        return name;
    }

    /** Whether the class's item in the Ready Up menu lists it. */
    public boolean readyUp() {
        return readyUp;
    }

    /** At a class level; {@code solo} when nobody else in the run plays the class (the base doubles, if it's a bonus). */
    public double value(int level, boolean solo) {
        int l = Math.clamp(level, 0, MAX_LEVEL);
        return (solo && base > 0 ? 2 * base : base) + levels.applyAsDouble(l);
    }

    /** Whether being the class's only player changes it. */
    public boolean doubles() {
        return base > 0;
    }

    /** "&7Melee Damage: &c+55%", as the Ready Up menu has it. */
    public String readyUpLine(int level) {
        double value = value(level, false);
        return "&7" + name + ": " + color + (value >= 0 ? "+" : "") + format(value) + unit;
    }

    /** "&a[Berserk] &fMelee Damage &c55%&f -> &a95%", as recorded when the run starts. */
    public String soloLine(int level) {
        return "&a[" + dungeonClass.getDisplayName() + "] &f" + name + " &c" + format(value(level, false)) + unit + "&f -> &a"
                + format(value(level, true)) + unit;
    }

    public static List<ClassBonus> of(DungeonClass dungeonClass) {
        List<ClassBonus> out = new ArrayList<>();
        for (ClassBonus bonus : values()) if (bonus.dungeonClass == dungeonClass) out.add(bonus);
        return out;
    }

    /** The Ready Up item's stat lines for a class at a level. */
    public static List<String> readyUpLines(DungeonClass dungeonClass, int level) {
        List<String> out = new ArrayList<>();
        for (ClassBonus bonus : of(dungeonClass)) if (bonus.readyUp) out.add(bonus.readyUpLine(level));
        return out;
    }

    /**
     * What a player who's the only one of their class is told as the run starts: the recorded line, then
     * a line for each bonus that doubles. The Berserk's are as recorded; the other classes' names are
     * ours (never recorded).
     */
    public static List<String> soloMessage(DungeonClass dungeonClass, int level) {
        List<String> out = new ArrayList<>();
        out.add("&6Your &a" + dungeonClass.getDisplayName() + " &6stats are doubled because you are the only player using this class!");
        for (ClassBonus bonus : of(dungeonClass)) if (bonus.doubles()) out.add(bonus.soloLine(level));
        return out;
    }

    /** "55", "3.2", "175.6", "1,000": thousands grouped, one decimal at most, none when it's whole. */
    public static String format(double value) {
        return NUMBER.get().format(BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP));
    }

    /**
     * "1.6" for 1.65 and "1.3" for 1.278: a multiplier ("Grants 1.6x Mending") to one decimal, rounding
     * the double's exact value half up, which fits both recorded ones.
     */
    public static String multiplier(double factor) {
        return new BigDecimal(factor).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }
}
