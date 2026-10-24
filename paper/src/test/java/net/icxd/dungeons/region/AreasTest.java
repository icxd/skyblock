package net.icxd.dungeons.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

/** Against the recording (2026-10-24): where it crossed from one area into another, and what finding one said. */
class AreasTest {
    private static String at(double x, double y, double z) {
        return RegionType.getRegionType(x, y, z).getName();
    }

    /** Every recorded crossing falls on its side of the boxes' lines: the spot as the action bar named the area. */
    @Test
    void theRecordedCrossings() {
        assertEquals("Village", at(0.5, 77, -0.5));
        assertEquals("Community Center", at(-0.1, 79, 7.6));
        assertEquals("Village", at(0.1, 78.4, 3.9));
        assertEquals("Community Center", at(8.6, 79, 13.6));
        assertEquals("Election Room", at(0.5, 50, 45.5));
        assertEquals("Auction House", at(-29.1, 72.1, -11.9));
        assertEquals("Village", at(-22.5, 72.5, -15.5));
        assertEquals("Bazaar Alley", at(-28.7, 71.6, -26.1));
        assertEquals("Village", at(-25.1, 71.4, -34.2));
        assertEquals("Bank", at(-25.5, 72, -37));
        assertEquals("Village", at(-18.7, 72, -34.6));
        assertEquals("Farm", at(43.0, 70.6, -69.7));
        assertEquals("Village", at(37.3, 71.5, -65));
        assertEquals("Bazaar Alley", at(-30.8, 72, -29.4));
        assertEquals("Village", at(-23.1, 71.5, -24.5));
        // Where the NPCs of those areas stand.
        assertEquals("Bank", at(-29.5, 72, -38));
        assertEquals("Auction House", at(-39.5, 73, -12.5));
        assertEquals("None", at(500, 70, 500));
    }

    @Test
    void eachAreaIsFoundOnce() {
        Document profile = new Document();
        assertTrue(Areas.discover(profile, "Village"));
        assertFalse(Areas.discover(profile, "Village"));
        assertTrue(Areas.discover(profile, "Bank"));
        assertEquals(List.of("Village", "Bank"), profile.getList(Areas.KEY, String.class));
    }

    /** 03:17.0, 05:13.0: the chat as Hypixel wrote it (its icon is its resource pack's; ⏣ here). */
    @Test
    void whatFindingOneSays() {
        assertEquals(List.of("", " &6&lNEW AREA DISCOVERED!", "  &7⏣ &bVillage", "",
                "&7   ■ &fExplore the area to learn more about &bSkyBlock&f.", "&7   ■ &fVisit the &6Auction House&f.",
                "&7   ■ &fManage your &6coins &fin the &6Bank&f.", ""), Areas.discovery(RegionType.VILLAGE.displayName().replace('§', '&'),
                Areas.FOUND.get("Village")));
        assertEquals(List.of("Talk to the Banker.", "Store your coins to keep them safe.", "Earn interest on your coins."), Areas.FOUND.get("Bank"));
        assertEquals("§eFarm", RegionType.FARM.displayName());
        assertEquals("§6Bank", RegionType.BANK.displayName());
    }

    /** Each area's fanfare, as the packet had it: volume 0.3, pitch 1.4920635 (94/63). */
    @Test
    void theFanfareAsRecorded() {
        assertEquals(0.3f, Areas.FOUND_VOLUME);
        assertEquals(1.4920635f, Areas.FOUND_PITCH, 1e-6);
    }
}
