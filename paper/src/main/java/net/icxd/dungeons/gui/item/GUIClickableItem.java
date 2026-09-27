package net.icxd.dungeons.gui.item;

import java.util.function.Consumer;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.utils.StackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;


public interface GUIClickableItem extends GUIItem {
    void run(InventoryClickEvent event);

    static GUIClickableItem close(int slot) {
        return new GUIClickableItem() {
            @Override public void run(InventoryClickEvent event) { event.getWhoClicked().closeInventory(); }
            @Override public int slot() { return slot; }
            @Override public ItemStack stack() {
                return new StackBuilder(Material.BARRIER)
                        .setDisplayName("&cClose")
                        .build();
            }
        };
    }

    /** Whether a button answers this click: a left or right one, shift or not (not a double click, a number key or a middle click). */
    static boolean pressed(ClickType click) {
        return click == ClickType.LEFT || click == ClickType.RIGHT || click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;
    }

    /**
     * A menu's button: a left or right click on it (shift or not) runs {@code action} with that click
     * on the next tick, not inside the click, if {@code viewer} is still on.
     */
    static GUIClickableItem button(int slot, ItemStack stack, Player viewer, Consumer<ClickType> action) {
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                ClickType click = event.getClick();
                if (!pressed(click)) return;
                Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
                    if (viewer.isOnline()) action.accept(click);
                });
            }

            @Override
            public int slot() {
                return slot;
            }

            @Override
            public ItemStack stack() {
                return stack;
            }
        };
    }

    /** {@link #button(int, ItemStack, Player, Consumer)} for an action that doesn't need the click. */
    static GUIClickableItem button(int slot, ItemStack stack, Player viewer, Runnable action) {
        return button(slot, stack, viewer, click -> action.run());
    }
}
