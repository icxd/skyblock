package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.collection.PrivateData;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** Every Pet Item in the private items data does what its lines say, and the Tier Boost's rarity (skipped without it). */
class HeldItemsDataTest {
    private static PetData data;

    @BeforeAll
    static void load() {
        assumeTrue(Files.isRegularFile(PrivateData.itemsFile()), "no private items.json");
        assumeTrue(Files.isRegularFile(PetFixtures.dataFolder().resolve("pets/pets.json")), "no private pets.json");
        assertNull(ItemRegistry.loadData(PrivateData.itemsFile()).failure(), "the items didn't load");
        data = PetData.load(PetFixtures.dataFolder());
    }

    @Test
    void everyOneDoesSomething() {
        // Those given to pets that do nothing here: trophy fishing, the Garden's pests, Deployables, Titanium.
        List<String> nothing = new ArrayList<>();
        Map<String, SkyBlockItem> items = new TreeMap<>(ItemRegistry.getRegistry());
        for (Map.Entry<String, SkyBlockItem> e : items.entrySet()) {
            if (e.getValue().specificItemType() != SpecificItemType.PET_ITEM) continue;
            HeldItems.Effect effect = HeldItems.effect(e.getKey());
            if (!effect.appliable()) continue;
            boolean does = any(effect.grants()) || !effect.shares().isEmpty() || effect.allShare() > 0 || effect.everyLevels() > 0
                    || effect.onKillMillis() > 0 || effect.comboAt() > 0 || effect.speedCap() > 0 || effect.noKnockback() || effect.tiers() > 0
                    || effect.expShare() > 0 || HeldItems.expBoost(e.getKey(), Skill.COMBAT) > 0 || HeldItems.expBoost(e.getKey(), Skill.MINING) > 0
                    || HeldItems.expBoost(e.getKey(), Skill.FARMING) > 0 || HeldItems.expBoost(e.getKey(), Skill.FISHING) > 0
                    || HeldItems.expBoost(e.getKey(), Skill.FORAGING) > 0;
            if (!does) nothing.add(e.getKey());
        }
        assertEquals(List.of("BARREL_OF_RICHES", "BROWN_BANDANA", "LOTUS_CROWN", "PET_ITEM_BUBBLEGUM", "PET_ITEM_TITANIUM_MINECART"), nothing);
    }

    private static boolean any(Stats stats) {
        for (Stat stat : Stat.values()) if (stats.has(stat)) return true;
        return false;
    }

    @Test
    void theirNumbers() {
        assertEquals(35, HeldItems.stats("CROCHET_TIGER_PLUSHIE").get(Stat.ATTACK_SPEED), 1e-9);
        assertEquals(20, HeldItems.stats("PET_ITEM_SPOOKY_CUPCAKE").get(Stat.SPEED), 1e-9);
        assertEquals(50, HeldItems.effect("HEPHAESTUS_RELIC").allShare(), 1e-9);
        assertTrue(HeldItems.effect("HEPHAESTUS_SHELMET").noKnockback());
        assertEquals("Pig", HeldItems.effect("PET_ITEM_FLYING_PIG").onlyFor());
        assertTrue(HeldItems.compatible("PET_ITEM_FLYING_PIG", "PIG", "Pig"));
        assertTrue(!HeldItems.compatible("PET_ITEM_FLYING_PIG", "LION", "Lion"));
        assertTrue(!HeldItems.compatible("UNALLOYED_SPEED", "SNAIL", "Snail"), "the wiki's Unalloyed Speed");
        assertTrue(!HeldItems.compatible("PET_ITEM_EXP_SHARE", "KUUDRA", "Kuudra"), "the wiki's Kuudra Pet");
    }

    @Test
    void tierBoost() {
        OwnedPet lion = OwnedPet.of("LION", Rarity.EPIC).withExp(data.totalXp(data.pet("LION"), Rarity.EPIC, 100));
        assertEquals(100, Pets.level(lion, data));
        OwnedPet boosted = lion.withHeldItem("PET_ITEM_TIER_BOOST");
        assertEquals(Rarity.LEGENDARY, Pets.tier(boosted, data));
        assertTrue(Pets.level(boosted, data) < 100, "on the Legendary table");
        assertEquals(Rarity.EPIC, boosted.tier(), "its own stays");
        assertEquals(Rarity.LEGENDARY, Pets.tier(OwnedPet.of("LION", Rarity.LEGENDARY).withHeldItem("PET_ITEM_TIER_BOOST"), data),
                "not to Mythic");
        assertEquals(Rarity.EPIC, Pets.tier(OwnedPet.of("GRIFFIN", Rarity.EPIC).withHeldItem("PET_ITEM_TIER_BOOST"), data), "the Griffin's ignores it");
    }
}
