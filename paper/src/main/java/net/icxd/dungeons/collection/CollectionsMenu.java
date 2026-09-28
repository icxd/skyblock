package net.icxd.dungeons.collection;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.collection.CollectionData.Category;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.menu.SkyBlockMenu;

/**
 * Collections ({@code /collection}, or the SkyBlock Menu's painting), as recorded (00:40.3): the five
 * categories, Boss Collections, Crafted Minions and the rankings sign. Crafted Minions and the rankings do
 * nothing yet (LATER: minions, rankings). Main thread.
 */
public final class CollectionsMenu extends CollectionGUI {
    public static final String TITLE = "Collections";
    private static final Pattern MINION = Pattern.compile("[A-Z_]+_GENERATOR_\\d+");
    private static volatile int minionItems = -1;

    public CollectionsMenu(Player viewer) {
        super(TITLE, viewer);
    }

    /** How many minion items there are (the recorded "Crafted minions: 98/719" counts them). */
    static int minionItems() {
        if (minionItems < 0) {
            int count = 0;
            for (String id : ItemRegistry.getRegistry().keySet()) if (MINION.matcher(id).matches()) count++;
            minionItems = count;
        }
        return minionItems;
    }

    /** The unique minions the profile has crafted (none can be crafted yet). */
    static int craftedMinions(Document profile) {
        Document minions = profile.get("minions") instanceof Document d ? d : null;
        return minions != null && minions.get("craftedMinions") instanceof List<?> list ? list.size() : 0;
    }

    @Override
    protected Map<Integer, MenuSlot> slots(Document profile) {
        return CollectionMenus.collections(profile, craftedMinions(profile), minionItems());
    }

    @Override
    protected void buttons(Document profile) {
        List<Category> categories = Collections.data().categories();
        for (int i = 0; i < categories.size() && i < CollectionMenus.CATEGORY_SLOTS.length; i++) {
            Category category = categories.get(i);
            on(CollectionMenus.CATEGORY_SLOTS[i], () -> new CategoryMenu(viewer, category.id()).open(viewer));
        }
        on(CollectionMenus.BOSSES, () -> new CategoryMenu(viewer, CollectionData.BOSS).open(viewer));
        on(CollectionMenus.BACK, () -> new SkyBlockMenu(viewer).open(viewer));
    }
}
