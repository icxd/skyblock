package net.icxd.dungeons.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.gui.GUIListener;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.listeners.PlayerListener;
import net.icxd.dungeons.menu.SkyBlockMenuListener.Response;

/** The SkyBlock Menu item: the recorded one (the menu tour, inventory slot 44), and what clicks do to it. */
class SkyBlockMenuItemTest {
    @Test
    void recordedItem() {
        assertEquals(8, SkyBlockMenuItem.SLOT);
        assertEquals("&aSkyBlock Menu &7(Click)", SkyBlockMenuItem.NAME);
        assertEquals(List.of("&7View all of your SkyBlock progress,", "&7including your Skills, Collections,", "&7Recipes, and more!", "",
                "&eClick to open!"), SkyBlockMenuItem.LORE);
        // Its own tag (the not-saved one is StoredInventory's), not a SkyBlock item id that items would rebuild.
        assertEquals("skyblock:skyblock_menu", SkyBlockMenuItem.KEY.toString());
    }

    /** The Magical Map has hotbar slot 9 in dungeons. */
    @Test
    void notOnDungeonServers() {
        assertFalse(SkyBlockMenuItem.gives(ServerType.DUNGEONS));
        for (ServerType type : ServerType.values()) {
            if (type != ServerType.DUNGEONS) assertTrue(SkyBlockMenuItem.gives(type), type.name());
        }
    }

    @Test
    void clicks() {
        // Left or right, shift or not, and dropping it: the menu opens.
        for (ClickType click : List.of(ClickType.LEFT, ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT, ClickType.DROP,
                ClickType.CONTROL_DROP)) {
            assertEquals(Response.OPEN, SkyBlockMenuListener.click(click, true, false, false), click.name());
        }
        // Anything else on it only does nothing.
        for (ClickType click : List.of(ClickType.NUMBER_KEY, ClickType.MIDDLE, ClickType.DOUBLE_CLICK, ClickType.SWAP_OFFHAND,
                ClickType.CREATIVE)) {
            assertEquals(Response.BLOCK, SkyBlockMenuListener.click(click, true, false, false), click.name());
        }
        // Key 9 on another slot would swap it out; so would a copy on the cursor.
        assertEquals(Response.BLOCK, SkyBlockMenuListener.click(ClickType.NUMBER_KEY, false, true, false));
        assertEquals(Response.BLOCK, SkyBlockMenuListener.click(ClickType.LEFT, false, false, true));
        assertEquals(Response.NONE, SkyBlockMenuListener.click(ClickType.LEFT, false, false, false));
    }

    /**
     * Its clicks come after the freeze and before the menus' own: a shop never sells it. It's given
     * after PlayerListener has put their stored items on them.
     */
    @Test
    void order() throws NoSuchMethodException {
        EventHandler item = SkyBlockMenuListener.class.getMethod("onClick", InventoryClickEvent.class).getAnnotation(EventHandler.class);
        EventHandler freeze = InventorySyncListener.class.getMethod("onClick", InventoryClickEvent.class).getAnnotation(EventHandler.class);
        EventHandler menus = GUIListener.class.getMethod("onInventoryClick", InventoryClickEvent.class).getAnnotation(EventHandler.class);
        assertTrue(freeze.priority().getSlot() < item.priority().getSlot());
        assertTrue(item.priority().getSlot() < menus.priority().getSlot());
        assertTrue(menus.ignoreCancelled());

        EventHandler give = SkyBlockMenuListener.class.getMethod("onJoin", PlayerJoinEvent.class).getAnnotation(EventHandler.class);
        EventHandler restore = PlayerListener.class.getMethod("onJoin", PlayerJoinEvent.class).getAnnotation(EventHandler.class);
        assertTrue(restore.priority().getSlot() < give.priority().getSlot());
    }
}
