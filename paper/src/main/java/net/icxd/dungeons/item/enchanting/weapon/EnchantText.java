package net.icxd.dungeons.item.enchanting.weapon;

import net.icxd.dungeons.item.enchanting.EnchantmentData;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The numbers in an enchantment level's text, which is what Hypixel shows ("&7Deals &a5% &7of your damage dealt to
 * other monsters within &a3.3 &7blocks of the target." is 5 and 3.3), so what an enchantment does follows its book
 * (the private data's hex/enchantments.json), not a table here. Each enchantment reads its numbers by where they
 * stand in its text (ENCHANTS_WEAPONS.md lists them; PrivateWeaponTextTest checks every level against the data).
 * Read once a level and kept until another table comes in. No Bukkit; main thread.
 */
public final class EnchantText {
    /** "5", "3.3", "1,000", "50k", "1.5m", "1M"; a "k" or "m" is a thousand or a million unless a word goes on ("5 more"). */
    private static final Pattern NUMBER = Pattern.compile("(\\d[\\d,]*(?:\\.\\d+)?)([kKmM](?![A-Za-z]))?");
    private static final Pattern COLOUR = Pattern.compile("[&§][0-9a-fk-orA-FK-OR]");
    private static final double[] NONE = new double[0];
    /** The levels past which nothing has text are looked for up to here. */
    private static final int MAX_LEVEL = 20;

    private static EnchantmentData cachedFor;
    private static double[][][] cache = new double[WeaponEnchant.values().length][][];

    private EnchantText() {
    }

    /** Every number in the text, in order, without its colours; none for null. */
    public static double[] numbers(String text) {
        if (text == null) return NONE;
        Matcher m = NUMBER.matcher(COLOUR.matcher(text).replaceAll(""));
        double[] found = new double[8];
        int n = 0;
        while (m.find()) {
            if (n == found.length) found = Arrays.copyOf(found, n * 2);
            found[n++] = number(m.group(1), m.group(2));
        }
        return Arrays.copyOf(found, n);
    }

    /** "50k" is 50,000, "1.5m" 1,500,000, "1,000" 1,000. */
    static double number(String digits, String suffix) {
        double value = Double.parseDouble(digits.replace(",", ""));
        if (suffix == null || suffix.isEmpty()) return value;
        return value * (Character.toLowerCase(suffix.charAt(0)) == 'k' ? 1_000 : 1_000_000);
    }

    /** The first number in a tier's "&850k Combat XP to tier up!": what the count must reach for the next tier; 0 for none. */
    public static double tierUp(String text) {
        double[] numbers = numbers(text);
        return numbers.length == 0 ? 0 : numbers[0];
    }

    /** The numbers of this level's text (none if it has none). */
    public static double[] of(WeaponEnchant enchant, int level) {
        EnchantmentData data = EnchantmentData.current();
        if (data != cachedFor) {
            cachedFor = data;
            cache = new double[WeaponEnchant.values().length][][];
        }
        if (level < 1 || level > MAX_LEVEL) return NONE;
        double[][] levels = cache[enchant.ordinal()];
        if (levels == null) levels = cache[enchant.ordinal()] = new double[MAX_LEVEL + 1][];
        double[] numbers = levels[level];
        if (numbers == null) {
            EnchantmentData.Entry entry = data.get(enchant.id());
            EnchantmentData.Level at = entry == null ? null : entry.level(level);
            numbers = levels[level] = at == null ? NONE : numbers(at.text());
        }
        return numbers;
    }

    /** Its {@code index}th number at this level; 0 if it has no such number (no text, no table). */
    public static double at(WeaponEnchant enchant, int level, int index) {
        double[] numbers = of(enchant, level);
        return index < numbers.length ? numbers[index] : 0;
    }

    /**
     * The same for an amount that grows by the level (Life Steal's 2.4, 4.8, ... 14.4; Drain's; Vampirism's), at a
     * level that may be past the texts (the Blood-Soaked reforge's one more on the highest: Life Steal VII): there, the
     * highest level with text's, times the level over its level (Life Steal VII 16.8, the wiki's history).
     */
    public static double linear(WeaponEnchant enchant, int level, int index) {
        if (level < 1) return 0;
        if (of(enchant, level).length > index) return at(enchant, level, index);
        for (int below = Math.min(level - 1, MAX_LEVEL); below >= 1; below--) {
            double[] numbers = of(enchant, below);
            if (numbers.length > index) return numbers[index] * level / below;
        }
        return 0;
    }

    /** The tier-up text's number at this level (what its count must reach to go up one); 0 at the last tier, or none. */
    public static double tierUp(WeaponEnchant enchant, int level) {
        EnchantmentData.Entry entry = EnchantmentData.current().get(enchant.id());
        EnchantmentData.Level at = entry == null ? null : entry.level(level);
        return at == null ? 0 : tierUp(at.tierUp());
    }

    /** Whether it has text at this level (the table has it). */
    public static boolean hasLevel(WeaponEnchant enchant, int level) {
        EnchantmentData.Entry entry = EnchantmentData.current().get(enchant.id());
        return entry != null && entry.level(level) != null;
    }
}
