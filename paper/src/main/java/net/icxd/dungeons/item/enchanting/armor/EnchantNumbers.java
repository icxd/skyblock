package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.enchanting.EnchantmentData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The numbers in an enchantment's text at a level, in the order the text has them, so what an enchantment does
 * follows what its book says (Thorns II's "Grants a 50% chance to rebound 6% of damage dealt back at the
 * attacker." is 50 and 6). The text is read without its colour codes ("&82 S runs" is 2, not 82). Kept until the
 * table is read again. Main thread.
 */
public final class EnchantNumbers {
    /** "50", "2.5", "1,000", ".5". */
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?|\\.\\d+");
    private static final double[] NONE = new double[0];
    /** By enchantment id, then by level (index 0 unused); a level with no text has none. */
    private static final Map<String, double[][]> CACHE = new HashMap<>();
    private static EnchantmentData cachedFor;

    private EnchantNumbers() {
    }

    /** Every number in the enchantment's text at this level; none if the table has no text for it. */
    public static double[] of(String id, int level) {
        EnchantmentData data = EnchantmentData.current();
        if (data != cachedFor) {
            CACHE.clear();
            cachedFor = data;
        }
        double[][] levels = CACHE.get(id);
        if (levels == null) {
            levels = read(data.get(id));
            CACHE.put(id, levels);
        }
        return level >= 1 && level < levels.length ? levels[level] : NONE;
    }

    /** Its {@code index}th number at this level (from 0); 0 if there's no such number. */
    public static double get(String id, int level, int index) {
        double[] numbers = of(id, level);
        return index < numbers.length ? numbers[index] : 0;
    }

    /** The numbers in its "tier up" line at this level ("&82 S runs to tier up!" is 2); none for the last tier. */
    public static double[] tierUp(String id, int level) {
        EnchantmentData.Entry entry = EnchantmentData.current().get(id);
        EnchantmentData.Level at = entry == null ? null : entry.level(level);
        return at == null || at.tierUp() == null ? NONE : numbers(at.tierUp());
    }

    private static double[][] read(EnchantmentData.Entry entry) {
        if (entry == null) return new double[0][];
        double[][] levels = new double[entry.max() + 1][];
        for (int level = 1; level <= entry.max(); level++) {
            EnchantmentData.Level at = entry.level(level);
            levels[level] = at == null ? NONE : numbers(at.text());
        }
        return levels;
    }

    /** Every number in a text, colour codes left out. */
    public static double[] numbers(String text) {
        if (text == null) return NONE;
        Matcher m = NUMBER.matcher(AbilityText.plain(List.of(text)));
        List<Double> found = new ArrayList<>();
        while (m.find()) found.add(Double.parseDouble(m.group().replace(",", "")));
        double[] numbers = new double[found.size()];
        for (int i = 0; i < numbers.length; i++) numbers[i] = found.get(i);
        return numbers;
    }
}
