package net.icxd.dungeons.dungeons.instance;

import static net.icxd.dungeons.stats.Stat.CRIT_DAMAGE;
import static net.icxd.dungeons.stats.Stat.DAMAGE;
import static net.icxd.dungeons.stats.Stat.DEFENSE;
import static net.icxd.dungeons.stats.Stat.HEALTH;
import static net.icxd.dungeons.stats.Stat.HEALTH_REGEN;
import static net.icxd.dungeons.stats.Stat.INTELLIGENCE;
import static net.icxd.dungeons.stats.Stat.SPEED;
import static net.icxd.dungeons.stats.Stat.STRENGTH;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonLevels;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Dungeon Blessings: stat boosts for the whole team for the rest of the run (the wiki's Dungeon
 * Blessing page, every number checked against the recorded chat). Each level gives a flat amount and a
 * share of the stat: the flat amounts go on first, then the stat is multiplied, so they scale with
 * whatever the player wears. Blessings of the same kind add up by level (Wisdom V, V and II show as
 * Wisdom XII, and 5,238 health with Life VI became 6,181: x1.18, not x1.15 x1.03).
 *
 * <p>Mobs and puzzles give level V, secrets I or II. On Floor III and up they're 20% stronger.
 * Declared in the order Hypixel's tab list footer lists them (recorded: Power, Wisdom, Stone, Life
 * whatever order they were found in); Time was never recorded, so it's last.
 */
public enum Blessing {
    POWER("Power", new Effect(STRENGTH, 4, 2), new Effect(CRIT_DAMAGE, 4, 2)),
    WISDOM("Wisdom", new Effect(INTELLIGENCE, 4, 2), new Effect(SPEED, 4, 0)),
    STONE("Stone", new Effect(DEFENSE, 4, 2), new Effect(DAMAGE, 6, 0)),
    LIFE("Life", new Effect(HEALTH, 0, 3), new Effect(HEALTH_REGEN, 0, 3)),
    /** Only from the Quiz (Floor IV and up). */
    TIME("Time", new Effect(HEALTH, 4, 2), new Effect(DEFENSE, 4, 2), new Effect(INTELLIGENCE, 4, 2), new Effect(STRENGTH, 4, 2));

    /** What one level gives: {@code flat} of the stat, and {@code percent} % of it. */
    public record Effect(Stat stat, double flat, double percent) {
    }

    /** The ones rooms, secrets, bats and puzzles give (Time comes only from the Quiz). */
    public static final List<Blessing> FOUND = List.of(POWER, WISDOM, STONE, LIFE);

    private final String kind;
    private final List<Effect> effects;

    Blessing(String kind, Effect... effects) {
        this.kind = kind;
        this.effects = List.of(effects);
    }

    public List<Effect> effects() {
        return effects;
    }

    /** "Wisdom" or "Blessing of Wisdom" (any case); null for neither. */
    public static Blessing named(String name) {
        if (name == null) return null;
        String kind = name.strip();
        if (kind.regionMatches(true, 0, "Blessing of ", 0, 12)) kind = kind.substring(12).strip();
        for (Blessing blessing : values()) {
            if (blessing.kind.equalsIgnoreCase(kind)) return blessing;
        }
        return null;
    }

    /** "Blessing of Stone". */
    public String displayName() {
        return "Blessing of " + kind;
    }

    /** "Blessing of Stone I". */
    public String displayName(int level) {
        return displayName() + " " + DungeonLevels.roman(level);
    }

    /** How much stronger blessings are on this floor: 20% from Floor III (Master Mode as its normal floor). */
    public static double strength(DungeonFloor floor) {
        return floor.getNumber() >= 3 ? 1.2 : 1;
    }

    /**
     * The stats with these blessings (kind to level): every blessing's flat amounts first, then each
     * affected stat times each blessing's (1 + its share). Whether two kinds on one stat (Time and Life on
     * health) multiply or add is UNKNOWN (the wiki says the percentages multiply); it can't happen before
     * Floor IV.
     */
    public static void apply(Stats stats, Map<Blessing, Integer> levels, double strength) {
        Map<Stat, Double> flat = new EnumMap<>(Stat.class);
        Map<Stat, Double> times = new EnumMap<>(Stat.class);
        for (Map.Entry<Blessing, Integer> e : levels.entrySet()) {
            int level = e.getValue();
            if (level <= 0) continue;
            for (Effect effect : e.getKey().effects) {
                flat.merge(effect.stat(), level * effect.flat() * strength, Double::sum);
                times.merge(effect.stat(), 1 + level * effect.percent() / 100 * strength, (a, b) -> a * b);
            }
        }
        for (Map.Entry<Stat, Double> e : times.entrySet()) {
            Stat stat = e.getKey();
            stats.set(stat, (stats.get(stat) + flat.getOrDefault(stat, 0.0)) * e.getValue());
        }
    }

    /**
     * What finding one at this level grants, as Hypixel's chat says it under "DUNGEON BUFF!", five spaces
     * in (recorded for all four found on the Entrance, e.g. Stone I: "Granted you +4 & +1.02x ❈ Defense and
     * +6 ❁ Damage."). Time's line is SkyHanni's pattern, never recorded, and its numbers' form is a guess.
     */
    public List<String> granted(int level, double strength) {
        return switch (this) {
            case POWER -> List.of(
                    "     &7Granted you &a&a+" + flat(0, level, strength) + "&7 & &a+" + times(0, level, strength) + "x " + STRENGTH.label() + "&7.",
                    "     &7Also granted you &a&a+" + flat(1, level, strength) + "&7 & &a+" + times(1, level, strength) + "x "
                            + CRIT_DAMAGE.label() + "&7.");
            case WISDOM -> List.of("     &7Granted you &a&a+" + flat(0, level, strength) + "&7 & &a+" + times(0, level, strength) + "x "
                    + INTELLIGENCE.label() + " &7and &a+" + flat(1, level, strength) + " " + SPEED.label() + "&7.");
            case STONE -> List.of("     &7Granted you &a&a+" + flat(0, level, strength) + "&7 & &a+" + times(0, level, strength) + "x "
                    + DEFENSE.label() + " &7and &a+" + flat(1, level, strength) + " " + DAMAGE.label() + "&7.");
            case LIFE -> List.of("     &7Granted you &a&a+" + times(0, level, strength) + "x HP &7and &a&a+" + times(1, level, strength) + "x "
                    + HEALTH_REGEN.label() + "&7.");
            case TIME -> List.of("     &7Granted you &a+" + both(0, level, strength) + " HP&7, &a+" + both(1, level, strength) + " Defense&7, &a+"
                    + both(2, level, strength) + " Intelligence&7, and &a+" + both(3, level, strength) + " Strength&7.");
        };
    }

    /**
     * "DUNGEON BUFF! You found a Blessing of Wisdom V! (14s)": to the finder ({@code finder} null), and to
     * everyone else with the finder's rank and name in place of "You" (SkyHanni's pattern; never recorded,
     * the runs were solo). {@code elapsed} is the run's time for a picked up one ("14s", "01m 22s"), null
     * for a chest's.
     */
    public String found(String finder, int level, String elapsed) {
        return "&6&lDUNGEON BUFF! " + (finder == null ? "&fYou found a " : finder + " &ffound a ") + "&d" + displayName(level) + "&f!"
                + (elapsed == null ? "" : " (&a" + elapsed + "&f)");
    }

    /** The run's time in a picked up one's line: "14s", "01m 22s" (whole seconds, as recorded). */
    static String elapsed(long millis) {
        return RunText.elapsed(Math.max(0, millis - millis % 1000));
    }

    /**
     * A kind for a room's, a secret's or a puzzle's blessing. Which kind Hypixel gives is UNKNOWN (recorded:
     * Wisdom three times, Power, Stone and Life from rooms and the Water Board, Stone, Wisdom and Life from
     * chests), so each of the four is as likely.
     */
    public static Blessing random(RandomGenerator random) {
        return FOUND.get(random.nextInt(FOUND.size()));
    }

    private String flat(int effect, int level, double strength) {
        return number(level * effects.get(effect).flat() * strength);
    }

    private String times(int effect, int level, double strength) {
        return number(1 + level * effects.get(effect).percent() / 100 * strength);
    }

    private String both(int effect, int level, double strength) {
        return flat(effect, level, strength) + " & +" + times(effect, level, strength) + "x";
    }

    /** "20", "1.1", "1.02": at most two decimals, none that are 0 (as recorded: +1.1x, +1.04x). */
    static String number(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        if (text.contains(".")) text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        return text;
    }
}
