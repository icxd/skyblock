package net.icxd.dungeons.mob.goals;

import com.destroystokyo.paper.entity.RangedEntity;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.EnumSet;

/**
 * Shoots its target with its bow once every {@code intervalTicks} while it can see it within
 * {@code range}, standing its ground; otherwise it walks closer. The arrows are its own hits (see
 * {@link net.icxd.dungeons.mob.Mobs}), not vanilla damage.
 */
public class RangedBowGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "ranged_bow"));

    private final RangedEntity mob;
    private final double speed;
    private final int intervalTicks;
    private final double range;
    private int cooldown;

    public RangedBowGoal(RangedEntity mob, double speed, int intervalTicks, double range) {
        this.mob = mob;
        this.speed = speed;
        this.intervalTicks = intervalTicks;
        this.range = range;
    }

    @Override
    public boolean shouldActivate() {
        return mob.getTarget() != null;
    }

    @Override
    public void start() {
        cooldown = intervalTicks;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.lookAt(target);
        if (cooldown > 0) cooldown--;
        boolean inRange = mob.getLocation().distanceSquared(target.getLocation()) <= range * range && mob.hasLineOfSight(target);
        if (!inRange) {
            mob.getPathfinder().moveTo(target, speed);
            return;
        }
        mob.getPathfinder().stopPathfinding();
        if (cooldown == 0) {
            mob.rangedAttack(target, 1);
            cooldown = intervalTicks;
        }
    }

    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    @Override
    public GoalKey<Mob> getKey() {
        return KEY;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
