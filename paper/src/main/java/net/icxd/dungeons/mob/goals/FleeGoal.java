package net.icxd.dungeons.mob.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runs away from the nearest player within {@code distance} blocks, rather than fighting them; unless it
 * didn't notice them (see {@link #setUnnoticed}: the Stealth enchantment), which is asked once each time a
 * player comes near.
 */
public class FleeGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "flee"));
    /** How often it picks a new spot to run to, in ticks. */
    private static final int REPLAN = 10;
    /** How often it forgets the players who've gone, in ticks. */
    private static final int FORGET = 20;

    /** Whether a mob that would run from a player stays still instead. */
    @FunctionalInterface
    public interface Unnoticed {
        boolean unnoticed(Mob mob, Player player);
    }

    private static Unnoticed unnoticed = (mob, player) -> false;

    private final Mob mob;
    private final double distance;
    private final double speed;
    private Player from;
    private int replan;
    /** The players near it it has been asked about, and whether it noticed each (see {@link #setUnnoticed}). */
    private final Map<UUID, Boolean> noticed = new HashMap<>();
    private int forget;

    public FleeGoal(Mob mob, double distance, double speed) {
        this.mob = mob;
        this.distance = distance;
        this.speed = speed;
    }

    /**
     * What decides whether a mob noticed a player who's come near (Stealth's "Timid mobs have a 60% chance to
     * remain still instead of fleeing"): asked once until they've gone more than half as far again away.
     */
    public static void setUnnoticed(Unnoticed test) {
        unnoticed = test;
    }

    @Override
    public boolean shouldActivate() {
        if (!noticed.isEmpty() && --forget <= 0) {
            forget = FORGET;
            noticed.keySet().removeIf(id -> {
                Player player = Bukkit.getPlayer(id);
                return player == null || !TargetNearestPlayerGoal.fair(mob, player, distance * 1.5);
            });
        }
        Player nearest = TargetNearestPlayerGoal.nearest(mob, distance, false);
        from = nearest != null && noticed.computeIfAbsent(nearest.getUniqueId(), id -> !unnoticed.unnoticed(mob, nearest)) ? nearest : null;
        return from != null;
    }

    @Override
    public boolean shouldStayActive() {
        return from != null && TargetNearestPlayerGoal.fair(mob, from, distance * 1.5);
    }

    @Override
    public void start() {
        replan = 0;
    }

    @Override
    public void tick() {
        if (from == null || --replan > 0) return;
        replan = REPLAN;
        Location me = mob.getLocation();
        Vector away = me.toVector().subtract(from.getLocation().toVector()).setY(0);
        if (away.lengthSquared() < 1e-6) away = new Vector(1, 0, 0);
        mob.getPathfinder().moveTo(me.clone().add(away.normalize().multiply(distance)), speed);
    }

    @Override
    public void stop() {
        from = null;
        mob.getPathfinder().stopPathfinding();
    }

    @Override
    public GoalKey<Mob> getKey() {
        return KEY;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
