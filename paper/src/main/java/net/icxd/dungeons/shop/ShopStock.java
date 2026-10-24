package net.icxd.dungeons.shop;

import org.bson.Document;

/**
 * What a profile has bought from NPC shops today, against their stock: "Stackable items sold by NPC
 * shops for Coins, that can be traded on Bazaar, have a stock of 640 items" (the wiki's Shop), the
 * recorded Farm Merchant's "640 remaining" (2026-10-24). It counts items, not trades (three Wheat a
 * time), each shop's apart (UNKNOWN: the wiki has the Farm Merchant sharing with the Windmill Operator,
 * who isn't here), the day starting at 00:00 UTC as the sell limit's (the wiki has the stock reset at
 * midnight GMT too). Kept on the profile: {@code npcStock: {day, bought: {<shop>: {<item>: n}}}}.
 */
public final class ShopStock {
    public static final String FIELD = "npcStock";
    /** A day's stock of a ware that has one. */
    public static final int DAILY = 640;

    private ShopStock() {
    }

    /** How many more of the ware's item they may buy from this shop today. */
    public static int remaining(Document profile, Shop shop, Shop.Ware ware, long now) {
        return Math.max(0, ware.stock() - bought(profile, shop, ware, now));
    }

    /** Whether there's enough left for one trade of it (always, for a ware with no stock). */
    public static boolean allows(Document profile, Shop shop, Shop.Ware ware, long now) {
        return ware.stock() <= 0 || remaining(profile, shop, ware, now) >= ware.amount();
    }

    /** One trade of it, bought now. */
    public static void record(Document profile, Shop shop, Shop.Ware ware, long now) {
        if (ware.stock() <= 0) return;
        String day = SellLimit.day(now);
        Document stock = profile.get(FIELD) instanceof Document d && day.equals(d.getString("day")) ? d : new Document("day", day);
        Document bought = stock.get("bought") instanceof Document b ? b : new Document();
        Document fromShop = bought.get(shop.name()) instanceof Document s ? s : new Document();
        fromShop.put(ware.item(), bought(profile, shop, ware, now) + ware.amount());
        bought.put(shop.name(), fromShop);
        stock.put("bought", bought);
        profile.put(FIELD, stock);
    }

    private static int bought(Document profile, Shop shop, Shop.Ware ware, long now) {
        return profile.get(FIELD) instanceof Document stock && SellLimit.day(now).equals(stock.getString("day"))
                && stock.get("bought") instanceof Document bought && bought.get(shop.name()) instanceof Document fromShop
                && fromShop.get(ware.item()) instanceof Number n ? n.intValue() : 0;
    }
}
