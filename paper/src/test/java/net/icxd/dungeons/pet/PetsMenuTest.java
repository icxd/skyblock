package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;

/** The Pets menu's Search: its titles (as SkyHanni's tests have Hypixel's) and what it finds. */
class PetsMenuTest {
    private static final PetData DATA = PetFixtures.fixture();

    @Test
    void titles() {
        assertEquals("Pets", PetsMenu.title(1, 1, null));
        assertEquals("(1/3) Pets", PetsMenu.title(1, 3, null));
        assertEquals("Pets: \"a\"", PetsMenu.title(1, 1, "a"));
        assertEquals("(6/6) Pets: \"e\"", PetsMenu.title(6, 6, "e"));
    }

    @Test
    void search() {
        OwnedPet bee = OwnedPet.of("BEE", Rarity.COMMON);
        OwnedPet guardian = OwnedPet.of("GUARDIAN", Rarity.MYTHIC);
        List<OwnedPet> pets = List.of(bee, guardian);
        assertEquals(pets, PetsMenu.found(pets, DATA, null));
        assertEquals(List.of(bee), PetsMenu.found(pets, DATA, "bee"));
        assertEquals(List.of(guardian), PetsMenu.found(pets, DATA, "ARD"), "any case");
        assertEquals(List.of(), PetsMenu.found(pets, DATA, "lion"));
    }
}
