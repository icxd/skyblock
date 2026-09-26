package net.icxd.dungeons.mob.mobs;

import net.icxd.dungeons.mob.MobDrop;
import net.icxd.dungeons.mob.MobDropType;
import net.icxd.dungeons.mob.SkyBlockMob;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class MagmaCube implements SkyBlockMob {
    @Override public String getId() { return "MAGMA_CUBE"; }
    @Override public EntityType getEntityType() { return EntityType.MAGMA_CUBE; }
    @Override public String getName() { return "Magma Cube"; }
    @Override public int getLevel() { return 75; }
    @Override public double getMaxHealth() { return 1_000_000; }
    @Override public List<MobDrop> getDrops() { return List.of(new MobDrop("DARK_CLAYMORE", MobDropType.RNGESUS_INCARNATE, 100)); }
    @Override public void onSpawn(LivingEntity entity) {
        if (entity instanceof org.bukkit.entity.MagmaCube cube) cube.setSize(3);
    }
}
