package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.ability.PetAbilities;
import net.icxd.dungeons.pet.ability.PetAbility;

/**
 * Every pet ability's text is as the private data's pets have it, at every rarity it's at (else it doesn't count there),
 * and their numbers there are the text's at the pet's level.
 */
class PetAbilitiesDataTest {
    private static PetData data;

    @BeforeAll
    static void load() {
        assumeTrue(Files.isRegularFile(PetFixtures.dataFolder().resolve("pets/pets.json")), "no private pets.json");
        data = PetData.load(PetFixtures.dataFolder());
    }

    @Test
    void textsAreThere() {
        assertEquals(List.of(), PetAbilities.problems(data));
    }

    /** A pet of this type and rarity at this level, and its abilities that count summoned, by name (the last of a name). */
    private static Map<String, PetAbility.Active> at(String type, Rarity tier, int level) {
        Pet kind = data.pet(type);
        OwnedPet pet = OwnedPet.of(type, tier).withExp(data.totalXp(kind, tier, level));
        assertEquals(level, Pets.level(pet, data));
        return PetAbilities.of(pet, data).stream().collect(Collectors.toMap(a -> a.ability().name(), a -> a, (x, y) -> y));
    }

    @Test
    void numbersAtTheirLevel() {
        Map<String, PetAbility.Active> dragon = at("ENDER_DRAGON", Rarity.LEGENDARY, 100);
        assertEquals(200, dragon.get("End Strike").n(0), 1e-9, "Deal 200% more damage to Ender mobs");
        assertEquals(10, dragon.get("Superior").n(0), 1e-9);
        assertEquals(50, dragon.get("One With The Dragon").n(0), 1e-9);
        assertEquals(30, dragon.get("One With The Dragon").n(1), 1e-9);
        Map<String, PetAbility.Active> tiger = at("TIGER", Rarity.EPIC, 37);
        assertEquals(0.3 * 37, tiger.get("Merciless Swipe").n(0), 1e-9);
        // A number written in the text is the text's, at every level.
        assertEquals(15, at("TIGER", Rarity.LEGENDARY, 1).get("Apex Predator").n(1), 1e-9);
        Map<String, PetAbility.Active> golem = at("GOLEM", Rarity.LEGENDARY, 100);
        assertEquals(List.of(20.0, 20.0, 40.0, 40.0, 12.0, 60.0), List.of(golem.get("Last Stand").n(0), golem.get("Last Stand").n(1),
                golem.get("Last Stand").n(2), golem.get("Last Stand").n(3), golem.get("Last Stand").n(4), golem.get("Last Stand").n(5)));
    }

    @Test
    void killCombosTable() {
        // Its numbers are in the unnamed block after it: 5 Combo lasting 10s, +3% Magic Find, and so on.
        PetAbility.Active combo = at("GRANDMA_WOLF", Rarity.LEGENDARY, 100).get("Kill Combo");
        assertEquals(List.of(5.0, 10.0, 3.0, 10.0, 8.0, 10.0), List.of(combo.n(0), combo.n(1), combo.n(2), combo.n(3), combo.n(4), combo.n(5)));
        assertEquals(30, combo.n(15), 1e-9);
        assertTrue(combo.ability().always(), "from the Pets menu too");
    }

    @Test
    void whichOfANameCounts() {
        // High Stride's Jump Boost goes by rarity: II, III, IV, each its own text.
        long horses = PetAbilities.of(OwnedPet.of("HORSE", Rarity.COMMON), data).stream().filter(a -> a.ability().name().equals("High Stride")).count();
        assertEquals(1, horses);
        // Golden Dragon's egg (below 100) has none.
        Pet golden = data.pet("GOLDEN_DRAGON");
        OwnedPet egg = OwnedPet.of("GOLDEN_DRAGON", Rarity.LEGENDARY).withExp(data.totalXp(golden, Rarity.LEGENDARY, 50));
        assertEquals(List.of(), PetAbilities.of(egg, data));
        assertEquals(250, at("GOLDEN_DRAGON", Rarity.LEGENDARY, 200).get("Legendary Treasure").n(1), 1e-9);
    }
}
