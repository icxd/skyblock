package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.collection.PrivateData;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.enums.Rarity;

/** Kat on the private data (skipped without it): Hire Kat as recorded, and the pets she won't upgrade (the wiki's). */
class KatDataTest {
    private static PetData data;

    @BeforeAll
    static void load() {
        assumeTrue(Files.isRegularFile(PrivateData.itemsFile()), "no private items.json");
        assumeTrue(Files.isRegularFile(PetFixtures.dataFolder().resolve("pets/pets.json")), "no private pets.json");
        assertNull(ItemRegistry.loadData(PrivateData.itemsFile()).failure(), "the items didn't load");
        data = PetData.load(PetFixtures.dataFolder());
    }

    @Test
    void hireKatAsRecorded() {
        // 2026-10-25, 00:59.3: a level 1 Common Bee.
        assertEquals(List.of("&7Kat will take care of your &fBee &7for &91", "&9minute&7 then its &9rarity&7 will be", "&7upgraded!", "",
                "&7Cost", "&aSmall Honey Dipper", "", "&eClick to hire Kat!"),
                Kat.hire(Kat.offer(OwnedPet.of("BEE", Rarity.COMMON), data), data, null).lore());
    }

    @Test
    void whomSheWont() {
        for (String type : List.of("GRANDMA_WOLF", "KUUDRA", "BINGO", "DROPLET_WISP", "FROST_WISP", "GLACIAL_WISP", "SUBZERO_WISP", "GOLDEN_DRAGON")) {
            for (Rarity rarity : data.pet(type).variants().keySet()) assertNull(Kat.offer(OwnedPet.of(type, rarity), data), type + " " + rarity);
        }
        assertNull(Kat.offer(OwnedPet.of("BEE", Rarity.MYTHIC), data), "the top rarity");
        assertNotNull(Kat.offer(OwnedPet.of("BEE", Rarity.LEGENDARY), data), "to Mythic");
        assertEquals(25, Kat.offer(OwnedPet.of("BEE", Rarity.LEGENDARY), data).taming());
    }
}
