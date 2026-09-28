package net.icxd.dungeons.item.ability.utility;

import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * The numbers in an ability's text, as items.json has it ("&7Grants &f+100✦ Speed &7for &a30s&7."): the
 * text is Hypixel's, so what an ability does follows what its item says, whatever the item (a Weirder Tuba's
 * Howl lasts the 30 seconds its text gives). The text is read without its colour codes, all lines as one.
 * Public, so bonuses and enchantments read their numbers from their text the same way (EFFECTS.md).
 */
public final class AbilityText {
    /** "+30❁ Strength", "+2.5▚ Gemstone Spread": a number, then a symbol and a name. */
    private static final Pattern STAT = Pattern.compile("\\+([\\d,]+(?:\\.\\d+)?)%?\\s*(\\S)\\s*([A-Z][A-Za-z']*(?: [A-Z][A-Za-z']*)*)");
    /** "for 20 seconds", "for 5s", "for 1m", "for 1.5s". */
    private static final Pattern FOR = Pattern.compile("(?i)for (?:the next )?([\\d.]+)\\s*(s|secs?|seconds?|m|mins?|minutes?)\\b");
    /** "within 18 blocks", "in a 10 block radius", "in a 10 block range". */
    private static final Pattern BLOCKS = Pattern.compile("(?i)(?:within|in an?) ([\\d.]+) blocks?");
    /** "up to 5 players", "You and 4 nearby players". */
    private static final Pattern PLAYERS = Pattern.compile("(?i)(?:up to|and) (\\d+) (?:nearby )?players");

    private AbilityText() {
    }

    /** The lines as one, without colour codes and with single spaces. */
    public static String plain(List<String> lines) {
        return String.join(" ", lines).replaceAll("[&§][0-9a-fk-orA-FK-OR]", "").replaceAll("\\s+", " ").trim();
    }

    /**
     * Every "+30❁ Strength" in it, by the stat's symbol and name ("+10☠ Crit Damage"); the Rift's own
     * stats, whose names are the same as others', never. A number with a "%" is its percent ("+5% ..."
     * counts 5).
     */
    public static Stats stats(String plain) {
        Stats stats = new Stats();
        Matcher m = STAT.matcher(plain);
        while (m.find()) {
            Stat stat = stat(m.group(2), m.group(3));
            if (stat != null) stats.add(stat, number(m.group(1)));
        }
        return stats;
    }

    /** The stat with this symbol whose name starts the words after it ("Strength &7for" is Strength). */
    private static Stat stat(String symbol, String words) {
        Stat best = null;
        for (Stat stat : Stat.values()) {
            if (stat.name().startsWith("RIFT_") || !stat.getSymbol().equals(symbol)) continue;
            String name = stat.getDisplayName();
            if (!(words.equals(name) || words.startsWith(name + " "))) continue;
            // "Mining Speed" over "Mining" if there were one: the longest name that fits.
            if (best == null || name.length() > best.getDisplayName().length()) best = stat;
        }
        return best;
    }

    /** How long, in milliseconds: the first "for 20 seconds" in it. */
    public static OptionalDouble millis(String plain) {
        Matcher m = FOR.matcher(plain);
        if (!m.find()) return OptionalDouble.empty();
        double amount = Double.parseDouble(m.group(1));
        return OptionalDouble.of(amount * (m.group(2).toLowerCase().startsWith("m") ? 60_000 : 1_000));
    }

    /** How far, in blocks: the first "within 18 blocks" in it. */
    public static OptionalDouble blocks(String plain) {
        Matcher m = BLOCKS.matcher(plain);
        return m.find() ? OptionalDouble.of(Double.parseDouble(m.group(1))) : OptionalDouble.empty();
    }

    /** How many players: the first "up to 5 players" or "You and 4 nearby players" in it. */
    public static OptionalDouble players(String plain) {
        Matcher m = PLAYERS.matcher(plain);
        return m.find() ? OptionalDouble.of(Double.parseDouble(m.group(1))) : OptionalDouble.empty();
    }

    /** The first number after {@code before} in it ("Heal for " in "Heal for 1,000❤."), commas and all. */
    public static OptionalDouble after(String plain, String before) {
        Matcher m = Pattern.compile(Pattern.quote(before) + "\\s*\\+?([\\d,]+(?:\\.\\d+)?)").matcher(plain);
        return m.find() ? OptionalDouble.of(number(m.group(1))) : OptionalDouble.empty();
    }

    private static double number(String digits) {
        return Double.parseDouble(digits.replace(",", ""));
    }
}
