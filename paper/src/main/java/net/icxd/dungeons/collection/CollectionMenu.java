package net.icxd.dungeons.collection;

import java.util.Map;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.collection.CollectionData.Collection;

/**
 * A collection's tiers ("Blaze Rod Collection", recorded 00:44.9; a boss's, "Bonzo Collection", 00:59.5):
 * a tier whose Rewards menu has something to show opens it. Main thread.
 */
public final class CollectionMenu extends CollectionGUI {
    private final String id;

    public CollectionMenu(Player viewer, String id) {
        super(title(id), viewer);
        this.id = id;
    }

    static String title(String id) {
        Collection collection = Collections.data().collection(id);
        return (collection == null ? id : collection.name()) + " Collection";
    }

    @Override
    protected Map<Integer, MenuSlot> slots(Document profile) {
        Collection collection = Collections.data().collection(id);
        return collection == null ? Map.of() : CollectionMenus.collection(profile, collection);
    }

    @Override
    protected void buttons(Document profile) {
        Collection collection = Collections.data().collection(id);
        if (collection == null) return;
        int[] slots = CollectionMenus.rowSlots(collection.maxTier());
        for (int n = 1; n <= collection.maxTier() && n <= slots.length; n++) {
            if (!CollectionMenus.hasRewardsMenu(collection.tier(n))) continue;
            int tier = n;
            on(slots[n - 1], () -> new RewardsMenu(viewer, id, tier).open(viewer));
        }
        on(CollectionMenus.BACK, () -> new CategoryMenu(viewer, collection.category()).open(viewer));
    }
}
