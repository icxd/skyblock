package net.icxd.dungeons.hex.enchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.hex.HexCategories;
import net.icxd.dungeons.hex.HexFakes;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.item.enchanting.FakeEnchantments;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;

/** The Enchantments and Ultimate Enchantments categories: which items they're for, and their summary lines. */
class EnchantCategoriesTest {
    @AfterEach
    void noTable() {
        FakeEnchantments.reset();
    }

    private static HexItem item(SpecificItemType type, String... enchantments) {
        HexItem item = HexFakes.hexItem(HexFakes.item("TEST_" + type, "Test " + type, Rarity.EPIC, type));
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < enchantments.length; i += 2) {
            NBTTagCompound e = new NBTTagCompound();
            e.setString("name", enchantments[i]);
            e.setInt("lvl", Integer.parseInt(enchantments[i + 1]));
            list.add(e);
        }
        item.tag().set("enchantments", list);
        return item;
    }

    @Test
    void whatTheyreFor() {
        FakeEnchantments.use();
        HexItem sword = item(SpecificItemType.SWORD);
        assertTrue(HexCategories.ENCHANTMENTS.applies(sword));
        assertTrue(HexCategories.ULTIMATE_ENCHANTMENTS.applies(sword));
        // Only ultimates go on a wand (made-up table: Ultimate Wise).
        assertFalse(HexCategories.ENCHANTMENTS.applies(item(SpecificItemType.WAND)));
        assertTrue(HexCategories.ULTIMATE_ENCHANTMENTS.applies(item(SpecificItemType.WAND)));
        assertFalse(HexCategories.ENCHANTMENTS.applies(item(SpecificItemType.ACCESSORY)));
        assertFalse(HexCategories.ULTIMATE_ENCHANTMENTS.applies(item(SpecificItemType.ACCESSORY)));
    }

    @Test
    void withoutTheTableNothing() {
        assertFalse(HexCategories.ENCHANTMENTS.applies(item(SpecificItemType.SWORD)));
        assertFalse(HexCategories.ULTIMATE_ENCHANTMENTS.applies(item(SpecificItemType.SWORD)));
    }

    @Test
    void summaries() {
        FakeEnchantments.use();
        HexItem sword = item(SpecificItemType.SWORD, "sharpness", "5", "ultimate_wise", "2");
        assertEquals(List.of("  &7Enchantments &e1&7/&a5"), HexCategories.ENCHANTMENTS.summary(sword));
        assertEquals(List.of("  &7Ultimate Enchantments &a1&7/&a1"), HexCategories.ULTIMATE_ENCHANTMENTS.summary(sword));
    }
}
