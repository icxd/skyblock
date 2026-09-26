package net.icxd.dungeons.item.behaviour;

import net.icxd.dungeons.item.SkyBlockItem;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Each item's {@link ItemBehaviour}, by item id; most items have none, and get one that changes nothing. */
public final class ItemBehaviours {
    private static final ItemBehaviour NONE = new ItemBehaviour() {
    };
    private static final Map<String, ItemBehaviour> BY_ID = new HashMap<>();

    static {
        NecronsBlade blade = new NecronsBlade();
        for (String id : NecronsBlade.IDS) BY_ID.put(id, blade);
        BY_ID.put("ATTRIBUTE_SHARD", new AttributeShard());
    }

    private ItemBehaviours() {
    }

    public static ItemBehaviour of(SkyBlockItem item) {
        return BY_ID.getOrDefault(item.id().toUpperCase(Locale.ROOT), NONE);
    }
}
