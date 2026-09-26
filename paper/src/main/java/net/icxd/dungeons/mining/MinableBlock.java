package net.icxd.dungeons.mining;

import net.icxd.dungeons.item.SkyBlockItem;
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

    default void onBreak(Block block, Player player) {}

    default void place(Location location) {
        location.getBlock().setType(material());
    }

}
