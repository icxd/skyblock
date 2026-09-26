package net.icxd.dungeons.item.items.none;

import net.icxd.dungeons.item.SkyBlockItem;
import org.bukkit.Material;

public class Mithril implements SkyBlockItem {
    @Override public Material material() { return Material.PRISMARINE_CRYSTALS; }
    @Override public String name() { return "Mithril"; }
    @Override public double npcSellPrice() { return 8; }
    @Override public String description() { return "%%gray%%%%italic%%\"The Man called it \"true-silver\" while the Dwarves, who loved it above all things, had their own, secret name for it.\""; }
    @Override public String id() { return "MITHRIL_ORE"; }
}
