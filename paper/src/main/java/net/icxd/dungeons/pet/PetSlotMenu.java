package net.icxd.dungeons.pet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.user.User;

/**
 * A menu with a slot for a pet (Kat's Pet Sitter, George's Offer Pets), as recorded (2026-10-25, 00:51.7 and 00:53.9):
 * a pet item clicked in their inventory goes in it ({@link #PET}, the one empty slot among the glass), clicked there
 * it goes back, and it's back in their inventory when the menu closes ({@link PlacedPet}). Another pet clicked takes
 * its place, the first going back (UNKNOWN). Everything else in their inventory stays put. Main thread.
 */
abstract class PetSlotMenu extends GUI {
    static final int PET = 13;

    protected final Player viewer;
    protected PlacedPet placed;

    PetSlotMenu(String title, int size, Player viewer, PlacedPet placed) {
        super(title, size);
        this.viewer = viewer;
        this.placed = placed;
        if (placed != null) placed.passTo(this);
    }

    @Override
    public final void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        set(GUIClickableItem.button(PET, placed == null ? ItemStack.empty() : placed.item(), viewer, () -> takeBack()));
        draw();
    }

    /** The rest of it, over the glass: what it does with the pet in {@link #placed} (null: none yet). */
    abstract void draw();

    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack cursor = event.getCursor();
        if (!GUIClickableItem.pressed(event.getClick()) || (cursor != null && !cursor.isEmpty())) return true;
        int slot = event.getSlot();
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (viewer.isOnline() && GUI_MAP.get(viewer.getUniqueId()) == this) place(slot);
        });
        return true;
    }

    private void place(int slot) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(viewer)) return;
        ItemStack stack = viewer.getInventory().getItem(slot);
        if (stack == null || stack.isEmpty() || PetItems.pet(ItemNBT.read(stack)) == null) return;
        PlacedPet before = placed;
        placed = PlacedPet.take(viewer, user.profile(), slot, this);
        if (before != null) before.giveBack(viewer);
        redraw();
    }

    private void takeBack() {
        if (placed == null) return;
        placed.giveBack(viewer);
        placed = null;
        redraw();
    }

    /** Shows it again as it is now, without closing it (which would give the pet back). */
    void redraw() {
        if (GUI_MAP.get(viewer.getUniqueId()) != this) return;
        beforeOpen(viewer);
        refresh(viewer.getOpenInventory().getTopInventory());
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (placed != null && placed.ownedBy(this)) placed.giveBack(viewer);
    }
}
