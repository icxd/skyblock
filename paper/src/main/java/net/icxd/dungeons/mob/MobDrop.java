package net.icxd.dungeons.mob;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Something a mob can drop: {@code chance} percent (before magic find), {@code min} to {@code max} of
 * the item, or of one of several items picked at random (the wiki's "Rotten Armor Piece" is one piece of
 * the set; which one is UNKNOWN, so each is as likely). Items are named by id, so mobs can be defined
 * before the items are.
 */
public record MobDrop(List<String> itemIds, MobDropType type, double chance, int min, int max) {
    public MobDrop {
        itemIds = List.copyOf(itemIds);
        if (itemIds.isEmpty()) throw new IllegalArgumentException("a drop needs an item");
    }

    public MobDrop(String itemId, MobDropType type, double chance, int min, int max) {
        this(List.of(itemId), type, chance, min, max);
    }

    public MobDrop(String itemId, MobDropType type, double chance) {
        this(itemId, type, chance, 1, 1);
    }

    /** One of these items, each as likely. */
    public static MobDrop oneOf(MobDropType type, double chance, String... itemIds) {
        return new MobDrop(List.of(itemIds), type, chance, 1, 1);
    }

    /** The item this drop gives (one of its items, at random); null if there's no such item. */
    public SkyBlockItem item() {
        return ItemRegistry.get(itemIds.get(ThreadLocalRandom.current().nextInt(itemIds.size())));
    }
}
