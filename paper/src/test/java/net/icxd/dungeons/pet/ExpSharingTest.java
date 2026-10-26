package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.Skill;

/** Exp Sharing's rules (the wiki's Pets and Kuudra Pet) and its recorded menu (2026-10-25, 03:18.6). */
class ExpSharingTest {
    private static final PetData DATA = PetFixtures.fixture();
    private static final OwnedPet BEE = OwnedPet.of("BEE", Rarity.COMMON);
    private static final OwnedPet GUARDIAN = OwnedPet.of("GUARDIAN", Rarity.MYTHIC);
    private static final OwnedPet KUUDRA = OwnedPet.of("KUUDRA", Rarity.LEGENDARY);

    private static Document profile(OwnedPet... pets) {
        Document profile = new Document();
        Pets.set(profile, Arrays.asList(pets));
        return profile;
    }

    @Test
    void theRate() {
        assertEquals(0, ExpSharing.rate(0, null), 1e-9);
        assertEquals(12, ExpSharing.rate(60, null), 1e-9, "0.2% a Taming level");
    }

    @Test
    void itsSlots() {
        Document profile = profile(BEE, GUARDIAN);
        assertEquals(Arrays.asList(null, null, null), ExpSharing.slots(profile));
        ExpSharing.put(profile, 0, BEE.uuid());
        assertEquals(BEE, ExpSharing.slots(profile).getFirst());
        ExpSharing.put(profile, 2, BEE.uuid());
        assertEquals(Arrays.asList(null, null, BEE), ExpSharing.slots(profile), "out of the one it was in");
        Pets.remove(profile, BEE.uuid());
        assertEquals(Arrays.asList(null, null, null), ExpSharing.slots(profile), "it's left the menu");
    }

    @Test
    void whoShares() {
        Document profile = profile(BEE, GUARDIAN, KUUDRA);
        assertEquals(List.of(), ExpSharing.sharers(profile, GUARDIAN, false));
        ExpSharing.put(profile, 0, BEE.uuid());
        assertEquals(List.of(new ExpSharing.Sharer(BEE, 1, true)), ExpSharing.sharers(profile, GUARDIAN, false));
        assertEquals(List.of(), ExpSharing.sharers(profile, BEE, false), "the summoned one gains no more");
        ExpSharing.put(profile, 1, BEE.uuid());
        assertEquals(List.of(), ExpSharing.sharers(profile, GUARDIAN, false), "slot #2 needs Diana");
        // The Kuudra pet: off the Crimson Isle never; there always, twice in a slot, its item's boost not counting.
        assertEquals(List.of(new ExpSharing.Sharer(KUUDRA, 1, false)), ExpSharing.sharers(profile, GUARDIAN, true));
        ExpSharing.put(profile, 0, KUUDRA.uuid());
        assertEquals(List.of(), ExpSharing.sharers(profile, GUARDIAN, false));
        assertEquals(List.of(new ExpSharing.Sharer(KUUDRA, 2, false)), ExpSharing.sharers(profile, GUARDIAN, true));
    }

    @Test
    void theirXp() {
        // 100 of the summoned pet's, at Taming 10: 2% of it, then Taming's +10%.
        assertEquals(2.2, ExpSharing.xp(new ExpSharing.Sharer(BEE, 1, true), DATA.pet("BEE"), Skill.FARMING, 100, 10), 1e-9);
        assertEquals(4.4, ExpSharing.xp(new ExpSharing.Sharer(KUUDRA, 2, false), null, Skill.FARMING, 100, 10), 1e-9);
        assertEquals(4.4, ExpSharing.xp(new ExpSharing.Sharer(OwnedPet.of("REINDEER", Rarity.LEGENDARY), 1, true), DATA.pet("REINDEER"),
                Skill.FISHING, 100, 10), 1e-9, "its own x2");
    }

    @Test
    void theSlotsAsRecorded() {
        Icon first = ExpSharingMenu.slot(0, null, 0);
        assertEquals(Material.RED_STAINED_GLASS_PANE, first.material());
        assertEquals("&cExp Share Slot #1", first.name());
        assertEquals(List.of("&7Let your pets gain &aPet Exp &7without", "&7them being actively deployed!", "", "&c&lINACTIVE"), first.lore());
        assertEquals(List.of("&7Let your pets gain &aPet Exp &7without", "&7them being actively deployed!", "", "&c&lDISABLED",
                "&8Enabled by a Mayor Diana perk."), ExpSharingMenu.slot(1, null, 0).lore());
        assertEquals("&cExp Share Slot #3", ExpSharingMenu.slot(2, BEE, 0).name(), "disabled with a pet too");
        Icon active = ExpSharingMenu.slot(0, BEE, 10);
        assertEquals("&aExp Share Slot #1", active.name());
        assertTrue(active.lore().contains("&7Exp Share rate: &a2%"));
        assertEquals("&a&lACTIVE", active.lore().getLast());
    }

    @Test
    void theirLore() {
        List<String> lore = PetText.lore(BEE, DATA, PetText.Where.EXP_SHARE, false);
        assertEquals("&eClick to remove!", lore.getLast());
        assertEquals("&eClick to choose!", PetText.lore(BEE, DATA, PetText.Where.CHOOSE, false).getLast());
        assertNull(lore.stream().filter(line -> line.equals(PetText.KAT)).findFirst().orElse(null));
    }
}
