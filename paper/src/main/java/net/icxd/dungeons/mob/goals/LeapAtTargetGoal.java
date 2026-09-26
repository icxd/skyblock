package net.icxd.dungeons.mob.goals;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

/** Vanilla's leap-at-target goal (what 1.8's PathfinderGoalLeapAtTarget did) with a custom jump height. */
public class LeapAtTargetGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "leap_at_target"));

    private final Mob mob;
    private final double leapY;

    public LeapAtTargetGoal(Mob mob, double leapY) {
        this.mob = mob;
        this.leapY = leapY;
    }

    @Override
    public boolean shouldActivate() {
        LivingEntity target = mob.getTarget();
        if (target == null || !mob.isOnGround()) return false;
        double distance = mob.getLocation().distanceSquared(target.getLocation());
        return distance >= 4 && distance <= 16 && ThreadLocalRandom.current().nextInt(5) == 0;
    }

    @Override
    public boolean shouldStayActive() {
        return !mob.isOnGround();
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        Vector velocity = mob.getVelocity();
        double dx = target.getLocation().getX() - mob.getLocation().getX();
        double dz = target.getLocation().getZ() - mob.getLocation().getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > 0) {
            velocity.setX(dx / length * 0.5 * 0.8 + velocity.getX() * 0.2);
            velocity.setZ(dz / length * 0.5 * 0.8 + velocity.getZ() * 0.2);
        }
        velocity.setY(leapY);
        mob.setVelocity(velocity);
    }

    @Override
    public GoalKey<Mob> getKey() {
        return KEY;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.JUMP, GoalType.MOVE);
    }
}
