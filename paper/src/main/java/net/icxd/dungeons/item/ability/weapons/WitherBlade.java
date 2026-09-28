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
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.abilities.InstantTransmission;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.Absorption;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.item.bonus.SetBonuses;

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
    /** The shield's absorption's source (see {@link Absorption}). */
    static final String SHIELD = "Wither Shield";
    /** Wither Impact's "0.15s" cooldown, not shown in game ("nerfed with a 3 tick cooldown due to autoclickers"). */
    static final long IMPACT_COOLDOWN_MILLIS = 150;
    /** Shadow Warp: "Use this ability again within 5 seconds to detonate the warp". */
    static final int WARP_TICKS = 100;
    /** How far around the warp it pulls enemies from: UNKNOWN (Implosion's 6). */
    static final double WARP_PULL = 6;
    private static final double PULL_SPEED = 0.35;

    /** Whose Wither Shield is up, until when. */
    private static final Map<UUID, Long> SHIELDED = new HashMap<>();
    /** Each player's shield that's up (the latest cast's; see {@link ShieldUp}). */
    private static final Map<UUID, ShieldUp> SHIELDS = new HashMap<>();
    /** Each player's Shadow Warp waiting to go off. */
    private static final Map<UUID, Warp> WARPS = new HashMap<>();

    private record Warp(Location at, BukkitRunnable pull) {
    }

    /**
     * A cast's shield: the Vitality it cost and the absorption it gave, and, once it isn't the one up any more (a
     * new cast took its place, or they left), the absorption it had left then (negative while it's up).
     */
    private static final class ShieldUp {
        final double vitality;
        final double absorption;
        double leftWhenDown = -1;

        ShieldUp(double vitality, double absorption) {
            this.vitality = vitality;
            this.absorption = absorption;
        }
    }

    private WitherBlade() {
    }

    /** They left: their shield is down, and a warp waiting to go off is gone. */
    static void forget(UUID player) {
        SHIELDED.remove(player);
        // Its end still comes, and gives nothing back.
        ShieldUp shield = SHIELDS.remove(player);
        if (shield != null) shield.leftWhenDown = 0;
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
     * Wither Shield: 10% less damage taken for 5 seconds (see {@link #takenFactor}), and its absorption shield
     * for as long, "(12 + CatacombsLevel * 0.32) * 50" (the wiki's Absorption has it at 984 to 1,400: levels 24
     * to 50); since 0.26.1 "After 5 seconds of having the Absorption shield active, Vitality is refunded based
     * on how much of it wasn't used" (the wiki's Necron's Blade Scrolls): 5 seconds after each cast, by the share
     * of its absorption left when it ended, then or when a new cast's took its place. So a cast's Vitality comes
     * back no sooner than it did before absorption, however fast they cast (UNKNOWN whether Hypixel's refund
     * is worked out so).
     */
    static void shield(Player player, double vitality) {
        UUID id = player.getUniqueId();
        SHIELDED.put(id, System.currentTimeMillis() + SHIELD_MILLIS);
        ShieldUp before = SHIELDS.get(id);
        if (before != null) before.leftWhenDown = Absorption.left(player, SHIELD);
        ShieldUp up = new ShieldUp(vitality, shieldAbsorption(ItemBuilder.catacombsLevel(player)));
        SHIELDS.put(id, up);
        // The shield's end (a task, 5 seconds of ticks) takes it off; its own time is only in case that runs late.
        Absorption.give(player, SHIELD, up.absorption, SHIELD_MILLIS * 2);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.4f, 1.5f); // UNKNOWN
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> shieldEnd(id, up), SHIELD_MILLIS / 50);
    }

    /**
     * A cast's 5 seconds are up: if its shield is still the one up, its absorption goes; either way the Vitality
     * its unused share paid for comes back.
     */
    private static void shieldEnd(UUID id, ShieldUp shield) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) return;
        double left = shield.leftWhenDown;
        if (SHIELDS.remove(id, shield)) {
            left = Absorption.left(player, SHIELD);
            Absorption.remove(player, SHIELD);
        }
        double refund = refund(shield.vitality, Math.max(0, left), shield.absorption);
        if (refund > 0) PlayerSession.of(player).setVitality(Math.min(Vitality.max(player), Vitality.get(player) + refund));
    }

    /** The shield's absorption at this Catacombs level (at most 50): (12 + level x 0.32) x 50. */
    static double shieldAbsorption(int catacombsLevel) {
        return (12 + Math.max(0, catacombsLevel) * 0.32) * 50;
    }

    /** The Vitality back once the shield is down: the share of its absorption that wasn't used. */
    static double refund(double vitality, double left, double absorption) {
        return absorption <= 0 ? vitality : vitality * Math.max(0, Math.min(1, left / absorption));
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
        SetBonuses.teleported(player);
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
     * "Teleport 10 blocks ahead of you dealing ... damage to nearby enemies. Also reduces your damage taken and
     * grants an absorption shield for 5 seconds": the shield only with its 50 Vitality ("not having enough Vitality
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
