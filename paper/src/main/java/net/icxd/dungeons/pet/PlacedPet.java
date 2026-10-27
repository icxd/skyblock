package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.StoredInventory;

/**
 * A pet item taken out of its owner's inventory into a menu's slot (Kat's Pet Sitter, George's Offer Pets), as
 * Hypixel's are (recorded, 2026-10-25: it leaves the inventory, and is back in it when the menu closes). It goes from
 * one menu to the next ({@link #owner}: Pet Sitter to Confirm Hiring Kat and back) and back to them when the one that
 * has it closes, however that happens; it's used up when it's hired out or sold. While it's out, a copy waits in the
 * profile's overflow ({@link StoredInventory#OVERFLOW}), which their next join gives back, so a crash or a server
 * stop (which no menu hears close) can't lose it, and every save has it there or wherever it went since, never both
 * (as the Hex keeps its item, hex/HexSession). Main thread.
 */
final class PlacedPet {
    private final ItemStack item;
    private final OwnedPet pet;
    private final Document profile;
    private Binary saved;
    /** The menu that has it now; null once it's back or used up. */
    private GUI owner;

    private PlacedPet(ItemStack item, OwnedPet pet, Document profile, GUI owner) {
        this.item = item;
        this.pet = pet;
        this.profile = profile;
        this.owner = owner;
    }

    /** The pet item in this slot of their inventory, taken out of it into {@code owner}; null if that's no pet. */
    static PlacedPet take(Player player, Document profile, int slot, GUI owner) {
        ItemStack stack = player.getInventory().getItem(slot);
        OwnedPet pet = stack == null || stack.isEmpty() ? null : PetItems.pet(ItemNBT.read(stack));
        if (pet == null) return null;
        PlacedPet placed = new PlacedPet(stack.clone(), pet, profile, owner);
        player.getInventory().setItem(slot, null);
        if (StoredInventory.saved(placed.item)) {
            placed.saved = new Binary(placed.item.serializeAsBytes());
            Document storage = StoredInventory.storage(profile);
            List<Object> overflow = new ArrayList<>(list(storage.get(StoredInventory.OVERFLOW)));
            overflow.add(placed.saved);
            storage.put(StoredInventory.OVERFLOW, overflow);
        }
        return placed;
    }

    OwnedPet pet() {
        return pet;
    }

    /** The item as it was taken, to show. */
    ItemStack item() {
        return item.clone();
    }

    boolean ownedBy(GUI menu) {
        return owner == menu;
    }

    /** Now {@code menu}'s: the one that had it closing (as {@code menu} opens) doesn't give it back. */
    void passTo(GUI menu) {
        if (owner != null) owner = menu;
    }

    /** Back into their inventory (else their stash), if it isn't back or used up already. */
    void giveBack(Player player) {
        if (!release()) return;
        ItemStash.give(player, item.clone());
    }

    /** Used up (hired out, sold): it doesn't come back. False if it's back or used up already. */
    boolean use() {
        return release();
    }

    private boolean release() {
        if (owner == null) return false;
        owner = null;
        if (saved != null) {
            Document storage = StoredInventory.storage(profile);
            List<Object> overflow = new ArrayList<>();
            // That very copy: another item may be the same.
            for (Object entry : list(storage.get(StoredInventory.OVERFLOW))) if (entry != saved) overflow.add(entry);
            storage.put(StoredInventory.OVERFLOW, overflow);
            saved = null;
        }
        return true;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }
}
