package net.icxd.dungeons.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.gemstone.Gem;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.item.gemstone.GemstoneQuality;
import net.icxd.dungeons.item.gemstone.GemstoneTable;
import net.icxd.dungeons.item.gemstone.GemstoneTable.Stone;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.gemstone.PrivateGemstones;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Gems in an item's lore and stats, on a made-up item and table: the "&d(+N)" bracket in the live order (after the
 * potato books' and the reforge's, before the dungeon's), in the total and scaled in a dungeon.
 */
class ItemGemstonesTest {
    private static final String STAR = "[{\"amount\":10,\"essence\":\"WITHER\"}]";
    /** Like a Shadow Assassin Helmet: Legendary, a Jasper slot and Combat ones. */
    private static final DataItem HELMET = item("""
            "TEST_ASSASSIN_HELMET":{"dungeon_item":true,"gemstone_slots":[{"type":"JASPER"},{"type":"COMBAT"},\
            {"type":"COMBAT"}],"material":"LEATHER_HELMET","name":"Test Assassin Helmet","rarity":"LEGENDARY",\
            "stats":{"HEALTH":100,"STRENGTH":30,"CRIT_DAMAGE":10},\
            "type":"HELMET","upgrade_costs":[@,@,@,@,@]}""".replace("@", STAR));

    private static DataItem item(String json) {
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{" + json + "}}"));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterEach
    void noTable() {
        GemstoneTable.use(null);
    }

    /** Fine Jasper: 6 Strength at Legendary, 7 at Mythic; Fine Onyx 5% Crit Damage and Fine Ruby 10 Health at Legendary. */
    private static void table() {
        GemstoneTable.use(new GemstoneTable(Map.of(
                GemstoneType.JASPER, new Stone(Stat.STRENGTH, Map.of(GemstoneQuality.FINE, Map.of(Rarity.LEGENDARY, 6.0, Rarity.MYTHIC, 7.0))),
                GemstoneType.ONYX, new Stone(Stat.CRIT_DAMAGE, Map.of(GemstoneQuality.FINE, Map.of(Rarity.LEGENDARY, 5.0))),
                GemstoneType.RUBY, new Stone(Stat.HEALTH, Map.of(GemstoneQuality.FINE, Map.of(Rarity.LEGENDARY, 10.0)))),
                Map.of(), Map.of(), Map.of(), Map.of()));
    }

    private static NBTTagCompound gems() {
        NBTTagCompound tag = ItemBuilder.newData(HELMET);
        tag.remove("attribute_1");
        tag.remove("attribute_2");
        GemSlots.apply(tag, HELMET, 0, new Gem(GemstoneType.JASPER, GemstoneQuality.FINE));
        GemSlots.apply(tag, HELMET, 1, new Gem(GemstoneType.ONYX, GemstoneQuality.FINE));
        GemSlots.apply(tag, HELMET, 2, new Gem(GemstoneType.RUBY, GemstoneQuality.FINE));
        return tag;
    }

    @Test
    void theBracketAndTheLine() {
        table();
        NBTTagCompound tag = gems();
        tag.setInt("hot_potato_books", 2);
        List<String> lore = ItemBuilder.lore(HELMET, tag);
        // The books' first, then the gems'; in a dungeon (Catacombs 0) all of it +10%.
        assertTrue(lore.contains("&7Health: &c+118 &e(+8) &d(+10) &8(+129.8)"), lore.toString());
        assertTrue(lore.contains("&7Strength: &c+36 &d(+6) &8(+39.6)"), lore.toString());
        assertTrue(lore.contains("&7Crit Damage: &9+15% &d(+5%) &8(+16.5%)"), lore.toString());
        assertTrue(lore.contains("&7Gemstones: &9[&d❁&9] &9[&8⚔&9] &9[&c⚔&9]"), lore.toString());
    }

    @Test
    void theItemsStats() {
        table();
        NBTTagCompound tag = gems();
        Stats out = ItemStats.of(HELMET, tag, null);
        assertEquals(36, out.get(Stat.STRENGTH));
        assertEquals(15, out.get(Stat.CRIT_DAMAGE));
        // Scaled in a dungeon with the rest: the lore's dark gray bracket.
        assertEquals(36 * 1.1, ItemStats.of(HELMET, tag, 0.1).get(Stat.STRENGTH), 1e-9);
        // Recombobulated, Mythic's.
        tag.setBoolean("recombobulated", true);
        assertEquals(37, ItemStats.of(HELMET, tag, null).get(Stat.STRENGTH));
    }

    /**
     * The recordings' items (hex/gems_lore.txt), with the private item data and table: a recombobulated Shadow
     * Assassin Helmet's two Fine Jasper "&d(+14)", an Aspect of the Void's Fine Sapphire "&d(+10)".
     */
    @Test
    void recordedItems() throws IOException {
        Path items = GoldenItemsTest.itemsFile();
        assumeTrue(Files.exists(items), "no " + items);
        GemstoneTable.use(PrivateGemstones.table());
        Map<String, DataItem> all;
        try (Reader reader = Files.newBufferedReader(items)) {
            all = ItemData.load(reader).items();
        }
        Gem fineJasper = new Gem(GemstoneType.JASPER, GemstoneQuality.FINE);
        DataItem helmet = all.get("STARRED_SHADOW_ASSASSIN_HELMET");
        NBTTagCompound tag = ItemBuilder.newData(helmet);
        tag.setBoolean("recombobulated", true);
        GemSlots.apply(tag, helmet, 0, fineJasper);
        GemSlots.apply(tag, helmet, 1, fineJasper);
        assertEquals(14, GemSlots.stats(helmet, tag).get(Stat.STRENGTH));
        List<String> lore = ItemBuilder.lore(helmet, tag);
        assertTrue(lore.stream().anyMatch(l -> l.startsWith("&7Strength: ") && l.contains(" &d(+14) &8(")), lore.toString());
        assertTrue(lore.contains("&7Gemstones: &9[&d❁&9] &9[&d⚔&9]"), lore.toString());

        DataItem aotv = all.get("ASPECT_OF_THE_VOID");
        NBTTagCompound void_ = ItemBuilder.newData(aotv);
        GemSlots.apply(void_, aotv, 0, new Gem(GemstoneType.SAPPHIRE, GemstoneQuality.FINE));
        assertTrue(ItemBuilder.lore(aotv, void_).stream().anyMatch(l -> l.startsWith("&7Intelligence: ") && l.endsWith(" &d(+10)")));
    }

    @Test
    void withoutTheTableGemsShowButGiveNothing() {
        NBTTagCompound tag = gems();
        List<String> lore = ItemBuilder.lore(HELMET, tag);
        assertTrue(lore.contains("&7Strength: &c+30 &8(+33)"), lore.toString());
        assertTrue(lore.contains("&7Gemstones: &9[&d❁&9] &9[&8⚔&9] &9[&c⚔&9]"), lore.toString());
    }
}
