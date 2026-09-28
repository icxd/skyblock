package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.utils.Text;

/** The Hex's main menu against the wiki's screens (The Hex/UI and its tabs) and the official screenshot's pane. */
class HexMenuTest {
    private static final int[] PANES = {12, 13, 14, 21, 23, 30, 31, 32};

    @Test
    void buttonsFillAsManyColumnsAsTheyNeedRowByRow() {
        // The wiki's: one (an accessory's Modifiers) in 15; five (a weapon's, an armor piece's) in 15, 16, 24, 25, 33.
        assertEquals(List.of(15), HexMenu.buttonSlots(1));
        assertEquals(List.of(15, 16, 24, 25, 33), HexMenu.buttonSlots(5));
        // The rest by the same rule (UNKNOWN on Hypixel).
        assertEquals(List.of(15, 24), HexMenu.buttonSlots(2));
        assertEquals(List.of(15, 24, 33), HexMenu.buttonSlots(3));
        assertEquals(List.of(15, 16, 24, 25), HexMenu.buttonSlots(4));
        assertEquals(List.of(15, 16, 24, 25, 33, 34), HexMenu.buttonSlots(6));
        assertEquals(List.of(15, 16, 17, 24, 25, 26, 33), HexMenu.buttonSlots(7));
        assertEquals(List.of(), HexMenu.buttonSlots(0));
    }

    private static List<HexCategory> categories(int applying) {
        List<HexCategory> categories = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            int n = i;
            categories.add(HexFakes.category("Category " + i, i >= 5 ? 25 : 0, item -> n < applying, item -> List.of("  &7Line " + n + " &c✖")));
        }
        return categories;
    }

    @Test
    void empty() {
        Map<Integer, Icon> icons = HexMenu.icons(new HexMenu.View(false, null, categories(7), level -> true));
        Icon gray = new Icon(Material.GRAY_STAINED_GLASS_PANE, "&d&kGive your life to The Hex!", "&7Upgrade an item with a variety",
                "&7of bells and whistles, all in", "&7one place!", "", "&8The Hex - Your one-stop shop", "&8for personal refinement!",
                "&d&kGive your life to The Hex!");
        for (int slot : PANES) assertEquals(gray, icons.get(slot));
        // No buttons, not even for categories that apply to everything.
        assertEquals(PANES.length, icons.size());
    }

    @Test
    void displeased() {
        Icon red = new Icon(Material.RED_STAINED_GLASS_PANE, "&cThe Hex is displeased!", "&7You cannot modify this item!");
        // Not a SkyBlock item.
        Map<Integer, Icon> icons = HexMenu.icons(new HexMenu.View(true, null, categories(7), level -> true));
        for (int slot : PANES) assertEquals(red, icons.get(slot));
        assertEquals(PANES.length, icons.size());
        // A SkyBlock item no category is for (an item of no type: no book goes on it, say).
        HexItem thing = HexFakes.hexItem(HexFakes.item("TEST_THING", "Test Thing", Rarity.COMMON, SpecificItemType.NONE));
        icons = HexMenu.icons(new HexMenu.View(true, thing, HexCategories.all(), level -> true));
        for (int slot : PANES) assertEquals(red, icons.get(slot));
        assertEquals(PANES.length, icons.size());
    }

    @Test
    void summaryAndButtons() {
        HexItem sword = HexFakes.sword();
        Map<Integer, Icon> icons = HexMenu.icons(new HexMenu.View(true, sword, categories(5), level -> true));
        Icon pane = icons.get(12);
        assertEquals(Material.PURPLE_STAINED_GLASS_PANE, pane.material());
        assertEquals("&d&kGive your mind to The Hex!", pane.name());
        assertEquals(List.of("&7Upgrade your §6Test Sword &7with", "&7a variety of bells and", "&7whistles, all in one place!", "",
                "  &7Line 0 &c✖", "", "  &7Line 1 &c✖", "", "  &7Line 2 &c✖", "", "  &7Line 3 &c✖", "", "  &7Line 4 &c✖", "",
                "&8The Hex - Your one-stop shop", "&8for personal refinement!", "&d&kGive your mind to The Hex!"), pane.lore());
        for (int slot : PANES) assertEquals(pane, icons.get(slot));
        int[] buttons = {15, 16, 24, 25, 33};
        for (int i = 0; i < buttons.length; i++) {
            assertEquals(new Icon(Material.BOOK, "&aCategory " + i, List.of("&7What Category " + i + " does.", "", "  &7Line " + i + " &c✖", "",
                    "&eClick to view!")), icons.get(buttons[i]));
        }
        assertEquals(PANES.length + 5, icons.size());
    }

    @Test
    void anUnmetCarpentryLevelShowsInPlaceOfClickToView() {
        Map<Integer, Icon> icons = HexMenu.icons(new HexMenu.View(true, HexFakes.sword(), categories(7), level -> level < 25));
        assertEquals("&eClick to view!", icons.get(15).lore().getLast());
        // Categories 5 and 6 need Carpentry 25: in 26 and 33.
        assertEquals("&4❣ &cRequires &aCarpentry Skill 25&c.", icons.get(26).lore().getLast());
        assertEquals("&4❣ &cRequires &aCarpentry Skill 25&c.", icons.get(33).lore().getLast());
        assertEquals(PANES.length + 7, icons.size());
    }

    @Test
    void anAccessoryGivesItsTime() {
        HexItem ring = HexFakes.hexItem(HexFakes.item("TEST_RING", "Test Ring", Rarity.RARE, SpecificItemType.ACCESSORY));
        Icon pane = HexMenu.icons(new HexMenu.View(true, ring, categories(1), level -> true)).get(12);
        assertEquals("&d&kGive your time to The Hex!", pane.name());
        assertEquals("&d&kGive your time to The Hex!", pane.lore().getLast());
    }

    @Test
    void aCategoryWithNoLinesAddsNoGroup() {
        List<HexCategory> categories = List.of(HexFakes.category("Quiet", 0, item -> true, item -> List.of()),
                HexFakes.category("Loud", 0, item -> true, item -> List.of("  &7Loud &a✔")));
        List<String> lore = HexMenu.icons(new HexMenu.View(true, HexFakes.sword(), categories, level -> true)).get(12).lore();
        assertEquals(List.of("", "  &7Loud &a✔", ""), lore.subList(3, 6));
        assertEquals(List.of("&7What Quiet does.", "", "&eClick to view!"), HexMenu.icons(new HexMenu.View(true, HexFakes.sword(), categories,
                level -> true)).get(15).lore());
    }

    @Test
    void theScreenshotsWrap() {
        // The official 0.14 screenshot: a Fabled Livid Dagger with five stars.
        List<String> lore = HexMenu.paneLore("&dFabled Livid Dagger &6✪✪✪✪✪", List.of(), "&d&kGive your mind to The Hex!");
        assertEquals(List.of("&7Upgrade your &dFabled Livid", "&dDagger &6✪✪✪✪✪ &7with a", "&7variety of bells and whistles,",
                "&7all in one place!"), lore.subList(0, 4));
        // The gray pane's lines are the same width's.
        assertEquals(List.of("&7Upgrade an item with a variety", "&7of bells and whistles, all in", "&7one place!"),
                Text.wrap("&7Upgrade an item with a variety of bells and whistles, all in one place!", HexMenu.PANE_WIDTH, HexMenu::width));
    }

    @Test
    void theItemSlotIsNeverAButton() {
        Map<Integer, Icon> icons = HexMenu.icons(new HexMenu.View(true, HexFakes.sword(), categories(7), level -> true));
        assertNull(icons.get(HexMenu.ITEM));
        assertFalse(icons.containsKey(HexMenu.CLOSE));
    }
}
