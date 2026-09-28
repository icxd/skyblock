package net.icxd.dungeons.item.enchanting.armor;

import com.google.gson.JsonParser;
import net.icxd.dungeons.collection.CollectionData;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.reforge.ArmorReforgeBonuses;
import net.icxd.dungeons.reforge.ReforgeTable;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The armor enchantments' and reforges' rules, on a made-up enchantment table whose texts have the books' shapes
 * with made-up numbers (Hypixel's table stays out of this repository), and made-up items.
 */
class ArmorEnchantsTest {
    private static final DataItem HELMET = item("TEST_ARMOR_HELMET", "HELMET", "");
    private static final DataItem BOOTS = item("TEST_ARMOR_BOOTS", "BOOTS", "");
    private static final DataItem SLAYER_SWORD = item("TEST_SLAYER_SWORD", "SWORD",
            ",\"requirements\":[{\"level\":3,\"slayer_boss_type\":\"zombie\",\"type\":\"SLAYER\"}]");
    private static final DataItem SLAYER_WAND = item("TEST_SLAYER_WAND", "WAND",
            ",\"requirements\":[{\"level\":1,\"slayer_boss_type\":\"zombie\",\"type\":\"SLAYER\"}]");

    @BeforeEach
    void tables() {
        List<String> problems = new ArrayList<>();
        EnchantmentData.use(EnchantmentData.parse(JsonParser.parseString(enchantments()).getAsJsonObject(), problems));
        ReforgeTable.set(ReforgeTable.read(JsonParser.parseString("""
                {"reforges": {
                  "renowned": {"name": "Renowned", "stats": {"EPIC": {"HEALTH": 1}},
                               "bonus": {"EPIC": ["&7Increases all &cCombat &7stats and &b✯ &bMagic Find &7by &a+3%&7."]}},
                  "undead": {"name": "Undead", "stats": {"EPIC": {"HEALTH": 1}},
                             "bonus": {"EPIC": ["&7Decreases damage taken from &2&2༕ &2Undead&7 mobs by &a4%&7."]}}}}"""), problems));
        assertEquals(List.of(), problems);
    }

    @AfterEach
    void noTables() {
        EnchantmentData.use(EnchantmentData.EMPTY);
        ReforgeTable.set(null);
    }

    private static String entry(String id, String levels) {
        return "\"" + id + "\":{\"name\":\"Test\",\"hypixel\":\"" + id + "\",\"ultimate\":false,\"min\":1,\"max\":3,\"table\":null,"
                + "\"xp\":[1,2,3],\"enchanting\":0,\"applies\":[\"Armor\"],\"conflicts\":[],\"levels\":{" + levels + "}}";
    }

    private static String level(int level, String text) {
        return "\"" + level + "\":{\"text\":\"" + text + "\",\"rarity\":\"COMMON\"}";
    }

    private static String enchantments() {
        return "{\"format\":1,\"order\":[],\"enchantments\":{" + String.join(",",
                entry("thorns", level(1, "&7Grants a &a50% &7chance to rebound &a5% &7of damage dealt back at the attacker.")),
                entry("feather_falling", level(2, "&7Increases how high you can fall before taking fall damage by &a2&7 and reduces fall "
                        + "damage by &a8%&7.")),
                entry("habanero_tactics", level(1, "&8ℏ &7Heal &a+5% &7more from wands. &8ℏ &7Deal &c+10% damage &7with Slayer weapons. "
                        + "&8ℏ &7Gain &3+2☯ Combat Wisdom &7with Slayer weapons.")),
                entry("hecatomb", "\"1\":{\"text\":\"&7Gain &a+0.5% &cCatacombs &7XP & &a+1% &3Class &7XP, doubled on &b&lS+ &7runs. Grants "
                        + "&c+3❤ &7per 10 &cCatacombs &7levels.\",\"rarity\":\"COMMON\",\"tier_up\":\"&83 S runs to tier up!\"},"
                        + "\"2\":{\"text\":\"&7Gain &a+0.6% &cCatacombs &7XP.\",\"rarity\":\"COMMON\",\"tier_up\":\"&87 S runs to tier up!\"},"
                        + "\"3\":{\"text\":\"&7Gain &a+0.7% &cCatacombs &7XP.\",\"rarity\":\"COMMON\"}")) + "}}";
    }

    private static DataItem item(String id, String type, String more) {
        String json = "{\"format\":1,\"items\":{\"" + id + "\":{\"material\":\"STONE\",\"name\":\"Test " + type + "\",\"rarity\":\"EPIC\","
                + "\"type\":\"" + type + "\"" + more + "}}}";
        try {
            ItemData.Result result = ItemData.load(new StringReader(json));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A worn piece of this item with these enchantments (id, level, ...) and this reforge (null for none). */
    private static Worn.Piece worn(DataItem item, String reforge, Object... enchantments) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", item.id());
        if (reforge != null) tag.setString("reforge", reforge);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < enchantments.length; i += 2) {
            NBTTagCompound enchantment = new NBTTagCompound();
            enchantment.setString("name", (String) enchantments[i]);
            enchantment.setShort("lvl", (short) (int) (Integer) enchantments[i + 1]);
            list.add(enchantment);
        }
        tag.set("enchantments", list);
        return new Worn.Piece(item, tag, item.blocks(), null);
    }

    // ---------- the numbers ----------

    /** Every number in order, colour codes left out ("&82" is 2), decimals and thousands. */
    @Test
    void numbersInATextsOrder() {
        assertArrayEquals(new double[] {50, 5}, EnchantNumbers.numbers("&7Grants a &a50% &7chance to rebound &a5% &7of damage."));
        assertArrayEquals(new double[] {2}, EnchantNumbers.numbers("&82 S runs to tier up!"));
        assertArrayEquals(new double[] {1000, 0.5}, EnchantNumbers.numbers("&c+1,000❤ and &a0.5x"));
        assertArrayEquals(new double[0], EnchantNumbers.numbers(null));
    }

    /** A level's text's numbers from the table in use; none for a level or an enchantment it doesn't have, and the tier-up line's. */
    @Test
    void numbersFromTheTable() {
        assertArrayEquals(new double[] {50, 5}, EnchantNumbers.of("thorns", 1));
        assertEquals(5, EnchantNumbers.get("thorns", 1, 1));
        assertEquals(0, EnchantNumbers.get("thorns", 1, 7));
        assertArrayEquals(new double[0], EnchantNumbers.of("thorns", 2));
        assertArrayEquals(new double[0], EnchantNumbers.of("thorns", 9));
        assertArrayEquals(new double[0], EnchantNumbers.of("not_one", 1));
        assertArrayEquals(new double[] {3}, EnchantNumbers.tierUp("hecatomb", 1));
        assertArrayEquals(new double[0], EnchantNumbers.tierUp("hecatomb", 3));
    }

    // ---------- worn pieces ----------

    /** Each piece's enchantments by the plugin's id (an ultimate's without "ultimate_"), its reforge, and totals over pieces. */
    @Test
    void wornPieces() {
        List<WornEnchants.Piece> pieces = WornEnchants.pieces(new Worn(List.of(
                worn(HELMET, "renowned", "thorns", 1, "ultimate_legion", 2), worn(BOOTS, null, "thorns", 1, "feather_falling", 2))));
        assertEquals(2, pieces.size());
        assertEquals(2, pieces.get(0).level("legion"));
        assertEquals(0, pieces.get(1).level("legion"));
        assertTrue(pieces.get(0).reforged(ArmorReforgeBonuses.RENOWNED));
        assertFalse(pieces.get(1).reforged(ArmorReforgeBonuses.RENOWNED));
        assertEquals(2, WornEnchants.total(pieces, "thorns"));
        assertSame(pieces.get(1), WornEnchants.ofType(pieces, SpecificItemType.BOOTS));
        assertNull(WornEnchants.ofType(pieces, SpecificItemType.NECKLACE));
        assertEquals(List.of(), WornEnchants.pieces(Worn.NOTHING));
        // Renowned's percent from its bonus's text at the piece's rarity.
        assertEquals(3, ArmorReforgeBonuses.combatPercent(pieces), 1e-9);
        assertEquals(0, ArmorReforgeBonuses.defensePercent(pieces), 1e-9);
    }

    // ---------- hits taken ----------

    /** Thorns rebounds its share of what the hit took when its roll comes up; Reflection's is times their Intelligence. */
    @Test
    void thornsAndReflection() {
        assertEquals(50, HitsTaken.thorns(1000, 50, 5, 0.2), 1e-9);
        assertEquals(0, HitsTaken.thorns(1000, 50, 5, 0.6), 1e-9);
        assertEquals(0, HitsTaken.thorns(-5, 50, 5, 0), 1e-9);
        assertEquals(3000, HitsTaken.reflection(100, 30), 1e-9);
        assertEquals(0, HitsTaken.reflection(-20, 30), 1e-9);
    }

    /** Last Stand goes off when health falls from its share or more to below it, not when it was already below, nor for a killing hit. */
    @Test
    void lastStandFallsBelow() {
        assertTrue(HitsTaken.fellBelow(500, 300, 1000, 0.4));
        assertTrue(HitsTaken.fellBelow(400, 399, 1000, 0.4));
        assertFalse(HitsTaken.fellBelow(390, 300, 1000, 0.4));
        assertFalse(HitsTaken.fellBelow(900, 500, 1000, 0.4));
        assertFalse(HitsTaken.fellBelow(900, 0, 1000, 0.4));
    }

    // ---------- conversions ----------

    /** A conversion adds to what's left of it while it lasts, never past its max; once it's run out it starts again. */
    @Test
    void conversionsPool() {
        assertEquals(10, Conversions.pooled(0, 0, 1000, 10, 25), 1e-9);
        assertEquals(22, Conversions.pooled(12, 5000, 1000, 10, 25), 1e-9);
        assertEquals(25, Conversions.pooled(20, 5000, 1000, 10, 25), 1e-9);
        assertEquals(10, Conversions.pooled(20, 900, 1000, 10, 25), 1e-9);
    }

    // ---------- protections ----------

    /** Feather Falling's cut is its text's, with Old Blood's 3% a level on top; fall damage never goes below none. */
    @Test
    void featherFalling() {
        List<WornEnchants.Piece> pieces = WornEnchants.pieces(new Worn(List.of(worn(BOOTS, null, "feather_falling", 2))));
        assertEquals(8, Protections.fallReduction(pieces, false), 1e-9);
        assertEquals(14, Protections.fallReduction(pieces, true), 1e-9);
        assertEquals(2, Protections.sum(pieces, Protections.FEATHER_FALLING), 1e-9);
        assertEquals(0.92, Protections.fallFactor(8), 1e-9);
        assertEquals(0, Protections.fallFactor(130), 1e-9);
        assertEquals(1, Protections.waterMovement(100), 1e-9);
        assertEquals(0.33, Protections.waterMovement(33), 1e-9);
        assertEquals(1, Protections.waterMovement(150), 1e-9);
    }

    // ---------- stats ----------

    /** Cayenne's digits of Accessory Power, Wisdom's Intelligence per five levels up to its cap, Hecatomb's Health per ten Catacombs levels. */
    @Test
    void statsFromOtherThings() {
        assertEquals(0, StatEnchants.digits(0));
        assertEquals(1, StatEnchants.digits(9));
        assertEquals(4, StatEnchants.digits(1234));
        Stats cayenne = new Stats();
        StatEnchants.cayenne(cayenne, new double[] {1, 0.5}, 4);
        assertEquals(4, cayenne.get(Stat.HEALTH), 1e-9);
        assertEquals(2, cayenne.get(Stat.TRUE_DEFENSE), 1e-9);
        Stats theOne = new Stats();
        StatEnchants.theOne(theOne, new double[] {1, 0.2, 73}, 10);
        assertEquals(10, theOne.get(Stat.HEALTH), 1e-9);
        assertEquals(2, theOne.get(Stat.STRENGTH), 1e-9);
        assertEquals(6, StatEnchants.wisdom(new double[] {2, 5, 20}, 17), 1e-9);
        assertEquals(20, StatEnchants.wisdom(new double[] {2, 5, 20}, 200), 1e-9);
        assertEquals(0, StatEnchants.wisdom(new double[] {2, 5, 20}, 4), 1e-9);
        assertEquals(12, StatEnchants.hecatombHealth(new double[] {0.5, 1, 3, 10}, 42), 1e-9);
        assertEquals(0, StatEnchants.hecatombHealth(new double[] {0.5, 1, 3, 10}, 9), 1e-9);
    }

    /** Quantum: Vitality on weekdays; on weekends one Wisdom, the same on the Saturday and the Sunday. */
    @Test
    void quantum() {
        Stats weekday = new Stats();
        StatEnchants.quantum(weekday, new double[] {3, 2}, LocalDate.of(2026, 9, 28));
        assertEquals(3, weekday.get(Stat.VITALITY), 1e-9);
        Stat saturday = StatEnchants.weekendWisdom(LocalDate.of(2026, 10, 3));
        assertTrue(StatEnchants.WISDOMS.contains(saturday));
        assertEquals(saturday, StatEnchants.weekendWisdom(LocalDate.of(2026, 10, 4)));
        Stats weekend = new Stats();
        StatEnchants.quantum(weekend, new double[] {3, 2}, LocalDate.of(2026, 10, 4));
        assertEquals(2, weekend.get(saturday), 1e-9);
        assertEquals(0, weekend.get(Stat.VITALITY), 1e-9);
    }

    /** Legion's share for each player near up to its most, and the shares more of the Combat stats and Magic Find (Defense's own on top). */
    @Test
    void legionAndRaising() {
        assertEquals(0.7, StatEnchants.legion(0.35, 2, 20), 1e-9);
        assertEquals(7, StatEnchants.legion(0.35, 30, 20), 1e-9);
        Stats stats = new Stats().set(Stat.STRENGTH, 100).set(Stat.DEFENSE, 100).set(Stat.MAGIC_FIND, 50).set(Stat.SPEED, 100);
        StatEnchants.raise(stats, 10, 2);
        assertEquals(110, stats.get(Stat.STRENGTH), 1e-9);
        assertEquals(112, stats.get(Stat.DEFENSE), 1e-9);
        assertEquals(55, stats.get(Stat.MAGIC_FIND), 1e-9);
        assertEquals(100, stats.get(Stat.SPEED), 1e-9);
    }

    /** The One counts the collections at their last tier. */
    @Test
    void maxedCollections() {
        CollectionData.Tier tier = new CollectionData.Tier(100, List.of());
        List<CollectionData.Collection> collections = List.of(
                new CollectionData.Collection("A", "A", "FARMING", "A", null, 0, List.of(tier)),
                new CollectionData.Collection("B", "B", "FARMING", "B", null, 0, List.of(tier, new CollectionData.Tier(500, List.of()))),
                new CollectionData.Collection("C", "C", "FARMING", "C", null, 0, List.of()));
        Document profile = new Document("collections", new Document(Map.of("A", 100, "B", 400, "C", 10)));
        assertEquals(1, StatEnchants.maxedCollections(profile, collections));
    }

    // ---------- Habanero Tactics ----------

    /** A Slayer weapon asks for a Slayer level; a wand that does isn't one. Its damage and heal are its text's, piece by piece. */
    @Test
    void habanero() {
        assertTrue(Habanero.slayerWeapon(SLAYER_SWORD));
        assertFalse(Habanero.slayerWeapon(SLAYER_WAND));
        assertFalse(Habanero.slayerWeapon(HELMET));
        assertFalse(Habanero.slayerWeapon((DataItem) null));
        List<WornEnchants.Piece> pieces = WornEnchants.pieces(new Worn(List.of(
                worn(HELMET, null, "ultimate_habanero_tactics", 1), worn(BOOTS, null, "habanero_tactics", 1))));
        assertEquals(20, Habanero.damage(pieces), 1e-9);
        assertEquals(10, Habanero.wandHeal(pieces), 1e-9);
    }

    // ---------- Hecatomb ----------

    /** Its boost doubles on S+; S and S+ runs count; each level's "N S runs to tier up!" is the count the next takes in all. */
    @Test
    void hecatomb() {
        assertEquals(0.5, Hecatomb.boost(EnchantNumbers.of("hecatomb", 1), "S").catacombs(), 1e-9);
        assertEquals(2, Hecatomb.boost(EnchantNumbers.of("hecatomb", 1), "S+").classes(), 1e-9);
        assertTrue(Hecatomb.sRun("S"));
        assertTrue(Hecatomb.sRun("S+"));
        assertFalse(Hecatomb.sRun("A"));
        assertEquals(1, Hecatomb.level(1, 2, at -> EnchantNumbers.tierUp("hecatomb", at)));
        assertEquals(2, Hecatomb.level(1, 3, at -> EnchantNumbers.tierUp("hecatomb", at)));
        assertEquals(3, Hecatomb.level(1, 50, at -> EnchantNumbers.tierUp("hecatomb", at)));
        assertEquals(3, Hecatomb.level(3, 51, at -> EnchantNumbers.tierUp("hecatomb", at)));
    }

    // ---------- the rest ----------

    /** Transylvanian's heal up to its most, and Stealth's roll and message. */
    @Test
    void theRest() {
        assertEquals(6, ArmorEnchants.transylvanian(2, 3, 20), 1e-9);
        assertEquals(20, ArmorEnchants.transylvanian(2, 30, 20), 1e-9);
        assertTrue(Stealth.stays(60, 0.5));
        assertFalse(Stealth.stays(60, 0.7));
        assertEquals("&7&oThe Scared Skeleton didn't notice your presence thanks to your Stealth enchantment!", Stealth.message("Scared Skeleton"));
    }
}
