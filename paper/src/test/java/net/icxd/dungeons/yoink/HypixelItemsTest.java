package net.icxd.dungeons.yoink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.enchanting.FakeEnchantments;
import net.icxd.dungeons.item.gemstone.Gem;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.item.gemstone.GemstoneQuality;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.modifier.ItemModifiers;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;

/**
 * The fixture's items (profiles.json, made by profiles.py) made into this plugin's, on made-up items with Hypixel's ids
 * and the made-up enchantment table.
 */
class HypixelItemsTest {
    private static final UUID ME = UUID.fromString("0f0e0d0c-0b0a-0908-0706-050403020100");
    private static final String ITEMS = """
            {"format":1,"items":{
            "ASPECT_OF_THE_END":{"material":"DIAMOND_SWORD","name":"Test End Sword","rarity":"RARE","type":"SWORD","unstackable":true,
              "reforgeable":true,"stats":{"DAMAGE":100},"requirements":[{"type":"SKILL","skill":"COMBAT","level":5}]},
            "HYPERION":{"material":"IRON_SWORD","name":"Test Blade","rarity":"LEGENDARY","type":"SWORD","unstackable":true,"dungeon_item":true,
              "reforgeable":true,"gemstone_slots":[{"type":"SAPPHIRE"},{"type":"COMBAT","costs":[{"coins":250000}]}]},
            "TEST_GEMMED":{"material":"STICK","name":"Test Gems","rarity":"RARE","gemstone_slots":[{"type":"SAPPHIRE"},{"type":"COMBAT"},
              {"type":"SAPPHIRE"}]},
            "ENCHANTED_DIAMOND":{"material":"DIAMOND","name":"Test Diamond","rarity":"UNCOMMON"},
            "POWER_WITHER_HELMET":{"material":"IRON_HELMET","name":"Test Head","rarity":"LEGENDARY","type":"HELMET","unstackable":true,
              "dungeon_item":true,"can_have_attributes":true},
            "POWER_WITHER_CHESTPLATE":{"material":"LEATHER_CHESTPLATE","name":"Test Chestplate","rarity":"LEGENDARY","type":"CHESTPLATE",
              "unstackable":true,"dungeon_item":true},
            "POWER_WITHER_LEGGINGS":{"material":"LEATHER_LEGGINGS","name":"Test Leggings","rarity":"LEGENDARY","type":"LEGGINGS","unstackable":true},
            "PET":{"material":"PLAYER_HEAD","name":"Test Pet","rarity":"COMMON"},
            "POTION":{"material":"POTION","name":"Test Bottle","rarity":"COMMON"}}}""";

    @TempDir
    Path folder;

    @BeforeEach
    void items() throws IOException {
        FakeEnchantments.use();
        Path file = folder.resolve("items.json");
        Files.writeString(file, ITEMS);
        assertNull(ItemRegistry.loadData(file).failure());
    }

    @AfterEach
    void none() {
        // A file that isn't there: no items.
        ItemRegistry.loadData(folder.resolve("none.json"));
        FakeEnchantments.reset();
    }

    private static List<HypixelItems.Item> read(String path, HypixelItems.Left left) throws IOException {
        return HypixelItems.read(HypixelProfileTest.banana().at(path + ".data").getAsString(), ME, left);
    }

    private static List<String> enchantments(NBTTagCompound tag) {
        NBTTagList list = tag.getList("enchantments", 10);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(list.get(i).getString("name") + " " + list.get(i).getInt("lvl"));
        return out;
    }

    @Test
    void whatWasDoneToThem() throws IOException {
        HypixelItems.Left left = new HypixelItems.Left();
        List<HypixelItems.Item> inventory = read("inventory.inv_contents", left);
        assertEquals(36, inventory.size());

        HypixelItems.Item aote = inventory.get(0);
        assertEquals("ASPECT_OF_THE_END", aote.kind().id());
        NBTTagCompound tag = aote.tag();
        assertEquals("11111111-2222-3333-4444-555555555555", tag.getString("uuid"), "Hypixel's uuid");
        assertEquals(ME.toString(), tag.getString("owner"), "it has requirements, so it's owned");
        assertEquals("heroic", tag.getString("reforge"));
        assertTrue(tag.getBoolean("recombobulated"));
        assertEquals(12, tag.getInt("hot_potato_books"));
        assertTrue(tag.getBoolean("art_of_war"));
        assertEquals(4, tag.getInt(ItemModifiers.TUNERS));
        assertEquals(List.of("sharpness 6", "wise 3", "critical 6"), enchantments(tag), "an ultimate without its ultimate_");
        assertFalse(tag.hasKey("timestamp"));

        NBTTagCompound blade = inventory.get(1).tag();
        assertEquals(8, blade.getInt("upgrade_count"));
        assertEquals("withered", blade.getString("reforge"));
        assertTrue(blade.getBoolean("implosion") && blade.getBoolean("shadow_warp") && blade.getBoolean("wither_shield"));
        assertEquals(15, blade.getInt("hot_potato_books"));
        assertEquals(1234, blade.getInt("stats_book"));
        assertEquals(1000.0, blade.getDouble("champion_combat_xp"));
        assertEquals("", blade.getString("owner"), "nothing to own it for");
        List<GemSlots.Slot> slots = GemSlots.of(inventory.get(1).kind(), blade);
        assertEquals(new Gem(GemstoneType.SAPPHIRE, GemstoneQuality.PERFECT), slots.get(0).gem());
        assertEquals(new Gem(GemstoneType.JASPER, GemstoneQuality.FLAWLESS), slots.get(1).gem(), "a Combat slot's gem, newer items' way");
        assertFalse(slots.get(1).locked());

        HypixelItems.Item diamonds = inventory.get(2);
        assertEquals(64, diamonds.amount());
        assertFalse(diamonds.tag().hasKey("uuid"), "it stacks");

        // The pet, the empty slots, and what this plugin hasn't.
        assertNull(inventory.get(3));
        assertNull(inventory.get(4));
        assertNull(inventory.get(10));
        assertEquals(Map.of("ENDER_DRAGON Pet", 1, "NOT_A_REAL_ITEM_EVER", 3), left.items);
        assertTrue(left.enchantments.isEmpty());
    }

    @Test
    void armorAttributesAndRunes() throws IOException {
        HypixelItems.Left left = new HypixelItems.Left();
        List<HypixelItems.Item> armor = read("inventory.inv_armor", left);
        assertNull(armor.get(0), "boots first, and there are no such boots");
        assertEquals("POWER_WITHER_LEGGINGS", armor.get(1).kind().id());
        NBTTagCompound chest = armor.get(2).tag();
        assertEquals("GRAND_SEARING", chest.getString("rune"));
        assertEquals(3, chest.getInt("rune_level"));
        NBTTagCompound helmet = armor.get(3).tag();
        assertEquals("", helmet.getString("rune"), "there's no Music rune here");
        assertEquals("MANA_POOL", helmet.getString("attribute_1"));
        assertEquals(5, helmet.getInt("attribute_1_level"));
        assertFalse(helmet.hasKey("attribute_2"), "not a random one");
        assertEquals(5, helmet.getInt("upgrade_count"));
        assertEquals(List.of("growth 5"), enchantments(helmet));
        assertEquals(Map.of("protection", 1), left.enchantments);
        assertEquals(Map.of("POWER_WITHER_BOOTS", 1), left.items);
    }

    @Test
    void theMenuAndPotionsStayHere() {
        HypixelItems.Left left = new HypixelItems.Left();
        assertNull(HypixelItems.item(slot(HypixelItems.SKYBLOCK_MENU, 1), ME, left));
        assertNull(HypixelItems.item(slot("POTION", 2), ME, left));
        assertNull(HypixelItems.item(Map.of("id", (short) 1, "Count", (byte) 5), ME, left));
        assertEquals(Map.of("POTION", 2, "minecraft:1", 5), left.items);
    }

    private static Map<String, Object> slot(String id, int count) {
        Map<String, Object> slot = new LinkedHashMap<>();
        slot.put("id", (short) 399);
        slot.put("Count", (byte) count);
        slot.put("tag", Map.of("ExtraAttributes", Map.of("id", id)));
        return slot;
    }

    @Test
    void gemSlotsByTypeAndNumber() {
        var slots = ItemRegistry.get("TEST_GEMMED").gemstoneSlots().getSlots();
        assertEquals(0, HypixelItems.slot(slots, "SAPPHIRE_0"));
        assertEquals(2, HypixelItems.slot(slots, "SAPPHIRE_1"));
        assertEquals(1, HypixelItems.slot(slots, "COMBAT_0"));
        assertEquals(-1, HypixelItems.slot(slots, "COMBAT_1"));
        assertEquals(-1, HypixelItems.slot(slots, "JASPER_0"));
        assertEquals(-1, HypixelItems.slot(slots, "COMBAT_0_gem"));
        assertEquals(-1, HypixelItems.slot(slots, "unlocked_slots"));
    }

    @Test
    void wardrobePagesAsSets() {
        List<String> slots = new ArrayList<>();
        for (int page = 0; page < 2; page++) {
            for (int row = 0; row < 4; row++) for (int column = 0; column < 9; column++) slots.add(page + ":" + row + ":" + column);
        }
        List<List<String>> sets = Yoink.wardrobeSets(slots);
        assertEquals(18, sets.size());
        assertEquals(List.of("0:0:0", "0:1:0", "0:2:0", "0:3:0"), sets.get(0), "helmet to boots");
        assertEquals(List.of("1:0:1", "1:1:1", "1:2:1", "1:3:1"), sets.get(10));
        assertTrue(Yoink.wardrobeSets(List.of()).isEmpty());
    }

    @Test
    void whatsLeftMostFirst() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("A", 1);
        counts.put("B", 5);
        assertEquals("B x5&8, &7A", Yoink.summary(counts));
        for (int i = 0; i < 13; i++) counts.put("C" + i, 2);
        assertTrue(Yoink.summary(counts).startsWith("B x5&8, &7C"));
        assertTrue(Yoink.summary(counts).endsWith(" &8and 3 more"));
        assertEquals("1 item", Yoink.plural(1, "item"));
        assertEquals("0 sets", Yoink.plural(0, "set"));
        assertEquals("2 accessories", Yoink.plural(2, "accessory"));
    }
}
