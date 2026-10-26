package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;

/** George as recorded (2026-10-25: a level 4 Uncommon Bee, sold for 250 coins), on the made-up data. */
class GeorgeTest {
    private static final PetData DATA = PetFixtures.fixture();

    private static OwnedPet bee(Rarity rarity, int level) {
        return OwnedPet.of("BEE", rarity).withExp(DATA.totalXp(DATA.pet("BEE"), rarity, level));
    }

    @Test
    void hisPrices() {
        assertEquals(250, George.price(bee(Rarity.UNCOMMON, 4), DATA));
        assertEquals(0, George.price(bee(Rarity.COMMON, 1), DATA), "none in this data");
        assertEquals(0, George.price(OwnedPet.of("NOT_A_PET", Rarity.COMMON), DATA));
    }

    @Test
    void theSale() {
        OwnedPet bee = bee(Rarity.UNCOMMON, 4);
        assertEquals(List.of("&7George is willing to make an offer on", "&7your pet!", "", "&9Offer:", "&6250 Coins", "",
                "&cWARNING: This will permanently", "&cremove your pet.", "", "&eClick to accept offer!"), George.accept(250).lore());
        Icon confirm = George.confirmSale(bee, DATA, 250);
        assertEquals("&aConfirm", confirm.name());
        assertEquals(List.of("&7Giving: &aBee", "&7Receiving: &6250 Coins"), confirm.lore());
        assertEquals("&aYou sold your &7[Lvl 4] &aBee to &eGeorge &afor &6250 Coins&a!", George.sold(bee, DATA, 250));
    }

    @Test
    void hisDailyLimit() {
        Document profile = new Document();
        long day = 1_761_436_800_000L;
        George.recordSale(profile, 30_000_000, day);
        George.recordSale(profile, 15_000_000, day + 3_600_000);
        assertEquals(45_000_000, George.soldToday(profile, day + 7_200_000));
        assertEquals(0, George.soldToday(profile, day + 86_400_000), "a new day");
    }

    @Test
    void theCapItemAsRecorded() {
        assertEquals(List.of("&7Raise your Taming Skill level cap by", "&7one, up to &f+10&7.", "", "&7Your cap: &dTaming L", "",
                "&7Pets left to donate:", "  &dMythic Enderman", "  &6Legendary Black Cat", "  &5Epic Rift Ferret", "  &5Epic Slug",
                "  &5Epic Spirit", "  &5Epic Giraffe", "  &5Epic Jellyfish", "  &5Epic Bal", "  &5Epic Baby Yeti", "  &9Rare Frost Wisp", "",
                "&8+&b10 SkyBlock XP", "", "&7Donate any of the pets above to", "&7increase your cap by one."),
                George.cap(new Document(), DATA, false).lore());
    }

    @Test
    void whatHeCollects() {
        Document profile = new Document();
        assertEquals("SLUG", George.wanted(profile, OwnedPet.of("SLUG", Rarity.EPIC)).type());
        assertEquals("SLUG", George.wanted(profile, OwnedPet.of("SLUG", Rarity.LEGENDARY)).type(), "or better");
        assertNull(George.wanted(profile, OwnedPet.of("SLUG", Rarity.RARE)));
        assertNull(George.wanted(profile, OwnedPet.of("BEE", Rarity.MYTHIC)));
        George.yoinked(profile, List.of("slug", "SLUG", "FROST_WISP"));
        assertEquals(List.of("SLUG", "FROST_WISP"), George.sacrificed(profile));
        assertNull(George.wanted(profile, OwnedPet.of("SLUG", Rarity.EPIC)), "given already");
        List<String> lore = George.cap(profile, DATA, true).lore();
        assertEquals("&7Your cap: &dTaming LII", lore.get(3), "two given");
        assertEquals(8, lore.stream().filter(line -> line.startsWith("  &")).count());
        assertEquals("&eClick to donate your pet!", lore.getLast());
    }

    @Test
    void theTamingCap() {
        George.enable();
        George.enable();
        Document profile = new Document();
        assertEquals(50, Skills.cap(profile, Skill.TAMING));
        George.yoinked(profile, List.of("ENDERMAN", "BLACK_CAT", "SLUG"));
        assertEquals(53, Skills.cap(profile, Skill.TAMING), "once each, however often it's enabled");
        assertEquals(60, Skills.cap(profile, Skill.COMBAT));
        Skills.add(profile, Skill.TAMING, Skill.TAMING.cumulative(58));
        assertEquals(53, Skills.level(profile, Skill.TAMING));
        George.yoinked(profile, George.WANTED.stream().map(George.Wanted::type).toList());
        assertEquals(58, Skills.level(profile, Skill.TAMING), "its XP past the cap was kept");
    }
}
