package net.icxd.dungeons.mining.blocks;

import net.icxd.dungeons.dwarven.PowderType;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.mining.MinableBlock;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.utils.Tuple;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class MithrilBlock {
    private static final int MIN_BREAKING_POWER = 4;

    /**
     * Gray Wool: strength 500, "1 Mithril" and "1 Mithril Powder" (the wiki's Mithril and Mithril Powder), and Mining
     * XP 45, Mithril's "Base Mining XP" (the wiki's Dwarven Metals list, a block's column: per block broken, not per
     * Mithril its fortune drops). Compact's enchanted item from it is Enchanted Mithril, the wiki's "next_material"
     * of Mithril (UNKNOWN whether Hypixel gives that one).
     */
    public static class GrayMithrilBlock implements MinableBlock {
        @Override public Material material() { return Material.GRAY_WOOL; }
        @Override public String name() { return "Mithril"; }
        @Override public int minBreakingPower() { return MIN_BREAKING_POWER; }
        @Override public int blockStrength() { return 500; }
        @Override public int instaBreakStrength() { return 30000; }
        @Override public Stat fortune() { return Stat.DWARVEN_METAL_FORTUNE; }
        @Override public ArrayList<Tuple<SkyBlockItem, Integer>> drops() { return new ArrayList<>(List.of(new Tuple<>(ItemRegistry.get("MITHRIL_ORE"), 1))); }
        @Override public double miningXp() { return 45; }
        @Override public int powder() { return 1; }
        @Override public PowderType powderType() { return PowderType.MITHRIL; }
        @Override public String compactDrop() { return "ENCHANTED_MITHRIL"; }
    }
}
