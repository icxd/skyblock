package net.icxd.dungeons.mob.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.EnumSet;

/** Targets the nearest player in survival or adventure within {@code range} blocks, and lets go past it. */
public class TargetNearestPlayerGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "target_nearest_player"));

    private final Mob mob;
    private final double range;

    public TargetNearestPlayerGoal(Mob mob, double range) {
        this.mob = mob;
        this.range = range;
    }

    private boolean fair(Player player) {
        return player.isValid() && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
                && player.getWorld().equals(mob.getWorld()) && player.getLocation().distanceSquared(mob.getLocation()) <= range * range;
    }

    @Override
    public boolean shouldActivate() {
        return true;
    }

    @Override
    public void tick() {
        if (mob.getTarget() instanceof Player current && fair(current)) return;
        mob.setTarget(mob.getWorld().getPlayers().stream().filter(this::fair)
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(mob.getLocation()))).orElse(null));
    }

    @Override
    public GoalKey<Mob> getKey() {
        return KEY;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}
