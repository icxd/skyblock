package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.menu.Icon;

/**
 * Storage's menus against the recorded ones: Storage and its Ender Chest pages and backpacks (the Loadouts and
 * Storage tour, 02:09.3 to 02:50.7).
 */
class StorageMenusTest {
    // Storage

    /** The recorded one: five pages, four Greater Backpacks. */
    private static StorageMenu.View recorded() {
        List<StorageMenu.BackpackSlot> backpacks = new ArrayList<>();
        for (int slot = 1; slot <= StorageMenu.BACKPACK_SLOTS; slot++) {
            backpacks.add(slot <= 4 ? new StorageMenu.BackpackSlot(StorageMenu.SlotState.FILLED, "&5Greater Backpack", 36)
                    : StorageMenu.BackpackSlot.EMPTY);
        }
        return new StorageMenu.View(5, Arrays.asList(new String[StorageMenu.PAGES]), backpacks);
    }

    @Test
    void storage() {
        Map<Integer, Shown> icons = StorageMenu.icons(recorded());
        assertEquals(Shown.of(new Icon(Material.ENDER_CHEST, "&aEnder Chest", "&7Store global items you can access",
                "&7anywhere in your ender chest.")), icons.get(4));
        for (int page = 1; page <= 5; page++) {
            assertEquals(Shown.of(new Icon(Material.PURPLE_STAINED_GLASS_PANE, "&aEnder Chest Page " + page, "",
                    "&8Also accessible via /enderchest " + page, "", "&eLeft-click to open!", "&eRight-click to change icon!"), page),
                    icons.get(8 + page));
        }
        for (int slot = 14; slot <= 17; slot++) {
            assertEquals(Shown.of(new Icon(Material.RED_STAINED_GLASS_PANE, "&cLocked Page", "&7Unlock more Ender Chest pages in",
                    "&7the community shop!")), icons.get(slot));
        }
        assertEquals(Shown.of(new Icon(Material.CHEST, "&aBackpacks", "&7Place backpack items in these slots", "&7to use them as additional storage",
                "&7that can be accessed anywhere.")), icons.get(22));
        for (int slot = 1; slot <= 4; slot++) {
            assertEquals(Shown.of(new Icon(Material.PLAYER_HEAD, "&6Backpack Slot " + slot, "&5Greater Backpack", "&7This backpack has &a36&7 slots.",
                    "", "&8Also accessible via /backpack " + slot, "", "&eLeft-click to open!", "&eRight-click to remove!"), slot),
                    icons.get(26 + slot));
        }
        for (int slot = 5; slot <= 18; slot++) {
            assertEquals(Shown.of(new Icon(Material.BROWN_STAINED_GLASS_PANE, "&eEmpty Backpack Slot " + slot, "",
                    "&eLeft-click a backpack item on this", "&eslot to place it!"), slot), icons.get(26 + slot));
        }
        assertEquals(1 + 9 + 1 + 18, icons.size());
    }

    @Test
    void storageView() {
        // Everyone has every page and slot for now (the Community Shop and Tia aren't here).
        Document storage = new Document();
        StorageMenu.View none = StorageMenu.view(storage);
        assertEquals(StorageMenu.PAGES, none.pages());
        assertTrue(none.backpacks().stream().allMatch(b -> b.state() == StorageMenu.SlotState.EMPTY));

        // A page's icon in place of its glass; a backpack shows in its slot.
        StorageDocument.setIcon(storage, 1, "INK_SAC");
        StorageDocument.placeBackpack(storage, 3, "JUMBO_BACKPACK", new Binary(new byte[]{1}));
        StorageMenu.View view = StorageMenu.view(storage);
        assertEquals(Material.INK_SAC, StorageMenu.icons(view).get(9).icon().material());
        assertEquals(StorageMenu.SlotState.FILLED, view.backpacks().get(2).state());
        assertEquals(StorageMenu.SlotState.EMPTY, view.backpacks().get(3).state());
    }

    @Test
    void lockedSlots() {
        // How a locked backpack slot shows, for when slots come from Tia again (the wiki's Storage/UI).
        List<StorageMenu.BackpackSlot> backpacks = new ArrayList<>();
        for (int slot = 1; slot <= StorageMenu.BACKPACK_SLOTS; slot++) {
            backpacks.add(slot == 1 ? StorageMenu.BackpackSlot.EMPTY : StorageMenu.BackpackSlot.LOCKED);
        }
        StorageMenu.View one = new StorageMenu.View(1, Arrays.asList(new String[StorageMenu.PAGES]), backpacks);
        assertEquals(Shown.of(new Icon(Material.GRAY_DYE, "&cLocked Backpack Slot 2", "&7Talk to Tia the Fairy to", "&7unlock more Backpack Slots!")),
                StorageMenu.icons(one).get(28));
    }

    @Test
    void pageBar() {
        // Page 1 of 5: only onwards (recorded 02:15.8); 2 of 5: all four (02:17.7); 5 of 5: only back (02:19.7).
        assertEquals(0, PageBar.target(PageBar.FIRST, 1, 5));
        assertEquals(2, PageBar.target(PageBar.NEXT, 1, 5));
        assertEquals(5, PageBar.target(PageBar.LAST, 1, 5));
        assertEquals(1, PageBar.target(PageBar.FIRST, 2, 5));
        assertEquals(1, PageBar.target(PageBar.PREVIOUS, 2, 5));
        assertEquals(4, PageBar.target(PageBar.PREVIOUS, 5, 5));
        assertEquals(0, PageBar.target(PageBar.NEXT, 5, 5));
        assertEquals(0, PageBar.target(PageBar.BACK, 3, 5));
        assertEquals(0, PageBar.target(PageBar.NEXT, 1, 1));

        Map<Integer, Shown> first = PageBar.icons(1, 5, null);
        assertEquals(new Icon(Material.BARRIER, "&cClose"), first.get(0).icon());
        assertEquals(new Icon(Material.ARROW, "&eBack"), first.get(1).icon());
        for (int slot = 2; slot <= 6; slot++) assertEquals(Shown.blank(Material.BLACK_STAINED_GLASS_PANE), first.get(slot));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aNext Page →", List.of(), PageBar.NEXT_HEAD), first.get(7).icon());
        assertEquals(new Icon(Material.PLAYER_HEAD, "&eLast Page »", List.of(), PageBar.LAST_HEAD), first.get(8).icon());
        Map<Integer, Shown> last = PageBar.icons(5, 5, "INK_SAC");
        assertEquals(new Icon(Material.PLAYER_HEAD, "&e« First Page", List.of(), PageBar.FIRST_HEAD), last.get(5).icon());
        assertEquals(new Icon(Material.PLAYER_HEAD, "&a← Previous Page", List.of(), PageBar.PREVIOUS_HEAD), last.get(6).icon());
        // The page's icon where the glass was.
        assertEquals(Shown.blank(Material.INK_SAC), last.get(8));
        assertEquals(9, last.size());
    }

    @Test
    void chooseAnIcon() {
        Map<Integer, Shown> icons = IconMenu.icons();
        List<String> says = List.of("&7Ender Chest icons replace the glass", "&7panes in the navigation bar.", "");
        List<String> reset = new ArrayList<>(says);
        reset.add("&eClick to reset!");
        assertEquals(Shown.of(new Icon(Material.BARRIER, "&cReset", reset)), icons.get(IconMenu.RESET));
        List<String> select = new ArrayList<>(says);
        select.add("&eClick to select!");
        // The recorded page: the middle seven of four rows.
        List<Integer> slots = List.of(11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43);
        for (int i = 0; i < slots.size(); i++) {
            assertEquals(Shown.of(new Icon(IconMenu.ICONS.get(i), null, select)), icons.get(slots.get(i)), "slot " + slots.get(i));
        }
        assertEquals(Material.COAL, icons.get(11).icon().material());
        assertEquals(Material.PRISMARINE_CRYSTALS, icons.get(43).icon().material());
        assertEquals(28, icons.size());
    }

    // Backpacks

    @Test
    void backpackSize() {
        // A backpack's own words (items.json).
        assertEquals(36, StorageItems.backpackSize(List.of("&7A bag with &a36&7 slots which can be", "&7placed in your Storage Menu")));
        assertEquals(9, StorageItems.backpackSize(List.of("", "&7A bag with &a9&7 slots which can be")));
        assertEquals(0, StorageItems.backpackSize(List.of("&7A bag of holding")));
        assertEquals(0, StorageItems.backpackSize((net.icxd.dungeons.item.SkyBlockItem) null));
    }
}
