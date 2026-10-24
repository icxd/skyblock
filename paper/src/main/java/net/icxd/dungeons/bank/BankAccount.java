package net.icxd.dungeons.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;

import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.utils.SkyBlockTime;

/**
 * A profile's bank account, as plain functions on its document (no server needed): {@code bank} on the
 * profile, its {@code balance} and its last {@link #KEPT} {@code transactions}, oldest first, each
 * {@code {player, timestamp, amount, type}}. Coins go in as they are (half a 20.5 purse is 10.25) and
 * come out whole (half of 20.5 is 10), as recorded (2026-10-24): every withdrawal the recording made,
 * and each the menus offered, was a whole number of coins, the 0.5 staying in the bank.
 *
 * <p>Interest is due at the start of every season (the Banker, and the menu's countdown, which matched
 * these to the second), but isn't paid: no recording has a season starting, so what it says and how it
 * shows in the transactions is UNKNOWN. Nothing here saves: the profile is saved with the rest of their
 * data, as the purse is.
 */
public final class BankAccount {
    public static final String KEY = "bank";
    /** "Balance limit: 50 Million", a new profile's (the Starter Account's: there are no upgrades here). */
    public static final double LIMIT = 50_000_000;
    /** "The last 10 transactions" (the wiki's Bank). */
    public static final int KEPT = 10;
    /** A season, three SkyBlock months: "every 31 hours". */
    public static final long SEASON_MS = 3 * SkyBlockTime.MONTH_MS;

    private BankAccount() {
    }

    public enum Type {
        DEPOSIT, WITHDRAW
    }

    /** One, as the menu lists it: what, how much, when, and who (the player who made it). */
    public record Transaction(Type type, double amount, long at, UUID by) {
    }

    /** Why nothing went in or came out. */
    public enum Refusal {
        /** Nothing to move: an empty purse, or an account with less than a coin. */
        TOO_LITTLE,
        /** More than the purse has (a deposit) or the account (a withdrawal). */
        NOT_ENOUGH,
        /** The account is at its limit. */
        FULL
    }

    /** What a deposit or withdrawal did: the coins moved, or why none were (null if they were). */
    public record Result(double amount, Refusal refusal) {
        static Result refused(Refusal why) {
            return new Result(0, why);
        }

        public boolean done() {
            return refusal == null;
        }
    }

    // The account

    private static Document bank(Document profile) {
        if (profile.get(KEY) instanceof Document bank) return bank;
        Document bank = new Document("balance", 0.0).append("transactions", new ArrayList<Document>());
        profile.put(KEY, bank);
        return bank;
    }

    /** What's in it (a new profile's was saved as a whole 0). */
    public static double balance(Document profile) {
        return profile != null && profile.get(KEY) instanceof Document bank && bank.get("balance") instanceof Number n ? n.doubleValue() : 0;
    }

    /** Its transactions, newest first (at most {@link #KEPT}); any that can't be read are left out. */
    public static List<Transaction> recent(Document profile) {
        List<Transaction> recent = new ArrayList<>();
        if (profile == null || !(profile.get(KEY) instanceof Document bank) || !(bank.get("transactions") instanceof List<?> list)) return recent;
        for (Object o : list) {
            if (!(o instanceof Document d) || !(d.get("amount") instanceof Number amount) || !(d.get("timestamp") instanceof Number at)) continue;
            Type type;
            try {
                type = Type.valueOf(d.getString("type"));
            } catch (RuntimeException e) {
                continue;
            }
            UUID by = null;
            try {
                if (d.getString("player") != null) by = UUID.fromString(d.getString("player"));
            } catch (IllegalArgumentException ignored) {
                // Who doesn't matter to the list.
            }
            recent.add(0, new Transaction(type, amount.doubleValue(), at.longValue(), by));
        }
        return recent.size() > KEPT ? new ArrayList<>(recent.subList(0, KEPT)) : recent;
    }

    /**
     * Puts coins from the purse in: all of {@code amount} if there's room for it, or what fits under the
     * limit (UNKNOWN on Hypixel, which may refuse it all instead).
     */
    public static Result deposit(Document profile, double amount, UUID by, long now) {
        if (!(amount > 0) || Double.isInfinite(amount)) return Result.refused(Refusal.TOO_LITTLE);
        if (!Purse.has(profile, amount)) return Result.refused(Refusal.NOT_ENOUGH);
        double balance = balance(profile);
        double room = sum(LIMIT, -balance);
        if (room <= 0) return Result.refused(Refusal.FULL);
        double in = Math.min(amount, room);
        Purse.take(profile, in);
        record(profile, sum(balance, in), Type.DEPOSIT, in, by, now);
        return new Result(in, null);
    }

    /** Takes whole coins out into the purse: {@code amount}, less any fraction of a coin. */
    public static Result withdraw(Document profile, double amount, UUID by, long now) {
        double out = whole(amount);
        if (!(out > 0)) return Result.refused(Refusal.TOO_LITTLE);
        double balance = balance(profile);
        if (out > balance) return Result.refused(Refusal.NOT_ENOUGH);
        Purse.add(profile, out);
        record(profile, sum(balance, -out), Type.WITHDRAW, out, by, now);
        return new Result(out, null);
    }

    private static void record(Document profile, double balance, Type type, double amount, UUID by, long now) {
        Document bank = bank(profile);
        bank.put("balance", balance);
        List<Object> transactions = bank.get("transactions") instanceof List<?> list ? new ArrayList<>(list) : new ArrayList<>();
        transactions.add(new Document("player", by == null ? null : by.toString()).append("timestamp", now).append("amount", amount)
                .append("type", type.name()));
        while (transactions.size() > KEPT) transactions.remove(0);
        bank.put("transactions", transactions);
    }

    // What the menus offer

    /** "Your whole purse". */
    public static double wholePurse(Document profile) {
        return Purse.coins(profile);
    }

    /** "Half your purse": exactly half (10.25 of 20.5). */
    public static double halfPurse(Document profile) {
        return BigDecimal.valueOf(Purse.coins(profile)).divide(BigDecimal.valueOf(2)).doubleValue();
    }

    /** "Everything in the account": its whole coins (20 of 20.5). */
    public static double everything(Document profile) {
        return whole(balance(profile));
    }

    /** "Half the account": whole coins (7 of 15.5). */
    public static double half(Document profile) {
        return whole(BigDecimal.valueOf(balance(profile)).divide(BigDecimal.valueOf(2)).doubleValue());
    }

    /** "Withdraw 20%": whole coins (4 of 20.5, 1 of 8.5). */
    public static double fifth(Document profile) {
        return whole(BigDecimal.valueOf(balance(profile)).multiply(BigDecimal.valueOf(0.2)).doubleValue());
    }

    // Interest

    /** When interest is next due: the next season's start. */
    public static long nextInterest(long now) {
        return SkyBlockTime.EPOCH + (Math.floorDiv(now - SkyBlockTime.EPOCH, SEASON_MS) + 1) * SEASON_MS;
    }

    private static double whole(double coins) {
        return Double.isFinite(coins) ? BigDecimal.valueOf(coins).setScale(0, RoundingMode.FLOOR).doubleValue() : 0;
    }

    /** a + b in decimal, as the purse adds. */
    private static double sum(double a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).doubleValue();
    }
}
