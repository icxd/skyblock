package net.icxd.dungeons.region;

import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.utils.Replacement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = event.getTo();
        RegionType type = RegionType.getRegionType(location);
        if (type == null) return;
        Region region = Region.regionCache.getOrDefault(player.getUniqueId(),
                new Region(RegionType.getRegionType(player.getLocation())));
        if (region.getType() != type) {
            region.setType(type);
            long cms = System.currentTimeMillis();
            StatsRunnable.DEFENSE_REPLACEMENT_MAP.put(player.getUniqueId(), new Replacement() {
                @Override
                public String getReplacement() {
                    return "&7⏣ &7" + Region.regionCache.getOrDefault(player.getUniqueId(),
                            new Region(RegionType.getRegionType(player.getLocation()))).toString();
                }

                @Override
                public long getEnd() {
                    return cms + 1000;
                }
            });
        }
        Region.regionCache.put(player.getUniqueId(), region);
    }
}
