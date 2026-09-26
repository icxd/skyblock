package net.icxd.dungeons.item.items.none;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import org.bukkit.Material;

/** This plugin's own item (Hypixel has none by this name). */
public class PerfectHopper implements SkyBlockItem {
    @Override public String id() { return "PERFECT_HOPPER"; }
    @Override public String name() { return "Perfect Hopper"; }
    @Override public Material material() { return Material.HOPPER; }
    @Override public Rarity rarity() { return Rarity.EPIC; }
    @Override public boolean glowing() { return true; }
    @Override public double npcSellPrice() { return 500000; }
}
