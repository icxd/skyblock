package net.icxd.dungeons.mob.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Targets the nearest player in survival or adventure within {@code range} blocks, and lets go past it.
 * With {@code sight}, it only picks up players it can see (a dungeon room's mobs don't come for you
 * through the walls), though it keeps after one it has lost sight of.
 */
public class TargetNearestPlayerGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "target_nearest_player"));
    private static final List<BiPredicate<LivingEntity, Player>> IGNORED = new ArrayList<>();

    private final Mob mob;
    private final double range;
    private final boolean sight;

    public TargetNearestPlayerGoal(Mob mob, double range) {
        this(mob, range, false);
    }

    public TargetNearestPlayerGoal(Mob mob, double range, boolean sight) {
        this.mob = mob;
        this.range = range;
        this.sight = sight;
    }

    /** A player it may go for: alive, not invulnerable (a dungeon ghost), in survival or adventure, in its world and in range. */
    public static boolean fair(LivingEntity from, Player player, double range) {
        return player.isValid() && !player.isInvulnerable() && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
                && player.getWorld().equals(from.getWorld()) && player.getLocation().distanceSquared(from.getLocation()) <= range * range;
    }

    /** The nearest fair player (that it can see, with {@code sight}); null for none. */
    public static Player nearest(LivingEntity from, double range, boolean sight) {
        return from.getWorld().getPlayers().stream().filter(p -> fair(from, p, range) && (!sight || from.hasLineOfSight(p)))
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(from.getLocation()))).orElse(null);
    }

    /**
     * Adds what keeps a mob from going for a player (the Intimidation Talisman's "Level 1 monsters will no longer
     * target you"): asked for each player it would choose. It can still flee from them.
     */
    public static void addIgnored(BiPredicate<LivingEntity, Player> ignores) {
        IGNORED.add(ignores);
    }

    /** Whether it leaves this player alone (see {@link #addIgnored}). */
    static boolean ignores(LivingEntity from, Player player) {
        for (BiPredicate<LivingEntity, Player> ignores : IGNORED) if (ignores.test(from, player)) return true;
        return false;
    }

    /** Who it goes for now: the one it had while they're still fair, else the nearest; never one it leaves alone. */
    public static Player choose(LivingEntity from, LivingEntity current, double range, boolean sight) {
        if (current instanceof Player player && fair(from, player, range) && !ignores(from, player)) return player;
        if (IGNORED.isEmpty()) return nearest(from, range, sight);
        return from.getWorld().getPlayers().stream().filter(p -> fair(from, p, range) && (!sight || from.hasLineOfSight(p)) && !ignores(from, p))
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(from.getLocation()))).orElse(null);
    }

    @Override
    public boolean shouldActivate() {
        return true;
    }

    @Override
    public void tick() {
        Player target = choose(mob, mob.getTarget(), range, sight);
        if (target != mob.getTarget()) mob.setTarget(target);
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
