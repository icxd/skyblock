package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.event.GUIOpenEvent;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;

/**
 * A storage menu with real items in it (an Ender Chest page, a backpack, a bag): some of its slots hold
 * what's stored, the rest are its buttons. Items move in and out of those slots the way they do in a
 * chest, by click, shift-click and number key (not by dragging, which menus don't allow, nor a double
 * click); an item a slot doesn't take (see {@link #accepts}) stays out (see StorageListener).
 *
 * <p>The menu is where the items are while it's open: they're written back into the profile after each
 * click (on the next tick), when it closes (also on disconnect, and before a hand-off or a profile switch,
 * which close it first), before another storage menu reads the profile, and whenever the inventory is
 * saved (StoredInventory#captureWith), so a save always has an item in exactly one of the two. It writes
 * to the profile it was opened on, and nothing once their data has been handed off, but for that last
 * save itself: when the server stops, the plugin's events are off, so the menu doesn't hear itself close,
 * and the save that lets their data go is the only one left to write it. Main thread.
 */
abstract class ItemPage extends GUI {
    protected final Player viewer;
    /** The menu's slots that hold items, in the order they're stored. */
    private final int[] itemSlots;
    /** The profile it shows, and that document's storage. */
    protected final Document profile;
    protected final Document storage;
    private Inventory inventory;
    /** Written back, and not to be again (it has closed). */
    private boolean done;

    ItemPage(String title, int size, Player viewer, Document profile, int[] itemSlots) {
        super(title, size);
        this.viewer = viewer;
        this.profile = profile;
        this.storage = StoredInventory.storage(profile);
        this.itemSlots = itemSlots;
    }

    /** The stored items, one for each item slot (null for an empty one). */
    abstract ItemStack[] load();

    /**
     * Writes the items back, one blob (or null) for each item slot. False if there's nowhere to keep them
     * now (a backpack that's gone from its slot): then they go to the player (see {@link #sync}).
     */
    abstract boolean store(List<Binary> items);

    /**
     * Whether this item may go in this slot (a menu slot): anything storage may keep (see
     * StorageItems#storable) but a backpack (the wiki's Backpack: "Backpacks cannot be placed into the
     * Ender Chest"), unless a page says otherwise.
     */
    boolean accepts(int slot, ItemStack item) {
        return StorageItems.storable(item) && !StorageItems.isBackpack(item);
    }

    /** After the items have been written back. */
    void stored() {
    }

    int[] itemSlots() {
        return itemSlots;
    }

    boolean isItemSlot(int slot) {
        for (int s : itemSlots) if (s == slot) return true;
        return false;
    }

    /**
     * The items go in before the menu is shown, with its buttons. A storage menu that's open still (being
     * replaced by this one) is written back first, so this reads what it has.
     */
    @Override
    public void onOpen(GUIOpenEvent event) {
        if (GUI.GUI_MAP.get(viewer.getUniqueId()) instanceof ItemPage open && open != this) open.sync();
        inventory = event.getInventory();
        ItemStack[] items = load();
        for (int i = 0; i < itemSlots.length; i++) inventory.setItem(itemSlots[i], i < items.length ? items[i] : null);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        sync();
        done = true;
    }

    /**
     * Writes what's in the item slots back into the profile. If there's nowhere to keep them (or an item
     * can't be written), they leave the menu for the player's inventory (or their stash), so they're
     * neither lost nor kept twice.
     */
    void sync() {
        sync(false);
    }

    /** {@code saving}: from a save of their data (see {@link #writes}). */
    void sync(boolean saving) {
        if (inventory == null || done) return;
        User user = User.cached(viewer.getUniqueId());
        if (user == null || !writes(user.isInventoryRestored(), user.isReleased(), saving)) return;
        List<Binary> blobs = new ArrayList<>(itemSlots.length);
        List<ItemStack> left = new ArrayList<>();
        for (int slot : itemSlots) {
            ItemStack item = inventory.getItem(slot);
            Binary blob = null;
            try {
                blob = StorageItems.encode(new ItemStack[]{item}).getFirst();
            } catch (RuntimeException e) {
                Dungeons.getInstance().getLogger().log(Level.SEVERE, "Couldn't save an item in " + viewer.getName() + "'s " + getTitle(), e);
            }
            // Not written (it couldn't be, or it's what's never saved, which can't be put in): it goes to them.
            if (blob == null && item != null && !item.isEmpty()) {
                left.add(item);
                inventory.setItem(slot, null);
            }
            blobs.add(blob);
        }
        if (!store(blobs)) {
            for (int slot : itemSlots) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && !item.isEmpty()) left.add(item);
                inventory.setItem(slot, null);
            }
        }
        if (!left.isEmpty()) ItemStash.give(viewer, left.toArray(new ItemStack[0]));
        stored();
    }

    /**
     * Whether the menu writes into their profile: only once their inventory is theirs here (it's saved from
     * then on), and not after their data has been let go of, unless it's the save that lets it go (which
     * marks it let go of first, then writes the inventory and this with it).
     */
    static boolean writes(boolean restored, boolean released, boolean saving) {
        return restored && (!released || saving);
    }

    /** The storage menu a player has open; null for none (or another menu). */
    static ItemPage current(Player player) {
        return GUI.GUI_MAP.get(player.getUniqueId()) instanceof ItemPage page ? page : null;
    }

    @Override
    public boolean allowHotkeying() {
        return true;
    }

    /** Only its buttons wait out the cooldown between clicks: items move as fast as in a chest. */
    @Override
    public boolean rateLimited(InventoryClickEvent event) {
        return event.getClickedInventory() == event.getView().getTopInventory() && !isItemSlot(event.getSlot());
    }

    /**
     * A shift-click on an item in their inventory puts it in this page's item slots that take it: onto
     * stacks of the same item first, then into the first empty slot, as a chest does. What doesn't fit
     * stays where it was.
     */
    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return false;
        event.setCancelled(true);
        ItemStack moving = event.getCurrentItem();
        if (moving == null || moving.isEmpty() || !(event.getClickedInventory() instanceof PlayerInventory from)) return true;
        Inventory top = event.getView().getTopInventory();
        int left = moving.getAmount();
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (int slot : itemSlots) {
                if (!accepts(slot, moving)) continue;
                ItemStack there = top.getItem(slot);
                boolean empty = there == null || there.isEmpty();
                if (pass == 0 ? empty || !there.isSimilar(moving) : !empty) continue;
                int room = pass == 0 ? there.getMaxStackSize() - there.getAmount() : Math.min(moving.getMaxStackSize(), top.getMaxStackSize());
                int put = Math.min(room, left);
                if (put <= 0) continue;
                ItemStack placed = (pass == 0 ? there : moving).clone();
                placed.setAmount(pass == 0 ? there.getAmount() + put : put);
                top.setItem(slot, placed);
                left -= put;
                if (left == 0) break;
            }
        }
        if (left == moving.getAmount()) return true;
        ItemStack rest = null;
        if (left > 0) {
            rest = moving.clone();
            rest.setAmount(left);
        }
        from.setItem(event.getSlot(), rest);
        // Moved at once, so it's written at once.
        sync();
        return true;
    }
}
