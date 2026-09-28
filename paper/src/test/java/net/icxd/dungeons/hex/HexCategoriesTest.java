package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.utils.Utils;

/** The seven categories: their order, requirements and buttons as the wiki has them. */
class HexCategoriesTest {
    @Test
    void order() {
        assertEquals(List.of("Enchantments", "Ultimate Enchantments", "Books", "Modifiers", "Reforges", "Item Upgrades", "Gemstones"),
                HexCategories.all().stream().map(HexCategory::name).toList());
    }

    @Test
    void carpentry() {
        // The wiki's The Hex: Books and Modifiers need Carpentry 20, Item Upgrades and Gemstones 25, the rest nothing.
        assertEquals(List.of(0, 0, 20, 20, 0, 25, 25), HexCategories.all().stream().map(HexCategory::requiredCarpentry).toList());
    }

    @Test
    void buttonsAsTheWikisWeaponTab() {
        assertEquals(List.of("&7This special &aEnchantment Table", "&7allows you to access way more", "&7Enchantments and gives you the",
                "&7option to consume &aBottles of", "&aEnchanting &7directly!"), HexCategories.ENCHANTMENTS.description());
        assertEquals(Material.ENCHANTING_TABLE, HexCategories.ENCHANTMENTS.look().material());
        assertEquals(List.of("&7Allows you to apply &d&lUltimate", "&d&lEnchantments &7and gives you the", "&7option to consume &aBottles of",
                "&aEnchanting &7directly!"), HexCategories.ULTIMATE_ENCHANTMENTS.description());
        assertEquals(Material.WRITABLE_BOOK, HexCategories.ULTIMATE_ENCHANTMENTS.look().material());
        assertEquals(List.of("&7Knowledge is &6power&7! Apply", "&7special books to your item to", "&7upgrade it!"),
                HexCategories.BOOKS.description());
        assertEquals(Material.BOOK, HexCategories.BOOKS.look().material());
        assertEquals(List.of("&7Apply miscellaneous item", "&7modifiers like the", "&6Recombobulator 3000&7,", "&5Wither Scrolls&7, and &cMaster",
                "&cStars&7!"), HexCategories.MODIFIERS.description());
        assertEquals(List.of("&7Apply &aReforges &7to your item", "&7with &aReforge Stones &7or by", "&7rolling a &brandom &7reforge."),
                HexCategories.REFORGES.description());
    }

    @Test
    void noneAppliesUntilItsPartIsBuilt() {
        HexItem sword = HexFakes.sword();
        for (HexCategory category : HexCategories.all()) assertFalse(category.applies(sword), category.name());
    }

    @Test
    void textureHashes() {
        String hash = "57ccd36dc8f72adcb1f8c8e61ee82cd96ead140cf2a16a1366be9b5a8e3cc3fc";
        assertEquals(hash, HexCategory.textureHash(Utils.texture(hash)));
        assertNull(HexCategory.textureHash("not base64!"));
    }
}
