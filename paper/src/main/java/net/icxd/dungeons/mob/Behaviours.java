package net.icxd.dungeons.mob;

import com.destroystokyo.paper.entity.RangedEntity;
import com.destroystokyo.paper.entity.ai.Goal;
import net.icxd.dungeons.mob.goals.AvatarControl;
import net.icxd.dungeons.mob.goals.BoneThrowGoal;
import net.icxd.dungeons.mob.goals.FleeGoal;
import net.icxd.dungeons.mob.goals.MeleeAttackGoal;
import net.icxd.dungeons.mob.goals.RangedBowGoal;
import net.icxd.dungeons.mob.goals.TargetNearestPlayerGoal;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.function.Function;

/**
 * How the dungeon kinds fight (research mobs.md 1.7 and the wiki's mob pages), as mob goals on their
 * zombie or skeleton; player-shaped ones are steered by {@link AvatarControl}. Their numbers (ranges,
 * rates) aren't published: each is marked where it's a stand-in.
 */
public final class Behaviours {
    /** How far they look for players: UNKNOWN, so a skeleton's vanilla follow range. */
    static final double RANGE = 16;
    /** Vanilla mobs hit once a second. */
    private static final int SWING_EVERY = 20;
    /** A skeleton's vanilla shot, on hard difficulty: once a second. */
    private static final int SHOT_EVERY = 20;

    private Behaviours() {
    }

    /** Its goals only, in this order (the first first): a zombie's own would have it go for villagers. */
    @SafeVarargs
    static void goals(LivingEntity entity, Function<Mob, Goal<Mob>>... goals) {
        if (!(entity instanceof Mob mob)) return;
        Bukkit.getMobGoals().removeAllGoals(mob);
        for (int i = 0; i < goals.length; i++) Bukkit.getMobGoals().addGoal(mob, i, goals[i].apply(mob));
    }

    private static Goal<Mob> target(Mob mob) {
        return new TargetNearestPlayerGoal(mob, RANGE, true);
    }

    private static Goal<Mob> melee(Mob mob) {
        return new MeleeAttackGoal(mob, 1, SWING_EVERY);
    }

    /** Walks at you and hits you (the Zombie Grunt). */
    public static MobBehaviour melee() {
        return new MobBehaviour() {
            @Override
            public void spawned(DataMob mob, LivingEntity entity) {
                goals(entity, Behaviours::target, Behaviours::melee);
            }
        };
    }

    /**
     * The Tank Zombie: "immune to knockback, and have high damage reduction. They deal large amounts of
     * knockback" (the wiki; its reduction is its 2,000 Defense). How much knockback is UNKNOWN: twice the usual.
     */
    public static MobBehaviour tank() {
        return new MobBehaviour() {
            @Override
            public void spawned(DataMob mob, LivingEntity entity) {
                goals(entity, Behaviours::target, Behaviours::melee);
            }

            @Override
            public double knockback() {
                return 2;
            }

            @Override
            public boolean knockbackImmune() {
                return true;
            }
        };
    }

    /** The Crypt Lurker: throws its bone once, then fights as a zombie. */
    public static MobBehaviour boneThrower() {
        return new MobBehaviour() {
            @Override
            public void spawned(DataMob mob, LivingEntity entity) {
                goals(entity, Behaviours::target, BoneThrowGoal::new, Behaviours::melee);
            }
        };
    }

    /**
     * Shoots you with its bow, as often as a vanilla skeleton (the Undead Skeleton) or, {@code fast}, twice
     * as often: the Skeleton Grunt "fire[s] at a much higher rate compared to other Skeletons" (the wiki),
     * how much higher is UNKNOWN.
     */
    public static MobBehaviour archer(boolean fast) {
        int every = fast ? SHOT_EVERY / 2 : SHOT_EVERY;
        return new MobBehaviour() {
            @Override
            public void spawned(DataMob mob, LivingEntity entity) {
                if (entity instanceof RangedEntity ranged) goals(entity, Behaviours::target, m -> new RangedBowGoal(ranged, 1, every, RANGE));
            }
        };
    }

    /**
     * The Scared Skeleton: "When a player is near ... it will try to run away from the player, instead
     * of attacking them" (the wiki). How near is UNKNOWN: 8 blocks.
     */
    public static MobBehaviour scared() {
        return new MobBehaviour() {
            @Override
            public void spawned(DataMob mob, LivingEntity entity) {
                goals(entity, m -> new FleeGoal(m, 8, 1.2));
            }
        };
    }

    /**
     * A player-shaped mob: runs at you and hits you (the Crypt Undead, the minibosses); with
     * {@code skulls}, fires wither skulls at you when you're out of its reach (the Crypt Dreadlord and
     * Souleater: "shoots wither skulls at players", the wiki; recorded).
     */
    public static MobBehaviour avatar(boolean skulls) {
        return new MobBehaviour() {
            private AvatarControl control;

            @Override
            public void spawned(DataMob mob, LivingEntity entity) {
                control = new AvatarControl(entity, RANGE, mob.speed(), skulls);
            }

            @Override
            public void tick(DataMob mob, LivingEntity entity) {
                if (control != null) control.tick();
            }

            @Override
            public void damaged(DataMob mob, LivingEntity entity, Player by, double damage) {
                if (control != null) control.staggered(mob.knockbackImmune());
            }
        };
    }
}
