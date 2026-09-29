package net.icxd.dungeons.mining;

import net.icxd.dungeons.dwarven.PowderType;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.utils.Tuple;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public interface MinableBlock {
    Material material();
    int minBreakingPower();
    int blockStrength();

    default int regenTime() { return 20*5; }
    default int instaBreakStrength() { return -1; } // -1 = no insta break
    default Material blockWhenBroken() { return Material.BEDROCK; }
    default ArrayList<Tuple<SkyBlockItem, Integer>> drops() { return null; }
    /** What it's called in chat ("to mine Mithril"). */
    default String name() { return "this block"; }

    /**
     * The fortune stat that's added to Mining Fortune on this block (Dwarven Metal Fortune on Mithril and
     * the other Dwarven Metals: the wiki's Dwarven Metal Fortune and Mining Fortune); null for none.
     */
    default Stat fortune() { return null; }

    /** The Mining XP each one broken gives, before Mining Wisdom (the wiki's Dwarven Metals list: "Base Mining XP"); 0 for none. */
    default double miningXp() { return 0; }

    /** The powder each one broken gives ({@link #powderType}), before anything raises it; 0 for none. */
    default int powder() { return 0; }
    default PowderType powderType() { return null; }

    /** What Compact's "chance to drop an enchanted item" gives from it, by item id; null for nothing. */
    default String compactDrop() { return null; }

    default void onBreak(Block block, Player player) {}

    default void place(Location location) {
        location.getBlock().setType(material());
    }

}
