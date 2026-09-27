package net.icxd.dungeons.shop;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.bson.Document;

/**
 * NPC shops pay a profile at most 500M coins a day, the day starting at 00:00 UTC (the wiki's Shops,
 * "Sell Limit"; 200M until 2025/Oct 15). Kept on the profile: {@code npcSales: {day, coins}}.
 */
public final class SellLimit {
    public static final String FIELD = "npcSales";
    public static final double DAILY = 500_000_000;

    private SellLimit() {
    }

    /** Coins NPC shops have paid them today. */
    public static double soldToday(Document profile, long now) {
        return profile.get(FIELD) instanceof Document sales && day(now).equals(sales.getString("day"))
                && sales.get("coins") instanceof Number coins ? coins.doubleValue() : 0;
    }

    /** Whether a sale for this much keeps them within today's limit. */
    public static boolean allows(Document profile, double coins, long now) {
        return soldToday(profile, now) + coins <= DAILY;
    }

    /** A sale for this much, now. */
    public static void record(Document profile, double coins, long now) {
        double today = BigDecimal.valueOf(soldToday(profile, now)).add(BigDecimal.valueOf(coins)).doubleValue();
        profile.put(FIELD, new Document("day", day(now)).append("coins", today));
    }

    /** "2026-09-27", the UTC day. */
    static String day(long now) {
        return LocalDate.ofInstant(Instant.ofEpochMilli(now), ZoneOffset.UTC).toString();
    }
}
