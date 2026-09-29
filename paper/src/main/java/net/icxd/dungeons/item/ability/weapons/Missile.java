package net.icxd.dungeons.item.ability.weapons;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.icxd.dungeons.Dungeons;

/**
 * Something an ability sends flying (a skull, a bat, a balloon, a thrown dagger), moved by the server a
 * step each tick rather than by vanilla's physics, so it goes where the ability says and only SkyBlock's
 * mobs are hit: each mob it touches once ({@link #onHit} says whether it goes on through), and it stops
 * at the first block in its way (unless it goes {@link #throughBlocks}) or at the end of its range. What it
 * looks like is an entity carried along with it, if any, and a trail. It ends when its caster can't hit any
 * more (gone, dead, a ghost), unless it's a ghost's own ({@link #byGhost}). Main thread.
 */
final class Missile {
    /** Touched a mob it hadn't: whether it goes on. */
    @FunctionalInterface
    interface Hit {
        boolean on(Missile missile, LivingEntity mob);
    }

    /** It stopped here: at a block or a mob it didn't go through ({@code impact}), or at the end of its range. */
    @FunctionalInterface
    interface End {
        void at(Missile missile, Location where, boolean impact);
    }

    /** A safety net: nothing flies for more than 20 seconds. */
    private static final int MAX_TICKS = 400;

    private final Player caster;
    private final Location at;
    private Vector velocity;
    private double range = 32;
    private double gravity;
    private double width = 0.3;
    private boolean throughBlocks;
    private boolean byGhost;
    private Function<Missile, Vector> steer;
    private Hit onHit = (missile, mob) -> false;
    private End onEnd = (missile, where, impact) -> {
    };
    private Entity look;
    private Consumer<Location> trail;
    private final Set<UUID> struck = new HashSet<>();
    private double travelled;
    private int ticks;
    private BukkitTask task;
    private boolean done;

    /** From {@code from}, moving {@code velocity} blocks a tick. */
    Missile(Player caster, Location from, Vector velocity) {
        this.caster = caster;
        this.at = from.clone();
        this.velocity = velocity.clone();
    }

    /** How far it goes, in blocks (32 unless it says). */
    Missile range(double blocks) {
        this.range = blocks;
        return this;
    }

    /** How much it falls each tick, for one thrown in an arc. */
    Missile gravity(double perTick) {
        this.gravity = perTick;
        return this;
    }

    /** How close to a mob's hitbox counts as touching it. */
    Missile width(double blocks) {
        this.width = blocks;
        return this;
    }

    Missile throughBlocks() {
        this.throughBlocks = true;
        return this;
    }

    /** A dungeon ghost's (a ghost ability): it flies while its caster is still on and a ghost, as ghosts can't be hurt. */
    Missile byGhost() {
        this.byGhost = true;
        return this;
    }

    /** Where it heads each tick (at the same speed); null from the function keeps it going as it was. */
    Missile steer(Function<Missile, Vector> steer) {
        this.steer = steer;
        return this;
    }

    Missile onHit(Hit onHit) {
        this.onHit = onHit;
        return this;
    }

    Missile onEnd(End onEnd) {
        this.onEnd = onEnd;
        return this;
    }

    /** Carried along with it, and removed when it ends. */
    Missile look(Entity look) {
        this.look = look;
        return this;
    }

    /** Called where it is after each step (particles). */
    Missile trail(Consumer<Location> trail) {
        this.trail = trail;
        return this;
    }

    Missile launch() {
        task = Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), this::tick, 0, 1);
        return this;
    }

    Player caster() {
        return caster;
    }

    /** Where it is now (a copy). */
    Location at() {
        return at.clone();
    }

    double travelled() {
        return travelled;
    }

    /** Still in the air: launched, and not ended or cancelled. */
    boolean flying() {
        return task != null && !done;
    }

    Vector velocity() {
        return velocity.clone();
    }

    /** Whether it has touched this mob already. */
    boolean touched(Entity mob) {
        return struck.contains(mob.getUniqueId());
    }

    /** The mobs it has touched can be touched again (a boomerang on its way back). */
    void forget() {
        struck.clear();
    }

    /** Stops it where it is, without its end. */
    void cancel() {
        finish();
    }

    private void tick() {
        if (done) return;
        if (!(byGhost ? caster.isOnline() && !caster.isDead() : Hits.canStillHit(caster)) || !caster.getWorld().equals(at.getWorld()) || ++ticks > MAX_TICKS) {
            finish();
            return;
        }
        if (steer != null) {
            Vector heading = steer.apply(this);
            // Steering may have ended it (a boomerang caught).
            if (done) return;
            if (heading != null && heading.lengthSquared() > 1e-9) velocity = heading.clone().normalize().multiply(velocity.length());
        }
        velocity.setY(velocity.getY() - gravity);
        double length = velocity.length();
        if (length < 1e-9) return;
        Vector direction = velocity.clone().multiply(1 / length);
        double reach = length;
        boolean block = false;
        if (!throughBlocks) {
            RayTraceResult hit = at.getWorld().rayTraceBlocks(at, direction, length, FluidCollisionMode.NEVER, true);
            if (hit != null) {
                reach = hit.getHitPosition().distance(at.toVector());
                block = true;
            }
        }
        Vector from = at.toVector();
        List<LivingEntity> touched = Hits.along(at, direction, Math.max(reach, 1e-3), width);
        for (LivingEntity mob : touched) {
            if (!struck.add(mob.getUniqueId())) continue;
            if (!onHit.on(this, mob)) {
                double to = Math.max(0, Shapes.along(from, direction, reach, width, mob.getBoundingBox()));
                at.add(direction.clone().multiply(to));
                end(true);
                return;
            }
            if (done) return;
        }
        at.add(direction.clone().multiply(reach));
        travelled += reach;
        if (look != null && look.isValid()) look.teleport(at);
        if (trail != null) trail.accept(at.clone());
        if (block || travelled >= range) end(block);
    }

    private void end(boolean impact) {
        if (done) return;
        finish();
        onEnd.at(this, at.clone(), impact);
    }

    private void finish() {
        if (done) return;
        done = true;
        if (task != null) task.cancel();
        if (look != null) look.remove();
    }

    /**
     * An item shown at a spot (a skull, a boulder, a dagger), {@code scale} times its size, spun about
     * its middle by {@code roll} degrees; not saved with the world, and moved smoothly when teleported.
     */
    static ItemDisplay display(Location at, ItemStack item, float scale, float roll) {
        return at.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setPersistent(false);
            display.setItemStack(item);
            display.setTeleportDuration(1);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                    new Vector3f(scale, scale, scale), new Quaternionf().rotateZ((float) Math.toRadians(roll))));
        });
    }
}
