package net.icxd.dungeons.hub;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.ServerType;

/**
 * The Hub: the main world of LOBBY servers (and of NONE ones in development). Islands' portals and
 * {@code /hub} lead here: on a server that has the Hub, to its spawn; from anywhere else, through the
 * proxy to the emptiest hub server.
 */
public final class Hub {
    /** Hypixel's Hub spawn, facing north to the portal home (the arrival teleport: 0.5 77 -0.5, yaw 180). */
    public static final double[] SPAWN = {0.5, 77, -0.5};
    public static final float SPAWN_YAW = 180;

    private Hub() {
    }

    /** Whether this kind of server has the Hub in its main world. */
    public static boolean hostedOn(ServerType type) {
        return type == ServerType.LOBBY || type == ServerType.NONE;
    }

    /** Whether this world is the Hub. */
    public static boolean is(World world) {
        return hostedOn(Dungeons.getSkyBlockServer().getServerType()) && world.equals(Dungeons.getSkyBlockServer().getMainWorld());
    }

    /**
     * Where players arrive: Hypixel's spawn on the Hub's map (config.yml's {@code map: hub}, which
     * servermgr sets when it puts the map in), else the world's own spawn.
     */
    public static Location spawn() {
        World world = Dungeons.getSkyBlockServer().getMainWorld();
        if (!Dungeons.getSkyBlockServer().hasMap(HubMap.NAME)) return world.getSpawnLocation();
        return new Location(world, SPAWN[0], SPAWN[1], SPAWN[2], SPAWN_YAW, 0);
    }

    /** To the Hub: its spawn if this server has it, else the proxy sends them to a hub server. */
    public static void send(Player player) {
        if (hostedOn(Dungeons.getSkyBlockServer().getServerType())) {
            player.setFallDistance(0);
            player.teleport(spawn());
        } else {
            Dungeons.getProxyLink().send(player, ServerType.LOBBY.name());
        }
    }
}
