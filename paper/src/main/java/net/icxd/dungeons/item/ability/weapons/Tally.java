package net.icxd.dungeons.item.ability.weapons;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * An ability's hits, and the chat line that sums them up: "&7Your Giant's Sword hit &c3 &7enemies for
 * &c8,103,803 &7damage." (recorded, the total the sum of its hits' numbers), "&7Your Throwing Axe hit &c1
 * &7enemy for &c14,689,667.3 &7damage." (recorded: one decimal when it isn't whole). No line when it hit
 * nothing (none was recorded).
 */
public final class Tally {
    private static final ThreadLocal<DecimalFormat> ONE_DECIMAL =
            ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.US)));

    private int hits;
    private double total;

    public void add(double damage) {
        hits++;
        total += damage;
    }

    public int hits() {
        return hits;
    }

    public double total() {
        return total;
    }

    /** "Your {@code name} hit ..." for these hits; null for none. */
    public String message(String name) {
        return hits == 0 ? null : message(name, hits, total);
    }

    public static String message(String name, int hits, double total) {
        return "&7Your " + name + " hit &c" + hits + " &7" + (hits == 1 ? "enemy" : "enemies") + " for &c"
                + ONE_DECIMAL.get().format(total) + " &7damage.";
    }
}
