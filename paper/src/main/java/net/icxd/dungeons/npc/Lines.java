package net.icxd.dungeons.npc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.utils.Text;

/**
 * An NPC saying its lines to a player, as Hypixel's do: each in chat at its tick from the click, in
 * the NPC's voice, and what comes of it with the last (an objective done, a menu). One talk at a time
 * for each player: a click while one goes on does nothing. Main thread.
 */
public final class Lines {
    private static final Set<UUID> TALKING = new HashSet<>();

    private Lines() {
    }

    /**
     * Says these lines at these ticks ({@code ticks[i]} for {@code lines.get(i)}), then {@code atLast}
     * with the last (null for nothing), if they're still on. False if they were already being talked to.
     */
    public static boolean say(Plugin plugin, Player player, List<String> lines, long[] ticks, Voice voice, Runnable atLast) {
        UUID id = player.getUniqueId();
        if (!TALKING.add(id)) return false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean last = i == lines.size() - 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (last) TALKING.remove(id);
                if (!player.isOnline()) return;
                player.sendMessage(Text.line(line));
                voice.speak(player);
                if (last && atLast != null) atLast.run();
            }, ticks[i]);
        }
        return true;
    }

    /** Whether a talk with them goes on. */
    public static boolean talking(Player player) {
        return TALKING.contains(player.getUniqueId());
    }
}
