package net.icxd.dungeons.mob.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Crypt Lurker's: it throws the bone it holds at its target, once ("can throw that bone dealing
 * massive damage. However, after they throw the bone, they act like normal zombies", the wiki; the
 * recordings show its hand empty as the bone flies). The bone is a snowball that looks like one and
 * hits for the mob's damage (see {@link net.icxd.dungeons.mob.Mobs}); it can be dodged.
 */
public class BoneThrowGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("dungeons", "bone_throw"));
    /** When it throws is UNKNOWN: here from 4 to 12 blocks, with a 1 in 20 chance each tick it could. */
    private static final double MIN = 4;
    private static final double MAX = 12;
    private static final int ONE_IN = 20;
    private static final double SPEED = 1.2;

    private final Mob mob;
    private boolean thrown;

    public BoneThrowGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        LivingEntity target = mob.getTarget();
        if (thrown || target == null || ThreadLocalRandom.current().nextInt(ONE_IN) != 0) return false;
        double distance = mob.getLocation().distance(target.getLocation());
        return distance >= MIN && distance <= MAX && mob.hasLineOfSight(target);
    }

    @Override
    public boolean shouldStayActive() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        thrown = true;
        mob.lookAt(target);
        mob.swingMainHand();
        Vector aim = target.getEyeLocation().toVector().subtract(mob.getEyeLocation().toVector());
        double horizontal = Math.hypot(aim.getX(), aim.getZ());
        // A little up, for the snowball's fall on the way.
        Vector velocity = aim.normalize().multiply(SPEED).add(new Vector(0, horizontal * 0.02, 0));
        Snowball bone = mob.launchProjectile(Snowball.class, velocity);
        bone.setItem(new ItemStack(Material.BONE));
        bone.setPersistent(false);
        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null) equipment.setItemInMainHand(null);
    }

    @Override
    public GoalKey<Mob> getKey() {
        return KEY;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.LOOK);
    }
}
