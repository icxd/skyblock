package net.icxd.dungeons.island;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.menu.Icon;

/** Jerry against the recording (2026-10-24): his lines and their timing, his tags, his menu at 01:55.2. */
class JerryTest {
    @Test
    void whatHeSays() {
        assertEquals("&e[NPC] Jerry&f: &rUse the &5Portal &fto warp to the first of those islands - the SkyBlock Hub!", Jerry.LINES.get(2));
        // 01:47.5, 01:48.9, 01:50.9.
        assertArrayEquals(new long[]{0, 29, 69}, Jerry.LINE_TICKS);
        assertEquals(List.of("&6&lNEW UPDATE", "Jerry", "&e&lCLICK"), Jerry.TAGS);
    }

    @Test
    void hisMenu() {
        var icons = JerryMenu.icons();
        assertEquals(List.of(11, 13, 15, 27, 32, 35), List.copyOf(icons.keySet()));
        assertEquals(new Icon(Material.NETHER_BRICK, "&aRemove Hub Portal", "&7Removes all of the portal blocks", "&7from the Hub Portal on your island!", "",
                "&cRequires Level V in any skill!"), icons.get(32));
        assertEquals(new Icon(Material.BEDROCK, "&aMove Jerry"), icons.get(35));
        assertEquals(31, JerryMenu.CLOSE);
    }
}
