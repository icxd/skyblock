package net.icxd.dungeons.item.enchanting.weapon;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** What weapon enchantments do to stats and to their abilities' mana (on a made-up table and made-up items). */
class WeaponStatsTest {
    @BeforeEach
    void table() {
        WeaponTexts.use();
    }

    @AfterEach
    void reset() {
        WeaponTexts.reset();
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

    static NBTTagCompound enchanted(String id, String... enchantments) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < enchantments.length; i += 2) {
            NBTTagCompound e = new NBTTagCompound();
            e.setString("name", enchantments[i]);
            e.setInt("lvl", Integer.parseInt(enchantments[i + 1]));
            list.add(e);
        }
        tag.set("enchantments", list);
        return tag;
    }

    /** Tabasco's "+2 weapon damage" and Toxophilite's Crit Chance come with the stats their text grants (lore and stats alike). */
    @Test
    void statsTheTextsGrant() {
        assertEquals(2, EnchantmentType.getByNamespace("tabasco").getStats(2).get(Stat.DAMAGE), 1e-9);
        assertEquals(3.7, EnchantmentType.getByNamespace("toxophilite").getStats(1).get(Stat.CRIT_CHANCE), 1e-9);
        // Nothing for an enchantment that isn't one of these.
        assertEquals(new Stats(), WeaponStats.listed("cleave", "&7Grants &f+2 &7weapon damage.", new Stats()));
    }

    /** Ultimate Jerry's base damage counts in the stats, not the lore (a live one shows its own Damage), on an Aspect of the Jerry. */
    @Test
    void ultimateJerry() {
        DataItem jerry = item("""
                "TEST_JERRY":{"material":"WOODEN_SWORD","name":"Aspect of the Jerry","rarity":"COMMON","stats":{"DAMAGE":1},"type":"SWORD"}""");
        DataItem other = item("""
                "TEST_OTHER":{"material":"WOODEN_SWORD","name":"Other Sword","rarity":"COMMON","stats":{"DAMAGE":1},"type":"SWORD"}""");
        assertEquals(11, ItemStats.of(jerry, enchanted("TEST_JERRY", "ultimate_jerry", "1"), null).get(Stat.DAMAGE), 1e-9);
        assertEquals(1, ItemStats.of(other, enchanted("TEST_OTHER", "jerry", "1"), null).get(Stat.DAMAGE), 1e-9);
    }

    /** Ultimate Wise V: half the mana, in the cost and in the lore's Mana Cost lines (a live Aspect of the Void's 45 shows 23). */
    @Test
    void ultimateWise() {
        assertEquals(0.5, WeaponStats.wise(5), 1e-9);
        assertEquals(1, WeaponStats.wise(0), 1e-9);
        ItemBlock transmission = new ItemBlock("ABILITY", "Instant Transmission", "&6Ability: Instant Transmission", "RIGHT_CLICK", List.of(),
                45, 0, 0, 0, 0, 0, 0);
        ItemBlock bonus = new ItemBlock("FULL_SET", "Set", "&6Full Set Bonus: Set", null, List.of(), 0, 0, 0, 0, 0, 0, 4);
        List<ItemBlock> blocks = List.of(transmission, bonus);
        List<ItemBlock> shown = WeaponStats.withWise(enchanted("TEST", "ultimate_wise", "5"), blocks);
        assertEquals(23, shown.get(0).mana());
        assertSame(bonus, shown.get(1));
        assertSame(blocks, WeaponStats.withWise(enchanted("TEST", "sharpness", "5"), blocks));
    }
}
