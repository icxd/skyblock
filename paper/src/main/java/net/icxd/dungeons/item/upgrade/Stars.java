package net.icxd.dungeons.item.upgrade;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

import java.util.List;

/**
 * Star upgrades ({@code ✪}, Hypixel's "item level"): bought one at a time, in order, each for its own essence and
 * items (the item data's upgrade costs, the API's: 5, 10 or 15 of them), at Malik's Essence Crafting or in the Hex,
 * here {@code /upgrade} and the Hex's Item Upgrades. Each gives 2% of the item's stats (ItemBuilder#starBonus).
 * Kept in the data's {@code upgrade_count} (Hypixel's {@code upgrade_level}); master stars carry it on past 5 on a
 * dungeon item. NEU's table also charges coins from the 4th star on, which the API doesn't have: none are charged
 * (UNKNOWN which is live).
 */
public final class Stars {
    private Stars() {
    }

    /** How many stars its upgrade costs go to; 0 if it can't be upgraded. */
    public static int max(SkyBlockItem item) {
        return item.upgradeCosts() == null ? 0 : item.upgradeCosts().getCosts().size();
    }

    /** What star {@code n} (from 1) costs; null if it has no such star. */
    public static UpgradeCost cost(SkyBlockItem item, int n) {
        if (n < 1 || n > max(item)) return null;
        List<UpgradeCost> costs = item.upgradeCosts().getCosts();
        return costs.get(n - 1);
    }

    /** What its next star costs; null if it can't take one (it can't be upgraded, or has them all). */
    public static UpgradeCost next(SkyBlockItem item, NBTTagCompound tag) {
        return cost(item, ItemBuilder.starCount(tag) + 1);
    }

    /** One more star on the item's data. */
    public static void add(NBTTagCompound tag) {
        tag.setInt("upgrade_count", ItemBuilder.starCount(tag) + 1);
    }
}
