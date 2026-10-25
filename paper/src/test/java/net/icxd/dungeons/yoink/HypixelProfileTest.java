package net.icxd.dungeons.yoink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A {@code skyblock/profiles} answer in API v2's layout (the paths NEU's profile viewer reads): the test fixture's
 * three profiles, one of them left.
 */
class HypixelProfileTest {
    static final String PLAYER = "b876ec32e396476ba1158438d83c67d4";

    static JsonObject answer() throws IOException {
        try (InputStream in = HypixelProfileTest.class.getResourceAsStream("/yoink/profiles.json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    static HypixelProfile banana() throws IOException {
        return HypixelProfile.of(answer(), PLAYER).getFirst();
    }

    @Test
    void theirProfilesThePlayedOneFirst() throws IOException {
        List<HypixelProfile> profiles = HypixelProfile.of(answer(), PLAYER);
        assertEquals(List.of("Banana", "Strawberry"), profiles.stream().map(HypixelProfile::name).toList(), "Kiwi was left");
        HypixelProfile banana = profiles.getFirst();
        assertTrue(banana.selected());
        assertEquals("Ironman", banana.modeName());
        assertEquals(2, banana.members());
        assertEquals(123456.7, banana.purse());
        assertEquals(1000000000.5, banana.bank());
        assertEquals(250, banana.level());
        HypixelProfile strawberry = profiles.get(1);
        assertNull(strawberry.modeName());
        assertNull(strawberry.bank(), "its banking API is off");
        // Dashes or none, either case.
        assertEquals(2, HypixelProfile.of(answer(), "B876EC32-E396-476B-A115-8438D83C67D4").size());
        assertTrue(HypixelProfile.of(new JsonObject(), PLAYER).isEmpty());
    }

    @Test
    void whatTheApiHas() throws IOException {
        HypixelProfile banana = banana();
        assertTrue(YoinkPart.INVENTORY.available(banana));
        assertTrue(YoinkPart.MINING.available(banana));
        assertTrue(YoinkPart.CRIMSON_ISLE.available(banana));
        assertTrue(YoinkPart.FAIRY_SOULS.available(banana));
        HypixelProfile strawberry = HypixelProfile.of(answer(), PLAYER).get(1);
        assertFalse(YoinkPart.INVENTORY.available(strawberry), "its inventory API is off");
        assertTrue(YoinkPart.COINS.available(strawberry));
        assertFalse(YoinkPart.CRIMSON_ISLE.available(strawberry), "it's never been to the Crimson Isle");
        assertEquals(55172425.5, banana.at("player_data.experience.SKILL_COMBAT").getAsDouble());
        assertNull(banana.at("player_data.nothing.here"));
    }
}
