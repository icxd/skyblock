package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Weapons' abilities that don't hit by themselves: they make their holder (or their next hit) stronger, or
 * move (or hold) their enemies. None has a message (UNKNOWN).
 */
final class Buffs {
    private Buffs() {
    }

    static void forget(UUID player) {
        Hellstorm.UNTIL.remove(player);
        SmashHead.PENDING.remove(player);
        SmashHead.SMASHED.remove(player);
        GravityStorm.SLOWED.remove(player);
    }

    /**
     * The Sword of Bad Health's Bad Health: "Gain +5 Strength for every 5% of total HP you have for 5s.
     * Capped at +100 Strength", after its 100 health (not from the last of it: its cost, which every use
     * charges, see Activations#use).
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
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
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
     * them deal -35% damage for 30 seconds. Debuff doesn't stack." The next melee hit (theirs, or an ability's
     * worked out as one) is doubled, as a multiplicative buff; the mob it lands on, if it's an Animal mob, deals
     * its text's share less to players for its text's time (see {@link #weakened}): a new one starts the time again.
     */
    static final class SmashHead implements AbilityHandler {
        private static final Pattern WEAKENS = Pattern.compile("making them deal -(\\d+)% damage for (\\d+) seconds");
        /** Cast, and waiting for the next hit: the weakening it puts on (a share less, for how long), by player. */
        private static final Map<UUID, double[]> PENDING = new HashMap<>();
        /** Its hit worked out, not landed yet. */
        private static final Map<UUID, double[]> SMASHED = new HashMap<>();
        /** Weakened mobs: until when, and the factor on their hits. */
        private static final Map<UUID, double[]> WEAKENED = new HashMap<>();

        /** Their melee hit's factor (see Combat#addMultiplier): 2 for the next one after a cast, once. */
        static double multiplier(Player player, Boolean ranged) {
            if (ranged) return 1;
            double[] weakens = PENDING.remove(player.getUniqueId());
            if (weakens == null) return 1;
            SMASHED.put(player.getUniqueId(), weakens);
            return 2;
        }

        /** The doubled hit landed (a melee hit, or an ability's): an Animal mob it hit is weakened. */
        static void landed(Player player, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (SMASHED.isEmpty() || landing.kind() != HitKind.MELEE && landing.kind() != HitKind.ABILITY) return;
            double[] weakens = SMASHED.remove(player.getUniqueId());
            if (weakens == null || killed || !target.types().contains(MobType.ANIMAL)) return;
            long now = System.currentTimeMillis();
            WEAKENED.values().removeIf(w -> w[0] <= now);
            WEAKENED.put(landing.entity().getUniqueId(), new double[] {now + weakens[1], 1 - weakens[0]});
        }

        /** The factor on a hit by this mob: less while it's weakened, else 1. */
        static double weakened(Entity mob) {
            if (mob == null || WEAKENED.isEmpty()) return 1;
            double[] weak = WEAKENED.get(mob.getUniqueId());
            return weak != null && weak[0] > System.currentTimeMillis() ? weak[1] : 1;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Matcher m = WEAKENS.matcher(AbilityText.plain(block.text()));
            PENDING.put(player.getUniqueId(), m.find() ? new double[] {Double.parseDouble(m.group(1)) / 100, Long.parseLong(m.group(2)) * 1000} : new double[] {0, 0});
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1, 0.6f);
        }
    }

    /**
     * The Fire Freeze Staff's Fire Freeze: "Creates a circle with a radius of 5 blocks. After 5s, all mobs
     * inside are frozen for 10s." The circle is where they stood as they cast it, and inside is any part of a
     * mob within 5 blocks of its middle (both UNKNOWN); frozen is rooted, as Ice Spray's is, but without its
     * 10% more damage taken (not in this text). The wiki's "it does not work on regular Dungeon minibosses"
     * isn't built (which mobs it means is UNKNOWN). Its look is UNKNOWN (a ring of flames, then snowflakes).
     */
    static final class FireFreeze implements AbilityHandler {
        static final double RADIUS = 5;
        static final int DELAY_TICKS = 100;
        static final int FROZEN_TICKS = 200;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Location at = player.getLocation();
            new BukkitRunnable() {
                private int ticks;

                @Override
                public void run() {
                    if (!Hits.canStillHit(player)) {
                        cancel();
                        return;
                    }
                    if ((ticks += 5) < DELAY_TICKS) {
                        Areas.ring(at, RADIUS, Particle.FLAME);
                        return;
                    }
                    cancel();
                    for (LivingEntity mob : Hits.near(at, RADIUS)) Hits.root(mob, FROZEN_TICKS);
                    at.getWorld().spawnParticle(Particle.SNOWFLAKE, at, 80, RADIUS / 2, 0.5, RADIUS / 2, 0.02);
                    at.getWorld().playSound(at, Sound.BLOCK_GLASS_BREAK, 1, 0.6f);
                }
            }.runTaskTimer(Dungeons.getInstance(), 0, 5);
            player.getWorld().playSound(at, Sound.ITEM_FIRECHARGE_USE, 1, 1.2f);
        }
    }

    /**
     * The Gyrokinetic Wand's Gravity Storm (LEFT CLICK): "Create a large rift at the aimed location, pulling all
     * mobs together." How large, how far it pulls from and for how long are UNKNOWN (8 blocks, 3 seconds, aimed
     * up to 20 blocks away). "Regen mana 10x slower for 3s after cast": their mana regeneration a tenth of itself
     * for that long ({@link #regenFactor}). Its 10 Soulflow isn't charged (no Soulflow yet).
     */
    static final class GravityStorm implements AbilityHandler {
        static final double PULL = 8;
        static final int TICKS = 60;
        private static final Pattern SLOWER = Pattern.compile("Regen mana (\\d+)x slower for (\\d+)s after cast");
        /** Whose mana regenerates slower: until when, and the factor. */
        private static final Map<UUID, double[]> SLOWED = new HashMap<>();

        /** Their mana regeneration's factor now: 0.1 for "10x slower" while it lasts, else 1. */
        static double regenFactor(Player player) {
            double[] slowed = SLOWED.get(player.getUniqueId());
            if (slowed == null) return 1;
            if (slowed[0] > System.currentTimeMillis()) return slowed[1];
            SLOWED.remove(player.getUniqueId());
            return 1;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Matcher m = SLOWER.matcher(AbilityText.plain(block.text()));
            if (m.find()) SLOWED.put(player.getUniqueId(), new double[] {System.currentTimeMillis() + Long.parseLong(m.group(2)) * 1000, 1.0 / Double.parseDouble(m.group(1))});
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
