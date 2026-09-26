package net.icxd.dungeons.item;

import net.icxd.dungeons.crimsonisle.kuudra.KuudraTier;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.Soulbound;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.requirement.KuudraTierRequirement;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonTierRequirement;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonType;
import net.icxd.dungeons.item.requirement.hotm.HeartOfTheMountainRequirement;
import net.icxd.dungeons.item.requirement.skill.SkillRequirement;
import net.icxd.dungeons.item.requirement.slayer.SlayerBossType;
import net.icxd.dungeons.item.requirement.slayer.SlayerRequirement;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Items from data (items.json, see {@link ItemData}): made-up items only, since Hypixel's text stays out
 * of this repository; and, when it's there, the real file, which only has to load and render.
 */
class ItemDataTest {
    private static final String SWORD = """
            "TEST_SWORD":{"abilities":[{"activation":"RIGHT_CLICK","cooldown":0.5,"header":"&6Ability: Test Strike  &e&lRIGHT CLICK",\
            "kind":"ABILITY","mana":45,"name":"Test Strike","text":["&7Hits a test dummy."]},{"header":"&6Full Set Bonus: Testing &7(0/4)",\
            "kind":"FULL_SET","name":"Testing","pieces":4,"text":["&7Tests things."]}],"can_have_attributes":true,\
            "categories":["Test Category"],"dungeon_item":true,"gear_score":100,"gemstone_slots":[{"type":"SAPPHIRE"},\
            {"costs":[{"coins":250000},{"amount":4,"item":"TEST_GEM"},{"amount":10,"essence":"WITHER"}],"type":"COMBAT"}],\
            "glowing":true,"lore":["&7A sword for tests.","","&7More about it."],"material":"IRON_SWORD","name":"Test Sword",\
            "npc_sell_price":12.5,"rarity":"EPIC","reforgeable":false,"requirements":[{"level":22,"skill":"COMBAT","type":"SKILL"},\
            {"level":7,"slayer_boss_type":"enderman","type":"SLAYER"},{"dungeon_type":"MASTER_CATACOMBS","tier":7,"type":"DUNGEON_TIER"},\
            {"tier":5,"type":"HEART_OF_THE_MOUNTAIN"},{"kuudra_tier":"NONE","type":"KUUDRA_COMPLETION"},\
            {"kuudra_tier":"HOT","type":"KUUDRA_COMPLETION"}],"shot_cooldown":0.25,"soulbound":"COOP",\
            "stats":{"CRIT_CHANCE":2.5,"DAMAGE":120},"type":"SWORD","type_label":"TEST WORDS","unstackable":true,\
            "upgrade_costs":[[{"amount":10,"essence":"WITHER"}],[{"amount":20,"essence":"CRIMSON"},{"amount":1,"item":"TEST_GEM"}]]}""";

    @AfterEach
    void javaItemsOnly() {
        ItemRegistry.clearData();
    }

    private static ItemData.Result load(String... items) throws IOException {
        return ItemData.load(new StringReader("{\"format\":1,\"source\":{\"test\":true},\"items\":{\n" + String.join(",\n", items) + "\n}}"));
    }

    private static DataItem only(String... items) throws IOException {
        ItemData.Result result = load(items);
        assertEquals(List.of(), result.errors());
        assertEquals(1, result.items().size());
        return result.items().values().iterator().next();
    }

    private static NBTTagCompound fresh(SkyBlockItem item) {
        NBTTagCompound tag = ItemBuilder.newData(item);
        for (String key : List.of("attribute_1", "attribute_1_level", "attribute_2", "attribute_2_level", "uuid")) tag.remove(key);
        return tag;
    }

    @Test
    void everyField() throws IOException {
        ItemData.Result result = load(SWORD);
        assertEquals(List.of(), result.warnings());
        DataItem item = result.items().get("TEST_SWORD");
        assertEquals("TEST_SWORD", item.id());
        assertEquals("Test Sword", item.name());
        assertEquals(Material.IRON_SWORD, item.material());
        assertEquals(Rarity.EPIC, item.rarity());
        assertEquals(SpecificItemType.SWORD, item.specificItemType());
        assertEquals(GenericItemType.WEAPON, item.genericItemType());
        assertEquals("SWORD", item.typeKey());
        assertEquals("TEST WORDS", item.typeLabel());
        assertEquals(List.of("Test Category"), item.categories());
        assertNull(item.skin());
        assertNull(item.color());
        assertTrue(item.glowing());
        assertTrue(item.unstackable());
        assertTrue(item.dungeonItem());
        assertTrue(item.canHaveAttributes());
        // A weapon is reforgeable by the usual rule; the data says it isn't.
        assertFalse(item.reforgeable());
        assertEquals(Soulbound.COOP, item.soulbound());
        assertEquals(100, item.gearScore());
        assertEquals(12.5, item.npcSellPrice());
        assertEquals(new Stats().set(Stat.DAMAGE, 120).set(Stat.CRIT_CHANCE, 2.5), item.stats());
        assertEquals(0.25, item.shotCooldown());
        assertEquals(List.of("&7A sword for tests.", "", "&7More about it."), item.lore());
        assertNull(item.ability());
        assertNull(item.nbt());
        assertTrue(item.isOwnable());

        List<GemstoneSlot> slots = item.gemstoneSlots().getSlots();
        assertEquals(List.of(GemstoneType.SAPPHIRE, GemstoneType.COMBAT), slots.stream().map(GemstoneSlot::getType).toList());
        assertEquals(List.of(), slots.get(0).getCosts());
        List<Cost> unlock = slots.get(1).getCosts();
        assertEquals(250000, assertInstanceOf(CoinCost.class, unlock.get(0)).getAmount());
        assertEquals("TEST_GEM", assertInstanceOf(ItemCost.class, unlock.get(1)).getItemId());
        assertEquals(4, ((ItemCost) unlock.get(1)).getAmount());
        assertEquals(EssenceType.WITHER, assertInstanceOf(EssenceCost.class, unlock.get(2)).getEssenceType());
        assertEquals(10, ((EssenceCost) unlock.get(2)).getAmount());

        List<UpgradeCost> stars = item.upgradeCosts().getCosts();
        assertEquals(2, stars.size());
        assertEquals(1, stars.get(0).getCosts().size());
        EssenceCost second = assertInstanceOf(EssenceCost.class, stars.get(1).getCosts().get(0));
        assertEquals(EssenceType.CRIMSON, second.getEssenceType());
        assertEquals(20, second.getAmount());
        assertEquals("TEST_GEM", assertInstanceOf(ItemCost.class, stars.get(1).getCosts().get(1)).getItemId());

        List<Requirement> requirements = item.requirements().getRequirements();
        assertEquals(6, requirements.size());
        SkillRequirement skill = assertInstanceOf(SkillRequirement.class, requirements.get(0));
        assertEquals(Skill.COMBAT, skill.getSkill());
        assertEquals(22, skill.getLevel());
        SlayerRequirement slayer = assertInstanceOf(SlayerRequirement.class, requirements.get(1));
        assertEquals(SlayerBossType.ENDERMAN, slayer.getBossType());
        assertEquals(7, slayer.getLevel());
        DungeonTierRequirement dungeon = assertInstanceOf(DungeonTierRequirement.class, requirements.get(2));
        assertEquals(DungeonType.MASTER_CATACOMBS, dungeon.getDungeonType());
        assertEquals(7, dungeon.getTier());
        assertEquals(5, assertInstanceOf(HeartOfTheMountainRequirement.class, requirements.get(3)).getLevel());
        // The API's NONE is the basic tier.
        assertEquals(KuudraTier.BASIC, assertInstanceOf(KuudraTierRequirement.class, requirements.get(4)).getTier());
        assertEquals(KuudraTier.HOT, ((KuudraTierRequirement) requirements.get(5)).getTier());

        assertEquals(List.of(
                new ItemBlock("ABILITY", "Test Strike", "&6Ability: Test Strike  &e&lRIGHT CLICK", "RIGHT_CLICK",
                        List.of("&7Hits a test dummy."), 45, 0, 0.5, 0, 0, 0, 0),
                new ItemBlock("FULL_SET", "Testing", "&6Full Set Bonus: Testing &7(0/4)", null, List.of("&7Tests things."), 0, 0, 0, 0, 0, 0, 4)),
                item.blocks());
    }

    @Test
    void onlyNameAndMaterialAreNeeded() throws IOException {
        DataItem item = only("\"TEST_ROCK\":{\"material\":\"STONE\",\"name\":\"Test Rock\"}");
        assertEquals(Rarity.COMMON, item.rarity());
        assertEquals(SpecificItemType.NONE, item.specificItemType());
        assertEquals("OTHER", item.typeKey());
        assertNull(item.typeLabel());
        assertEquals(new Stats(), item.stats());
        assertNull(item.gemstoneSlots());
        assertNull(item.upgradeCosts());
        assertNull(item.requirements());
        assertFalse(item.isOwnable());
        assertEquals(List.of(), item.categories());
        assertEquals(List.of(), item.lore());
        assertEquals(List.of(), item.blocks());
        assertEquals(Soulbound.NONE, item.soulbound());
        assertFalse(item.reforgeable());
        assertFalse(item.glowing() || item.unstackable() || item.dungeonItem() || item.canHaveAttributes());
        assertEquals(0, item.gearScore());
        assertEquals(0, item.npcSellPrice());
        assertEquals(0, item.shotCooldown());
    }

    @Test
    void headsAndColours() throws IOException {
        String hash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        ItemData.Result result = load(
                "\"TEST_HEAD\":{\"material\":\"PLAYER_HEAD\",\"name\":\"Test Head\",\"texture\":\"" + hash + "\"}",
                "\"TEST_SKIN\":{\"material\":\"PLAYER_HEAD\",\"name\":\"Test Skin\",\"skin\":\"dGVzdA==\",\"texture\":\"" + hash + "\"}",
                "\"TEST_BOOTS\":{\"color\":\"#E65300\",\"material\":\"LEATHER_BOOTS\",\"name\":\"Test Boots\",\"type\":\"BOOTS\"}");
        assertEquals(Utils.texture(hash), result.items().get("TEST_HEAD").skin());
        // An exact skin wins over the texture.
        assertEquals("dGVzdA==", result.items().get("TEST_SKIN").skin());
        assertEquals(Color.fromRGB(0xE65300), result.items().get("TEST_BOOTS").color());
        assertEquals(GenericItemType.ARMOR, result.items().get("TEST_BOOTS").genericItemType());
        assertTrue(result.items().get("TEST_BOOTS").reforgeable());
    }

    @Test
    void statsAreACopy() throws IOException {
        DataItem item = load(SWORD).items().get("TEST_SWORD");
        item.stats().add(Stat.DAMAGE, 1000);
        assertEquals(120, item.stats().get(Stat.DAMAGE));
    }

    /** Each bad item is skipped, for its reason; the rest load. */
    @Test
    void errorsSkipTheItem() throws IOException {
        ItemData.Result result = load(
                "\"TEST_ROCK\":{\"material\":\"STONE\",\"name\":\"Test Rock\"}",
                "\"NO_NAME\":{\"material\":\"STONE\"}",
                "\"BLANK_NAME\":{\"material\":\"STONE\",\"name\":\" \"}",
                "\"NO_MATERIAL\":{\"name\":\"Test\"}",
                "\"LEGACY_MATERIAL\":{\"material\":\"LEGACY_WOOL\",\"name\":\"Test\"}",
                "\"UNKNOWN_MATERIAL\":{\"material\":\"TEST_NOT_A_MATERIAL\",\"name\":\"Test\"}",
                "\"UNKNOWN_RARITY\":{\"material\":\"STONE\",\"name\":\"Test\",\"rarity\":\"TEST_RARE\"}",
                "\"UNKNOWN_SOULBOUND\":{\"material\":\"STONE\",\"name\":\"Test\",\"soulbound\":\"TEAM\"}",
                "\"TEST_ROCK\":{\"material\":\"STONE\",\"name\":\"Test Rock Again\"}",
                "\"test_rock\":{\"material\":\"STONE\",\"name\":\"Test Rock In Lower Case\"}",
                "\"STRING_NUMBER\":{\"gear_score\":\"100\",\"material\":\"STONE\",\"name\":\"Test\"}",
                "\"FRACTION\":{\"gear_score\":1.5,\"material\":\"STONE\",\"name\":\"Test\"}",
                "\"STRING_FLAG\":{\"glowing\":\"true\",\"material\":\"STONE\",\"name\":\"Test\"}",
                "\"STRING_STAT\":{\"material\":\"STONE\",\"name\":\"Test\",\"stats\":{\"DAMAGE\":\"a lot\"}}",
                "\"BAD_COLOR\":{\"color\":\"orange\",\"material\":\"LEATHER_BOOTS\",\"name\":\"Test\"}",
                "\"EMPTY_COST\":{\"material\":\"STONE\",\"name\":\"Test\",\"upgrade_costs\":[[{}]]}",
                "\"NO_AMOUNT\":{\"material\":\"STONE\",\"name\":\"Test\",\"upgrade_costs\":[[{\"item\":\"TEST_ROCK\"}]]}",
                "\"NO_LEVEL\":{\"material\":\"STONE\",\"name\":\"Test\",\"requirements\":[{\"skill\":\"COMBAT\",\"type\":\"SKILL\"}]}",
                "\"LIST_FOR_TEXT\":{\"lore\":\"one line\",\"material\":\"STONE\",\"name\":\"Test\"}",
                "\"NOT_AN_OBJECT\":5",
                "\"TEST_PEBBLE\":{\"material\":\"COBBLESTONE\",\"name\":\"Test Pebble\"}");
        assertEquals(List.of("TEST_ROCK", "TEST_PEBBLE"), List.copyOf(result.items().keySet()));
        assertEquals("Test Rock", result.items().get("TEST_ROCK").name());
        Map<String, String> kinds = new TreeMap<>();
        for (ItemData.Problem error : result.errors()) {
            // "TEST_ROCK" and "test_rock" both come after the first.
            kinds.merge(error.id(), error.kind(), (a, b) -> a + "," + b);
        }
        assertEquals(new TreeMap<>(Map.ofEntries(
                Map.entry("NO_NAME", "name"), Map.entry("BLANK_NAME", "name"), Map.entry("NO_MATERIAL", "material"),
                Map.entry("LEGACY_MATERIAL", "material"), Map.entry("UNKNOWN_MATERIAL", "material"),
                Map.entry("UNKNOWN_RARITY", "rarity"), Map.entry("UNKNOWN_SOULBOUND", "soulbound"),
                Map.entry("TEST_ROCK", "duplicate"), Map.entry("test_rock", "duplicate"),
                Map.entry("STRING_NUMBER", "value"), Map.entry("FRACTION", "value"), Map.entry("STRING_FLAG", "value"),
                Map.entry("STRING_STAT", "value"), Map.entry("BAD_COLOR", "value"), Map.entry("EMPTY_COST", "value"),
                Map.entry("NO_AMOUNT", "value"), Map.entry("NO_LEVEL", "value"), Map.entry("LIST_FOR_TEXT", "value"),
                Map.entry("NOT_AN_OBJECT", "value"))), kinds);
        assertEquals(List.of(), result.warnings());
    }

    /** Names this plugin doesn't have are left out, with a warning each; the item loads. */
    @Test
    void unknownNamesAreDropped() throws IOException {
        ItemData.Result result = load("""
                "TEST_WAND":{"abilities":[{"activation":"TEST_CLICK","header":"&6Ability: Test  &e&lTEST CLICK","kind":"ABILITY",\
                "name":"Test"}],"gemstone_slots":[{"type":"TEST_SLOT"},{"type":"RUBY"}],"material":"STICK","name":"Test Wand",\
                "requirements":[{"type":"TEST_REQUIREMENT"},{"level":5,"skill":"TEST_SKILL","type":"SKILL"},\
                {"level":1,"slayer_boss_type":"test_boss","type":"SLAYER"},{"dungeon_type":"TEST_DUNGEON","tier":1,"type":"DUNGEON_TIER"},\
                {"kuudra_tier":"TEST_TIER","type":"KUUDRA_COMPLETION"}],"stats":{"DAMAGE":10,"TEST_STAT":5},"type":"TEST_WAND_TYPE",\
                "upgrade_costs":[[{"amount":5,"essence":"TEST_ESSENCE"},{"coins":100}]]}""",
                "\"TEST_BAG\":{\"gemstone_slots\":[{\"type\":\"TEST_SLOT\"}],\"material\":\"CHEST\",\"name\":\"Test Bag\",\"type\":\"TEST_BAG\",\"type_label\":\"TEST LABEL\"}");
        assertEquals(List.of(), result.errors());
        assertEquals(List.of("TEST_WAND: type TEST_WAND_TYPE", "TEST_WAND: stat TEST_STAT", "TEST_WAND: gemstone slot TEST_SLOT",
                        "TEST_WAND: essence TEST_ESSENCE", "TEST_WAND: requirement TEST_REQUIREMENT", "TEST_WAND: requirement SKILL TEST_SKILL",
                        "TEST_WAND: requirement SLAYER test_boss", "TEST_WAND: requirement DUNGEON_TIER TEST_DUNGEON",
                        "TEST_WAND: requirement KUUDRA_COMPLETION TEST_TIER", "TEST_WAND: activation TEST_CLICK",
                        "TEST_BAG: type TEST_BAG", "TEST_BAG: gemstone slot TEST_SLOT"),
                result.warnings().stream().map(ItemData.Problem::toString).toList());

        DataItem wand = result.items().get("TEST_WAND");
        assertEquals(SpecificItemType.NONE, wand.specificItemType());
        assertEquals("TEST_WAND_TYPE", wand.typeKey());
        // Its rarity line still says what it is.
        assertEquals("TEST WAND TYPE", wand.typeLabel());
        assertEquals(new Stats().set(Stat.DAMAGE, 10), wand.stats());
        assertEquals(List.of(GemstoneType.RUBY), wand.gemstoneSlots().getSlots().stream().map(GemstoneSlot::getType).toList());
        assertNull(wand.requirements());
        List<Cost> star = wand.upgradeCosts().getCosts().get(0).getCosts();
        assertEquals(1, star.size());
        assertInstanceOf(CoinCost.class, star.get(0));
        assertNull(wand.blocks().get(0).activation());
        assertEquals("&6Ability: Test  &e&lTEST CLICK", wand.blocks().get(0).header());

        DataItem bag = result.items().get("TEST_BAG");
        assertEquals("TEST LABEL", bag.typeLabel());
        assertEquals("TEST_BAG", bag.typeKey());
        assertNull(bag.gemstoneSlots());
    }

    @Test
    void wholeFileFailures() {
        assertThrows(IOException.class, () -> ItemData.load(new StringReader("{\"format\":2,\"items\":{}}")));
        assertThrows(IOException.class, () -> ItemData.load(new StringReader("{\"items\":{}}")));
        assertThrows(IOException.class, () -> ItemData.load(new StringReader("{\"format\":1,\"items\":{\"TEST_ROCK\":{\"name\":")));
        assertThrows(IOException.class, () -> ItemData.load(new StringReader("{\"format\":1,\"items\":{}} {}")));
    }

    @Test
    void blocksShowWhereAbilitiesDo() throws IOException {
        DataItem staff = only("""
                "TEST_STAFF":{"abilities":[{"activation":"RIGHT_CLICK","cooldown":30,"header":"&6Ability: Test Blast  &e&lRIGHT CLICK",\
                "health_cost":15.6,"kind":"ABILITY","mana":1000,"name":"Test Blast","text":["&7Blasts a test."],"vitality":30},\
                {"activation":"RIGHT_CLICK","cooldown":7200,"header":"&6Ability: Test Orb  &e&lRIGHT CLICK","kind":"ABILITY",\
                "mana_percent":50,"name":"Test Orb","soulflow":2},{"header":"&9Shortbow: Instantly shoots!","kind":"SHORTBOW","name":"Instantly shoots!"}],\
                "lore":["&7A staff for tests."],"material":"STICK","name":"Test Staff","rarity":"RARE","soulbound":"SOLO",\
                "stats":{"DAMAGE":60},"type":"WAND"}""");
        NBTTagCompound tag = fresh(staff);
        assertEquals("§9Test Staff", ItemBuilder.name(staff, tag));
        assertEquals(List.of(
                "&7Damage: &c+60",
                "",
                "&7A staff for tests.",
                "",
                "&6Ability: Test Blast  &e&lRIGHT CLICK",
                "&7Blasts a test.",
                "&8Mana Cost: &b1,000✎",
                "&8Health Cost: &c15.6❤",
                "&8Vitality Cost: &430♨",
                "&8Cooldown: &a30s",
                "",
                "&6Ability: Test Orb  &e&lRIGHT CLICK",
                "&8Soulflow Cost: &32",
                "&8Mana Cost: &b50% of max",
                "&8Cooldown: &a2h",
                "",
                "&9Shortbow: Instantly shoots!",
                "",
                "&8This item can be reforged!",
                "&8&l* &8Soulbound &8&l*",
                "§9§lRARE WAND"), ItemBuilder.lore(staff, tag));
    }

    @Test
    void rarityLineWords() throws IOException {
        ItemData.Result result = load(
                "\"TEST_THING\":{\"material\":\"STONE\",\"name\":\"Test Thing\",\"type\":\"TEST_THING\"}",
                "\"TEST_LABELLED\":{\"dungeon_item\":true,\"material\":\"IRON_SWORD\",\"name\":\"Test\",\"type\":\"SWORD\",\"type_label\":\"TEST WORDS\"}",
                "\"TEST_DUNGEON_ITEM\":{\"dungeon_item\":true,\"material\":\"STONE\",\"name\":\"Test\",\"rarity\":\"RARE\",\"type_label\":\"ITEM\"}",
                "\"TEST_COOP\":{\"material\":\"STONE\",\"name\":\"Test\",\"soulbound\":\"COOP\"}",
                "\"TEST_SACK\":{\"material\":\"STONE\",\"name\":\"Test\",\"rarity\":\"RARE\",\"type\":\"TEST_SACK\",\"type_label\":\"\"}");
        Map<String, DataItem> items = result.items();
        assertEquals("§f§lCOMMON TEST THING", last(items.get("TEST_THING")));
        assertEquals("§f§lCOMMON DUNGEON TEST WORDS", last(items.get("TEST_LABELLED")));
        assertEquals("§9§lRARE DUNGEON ITEM", last(items.get("TEST_DUNGEON_ITEM")));
        // An empty label: just the rarity.
        assertEquals("§9§lRARE", last(items.get("TEST_SACK")));
        List<String> coop = ItemBuilder.lore(items.get("TEST_COOP"), fresh(items.get("TEST_COOP")));
        assertEquals("&8&l* &8Co-op Soulbound &8&l*", coop.get(coop.size() - 2));

        NBTTagCompound recombobulated = fresh(items.get("TEST_THING"));
        recombobulated.setBoolean("recombobulated", true);
        assertEquals("§a§l&ka&r §a§lUNCOMMON TEST THING §a§l&ka", ItemBuilder.rarityLine(items.get("TEST_THING"), recombobulated,
                ItemBuilder.rarity(items.get("TEST_THING"), recombobulated)));
    }

    @Test
    void cooldowns() {
        assertEquals("0.5s", ItemBuilder.cooldown(0.5));
        assertEquals("90s", ItemBuilder.cooldown(90));
        assertEquals("1m", ItemBuilder.cooldown(60));
        assertEquals("2h", ItemBuilder.cooldown(7200));
        assertEquals("48h", ItemBuilder.cooldown(172800));
    }

    private static String last(SkyBlockItem item) {
        List<String> lore = ItemBuilder.lore(item, fresh(item));
        return lore.get(lore.size() - 1);
    }

    /** The Java items win over data with their ids, and a second load replaces the first one's items. */
    @Test
    void registryKeepsJavaItems(@TempDir Path folder) throws IOException {
        SkyBlockItem hyperion = ItemRegistry.get("HYPERION");
        int javaItems = ItemRegistry.getRegistry().size();
        Path file = folder.resolve("items.json");
        Files.writeString(file, "{\"format\":1,\"items\":{\"HYPERION\":{\"material\":\"STONE\",\"name\":\"Test Rock\"},"
                + "\"TEST_ROCK\":{\"material\":\"STONE\",\"name\":\"Test Rock\"},\"NO_NAME\":{\"material\":\"STONE\"}}}");
        ItemRegistry.LoadReport report = ItemRegistry.loadData(file);
        assertNull(report.failure());
        assertEquals(1, report.loaded());
        assertEquals(1, report.javaKept());
        assertEquals(Map.of("name", 1), report.errorsByKind());
        assertSame(hyperion, ItemRegistry.get("HYPERION"));
        assertInstanceOf(DataItem.class, ItemRegistry.get("test_rock"));
        assertEquals(javaItems + 1, ItemRegistry.getRegistry().size());

        Files.writeString(file, "{\"format\":1,\"items\":{\"TEST_PEBBLE\":{\"material\":\"STONE\",\"name\":\"Test Pebble\"}}}");
        ItemRegistry.loadData(file);
        assertNull(ItemRegistry.get("TEST_ROCK"));
        assertInstanceOf(DataItem.class, ItemRegistry.get("TEST_PEBBLE"));

        ItemRegistry.LoadReport missing = ItemRegistry.loadData(folder.resolve("none.json"));
        assertEquals("missing", missing.failure());
        assertEquals(javaItems, ItemRegistry.getRegistry().size());
    }

    /**
     * The real items.json (-Ditems.file, else the private data repository next to this one), when it's
     * here: it loads as a whole, and every item it has renders.
     */
    @Test
    void realFile() {
        String property = System.getProperty("items.file");
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        Path file = property != null ? Path.of(property) : repository.resolveSibling("skyblock-dungeon-data/items/items.json");
        assumeTrue(Files.exists(file), "no " + file);

        ItemRegistry.LoadReport report = ItemRegistry.loadData(file);
        assertNull(report.failure(), "the file didn't load");
        System.out.println(file + ": " + report.loaded() + " items loaded in " + report.millis() + " ms, " + report.javaKept()
                + " kept as their Java items; " + report.errors().size() + " skipped " + report.errorsByKind() + "; "
                + report.warnings().size() + " names dropped " + report.warningsByKind());

        long start = System.nanoTime();
        Map<String, List<String>> failures = new TreeMap<>();
        int rendered = 0;
        for (SkyBlockItem item : ItemRegistry.getRegistry().values()) {
            if (!(item instanceof DataItem)) continue;
            try {
                NBTTagCompound tag = fresh(item);
                ItemBuilder.name(item, tag);
                ItemBuilder.lore(item, tag);
                rendered++;
            } catch (RuntimeException e) {
                failures.computeIfAbsent(e.toString(), k -> new java.util.ArrayList<>()).add(item.id());
            }
        }
        System.out.println(rendered + " rendered in " + (System.nanoTime() - start) / 1_000_000 + " ms");
        assertEquals(Map.of(), failures.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())),
                () -> "items that don't render: " + failures);
    }
}
