package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.hex.HexPage.Placement;
import net.icxd.dungeons.menu.Icon;

/** The Hex's pages' frame and grid, against the wiki's screens and NEU's Hex. */
class HexPageTest {
    @Test
    void centredAsTheWikisScreens() {
        assertEquals(List.of(23), HexPage.centred(1));
        assertEquals(List.of(21, 23, 25), HexPage.centred(3));
        assertEquals(List.of(21, 22, 24, 25), HexPage.centred(4));
        assertEquals(List.of(21, 22, 23, 24, 25), HexPage.centred(5));
        assertEquals(List.of(22, 23, 24, 31, 32, 33), HexPage.centred(6));
        assertEquals(List.of(12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31), HexPage.centred(12));
        // UNKNOWN on Hypixel: 2 with 4's gap in the middle, 7 and up row by row.
        assertEquals(List.of(22, 24), HexPage.centred(2));
        assertEquals(List.of(12, 13, 14, 15, 16, 21, 22), HexPage.centred(7));
        assertEquals(List.of(), HexPage.centred(0));
    }

    @Test
    void rowMajorFillsFromTwelve() {
        // The wiki's third page of sword enchantments: 4 in 12-15.
        assertEquals(Map.of(12, 30, 13, 31, 14, 32, 15, 33), HexPage.grid(34, 2, Placement.ROW_MAJOR));
        Map<Integer, Integer> full = HexPage.grid(34, 0, Placement.ROW_MAJOR);
        assertEquals(List.of(12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34), new ArrayList<>(full.keySet()));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14), new ArrayList<>(full.values()));
    }

    @Test
    void aCentredListCentresEachPage() {
        // 20 reforge stones: a full first page, then 5 in the middle row (the wiki's 5).
        assertEquals(15, HexPage.grid(20, 0, Placement.CENTRED).size());
        assertEquals(Map.of(21, 15, 22, 16, 23, 17, 24, 18, 25, 19), HexPage.grid(20, 1, Placement.CENTRED));
        assertEquals(Map.of(), HexPage.grid(0, 0, Placement.CENTRED));
    }

    @Test
    void pages() {
        assertEquals(1, HexPage.pages(0));
        assertEquals(1, HexPage.pages(15));
        assertEquals(2, HexPage.pages(16));
        // Swords' 34 enchantments: 15, 15, 4 (the wiki).
        assertEquals(3, HexPage.pages(34));
    }

    @Test
    void frame() {
        Icon header = new Icon(Material.ANVIL, "&aApply Books", "&7Knowledge is &6power&7! Apply");
        Map<Integer, Icon> first = HexPage.frame(34, 0, header, "&7To The Hex");
        assertEquals(header, first.get(HexPage.HEADER));
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To The Hex"), first.get(HexPage.BACK));
        // No previous page; the next is page 2 (NEU reads the number off "&8Page 2").
        assertFalse(first.containsKey(HexPage.PREVIOUS));
        assertEquals(new Icon(Material.ARROW, "&aNext Page", "&8Page 2"), first.get(HexPage.NEXT));
        Map<Integer, Icon> second = HexPage.frame(34, 1, header, "&7To The Hex");
        assertEquals(new Icon(Material.ARROW, "&aPrevious Page", "&8Page 1"), second.get(HexPage.PREVIOUS));
        assertEquals(new Icon(Material.ARROW, "&aNext Page", "&8Page 3"), second.get(HexPage.NEXT));
        Map<Integer, Icon> last = HexPage.frame(34, 2, header, "&7To Enchant Item");
        assertEquals(new Icon(Material.ARROW, "&aPrevious Page", "&8Page 2"), last.get(HexPage.PREVIOUS));
        assertFalse(last.containsKey(HexPage.NEXT));
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To Enchant Item"), last.get(HexPage.BACK));
        // One page: no arrows.
        assertEquals(2, HexPage.frame(3, 0, header, "&7To The Hex").size());
    }
}
