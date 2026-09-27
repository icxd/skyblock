package net.icxd.dungeons.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.gui.GUIListener;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.listeners.PlayerListener;
import net.icxd.dungeons.menu.SkyBlockMenuItem.Plan;
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

    /** A player's inventory in Bukkit's slots: 36 storage, 4 armor, the off hand. */
    private static final int SIZE = 41;

    private static Plan plan(int[] items, int[] filled) {
        boolean[] item = new boolean[SIZE];
        boolean[] empty = new boolean[SIZE];
        Arrays.fill(empty, true);
        for (int slot : items) {
            item[slot] = true;
            empty[slot] = false;
        }
        for (int slot : filled) empty[slot] = false;
        return Plan.of(item, empty);
    }

    /** Every storage slot but those given. */
    private static int[] storageBut(int... free) {
        return IntStream.range(0, Plan.STORAGE).filter(slot -> IntStream.of(free).noneMatch(f -> f == slot)).toArray();
    }

    @Test
    void give() {
        // There already: nothing changes but copies elsewhere going, the off hand's too.
        assertEquals(new Plan(List.of(20, 40), -1, false), plan(new int[] {8, 20, 40}, new int[0]));
        assertEquals(new Plan(List.of(), -1, false), plan(new int[] {8}, storageBut()));
        // Its slot empty: it's put there.
        assertEquals(new Plan(List.of(), -1, true), plan(new int[0], new int[] {0, 1, 2}));
        // Something in its slot moves to the first empty storage slot.
        assertEquals(new Plan(List.of(), 5, true), plan(new int[0], new int[] {0, 1, 2, 3, 4, 8}));
        assertEquals(new Plan(List.of(), 9, true), plan(new int[0], new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8}));
        // A copy's slot is free once it's taken away.
        assertEquals(new Plan(List.of(30), 30, true), plan(new int[] {30}, storageBut(30)));
        // No room in storage (empty armor slots don't count, and a copy in the off hand frees none): wait.
        assertEquals(new Plan(List.of(), -1, false), plan(new int[0], storageBut()));
        assertEquals(new Plan(List.of(40), -1, false), plan(new int[] {40}, storageBut()));
    }

    /**
     * A pick (a middle click on a block) with a full hotbar can be given its slot; it goes where the
     * server would put it without the item there.
     */
    @Test
    void picks() {
        boolean[] full = new boolean[9];
        boolean[] plain = new boolean[9];
        boolean[] enchanted = {true, true, true, true, true, true, true, true, false};
        // Holding it: the next slot on, from the start of the hotbar.
        assertEquals(0, SkyBlockMenuListener.pickSlot(8, full, plain));
        // The server skips enchanted slots from the selected one on, and would reach it next.
        boolean[] sixAndSeven = {false, false, false, false, false, false, true, true, false};
        assertEquals(0, SkyBlockMenuListener.pickSlot(6, full, sixAndSeven));
        // Every other slot enchanted: the selected one, as the server does; holding it, no pick at all.
        assertEquals(3, SkyBlockMenuListener.pickSlot(3, full, enchanted));
        assertEquals(-1, SkyBlockMenuListener.pickSlot(8, full, enchanted));
        // An empty slot first, if there is one.
        boolean[] fiveEmpty = new boolean[9];
        fiveEmpty[5] = true;
        assertEquals(5, SkyBlockMenuListener.pickSlot(8, fiveEmpty, enchanted));
        // Never its slot, whatever the hotbar holds.
        boolean[] someEmpty = new boolean[9];
        boolean[] someEnchanted = new boolean[9];
        for (int emptyMask = 0; emptyMask < 1 << 9; emptyMask++) {
            for (int enchantedMask = 0; enchantedMask < 1 << 9; enchantedMask++) {
                for (int slot = 0; slot < 9; slot++) {
                    someEmpty[slot] = (emptyMask >> slot & 1) == 1;
                    someEnchanted[slot] = (enchantedMask >> slot & 1) == 1;
                }
                for (int selected = 0; selected < 9; selected++) {
                    int pick = SkyBlockMenuListener.pickSlot(selected, someEmpty, someEnchanted);
                    assertTrue(pick >= -1 && pick < 9 && pick != SkyBlockMenuItem.SLOT);
                }
            }
        }
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
