package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;

/** Fann's training as the wiki's Fann has it (and its pet calculator's Free XP), and what SkyHanni reads of her menus. */
class FannTest {
    private static final PetData DATA = PetFixtures.fixture();
    private static final OwnedPet BEE = OwnedPet.of("BEE", Rarity.COMMON);

    @Test
    void freeTraining() {
        assertEquals(20_000, Fann.freeXp(10), 1e-9);
        assertEquals(20_000, Fann.freeXp(5), 1e-9);
        assertEquals(39_500, Fann.freeXp(49), 1e-9);
        assertEquals(40_000, Fann.freeXp(50), 1e-9);
        assertEquals(50_000, Fann.freeXp(60), 1e-9);
        assertEquals(List.of("&7Fann's Free Training session gets", "&7increasingly better the higher the", "&aTaming Level &7of her customers.", "",
                "&7Taming Level: &a10 &8(+0 EXP)", "&7Free Training: &b20,000 EXP/day"), PetTrainingMenu.freeTraining(10).lore());
    }

    @Test
    void whatSessionsTakeAndGive() {
        // Light at Taming 20 with an Owl's +10% and 2% off, for three days.
        Fann.Plan light = Fann.forDays(Fann.Training.LIGHT, 20, 10, 2, 3);
        assertEquals(825_000, light.xp(), 1e-6);
        assertEquals(588_000, light.coins(), 1e-6);
        assertEquals(0, light.bits(), 1e-9);
        // Ultra until 750,000 more: half a day.
        Fann.Plan ultra = Fann.forXp(Fann.Training.ULTRA, 50, 0, 0, 750_000);
        assertEquals(0.5, ultra.days(), 1e-9);
        assertEquals(1_375_000, ultra.coins(), 1e-6);
        assertEquals(1_000, Fann.forDays(Fann.Training.TURBO, 60, 0, 0, 2).bits(), 1e-9);
        assertEquals(0, Fann.forDays(Fann.Training.FREE, 10, 0, 0, 5).coins(), 1e-9);
        assertEquals(List.of(Fann.Training.FREE, Fann.Training.LIGHT, Fann.Training.MODERATE), Fann.allowed(35));
    }

    @Test
    void theOwlsRefunds() {
        assertEquals(0, Fann.refund(999_999), 1e-9);
        assertEquals(1, Fann.refund(1_000_000), 1e-9);
        assertEquals(5, Fann.refund(7_500_000), 1e-9);
        assertEquals(0, Fann.owlBonus(null, DATA), 1e-9);
        assertEquals(0, Fann.owlBonus(BEE, DATA), 1e-9, "not an Owl");
    }

    @Test
    void sessions() {
        Fann.Session session = new Fann.Session(BEE, null, Fann.Training.LIGHT, 1_000, 3_000, 500, false);
        assertEquals(250, session.xpBy(2_000), 1e-9);
        assertEquals(500, session.xpBy(9_000), 1e-9);
        assertFalse(session.done(2_999));
        assertTrue(session.done(3_000));
        Document profile = new Document();
        OwnedPet owl = OwnedPet.of("OWL", Rarity.LEGENDARY);
        Fann.put(profile, Arrays.asList(new Fann.Session(BEE, owl, Fann.Training.TURBO, 1, 2, 3.5, true), null, null));
        List<Fann.Session> back = Fann.sessions(profile);
        assertEquals(new Fann.Session(BEE, owl, Fann.Training.TURBO, 1, 2, 3.5, true), back.getFirst());
        assertNull(back.get(1));
        assertEquals(3, back.size());
    }

    @Test
    void herWords() {
        assertEquals("0d 0h 10m 29s", Fann.duration(629_000));
        assertEquals("44d 6h 14m 40s", Fann.duration(((44L * 24 + 6) * 60 + 14) * 60_000 + 40_000));
        assertEquals("22,795.5", Fann.decimal(22_795.54));
        assertEquals("&e[#1] &fLight &fBee &fhas concluded and is now &7[Lvl 1]",
                Fann.concluded(0, new Fann.Session(BEE, null, Fann.Training.LIGHT, 0, 1, 0, false), DATA));
    }

    @Test
    void herMenus() {
        assertEquals("&b▶ Amount of Days", TrainingSlotMenu.mode(false).lore().getFirst());
        assertEquals("&b▶ Until Specific Level", TrainingSlotMenu.mode(true).lore().get(1));
        assertEquals("&b▶ 5 Days", TrainingSlotMenu.amount(false, 5, 0).lore().getFirst());
        assertEquals("&b▶ 1 Day", TrainingSlotMenu.amount(false, 1, 0).lore().getFirst());
        List<String> types = TrainingSlotMenu.type(Fann.Training.LIGHT, 20).lore();
        assertEquals(List.of("&7Free", "&a▶ Light", "&cModerate &8(Taming 30)"), types.subList(0, 3));
        assertEquals(2, TrainingSlotMenu.stepped(1, ClickType.LEFT, 1, 100));
        assertEquals(1, TrainingSlotMenu.stepped(1, ClickType.RIGHT, 1, 100));
        assertEquals(15, TrainingSlotMenu.stepped(5, ClickType.SHIFT_LEFT, 1, 100));
        assertEquals(100, TrainingSlotMenu.stepped(95, ClickType.SHIFT_LEFT, 1, 100));
        assertEquals(List.of("&8Empty", "", "&7This slot is currently empty!", "&8You can level up pets by training", "&8them here.", "",
                "&eClick to start a session!"), PetTrainingMenu.EMPTY.lore());
    }
}
