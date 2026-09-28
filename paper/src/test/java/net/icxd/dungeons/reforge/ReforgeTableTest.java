package net.icxd.dungeons.reforge;

import com.google.gson.JsonParser;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.stats.Stat;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The reforge table and how items' reforges are read from it, on a made-up table (Hypixel's is private). */
class ReforgeTableTest {
    /**
     * Two basic pools and three stones: "Keen" (made up) from a stone on swords, with a bonus and a Strength a
     * Catacombs level, "Shiny" on vacuums, "Odd One" only on one item, and a Divine row (and bonus) on the pickaxe one.
     */
    static final String TABLE = """
            {"format": 1,
             "random_price": {"COMMON": 10, "LEGENDARY": 50, "VERY_SPECIAL": 90},
             "reforges": {
              "heroic": {"name": "Heroic", "stats": {"EPIC": {"STRENGTH": 3, "INTELLIGENCE": 8}, "MYTHIC": {"STRENGTH": 5}}},
              "gentle": {"name": "Gentle", "stats": {"EPIC": {"STRENGTH": 1}}},
              "sharp": {"name": "Sharp", "stats": {}},
              "clean": {"name": "Clean", "stats": {"EPIC": {"HEALTH": 2}}},
              "keen": {"name": "Keen", "stats": {"LEGENDARY": {"STRENGTH": 10}},
                       "bonus": {"RARE": ["&7Grants a test."], "LEGENDARY": ["&7Grants more", "&7of a test."]},
                       "per_catacombs_level": {"STRENGTH": 1}},
              "shiny": {"name": "Shiny", "stats": {"RARE": {"SPEED": 1}}},
              "odd_one": {"name": "Odd One", "bonus_title": "&9Test's Gift &8(Odd One)", "stats": {},
                          "bonus": {"COMMON": ["&7A gift."]}},
              "deep": {"name": "Deep", "stats": {"MYTHIC": {"MINING_SPEED": 4}, "DIVINE": {"MINING_SPEED": 6}},
                       "bonus": {"MYTHIC": ["&7Mythic's."], "DIVINE": ["&7Divine's."]}},
              "wise": {"name": "Wise", "stats": {}}
             },
             "pools": [{"type": "SWORD/ROD", "reforges": ["gentle", "heroic", "sharp"]}, {"type": "ARMOR", "reforges": ["clean", "wise"]},
                       {"type": "PICKAXE", "reforges": ["gentle"]}],
             "stones": [
              {"item": "TEST_STONE", "reforge": "keen", "type": "SWORD", "costs": {"RARE": 100, "LEGENDARY": 300}},
              {"item": "TEST_WINGS", "reforge": "shiny", "type": "VACUUM", "costs": {"RARE": 5}},
              {"item": "TEST_GIFT", "reforge": "odd_one", "items": ["TEST_ONE_ITEM"], "costs": {"COMMON": 7}},
              {"item": "TEST_ORE", "reforge": "deep", "type": "PICKAXE", "costs": {"MYTHIC": 1, "DIVINE": 2}}
             ],
             "prefixes": [{"reforge": "wise", "name": "Wise Test", "prefix": "Very"}]}
            """;

    @AfterEach
    void noTable() {
        ReforgeTable.set(null);
    }

    static ReforgeTable table() {
        List<String> problems = new ArrayList<>();
        ReforgeTable table = ReforgeTable.read(JsonParser.parseString(TABLE), problems);
        assertEquals(List.of(), problems);
        return table;
    }

    /** A made-up item of this Hypixel type ("SWORD", "VACUUM"), reforgeable as its data says. */
    static SkyBlockItem item(String id, String type, boolean reforgeable) {
        SpecificItemType specific;
        try {
            specific = SpecificItemType.valueOf(type);
        } catch (IllegalArgumentException e) {
            specific = SpecificItemType.NONE;
        }
        SpecificItemType known = specific;
        return new SkyBlockItem() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return "Test " + type;
            }

            @Override
            public Material material() {
                return Material.STICK;
            }

            @Override
            public SpecificItemType specificItemType() {
                return known;
            }

            @Override
            public String typeKey() {
                return type;
            }

            @Override
            public boolean reforgeable() {
                return reforgeable;
            }
        };
    }

    @Test
    void readsItemsReforgeByIdOrOldName() {
        ReforgeTable.set(table());
        Reforge heroic = ReforgeTable.get().reforges().get("heroic");
        // Items made before the table kept the enum's name.
        assertSame(heroic, Reforge.of("HEROIC"));
        assertSame(heroic, Reforge.of("heroic"));
        assertSame(heroic, Reforge.of("Heroic"));
        assertSame(ReforgeTable.get().reforges().get("odd_one"), Reforge.of("Odd One"));
        assertNull(Reforge.of(""));
        assertNull(Reforge.of((String) null));
    }

    /** One the table doesn't have (or with no table at all) keeps a name and gives nothing: nothing throws. */
    @Test
    void anUnknownReforgeKeepsItsName() {
        Reforge unknown = Reforge.of("double_bit");
        assertEquals("Double Bit", unknown.name());
        assertEquals("double_bit", unknown.id());
        assertEquals(0, unknown.stat(Stat.STRENGTH, Rarity.MYTHIC, 50));
        assertEquals(List.of(), unknown.bonusSection(Rarity.MYTHIC));
        assertEquals("Withered", Reforge.of("WITHERED").name());
        ReforgeTable.set(table());
        assertEquals("Godly", Reforge.of("godly").name());
    }

    /** Special and Very Special give Mythic's numbers; a rarity with none (Divine, but on mining tools) gives nothing. */
    @Test
    void statsByRarity() {
        ReforgeTable table = table();
        Reforge heroic = table.reforges().get("heroic");
        assertEquals(3, heroic.stat(Stat.STRENGTH, Rarity.EPIC, 0));
        assertEquals(8, heroic.statsAt(Rarity.EPIC, 0).get(Stat.INTELLIGENCE));
        assertEquals(5, heroic.stat(Stat.STRENGTH, Rarity.SPECIAL, 0));
        assertEquals(5, heroic.stat(Stat.STRENGTH, Rarity.VERY_SPECIAL, 0));
        assertEquals(0, heroic.stat(Stat.STRENGTH, Rarity.DIVINE, 0));
        assertEquals(0, heroic.stat(Stat.STRENGTH, Rarity.COMMON, 0));
        Reforge deep = table.reforges().get("deep");
        assertEquals(6, deep.stat(Stat.MINING_SPEED, Rarity.DIVINE, 0));
        assertEquals(4, deep.stat(Stat.MINING_SPEED, Rarity.SPECIAL, 0));
    }

    /** Withered's kind: a stat a Catacombs level, on top of the rarity's. */
    @Test
    void aStatACatacombsLevel() {
        Reforge keen = table().reforges().get("keen");
        assertEquals(10, keen.stat(Stat.STRENGTH, Rarity.LEGENDARY, 0));
        assertEquals(22, keen.stat(Stat.STRENGTH, Rarity.LEGENDARY, 12));
        assertEquals(22, keen.statsAt(Rarity.LEGENDARY, 12).get(Stat.STRENGTH));
        assertEquals(12, keen.stat(Stat.STRENGTH, Rarity.EPIC, 12));
    }

    /** A rarity with no text of its own shows the nearest lower one's; the heading is "&9<Reforge> Bonus" or its own. */
    @Test
    void bonus() {
        ReforgeTable table = table();
        Reforge keen = table.reforges().get("keen");
        assertEquals(List.of(), keen.bonusSection(Rarity.UNCOMMON));
        assertEquals(List.of("&9Keen Bonus", "&7Grants a test."), keen.bonusSection(Rarity.RARE));
        assertEquals(List.of("&9Keen Bonus", "&7Grants a test."), keen.bonusSection(Rarity.EPIC));
        assertEquals(List.of("&9Keen Bonus", "&7Grants more", "&7of a test."), keen.bonusSection(Rarity.DIVINE));
        assertEquals(List.of("&9Test's Gift &8(Odd One)", "&7A gift."), table.reforges().get("odd_one").bonusSection(Rarity.EPIC));
        // Special and Very Special have Mythic's, as their numbers are, not Divine's.
        Reforge deep = table.reforges().get("deep");
        assertEquals(List.of("&7Divine's."), deep.bonusLines(Rarity.DIVINE));
        assertEquals(List.of("&7Mythic's."), deep.bonusLines(Rarity.SPECIAL));
        assertEquals(List.of("&7Mythic's."), deep.bonusLines(Rarity.VERY_SPECIAL));
    }

    @Test
    void anItemNamedLikeItsReforge() {
        Reforge wise = table().reforges().get("wise");
        assertEquals("Very", wise.prefix("Wise Test Helmet"));
        assertEquals("Wise", wise.prefix("Wiser Test Helmet"));
        assertEquals("Wise", wise.prefix("Test Helmet"));
    }

    /** A stone goes on its type's items, at the rarities it has a fee for, and only on items that can be reforged. */
    @Test
    void whichStonesFit() {
        ReforgeTable table = table();
        SkyBlockItem longsword = item("TEST_LONGSWORD", "LONGSWORD", true);
        assertEquals(List.of("TEST_STONE"), table.stones(longsword, Rarity.LEGENDARY).stream().map(ReforgeStone::item).toList());
        assertEquals(List.of(), table.stones(longsword, Rarity.EPIC));
        assertEquals(List.of(), table.stones(item("TEST_RIFT_SWORD", "SWORD", false), Rarity.LEGENDARY));
        assertEquals(List.of(), table.stones(item("TEST_BOW", "BOW", true), Rarity.LEGENDARY));
        // Vacuums never say they can be reforged, but take their stones.
        SkyBlockItem vacuum = item("TEST_VACUUM", "VACUUM", false);
        assertEquals(List.of("TEST_WINGS"), table.stones(vacuum, Rarity.RARE).stream().map(ReforgeStone::item).toList());
        assertTrue(table.reforgeable(vacuum));
        // A stone of named items goes on those alone, whatever their data says.
        assertEquals(1, table.stones(item("TEST_ONE_ITEM", "WAND", false), Rarity.COMMON).size());
        assertEquals(0, table.stones(item("TEST_OTHER_ITEM", "WAND", true), Rarity.COMMON).size());
        assertEquals(1, table.stones(item("TEST_DRILL", "DRILL", true), Rarity.DIVINE).size());
    }

    @Test
    void pools() {
        ReforgeTable table = table();
        assertEquals("SWORD/ROD", table.pool(item("TEST_ROD", "FISHING_ROD", true)).type());
        assertEquals("ARMOR", table.pool(item("TEST_MASK", "CARNIVAL_MASK", true)).type());
        // UNKNOWN which Hypixel rolls a gauntlet from: the first pool it's of.
        assertEquals("SWORD/ROD", table.pool(item("TEST_GAUNTLET", "GAUNTLET", true)).type());
        assertNull(table.pool(item("TEST_SWORD", "SWORD", false)));
        assertNull(table.pool(item("TEST_WAND", "WAND", true)));
        assertFalse(table.reforgeable(item("TEST_WAND", "WAND", true)));
        assertTrue(table.reforgeable(item("TEST_SWORD", "SWORD", true)));
        // Without the table, whether the item says it can be.
        assertTrue(ReforgeTable.EMPTY.reforgeable(item("TEST_WAND", "WAND", true)));
        assertEquals(50, table.randomPrice(Rarity.LEGENDARY));
        assertNull(table.randomPrice(Rarity.UNOBTAINABLE));
    }

    /** A random reforge is any of the pool's but the one the item has. */
    @Test
    void rollsAnotherReforge() {
        ReforgeTable table = table();
        ReforgeTable.Pool pool = table.pools().getFirst();
        Reforge heroic = table.reforges().get("heroic");
        Random random = new Random(7);
        Set<String> rolled = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            Reforge next = ReforgeTable.roll(pool, heroic, random);
            assertNotEquals("heroic", next.id());
            rolled.add(next.id());
        }
        assertEquals(Set.of("gentle", "sharp"), rolled);
        ReforgeTable.Pool lone = table.pools().get(2);
        assertNull(ReforgeTable.roll(lone, table.reforges().get("gentle"), random));
        assertEquals("gentle", ReforgeTable.roll(lone, null, random).id());
    }

    /** What's wrong in the file is a problem, never an exception, and the rest is read. */
    @Test
    void problems() {
        List<String> problems = new ArrayList<>();
        ReforgeTable table = ReforgeTable.read(JsonParser.parseString("""
                {"reforges": {"a": {"name": "A", "stats": {"EPIC": {"NOT_A_STAT": 1, "SPEED": 2}, "NOT_A_RARITY": {}}}},
                 "pools": [{"type": "SWORD/ROD", "reforges": ["a", "b"]}],
                 "stones": [{"item": "X", "reforge": "b", "type": "SWORD", "costs": {}},
                            {"item": "Y", "reforge": "a", "type": "NOT_A_TYPE", "costs": {}}]}"""), problems);
        assertEquals(List.of("a: no stat NOT_A_STAT", "a: no rarity NOT_A_RARITY", "pool SWORD/ROD: no reforge b", "X: no reforge b",
                "Y: goes on an unknown type NOT_A_TYPE"), problems);
        assertEquals(2, table.reforges().get("a").stat(Stat.SPEED, Rarity.EPIC, 0));
        assertEquals(List.of(), table.stones());
        assertNull(ReforgeTable.read(null, problems));
    }
}
