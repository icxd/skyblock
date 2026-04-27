package net.icxd.dungeons.mining.blocks;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.mining.MinableBlock;
import net.icxd.dungeons.utils.Tuple;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class MithrilBlock {
    private static final int MIN_BREAKING_POWER = 4;

    public static class GrayMithrilBlock implements MinableBlock {
        @Override public Material material() { return Material.WOOL; }
        @Override public int data() { return 7; }
        @Override public int minBreakingPower() { return MIN_BREAKING_POWER; }
        @Override public int blockStrength() { return 500; }
        @Override public int instaBreakStrength() { return 30000; }
        @Override public ArrayList<Tuple<SkyBlockItem, Integer>> drops() { return new ArrayList<>(List.of(new Tuple<>(ItemRegistry.get("MITHRIL_ORE"), 1))); }
    }
}
