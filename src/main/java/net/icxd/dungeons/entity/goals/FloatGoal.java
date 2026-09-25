package net.icxd.dungeons.entity.goals;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

/** Vanilla's float goal (1.8's PathfinderGoalFloat): swim up in water and lava. */
public class FloatGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "float"));

    private final Mob mob;

    public FloatGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        return mob.isInWater() || mob.isInLava();
    }

    @Override
    public void tick() {
        if (ThreadLocalRandom.current().nextFloat() < 0.8f) mob.setJumping(true);
    }

    @Override
    public GoalKey<Mob> getKey() {
        return KEY;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.JUMP);
    }
}
