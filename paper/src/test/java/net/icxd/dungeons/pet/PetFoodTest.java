package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;

/** Pet Food: what the items' lines (as the items data has them) say they grant, and who eats what. */
class PetFoodTest {
    private static final PetData DATA = PetFixtures.fixture();

    @Test
    void whatTheyGrant() {
        PetFood.Food simple = PetFood.of("SIMPLE_CARROT_CANDY", "&aSimple Carrot Candy", List.of("&8Consumable", "",
                "&aCarrot Candy &7gives pets boosts of &ePet EXP&7!", "&7Pets can only eat &a10 &7candy in their lifetime!", "",
                "&7Grants &a20,000 &ePet EXP&7.", "", "&eRight-click on your summoned pet to", "&efeed it!"));
        assertEquals(20_000, simple.xp(), 1e-9);
        assertTrue(simple.candy());
        PetFood.Food cake = PetFood.of("PET_CAKE", "&fPet Cake", List.of("&7Feeding this to your pets gives them", "&7a boost of &a10,000 Exp&7!", "",
                "&eRight-click on your summoned pet to", "&efeed it!"));
        assertEquals(10_000, cake.xp(), 1e-9);
        assertFalse(cake.candy());
        PetFood.Food fish = PetFood.of("FISH_FOOD", "&fFish Food", List.of("&7Despite its name, can be given to any",
                "&7pet! Feeding this to your pet will not", "&7increase its candy count.", "", "&7Grants &a2,000 &ePet EXP&7."));
        assertEquals(2_000, fish.xp(), 1e-9);
        assertFalse(fish.candy());
        PetFood.Food crude = PetFood.of("CRUDE_GABAGOOL", "&aCrude Gabagool", List.of("&7Created by Inferno minions, use to"));
        assertEquals(100, crude.xp(), 1e-9);
        assertTrue(crude.gabagool());
        assertNull(PetFood.of("ENCHANTED_CARROT", "&aEnchanted Carrot", List.of("&7A carrot.")));
    }

    @Test
    void whoEatsWhat() {
        PetFood.Food candy = new PetFood.Food("SIMPLE_CARROT_CANDY", "Simple", 20_000, true, false);
        PetFood.Food gabagool = new PetFood.Food("CRUDE_GABAGOOL", "Crude", 100, false, true);
        OwnedPet bee = OwnedPet.of("BEE", Rarity.COMMON);
        assertNull(PetFood.refusal(bee, candy, DATA));
        assertEquals(PetFood.TOO_MUCH_CANDY, PetFood.refusal(bee.withCandyUsed(10), candy, DATA));
        assertEquals(PetFood.WISPS_ONLY, PetFood.refusal(bee, gabagool, DATA));
        assertNull(PetFood.refusal(OwnedPet.of("DROPLET_WISP", Rarity.UNCOMMON), gabagool, DATA));
        OwnedPet maxed = bee.withExp(DATA.totalXp(DATA.pet("BEE"), Rarity.COMMON, 5));
        assertEquals(PetFood.MAXED, PetFood.refusal(maxed, candy, DATA));
        // Gabagool's boosted by Taming; candy isn't.
        assertEquals(110, PetFood.xp(gabagool, bee, 10), 1e-9);
        assertEquals(20_000, PetFood.xp(candy, bee, 10), 1e-9);
    }

    @Test
    void theConfirmation() {
        PetFood.Food candy = new PetFood.Food("GREAT_CARROT_CANDY", "&9Great Carrot Candy", 200_000, true, false);
        assertEquals(List.of("&7Feed: &9Great Carrot Candy", "&7To: &fBee", "&7Grants: &e200,000 Pet EXP", "&7Candy used: &a4&7/&a10"),
                FeedPetMenu.confirm(OwnedPet.of("BEE", Rarity.COMMON).withCandyUsed(3), DATA, candy, 200_000).lore());
    }
}
