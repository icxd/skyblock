package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.icxd.dungeons.item.enums.Rarity;

/** Pet skins: as pets.json has them, and the recorded skinned Enderman (2026-10-25, 00:21.3) on the private data. */
class PetSkinsTest {
    @Test
    void asTheDataHasThem() {
        JsonObject json = JsonParser.parseString(PetFixtures.DATA).getAsJsonObject();
        json.add("skins", JsonParser.parseString("""
                {"BEE_RGBEE": {"name": "RGBee Bee Skin", "short": "RGBee", "rarity": "MYTHIC", "color": "d", "texture": "rgb", "pets": ["BEE"]}}
                """));
        PetData data = PetData.parse(json);
        PetData.Skin skin = data.skin("bee_rgbee");
        assertEquals(new PetData.Skin("BEE_RGBEE", "RGBee Bee Skin", "RGBee", "MYTHIC", 'd', "rgb", List.of("BEE")), skin);
        OwnedPet bee = OwnedPet.of("BEE", Rarity.COMMON).withSkin("BEE_RGBEE");
        assertEquals("&fBee&d ✦", PetText.colored(bee, data));
        assertEquals("&8Farming Pet, RGBee Skin", PetText.lore(bee, data, PetText.Where.MENU, false).getFirst());
        assertEquals("rgb", Pets.texture(bee, data));
        // Another pet's skin, or one the data hasn't, shows nothing.
        OwnedPet guardian = OwnedPet.of("GUARDIAN", Rarity.MYTHIC).withSkin("BEE_RGBEE");
        assertNull(Pets.skin(guardian, data));
        assertEquals("&dGuardian", PetText.colored(guardian, data));
        assertEquals("bee0", Pets.texture(OwnedPet.of("BEE", Rarity.COMMON).withSkin("NOT_A_SKIN"), data));
    }

    @Test
    void theRecordedEnderman() {
        assumeTrue(Files.isRegularFile(PetFixtures.dataFolder().resolve("pets/pets.json")), "no private pets.json");
        PetData data = PetData.load(PetFixtures.dataFolder());
        assumeTrue(data.skin("ENDERMAN_SLAYER") != null, "a pets.json from before skins");
        OwnedPet enderman = OwnedPet.of("ENDERMAN", Rarity.MYTHIC).withExp(data.totalXp(data.pet("ENDERMAN"), Rarity.MYTHIC, 100))
                .withSkin("ENDERMAN_SLAYER");
        assertEquals("&7[Lvl 100] &dEnderman&5 ✦", PetText.name(enderman, data, PetText.Where.MENU));
        assertEquals("&8Combat Pet, Void Conqueror Skin", PetText.lore(enderman, data, PetText.Where.MENU, false).getFirst());
        assertEquals("8fff41e1afc597b14f77b8e44e2a134dabe161a1526ade80e6290f2df331dc11", Pets.texture(enderman, data));
    }
}
