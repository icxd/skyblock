package net.icxd.dungeons.item;

import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.upgrade.Book;
import net.icxd.dungeons.item.upgrade.Stars;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the Hex's books, stars and dungeon conversion do to an item's stats and lore, against live items (the
 * September 2026 auction house), on made-up items.
 */
class ItemUpgradesTest {
    private static final String STAR = "[{\"amount\":30,\"essence\":\"CRIMSON\"}]";
    /** Like the live Crimson Chestplate: 230 Health and 65 Defense, 10 stars, not a dungeon item. */
    private static final DataItem CHESTPLATE = item("""
            "TEST_CHESTPLATE":{"material":"IRON_CHESTPLATE","name":"Test Chestplate","rarity":"LEGENDARY",\
            "stats":{"HEALTH":230,"DEFENSE":65,"HEALTH_REGEN":2,"VITALITY":10},"type":"CHESTPLATE","upgrade_costs":[@,@,@,@,@,@,@,@,@,@]}"""
            .replace("@", STAR));
    /** A sword that can be made a dungeon item, like the Aspect of the Dragons. */
    private static final DataItem SWORD = item("""
            "TEST_DRAGON_SWORD":{"dungeon_conversion_cost":[{"amount":150,"essence":"DRAGON"}],"material":"DIAMOND_SWORD",\
            "name":"Test Dragon Sword","rarity":"LEGENDARY","reforgeable":true,"stats":{"DAMAGE":225,"STRENGTH":100},"type":"SWORD",\
            "upgrade_costs":[@,@,@,@,@]}""".replace("@", STAR));
    private static final DataItem AXE = item("""
            "TEST_AXE":{"material":"IRON_AXE","name":"Test Axe","rarity":"EPIC","stats":{"DAMAGE":10},"type":"AXE"}""");
    private static final DataItem DRILL = item("""
            "TEST_DRILL":{"material":"PRISMARINE_SHARD","name":"Test Drill","rarity":"LEGENDARY",\
            "stats":{"MINING_SPEED":1500,"MINING_FORTUNE":100},"type":"DRILL"}""");
    private static final DataItem VACUUM = item("""
            "TEST_VACUUM":{"material":"IRON_SHOVEL","name":"Test Vacuum","rarity":"LEGENDARY",\
            "stats":{"DAMAGE":400,"FARMING_FORTUNE":25},"type":"VACUUM"}""");
    private static final DataItem BLINK = item("""
            "TEST_BLINK_SWORD":{"abilities":[{"activation":"RIGHT_CLICK","header":"&6Ability: Test Blink  &e&lRIGHT CLICK",\
            "kind":"ABILITY","mana":45,"name":"Test Blink","text":["&7Moves you ahead."]}],"lore":["&7A sword for tests."],\
            "material":"DIAMOND_SHOVEL","name":"Test Blink Sword","rarity":"EPIC","reforgeable":true,"stats":{"DAMAGE":120},"type":"SWORD"}""");

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
        tag.remove("attribute_1");
        tag.remove("attribute_2");
        return tag;
    }

    private static List<String> stats(SkyBlockItem item, NBTTagCompound tag) {
        return ItemBuilder.statLines(item, tag, ItemBuilder.rarity(item, tag), null);
    }

    /**
     * Stars on an item that isn't a dungeon item: 2% of its own stats each, every star, with no bracket (the live
     * Crimson Chestplate's 230 Health: 234.6 with 1, 257.6 with 6), but not Health Regen or Vitality.
     */
    @Test
    void starsOutsideDungeonItems() {
        NBTTagCompound tag = data(CHESTPLATE);
        tag.setInt("upgrade_count", 1);
        assertEquals("&7Health: &c+234.6", stats(CHESTPLATE, tag).get(0));
        tag.setInt("upgrade_count", 6);
        assertEquals(List.of("&7Health: &c+257.6", "&7Defense: &a+72.8", "&7Health Regen: &c+2", "&7Vitality: &4+10"), stats(CHESTPLATE, tag));
        Stats stats = ItemStats.of(CHESTPLATE, tag, null);
        assertEquals(257.6, stats.get(Stat.HEALTH), 1e-9);
        assertEquals(2, stats.get(Stat.HEALTH_REGEN), 1e-9);
        // In a dungeon too: it isn't a dungeon item, so nothing more.
        assertEquals(257.6, ItemStats.of(CHESTPLATE, tag, 1.95).get(Stat.HEALTH), 1e-9);
        // Past 5, purple from the left.
        assertEquals(" &d✪&6✪✪✪✪", ItemBuilder.stars(CHESTPLATE, tag));
    }

    /** Potato books on an axe (live: the Silva Dominus's "(+18)"), and The Art of Peace after them (the live Mender Crown's). */
    @Test
    void books() {
        NBTTagCompound axe = data(AXE);
        axe.setInt("hot_potato_books", 9);
        Book.ART_OF_WAR.apply(axe);
        assertEquals(List.of("&7Damage: &c+28 &e(+18)", "&7Strength: &c+23 &e(+18) &6[+5]"), stats(AXE, axe));
        assertEquals(23, ItemStats.of(AXE, axe, null).get(Stat.STRENGTH), 1e-9);

        NBTTagCompound chestplate = data(CHESTPLATE);
        chestplate.setInt("hot_potato_books", 15);
        Book.ART_OF_PEACE.apply(chestplate);
        assertEquals("&7Health: &c+330 &e(+60) &c[+40]", stats(CHESTPLATE, chestplate).get(0));
        assertEquals(330, ItemStats.of(CHESTPLATE, chestplate, null).get(Stat.HEALTH), 1e-9);
    }

    /** The live Divan's Drill's and Infini-Vacuum's brackets, before the reforge's. */
    @Test
    void toolBooks() {
        NBTTagCompound drill = data(DRILL);
        drill.setInt("polarvoid", 5);
        assertEquals(List.of("&7Mining Speed: &6+1,550 &9[+50]", "&7Mining Fortune: &6+105 &9[+5]"), stats(DRILL, drill));

        NBTTagCompound vacuum = data(VACUUM);
        vacuum.setInt("bookworm_books", 5);
        vacuum.setInt("farming_for_dummies_count", 5);
        assertEquals(List.of("&7Damage: &c+500 &6(+100)", "&7Farming Fortune: &6+30 &a(+5)"), stats(VACUUM, vacuum));
        Stats stats = ItemStats.of(VACUUM, vacuum, null);
        assertEquals(500, stats.get(Stat.DAMAGE), 1e-9);
        assertEquals(30, stats.get(Stat.FARMING_FORTUNE), 1e-9);
    }

    /** The Book of Stats' count: its own section after the abilities, before "This item can be reforged!" (live). */
    @Test
    void kills() {
        NBTTagCompound tag = data(BLINK);
        tag.setInt("stats_book", 2133);
        List<String> lore = ItemBuilder.lore(BLINK, tag);
        assertEquals(List.of("&8Mana Cost: &b45✎", "", "&fKills: &62,133", "", "&8This item can be reforged!", "§5§lEPIC SWORD"),
                lore.subList(lore.size() - 6, lore.size()));
    }

    /**
     * Made a dungeon item: its data's flag makes it one everywhere (the live converted Superior Dragon Leggings:
     * "LEGENDARY DUNGEON LEGGINGS", the gray brackets), and its stars count as a dungeon item's.
     */
    @Test
    void convertedToADungeonItem() {
        NBTTagCompound tag = data(SWORD);
        assertFalse(DungeonItems.is(SWORD, tag));
        assertTrue(DungeonItems.convertible(SWORD, tag));
        EssenceCost cost = (EssenceCost) SWORD.dungeonConversionCost().getCosts().getFirst();
        assertEquals(EssenceType.DRAGON, cost.getEssenceType());
        assertEquals(150, cost.getAmount());
        tag.setInt("upgrade_count", 5);
        assertEquals("&7Damage: &c+247.5", stats(SWORD, tag).get(0));

        DungeonItems.convert(tag);
        assertTrue(DungeonItems.is(SWORD, tag));
        assertFalse(DungeonItems.convertible(SWORD, tag));
        // At Catacombs 0 (no owner): +50% for the stars, +10% for the level.
        assertEquals("&7Damage: &c+247.5 &8(+360)", stats(SWORD, tag).get(0));
        List<String> lore = ItemBuilder.lore(SWORD, tag);
        assertEquals("§6§lLEGENDARY DUNGEON SWORD", lore.getLast());
        assertEquals(225 * 1.6, ItemStats.of(SWORD, tag, ItemBuilder.catacombsBoost(0)).get(Stat.DAMAGE), 1e-9);
        // A master star shows as one, and adds nothing outside Master Mode.
        tag.setInt("upgrade_count", 6);
        assertEquals(" &6✪✪✪✪✪&c➊", ItemBuilder.stars(SWORD, tag));
        assertEquals("&7Damage: &c+247.5 &8(+360)", stats(SWORD, tag).get(0));
        // The kind alone isn't one.
        assertFalse(DungeonItems.is(SWORD, null));
        assertEquals(Rarity.LEGENDARY, ItemBuilder.rarity(SWORD, tag));
    }

    /** One star at a time, in order, each for its own cost; the older dungeon-star count counts too. */
    @Test
    void starUpgrades() {
        assertEquals(10, Stars.max(CHESTPLATE));
        assertEquals(0, Stars.max(AXE));
        NBTTagCompound tag = data(CHESTPLATE);
        assertEquals(CHESTPLATE.upgradeCosts().getCosts().getFirst(), Stars.next(CHESTPLATE, tag));
        Stars.add(tag);
        assertEquals(1, tag.getInt("upgrade_count"));
        assertEquals(CHESTPLATE.upgradeCosts().getCosts().get(1), Stars.next(CHESTPLATE, tag));
        tag.setInt("upgrade_count", 10);
        assertNull(Stars.next(CHESTPLATE, tag));
        assertNull(Stars.next(AXE, data(AXE)));
        assertNull(Stars.cost(CHESTPLATE, 0));

        NBTTagCompound legacy = data(SWORD);
        legacy.setString("dungeon_star", "THREE");
        assertEquals(SWORD.upgradeCosts().getCosts().get(3), Stars.next(SWORD, legacy));
        Stars.add(legacy);
        assertEquals(4, ItemBuilder.starCount(legacy));
    }
}
