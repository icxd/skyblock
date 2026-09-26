package net.icxd.dungeons.mob.mobs;

import net.icxd.dungeons.mob.SkyBlockMob;
import net.icxd.dungeons.mob.goals.LeapAtTargetGoal;
import net.icxd.dungeons.mob.goals.MeleeAttackGoal;
import net.icxd.dungeons.mob.goals.TargetNearestPlayerGoal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/** The Crimson Isle miniboss: leaps at players, withers them, and now and then fires a ring of wither skulls. */
public class Bladesoul implements SkyBlockMob {
    private static final int SKULLS = 16;

    @Override public String getId() { return "BLADESOUL"; }
    @Override public EntityType getEntityType() { return EntityType.SKELETON; }
    @Override public String getName() { return "&8&lBladesoul"; }
    @Override public int getLevel() { return 200; }
    @Override public double getMaxHealth() { return 50_000_000; }
    @Override public double getDamage() { return 4000; }
    @Override public boolean isBoss() { return true; }
    @Override public ItemStack getItemInHand() { return new ItemStack(Material.GOLDEN_AXE); }

    /** The upside-down blaze on its shoulders; part of how it looks, so nothing hurts it. */
    @Override public SkyBlockMob getPassenger() {
        return new SkyBlockMob() {
            @Override public String getId() { return "BLADESOUL_BLAZE"; }
            @Override public EntityType getEntityType() { return EntityType.BLAZE; }
            @Override public boolean isUpsideDown() { return true; }
            @Override public boolean isInvulnerable() { return true; }
            // Scenery: it shouldn't shoot fireballs at anyone.
            @Override public void onSpawn(LivingEntity entity) {
                entity.setAI(false);
            }
        };
    }

    @Override
    public void onSpawn(LivingEntity entity) {
        // Its own goals only: a skeleton's would have it shoot arrows it doesn't have.
        Mob mob = (Mob) entity;
        Bukkit.getMobGoals().removeAllGoals(mob);
        Bukkit.getMobGoals().addGoal(mob, 0, new TargetNearestPlayerGoal(mob, 24));
        Bukkit.getMobGoals().addGoal(mob, 1, new LeapAtTargetGoal(mob, 0.6));
        Bukkit.getMobGoals().addGoal(mob, 2, new MeleeAttackGoal(mob, 1.2, 20));
    }

    @Override
    public void onAttack(LivingEntity entity, Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 5, 5));
    }

    /** About once every 5 seconds: wither skulls out in every direction. */
    @Override
    public void onTick(LivingEntity entity) {
        if (ThreadLocalRandom.current().nextInt(100) != 0) return;
        Location from = entity.getLocation().add(0, 2, 0);
        for (int i = 0; i < SKULLS; i++) {
            double angle = 2 * Math.PI * i / SKULLS;
            Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle));
            WitherSkull skull = entity.getWorld().spawn(from.clone().add(direction), WitherSkull.class, s -> {
                s.setShooter(entity);
                s.setPersistent(false);
            });
            skull.setVelocity(direction.multiply(2));
        }
    }
}
