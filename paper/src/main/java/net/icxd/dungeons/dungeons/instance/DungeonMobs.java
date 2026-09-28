package net.icxd.dungeons.dungeons.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobDebuffs;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.KillingBlow;
import net.icxd.dungeons.mob.MobType;

/**
 * Dungeon mobs (the Watcher and his undeads) and how they fight. Their health is SkyBlock health,
 * kept here rather than in the entity; hits on them are worked out by {@code Combat} (as every
 * player's hit is), and their hits on players go through {@link #hit}.
 */
public final class DungeonMobs {
    /** One of our mobs. */
    public interface Mob {
        /** A player hit it for this much damage. */
        void hurt(Player by, double damage);

        /** Takes no hits at all (the Watcher): the hit is cancelled rather than shown. */
        default boolean invulnerable() {
            return false;
        }

        /** What its own (vanilla) attacks do to a player, in SkyBlock damage; 0 if it has none. */
        default double attackDamage() {
            return 0;
        }

        /** Its health now, and at most (for Giant Killer, Prosecute and Execute). */
        default double health() {
            return 1;
        }

        default double maxHealth() {
            return 1;
        }

        /** What hits on it are reduced by: 100 / (100 + Defense). */
        default double defense() {
            return 0;
        }

        /** The share of magic (ability) damage it resists: 0.1 for 10%. */
        default double magicResistance() {
            return 0;
        }

        /** For Smite and the like. */
        default Set<MobType> types() {
            return Set.of();
        }
    }

    /** On every entity that's one of ours, dead or alive: their deaths drop nothing. */
    public static final String TAG = "skyblock_dungeon_mob";

    private static final Map<UUID, Mob> MOBS = new HashMap<>();
    /** How many hits each has taken (First Strike and Triple-Strike count them). */
    private static final Map<UUID, Integer> HITS = new HashMap<>();

    private DungeonMobs() {
    }

    /** The dungeon mob this entity is, or null. */
    public static Mob of(Entity entity) {
        return entity == null ? null : MOBS.get(entity.getUniqueId());
    }

    static void add(Entity entity, Mob mob) {
        entity.addScoreboardTag(TAG);
        MOBS.put(entity.getUniqueId(), mob);
    }

    static void remove(Entity entity) {
        MOBS.remove(entity.getUniqueId());
        HITS.remove(entity.getUniqueId());
        MobDebuffs.forget(entity);
    }

    /** How many hits it has taken from players so far. */
    public static int hitsTaken(Entity entity) {
        return HITS.getOrDefault(entity.getUniqueId(), 0);
    }

    /** A mob hits a player for SkyBlock damage, less their defense (see {@link PlayerDamage}). */
    public static void hit(Player player, double damage, Entity by) {
        PlayerDamage.hit(player, damage, by);
    }

    /**
     * A player hit one of our mobs for this much SkyBlock damage (see {@link #damage}). The hit itself
     * goes through with no vanilla damage (so it still flinches and takes knockback), or not at all if
     * it's invulnerable.
     */
    public static void playerHit(EntityDamageByEntityEvent event, Player player, Mob mob, double damage, DamageIndicators.Look look) {
        playerHit(event, player, mob, damage, look, HitKind.MELEE, null);
    }

    /** The same, saying what the hit was and what it was dealt with (for the death event's killing blow). */
    public static void playerHit(EntityDamageByEntityEvent event, Player player, Mob mob, double damage, DamageIndicators.Look look,
                                 HitKind kind, NBTTagCompound weapon) {
        if (mob.invulnerable()) {
            event.setCancelled(true);
        } else {
            event.setDamage(0);
            HITS.merge(event.getEntity().getUniqueId(), 1, Integer::sum);
        }
        damage(event.getEntity(), player, damage, look, kind, weapon);
    }

    /**
     * A player deals one of our mobs this much SkyBlock damage, with no vanilla hit needed (an ability's,
     * say), with its damage number as for SkyBlock's other mobs. The Watcher can't be hurt: he zaps them
     * for trying. Nothing for an entity that isn't one of ours. What dealt it isn't said ({@link HitKind#OTHER}).
     */
    public static void damage(Entity entity, Player player, double damage, DamageIndicators.Look look) {
        damage(entity, player, damage, look, HitKind.OTHER, null);
    }

    /**
     * The same, saying what dealt it and with what: if it kills, that's the death event's killing blow
     * (the undeads' deaths come from their own {@link Mob#hurt}, so it's set around that).
     */
    public static void damage(Entity entity, Player player, double damage, DamageIndicators.Look look, HitKind kind, NBTTagCompound weapon) {
        Mob mob = of(entity);
        if (mob == null) return;
        KillingBlow before = KillingBlow.dealing(mob.invulnerable() ? null : KillingBlow.of(kind, weapon, damage, mob.health()));
        try {
            mob.hurt(player, damage);
        } finally {
            KillingBlow.dealing(before);
        }
        if (!mob.invulnerable()) DamageIndicators.show(entity, damage, look);
    }
}
