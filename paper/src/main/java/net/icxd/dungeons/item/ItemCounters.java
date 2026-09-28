package net.icxd.dungeons.item;

import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Counts kept on an item, in its data (the Book of Stats' kills, Champion's "champion_combat_xp", Hecatomb's
 * "hecatomb_s_runs", the Growth armor's kills, Tempest's arrows): what an effect that grows with use counts,
 * under a key of its own, never below 0. Adding to one on a held or worn item rebuilds it for its holder, so a
 * lore line an item behaviour writes from the count ({@code ItemBehaviour.lore}) follows. Nothing is written
 * until something counts, so new items stay as they were. Equipment (necklaces, cloaks, belts, gloves) is kept
 * in storage, not the inventory: counting on it is LATER (Equipment has no way to put a piece back yet). Main thread.
 */
public final class ItemCounters {
    private ItemCounters() {
    }

    /** The count under {@code key} in this data; 0 for none (or no data). */
    public static double get(NBTTagCompound tag, String key) {
        return tag == null ? 0 : Math.max(0, tag.getDouble(key));
    }

    /** Adds {@code amount} to the count in this data (never below 0), and returns the new count. */
    public static double add(NBTTagCompound tag, String key, double amount) {
        double count = Math.max(0, get(tag, key) + amount);
        tag.setDouble(key, count);
        return count;
    }

    /** The stack with {@code amount} more on its count, rebuilt for {@code holder}; null if it isn't a SkyBlock item. */
    public static ItemStack add(ItemStack stack, Player holder, String key, double amount) {
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null) return null;
        add(tag, key, amount);
        return ItemBuilder.build(item, tag, stack.getAmount(), holder);
    }

    /**
     * Adds to the count on what they hold in their main hand; false if it isn't a SkyBlock item, or their
     * inventory can't change just now (it's being handed to another server).
     */
    public static boolean addHeld(Player player, String key, double amount) {
        return addIn(player, EquipmentSlot.HAND, key, amount);
    }

    /** Adds to the count on the armor they wear in this slot (HEAD, CHEST, LEGS, FEET); false as for {@link #addHeld}. */
    public static boolean addWorn(Player player, EquipmentSlot slot, String key, double amount) {
        return addIn(player, slot, key, amount);
    }

    private static boolean addIn(Player player, EquipmentSlot slot, String key, double amount) {
        if (InventorySyncListener.frozen(player)) return false;
        PlayerInventory inventory = player.getInventory();
        ItemStack counted = add(inventory.getItem(slot), player, key, amount);
        if (counted == null) return false;
        inventory.setItem(slot, counted);
        return true;
    }
}
