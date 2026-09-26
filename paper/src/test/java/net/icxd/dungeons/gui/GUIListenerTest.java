package net.icxd.dungeons.gui;

import net.icxd.dungeons.listeners.InventorySyncListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GUIListenerTest {
    /**
     * A click cancelled for a frozen inventory (a hand-off) never reaches a menu's buttons, as the
     * item browser's give items: the freeze runs first, and the menus skip what it cancelled.
     */
    @Test
    void menusSkipClicksTheFreezeCancelled() throws NoSuchMethodException {
        EventHandler menus = GUIListener.class.getMethod("onInventoryClick", InventoryClickEvent.class).getAnnotation(EventHandler.class);
        EventHandler freeze = InventorySyncListener.class.getMethod("onClick", InventoryClickEvent.class).getAnnotation(EventHandler.class);
        assertTrue(menus.ignoreCancelled());
        assertTrue(freeze.priority().getSlot() < menus.priority().getSlot());
    }
}
