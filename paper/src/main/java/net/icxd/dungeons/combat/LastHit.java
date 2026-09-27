package net.icxd.dungeons.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * The last SkyBlock hit each player took ({@link PlayerDamage#hit}): what did it and how, so a death
 * can say who killed them ("You were killed by Zombie Grunt"). Main thread.
 */
public final class LastHit {
    /** {@code by} is null for a hit with no one behind it (a trap's). */
    public record Hit(Entity by, PlayerDamage.Kind kind, long at) {
    }

    private static final Map<UUID, Hit> HITS = new HashMap<>();

    private LastHit() {
    }

    static void record(Player player, Entity by, PlayerDamage.Kind kind) {
        HITS.put(player.getUniqueId(), new Hit(by, kind, System.currentTimeMillis()));
    }

    /** Their last hit if it was at most {@code millis} ago; null if not. */
    public static Hit within(Player player, long millis) {
        Hit hit = HITS.get(player.getUniqueId());
        return hit != null && System.currentTimeMillis() - hit.at() <= millis ? hit : null;
    }

    public static void forget(UUID player) {
        HITS.remove(player);
    }
}
