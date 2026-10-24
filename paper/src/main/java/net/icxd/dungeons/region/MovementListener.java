package net.icxd.dungeons.region;

import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.quest.QuestBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Tracks which region each player is in, for the sidebar and tab list, says so when it changes, and
 * has the boss bar follow the quest at home there (see {@link Areas}).
 */
@OnlyOn({ServerType.LOBBY, ServerType.CRIMSON_ISLE, ServerType.DWARVEN_MINES})
public class MovementListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Regions are boxes of whole blocks; turning on the spot can't change it.
        if (!event.hasChangedBlock()) return;
        if (Areas.update(event.getPlayer(), event.getTo())) QuestBar.moved(event.getPlayer());
    }
}
