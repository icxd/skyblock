package net.icxd.dungeons.economy;

import java.math.BigDecimal;

import org.bson.Document;

import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.user.User;

/**
 * The coins in a player's purse: {@code coins} on the profile they play on (each profile has its
 * own, as on Hypixel). Everything that gives or takes purse coins goes through here: mob kills,
 * NPC shops, upgrade costs, skill rewards. Coins can have fractions, as Hypixel's can (a Torch sells
 * for 0.3), so the purse is a double.
 *
 * <p>Nothing here saves: the profile is saved with the rest of their data, as their items are (see
 * UserStore), so a sale's coins and the item it took are saved together. Main thread.
 */
public final class Purse {
    public static final String FIELD = "coins";

    private Purse() {
    }

    /** What's in their purse, on the profile they play on. */
    public static double coins(User user) {
        return coins(user.profile());
    }

    /**
     * Gives them coins (a kill, a sale, a reward), straight into their purse, with no message: the
     * sidebar shows the change.
     *
     * @throws IllegalArgumentException for a negative amount, or one that isn't a number
     */
    public static void add(User user, double coins) {
        add(user.profile(), coins);
    }

    /** Whether they have this many coins in their purse. */
    public static boolean has(User user, double coins) {
        return has(user.profile(), coins);
    }

    /**
     * Takes coins from their purse, if it has them all; otherwise nothing's taken.
     *
     * @return whether they were taken
     * @throws IllegalArgumentException for a negative amount, or one that isn't a number
     */
    public static boolean take(User user, double coins) {
        return take(user.profile(), coins);
    }

    // The same on a profile document.

    /** A purse saved before it held fractions is a whole number; any number reads. */
    public static double coins(Document profile) {
        return profile != null && profile.get(FIELD) instanceof Number n ? n.doubleValue() : 0;
    }

    public static void add(Document profile, double coins) {
        check(coins);
        profile.put(FIELD, sum(coins(profile), coins));
    }

    public static boolean has(Document profile, double coins) {
        check(coins);
        return coins(profile) >= coins;
    }

    public static boolean take(Document profile, double coins) {
        if (!has(profile, coins)) return false;
        profile.put(FIELD, sum(coins(profile), -coins));
        return true;
    }

    /**
     * Makes every profile's purse a double: purses were whole numbers (an int) before. Reading works
     * either way; this is so {@code /pd set coins} takes fractions.
     *
     * @return whether any changed
     */
    public static boolean migrate(Document doc) {
        boolean changed = false;
        for (Profiles.Entry entry : Profiles.ordered(doc)) {
            Object coins = entry.profile().get(FIELD);
            if (coins instanceof Number n && !(coins instanceof Double)) {
                entry.profile().put(FIELD, n.doubleValue());
                changed = true;
            }
        }
        return changed;
    }

    /** a + b in decimal, so adding 0.1 ten times makes 1 and not 0.9999999999999999. */
    static double sum(double a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).doubleValue();
    }

    private static void check(double coins) {
        if (!(coins >= 0) || Double.isInfinite(coins)) throw new IllegalArgumentException("Not an amount of coins: " + coins);
    }
}
