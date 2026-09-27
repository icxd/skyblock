package net.icxd.dungeons.mob.goals;

import net.icxd.dungeons.mob.Mobs;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.util.Vector;

/**
 * Fights for a player-shaped mob (a Mannequin, as the Crypt Dreadlord and Hypixel's other "player"
 * mobs are here). Mannequins aren't vanilla mobs, so they can't have mob goals: this steers one the
 * way the Watcher's undeads are, straight at its target, jumping onto anything a block high in the
 * way, and hits it when close (the goals' rule: nearest player it can see within range, melee once a
 * second in reach). With {@code skulls}, when its target is out of reach but in sight it fires wither
 * skulls instead, as the Crypt Dreadlord and Souleater do. Call {@link #tick} every tick.
 */
public final class AvatarControl {
    /** How often it fires a skull: UNKNOWN (the recorded ones came 0.4 to 1 s apart), so once a second. */
    private static final int SKULL_EVERY = 20;
    /** "Swing" like the mob goals: once a second in reach. */
    private static final int SWING_EVERY = 20;
    /** The recorded skulls left 1.47 blocks above the mob's feet. */
    private static final double SKULL_FROM = 1.47;
    private static final double SKULL_SPEED = 0.6;
    /** Hit, it lets itself be knocked back this long before it steers again. */
    private static final int STAGGER = 8;

    private final LivingEntity body;
    private final double range;
    private final double blocksPerTick;
    private final boolean skulls;
    private Player target;
    private int age;
    private int nextSwing;
    private int nextSkull;
    private int staggeredUntil;

    /** {@code speed} is its movement speed attribute, as a vanilla mob's (see {@link #blocksPerTick}). */
    public AvatarControl(LivingEntity body, double range, double speed, boolean skulls) {
        this.body = body;
        this.range = range;
        this.blocksPerTick = blocksPerTick(speed);
        this.skulls = skulls;
    }

    /**
     * How far a vanilla mob with this movement speed attribute walks a tick on flat ground, chasing at
     * the goals' speed of 1. Each tick vanilla adds its speed x its forward input, which for a mob is its
     * speed too, x 0.216 / 0.6^3 (0.6 is the ground's friction, so that's 1), and keeps 0.6 x 0.91 = 0.546
     * of its velocity to the next: speed^2 / (1 - 0.546), about 2.2 x speed^2 (0.26 walks 0.149 blocks a tick).
     */
    public static double blocksPerTick(double speed) {
        return speed * speed / (1 - 0.6 * 0.91);
    }

    /** It was hit, and knocked back (unless it's immune). */
    public void staggered(boolean knockbackImmune) {
        if (!knockbackImmune) staggeredUntil = age + STAGGER;
    }

    public void tick() {
        age++;
        target = TargetNearestPlayerGoal.choose(body, target, range, true);
        if (target == null || age < staggeredUntil) return;
        Location me = body.getLocation();
        Location them = target.getLocation();
        face(body, them);
        double reach = MeleeAttackGoal.reach(body, target);
        double distanceSquared = me.distanceSquared(them);
        if (distanceSquared <= reach * reach) {
            if (age >= nextSwing) {
                nextSwing = age + SWING_EVERY;
                body.swingMainHand();
                Mobs.Live live = Mobs.of(body);
                if (live != null) Mobs.mobHit(live, target, body);
            }
            return;
        }
        if (skulls && age >= nextSkull && body.hasLineOfSight(target)) {
            nextSkull = age + SKULL_EVERY;
            skull(target);
        }
        steer(body, them, blocksPerTick);
    }

    private void skull(Player at) {
        Location from = body.getLocation().add(0, SKULL_FROM, 0);
        Vector direction = at.getEyeLocation().toVector().subtract(from.toVector());
        if (direction.lengthSquared() < 1e-6) return;
        Vector aim = direction.normalize();
        from.add(aim.clone().multiply(0.6));
        body.getWorld().spawn(from, WitherSkull.class, s -> {
            s.setShooter(body);
            s.setPersistent(false);
            s.setAcceleration(aim.clone().multiply(0.1));
            s.setVelocity(aim.clone().multiply(SKULL_SPEED));
        });
    }

    /**
     * Steers a mob that has no goals (a Mannequin) towards a spot, this many blocks a tick, jumping onto
     * anything a block high in the way.
     */
    public static void steer(LivingEntity body, Location to, double blocksPerTick) {
        Location me = body.getLocation();
        Vector direction = to.toVector().subtract(me.toVector()).setY(0);
        if (direction.lengthSquared() < 1e-6) return;
        direction.normalize();
        double vy = body.getVelocity().getY();
        Block ahead = me.clone().add(direction.clone().multiply(0.8)).getBlock();
        if (body.isOnGround() && ahead.getType().isSolid() && !ahead.getRelative(0, 1, 0).getType().isSolid()) vy = 0.42;
        body.setVelocity(direction.multiply(blocksPerTick).setY(vy));
    }

    /** Turns it to look that way, level. */
    public static void face(LivingEntity body, Location at) {
        Location me = body.getLocation();
        float yaw = (float) Math.toDegrees(Math.atan2(-(at.getX() - me.getX()), at.getZ() - me.getZ()));
        body.setRotation(yaw, 0);
    }
}
