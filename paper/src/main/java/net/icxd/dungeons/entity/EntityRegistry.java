package net.icxd.dungeons.entity;

import net.icxd.dungeons.utils.Utils;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

public class EntityRegistry {
    @Getter
    private static final HashMap<String, CustomEntity> registry = new HashMap<>();
    public EntityRegistry() {
        try {
            for (Class<?> entity : Utils.instantiableSubTypesOf(CustomEntity.class)) {
                CustomEntity customEntity = (CustomEntity) entity.newInstance();
                registry.put(customEntity.getId(), customEntity);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    public static CustomEntity get(String id) {
        return registry.get(id.toUpperCase());
    }

    /** The custom mob this entity was spawned as (not every entity of its type is one), or null. */
    public static CustomEntity get(LivingEntity entity) {
        CustomEntity spawned = EntityBuilder.typeOf(entity);
        if (spawned != null) return spawned;
        String id = entity.getPersistentDataContainer().get(EntityBuilder.TYPE, PersistentDataType.STRING);
        return id == null ? null : registry.get(id);
    }
}
