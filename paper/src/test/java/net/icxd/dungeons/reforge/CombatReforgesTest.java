package net.icxd.dungeons.reforge;

import com.google.gson.JsonParser;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Reforges' bonuses in a fight: their numbers from their text at the item's rarity, and the weapon damage their lore leaves out. */
class CombatReforgesTest {
    private static final DataItem SWORD = item("""
            "TEST_REFORGE_SWORD":{"material":"IRON_SWORD","name":"Test Sword","rarity":"EPIC","reforgeable":true,\
            "stats":{"DAMAGE":100},"type":"SWORD"}""");

    @BeforeEach
    void table() {
        List<String> problems = new ArrayList<>();
        ReforgeTable.set(ReforgeTable.read(JsonParser.parseString("""
                {"reforges": {
                  "fanged": {"name": "Fanged", "stats": {"EPIC": {"STRENGTH": 10}},
                             "bonus": {"COMMON": ["&7Every &c7th &7melee hit on an enemy", "&7&7deals &c+100% &7damage."]}},
                  "warped": {"name": "Hyper", "stats": {},
                             "bonus": {"COMMON": ["&7Gain &f+1✦ Speed &7for &a5s &7after", "&7teleporting."],
                                       "EPIC": ["&7Gain &f+4✦ Speed &7for &a5s &7after", "&7teleporting."]}},
                  "suspicious": {"name": "Suspicious", "stats": {},
                                 "bonus": {"COMMON": ["&7Increases weapon damage by &c+15&7."]}}}}"""), problems));
        assertEquals(List.of(), problems);
    }

    @AfterEach
    void noTable() {
        ReforgeTable.set(null);
    }

    private static DataItem item(String json) {
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{" + json + "}}"));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static NBTTagCompound reforged(String reforge) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "TEST_REFORGE_SWORD");
        tag.setString("reforge", reforge);
        return tag;
    }

    @Test
    void numbersAtTheItemsRarity() {
        Reforge fanged = Reforge.of("fanged");
        // Rarities above the text's have the nearest lower one's: an Epic item, Common's text.
        assertArrayEquals(new double[] {7, 100}, CombatReforges.numbers(fanged, Rarity.EPIC));
        Reforge hyper = Reforge.of("warped");
        assertArrayEquals(new double[] {1, 5}, CombatReforges.numbers(hyper, Rarity.UNCOMMON));
        assertArrayEquals(new double[] {4, 5}, CombatReforges.numbers(hyper, Rarity.LEGENDARY));
        assertEquals(100, CombatReforges.number(fanged, SWORD, reforged("fanged"), 1));
        assertEquals(0, CombatReforges.number(fanged, SWORD, reforged("fanged"), 2));
        assertNotNull(CombatReforges.reforge(reforged("FANGED"), "fanged"));
        assertNull(CombatReforges.reforge(reforged("warped"), "fanged"));
    }
}
