package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

/**
 * Where the SkyBlock ender chest, the backpacks, the bags and the armor and equipment sets keep their
 * items: in the profile's {@code storage}, with the inventory (see StoredInventory), each slot a blob
 * the way the inventory's are ({@code ItemStack#serializeAsBytes}), null when it's empty. So they're in
 * the one document that's saved, handed off and switched with the profile, and saved at the same moment
 * as the inventory. As plain functions on the profile's {@code storage} document (no server needed);
 * what a blob is doesn't matter here.
 * <pre>
 * storage:
 *   enderChest:       [ page 1's 45 slots, page 2's, ... ]
 *   enderChestIcons:  { "1": "INK_SAC" }      a page's icon, in place of its glass
 *   backpacks:        { "1": { id: "GREATER_BACKPACK", item: &lt;the backpack&gt;, contents: [36 slots] } }
 *   accessoryBag, potionBag, fishingBag, sackOfSacks: [ slots ]
 *   armorSets:        [ [helmet, chestplate, leggings, boots], ... ]
 *   equipmentSets:    [ [necklace, cloak, belt, gloves/bracelet], ... ]
 *   equipment:        [necklace, cloak, belt, gloves/bracelet]      what they wear
 *   wornArmorSet, wornEquipmentSet:   the set what they wear came from, from 0; -1 for none
 * </pre>
 * A section longer than what's shown of it (a page from before a smaller backpack) keeps the rest.
 */
public final class StorageDocument {
    public static final String ENDER_CHEST = "enderChest";
    public static final String ICONS = "enderChestIcons";
    public static final String BACKPACKS = "backpacks";
    public static final String ARMOR_SETS = "armorSets";
    public static final String EQUIPMENT_SETS = "equipmentSets";
    public static final String EQUIPMENT = "equipment";
    public static final String WORN_ARMOR_SET = "wornArmorSet";
    public static final String WORN_EQUIPMENT_SET = "wornEquipmentSet";
    static final String ID = "id";
    static final String ITEM = "item";
    static final String CONTENTS = "contents";

    private StorageDocument() {
    }

    // Plain sections: a bag, the worn equipment

    /** A section's slots, {@code size} of them (the missing ones empty); a copy. */
    public static List<Object> slots(Document storage, String key, int size) {
        return padded(storage.get(key), size);
    }

    /** Writes the first {@code slots.size()} slots of a section, keeping any after them. */
    public static void setSlots(Document storage, String key, List<?> slots) {
        storage.put(key, merged(storage.get(key), slots));
    }

    // The ender chest

    /** A page's slots, from page 1. */
    public static List<Object> page(Document storage, int page, int size) {
        List<?> pages = list(storage.get(ENDER_CHEST));
        return padded(page - 1 < pages.size() ? pages.get(page - 1) : null, size);
    }

    public static void setPage(Document storage, int page, List<?> slots) {
        List<Object> pages = new ArrayList<>(list(storage.get(ENDER_CHEST)));
        while (pages.size() < page) pages.add(new ArrayList<>());
        pages.set(page - 1, merged(pages.get(page - 1), slots));
        storage.put(ENDER_CHEST, pages);
    }

    /** The material a page shows in place of its glass; null for the glass. */
    public static String icon(Document storage, int page) {
        return storage.get(ICONS) instanceof Document icons && icons.get(String.valueOf(page)) instanceof String icon ? icon : null;
    }

    /** Null puts the glass back. */
    public static void setIcon(Document storage, int page, String icon) {
        Document icons = storage.get(ICONS) instanceof Document d ? d : new Document();
        if (icon == null) icons.remove(String.valueOf(page));
        else icons.put(String.valueOf(page), icon);
        storage.put(ICONS, icons);
    }

    // Backpacks

    /** The backpack in a slot (from 1): its item id, the backpack itself and what's in it; null for an empty slot. */
    public record Backpack(String id, Object item, List<Object> contents) {
        /** Nothing in it: only then can it come out of its slot. */
        public boolean empty() {
            return StorageDocument.empty(contents);
        }
    }

    public static Backpack backpack(Document storage, int slot) {
        if (!(storage.get(BACKPACKS) instanceof Document backpacks) || !(backpacks.get(String.valueOf(slot)) instanceof Document b)) return null;
        if (!(b.get(ID) instanceof String id) || b.get(ITEM) == null) return null;
        return new Backpack(id, b.get(ITEM), new ArrayList<>(list(b.get(CONTENTS))));
    }

    /** Puts a backpack in an empty slot, with nothing in it. False if the slot has one. */
    public static boolean placeBackpack(Document storage, int slot, String id, Object item) {
        if (backpack(storage, slot) != null) return false;
        backpacks(storage).put(String.valueOf(slot), new Document(ID, id).append(ITEM, item).append(CONTENTS, new ArrayList<>()));
        return true;
    }

    /**
     * Takes the backpack out of its slot and returns it: only an empty one comes out (the wiki's Storage,
     * "Backpacks cannot be removed from the Storage if they contain items"). Null, and nothing changes,
     * for an empty slot or a backpack with something in it.
     */
    public static Backpack removeBackpack(Document storage, int slot) {
        Backpack backpack = backpack(storage, slot);
        if (backpack == null || !backpack.empty()) return null;
        backpacks(storage).remove(String.valueOf(slot));
        return backpack;
    }

    /** What's in a slot's backpack, {@code size} slots of it; empty for an empty slot. */
    public static List<Object> backpackContents(Document storage, int slot, int size) {
        Backpack backpack = backpack(storage, slot);
        return padded(backpack == null ? null : backpack.contents(), size);
    }

    /** False if there's no backpack in the slot (it came out in the meantime): nothing is written then. */
    public static boolean setBackpackContents(Document storage, int slot, List<?> contents) {
        if (!(storage.get(BACKPACKS) instanceof Document backpacks) || !(backpacks.get(String.valueOf(slot)) instanceof Document b)) return false;
        b.put(CONTENTS, merged(b.get(CONTENTS), contents));
        return true;
    }

    private static Document backpacks(Document storage) {
        if (storage.get(BACKPACKS) instanceof Document backpacks) return backpacks;
        Document backpacks = new Document();
        storage.put(BACKPACKS, backpacks);
        return backpacks;
    }

    // Armor and equipment sets

    /** A set's four pieces, from set 0. */
    public static List<Object> set(Document storage, String key, int set) {
        List<?> sets = list(storage.get(key));
        return padded(set < sets.size() ? sets.get(set) : null, 4);
    }

    public static void setSet(Document storage, String key, int set, List<?> pieces) {
        List<Object> sets = new ArrayList<>(list(storage.get(key)));
        while (sets.size() <= set) sets.add(new ArrayList<>());
        sets.set(set, merged(sets.get(set), pieces));
        storage.put(key, sets);
    }

    /** The set what they wear came from; -1 for none. */
    public static int worn(Document storage, String key) {
        return storage.get(key) instanceof Number n ? n.intValue() : -1;
    }

    public static void setWorn(Document storage, String key, int set) {
        storage.put(key, set);
    }

    // Lists

    /** No item in any slot. */
    public static boolean empty(List<?> slots) {
        for (Object slot : slots) {
            if (slot != null) return false;
        }
        return true;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static List<Object> padded(Object value, int size) {
        List<?> list = list(value);
        List<Object> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) out.add(i < list.size() ? list.get(i) : null);
        return out;
    }

    /** {@code slots}, then what {@code old} had after them. */
    private static List<Object> merged(Object old, List<?> slots) {
        List<?> before = list(old);
        List<Object> out = new ArrayList<>(slots);
        for (int i = slots.size(); i < before.size(); i++) out.add(before.get(i));
        return out;
    }
}
