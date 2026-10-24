package net.icxd.dungeons.island;

import org.bukkit.World;
import org.bukkit.entity.Player;

/** Something every island has or does (Jerry, the portal's sign, the welcome): told as islands load, unload and get visited. Main thread. */
public interface IslandFeature {
    /** An island's world has loaded (its chunks load as players come near). */
    default void loaded(World world) {
    }

    /** It's about to unload: what was spawned for it goes. */
    default void unloading(World world) {
    }

    /** A player has arrived on their island (sent there, or joined the server there). */
    default void arrived(Player player, World world) {
    }
}
