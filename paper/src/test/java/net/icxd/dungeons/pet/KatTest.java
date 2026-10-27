package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.menu.Icon;

/** Kat as recorded (2026-10-25: a Common Bee, for a Small Honey Dipper and a minute), on the made-up data. */
class KatTest {
    private static final PetData DATA = PetFixtures.fixture();
    private static final OwnedPet BEE = OwnedPet.of("BEE", Rarity.COMMON);

    @Test
    void whatSheUpgrades() {
        Kat.Offer offer = Kat.offer(BEE, DATA);
        assertEquals(Rarity.UNCOMMON, offer.to());
        assertEquals(60, offer.seconds());
        assertEquals(List.of(new PetData.ItemCost("SMALL_HONEY_DIPPER", 1)), offer.items());
        assertEquals(1, offer.taming());
        assertNull(Kat.offer(OwnedPet.of("BEE", Rarity.UNCOMMON), DATA), "no Rare Bee in this data");
        assertNull(Kat.offer(OwnedPet.of("BINGO", Rarity.COMMON), DATA), "not one she upgrades");
        assertNull(Kat.offer(OwnedPet.of("GUARDIAN", Rarity.MYTHIC), DATA));
        assertNull(Kat.offer(OwnedPet.of("NOT_A_PET", Rarity.COMMON), DATA));
    }

    @Test
    void herDiscount() {
        // "base price x (1 - 0.003 x pet level)".
        assertEquals(700_000, Kat.cost(1_000_000, 100));
        assertEquals(997_000, Kat.cost(1_000_000, 1));
        assertEquals(0, Kat.cost(0, 50));
    }

    @Test
    void hireKat() {
        Icon hire = Kat.hire(Kat.offer(BEE, DATA), DATA, null);
        assertEquals(Material.GREEN_TERRACOTTA, hire.material());
        assertEquals("&aHire Kat", hire.name());
        List<String> lore = hire.lore();
        assertEquals(List.of("&7Kat will take care of your &fBee &7for &91", "&9minute&7 then its &9rarity&7 will be", "&7upgraded!", "", "&7Cost"),
                lore.subList(0, 5));
        assertEquals(List.of("", "&eClick to hire Kat!"), lore.subList(lore.size() - 2, lore.size()));
        Icon red = Kat.hire(Kat.offer(BEE, DATA), DATA, Kat.NOT_ENOUGH_ITEMS);
        assertEquals(Material.RED_TERRACOTTA, red.material());
        assertEquals("&cYou don't have the required items!", red.lore().getLast());
    }

    @Test
    void costLines() {
        Kat.Offer offer = new Kat.Offer(BEE, Rarity.UNCOMMON, 60, 250_000, List.of(new PetData.ItemCost("NO_SUCH_ITEM", 64)), 1);
        assertEquals(List.of("&6250,000 Coins", "&fNO_SUCH_ITEM &8x64"), Kat.costLines(offer));
    }

    @Test
    void confirm() {
        Icon confirm = Kat.confirm(Kat.offer(BEE, DATA));
        assertEquals("&aConfirm", confirm.name());
        assertEquals(List.of("&cWARNING: You will not be able to", "&cretrieve your pet for 1 minute and", "&cits level will change as a result of",
                "&cthe rarity upgrade."), confirm.lore());
    }

    @Test
    void whatSheSays() {
        assertEquals(List.of("&e[NPC] &bKat&f: &rI'll get your &fBee &fupgraded to &a&lUNCOMMON &fin no time!",
                "&e[NPC] &bKat&f: &rCome back in &e1 minute &fto pick it up!"), Kat.hired(Kat.offer(BEE, DATA), DATA));
        Kat.Sitting sitting = new Kat.Sitting(BEE, Rarity.UNCOMMON, 0, false);
        assertEquals(List.of("&e[NPC] &bKat&f: &rOh, hey!", "&e[NPC] &bKat&f: &rI was able to upgrade your pet &fBee&f to &a&lUNCOMMON&f.",
                "&e[NPC] &bKat&f: &rIf you have any other pets you'd like to upgrade, you know where to find me!"), Kat.pickedUp(sitting, DATA, false));
        assertEquals("&e[NPC] &bKat&f: &b✆ &fOh, hey!", Kat.pickedUp(sitting, DATA, true).getFirst());
        assertEquals(List.of("&e[NPC] &bKat&f: &rI'm currently taking care of your &fBee&f!",
                "&e[NPC] &bKat&f: &rYou can pick it up in &e45 seconds&f.", "&e[NPC] &bKat&f: &rStop by once it's done, okay?"),
                Kat.caring(sitting, DATA, 44_001));
    }

    @Test
    void durations() {
        assertEquals("1 minute", Kat.duration(60));
        assertEquals("45 seconds", Kat.duration(45));
        assertEquals("1 day", Kat.duration(86_400));
        assertEquals("2 days", Kat.duration(172_800));
        assertEquals("1 day 12 hours", Kat.duration(129_600));
        assertEquals("1 hour 1 minute", Kat.duration(3_661));
        assertEquals("0 seconds", Kat.duration(0));
    }

    @Test
    void keptWithTheProfile() {
        Document profile = new Document();
        assertNull(Kat.sitting(profile));
        OwnedPet bee = BEE.withExp(1234.5).withHeldItem("PET_ITEM_ALL_SKILLS_BOOST_COMMON");
        Kat.sit(profile, new Kat.Sitting(bee, Rarity.UNCOMMON, 1_000L, false));
        Kat.Sitting back = Kat.sitting(profile);
        assertEquals(bee, back.pet());
        assertEquals(Rarity.UNCOMMON, back.to());
        assertEquals(1_000L, back.ready());
        assertFalse(back.rang());
        Kat.sit(profile, new Kat.Sitting(bee, Rarity.UNCOMMON, 1_000L, true));
        assertTrue(Kat.sitting(profile).rang());
        Kat.clear(profile);
        assertNull(Kat.sitting(profile));
    }

    @Test
    void theUpgradeKeepsItsXp() {
        OwnedPet bee = BEE.withExp(500);
        OwnedPet upgraded = bee.withTier(Rarity.UNCOMMON);
        assertEquals(500, upgraded.exp(), 1e-9);
        assertEquals(bee.uuid(), upgraded.uuid());
        assertTrue(DATA.level(DATA.pet("BEE"), Rarity.UNCOMMON, 500) <= DATA.level(DATA.pet("BEE"), Rarity.COMMON, 500),
                "the same XP is a lower level, or the same, a rarity up");
    }
}
