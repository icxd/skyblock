package net.icxd.dungeons.mob.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.EnumSet;

/** Walks up to its target and hits it, at most once every {@code cooldownTicks}. */
public class MeleeAttackGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "melee_attack"));

    private final Mob mob;
    private final double speed;
    private final int cooldownTicks;
    private int cooldown;

    public MeleeAttackGoal(Mob mob, double speed, int cooldownTicks) {
        this.mob = mob;
        this.speed = speed;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public boolean shouldActivate() {
        return mob.getTarget() != null;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.lookAt(target);
        mob.getPathfinder().moveTo(target, speed);
        if (cooldown > 0) cooldown--;
        double reach = mob.getWidth() * 2 + target.getWidth();
        if (cooldown == 0 && mob.getLocation().distanceSquared(target.getLocation()) <= reach * reach) {
            mob.swingMainHand();
            mob.attack(target);
            cooldown = cooldownTicks;
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
