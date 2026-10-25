package net.icxd.dungeons.storage;

import org.bson.Document;

/**
 * Jacobus's Accessory Bag upgrades (the wiki's Jacobus): each one 2 more slots for coins, 99 at most. The first costs
 * 1,500,000, then 5M each to the 5th, 8M to the 10th, 12M to the 20th and 20M to the 99th, 1,761,500,000 in all. The
 * profile keeps how many it bought ({@code accessoryBagUpgrades}, the API's {@code bag_upgrades_purchased}). No
 * server needed.
 */
public final class BagUpgrades {
    public static final String FIELD = "accessoryBagUpgrades";
    public static final int MOST = 99;
    public static final int SLOTS = 2;
    /** The upgrade the Jacobus Register comes with: "buying 10 Accessory Bag slots" (the wiki's Jacobus Register). */
    public static final int REGISTER = 5;

    private BagUpgrades() {
    }

    public static int bought(Document profile) {
        return profile != null && profile.get(FIELD) instanceof Number n ? Math.clamp(n.intValue(), 0, MOST) : 0;
    }

    public static void bought(Document profile, int upgrades) {
        profile.put(FIELD, Math.clamp(upgrades, 0, MOST));
    }

    /** The slots they've bought. */
    public static int slots(Document profile) {
        return bought(profile) * SLOTS;
    }

    /** What this upgrade (from 1) costs. */
    public static long cost(int upgrade) {
        if (upgrade <= 1) return 1_500_000;
        if (upgrade <= 5) return 5_000_000;
        if (upgrade <= 10) return 8_000_000;
        if (upgrade <= 20) return 12_000_000;
        return 20_000_000;
    }
}
