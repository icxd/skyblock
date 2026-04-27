package net.icxd.dungeons.entity;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

public class EntityRunnable implements Runnable {
    @Override
    public void run() {
        if (EntityBuilder.entities.size() < 1) return;
        for (LivingEntity entity : EntityBuilder.entities) {
            if (entity == null) continue;
            if (entity.isDead()) {
                EntityBuilder.entities.remove(entity);
                if (EntityBuilder.nameTags.containsKey(entity)) {
                    EntityBuilder.nameTags.get(entity).remove();
                    EntityBuilder.nameTags.remove(entity);
                }
                if (EntityBuilder.passengers.containsKey(entity)) {
                    EntityBuilder.passengers.get(entity).remove();
                    EntityBuilder.passengers.remove(entity);
                }
                continue;
            }
            CustomEntity customEntity = EntityRegistry.get(entity);
            if (customEntity == null) continue;
            customEntity.onTick(entity);
            ArmorStand nameTag = EntityBuilder.nameTags.get(entity);
            if (nameTag == null) continue;
            nameTag.teleport(entity.getLocation().add(0, 2, 0));
            customEntity.updateNameTag(nameTag, entity);
        }
    }
}
