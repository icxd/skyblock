package net.icxd.dungeons.session;

import lombok.Getter;
import lombok.Setter;
import net.icxd.dungeons.region.RegionType;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Replacement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What this server keeps about an online player while they're on it: their stats (worked out at
 * most once a tick), mana, ability cooldowns, the region they're in and what the action bar shows
 * in place of defense or mana. It ends when they leave (see PlayerListener), so nothing of theirs
 * stays behind, and a rejoin starts from full mana. Main thread.
 */
public final class PlayerSession {
    private static final Map<UUID, PlayerSession> sessions = new HashMap<>();

    private final Player player;
    private Stats stats;
    private int statsTick = Integer.MIN_VALUE;
    /** -1 until their mana pool is known, then full. */
    @Getter @Setter private int mana = -1;
    private Replacement defenseReplacement;
    private Replacement manaReplacement;
    private final Map<String, Long> cooldownEnds = new HashMap<>();
    /** Null until they've moved. */
    @Getter @Setter private RegionType region;
    /** When their next mining break animation may start (see MiningManager). */
    @Getter @Setter private long nextBreakPhase;

    private PlayerSession(Player player) {
        this.player = player;
    }

    /**
     * The player's session. For someone who has already left, a fresh one that isn't kept (so a late
     * task can't bring a session back after they've gone).
     */
    public static PlayerSession of(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session != null) return session;
        session = new PlayerSession(player);
        if (player.isOnline()) sessions.put(player.getUniqueId(), session);
        return session;
    }

    public static void end(UUID player) {
        sessions.remove(player);
    }

    /** Their stats now: the base, their armor and held item, worked out once per tick. */
    public Stats stats() {
        int tick = Bukkit.getCurrentTick();
        if (stats == null || statsTick != tick) {
            stats = PlayerStats.of(player);
            statsTick = tick;
        }
        return stats;
    }

    /** Null once it has run out. */
    public Replacement getDefenseReplacement() {
        if (defenseReplacement != null && defenseReplacement.expired()) defenseReplacement = null;
        return defenseReplacement;
    }

    public void setDefenseReplacement(Replacement replacement) {
        this.defenseReplacement = replacement;
    }

    /** Null once it has run out. */
    public Replacement getManaReplacement() {
        if (manaReplacement != null && manaReplacement.expired()) manaReplacement = null;
        return manaReplacement;
    }

    public void setManaReplacement(Replacement replacement) {
        this.manaReplacement = replacement;
    }

    /** Milliseconds left on a cooldown; 0 if it's ready. */
    public long cooldownLeft(String key) {
        Long end = cooldownEnds.get(key);
        return end == null ? 0 : Math.max(0, end - System.currentTimeMillis());
    }

    public void startCooldown(String key, long millis) {
        cooldownEnds.put(key, System.currentTimeMillis() + millis);
    }
}
