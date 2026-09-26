package net.icxd.dungeons.entity;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class EntityRunnable implements Runnable {
    @Override
    public void run() {
        if (EntityBuilder.entities.size() < 1) return;
        // A copy: dead ones are forgotten along the way.
        for (LivingEntity entity : List.copyOf(EntityBuilder.entities)) {
            if (entity == null) continue;
            if (entity.isDead()) {
                EntityBuilder.forget(entity);
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
