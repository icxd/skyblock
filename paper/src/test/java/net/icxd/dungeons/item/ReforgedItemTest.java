package net.icxd.dungeons.item;

import com.google.gson.JsonParser;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.reforge.ReforgeTable;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A reforged item's name, lore and stats (live items: "&9(+N)" after each stat it adds, and its bonus last), on
 * made-up items and a made-up reforge table.
 */
class ReforgedItemTest {
    private static final DataItem SWORD = item("""
            "TEST_REFORGE_SWORD":{"lore":["&7A sword for tests."],"material":"IRON_SWORD","name":"Test Sword","rarity":"EPIC",\
            "reforgeable":true,"stats":{"DAMAGE":100,"STRENGTH":20},"type":"SWORD"}""");
    private static final DataItem HELMET = item("""
            "TEST_WISE_HELMET":{"material":"IRON_HELMET","name":"Wise Test Helmet","rarity":"LEGENDARY","reforgeable":true,\
            "stats":{"DEFENSE":10},"type":"HELMET"}""");

    @BeforeEach
    void table() {
        List<String> problems = new ArrayList<>();
        ReforgeTable.set(ReforgeTable.read(JsonParser.parseString("""
                {"reforges": {
                  "keen": {"name": "Keen", "stats": {"EPIC": {"STRENGTH": 10, "CRIT_DAMAGE": 5}, "LEGENDARY": {"STRENGTH": 15},
                                                     "MYTHIC": {"STRENGTH": 17}},
                           "bonus": {"EPIC": ["&7Grants &a+1 &c❁ Strength &7per", "&cTest &7level."]},
                           "per_catacombs_level": {"STRENGTH": 1}},
                  "wise": {"name": "Wise", "stats": {"LEGENDARY": {"HEALTH": 3}}},
                  "digger": {"name": "Digger", "stats": {"EPIC": {"BREAKING_POWER": 1, "MINING_SPEED": 4}}}},
                 "prefixes": [{"reforge": "wise", "name": "Wise Test", "prefix": "Very"}]}"""), problems));
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

    private static NBTTagCompound reforged(SkyBlockItem item, String reforge) {
        NBTTagCompound tag = ItemBuilder.newData(item);
        tag.setString("reforge", reforge);
        return tag;
    }

    /** The name, the reforge's brackets and its bonus last (for no owner: no Catacombs level); no "can be reforged". */
    @Test
    void lore() {
        NBTTagCompound tag = reforged(SWORD, "keen");
        assertEquals("§5Keen Test Sword", ItemBuilder.name(SWORD, tag));
        assertEquals(List.of(
                "&7Damage: &c+100",
                "&7Strength: &c+30 &9(+10)",
                "&7Crit Damage: &9+5% &9(+5%)",
                "",
                "&7A sword for tests.",
                "",
                "&9Keen Bonus",
                "&7Grants &a+1 &c❁ Strength &7per",
                "&cTest &7level.",
                "",
                "§5§lEPIC SWORD"), ItemBuilder.lore(SWORD, tag));
    }

    /** As the items have it from before the table: the old enum's name. */
    @Test
    void anOldItemsReforge() {
        assertEquals(ItemBuilder.lore(SWORD, reforged(SWORD, "keen")), ItemBuilder.lore(SWORD, reforged(SWORD, "KEEN")));
    }

    /** Recombobulated to Legendary: Legendary's numbers, and the bonus Epic has (the nearest below). */
    @Test
    void recombobulated() {
        NBTTagCompound tag = reforged(SWORD, "keen");
        tag.setBoolean("recombobulated", true);
        List<String> lore = ItemBuilder.lore(SWORD, tag);
        assertEquals("&7Strength: &c+35 &9(+15)", lore.get(1));
        assertEquals("&9Keen Bonus", lore.get(5));
    }

    /** One the table doesn't know keeps its name and adds nothing; nothing throws. */
    @Test
    void anUnknownReforge() {
        NBTTagCompound tag = reforged(SWORD, "not_a_reforge");
        assertEquals("§5Not A Reforge Test Sword", ItemBuilder.name(SWORD, tag));
        List<String> lore = ItemBuilder.lore(SWORD, tag);
        assertEquals("&7Strength: &c+20", lore.get(1));
        assertFalse(lore.contains("&8This item can be reforged!"));
        assertEquals(20, ItemStats.of(SWORD, tag, null).get(Stat.STRENGTH));
    }

    /** Its Breaking Power is in the line under the name (live Scraped Gemstone Gauntlets: 8 and 1 show as 9), with no bracket. */
    @Test
    void breakingPower() {
        DataItem pickaxe = item("""
                "TEST_PICKAXE":{"material":"IRON_PICKAXE","name":"Test Pickaxe","rarity":"EPIC","reforgeable":true,\
                "stats":{"BREAKING_POWER":8,"MINING_SPEED":100},"type":"PICKAXE"}""");
        List<String> lore = ItemBuilder.lore(pickaxe, reforged(pickaxe, "digger"));
        assertEquals(List.of("&8Breaking Power 9", "", "&7Mining Speed: &6+104 &9(+4)"), lore.subList(0, 3));
        assertEquals("&8Breaking Power 8", ItemBuilder.lore(pickaxe, ItemBuilder.newData(pickaxe)).getFirst());
    }

    /** The reforge's word isn't doubled on an item named with it ("Very Wise Dragon"). */
    @Test
    void anItemNamedLikeItsReforge() {
        assertEquals("§6Very Wise Test Helmet", ItemBuilder.name(HELMET, reforged(HELMET, "wise")));
    }

    /** The stats count what the lore shows, and a stat a Catacombs level counts the wearer's. */
    @Test
    void stats() {
        NBTTagCompound tag = reforged(SWORD, "keen");
        assertEquals(30, ItemStats.of(SWORD, tag, null).get(Stat.STRENGTH));
        assertEquals(5, ItemStats.of(SWORD, tag, null).get(Stat.CRIT_DAMAGE));
        assertEquals(42, ItemStats.of(SWORD, tag, null, 12, 0).get(Stat.STRENGTH));
        // Very Special gives Mythic's.
        assertEquals(17, ReforgeTable.get().reforge("keen").stat(Stat.STRENGTH, Rarity.VERY_SPECIAL, 0));
    }

    /** No reforge: nothing changes (GoldenItemsTest has every real item so). */
    @Test
    void noReforge() {
        NBTTagCompound tag = ItemBuilder.newData(SWORD);
        assertEquals("§5Test Sword", ItemBuilder.name(SWORD, tag));
        List<String> lore = ItemBuilder.lore(SWORD, tag);
        assertEquals("&8This item can be reforged!", lore.get(lore.size() - 2));
    }
}
