package net.icxd.dungeons.entity;

import net.icxd.dungeons.Dungeons;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityBuilder {
    public static final ArrayList<LivingEntity> entities = new ArrayList<>();
    public static final HashMap<LivingEntity, ArmorStand> nameTags = new HashMap<>();
    public static final HashMap<LivingEntity, LivingEntity> passengers = new HashMap<>();
    /** Which custom mob each spawned entity is (passengers included, which aren't registered types). */
    private static final Map<UUID, CustomEntity> types = new HashMap<>();
    /** The same on the entity itself, so it's still known after a restart. */
    static final NamespacedKey TYPE = new NamespacedKey("skyblock", "mob");

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

        entity.getPersistentDataContainer().set(TYPE, PersistentDataType.STRING, customEntity.getId());
        types.put(entity.getUniqueId(), customEntity);
        customEntity.onSpawn(entity);
        entities.add(entity);
        return entity;
    }

    /** The custom mob an entity was spawned as, or null. */
    static CustomEntity typeOf(LivingEntity entity) {
        return types.get(entity.getUniqueId());
    }

    /** A mob that's gone: its name tag and passenger go too. */
    public static void forget(LivingEntity entity) {
        entities.remove(entity);
        types.remove(entity.getUniqueId());
        ArmorStand tag = nameTags.remove(entity);
        if (tag != null) tag.remove();
        LivingEntity passenger = passengers.remove(entity);
        if (passenger != null) {
            types.remove(passenger.getUniqueId());
            entities.remove(passenger);
            passenger.remove();
        }
    }
}
