package net.icxd.dungeons.mob;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;

/**
 * Something a mob can drop: {@code chance} percent (before magic find), {@code min} to {@code max} of
 * the item. The item is named by id, so mobs can be defined before the items are.
 */
public record MobDrop(String itemId, MobDropType type, double chance, int min, int max) {
    public MobDrop(String itemId, MobDropType type, double chance) {
        this(itemId, type, chance, 1, 1);
    }

    /** Null if there's no such item. */
    public SkyBlockItem item() {
        return ItemRegistry.get(itemId);
    }
}
