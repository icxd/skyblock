package net.icxd.dungeons.hub;

import java.util.Collection;

import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;

/**
 * The armor stands of a captured map (the Hub's, the Dungeon Hub's) stay where they are, as on
 * Hypixel. Captures made before the dungeon scanner saved them without gravity (those of
 * 2026-10-24) still have it, and a stand with nothing under it falls out of the world (7 of the
 * Hub's 813). Each one's gravity goes as its chunk's entities load, and those already loaded when
 * the plugin starts are done then. Main thread.
 */
@OnlyOn({ServerType.LOBBY, ServerType.DUNGEON_HUB})
public final class MapStands implements Listener {
    public MapStands() {
        if (here()) keepStill(Dungeons.getSkyBlockServer().getMainWorld().getEntities());
    }

    private static boolean here() {
        return HubMap.here() || DungeonHubMap.here();
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        World world = event.getWorld();
        if (here() && world.equals(Dungeons.getSkyBlockServer().getMainWorld())) keepStill(event.getEntities());
    }

    private static void keepStill(Collection<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (entity instanceof ArmorStand stand && stand.hasGravity()) stand.setGravity(false);
        }
    }
}
