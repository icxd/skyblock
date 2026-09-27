package net.icxd.dungeons.dungeons.instance;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** How Hypixel shows secrets: the action bar's count for the room you're in, and the tab list's lines. */
final class SecretText {
    /** Recorded messages. */
    static final String ALREADY_SEARCHED = "&cThis chest has already been searched!";
    static final String LEVER_USED = "&cThis lever has already been used.";
    static final String SOMETHING_OPENING = "&cYou hear the sound of something opening...";
    static final String NO_SPACE = "&cYou don't have enough space in your inventory to pick up this item!";

    private SecretText() {
    }

    /**
     * After the mana: "          &70/6 Secrets" in a room with secrets (ten spaces, as recorded), nothing in
     * one without (the entrance, the Fairy Room, puzzles without a chest).
     */
    static String actionBar(int found, int total) {
        return total <= 0 ? "" : "          &7" + found + "/" + total + " Secrets";
    }

    /**
     * The tab list's " Secrets Found: 19%": the team's share of every secret on the floor, to one decimal
     * with ".0" left off (recorded with 21 secrets: 4.8, 9.5, 14.3, then 19 for 4).
     */
    static String percent(int found, int total) {
        if (total <= 0) return "0";
        BigDecimal share = BigDecimal.valueOf(found * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        return share.stripTrailingZeros().toPlainString();
    }

    /** "You found a Wither Essence! Everyone gains an extra essence!" (SkyHanni's; never recorded). */
    static String witherEssence(String finder) {
        return (finder == null ? "&fYou found a " : finder + " &ffound a ") + "&dWither Essence&f! Everyone gains an extra essence!";
    }
}
