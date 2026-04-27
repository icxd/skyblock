package net.icxd.dungeons.entity.entities.crimsonisle;

import net.icxd.dungeons.entity.CustomEntity;
import net.icxd.dungeons.entity.EntityDrop;
import net.icxd.dungeons.entity.enums.EntityDropType;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.utils.RandomCollection;
import net.minecraft.server.v1_8_R3.Entity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftMagmaCube;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class MagmaCube implements CustomEntity {
    @Override public EntityType getEntityType() { return EntityType.MAGMA_CUBE; }
    @Override public String getId() { return "MAGMA_CUBE"; }
    @Override public String getName() { return "Magma Cube"; }
    @Override public int getLevel() { return 75; }
    @Override public double getMaxHealth() { return 1000000; }
    @Override public void onDeath(LivingEntity entity) {
        if (entity instanceof CraftMagmaCube magmaCube) {
            Entity nmsEntity = magmaCube.getHandle();
            nmsEntity.getDataWatcher().watch(16, (byte) 1);
        }
    }
    @Override public RandomCollection<EntityDrop> getDrops() {
        return new RandomCollection<EntityDrop>().add(1, new EntityDrop(ItemRegistry.get("DARK_CLAYMORE"), EntityDropType.RNGESUS_INCARNATE, 100));
    }
}
