package net.icxd.dungeons.pet;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;

/**
 * "Confirm Hiring Kat", as recorded (2026-10-25, 01:01.1): three rows with nothing in them but Confirm ("&cWARNING:
 * You will not be able to retrieve your pet for 1 minute and its level will change as a result of the rarity
 * upgrade.") and Cancel. It has the pet the Pet Sitter had ({@link PlacedPet}); Cancel goes back to it with the pet
 * (UNKNOWN: never recorded). Main thread.
 */
final class ConfirmHiringKatMenu extends GUI {
    static final String TITLE = "Confirm Hiring Kat";
    static final int CONFIRM = 11;
    static final int CANCEL = 15;
    static final Icon CANCEL_ICON = new Icon(Material.RED_TERRACOTTA, "&cCancel");

    private final Player viewer;
    private final PlacedPet placed;

    ConfirmHiringKatMenu(Player viewer, PlacedPet placed) {
        super(TITLE, Size.THREE);
        this.viewer = viewer;
        this.placed = placed;
        placed.passTo(this);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        // Its empty slots take nothing.
        fill(ItemStack.empty());
        Kat.Offer offer = Kat.offer(placed.pet(), Pets.data());
        if (offer != null) {
            set(GUIClickableItem.button(CONFIRM, Kat.confirm(offer).stack(), viewer, () -> Kat.hire(Dungeons.getInstance(), viewer, placed)));
        }
        set(GUIClickableItem.button(CANCEL, CANCEL_ICON.stack(), viewer, () -> {
            if (placed.ownedBy(this)) new PetSitterMenu(viewer, placed).open(viewer);
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
