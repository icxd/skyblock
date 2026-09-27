package net.icxd.dungeons.mob.mobs;

import net.icxd.dungeons.mob.DataMob;
import net.icxd.dungeons.mob.MobBehaviour;
import org.bukkit.entity.LivingEntity;

/** The Hub's test Magma Cube: a big vanilla magma cube (its stats are the kind's, see MobKinds). */
public class MagmaCube implements MobBehaviour {
    @Override
    public void spawned(DataMob mob, LivingEntity entity) {
        if (entity instanceof org.bukkit.entity.MagmaCube cube) cube.setSize(3);
    }
}
