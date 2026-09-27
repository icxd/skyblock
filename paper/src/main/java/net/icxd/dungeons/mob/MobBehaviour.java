package net.icxd.dungeons.mob;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * How one spawned mob of a kind fights (a fresh one for each mob, so it can keep state): its goals,
 * set when it spawns, and anything it does each tick or after it hits someone. The kinds' are in
 * {@link Behaviours}; the Hub mobs' in {@code mob/mobs}.
 */
public interface MobBehaviour {
    /** Once it's in the world (its gear on, its attributes set). */
    default void spawned(DataMob mob, LivingEntity entity) {
    }

    /** Every tick while it's alive. */
    default void tick(DataMob mob, LivingEntity entity) {
    }

    /** A player hit it for this much (and it's still alive). */
    default void damaged(DataMob mob, LivingEntity entity, Player by, double damage) {
    }

    /** After its hit (or its projectile's) on a player. */
    default void attacked(DataMob mob, LivingEntity entity, Player target) {
    }

    /** How hard its hits knock players back: 1 for the usual. */
    default double knockback() {
        return 1;
    }

    /** Players' hits don't knock it back. */
    default boolean knockbackImmune() {
        return false;
    }

    /** A mob riding it, spawned and removed with it (part of how it looks); null for none. */
    default SkyBlockMob passenger() {
        return null;
    }
}
