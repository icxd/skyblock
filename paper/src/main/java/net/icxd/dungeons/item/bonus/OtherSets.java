package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.SkyBlockTime;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The other armor sets whose bonuses the plugin's systems can do: Speedster, the Tuxedos, Blaze and
 * Frozen Blaze, Mushroom, Rabbit, the mining sets (Prospecting, Glacite, Goblin), Mercenary, Armor of the
 * Pack, Sponge and Shark Scale, Cactus, Zombie, Ember, Emerald and Bat Person's pieces, and the Spider
 * Hat, Racing Helmet and Ghast Head's own text. The rest wait for their systems (see BONUSES.md).
 */
final class OtherSets {
    private OtherSets() {
    }

    static List<Bonus> all() {
        return List.of(new Flat("Bonus Speed", new Stats().set(Stat.SPEED, 20)), new Flat("Beginner's Boost", new Stats().set(Stat.MINING_SPEED, 40)),
                new Dashing(), new BlazingAura(false), new BlazingAura(true), new NightAffinity(), new Springsneak(), new ExpertMiner(),
                new SmartMiner(), new DeathTax(), new ArmorOfThePack(), new PackPieces(), new Absorb(), new Deflect(), new ProjectileAbsorption(),
                new NetherLord(), new Tank(), new BatPerson(), new SpiderHat(), new RacingHelmet(), new GhastHead());
    }

    /** A full set bonus that's only stats: Speedster's "Increases Speed by +20", Prospecting Armor's "Grants +40 Mining Speed". */
    record Flat(String name, Stats bonus) implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            stats.add(bonus);
        }
    }

    /**
     * The Tuxedos' Dashing: "Max health set to 75. Deal +50% damage!" (Cheap), 150 and +100% (Fancy), 250
     * and +150% (Elegant); the damage in the additive buffs (the wiki's Additive Sources). Mixed, the
     * cheapest worn piece's (UNKNOWN).
     */
    static final class Dashing implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Dashing";
        }

        /** Max health and damage for the cheapest of these pieces. */
        static double[] of(List<Worn.Piece> pieces) {
            double[] cheapest = {250, 150};
            for (Worn.Piece piece : pieces) {
                double[] own = piece.id().startsWith("CHEAP_") ? new double[] {75, 50}
                        : piece.id().startsWith("FANCY_") ? new double[] {150, 100} : new double[] {250, 150};
                if (own[0] < cheapest[0]) cheapest = own;
            }
            return cheapest;
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            stats.set(Stat.HEALTH, of(active.pieces())[0]);
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            return new Combat.HitBuff(of(active.pieces())[1], 1);
        }
    }

    /**
     * Blaze Armor's Blazing Aura: "Damages mobs within 5 blocks for 3% of their max Health per second. Max
     * 500 damage/s, +100 per 5,000 rods" of the Blaze Rod collection (up to 5,000, the wiki); Frozen Blaze
     * Armor's "for 300 base damage + 3% of their max Health every second and applies Slowness I for 4
     * seconds", with the same most. The most is for each mob (UNKNOWN), and it's dealt as it is. Flames
     * show where it burns (Hypixel's look is UNKNOWN).
     */
    static final class BlazingAura implements Bonus {
        private final boolean frozen;

        BlazingAura(boolean frozen) {
            this.frozen = frozen;
        }

        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return frozen ? "Frozen Blazing Aura" : "Blazing Aura";
        }

        @Override
        public void second(Player player, Active active) {
            double most = most(SetBonuses.collection(player, "BLAZE_ROD"));
            for (LivingEntity mob : Bonuses.mobsNear(player, 5)) {
                Bonuses.damage(player, mob, damage(Bonuses.maxHealth(mob), frozen, most));
                mob.getWorld().spawnParticle(frozen ? Particle.SNOWFLAKE : Particle.FLAME, mob.getLocation().add(0, 1, 0), 6, 0.3, 0.5, 0.3, 0.01);
                if (frozen && !mob.isDead()) mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0));
            }
        }

        /** The most it deals a mob a second, with this many Blaze Rods collected. */
        static double most(double rods) {
            return Math.min(500 + 100 * Math.floor(Math.max(0, rods) / 5_000), 5_000);
        }

        /** What it deals a mob with this max health in a second. */
        static double damage(double maxHealth, boolean frozen, double most) {
            return Math.min((frozen ? 300 : 0) + 0.03 * maxHealth, most);
        }
    }

    /** Mushroom Armor's Night Affinity: "Grants the wearer permanent Night Vision while worn", until it's taken off. */
    static final class NightAffinity implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Night Affinity";
        }

        @Override
        public void second(Player player, Active active) {
            // Long enough that it never gets to the flicker vanilla gives Night Vision's last 10 seconds.
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 30, 0, true, false, true));
        }

        @Override
        public void ended(Player player) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    /**
     * Rabbit Armor's Springsneak (SNEAK): "While sneaking, you have permanent Jump Boost II." Its header
     * has no count: it takes the whole set.
     */
    static final class Springsneak implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Springsneak SNEAK";
        }

        @Override
        public int needs(SetKey set) {
            return 4;
        }

        @Override
        public void sneaked(Player player, Active active, boolean sneaking) {
            if (sneaking) jump(player);
            else player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }

        @Override
        public void second(Player player, Active active) {
            if (player.isSneaking()) jump(player);
        }

        @Override
        public void ended(Player player) {
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }

        /** Two seconds, given again every second while they sneak, so it can't outlast the set (or them leaving). */
        private static void jump(Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 1, true, false, true));
        }
    }

    /** Glacite Armor's Expert Miner: "Grants +2 Mining Speed per Mining Skill level unlocked." */
    static final class ExpertMiner implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Expert Miner";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            stats.add(Stat.MINING_SPEED, 2 * Skills.level(player, Skill.MINING));
        }
    }

    /**
     * Goblin Armor's Smart Miner: "Converts your Intelligence into Mining Speed. +1 Mining Speed for every
     * 15 Intelligence removed": all of it (UNKNOWN), in whole fifteens.
     */
    static final class SmartMiner implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Smart Miner";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            convert(stats);
        }

        static void convert(Stats stats) {
            double intelligence = stats.get(Stat.INTELLIGENCE);
            if (intelligence <= 0) return;
            stats.set(Stat.INTELLIGENCE, 0).add(Stat.MINING_SPEED, Math.floor(intelligence / 15));
        }
    }

    /**
     * Mercenary Armor's Death Tax: "Earn +5 coins and heal 20 each kill. Only against level 10+ mobs!",
     * at most every half second (the wiki: "Added a 0.5s cooldown", July 2026).
     */
    static final class DeathTax implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Death Tax";
        }

        @Override
        public void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
            if (event.variant().level() < 10 || !Bonuses.ready(player, name(), 0.5)) return;
            User user = User.ifLoaded(player.getUniqueId());
            if (user != null && !user.isReleased()) Purse.add(user, 5);
            PlayerHealth.heal(player, 20);
        }
    }

    /**
     * Armor of the Pack: "Gain +35 Strength and +80 Defense for each Armor of the Pack wearer within 30
     * blocks. Max of 3 players!", them among them (UNKNOWN).
     */
    static final class ArmorOfThePack implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Armor of the Pack";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            int wearers = Math.min(3, Bonuses.playersNear(player, 30, other -> SetBonuses.active(other, name())));
            stats.add(Stat.STRENGTH, 35 * wearers).add(Stat.DEFENSE, 80 * wearers);
        }
    }

    /**
     * Each Armor of the Pack piece's own text: "Gain +50 Defense against Animal mobs" (the chestplate's
     * +75: the wiki's "+225 additional Defense" is the set's), and "Gain +5 True Defense", always, as the
     * text reads (UNKNOWN: the wiki has the set's 20 True Defence against Animal mobs only).
     */
    static final class PackPieces implements Bonus {
        static final String CHESTPLATE = "CHESTPLATE_OF_THE_PACK";

        @Override
        public String kind() {
            return ITEM;
        }

        @Override
        public String name() {
            return "Pack";
        }

        @Override
        public boolean item(String id) {
            return id.endsWith("_OF_THE_PACK");
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            stats.add(Stat.TRUE_DEFENSE, 5 * active.count());
        }

        @Override
        public double defenseAgainst(Player player, Active active, Entity by) {
            return SetBonuses.types(by).contains(MobType.ANIMAL) ? defense(active.pieces()) : 0;
        }

        /** What these pieces add to Defense against Animal mobs. */
        static double defense(List<Worn.Piece> pieces) {
            double defense = 0;
            for (Worn.Piece piece : pieces) defense += piece.id().equals(CHESTPLATE) ? 75 : 50;
            return defense;
        }
    }

    /**
     * Sponge and Shark Scale Armor's Absorb: "Doubles your Defense while in water." Armor of Magma's and
     * Yog Armor's Absorb (kills of Magma Cubes and Yogs, kept on the item) aren't here.
     */
    static final class Absorb implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Absorb";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            for (Worn.Piece piece : active.pieces()) if (!piece.id().startsWith("SPONGE_") && !piece.id().startsWith("SHARK_SCALE_")) return;
            if (player.isInWater()) stats.set(Stat.DEFENSE, stats.get(Stat.DEFENSE) * 2);
        }
    }

    /** Cactus Armor's Deflect: "Rebound 33% of the damage you take back at your enemy." */
    static final class Deflect implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Deflect";
        }

        @Override
        public void hurt(Player player, Active active, Entity by, double taken) {
            if (PlayerDamage.attacker(by) instanceof LivingEntity mob) Bonuses.damage(player, mob, 0.33 * taken);
        }
    }

    /** Zombie Armor's Projectile Absorption: "Heals the wearer for 10 per second for 5 seconds when hit by a projectile." */
    static final class ProjectileAbsorption implements Bonus {
        private final Map<UUID, Long> until = new HashMap<>();

        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Projectile Absorption";
        }

        @Override
        public void hurt(Player player, Active active, Entity by, double taken) {
            if (by instanceof Projectile) until.put(player.getUniqueId(), System.currentTimeMillis() + 5_000);
        }

        @Override
        public void second(Player player, Active active) {
            Long end = until.get(player.getUniqueId());
            if (end == null) return;
            if (end < System.currentTimeMillis()) until.remove(player.getUniqueId());
            else PlayerHealth.heal(player, 10);
        }

        @Override
        public void forget(UUID player) {
            until.remove(player);
        }
    }

    /**
     * Ember Armor's Nether Lord: "Grants immunity to all damage from Lava and Fire." Its "Walking over Lava
     * creates Obsidian under your feet" isn't here.
     */
    static final class NetherLord implements Bonus {
        private static final Set<EntityDamageEvent.DamageCause> FIRE = Set.of(EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK, EntityDamageEvent.DamageCause.LAVA, EntityDamageEvent.DamageCause.CAMPFIRE);

        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Nether Lord";
        }

        @Override
        public boolean immune(Player player, Active active, EntityDamageEvent.DamageCause cause) {
            return FIRE.contains(cause);
        }
    }

    /**
     * Emerald Armor's Tank: "Increases Health by +1 and Defense by +1 for every 3,000 Emeralds in your
     * collection. Max 350 each." Nothing until there are collections (see {@link SetBonuses#setCollections}).
     */
    static final class Tank implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Tank";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            double each = each(SetBonuses.collection(player, "EMERALD"));
            stats.add(Stat.HEALTH, each).add(Stat.DEFENSE, each);
        }

        static double each(double emeralds) {
            return Math.min(Math.floor(Math.max(0, emeralds) / 3_000), 350);
        }
    }

    /**
     * Each Bat Person piece: "All Combat Stats on this armor piece are multiplied by 2x at night, or by 3x
     * during the Spooky Festival!": at SkyBlock's night (7pm to 6am, when the sidebar shows the moon) the
     * piece's Combat Stats count twice. There's no Spooky Festival yet.
     */
    static final class BatPerson implements Bonus {
        @Override
        public String kind() {
            return ITEM;
        }

        @Override
        public String name() {
            return "Bat Person";
        }

        @Override
        public boolean item(String id) {
            return DungeonSets.armorOf(id, "BAT_PERSON");
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            if (SkyBlockTime.now().isDay()) return;
            for (Worn.Piece piece : active.pieces()) twice(ItemStats.of(piece.stack(), player), stats);
        }

        /** A piece's Combat Stats (its own, reforge's, enchantments'...) once more, so they count twice. */
        static void twice(Stats piece, Stats stats) {
            for (Stat stat : DragonSets.COMBAT) stats.add(stat, piece.get(stat));
        }
    }

    // ---------- hats' own text ----------

    /** A helmet whose own text is its bonus. */
    abstract static class Hat implements Bonus {
        private final String id;

        Hat(String id) {
            this.id = id;
        }

        @Override
        public String kind() {
            return ITEM;
        }

        @Override
        public boolean item(String id) {
            return this.id.equals(id);
        }
    }

    /** The Spider Hat's "When worn, Arthropod mobs deal -30% damage." */
    static final class SpiderHat extends Hat {
        SpiderHat() {
            super("SPIDER_HAT");
        }

        @Override
        public String name() {
            return "Spider Hat";
        }

        @Override
        public double takenFrom(Player player, Active active, Entity by) {
            return SetBonuses.types(by).contains(MobType.ARTHROPOD) ? 0.7 : 1;
        }
    }

    /** The Racing Helmet's "Grants +100 Speed Cap." */
    static final class RacingHelmet extends Hat {
        RacingHelmet() {
            super("RACING_HELMET");
        }

        @Override
        public String name() {
            return "Racing Helmet";
        }

        @Override
        public double speedCap(Player player, Active active) {
            return 100;
        }
    }

    /** The Ghast Head's "Restores +5 Health every second while worn" (0.26.1's; it was 1% of their max health). */
    static final class GhastHead extends Hat {
        GhastHead() {
            super("GHAST_HEAD");
        }

        @Override
        public String name() {
            return "Ghast Head";
        }

        @Override
        public void second(Player player, Active active) {
            PlayerHealth.heal(player, 5);
        }
    }
}
