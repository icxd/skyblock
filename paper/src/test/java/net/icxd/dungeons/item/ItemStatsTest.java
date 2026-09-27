package net.icxd.dungeons.item;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stats combat uses from an item: what its lore says, and in a dungeon what the lore's dark gray
 * brackets say (research damage.md 1.8), on made-up items.
 */
class ItemStatsTest {
    private static final String STAR = "[{\"amount\":10,\"essence\":\"WITHER\"}]";
    /** Like the recorded Giant's Sword: 500 Damage, and Crit Chance to show it only gets the stars' boost. */
    private static final DataItem SWORD = item("""
            "TEST_GIANT_SWORD":{"dungeon_item":true,"material":"IRON_SWORD","name":"Test Giant Sword","rarity":"LEGENDARY",\
            "stats":{"DAMAGE":500,"CRIT_CHANCE":10,"HEALTH_REGEN":5},"type":"SWORD","upgrade_costs":[@,@,@,@,@]}""".replace("@", STAR));
    private static final DataItem PLAIN = item("""
            "TEST_PLAIN_SWORD":{"material":"IRON_SWORD","name":"Test Plain Sword","rarity":"RARE","stats":{"DAMAGE":100},"type":"SWORD"}""");

    private static DataItem item(String json) {
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{" + json + "}}"));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Five stars, 15 hot potato books and Critical VI. */
    private static NBTTagCompound upgraded(SkyBlockItem item) {
        NBTTagCompound tag = ItemBuilder.newData(item);
        tag.remove("attribute_1");
        tag.remove("attribute_2");
        tag.setInt("upgrade_count", 5);
        tag.setInt("hot_potato_books", 15);
        NBTTagList list = new NBTTagList();
        NBTTagCompound critical = new NBTTagCompound();
        critical.setString("name", "critical");
        critical.setInt("lvl", 6);
        list.add(critical);
        tag.set("enchantments", list);
        return tag;
    }

    @Test
    void criticalAndOverloadGrantStats() {
        assertEquals(70, EnchantmentType.CRITICAL.getStats(6).get(Stat.CRIT_DAMAGE), 1e-9);
        assertEquals(100, EnchantmentType.CRITICAL.getStats(7).get(Stat.CRIT_DAMAGE), 1e-9);
        Stats overload = EnchantmentType.OVERLOAD.getStats(5);
        assertEquals(5, overload.get(Stat.CRIT_DAMAGE), 1e-9);
        assertEquals(5, overload.get(Stat.CRIT_CHANCE), 1e-9);
        // "Increases melee damage dealt by 30%" is a damage buff, not a stat.
        assertEquals(new Stats(), EnchantmentType.SHARPNESS.getStats(5));
        assertEquals(75, EnchantmentType.GROWTH.getStats(5).get(Stat.HEALTH), 1e-9);
    }

    /** Out of a dungeon: each star adds 2% of the item's own stats. */
    @Test
    void outsideADungeon() {
        Stats stats = ItemStats.of(SWORD, upgraded(SWORD), null);
        assertEquals(500 + 30 + 50, stats.get(Stat.DAMAGE), 1e-9);
        assertEquals(30, stats.get(Stat.STRENGTH), 1e-9);
        assertEquals(70, stats.get(Stat.CRIT_DAMAGE), 1e-9);
        assertEquals(11, stats.get(Stat.CRIT_CHANCE), 1e-9);
    }

    /**
     * In one, at Catacombs 21 (+195%) with 5 stars (+50%): the recorded sword's factor was 3.48, which is
     * this 3.45 and the General's Medallion's 3% (no accessories here yet).
     */
    @Test
    void inADungeon() {
        Stats stats = ItemStats.of(SWORD, upgraded(SWORD), 1.95);
        assertEquals((500 + 30) * 3.45, stats.get(Stat.DAMAGE), 1e-9);
        assertEquals(30 * 3.45, stats.get(Stat.STRENGTH), 1e-9);
        assertEquals(70 * 3.45, stats.get(Stat.CRIT_DAMAGE), 1e-9);
        // Crit Chance: the stars' 50% only; Health Regen: nothing.
        assertEquals(15, stats.get(Stat.CRIT_CHANCE), 1e-9);
        assertEquals(5, stats.get(Stat.HEALTH_REGEN), 1e-9);
        // Only dungeon items.
        assertEquals(100, ItemStats.of(PLAIN, ItemBuilder.newData(PLAIN), 1.95).get(Stat.DAMAGE), 1e-9);
    }

    /** What combat uses in a dungeon is what the lore's brackets show (at Catacombs 0 here: no owner). */
    @Test
    void sameAsTheLore() {
        NBTTagCompound tag = upgraded(SWORD);
        Stats stats = ItemStats.of(SWORD, tag, ItemBuilder.catacombsBoost(0));
        List<String> lines = ItemBuilder.statLines(SWORD, tag, Rarity.LEGENDARY, null);
        assertTrue(lines.contains("&7Damage: &c+580 &e(+30) &8(+" + net.icxd.dungeons.utils.Text.number(stats.get(Stat.DAMAGE)) + ")"), lines.toString());
        assertTrue(lines.contains("&7Crit Damage: &9+70% &8(+" + net.icxd.dungeons.utils.Text.number(stats.get(Stat.CRIT_DAMAGE)) + "%)"), lines.toString());
    }
}
