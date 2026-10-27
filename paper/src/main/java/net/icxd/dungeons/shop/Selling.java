package net.icxd.dungeons.shop;

import java.math.BigDecimal;

import net.icxd.dungeons.economy.Coins;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Soulbound;

/**
 * Selling to an NPC shop: every shop pays the same, the item's NPC sell price (items.json's
 * {@code npc_sell_price}) for each one in the stack, fractions and all (research coins.md 2.2).
 */
public final class Selling {
    // The sale line is recorded (2026-10-24). Not verified: Hypixel's colours for the purchase and limit
    // lines aren't known (research coins.md 6). Their words are as SkyHanni matches them with the colours
    // taken out; the colours are in the style of the sale's. The refusals below it are this plugin's words.
    static final String LIMIT_REACHED = "&cYou've reached the daily limit of coins you may earn from NPC shops.";
    static final String OUT_OF_STOCK = "&cThis item is out of stock for today!";
    static final String NOT_ENOUGH_COINS = "&cYou don't have enough coins!";
    static final String NOT_ENOUGH_ITEMS = "&cYou don't have the required items!";
    static final String NOT_ENOUGH_ESSENCE = "&cYou don't have enough essence!";
    static final String FULL = "&cYour inventory is full!";

    private Selling() {
    }

    /**
     * Whether a shop takes it: only items with a sell price, not soulbound ones (Hypixel's rule for them
     * isn't known), nor the SkyBlock Menu. Dungeon items sell (not known either; many have a price).
     */
    public static boolean sellable(SkyBlockItem item) {
        return item != null && item.npcSellPrice() > 0 && item.soulbound() == Soulbound.NONE && !"SKYBLOCK_MENU".equals(item.id());
    }

    /** What a stack sells for: the price of one, times how many. */
    public static double total(double price, int amount) {
        return BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(amount)).doubleValue();
    }

    /**
     * "&aYou sold &fCobblestone&8 x20 &afor &620 Coins&a!", as recorded ("x1" for one, as Hypixel writes
     * it in SkyHanni's logs).
     */
    static String sold(String name, int amount, double total) {
        return "&aYou sold " + name + "&8 x" + amount + " &afor &6" + Coins.format(total) + " Coins&a!";
    }

    /**
     * "&aYou bought &fRookie Hoe &afor &610 Coins&a!", "&aYou bought &fExperience Bottle&8 x8 &afor &6240 Coins&a!", as
     * recorded (2026-10-24, 2026-10-25); with no coins, "&aYou bought Superboom TNT!" (UNKNOWN).
     */
    static String bought(String name, int amount, double coins) {
        return "&aYou bought " + name + (amount > 1 ? "&8 x" + amount : "") + (coins > 0 ? " &afor &6" + Coins.format(coins) + " Coins&a!" : "&a!");
    }
}
