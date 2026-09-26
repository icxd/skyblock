package net.icxd.dungeons.region;

import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.utils.Replacement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/** Tracks which region each player is in, for the sidebar and tab list, and says so when it changes. */
@OnlyOn({ServerType.LOBBY, ServerType.CRIMSON_ISLE, ServerType.DWARVEN_MINES})
public class MovementListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Regions are boxes of whole blocks; turning on the spot can't change it.
        if (!event.hasChangedBlock()) return;
        PlayerSession session = PlayerSession.of(event.getPlayer());
        RegionType type = RegionType.getRegionType(event.getTo());
        RegionType was = session.getRegion() != null ? session.getRegion() : RegionType.getRegionType(event.getFrom());
        session.setRegion(type);
        if (was != type) session.setDefenseReplacement(Replacement.forMillis("&7⏣ &7" + type.displayName(), 1000));
    }
}
