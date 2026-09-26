package net.icxd.dungeons.item.items.none;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.SpecificItemType;
import org.bukkit.Material;

import java.util.List;

public class Mithril implements SkyBlockItem {
    @Override public String id() { return "MITHRIL_ORE"; }
    @Override public String name() { return "Mithril"; }
    @Override public Material material() { return Material.PRISMARINE_CRYSTALS; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.DWARVEN_METAL; }
    @Override public List<String> categories() { return List.of("Brewing Ingredient", "Collection Item"); }
    @Override public List<String> lore() {
        return List.of("&7&o\"The Man called it \"true-silver\" while", "&7&othe Dwarves, who loved it above all",
                "&7&othings, had their own, secret name", "&7&ofor it.\"");
    }
    @Override public double npcSellPrice() { return 8; }
}
