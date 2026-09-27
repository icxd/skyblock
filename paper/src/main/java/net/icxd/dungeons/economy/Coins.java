package net.icxd.dungeons.economy;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** How Hypixel writes an amount of coins, which depends on where (research coins.md 1.1). */
public final class Coins {
    private static final ThreadLocal<DecimalFormat> WITH_TENTHS = format("#,##0.#", RoundingMode.HALF_EVEN);
    private static final ThreadLocal<DecimalFormat> WHOLE = format("#,##0", RoundingMode.FLOOR);
    private static final ThreadLocal<DecimalFormat> BELOW_ONE = format("0.0", RoundingMode.FLOOR);

    private Coins() {
    }

    private static ThreadLocal<DecimalFormat> format(String pattern, RoundingMode rounding) {
        return ThreadLocal.withInitial(() -> {
            DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
            format.setRoundingMode(rounding);
            return format;
        });
    }

    /**
     * "79,208,878.1", "557,306", "0": menus and chat (the profile menu's Purse Coins, a sale's total),
     * at most one decimal.
     */
    public static String format(double coins) {
        return WITH_TENTHS.get().format(coins);
    }

    /**
     * The sidebar's purse: a whole number ("79,208,878" while the profile menu says 79,208,878.1), but
     * "0.0" for a new profile's empty purse. Those are the only two cases recorded; that anything
     * under 1 shows a decimal ("0.3") is this plugin's guess from the "0.0".
     */
    public static String sidebar(double coins) {
        return coins < 1 ? BELOW_ONE.get().format(Math.max(0, coins)) : WHOLE.get().format(coins);
    }

    /** "+5", "-1,200", "+0.3": a change, as the sidebar's "(+5)" has it. */
    public static String signed(double coins) {
        return (coins < 0 ? "" : "+") + format(coins);
    }
}
