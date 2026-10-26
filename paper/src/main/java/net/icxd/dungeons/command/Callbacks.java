package net.icxd.dungeons.command;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Hypixel's {@code /cb <uuid>}, what a clickable chat line runs (recorded, 2026-10-25: Kat's call's "[PICK UP]" is
 * {@code /cb 6ebb7bbc-...}): one action, by a random id, for the player it was sent to. It runs once; what they haven't
 * clicked is forgotten when they leave, and past the last {@link #KEPT} sent them. UNKNOWN what Hypixel says for one
 * that's gone: nothing. Main thread.
 */
public final class Callbacks implements Listener {
    static final int KEPT = 16;

    private static final Map<UUID, Map<UUID, Runnable>> SENT = new HashMap<>();

    /** A new one for them: its id, for {@code /cb <id>}. */
    public static UUID add(Player player, Runnable action) {
        Map<UUID, Runnable> theirs = SENT.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashMap<>());
        if (theirs.size() >= KEPT) theirs.remove(theirs.keySet().iterator().next());
        UUID id = UUID.randomUUID();
        theirs.put(id, action);
        return id;
    }

    /** Runs theirs of this id, once; false if there's none (not theirs, run already, or not an id at all). */
    public static boolean run(Player player, String id) {
        Map<UUID, Runnable> theirs = SENT.get(player.getUniqueId());
        if (theirs == null) return false;
        Runnable action;
        try {
            action = theirs.remove(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (action == null) return false;
        action.run();
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SENT.remove(event.getPlayer().getUniqueId());
    }
}
