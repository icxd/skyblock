package net.icxd.dungeons.crimsonisle.miniboss;

import net.icxd.dungeons.entity.CustomEntity;
import net.icxd.dungeons.entity.EntityBuilder;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftBlaze;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftSkeleton;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Random;

public class Bladesoul implements CustomEntity {
    private final HashMap<LivingEntity, Location> spawnLocations = new HashMap<>();
    @Override public EntityType getEntityType() { return EntityType.SKELETON; }
    @Override public String getId() { return "BLADESOUL"; }
    @Override public String getName() { return "" + ChatColor.DARK_GRAY + ChatColor.BOLD + "Bladesoul"; }
    @Override public int getLevel() { return 200; }
    @Override public double getMaxHealth() { return 50000000; }
    @Override public double getDamage() { return 4000; }
    @Override public boolean isBoss() { return true; }
    @Override public ItemStack getItemInHand() { return new ItemStack(Material.GOLD_AXE); }
    @Override public CustomEntity getPassenger() {
        return new CustomEntity() {
            @Override public EntityType getEntityType() { return EntityType.BLAZE; }
            @Override public String getId() { return "BLADESOUL_BLAZE"; }
            @Override public boolean isCustomNameVisible() { return false; }
            @Override public boolean isUpsideDown() { return true; }
            @Override public String getName() { return "Dinnerbone"; }
            @Override public int getLevel() { return 200; }
            @Override public double getMaxHealth() { return 999999999; }
            @Override public void onSpawn(LivingEntity entity) {
                ((CraftBlaze) entity).getHandle().goalSelector.a(0, new PathfinderGoalFloat(((CraftBlaze) entity).getHandle()));
            }
        };
    }
    @Override public void onSpawn(LivingEntity entity) {
        spawnLocations.put(entity, entity.getLocation());
        // remove ai
        ((CraftSkeleton) entity).getHandle().goalSelector = new PathfinderGoalSelector(((CraftSkeleton) entity).getHandle().world.methodProfiler);
        ((CraftSkeleton) entity).getHandle().targetSelector = new PathfinderGoalSelector(((CraftSkeleton) entity).getHandle().world.methodProfiler);

        ((CraftSkeleton) entity).getHandle().goalSelector.a(0, new PathfinderGoalLeapAtTarget(((CraftSkeleton) entity).getHandle(), 5F));

        ArmorStand armorStand = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().clone().add(0, 4, 0), EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setMarker(true);
        armorStand.setCustomNameVisible(true);
        updateNameTag(armorStand, entity);
        EntityBuilder.nameTags.put(entity, armorStand);
    }
    @Override public void onDamaged(LivingEntity entity, double damage) { updateNameTag(EntityBuilder.nameTags.get(entity), entity); }
    @Override public void onDeath(LivingEntity entity) {
        //
    }
    @Override public void onAttack(LivingEntity entity, LivingEntity target) {
        target.addPotionEffect(PotionEffectType.WITHER.createEffect(20 * 5, 5));
    }
    @Override public void onTick(LivingEntity entity) {
        Random random = new Random();
        /*if (random.nextInt(100) == 0) {
            entity.getNearbyEntities(15, 15, 15).forEach(e -> {
                if (e instanceof Player player) {
                    WitherSkull skull = (WitherSkull) entity.getWorld().spawnEntity(entity.getLocation().add(0, 2, 0), EntityType.WITHER_SKULL);

            });
        } else */if (random.nextInt(100) == 1) {
            // spawn a ring of wither skulls around the entity pointing away from the entity
            int x = 16;
            for (int i = 0; i < x; i += 360 / x) {
                WitherSkull skull = (WitherSkull) entity.getWorld().spawnEntity(entity.getLocation().add(0, 2, 0), EntityType.WITHER_SKULL);
                skull.setVelocity(new Vector(Math.cos(i), 0, Math.sin(i)).multiply(2));
            }
        }
    }

    private Vector rotateAroundY(Vector vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double currentX = vector.getX();
        double currentZ = vector.getZ();
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        vector.setX(currentX * cosine - currentZ * sine);
        vector.setZ(currentX * sine + currentZ * cosine);
        return vector;
    }
}
