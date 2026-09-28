package net.icxd.dungeons.collection;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.collection.CollectionData.Category;
import net.icxd.dungeons.collection.CollectionData.Collection;

/**
 * A category's collections ("Combat Collections", recorded 00:42.9) or the bosses' ("Boss Collections",
 * 00:58.3): a found collection opens its tiers; one never found does nothing (UNKNOWN: none was clicked).
 * Main thread.
 */
public final class CategoryMenu extends CollectionGUI {
    private final String category;

    /** {@code category}: a category's id ("COMBAT"), or {@link CollectionData#BOSS}. */
    public CategoryMenu(Player viewer, String category) {
        super(title(category), viewer);
        this.category = category;
    }

    static String title(String category) {
        if (CollectionData.BOSS.equals(category)) return "Boss Collections";
        Category c = Collections.data().category(category);
        return (c == null ? category : c.name()) + " Collections";
    }

    private List<Collection> collections() {
        if (CollectionData.BOSS.equals(category)) return Collections.data().bosses();
        Category c = Collections.data().category(category);
        return c == null ? List.of() : c.collections().stream().map(id -> Collections.data().collection(id)).toList();
    }

    @Override
    protected Map<Integer, MenuSlot> slots(Document profile) {
        if (CollectionData.BOSS.equals(category)) return CollectionMenus.bosses(profile);
        Category c = Collections.data().category(category);
        return c == null ? Map.of() : CollectionMenus.category(profile, c);
    }

    @Override
    protected void buttons(Document profile) {
        List<Collection> collections = collections();
        for (int i = 0; i < collections.size() && i < CollectionMenus.INSIDE.length; i++) {
            Collection collection = collections.get(i);
            if (Collections.found(profile, collection.id())) {
                on(CollectionMenus.INSIDE[i], () -> new CollectionMenu(viewer, collection.id()).open(viewer));
            }
        }
        on(CollectionMenus.BACK, () -> new CollectionsMenu(viewer).open(viewer));
    }
}
