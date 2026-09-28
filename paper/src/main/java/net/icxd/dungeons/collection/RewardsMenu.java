package net.icxd.dungeons.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.collection.CollectionData.Collection;
import net.icxd.dungeons.collection.CollectionData.Reward;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.recipe.RecipeView;
import net.icxd.dungeons.recipe.Recipes;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.User;

/**
 * A tier's rewards ("Blaze Rod I Rewards", recorded 00:46.9; "Bonzo I Rewards", 01:01.0): a recipe opens
 * it; a boss's item, once its tier is reached, is claimed with a click, once (the menu's "claim rewards";
 * how it's claimed is UNKNOWN). Minion, pet and trade rewards do nothing yet (LATER). Main thread.
 */
public final class RewardsMenu extends CollectionGUI {
    private final String id;
    private final int tier;

    public RewardsMenu(Player viewer, String id, int tier) {
        super(title(id, tier), viewer);
        this.id = id;
        this.tier = tier;
    }

    /** "Blaze Rod I Rewards". */
    static String title(String id, int tier) {
        Collection collection = Collections.data().collection(id);
        return (collection == null ? id : CollectionText.named(collection, tier)) + " Rewards";
    }

    @Override
    protected Map<Integer, MenuSlot> slots(Document profile) {
        Collection collection = Collections.data().collection(id);
        return collection == null || tier < 1 || tier > collection.maxTier() ? Map.of() : CollectionMenus.rewards(profile, collection, tier);
    }

    @Override
    protected void buttons(Document profile) {
        Collection collection = Collections.data().collection(id);
        if (collection == null || tier < 1 || tier > collection.maxTier()) return;
        List<Reward> shown = new ArrayList<>();
        for (Reward reward : collection.tier(tier).rewards()) if (reward.shownInRewards()) shown.add(reward);
        int[] slots = CollectionMenus.rowSlots(shown.size());
        for (int i = 0; i < shown.size() && i < slots.length; i++) {
            Reward reward = shown.get(i);
            // Not one there's no crafting recipe for (a potion's is brewed): its view would be empty.
            if (reward.type() == Reward.Type.RECIPE && Recipes.data().recipe(reward.item()) != null) {
                on(slots[i], () -> new RecipeView(viewer, reward.item(), () -> new RewardsMenu(viewer, id, tier).open(viewer),
                        getTitle()).open(viewer));
            } else if (reward.type() == Reward.Type.ITEM && collection.boss()) {
                on(slots[i], () -> claim(collection));
            }
        }
        on(CollectionMenus.BACK, () -> new CollectionMenu(viewer, id).open(viewer));
    }

    /** The tier's items, if it's reached and they haven't been claimed; saved at once. */
    private void claim(Collection collection) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(viewer)) return;
        Document profile = user.profile();
        if (Collections.tier(profile, id) < tier || Collections.claimed(profile, id).contains(tier)) return;
        Collections.claim(profile, id, tier);
        for (Reward reward : collection.tier(tier).rewards()) {
            if (reward.type() != Reward.Type.ITEM) continue;
            SkyBlockItem item = ItemRegistry.get(reward.item());
            if (item != null) ItemStash.give(viewer, ItemBuilder.build(item, (int) Math.max(1, reward.amount())));
        }
        user.save();
        reopen();
    }
}
