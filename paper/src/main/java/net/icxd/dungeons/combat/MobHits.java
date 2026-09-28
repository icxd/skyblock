package net.icxd.dungeons.combat;

import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.Mobs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * SkyBlock's mobs as effects find and hurt them, both kinds alike ({@link Mobs}' and the Blood Room's
 * {@link DungeonMobs}): whether one can be hurt, what a hit on it is worked out against, and an effect's
 * own damage to it (Cleave's share, a lightning strike, a damage over time), which goes the same way as a
 * hit's (health, damage number, kill and drops for the player, the run's damage dealt) but tells no hit
 * listeners. Main thread.
 */
public final class MobHits {
    private MobHits() {
    }

    /** Whether it can be hurt: one of SkyBlock's mobs, alive and not invulnerable (the Watcher). Never a player. */
    public static boolean hittable(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player || entity.isDead()) return false;
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(entity);
        if (dungeonMob != null) return !dungeonMob.invulnerable();
        Mobs.Live mob = Mobs.of(entity);
        return mob != null && !mob.type().isInvulnerable() && mob.health() > 0;
    }

    /**
     * What a hit on it is worked out against now: its health, Defense (less what its debuffs take off, see
     * {@link MobDebuffs}), magic resistance, types and hits taken; null for anything that isn't one of
     * SkyBlock's mobs.
     */
    public static Damage.Target target(LivingEntity entity) {
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(entity);
        Damage.Target raw;
        if (dungeonMob != null) {
            raw = new Damage.Target(dungeonMob.health(), dungeonMob.maxHealth(), dungeonMob.defense(), dungeonMob.magicResistance(),
                    dungeonMob.types(), DungeonMobs.hitsTaken(entity));
        } else {
            Mobs.Live mob = Mobs.of(entity);
            if (mob == null) return null;
            raw = mob.target();
        }
        return MobDebuffs.target(entity, raw);
    }

    /**
     * An effect deals one of SkyBlock's mobs this much, as {@code kind} ({@link HitKind#OTHER} or {@link
     * HitKind#DOT}: a hit's own kinds are for the hit's path), with {@code weapon} behind it (for the killing
     * blow; null for none). A room mob waiting for its room wakes first, as for any hit. No hit listeners, no
     * Ferocity. False if it can't be hurt.
     */
    public static boolean deal(Player by, LivingEntity entity, double damage, DamageIndicators.Look look, HitKind kind, NBTTagCompound weapon) {
        if (!hittable(entity) || damage < 0) return false;
        RunManager.abilityHit(entity);
        if (DungeonMobs.of(entity) != null) DungeonMobs.damage(entity, by, damage, look, kind, weapon);
        else Mobs.damage(Mobs.of(entity), by, damage, look, kind, weapon);
        return true;
    }

    /** Whether it's still one of SkyBlock's mobs and alive (false once a hit has killed it). */
    public static boolean alive(LivingEntity entity) {
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(entity);
        if (dungeonMob != null) return entity.isValid();
        Mobs.Live mob = Mobs.of(entity);
        return mob != null && mob.health() > 0;
    }
}
