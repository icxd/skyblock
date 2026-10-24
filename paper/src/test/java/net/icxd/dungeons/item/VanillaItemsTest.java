package net.icxd.dungeons.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/** The plain item of a material, among Hypixel's items that share it (as items.json has them). */
class VanillaItemsTest {
    private static SkyBlockItem item(String id, Material material, double sellPrice, boolean glowing, List<String> lore) {
        return new SkyBlockItem() {
            @Override public String id() { return id; }
            @Override public String name() { return id; }
            @Override public Material material() { return material; }
            @Override public double npcSellPrice() { return sellPrice; }
            @Override public boolean glowing() { return glowing; }
            @Override public List<String> lore() { return lore; }
        };
    }

    @Test
    void thePlainOne() {
        Map<Material, String> ids = VanillaItems.build(List.of(
                item("ENCHANTED_OAK_LOG", Material.OAK_LOG, 320, true, List.of()),
                item("TAVERN_BARN_SKIN", Material.OAK_LOG, 0, false, List.of("&7Consume this item to unlock the")),
                item("LOG", Material.OAK_LOG, 2, false, List.of()),
                item("MIRRORED_WOOD", Material.OAK_PLANKS, 0, false, List.of()),
                item("WOOD", Material.OAK_PLANKS, 0.5, false, List.of()),
                item("PINWHEEL_HOUSE_BARN_SKIN", Material.OAK_PLANKS, 0, false, List.of("&7Consume this item")),
                item("WORKBENCH", Material.CRAFTING_TABLE, 1, false, List.of())));
        assertEquals("LOG", ids.get(Material.OAK_LOG));
        assertEquals("WOOD", ids.get(Material.OAK_PLANKS));
        assertEquals("WORKBENCH", ids.get(Material.CRAFTING_TABLE));
        assertEquals(null, ids.get(Material.STONE));
    }
}
