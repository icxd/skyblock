package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.abilities.InstantTransmission;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;

/**
 * The Wither scrolls' abilities on Necron's Blade and the swords made from it (the wiki's Necron's Blade
 * Scrolls; NecronsBlade gives the blade their blocks): Implosion, Wither Shield and Shadow Warp, and all
 * three together Wither Impact. Implosion's damage is base 10,000 with Intelligence scaling 0.3, "to all
 * mobs in a 6 block radius"; its chat line is "Your Implosion hit 3 enemies for ... damage." (as the mods
 * that filter it have it), Wither Impact's too.
 */
final class WitherBlade {
    static final Magic.Spell IMPLOSION = new Magic.Spell(10_000, 0.3);
    static final double RADIUS = 6;
    /** "Teleports 10 blocks ahead of you". */
    static final double TELEPORT = 10;
    /** Wither Shield: "Reduces damage taken by 10% for 5 seconds". */
    static final double SHIELD_TAKEN = 0.9;
    static final long SHIELD_MILLIS = 5_000;
    /** Wither Impact's "0.15s" cooldown, not shown in game ("nerfed with a 3 tick cooldown due to autoclickers"). */
    static final long IMPACT_COOLDOWN_MILLIS = 150;
    /** Shadow Warp: "Use this ability again within 5 seconds to detonate the warp". */
    static final int WARP_TICKS = 100;
    /** How far around the warp it pulls enemies from: UNKNOWN (Implosion's 6). */
    static final double WARP_PULL = 6;
    private static final double PULL_SPEED = 0.35;

    /** Whose Wither Shield is up, until when. */
    private static final Map<UUID, Long> SHIELDED = new HashMap<>();
    /** Each player's Shadow Warp waiting to go off. */
    private static final Map<UUID, Warp> WARPS = new HashMap<>();

    private record Warp(Location at, BukkitRunnable pull) {
    }

    private WitherBlade() {
    }

    /** They left: their shield is down, and a warp waiting to go off is gone. */
    static void forget(UUID player) {
        SHIELDED.remove(player);
        Warp warp = WARPS.remove(player);
        if (warp != null) warp.pull().cancel();
    }

    /** What every hit takes from them: 10% less while their Wither Shield is up. */
    static double takenFactor(Player player) {
        Long until = SHIELDED.get(player.getUniqueId());
        if (until == null) return 1;
        if (until > System.currentTimeMillis()) return SHIELD_TAKEN;
        SHIELDED.remove(player.getUniqueId());
        return 1;
    }

    /** Implodes around {@code at}: its damage to every enemy in 6 blocks, and the chat line. */
    static void implode(Player player, SkyBlockItem item, NBTTagCompound tag, Location at) {
        // How it looks and sounds is UNKNOWN: a big explosion's.
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 6, 1.5, 1, 1.5, 0);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
        Hits.report(player, "Implosion", Hits.spell(player, item, tag, IMPLOSION, Hits.near(at, RADIUS)));
    }

    /**
     * Wither Shield: 10% less damage taken for 5 seconds (see {@link #takenFactor}). Its absorption shield,
     * "(12 + CatacombsLevel * 0.32) * 50", isn't built (players have no absorption yet: LATER); since 0.26.1 "Vitality is refunded based on how much of
     * it wasn't used" after the 5 seconds, and none of it can be, so the 50 Vitality comes back then (APPROX).
     */
    static void shield(Player player, double vitality) {
        SHIELDED.put(player.getUniqueId(), System.currentTimeMillis() + SHIELD_MILLIS);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.4f, 1.5f); // UNKNOWN
        if (vitality <= 0) return;
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (!player.isOnline()) return;
            PlayerSession.of(player).setVitality(Math.min(Vitality.max(player), Vitality.get(player) + vitality));
        }, SHIELD_MILLIS / 50);
    }

    /**
     * A teleport 10 blocks ahead, as Instant Transmission's goes ("There are blocks in the way!" where
     * it's cut short); none if there's no room to move at all.
     */
    static void teleport(Player player) {
        InstantTransmission.Landing landing = InstantTransmission.landing(player, TELEPORT);
        if (landing == null) {
            player.sendMessage(InstantTransmission.BLOCKED);
            return;
        }
        Location from = player.getLocation();
        Location to = new Location(from.getWorld(), landing.x() + 0.5, landing.y(), landing.z() + 0.5, from.getYaw(), from.getPitch());
        player.teleport(to);
        player.setFallDistance(0);
        player.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        if (landing.blocked()) player.sendMessage(InstantTransmission.BLOCKED);
    }

    /** "Deals 35,173.8 damage to nearby enemies." */
    static final class Implosion implements AbilityHandler {
        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            implode(player, item, tag, player.getLocation());
        }
    }

    static final class WitherShield implements AbilityHandler {
        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            shield(player, block.vitality());
        }
    }

    /**
     * "Teleport 10 blocks ahead of you. Then implode dealing ... damage to nearby enemies. Also applies the
     * wither shield scroll ability ...": the shield only with its 50 Vitality ("not having enough Vitality
     * simply doesn't activate the shield"), which isn't in its lore. Every 0.15 s at most; a click sooner
     * does nothing and costs nothing.
     */
    static final class WitherImpact implements AbilityHandler {
        /** Wither Impact's Vitality cost, not listed in its lore (0.26.1). */
        static final double VITALITY = 50;

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return PlayerSession.of(player).cooldownLeft("wither_impact") <= 0;
        }

        @Override
        public boolean vitalityOptional() {
            return true;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            use(player, item, tag, block, true);
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, boolean vitalityPaid) {
            PlayerSession.of(player).startCooldown("wither_impact", IMPACT_COOLDOWN_MILLIS);
            // Its block has no Vitality cost (it isn't listed): it's spent here, when there's enough.
            boolean shielded = block.vitality() > 0 ? vitalityPaid : Vitality.spend(player, VITALITY);
            teleport(player);
            implode(player, item, tag, player.getLocation());
            if (shielded) shield(player, VITALITY);
        }
    }

    /**
     * "Creates a spatial distortion 10 blocks ahead of you that sucks all enemies around it. Use this ability
     * again within 5 seconds to detonate the warp and deal ... damage to enemies near it." It takes them
     * there too ("Shadow Warp's teleportation is disabled during the boss fight", the wiki), as the
     * teleport Wither Impact has from it. The second use costs its mana again, and the 10 second cooldown
     * starts once the warp is gone (both UNKNOWN); the detonation's chat line is Implosion's form (UNKNOWN).
     */
    static final class ShadowWarp implements AbilityHandler {
        /** PlayerListener's key for its cooldown. */
        private static final String COOLDOWN = "ability:Shadow Warp";

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            PlayerSession session = PlayerSession.of(player);
            long cooldown = (long) (block.cooldown() * 1000);
            Warp waiting = WARPS.remove(player.getUniqueId());
            if (waiting != null) {
                waiting.pull().cancel();
                detonate(player, item, tag, waiting.at());
                session.startCooldown(COOLDOWN, cooldown);
                return;
            }
            // Its cooldown waits for the warp (PlayerListener has just started it).
            session.startCooldown(COOLDOWN, 0);
            teleport(player);
            Location at = player.getLocation().add(0, 1, 0);
            BukkitRunnable pull = new BukkitRunnable() {
                private int ticks;

                @Override
                public void run() {
                    if (++ticks <= WARP_TICKS && Hits.canStillHit(player)) {
                        pull(at);
                        return;
                    }
                    cancel();
                    // Gone without going off.
                    Warp warp = WARPS.get(player.getUniqueId());
                    if (warp != null && warp.pull() == this) {
                        WARPS.remove(player.getUniqueId());
                        session.startCooldown(COOLDOWN, cooldown);
                    }
                }
            };
            WARPS.put(player.getUniqueId(), new Warp(at, pull));
            pull.runTaskTimer(Dungeons.getInstance(), 0, 1);
        }

        /** Everything around the warp drifts into it (how it looks is UNKNOWN: a swirl of portal particles). */
        private static void pull(Location at) {
            at.getWorld().spawnParticle(Particle.PORTAL, at, 12, 0.6, 0.6, 0.6, 0.4);
            for (LivingEntity mob : Hits.near(at, WARP_PULL)) {
                Vector in = at.toVector().subtract(mob.getLocation().toVector());
                if (in.lengthSquared() < 0.25) continue;
                mob.setVelocity(in.normalize().multiply(PULL_SPEED));
            }
        }

        private static void detonate(Player player, SkyBlockItem item, NBTTagCompound tag, Location at) {
            at.getWorld().spawnParticle(Particle.EXPLOSION, at, 6, 1.5, 1, 1.5, 0);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            Hits.report(player, "Shadow Warp", Hits.spell(player, item, tag, IMPLOSION, Hits.near(at, RADIUS)));
        }
    }
}
