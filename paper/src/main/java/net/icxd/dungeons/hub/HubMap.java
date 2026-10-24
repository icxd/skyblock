package net.icxd.dungeons.hub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.island.Islands;
import net.icxd.dungeons.npc.Hologram;

/**
 * Hypixel's Hub on its captured map (config.yml's {@code server.map: hub}): players arrive at its
 * spawn, its two portals work, with their signs over them as recorded (2026-10-24): the end portal
 * north of spawn, "Travel to: / Your Island" (sends them to their island), and the nether portal
 * south-west of spawn, "Travel to: / Dungeon Hub". Without the map (a flat world) none of this is
 * there.
 */
@OnlyOn({ServerType.LOBBY})
public final class HubMap implements Listener {
    public static final String NAME = "hub";
    /** The bottom line of each portal's sign; the line above is 0.375 higher. */
    static final double[] ISLAND_SIGN = {0.5, 69.0625, -27.5};
    static final double[] DUNGEON_HUB_SIGN = {-33.5, 71.75, 26.5};
    /** The Dungeon Hub portal's frame of portal blocks (x -36 to -32, y 72 to 79, at z 27), and a block around it. */
    static final int[] DUNGEON_HUB_PORTAL = {-37, 71, 26, -31, 80, 28};
    /** After a portal sends a player, it doesn't again for this long (the move takes a moment). */
    private static final long PORTAL_PAUSE_MILLIS = 3000;

    private final Map<UUID, Long> sentAt = new HashMap<>();

    /** Whether this server's main world is the Hub's map. */
    public static boolean here() {
        return Hub.hostedOn(Dungeons.getSkyBlockServer().getServerType()) && Dungeons.getSkyBlockServer().hasMap(NAME);
    }

    /** The portals' signs, their chunks kept loaded (the signs would go with them). */
    public HubMap(Plugin plugin) {
        if (!here()) return;
        World world = Dungeons.getSkyBlockServer().getMainWorld();
        world.setSpawnLocation(Hub.spawn());
        for (double[] sign : List.of(ISLAND_SIGN, DUNGEON_HUB_SIGN)) {
            Location at = new Location(world, sign[0], sign[1], sign[2]);
            at.getChunk().addPluginChunkTicket(plugin);
        }
        Hologram.spawn(new Location(world, ISLAND_SIGN[0], ISLAND_SIGN[1], ISLAND_SIGN[2]), List.of("&bTravel to:", "&aYour Island"));
        Hologram.spawn(new Location(world, DUNGEON_HUB_SIGN[0], DUNGEON_HUB_SIGN[1], DUNGEON_HUB_SIGN[2]), List.of("&bTravel to:", "&aDungeon Hub"));
    }

    /** Arriving on the Hub (from anywhere): at its spawn, as Hypixel puts players (recorded twice). */
    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        if (here()) event.setSpawnLocation(Hub.spawn());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock() || !here()) return;
        Player player = event.getPlayer();
        Block feet = event.getTo().getBlock();
        if (!feet.getWorld().equals(Dungeons.getSkyBlockServer().getMainWorld())) return;
        boolean island = feet.getType() == Material.END_PORTAL;
        boolean dungeonHub = !island && inDungeonHubPortal(feet) && (feet.getType() == Material.NETHER_PORTAL
                || feet.getRelative(0, 1, 0).getType() == Material.NETHER_PORTAL);
        if (!island && !dungeonHub) return;
        long now = System.currentTimeMillis();
        Long last = sentAt.get(player.getUniqueId());
        if (last != null && now - last < PORTAL_PAUSE_MILLIS) return;
        sentAt.put(player.getUniqueId(), now);
        if (island) Islands.send(player);
        else Dungeons.getProxyLink().send(player, ServerType.DUNGEON_HUB.name());
    }

    static boolean inDungeonHubPortal(Block block) {
        int[] box = DUNGEON_HUB_PORTAL;
        return block.getX() >= box[0] && block.getX() <= box[3] && block.getY() >= box[1] && block.getY() <= box[4]
                && block.getZ() >= box[2] && block.getZ() <= box[5];
    }

    /** Vanilla's portals go nowhere: the Hub's own do (onMove). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortal(PlayerPortalEvent event) {
        if (here() && event.getFrom().getWorld().equals(Dungeons.getSkyBlockServer().getMainWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sentAt.remove(event.getPlayer().getUniqueId());
    }
}
