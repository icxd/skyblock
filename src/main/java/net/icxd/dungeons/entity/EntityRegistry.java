package net.icxd.dungeons.entity;

import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.reflections.Reflections;

import java.util.HashMap;

public class EntityRegistry {
    @Getter
    private static final HashMap<String, CustomEntity> registry = new HashMap<>();
    public EntityRegistry() {
        try {
            for (Class<?> entity : new Reflections().getSubTypesOf(CustomEntity.class)) {
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

    public static CustomEntity get(LivingEntity entity) {
        for (CustomEntity customEntity : registry.values()) {
            if (customEntity.getEntityType() == entity.getType()) {
                return customEntity;
            }
        }
        return null;
    }
}
