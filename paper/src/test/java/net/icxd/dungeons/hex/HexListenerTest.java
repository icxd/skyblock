package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.gui.GUIListener;

/** What the Hex's input slot takes: one item, and only what storage would keep. */
class HexListenerTest {
    @Test
    void oneItemAtATime() {
        // Into the empty slot: one.
        assertNull(HexListener.refusal(0, 1, true, null));
        // A stack, or one more on the one there: refused, and they're told.
        assertEquals(HexListener.ONE_ITEM, HexListener.refusal(0, 64, true, null).message());
        assertEquals(HexListener.ONE_ITEM, HexListener.refusal(1, 1, true, null).message());
        // Taking it out is always fine.
        assertNull(HexListener.refusal(1, 0, true, null));
        assertNull(HexListener.refusal(1, 0, false, "&cNo!"));
    }

    @Test
    void notWhatStorageRefuses() {
        // A dungeon's map, the SkyBlock Menu: refused without a word, as storage refuses them.
        assertNull(HexListener.refusal(0, 1, false, null).message());
        assertNull(HexListener.refusal(0, 1, false, "&cNo!").message());
    }

    @Test
    void notWhatTheScreenRefuses() {
        // The grinder's: an item without gemstone slots, said before the one-item rule (a stack of them too).
        String grinder = "&cOnly items that can have Gemstones applied to them can be put in the Grinder!";
        assertEquals(grinder, HexListener.refusal(0, 1, true, grinder).message());
        assertEquals(grinder, HexListener.refusal(0, 64, true, grinder).message());
        // "" refuses without a word.
        assertNull(HexListener.refusal(0, 1, true, "").message());
    }

    @Test
    void afterTheMenusButtons() throws NoSuchMethodException {
        EventHandler slot = HexListener.class.getMethod("onClick", InventoryClickEvent.class).getAnnotation(EventHandler.class);
        EventHandler menus = GUIListener.class.getMethod("onInventoryClick", InventoryClickEvent.class).getAnnotation(EventHandler.class);
        assertTrue(slot.ignoreCancelled());
        assertTrue(menus.priority().getSlot() < slot.priority().getSlot());
    }
}
