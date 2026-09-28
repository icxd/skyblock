package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.user.StoredInventory;

/**
 * SkyBlock items in a profile's storage, counted and taken by their id, for what takes its costs from wherever a
 * player keeps things (the Hex: "The Hex knows of inventory, of Storage, of Ender Chest", its Handler says): the
 * Ender Chest's pages in order, then the backpacks by slot. Only what a page shows (a backpack's first slots, as
 * big as it is). Not while a storage page is open, whose items are in the menu and not the profile: then there's
 * nothing here. An item taken is written back with one less (or gone); every other slot's blob is left as it is.
 * Main thread.
 */
public final class StoredItems {
    private StoredItems() {
    }

    /** A stored slot's item as counting needs it: its SkyBlock id (upper case) and how many. */
    record Stack(String id, int amount) {
    }

    /** How blobs are read and written (ItemStack's bytes; plain values in tests). */
    interface Blobs {
        /** The item in a slot; null for none, or one that isn't a SkyBlock item (or can't be read). */
        Stack read(Object blob);

        /** The slot's item with {@code amount} left (more than 0). */
        Object withAmount(Object blob, int amount);
    }

    private static final Blobs ITEMS = new Blobs() {
        @Override
        public Stack read(Object blob) {
            ItemStack item = item(blob);
            if (item == null) return null;
            NBTTagCompound tag = ItemNBT.read(item);
            String id = tag == null ? "" : tag.getString("id");
            return id.isEmpty() ? null : new Stack(id.toUpperCase(), item.getAmount());
        }

        @Override
        public Object withAmount(Object blob, int amount) {
            ItemStack item = item(blob);
            return new Binary(item.asQuantity(amount).serializeAsBytes());
        }

        private static ItemStack item(Object blob) {
            byte[] bytes = blob instanceof Binary binary ? binary.getData() : blob instanceof byte[] raw ? raw : null;
            if (bytes == null) return null;
            try {
                return ItemStack.deserializeBytes(bytes);
            } catch (RuntimeException e) {
                return null;
            }
        }
    };

    /** Every item in their storage, by id (upper case): how many of each. Empty while a storage page is open. */
    public static Map<String, Integer> counts(Player player, Document profile) {
        Map<String, Integer> counts = new HashMap<>();
        if (ItemPage.current(player) != null) return counts;
        for (List<Object> slots : sections(StoredInventory.storage(profile))) count(slots, ITEMS, counts);
        return counts;
    }

    /**
     * Takes up to {@code amount} of an item from their storage, first to last; how many it took (0 while a storage
     * page is open). The caller saves.
     */
    public static int take(Player player, Document profile, String id, int amount) {
        if (ItemPage.current(player) != null || amount <= 0) return 0;
        Document storage = StoredInventory.storage(profile);
        int taken = 0;
        for (int page = 1; page <= StorageMenu.pages() && taken < amount; page++) {
            List<Object> slots = StorageDocument.page(storage, page, StorageMenu.PAGE_SIZE);
            int took = take(slots, id, amount - taken, ITEMS);
            if (took > 0) StorageDocument.setPage(storage, page, slots);
            taken += took;
        }
        for (int slot = 1; slot <= StorageMenu.backpackSlots() && taken < amount; slot++) {
            List<Object> slots = backpack(storage, slot);
            if (slots == null) continue;
            int took = take(slots, id, amount - taken, ITEMS);
            if (took > 0) StorageDocument.setBackpackContents(storage, slot, slots);
            taken += took;
        }
        return taken;
    }

    /** The Ender Chest's pages, then each backpack's slots. */
    private static List<List<Object>> sections(Document storage) {
        List<List<Object>> sections = new ArrayList<>();
        for (int page = 1; page <= StorageMenu.pages(); page++) sections.add(StorageDocument.page(storage, page, StorageMenu.PAGE_SIZE));
        for (int slot = 1; slot <= StorageMenu.backpackSlots(); slot++) {
            List<Object> slots = backpack(storage, slot);
            if (slots != null) sections.add(slots);
        }
        return sections;
    }

    /** A backpack's slots, as many as it has; null for an empty slot (or a backpack this server doesn't know). */
    private static List<Object> backpack(Document storage, int slot) {
        StorageDocument.Backpack backpack = StorageDocument.backpack(storage, slot);
        int size = backpack == null ? 0 : StorageItems.backpackSize(ItemRegistry.get(backpack.id()));
        return size <= 0 ? null : StorageDocument.backpackContents(storage, slot, size);
    }

    /** Adds what's in these slots to {@code counts}. */
    static void count(List<Object> slots, Blobs blobs, Map<String, Integer> counts) {
        for (Object blob : slots) {
            Stack stack = blob == null ? null : blobs.read(blob);
            if (stack != null) counts.merge(stack.id(), stack.amount(), Integer::sum);
        }
    }

    /** Takes up to {@code amount} of the item from these slots, first to last, changing them in place; how many it took. */
    static int take(List<Object> slots, String id, int amount, Blobs blobs) {
        int taken = 0;
        for (int i = 0; i < slots.size() && taken < amount; i++) {
            Object blob = slots.get(i);
            Stack stack = blob == null ? null : blobs.read(blob);
            if (stack == null || !stack.id().equalsIgnoreCase(id)) continue;
            int take = Math.min(stack.amount(), amount - taken);
            slots.set(i, take == stack.amount() ? null : blobs.withAmount(blob, stack.amount() - take));
            taken += take;
        }
        return taken;
    }
}
