package net.icxd.dungeons.mob.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.EnumSet;

/** Runs away from the nearest player within {@code distance} blocks, rather than fighting them. */
public class FleeGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "flee"));
    /** How often it picks a new spot to run to, in ticks. */
    private static final int REPLAN = 10;

    private final Mob mob;
    private final double distance;
    private final double speed;
    private Player from;
    private int replan;

    public FleeGoal(Mob mob, double distance, double speed) {
        this.mob = mob;
        this.distance = distance;
        this.speed = speed;
    }

    @Override
    public boolean shouldActivate() {
        from = TargetNearestPlayerGoal.nearest(mob, distance, false);
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
