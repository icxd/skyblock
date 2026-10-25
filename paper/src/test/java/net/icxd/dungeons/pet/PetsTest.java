package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** A profile's pets: what's kept, the menu's order and settings, the Pet Score, the stats and the XP a skill's gives. */
class PetsTest {
    private static final PetData DATA = PetFixtures.fixture();

    @BeforeEach
    void data() {
        Pets.data(DATA);
    }

    @AfterEach
    void none() {
        Pets.data(PetData.empty());
    }

    private static OwnedPet pet(String type, Rarity tier, double exp) {
        return OwnedPet.of(type, tier).withExp(exp);
    }

    @Test
    void kept() {
        Document profile = new Document();
        OwnedPet bee = pet("BEE", Rarity.COMMON, 150);
        assertTrue(Pets.add(profile, bee));
        assertEquals(List.of(bee), Pets.pets(profile));
        assertNull(Pets.active(profile));
        Pets.activate(profile, bee.uuid());
        assertEquals(bee, Pets.active(profile));
        OwnedPet more = bee.withExp(500);
        Pets.replace(profile, more);
        assertEquals(500, Pets.active(profile).exp());
        assertEquals(more, Pets.remove(profile, bee.uuid()));
        assertNull(Pets.active(profile), "out of the menu, it isn't out");
        assertNull(Pets.remove(profile, bee.uuid()));
    }

    @Test
    void full() {
        Document profile = new Document();
        List<OwnedPet> pets = new ArrayList<>();
        for (int i = 0; i < Pets.SLOTS; i++) pets.add(pet("BEE", Rarity.COMMON, i));
        Pets.set(profile, pets);
        assertFalse(Pets.add(profile, pet("BEE", Rarity.COMMON, 0)), "112, as recorded");
        Pets.upgrade(profile);
        assertEquals(Pets.SLOTS + 28, Pets.slots(profile));
        assertTrue(Pets.add(profile, pet("BEE", Rarity.COMMON, 0)));
        Pets.upgrade(profile);
        Pets.upgrade(profile);
        assertEquals(Pets.SLOTS + 56, Pets.slots(profile), "twice at most");
    }

    @Test
    void petInfo() {
        OwnedPet bee = new OwnedPet("abc", "BEE", Rarity.UNCOMMON, 12.5, "PET_ITEM_ALL_SKILLS_BOOST_COMMON", 2, "BEE_SKIN", true);
        OwnedPet back = OwnedPet.fromPetInfo(bee.petInfo(), null);
        assertEquals(bee.withFavorite(false), back, "an item isn't a favourite");
        assertEquals(bee, OwnedPet.from(bee.document()));
        // Hypixel's own (an item's ExtraAttributes, the API's pets_data): its uniqueId when there's no uuid.
        OwnedPet hypixel = OwnedPet.fromPetInfo("""
                {"type":"SPIDER","active":false,"exp":1939163.47,"tier":"MYTHIC","hideInfo":false,"heldItem":"DWARF_TURTLE_SHELMET",
                 "candyUsed":1,"uniqueId":"6d8f","hideRightClick":false,"noMove":false}""", "item-uuid");
        assertEquals(new OwnedPet("6d8f", "SPIDER", Rarity.MYTHIC, 1939163.47, "DWARF_TURTLE_SHELMET", 1, null, false), hypixel);
        assertNull(OwnedPet.fromPetInfo("{\"type\":\"BEE\"}", null), "no rarity");
        assertNull(OwnedPet.fromPetInfo("not json", null));
    }

    @Test
    void sorted() {
        OwnedPet bee = pet("BEE", Rarity.COMMON, 0);
        OwnedPet richBee = pet("BEE", Rarity.COMMON, 300);
        OwnedPet guardian = pet("GUARDIAN", Rarity.MYTHIC, 10);
        OwnedPet dragon = pet("GOLDEN_DRAGON", Rarity.LEGENDARY, 0).withFavorite(true);
        List<OwnedPet> pets = List.of(bee, richBee, guardian, dragon);
        // Favourites first; by rarity the best first, then the most XP (recorded).
        assertEquals(List.of(dragon, guardian, richBee, bee), Pets.sorted(pets, Pets.Sort.RARITY, DATA));
        assertEquals(List.of(dragon, richBee, bee, guardian), Pets.sorted(pets, Pets.Sort.A_TO_Z, DATA));
        assertEquals(List.of(dragon, guardian, richBee, bee), Pets.sorted(pets, Pets.Sort.Z_TO_A, DATA));
        assertEquals(List.of(dragon, richBee, guardian, bee), Pets.sorted(pets, Pets.Sort.PET_EXP, DATA));
        assertEquals(Pets.Sort.A_TO_Z, Pets.Sort.RARITY.step(false));
        assertEquals(Pets.Sort.SKILL, Pets.Sort.RARITY.step(true), "a right click goes back, round");
    }

    @Test
    void settings() {
        Document profile = new Document();
        assertEquals(Pets.Visibility.SHOW_ALL, Pets.visibility(profile));
        assertTrue(Pets.nametags(profile));
        // Recorded: Hide all, then Only your pet, then Show all.
        assertEquals(Pets.Visibility.ONLY_YOURS, Pets.Visibility.HIDE_ALL.next());
        assertEquals(Pets.Visibility.SHOW_ALL, Pets.Visibility.ONLY_YOURS.next());
        Pets.visibility(profile, Pets.Visibility.HIDE_ALL);
        Pets.nametags(profile, false);
        Pets.sort(profile, Pets.Sort.SKILL);
        assertEquals(List.of(Pets.Visibility.HIDE_ALL, false, Pets.Sort.SKILL), List.of(Pets.visibility(profile), Pets.nametags(profile), Pets.sort(profile)));
    }

    @Test
    void score() {
        Document profile = new Document();
        Pets.add(profile, pet("BEE", Rarity.COMMON, 0));
        assertEquals(1, Pets.score(profile, DATA));
        // The same type counts once, its best rarity; a maxed one one more.
        Pets.add(profile, pet("BEE", Rarity.UNCOMMON, 0));
        assertEquals(2, Pets.score(profile, DATA));
        Pets.add(profile, pet("GUARDIAN", Rarity.MYTHIC, 1_000_000));
        assertEquals(2 + 6 + 1, Pets.score(profile, DATA));
        assertTrue(Pets.raiseBest(profile, DATA));
        assertEquals(9, Pets.bestScore(profile));
        // Removing one lowers the score, not the best (the SkyBlock XP stays).
        Pets.set(profile, List.of(pet("BEE", Rarity.COMMON, 0)));
        assertEquals(1, Pets.score(profile, DATA));
        assertFalse(Pets.raiseBest(profile, DATA));
        assertEquals(9, Pets.bestScore(profile));
        assertEquals(List.of(0, 1, 1, 2, 3), List.of(Pets.magicFind(0, DATA), Pets.magicFind(1, DATA), Pets.magicFind(4, DATA),
                Pets.magicFind(5, DATA), Pets.magicFind(500, DATA)));
    }

    @Test
    void stats() {
        Document profile = new Document();
        OwnedPet bee = pet("BEE", Rarity.COMMON, 100 + 110);
        Pets.add(profile, bee);
        assertEquals(new Stats().add(Stat.MAGIC_FIND, 1), Pets.stats(profile), "the Pet Score's, with none out");
        Pets.activate(profile, bee.uuid());
        // Level 3: 5 + 0.25 x 3 Strength, 0.5 x 3 Intelligence.
        assertEquals(new Stats().add(Stat.STRENGTH, 5.75).add(Stat.INTELLIGENCE, 1.5).add(Stat.MAGIC_FIND, 1), Pets.stats(profile));
        // The dragon's start at level 6 (its egg's none).
        PetData data = DATA;
        Pet dragon = data.pet("GOLDEN_DRAGON");
        OwnedPet egg = pet("GOLDEN_DRAGON", Rarity.LEGENDARY, data.totalXp(dragon, Rarity.LEGENDARY, 5));
        assertEquals(new Stats(), Pets.stats(egg, data));
        assertEquals(new Stats().add(Stat.STRENGTH, 1.5), Pets.stats(egg.withExp(data.totalXp(dragon, Rarity.LEGENDARY, 6)), data));
    }

    @Test
    void xp() {
        Pet bee = DATA.pet("BEE");
        // Its own skill's, all of it; another's, a third; Alchemy's or Enchanting's, a twelfth; none from Carpentry or Taming.
        assertEquals(4, PetXp.share(bee, Skill.FARMING, 4), 1e-9);
        assertEquals(1, PetXp.share(bee, Skill.COMBAT, 3), 1e-9);
        assertEquals(1, PetXp.share(bee, Skill.ENCHANTING, 12), 1e-9);
        assertEquals(1.5 / 3, PetXp.share(bee, Skill.FISHING, 1), 1e-9, "Fishing's is 1.5x, a third of it");
        assertEquals(0, PetXp.share(bee, Skill.CARPENTRY, 10));
        assertEquals(0, PetXp.share(bee, Skill.TAMING, 10));
        assertEquals(12, PetXp.share(DATA.pet("GUARDIAN"), Skill.ENCHANTING, 12), 1e-9, "its own");
        assertEquals(10, PetXp.share(DATA.pet("BINGO"), Skill.MINING, 10), 1e-9, "All Skills");
        assertEquals(0, PetXp.share(DATA.pet("DROPLET_WISP"), Skill.FARMING, 10), "fed, not levelled");
        // The recorded Bee: a wheat's 4 Farming XP (4.8 with Wisdom) gave it 6.4, at Taming 60.
        assertEquals(6.4, PetXp.petXp(bee, Skill.FARMING, 4, 60, 0), 1e-9);
        assertEquals(6.4 * 1.5, PetXp.petXp(bee, Skill.FARMING, 4, 60, 0.5), 1e-9, "a +50% Farming Exp Boost");
        assertEquals(2 * 1.5 * 2, PetXp.petXp(DATA.pet("REINDEER"), Skill.FISHING, 2, 0, 0), 1e-9, "the Reindeer's 2x");
        assertEquals(1.2, PetXp.tamingXp(bee, Skill.FARMING, 4), 1e-9, "Taming: 30% of the pet's share");
        assertEquals("&aYour &fBee &aleveled up to level &92&a!", PetXp.levelUp(pet("BEE", Rarity.COMMON, 100), DATA, 2));
    }
}
