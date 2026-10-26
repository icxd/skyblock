package net.icxd.dungeons.pet;

import java.util.function.Supplier;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;

/**
 * What a menu with a pet in it asks before the pet goes, as recorded (2026-10-25: Kat's "Confirm Hiring Kat" at
 * 01:01.1, George's "Confirm Sale" at 04:07.7): three rows with nothing in them but Confirm and Cancel, opened without
 * a sound. It has the pet the menu before had ({@link PlacedPet}); Cancel goes back to that menu with it (UNKNOWN:
 * never recorded). Main thread.
 */
final class ConfirmPetMenu extends GUI {
    static final int CONFIRM = 11;
    static final int CANCEL = 15;
    static final Icon CANCEL_ICON = new Icon(Material.RED_TERRACOTTA, "&cCancel");

    private final Player viewer;
    private final PlacedPet placed;
    private final Icon confirm;
    private final Runnable confirmed;
    private final Supplier<GUI> back;

    ConfirmPetMenu(String title, Player viewer, PlacedPet placed, Icon confirm, Runnable confirmed, Supplier<GUI> back) {
        super(title, Size.THREE);
        this.viewer = viewer;
        this.placed = placed;
        this.confirm = confirm;
        this.confirmed = confirmed;
        this.back = back;
        placed.passTo(this);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        // Its empty slots take nothing.
        fill(ItemStack.empty());
        set(GUIClickableItem.button(CONFIRM, confirm.stack(), viewer, () -> {
            if (placed.ownedBy(this)) confirmed.run();
        }));
        set(GUIClickableItem.button(CANCEL, CANCEL_ICON.stack(), viewer, () -> {
            if (placed.ownedBy(this)) back.get().open(viewer);
        }));
    }

    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        return true;
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (placed.ownedBy(this)) placed.giveBack(viewer);
    }
}
