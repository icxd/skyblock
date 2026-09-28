package net.icxd.dungeons.combat;

import net.icxd.dungeons.session.PlayerSession;
import org.bukkit.entity.Player;

/**
 * Whether a player is "in combat" (Respite's "+3 Health Regen while out of combat", the Soulflow items' "per
 * 5s in combat", Overflow Mana "when leaving combat"): they dealt SkyBlock's mobs damage, or took a hit from
 * one, within the last {@link #WINDOW_MILLIS}. Kept on their session, so it's gone when they leave. Main thread.
 */
public final class CombatState {
    /**
     * How long a hit keeps them in combat. UNKNOWN: no page gives it (the wiki's Respite, Health Regen, Mana,
     * Soulflow and Combat pages say only "in combat"); the Soulflow items' "per 5s in combat" is the one number
     * near it, so 5 seconds.
     */
    public static final long WINDOW_MILLIS = 5_000;

    private CombatState() {
    }

    /** They dealt a mob damage just now (any of theirs: a hit, an ability, a damage over time). */
    public static void dealt(Player player) {
        PlayerSession.of(player).setLastDealtMillis(System.currentTimeMillis());
    }

    /**
     * A mob's hit (or its projectile's) took health from them just now. Traps' hits and vanilla damage (a fall,
     * fire) don't count (UNKNOWN whether Hypixel's do).
     */
    public static void took(Player player) {
        PlayerSession.of(player).setLastTakenMillis(System.currentTimeMillis());
    }

    /** Whether they're in combat now. */
    public static boolean inCombat(Player player) {
        return inCombat(player, WINDOW_MILLIS);
    }

    /** Whether they dealt or took damage within the last {@code windowMillis} (for effects whose own window is known). */
    public static boolean inCombat(Player player, long windowMillis) {
        PlayerSession session = PlayerSession.of(player);
        return inCombat(session.getLastDealtMillis(), session.getLastTakenMillis(), System.currentTimeMillis(), windowMillis);
    }

    /** Milliseconds since they last dealt or took damage; {@link Long#MAX_VALUE} if they never have here. */
    public static long sinceCombat(Player player) {
        PlayerSession session = PlayerSession.of(player);
        return since(session.getLastDealtMillis(), session.getLastTakenMillis(), System.currentTimeMillis());
    }

    /** Whether the later of the two (0 for never) is within {@code window} of {@code now}. */
    static boolean inCombat(long lastDealt, long lastTaken, long now, long window) {
        return since(lastDealt, lastTaken, now) <= window;
    }

    static long since(long lastDealt, long lastTaken, long now) {
        long last = Math.max(lastDealt, lastTaken);
        return last <= 0 ? Long.MAX_VALUE : Math.max(0, now - last);
    }
}
