package net.icxd.dungeons.hub;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;

/**
 * Hypixel's Dungeon Hub on its captured map (config.yml's {@code server.map: dungeon_hub}): players
 * arrive where Hypixel put them (recorded 2026-10-24: -28.5 121 0.5, facing west to Mort's gate), and
 * Ophelia stands where she stood then (see Ophelia). Without the map (a flat world) none of this is
 * there.
 */
@OnlyOn({ServerType.DUNGEON_HUB})
public final class DungeonHubMap implements Listener {
    public static final String NAME = "dungeon_hub";
    static final double[] SPAWN = {-28.5, 121, 0.5};
    static final float SPAWN_YAW = 90;
    /** Ophelia, as recorded: -49.5 119 -9.5, facing southeast. */
    static final double[] OPHELIA = {-49.5, 119, -9.5};
    static final float OPHELIA_YAW = -45;

    /** Whether this server's main world is the Dungeon Hub's map. */
    public static boolean here() {
        return Dungeons.getSkyBlockServer().getServerType() == ServerType.DUNGEON_HUB && Dungeons.getSkyBlockServer().hasMap(NAME);
    }

    public DungeonHubMap() {
        if (here()) Dungeons.getSkyBlockServer().getMainWorld().setSpawnLocation(spawn(Dungeons.getSkyBlockServer().getMainWorld()));
    }

    static Location spawn(World world) {
        return new Location(world, SPAWN[0], SPAWN[1], SPAWN[2], SPAWN_YAW, 0);
    }

    /** Where Ophelia stands on the map. */
    public static Location ophelia(World world) {
        return new Location(world, OPHELIA[0], OPHELIA[1], OPHELIA[2], OPHELIA_YAW, 0);
    }

    /** Arriving (from anywhere): where Hypixel puts players. */
    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        if (here()) event.setSpawnLocation(spawn(Dungeons.getSkyBlockServer().getMainWorld()));
    }
}
