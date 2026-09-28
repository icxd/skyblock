package net.icxd.dungeons.storage;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.SkyBlockMenuItem;
import net.icxd.dungeons.user.StoredInventory;

/**
 * Items in and out of storage's blobs, the inventory's way (see StoredInventory: a slot that can't be read
 * is kept in {@code storage.unreadable}, the rest still load), and what storage needs to know about an
 * item. Main thread.
 */
final class StorageItems {
    /** "A bag with &a36&7 slots which can be", a backpack's own words (items.json). */
    private static final Pattern BACKPACK_SLOTS = Pattern.compile("A bag with &a(\\d+)&7 slots");
    /** Dyed backpacks ("RED_GREATER_BACKPACK") are the same size as the undyed one, which has the words. */
    private static final Pattern BACKPACK_ID = Pattern.compile("(?:.+_)?(SMALL|MEDIUM|LARGE|GREATER|JUMBO)_BACKPACK");

    private StorageItems() {
    }

    /**
     * The items of stored slots, made again from their data so they look as items do now (as the
     * inventory's are when they join).
     */
    static ItemStack[] decode(Player player, List<?> slots, String section, Document storage) {
        return decode(player, slots, section, 0, storage);
    }

    /** The same for slots that start at slot {@code first} of their section (a bag's later page), as one that can't be read is kept. */
    static ItemStack[] decode(Player player, List<?> slots, String section, int first, Document storage) {
        ItemStack[] out = new ItemStack[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            ItemStack item = StoredInventory.read(player, slots.get(i), section, first + i, storage, log());
            out[i] = item == null ? null : refreshed(item);
        }
        return out;
    }

    /**
     * Made again from its data; as it is if that fails (data this plugin no longer knows, like a reforge
     * since removed), so a page shows it rather than failing to open and then being saved empty.
     */
    private static ItemStack refreshed(ItemStack item) {
        try {
            return ItemBuilder.refresh(item);
        } catch (RuntimeException e) {
            log().warning("Couldn't bring an item in storage up to date (" + e + "); it's kept as it was");
            return item;
        }
    }

    /**
     * One stored item as it is, to show or give (a backpack's own), without {@link #decode}'s keeping of
     * what can't be read: that stays where it is. Null for nothing, or what can't be read.
     */
    static ItemStack peek(Object blob) {
        byte[] bytes = blob instanceof Binary binary ? binary.getData() : blob instanceof byte[] raw ? raw : null;
        if (bytes == null) return null;
        ItemStack item;
        try {
            item = ItemStack.deserializeBytes(bytes);
        } catch (RuntimeException e) {
            return null;
        }
        return refreshed(item);
    }

    static List<Binary> encode(ItemStack[] items) {
        return StoredInventory.encode(items);
    }

    /** The SkyBlock item this is; null for none (nothing, or a vanilla item). */
    static SkyBlockItem skyBlockItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag == null ? null : ItemRegistry.get(tag.getString("id"));
    }

    /** Its data; null for none. */
    static NBTTagCompound tag(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : ItemNBT.read(stack);
    }

    /**
     * Whether it may be kept in storage at all: what the inventory would save (not an item that belongs to
     * where they are, like a dungeon's map), and never the SkyBlock Menu.
     */
    static boolean storable(ItemStack stack) {
        return StoredInventory.saved(stack) && !SkyBlockMenuItem.is(stack);
    }

    /** How many slots a backpack gives, by its item data ("A bag with 36 slots"); 0 if it isn't one. */
    static int backpackSize(SkyBlockItem item) {
        if (item == null) return 0;
        Matcher id = BACKPACK_ID.matcher(item.id());
        if (!id.matches()) return 0;
        SkyBlockItem plain = ItemRegistry.get(id.group(1) + "_BACKPACK");
        return plain == null ? 0 : backpackSize(plain.lore());
    }

    /** The number in a backpack's "A bag with 36 slots"; 0 if it doesn't say. */
    static int backpackSize(List<String> lore) {
        for (String line : lore) {
            Matcher m = BACKPACK_SLOTS.matcher(line);
            if (m.find()) return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    static boolean isBackpack(ItemStack stack) {
        return backpackSize(skyBlockItem(stack)) > 0;
    }

    private static Logger log() {
        Dungeons plugin = Dungeons.getInstance();
        return plugin != null ? plugin.getLogger() : Logger.getLogger(StorageItems.class.getName());
    }
}
