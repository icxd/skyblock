package net.icxd.dungeons.entity;

import net.icxd.dungeons.Dungeons;
import org.bukkit.Location;
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

        if (entity instanceof org.bukkit.entity.MagmaCube magmaCube) magmaCube.setSize(3);

        entity.setInvisible(customEntity.isInvisible());

        customEntity.onSpawn(entity);
        entities.add(entity);
        return entity;
    }
}
