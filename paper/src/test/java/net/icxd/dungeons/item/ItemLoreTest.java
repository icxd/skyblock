package net.icxd.dungeons.item;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enchanting.FakeEnchantments;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How item names and lore are laid out, as Hypixel lays them out, on made-up items and a made-up enchantment
 * table (Hypixel's text stays out of this repository). Items have no owner here, so no requirement lines.
 */
class ItemLoreTest {
    private static final String STAR = "[{\"amount\":10,\"essence\":\"WITHER\"}]";
    private static final DataItem SWORD = item("""
            "TEST_BLINK_SWORD":{"abilities":[{"activation":"RIGHT_CLICK","header":"&6Ability: Test Blink  &e&lRIGHT CLICK",\
            "kind":"ABILITY","mana":45,"name":"Test Blink","text":["&7Moves you &a5 test blocks&7 ahead.","&7Then stops."]}],\
            "gemstone_slots":[{"type":"SAPPHIRE"}],"material":"DIAMOND_SHOVEL","name":"Test Blink Sword","rarity":"EPIC",\
            "reforgeable":true,"stats":{"DAMAGE":120,"STRENGTH":100},"type":"SWORD"}""");
    private static final DataItem GEM = item("""
            "TEST_FINE_GEM":{"categories":["Test Category"],"lore":["&7A gem for tests.","","&7It gives &c❤ Health&7."],\
            "material":"PLAYER_HEAD","name":"❤ Fine Test Gemstone","rarity":"RARE",\
            "texture":"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef","type":"GEMSTONE"}""");
    private static final DataItem DUNGEON_SWORD = item("""
            "TEST_DUNGEON_SWORD":{"dungeon_item":true,"gear_score":500,"gemstone_slots":[{"type":"SAPPHIRE"},{"costs":[{"coins":100}],\
            "type":"COMBAT"}],"lore":["&7A dungeon sword for tests."],"material":"IRON_SWORD","name":"Test Dungeon Sword",\
            "rarity":"LEGENDARY","reforgeable":true,"stats":{"DAMAGE":200,"FEROCITY":20,"INTELLIGENCE":300,"STRENGTH":100},\
            "type":"SWORD","upgrade_costs":[@,@,@,@,@]}""".replace("@", STAR));
    private static final DataItem HELMET = item("""
            "TEST_HELMET":{"material":"IRON_HELMET","name":"Test Helmet","rarity":"LEGENDARY","stats":{"DEFENSE":100},\
            "type":"HELMET","upgrade_costs":[@,@,@,@,@,@,@,@,@,@]}""".replace("@", STAR));

    /** A Necron's blade (see ItemBehaviours): its id is what gives it the scrolls. */
    private static final String BLADE = """
            "NECRON_BLADE":{"dungeon_item":true,"lore":["&7A blade for tests.","","&eRight-click to use your class ability!"],\
            "material":"IRON_SWORD","name":"Test Blade","rarity":"LEGENDARY","reforgeable":true,"stats":{"DAMAGE":100},"type":"SWORD"}""";

    @BeforeEach
    void enchantments() {
        FakeEnchantments.use();
    }

    @AfterEach
    void noItems() {
        ItemRegistry.clearData();
        FakeEnchantments.reset();
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

    private static NBTTagCompound data(SkyBlockItem item) {
        NBTTagCompound tag = ItemBuilder.newData(item);
        // Attributes are random; these tests leave them out.
        tag.remove("attribute_1");
        tag.remove("attribute_2");
        return tag;
    }

    private static void enchant(NBTTagCompound tag, String name, int level) {
        NBTTagList list = tag.getList("enchantments", 10);
        NBTTagCompound enchantment = new NBTTagCompound();
        enchantment.setString("name", name);
        enchantment.setInt("lvl", level);
        list.add(enchantment);
        tag.set("enchantments", list);
    }

    /** Stats, the gemstone line, the ability with its cost, then the reforge note and the rarity line. */
    @Test
    void swordWithAnAbility() {
        NBTTagCompound tag = data(SWORD);
        assertEquals("§5Test Blink Sword", ItemBuilder.name(SWORD, tag));
        assertEquals(List.of(
                "&7Damage: &c+120",
                "&7Strength: &c+100",
                "&7Gemstones: &8[&7✎&8]",
                "",
                "&6Ability: Test Blink  &e&lRIGHT CLICK",
                "&7Moves you &a5 test blocks&7 ahead.",
                "&7Then stops.",
                "&8Mana Cost: &b45✎",
                "",
                "&8This item can be reforged!",
                "§5§lEPIC SWORD"), ItemBuilder.lore(SWORD, tag));
    }

    /** The dark gray lines under the name, the item's own text, and its type on the rarity line. */
    @Test
    void gemstone() {
        NBTTagCompound tag = data(GEM);
        assertEquals("§9❤ Fine Test Gemstone", ItemBuilder.name(GEM, tag));
        assertEquals(List.of(
                "&8Test Category",
                "",
                "&7A gem for tests.",
                "",
                "&7It gives &c❤ Health&7.",
                "",
                "§9§lRARE GEMSTONE"), ItemBuilder.lore(GEM, tag));
    }

    /** Hypixel's dungeon items show what each stat comes to in a dungeon: +10% at Catacombs 0. */
    @Test
    void dungeonItemStats() {
        List<String> lore = ItemBuilder.lore(DUNGEON_SWORD, data(DUNGEON_SWORD));
        assertEquals("&7Gear Score: &d500", lore.get(0));
        assertEquals("&7Damage: &c+200 &8(+220)", lore.get(1));
        assertEquals("&7Ferocity: &c+20 &8(+20)", lore.get(3));
        assertEquals("§6§lLEGENDARY DUNGEON SWORD", lore.get(lore.size() - 1));
    }

    @Test
    void oneEnchantWithItsDescription() {
        NBTTagCompound tag = data(SWORD);
        enchant(tag, "sharpness", 5);
        List<String> lore = ItemBuilder.lore(SWORD, tag);
        assertEquals(List.of("", "&9Sharpness V", "&7Increases melee damage dealt by &a30%", ""), lore.subList(3, 7));
    }

    /**
     * On a dungeon item even two enchantments share a line, without descriptions. Critical's crit damage
     * is part of the item's stats, as the recorded Giant's Sword's "Crit Damage: +70%" (Critical VI) was.
     */
    @Test
    void dungeonItemEnchantsAreCompact() {
        NBTTagCompound tag = data(DUNGEON_SWORD);
        enchant(tag, "smite", 7);
        enchant(tag, "critical", 6);
        List<String> lore = ItemBuilder.lore(DUNGEON_SWORD, tag);
        assertEquals("&7Crit Damage: &9+70% &8(+77%)", lore.get(3));
        assertEquals("&9Critical VI, &9Smite VII", lore.get(8));
    }

    @Test
    void stars() {
        NBTTagCompound tag = data(DUNGEON_SWORD);
        tag.setInt("upgrade_count", 7);
        assertEquals(" &6✪✪✪✪✪&c➋", ItemBuilder.stars(DUNGEON_SWORD, tag));
        NBTTagCompound helmet = data(HELMET);
        helmet.setInt("upgrade_count", 6);
        assertEquals(" &d✪&6✪✪✪✪", ItemBuilder.stars(HELMET, helmet));
    }

    /** One rarity up, in the name and the rarity line, from the item's own rarity and the flag. */
    @Test
    void recombobulated() {
        NBTTagCompound tag = data(DUNGEON_SWORD);
        tag.setBoolean("recombobulated", true);
        assertEquals(Rarity.MYTHIC, ItemBuilder.rarity(DUNGEON_SWORD, tag));
        assertEquals("§dTest Dungeon Sword", ItemBuilder.name(DUNGEON_SWORD, tag));
        List<String> lore = ItemBuilder.lore(DUNGEON_SWORD, tag);
        assertEquals("§d§l&ka&r §d§lMYTHIC DUNGEON SWORD §d§l&ka", lore.get(lore.size() - 1));
    }

    /** Items used to keep their rarity in their data too; what they show now is the item's. */
    @Test
    void storedRarityIsIgnored() {
        NBTTagCompound tag = data(DUNGEON_SWORD);
        tag.setString("rarity", "COMMON");
        assertEquals(Rarity.LEGENDARY, ItemBuilder.rarity(DUNGEON_SWORD, tag));
        // As /recombobulate left them: the upgraded rarity stored as well as the flag.
        tag.setString("rarity", "MYTHIC");
        tag.setBoolean("recombobulated", true);
        assertEquals(Rarity.MYTHIC, ItemBuilder.rarity(DUNGEON_SWORD, tag));
    }

    /** A scroll's item, whose ability block is what a blade with the scroll gets. */
    private static String scroll(String id, String ability, int mana) {
        return "\"" + id + "\":{\"abilities\":[{\"activation\":\"RIGHT_CLICK\",\"cooldown\":10,\"header\":\"&6Ability: " + ability
                + "  &e&lRIGHT CLICK\",\"kind\":\"ABILITY\",\"mana\":" + mana + ",\"name\":\"" + ability + "\",\"text\":[\"&7" + ability
                + " for tests.\"]}],\"lore\":[\"&7A scroll for tests.\"],\"material\":\"PAPER\",\"name\":\"" + ability + " Scroll\"}";
    }

    /** The lore under the blade's own text, which it always starts with. */
    private static List<String> afterText(SkyBlockItem blade, boolean implosion, boolean shield, boolean warp) {
        NBTTagCompound tag = data(blade);
        tag.setBoolean("implosion", implosion);
        tag.setBoolean("wither_shield", shield);
        tag.setBoolean("shadow_warp", warp);
        List<String> lore = ItemBuilder.lore(blade, tag);
        assertEquals(List.of("&7Damage: &c+100 &8(+110)", "", "&7A blade for tests."), lore.subList(0, 3));
        return new ArrayList<>(lore.subList(3, lore.size()));
    }

    /**
     * A Necron's blade's scrolls are its abilities, each as its scroll's item shows it, always in the order
     * Implosion, Wither Shield, Shadow Warp; all three make Wither Impact. The class ability line goes once
     * it has any.
     */
    @Test
    void necronsBladeScrolls(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("items.json");
        Files.writeString(file, "{\"format\":1,\"items\":{" + String.join(",", BLADE, scroll("IMPLOSION_SCROLL", "Test Burst", 300),
                scroll("WITHER_SHIELD_SCROLL", "Test Guard", 150), scroll("SHADOW_WARP_SCROLL", "Test Warp", 300)) + "}}");
        ItemRegistry.loadData(file);
        SkyBlockItem blade = ItemRegistry.get("NECRON_BLADE");

        NBTTagCompound fresh = ItemBuilder.newData(blade);
        assertEquals(List.of("0b", "0b", "0b"), List.of(fresh.get("implosion").toString(), fresh.get("wither_shield").toString(),
                fresh.get("shadow_warp").toString()));
        String rarity = "§6§lLEGENDARY DUNGEON SWORD";
        assertEquals(List.of("", "&eRight-click to use your class ability!", "", "&8This item can be reforged!", rarity),
                afterText(blade, false, false, false));
        assertEquals(List.of(
                "",
                "&6Ability: Test Burst  &e&lRIGHT CLICK",
                "&7Test Burst for tests.",
                "&8Mana Cost: &b300✎",
                "&8Cooldown: &a10s",
                "",
                "&6Ability: Test Warp  &e&lRIGHT CLICK",
                "&7Test Warp for tests.",
                "&8Mana Cost: &b300✎",
                "&8Cooldown: &a10s",
                "",
                "&8This item can be reforged!",
                rarity), afterText(blade, true, false, true));
        assertEquals(List.of(
                "",
                "&6Ability: Test Guard  &e&lRIGHT CLICK",
                "&7Test Guard for tests.",
                "&8Mana Cost: &b150✎",
                "&8Cooldown: &a10s",
                "",
                "&6Ability: Test Warp  &e&lRIGHT CLICK",
                "&7Test Warp for tests.",
                "&8Mana Cost: &b300✎",
                "&8Cooldown: &a10s",
                "",
                "&8This item can be reforged!",
                rarity), afterText(blade, false, true, true));
        List<String> impact = afterText(blade, true, true, true);
        assertEquals(List.of("", "&6Ability: Wither Impact  &e&lRIGHT CLICK"), impact.subList(0, 2));
        assertEquals(List.of("&8Mana Cost: &b300✎", "", "&8This item can be reforged!", rarity), impact.subList(impact.size() - 4, impact.size()));
        assertTrue(impact.stream().noneMatch(line -> line.contains("Test") || line.contains("class ability")), () -> "" + impact);
    }

    @Test
    void catacombsBoost() {
        assertEquals(0.10, ItemBuilder.catacombsBoost(0), 1e-9);
        assertEquals(0.31, ItemBuilder.catacombsBoost(4), 1e-9);
        assertEquals(1.95, ItemBuilder.catacombsBoost(21), 1e-9);
        assertEquals(4.85, ItemBuilder.catacombsBoost(50), 1e-9);
    }
}
