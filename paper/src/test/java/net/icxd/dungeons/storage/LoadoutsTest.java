package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.Rank;

/** Loadouts in the profile, and when one is what they have on (the Loadouts and Storage tour, 00:40.5 to 01:36.5). */
class LoadoutsTest {
    @Test
    void fresh() {
        Document profile = new Document();
        Loadouts.Loadout loadout = Loadouts.get(profile, 2);
        assertEquals(new Loadouts.Loadout("Loadout 3", null, null, null), loadout);
        assertFalse(loadout.customized());
        assertTrue(loadout.withPower("Silky").customized());
        assertTrue(loadout.withArmor(0).customized());
    }

    @Test
    void putAndGet() {
        Document profile = new Document();
        Loadouts.put(profile, 1, new Loadouts.Loadout("Dungeons", 1, 0, "Silky"));
        assertEquals(new Loadouts.Loadout("Dungeons", 1, 0, "Silky"), Loadouts.get(profile, 1));
        // The ones before it are there, as they were.
        assertEquals(Loadouts.fresh(0), Loadouts.get(profile, 0));
        Loadouts.put(profile, 1, Loadouts.get(profile, 1).withEquipment(null).withName("Mining"));
        assertEquals(new Loadouts.Loadout("Mining", 1, null, "Silky"), Loadouts.get(profile, 1));
        // Through the database's types: numbers come back as they went in.
        Document copy = Document.parse(profile.toJson());
        assertEquals(Loadouts.get(profile, 1), Loadouts.get(copy, 1));
        // A blank name is the loadout's own.
        Loadouts.put(profile, 0, Loadouts.fresh(0).withName(" "));
        assertEquals("Loadout 1", Loadouts.get(profile, 0).name());
    }

    @Test
    void equipped() {
        // Loadout 2 as recorded: its armor and equipment sets on, Silky selected; no "Left-click to equip!".
        Loadouts.Loadout two = new Loadouts.Loadout("Loadout 2", 1, 0, "Silky");
        assertTrue(Loadouts.equipped(two, 1, 0, "Silky"));
        // Loadout 1, armor only: its None power isn't the selected Silky, so it can be equipped (recorded).
        Loadouts.Loadout one = new Loadouts.Loadout("Loadout 1", 0, null, null);
        assertFalse(Loadouts.equipped(one, 0, 0, "Silky"));
        assertTrue(Loadouts.equipped(one, 0, -1, null));
        assertFalse(Loadouts.equipped(two, 1, 1, "Silky"));
        assertFalse(Loadouts.equipped(two, -1, 0, "Silky"));
    }

    @Test
    void slotsByRank() {
        // The wiki's Loadouts, Wardrobe and Equipment Wardrobe (no VIP or MVP here); staff have the Account Upgrades' too.
        assertEquals(List.of(4, 10, 18, 18, 27), List.of(Rank.DEFAULT, Rank.VIP_PLUS, Rank.MVP_PLUS, Rank.MVP_PLUS_PLUS, Rank.STAFF).stream()
                .map(Loadouts::loadouts).toList());
        assertEquals(List.of(4, 10, 18, 27), List.of(Rank.DEFAULT, Rank.VIP_PLUS, Rank.MVP_PLUS, Rank.STAFF).stream().map(Loadouts::armorSets)
                .toList());
        assertEquals(List.of(2, 5, 9, 18), List.of(Rank.DEFAULT, Rank.VIP_PLUS, Rank.MVP_PLUS, Rank.STAFF).stream().map(Loadouts::equipmentSets)
                .toList());
    }

    @Test
    void unlockLines() {
        // An MVP+ player's locked loadout, as recorded.
        assertEquals(List.of("&7Unlock more slots from:", "&8▶ &aAccount Upgrades &8- &69 Slots", "", "&cUnlock more slots from &dElizabeth &cat",
                "&cthe &bCommunity Center"), Loadouts.unlockLines(Rank.MVP_PLUS, Loadouts::loadouts));
        List<String> none = Loadouts.unlockLines(Rank.DEFAULT, Loadouts::loadouts);
        assertEquals(List.of("&7Unlock more slots from:", "&8▶ &aVIP&6+&a &8- &610 Slots", "&8▶ &bMVP&6+&b &8- &618 Slots",
                "&8▶ &aAccount Upgrades &8- &69 Slots", "", "&cUnlock more slots from &dElizabeth &cat", "&cthe &bCommunity Center"), none);
    }
}
