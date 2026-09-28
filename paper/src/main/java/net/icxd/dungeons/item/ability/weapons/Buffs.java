package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Weapons' abilities that don't hit by themselves: they make their holder (or their next hit) stronger, or
 * move their enemies. None has a message (UNKNOWN).
 */
final class Buffs {
    private Buffs() {
    }

    static void forget(UUID player) {
        Hellstorm.UNTIL.remove(player);
        SmashHead.PENDING.remove(player);
    }

    /**
     * The Sword of Bad Health's Bad Health: "Gain +5 Strength for every 5% of total HP you have for 5s.
     * Capped at +100 Strength", after its 100 health (not from the last of it).
     */
    static final class BadHealth implements AbilityHandler {
        static final long MILLIS = 5_000;

        /** +5 for every whole 5% of their health they have, at most +100. */
        static double strength(double health, double maxHealth) {
            if (maxHealth <= 0) return 0;
            double percent = Math.max(0, Math.min(1, health / maxHealth)) * 100;
            return Math.min(100, 5 * Math.floor(percent / 5 + 1e-9));
        }

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return Hits.canPayHealth(player, block.healthCost());
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Hits.payHealth(player, block.healthCost());
            double strength = strength(PlayerHealth.get(player), PlayerHealth.max(player));
            PlayerSession.of(player).buff("Bad Health", new Stats().set(Stat.STRENGTH, strength), MILLIS);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 1, 0.8f);
        }
    }

    /** The Hellstorm Wand's Hellstorm: "Gain 5 Ability Damage, but take double damage for 30s." */
    static final class Hellstorm implements AbilityHandler {
        static final long MILLIS = 30_000;
        private static final Map<UUID, Long> UNTIL = new HashMap<>();

        /** What every hit takes from them: twice as much while it lasts. */
        static double takenFactor(Player player) {
            Long until = UNTIL.get(player.getUniqueId());
            if (until == null) return 1;
            if (until > System.currentTimeMillis()) return 2;
            UNTIL.remove(player.getUniqueId());
            return 1;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            PlayerSession.of(player).buff("Hellstorm", new Stats().set(Stat.ABILITY_DAMAGE, 5), MILLIS);
            UNTIL.put(player.getUniqueId(), System.currentTimeMillis() + MILLIS);
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.5f);
        }
    }

    /**
     * The Edible Mace's ME SMASH HEAD: "Your next attack deals double damage and weakens Animal mobs, making
     * them deal -35% damage for 30 seconds." The next melee hit (theirs, or an ability's worked out as one) is
     * doubled, as a multiplicative buff; the weakening isn't built (mobs' damage has no per-target factor:
     * LATER).
     */
    static final class SmashHead implements AbilityHandler {
        private static final Set<UUID> PENDING = new HashSet<>();

        /** Their melee hit's factor (see Combat#addMultiplier): 2 for the next one after a cast, once. */
        static double multiplier(Player player, Boolean ranged) {
            if (ranged || !PENDING.remove(player.getUniqueId())) return 1;
            return 2;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            PENDING.add(player.getUniqueId());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1, 0.6f);
        }
    }

    /**
     * The Gyrokinetic Wand's Gravity Storm (LEFT CLICK): "Create a large rift at the aimed location, pulling all
     * mobs together." How large, how far it pulls from and for how long are UNKNOWN (8 blocks, 3 seconds, aimed
     * up to 20 blocks away). "Regen mana 10x slower for 3s after cast" isn't built (LATER: mana regeneration has
     * no hook), nor is its 10 Soulflow charged (no Soulflow yet).
     */
    static final class GravityStorm implements AbilityHandler {
        static final double PULL = 8;
        static final int TICKS = 60;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Location at = Hits.aimed(player, 20);
            new BukkitRunnable() {
                private int ticks;

                @Override
                public void run() {
                    if ((ticks += 2) > TICKS) {
                        cancel();
                        return;
                    }
                    at.getWorld().spawnParticle(Particle.REVERSE_PORTAL, at, 20, 1, 1, 1, 0.1);
                    for (Entity mob : Hits.near(at, PULL)) {
                        Vector in = at.toVector().subtract(mob.getLocation().toVector());
                        if (in.lengthSquared() > 0.5) mob.setVelocity(in.normalize().multiply(0.4));
                    }
                }
            }.runTaskTimer(Dungeons.getInstance(), 0, 2);
            player.getWorld().playSound(at, Sound.BLOCK_BEACON_POWER_SELECT, 1, 0.5f);
        }
    }
}
