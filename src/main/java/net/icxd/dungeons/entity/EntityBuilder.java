package net.icxd.dungeons.entity;

import net.icxd.dungeons.Dungeons;
import net.minecraft.server.v1_8_R3.Entity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftMagmaCube;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;

public class EntityBuilder {
    public static final ArrayList<LivingEntity> entities = new ArrayList<>();
    public static final HashMap<LivingEntity, ArmorStand> nameTags = new HashMap<>();
    public static final HashMap<LivingEntity, LivingEntity> passengers = new HashMap<>();

    public static LivingEntity spawn(CustomEntity customEntity, Location location) {
        LivingEntity entity = (LivingEntity) location.getWorld().spawn(location, customEntity.getEntityType().getEntityClass());

        entity.setMaxHealth(customEntity.getMaxHealth());
        entity.setHealth(customEntity.getMaxHealth());

        if (nameTags.containsKey(entity))
            customEntity.updateNameTag(nameTags.get(entity), entity);
        else if (customEntity.isCustomNameVisible())
            customEntity.updateNameTag(entity, entity);
        else if (customEntity.isUpsideDown())
            entity.setCustomName("Dinnerbone");
        entity.setCustomNameVisible(false);
        if (customEntity.hasPassenger()) {
            LivingEntity passenger = spawn(customEntity.getPassenger(), location);
            entity.setPassenger(passenger);
            passengers.put(entity, passenger);
        }
        entity.getEquipment().setItemInHand(customEntity.getItemInHand());
        entity.getEquipment().setHelmet(customEntity.getHelmet());
        entity.getEquipment().setChestplate(customEntity.getChestplate());
        entity.getEquipment().setLeggings(customEntity.getLeggings());
        entity.getEquipment().setBoots(customEntity.getBoots());

        entity.getEquipment().setItemInHandDropChance(0);
        entity.getEquipment().setHelmetDropChance(0);
        entity.getEquipment().setChestplateDropChance(0);
        entity.getEquipment().setLeggingsDropChance(0);
        entity.getEquipment().setBootsDropChance(0);

        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(false);

        if (entity instanceof CraftMagmaCube magmaCube) {
            Entity nmsEntity = magmaCube.getHandle();
            nmsEntity.getDataWatcher().watch(16, (byte) 3);
        }

        if (entity instanceof Entity nmsEntity) {
            nmsEntity.setInvisible(customEntity.isInvisible());
            customEntity.onSpawn(entity);
            return (LivingEntity) nmsEntity.getBukkitEntity();
        }

        customEntity.onSpawn(entity);
        entities.add(entity);
        return entity;
    }
}
