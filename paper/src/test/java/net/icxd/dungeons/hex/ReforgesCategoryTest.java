package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.reforge.ReforgeTable;

/** The Reforges category: which items it's for and its summary (the official screenshot's "Reforge ✔ / Fabled"). */
class ReforgesCategoryTest {
    @AfterEach
    void noTable() {
        ReforgeTable.set(null);
    }

    /** Without its table, the items that say they can be reforged: weapons, armor, tools, equipment. */
    @Test
    void appliesToReforgeableItems() {
        assertTrue(HexCategories.REFORGES.applies(HexFakes.sword()));
        assertFalse(HexCategories.REFORGES.applies(HexFakes.hexItem(HexFakes.item("TEST_RING", "Test Ring", Rarity.EPIC,
                SpecificItemType.ACCESSORY))));
    }

    @Test
    void summary() {
        HexItem sword = HexFakes.sword();
        assertEquals(List.of("  &7Reforge &c✖"), HexCategories.REFORGES.summary(sword));
        sword.tag().setString("reforge", "FABLED");
        assertEquals(List.of("  &7Reforge &a✔", "    &9Fabled"), HexCategories.REFORGES.summary(sword));
    }

    @Test
    void button() {
        assertEquals("Reforges", HexCategories.REFORGES.name());
        assertEquals(0, HexCategories.REFORGES.requiredCarpentry());
        // The Luxurious Spool's head, from the item data (none here).
        assertEquals(Material.PLAYER_HEAD, HexCategories.REFORGES.look().material());
    }
}
