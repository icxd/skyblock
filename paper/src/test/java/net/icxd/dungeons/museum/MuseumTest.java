package net.icxd.dungeons.museum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.museum.Museum.Sort;
import net.icxd.dungeons.museum.MuseumData.Entry;
import net.icxd.dungeons.museum.MuseumDonations.Outcome;
import net.icxd.dungeons.museum.MuseumDonations.Plan;

/** What a profile has donated, what counts, its SkyBlock XP, and what a click in the inventory would donate. */
class MuseumTest {
    private static final MuseumData DATA = MuseumFixtures.fixture();
    private static final List<Binary> ITEMS = List.of(new Binary(new byte[]{1}));

    private static void donate(Document profile, String entry) {
        Museum.donate(profile, entry, 1_792_888_296_000L, ITEMS);
    }

    @Test
    void donatedRetrievedAndStillCounting() {
        Document profile = new Document();
        Entry sword = DATA.entry("UNDEAD_SWORD");
        assertFalse(Museum.counts(profile, DATA, sword));
        assertNull(Museum.donation(profile, "UNDEAD_SWORD"));
        donate(profile, "UNDEAD_SWORD");
        Museum.Donation donation = Museum.donation(profile, "UNDEAD_SWORD");
        assertEquals(1_792_888_296_000L, donation.at());
        assertFalse(donation.retrieved());
        assertEquals(ITEMS, donation.items());
        assertEquals(1, Museum.xp(profile, DATA));
        // "It still counts towards your Museum progress".
        Museum.retrieved(profile, "UNDEAD_SWORD");
        assertTrue(Museum.donation(profile, "UNDEAD_SWORD").retrieved());
        assertTrue(Museum.counts(profile, DATA, sword));
        assertEquals(1, Museum.xp(profile, DATA));
        assertEquals(1, Museum.counted(profile, DATA, DATA.category("COMBAT")));
        assertEquals(0, Museum.counted(profile, DATA, DATA.category("MINING")));
    }

    /** "Donating a Yellow Belt will count as donating this item.", up the tiers. */
    @Test
    void higherTiersCountForLowerOnes() {
        Document profile = new Document();
        donate(profile, "DOJO_GREEN_BELT");
        assertTrue(Museum.credited(profile, DATA, DATA.entry("DOJO_YELLOW_BELT")));
        assertTrue(Museum.credited(profile, DATA, DATA.entry("DOJO_WHITE_BELT")));
        assertFalse(Museum.credited(profile, DATA, DATA.entry("DOJO_GREEN_BELT")), "donated, not credited");
        assertEquals(3 + 2 + 1, Museum.xp(profile, DATA));
        assertEquals(3, Museum.counted(profile, DATA));
        donate(profile, "REVENANT_SWORD");
        assertEquals(6 + 4 + 1, Museum.xp(profile, DATA));
    }

    @Test
    void milestones() {
        assertEquals(List.of(0, 0, 1, 36, 40), List.of(Museum.milestone(0), Museum.milestone(99), Museum.milestone(100), Museum.milestone(3646),
                Museum.milestone(5000)));
    }

    @Test
    void settings() {
        Document profile = new Document();
        assertEquals(Sort.ASCENDING_XP, Museum.sort(profile));
        assertTrue(Museum.details(profile), "Extra Details on, as recorded");
        Museum.sort(profile, Sort.A_TO_Z);
        Museum.details(profile, false);
        assertEquals(Sort.A_TO_Z, Museum.sort(profile));
        assertFalse(Museum.details(profile));
        assertEquals(Sort.DESCENDING_XP, Sort.ASCENDING_XP.step(false));
        assertEquals(Sort.Z_TO_A, Sort.ASCENDING_XP.step(true), "a right click goes back, round");
        assertEquals(Sort.ASCENDING_XP, Sort.Z_TO_A.step(false));
    }

    // What a click donates

    private static String[] inventory(Object... slotAndId) {
        String[] ids = new String[36];
        for (int i = 0; i < slotAndId.length; i += 2) ids[(Integer) slotAndId[i]] = (String) slotAndId[i + 1];
        return ids;
    }

    @Test
    void anItem() {
        Outcome outcome = MuseumDonations.plan(inventory(3, "UNDEAD_SWORD"), 3, new Document(), DATA);
        assertEquals(new Plan(DATA.entry("UNDEAD_SWORD"), 3, List.of(3)), outcome.plan());
        assertEquals(MuseumDonations.CANT, MuseumDonations.plan(inventory(3, "DIRT"), 3, new Document(), DATA).refusal());
        assertEquals(MuseumDonations.CANT, MuseumDonations.plan(inventory(), 3, new Document(), DATA).refusal());
    }

    @Test
    void aSetTakesEveryPieceFromTheInventory() {
        String[] ids = inventory(0, "CELESTE_BOOTS", 5, "CELESTE_HELMET", 7, "CELESTE_CHESTPLATE", 9, "CELESTE_HELMET", 30, "CELESTE_LEGGINGS");
        // Its pieces' order (boots, chestplate, helmet, leggings): the clicked helmet, the first of each other.
        assertEquals(List.of(0, 7, 9, 30), MuseumDonations.plan(ids, 9, new Document(), DATA).plan().slots());
        assertEquals(List.of(0, 7, 5, 30), MuseumDonations.plan(ids, 5, new Document(), DATA).plan().slots());
        ids[30] = null;
        assertEquals(MuseumDonations.NOT_ALL, MuseumDonations.plan(ids, 5, new Document(), DATA).refusal());
    }

    /** Monster Raider takes Spider's or Tarantula Boots; Monster Hunter shares its pieces. */
    @Test
    void aPieceInTwoSets() {
        String[] ids = inventory(1, "SKELETON_HELMET", 2, "CREEPER_LEGGINGS", 3, "GUARDIAN_CHESTPLATE", 4, "TARANTULA_BOOTS");
        Document profile = new Document();
        assertEquals(new Plan(DATA.entry("MONSTER_RAIDER"), 1, List.of(2, 3, 1, 4)), MuseumDonations.plan(ids, 1, profile, DATA).plan());
        donate(profile, "MONSTER_RAIDER");
        // Monster Hunter's boots are Spider's only.
        assertEquals(MuseumDonations.NOT_ALL, MuseumDonations.plan(ids, 1, profile, DATA).refusal());
        ids[4] = "SPIDER_BOOTS";
        assertEquals(DATA.entry("MONSTER_HUNTER"), MuseumDonations.plan(ids, 1, profile, DATA).plan().entry());
    }

    @Test
    void alreadyDonatedOrRetrieved() {
        Document profile = new Document();
        String[] ids = inventory(0, "UNDEAD_SWORD");
        donate(profile, "UNDEAD_SWORD");
        assertEquals(MuseumDonations.ALREADY, MuseumDonations.plan(ids, 0, profile, DATA).refusal());
        // Retrieved, it can go back.
        Museum.retrieved(profile, "UNDEAD_SWORD");
        assertEquals(List.of(0), MuseumDonations.plan(ids, 0, profile, DATA).plan().slots());
        assertEquals(Arrays.asList(ids), Arrays.asList(inventory(0, "UNDEAD_SWORD")), "nothing changed");
    }

    /** As the owner found on Hypixel (2026-10-25): a lower tier still goes in after its higher tier, for no more XP. */
    @Test
    void aLowerTierAfterItsHigherTier() {
        Document profile = new Document();
        donate(profile, "DOJO_YELLOW_BELT");
        assertTrue(Museum.credited(profile, DATA, DATA.entry("DOJO_WHITE_BELT")));
        int xp = Museum.xp(profile, DATA);
        Plan plan = MuseumDonations.plan(inventory(1, "DOJO_WHITE_BELT"), 1, profile, DATA).plan();
        assertEquals(new Plan(DATA.entry("DOJO_WHITE_BELT"), 1, List.of(1)), plan);
        donate(profile, "DOJO_WHITE_BELT");
        assertFalse(Museum.credited(profile, DATA, DATA.entry("DOJO_WHITE_BELT")), "donated now");
        assertEquals(xp, Museum.xp(profile, DATA));
    }
}
