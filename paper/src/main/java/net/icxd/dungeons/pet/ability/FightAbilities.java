package net.icxd.dungeons.pet.ability;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.icxd.dungeons.bank.BankAccount;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.Debuffs;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobDebuffs;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.combat.VanillaDamage;
import net.icxd.dungeons.item.ability.utility.Protection;
import net.icxd.dungeons.item.ability.weapons.Hits;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.session.Absorption;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;

/**
 * Pets' abilities in a fight, their numbers from their text: their hits' buffs (the wiki's Damage Calculation says which
 * are additive and which multiply), what happens once a hit lands (slows, damage over time, extra damage), the hits they
 * take, and the timed ones (the Guardian's Laserbeam, the Skeleton's arrow). Their own damage is an effect's
 * ({@link HitKind#OTHER}, {@link MobHits#deal}): it sets off nothing else.
 */
final class FightAbilities {
    /** How long Web-weaver's slow lasts (UNKNOWN: the text doesn't say). */
    static final long WEB_MILLIS = 3_000;
    /** How long Ferocious Roar's stun lasts, in ticks (UNKNOWN). */
    static final int ROAR_TICKS = 20;
    /** How long Turtle Shell's Vitality lasts (UNKNOWN). */
    static final long SHELL_MILLIS = 10_000;
    /** How far the Snow Cannon reaches ("can hit mobs up to 8 blocks away", the wiki's Snowman Pet). */
    static final double SNOW_CANNON_RANGE = 8;
    /** How close a mob "gets close to you" for Skeletal Defense (UNKNOWN). */
    static final double CLOSE = 3;
    /** How far the Guardian's Laserbeam reaches the mob they hit (UNKNOWN). */
    static final double LASER_RANGE = 16;
    /** How long a Blizzard's slow and weakness last, given again each second. */
    static final long BLIZZARD_MILLIS = 1_500;

    private FightAbilities() {
    }

    static List<PetAbility> all() {
        return List.of(
                // Buffs on their hits (additive or multiplicative as the wiki's Damage Calculation has them).
                additiveAgainst("ENDER_DRAGON", "End Strike", "Deal #% more damage to ⊙ Ender mobs.", MobType.ENDER),
                additiveAgainst("ZOMBIE", "Rotten Blade", "Deal #% more damage to ༕ Undead mobs", MobType.UNDEAD),
                additiveAgainst("WITHER_SKELETON", "Wither Blood", "Deal #% more damage to ☠ Wither mobs.", MobType.WITHER),
                new Ability("MAGMA_CUBE", "Salt Blade", "Deal #% more damage to slimes") {
                    @Override
                    public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
                        boolean slime = is(landing.entity(), EntityType.SLIME, EntityType.MAGMA_CUBE) || target.types().contains(MobType.CUBIC);
                        return slime ? new Combat.HitBuff(0, 1 + a.n(0) / 100) : null;
                    }
                },
                new Ability("PIGMAN", "Giant Slayer", "Deal +#% damage to monsters Level #+ and +#% damage to monsters Level #+.") {
                    @Override
                    public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
                        int level = level(landing.entity());
                        double buff = level >= a.n(3) ? a.n(2) : level >= a.n(1) ? a.n(0) : 0;
                        return buff > 0 ? new Combat.HitBuff(buff, 1) : null;
                    }
                },
                new Ability("SKELETON", "Bone Arrows", "Increase arrow damage by #% which is doubled while in dungeons") {
                    @Override
                    public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
                        if (landing.kind() != HitKind.ARROW) return null;
                        return new Combat.HitBuff(a.n(0) * (inDungeon(player) ? 2 : 1), 1);
                    }
                },
                new Ability("TIGER", "Apex Predator", "Deal +#% damage against targets with no other mobs within # blocks.") {
                    @Override
                    public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
                        for (LivingEntity other : Hits.near(landing.entity().getLocation(), a.n(1))) if (other != landing.entity()) return null;
                        return new Combat.HitBuff(a.n(0), 1);
                    }
                },
                new Ability("MEGALODON", "Blood Scent", "Deal up to +#% ❁ Damage based on the enemy's missing health") {
                    // "Up to": all of it at no health left, none at full (UNKNOWN between: in proportion).
                    @Override
                    public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
                        if (target.maxHealth() <= 0) return null;
                        double missing = Math.max(0, Math.min(1, 1 - Math.max(0, target.health()) / target.maxHealth()));
                        return missing > 0 ? new Combat.HitBuff(a.n(0) * missing, 1) : null;
                    }
                },
                new Ability("TYRANNOSAURUS", "Close Combat", "Deal #% more damage to enemies within # blocks.") {
                    @Override
                    public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
                        return distance(player, landing.entity()) <= a.n(1) ? new Combat.HitBuff(a.n(0), 1) : null;
                    }
                },
                new Ability("GIRAFFE", "Long Neck", "Increases your melee damage by #% if you are more than # blocks away from the target") {
                    @Override
                    public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
                        if (landing.kind() != HitKind.MELEE) return null;
                        return distance(player, landing.entity()) > a.n(1) ? new Combat.HitBuff(a.n(0), 1) : null;
                    }
                },
                new Ability("GOLDEN_DRAGON", "Legendary Treasure", "Gain #% damage for every million coins in your bank. (Max #%)") {
                    @Override
                    public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
                        User user = User.ifLoaded(player.getUniqueId());
                        if (user == null || user.isReleased()) return null;
                        double buff = Math.min(a.n(0) * Math.floor(BankAccount.balance(user.profile()) / 1_000_000), a.n(1));
                        return buff > 0 ? new Combat.HitBuff(buff, 1) : null;
                    }
                },
                new FirstPounce(),
                new KingOfTheJungle(),
                new GolemLastStand(),
                new Toss(),
                // What happens once a hit lands.
                new ClubbedTail(),
                new Ability("ANKYLOSAURUS", "Unyielding", "Increase the effectiveness of Last Stand and Lifeline by #%.") {
                    // There's no Lifeline here.
                    @Override
                    public double lastStand(Player player, Active a) {
                        return a.n(0);
                    }
                },
                new Ability("SPIDER", "Web-weaver", "Upon hitting a monster, it becomes slowed by #%.") {
                    @Override
                    public void landed(Player player, Active a, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
                        if (killed || !melee(landing) && landing.kind() != HitKind.ARROW) return;
                        MobDebuffs.add(landing.entity(), new Debuffs.Spec(source(player), Debuffs.Kind.SLOW, a.n(0) / 100, 1, WEB_MILLIS), player);
                    }
                },
                new Ability("WITHER_SKELETON", "Death's Touch", "Upon hitting an enemy inflict the wither effect for #% damage over # seconds") {
                    // "#% damage over # seconds", a share of the hit each second ("k%/3" a tick, the wiki); "Does not stack".
                    @Override
                    public void landed(Player player, Active a, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
                        if (killed || !melee(landing) && landing.kind() != HitKind.ARROW || a.n(1) <= 0) return;
                        if (MobDebuffs.hasDot(landing.entity(), source(player))) return;
                        MobDebuffs.dot(landing.entity(), source(player), player, damage * a.n(0) / 100 / a.n(1), 20, (int) a.n(1),
                                DamageIndicators.Look.NORMAL);
                    }
                },
                new FourthFlare(),
                new Ability("TYRANNOSAURUS", "Ferocious Roar", "Attacks have a #% chance to stun the target (#s cooldown).") {
                    private final Map<UUID, Long> ready = new HashMap<>();

                    @Override
                    public void landed(Player player, Active a, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
                        if (killed || !melee(landing) && landing.kind() != HitKind.ARROW) return;
                        long now = System.currentTimeMillis();
                        if (now < ready.getOrDefault(player.getUniqueId(), 0L) || ThreadLocalRandom.current().nextDouble() * 100 >= a.n(0)) return;
                        ready.put(player.getUniqueId(), now + (long) (a.n(1) * 1000));
                        Hits.stun(landing.entity(), ROAR_TICKS);
                    }

                    @Override
                    public void forget(UUID player) {
                        ready.remove(player);
                    }
                },
                new SnowCannon(),
                // What Snow Cannon asks for.
                new Ability("SNOWMAN", "Ouch!", "Your snowballs have #% chance of dealing double damage!") {
                },
                new Laserbeam(),
                new SkeletalDefense(),
                // Hits on them.
                lessFrom("ENDERMAN", "Enderian", "Take #% less damage from ⊙ Ender mobs", by -> Ability.types(by).contains(MobType.ENDER)),
                lessFrom("WITHER_SKELETON", "Stronger Bones", "Take #% less damage from 🦴 Skeletal mobs", by -> Ability.types(by).contains(MobType.SKELETAL)),
                lessFrom("ZOMBIE", "Bite Shield", "Reduce the damage taken from zombies by #%.", Ability::zombie),
                lessFrom("WOLF", "Alpha Dog", "Take #% less damage from wolves", by -> Ability.is(by, EntityType.WOLF)),
                new Ability("BLUE_WHALE", "Ingest", "If you have any absorption active, gain +#% Damage Reduction.") {
                    // "Applies in the same way as Defense damage reduction" (the wiki's Special Damage Reduction): 100 / (100 + #).
                    @Override
                    public double taken(Player player, Active a) {
                        return Absorption.get(player) > 0 ? Damage.defenseMultiplier(a.n(0)) : 1;
                    }
                },
                new TurtleShell(),
                new Ability("TURTLE", "Unflippable", "Gain immunity to knockback") {
                    @Override
                    public double knockbackResistance(Player player, Active a, Entity by) {
                        return 1;
                    }
                },
                new Ability("SNOWMAN", "Frostbite", "Your freezing aura slows enemy attacks causing you to take #% reduced damage") {
                    @Override
                    public double taken(Player player, Active a) {
                        return 1 - a.n(0) / 100;
                    }
                },
                new Blizzard(),
                new Ricochet(),
                new Rekindle(),
                new Stampede("HORSE", "Mob kills grant a stack of +# ✦ Speed and +# ❁ Strength for #s. (Max # stacks)", Stat.SPEED),
                new Stampede("SKELETON_HORSE", "Mob kills grant a stack of +# ⚔ Attack Speed and +# ❁ Strength for #s. (Max # stacks)",
                        Stat.ATTACK_SPEED),
                new Trample("HORSE"),
                new Trample("SKELETON_HORSE"),
                new SkeletonCombo());
    }

    /** "Deal #% more damage to ⊙ Ender mobs.": additive, on hits on mobs of that type. */
    static PetAbility additiveAgainst(String pet, String name, String text, MobType type) {
        return new Ability(pet, name, text) {
            @Override
            public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
                return target.types().contains(type) ? new Combat.HitBuff(a.n(0), 1) : null;
            }
        };
    }

    /** "Take #% less damage from ⊙ Ender mobs": a factor on hits by what passes. */
    static PetAbility lessFrom(String pet, String name, String text, Predicate<Entity> by) {
        return new Ability(pet, name, text) {
            @Override
            public double takenFrom(Player player, Active a, Entity hitter) {
                return hitter != null && by.test(hitter) ? 1 - a.n(0) / 100 : 1;
            }
        };
    }

    static boolean melee(Combat.Landing landing) {
        return landing.kind() == HitKind.MELEE;
    }

    static double distance(Player player, Entity entity) {
        return player.getWorld().equals(entity.getWorld()) ? player.getLocation().distance(entity.getLocation()) : Double.MAX_VALUE;
    }

    /** A line of dust from one place to another, so an effect's damage is seen to come from somewhere. */
    static void line(Location from, Location to, Color color) {
        if (from.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;
        Vector step = to.toVector().subtract(from.toVector());
        double length = step.length();
        if (length <= 0) return;
        step.multiply(0.4 / length);
        Location at = from.clone();
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.8f);
        for (double d = 0; d < length; d += 0.4) {
            from.getWorld().spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, dust);
            at.add(step);
        }
    }

    /** The point a little above a mob's middle, where an effect aims. */
    static Location middle(LivingEntity entity) {
        return entity.getLocation().add(0, entity.getHeight() * 0.6, 0);
    }

    // ---------- hits ----------

    /**
     * The Lion's First Pounce: "First Strike, Triple-Strike, and Combo are 100% more effective": First Strike's and
     * Triple-Strike's share of the hit, again by that much, on the hits they count for (Combo's kills aren't here: UNKNOWN
     * how its "more effective" reads).
     */
    static final class FirstPounce extends Ability {
        FirstPounce() {
            super("LION", "First Pounce", "First Strike, Triple-Strike, and Combo are #% more effective.");
        }

        @Override
        public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
            Map<String, Integer> enchants = attacker.enchantments();
            double strikes = Damage.enchantment("first_strike", enchants.getOrDefault("first_strike", 0), attacker, target)
                    + Damage.enchantment("triple_strike", enchants.getOrDefault("triple_strike", 0), attacker, target);
            return strikes > 0 ? new Combat.HitBuff(strikes * a.n(0) / 100, 1) : null;
        }
    }

    /**
     * The Lion's King of the Jungle: "Deal +150% ❁ Damage against mobs that have attacked you": any mob that has hit them
     * (the latest few dozen are kept).
     */
    static final class KingOfTheJungle extends Ability {
        static final int KEPT = 64;
        private final Map<UUID, Set<UUID>> attackers = new HashMap<>();

        KingOfTheJungle() {
            super("LION", "King of the Jungle", "Deal +#% ❁ Damage against mobs that have attacked you.");
        }

        @Override
        public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
            Set<UUID> by = attackers.get(player.getUniqueId());
            return by != null && by.contains(landing.entity().getUniqueId()) ? new Combat.HitBuff(a.n(0), 1) : null;
        }

        @Override
        public void hurt(Player player, Active a, Entity by, double taken) {
            Entity mob = PlayerDamage.attacker(by);
            if (!(mob instanceof LivingEntity living) || !mob(living)) return;
            Set<UUID> kept = attackers.computeIfAbsent(player.getUniqueId(), id -> new LinkedHashSet<>());
            kept.remove(mob.getUniqueId());
            kept.add(mob.getUniqueId());
            if (kept.size() > KEPT) kept.remove(kept.iterator().next());
        }

        @Override
        public void forget(UUID player) {
            attackers.remove(player);
        }
    }

    /**
     * The Golem's Last Stand: "While at less than 20% HP, reduce incoming damage by 20%. Additionally, gain a temporary
     * shield equal to 40% of your maximum health and deal 40% more damage. (Lasts 12s, 60s cooldown)": the less damage
     * while they're that low, the shield (absorption) and more damage (multiplicative, the wiki) once a cooldown, when a
     * hit takes them there.
     */
    static final class GolemLastStand extends Ability {
        private final Map<UUID, Long> ready = new HashMap<>();
        private final Map<UUID, Long> strongUntil = new HashMap<>();

        GolemLastStand() {
            super("GOLEM", "Last Stand", "While at less than #% HP, reduce incoming damage by #%. Additionally, gain a temporary shield equal to #% "
                    + "of your maximum health and deal #% more damage. (Lasts #s, #s cooldown)");
        }

        private static boolean low(Player player, Active a) {
            return PlayerHealth.get(player) < PlayerHealth.max(player) * a.n(0) / 100;
        }

        @Override
        public double taken(Player player, Active a) {
            return low(player, a) ? 1 - a.n(1) / 100 : 1;
        }

        @Override
        public void hurt(Player player, Active a, Entity by, double taken) {
            long now = System.currentTimeMillis();
            if (!low(player, a) || now < ready.getOrDefault(player.getUniqueId(), 0L)) return;
            ready.put(player.getUniqueId(), now + (long) (a.n(5) * 1000));
            long lasts = (long) (a.n(4) * 1000);
            Absorption.give(player, source(player), PlayerHealth.max(player) * a.n(2) / 100, lasts);
            strongUntil.put(player.getUniqueId(), now + lasts);
        }

        @Override
        public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
            return System.currentTimeMillis() < strongUntil.getOrDefault(player.getUniqueId(), 0L) ? new Combat.HitBuff(0, 1 + a.n(3) / 100) : null;
        }

        @Override
        public void forget(UUID player) {
            ready.remove(player);
            strongUntil.remove(player);
        }
    }

    /**
     * The Golem's Toss: "Every 5 hits, throw the enemy up into the air and deal 5x damage (5s cooldown)": every fifth
     * melee hit, when it's ready, does 5x (multiplicative, the wiki) and throws the mob up.
     */
    static final class Toss extends Ability {
        private final Map<UUID, Integer> hits = new HashMap<>();
        private final Map<UUID, Long> ready = new HashMap<>();
        private final Map<UUID, UUID> tossing = new HashMap<>();

        Toss() {
            super("GOLEM", "Toss", "Every # hits, throw the enemy up into the air and deal #x damage (#s cooldown)");
        }

        @Override
        public Combat.HitBuff hit(Player player, Active a, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
            if (!melee(landing) || a.n(0) <= 0) return null;
            int count = hits.merge(player.getUniqueId(), 1, Integer::sum);
            long now = System.currentTimeMillis();
            if (count % (int) a.n(0) != 0 || now < ready.getOrDefault(player.getUniqueId(), 0L)) return null;
            ready.put(player.getUniqueId(), now + (long) (a.n(2) * 1000));
            tossing.put(player.getUniqueId(), landing.entity().getUniqueId());
            return new Combat.HitBuff(0, a.n(1));
        }

        @Override
        public void landed(Player player, Active a, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            UUID tossed = tossing.remove(player.getUniqueId());
            if (tossed == null || !tossed.equals(landing.entity().getUniqueId()) || killed) return;
            LivingEntity mob = landing.entity();
            // After vanilla's knockback, which would change a velocity set now (EFFECTS.md).
            Bukkit.getScheduler().runTask(net.icxd.dungeons.Dungeons.getInstance(), () -> {
                if (mob.isValid()) mob.setVelocity(mob.getVelocity().setY(1.0));
            });
        }

        @Override
        public void forget(UUID player) {
            hits.remove(player);
            ready.remove(player);
            tossing.remove(player);
        }
    }

    // ---------- once a hit lands ----------

    /**
     * The Ankylosaurus's Clubbed Tail: "Every 5th hit deals 50.0% of your final damage to enemies within 5 blocks. Enemies
     * hit deal 10% less damage for 10s.": every fifth melee hit or arrow, the share of its damage to the mobs around the
     * one it hit, which (and that one, UNKNOWN) deal less to them for a while.
     */
    static final class ClubbedTail extends Ability {
        private final Map<UUID, Integer> hits = new HashMap<>();

        ClubbedTail() {
            super("ANKYLOSAURUS", "Clubbed Tail", "Every #th hit deals #% of your final damage to enemies within # blocks. Enemies hit deal #% less damage for #s.");
        }

        @Override
        public void landed(Player player, Active a, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (!melee(landing) && landing.kind() != HitKind.ARROW || a.n(0) <= 0) return;
            if (hits.merge(player.getUniqueId(), 1, Integer::sum) % (int) a.n(0) != 0) return;
            long millis = (long) (a.n(4) * 1000);
            double weaker = 1 - a.n(3) / 100;
            if (!killed) Protection.taunted(player, landing.entity(), weaker, millis);
            for (LivingEntity mob : Hits.near(landing.entity().getLocation(), a.n(2))) {
                if (mob == landing.entity()) continue;
                MobHits.deal(player, mob, damage * a.n(1) / 100, DamageIndicators.Look.NORMAL, HitKind.OTHER, landing.weapon());
                if (MobHits.alive(mob)) Protection.taunted(player, mob, weaker, millis);
            }
        }

        @Override
        public void forget(UUID player) {
            hits.remove(player);
        }
    }

    /**
     * The Phoenix's Fourth Flare: "On 4th melee strike, ignite mobs, dealing 13.0x your ☠ Crit Damage each second for 4
     * seconds": the stat, not the hit's crit ("based on the player's Crit Damage stat", the wiki's Phoenix Pet).
     */
    static final class FourthFlare extends Ability {
        private final Map<UUID, Integer> hits = new HashMap<>();

        FourthFlare() {
            super("PHOENIX", "Fourth Flare", "On #th melee strike, ignite mobs, dealing #x your ☠ Crit Damage each second for # seconds");
        }

        @Override
        public void landed(Player player, Active a, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (!melee(landing) || a.n(0) <= 0 || killed) return;
            if (hits.merge(player.getUniqueId(), 1, Integer::sum) % (int) a.n(0) != 0) return;
            double each = a.n(1) * Math.max(0, PlayerSession.of(player).stats().get(Stat.CRIT_DAMAGE));
            int seconds = (int) a.n(2);
            MobDebuffs.dot(landing.entity(), source(player), player, each, 20, seconds, DamageIndicators.Look.FIRE);
            landing.entity().setFireTicks(seconds * 20);
        }

        @Override
        public void forget(UUID player) {
            hits.remove(player);
        }
    }

    /**
     * The Snowman's Snow Cannon: "Shoots a snowball towards an enemy when you attack dealing 20.0% of your last dealt melee
     * damage, capped at 200,000. (1s cooldown)": at the mob the melee hit landed on, within 8 blocks; with Ouch!, a chance
     * of double ("Your snowballs have 50% chance of dealing double damage!").
     */
    static final class SnowCannon extends Ability {
        private final Map<UUID, Long> ready = new HashMap<>();

        SnowCannon() {
            super("SNOWMAN", "Snow Cannon", "Shoots a snowball towards an enemy when you attack dealing #% of your last dealt melee damage, capped at #. (#s cooldown)");
        }

        @Override
        public void landed(Player player, Active a, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (!melee(landing) || killed || distance(player, landing.entity()) > SNOW_CANNON_RANGE) return;
            long now = System.currentTimeMillis();
            if (now < ready.getOrDefault(player.getUniqueId(), 0L)) return;
            ready.put(player.getUniqueId(), now + (long) (a.n(2) * 1000));
            double snowball = Math.min(damage * a.n(0) / 100, a.n(1));
            for (PetAbility.Active ouch : PetAbilities.active(player)) {
                if (ouch.ability().pet().equals("SNOWMAN") && ouch.ability().name().equals("Ouch!")
                        && ThreadLocalRandom.current().nextDouble() * 100 < ouch.n(0)) snowball *= 2;
            }
            LivingEntity mob = landing.entity();
            line(player.getEyeLocation(), middle(mob), Color.WHITE);
            mob.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, middle(mob), 8, 0.2, 0.2, 0.2, 0);
            MobHits.deal(player, mob, snowball, DamageIndicators.Look.NORMAL, HitKind.OTHER, landing.weapon());
        }

        @Override
        public void forget(UUID player) {
            ready.remove(player);
        }
    }

    /**
     * The Guardian's Laserbeam: "Zaps your enemies for 120.0x your ✎ Intelligence every 3s": the mob their last melee hit
     * or arrow landed on in that time ("Ability damage does not provoke the Guardian's Lazerbeam on the targets they hit",
     * the wiki), within reach; not on Private Islands ("disabled on Private Islands"), nor with no Intelligence.
     */
    static final class Laserbeam extends Ability {
        private final Map<UUID, UUID> target = new HashMap<>();
        private final Map<UUID, Long> hitAt = new HashMap<>();
        private final Map<UUID, Integer> seconds = new HashMap<>();

        Laserbeam() {
            super("GUARDIAN", "Laserbeam", "Zaps your enemies for #x your ✎ Intelligence every #s.");
        }

        @Override
        public void landed(Player player, Active a, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (killed || !melee(landing) && landing.kind() != HitKind.ARROW) return;
            this.target.put(player.getUniqueId(), landing.entity().getUniqueId());
            hitAt.put(player.getUniqueId(), System.currentTimeMillis());
        }

        @Override
        public void second(Player player, Active a) {
            int every = Math.max(1, (int) Math.round(a.n(1)));
            if (seconds.merge(player.getUniqueId(), 1, Integer::sum) % every != 0 || onOwnIsland(player)) return;
            UUID id = target.get(player.getUniqueId());
            if (id == null || System.currentTimeMillis() - hitAt.getOrDefault(player.getUniqueId(), 0L) > every * 1000L) return;
            double intelligence = PlayerSession.of(player).stats().get(Stat.INTELLIGENCE);
            if (!(Bukkit.getEntity(id) instanceof LivingEntity mob) || !mob(mob) || intelligence <= 0 || distance(player, mob) > LASER_RANGE) return;
            line(player.getEyeLocation(), middle(mob), Color.AQUA);
            MobHits.deal(player, mob, a.n(0) * intelligence, DamageIndicators.Look.NORMAL, HitKind.OTHER, null);
        }

        @Override
        public void ended(Player player) {
            forget(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            target.remove(player);
            hitAt.remove(player);
            seconds.remove(player);
        }
    }

    /**
     * The Skeleton's Skeletal Defense: "Your skeleton shoots an arrow dealing 30x your ☠ Crit Damage when a mob gets close
     * to you (5s cooldown)": at the nearest mob within a few blocks, looked for once a second.
     */
    static final class SkeletalDefense extends Ability {
        private final Map<UUID, Long> ready = new HashMap<>();

        SkeletalDefense() {
            super("SKELETON", "Skeletal Defense", "Your skeleton shoots an arrow dealing #x your ☠ Crit Damage when a mob gets close to you (#s cooldown).");
        }

        @Override
        public void second(Player player, Active a) {
            long now = System.currentTimeMillis();
            if (now < ready.getOrDefault(player.getUniqueId(), 0L)) return;
            List<LivingEntity> close = Hits.near(player.getLocation(), CLOSE);
            if (close.isEmpty()) return;
            ready.put(player.getUniqueId(), now + (long) (a.n(1) * 1000));
            LivingEntity mob = close.get(0);
            line(player.getEyeLocation().add(0, 0.5, 0), middle(mob), Color.SILVER);
            MobHits.deal(player, mob, a.n(0) * Math.max(0, PlayerSession.of(player).stats().get(Stat.CRIT_DAMAGE)), DamageIndicators.Look.NORMAL,
                    HitKind.OTHER, null);
        }

        @Override
        public void forget(UUID player) {
            ready.remove(player);
        }
    }

    // ---------- hits on them ----------

    /**
     * The Turtle's Turtle Shell: "When under 40% maximum HP, you take 25.00% less damage. Gain +20.0♨ Vitality after taking
     * 10 hits.": the Vitality for a while (UNKNOWN how long), each time the hits come round.
     */
    static final class TurtleShell extends Ability {
        private final Map<UUID, Integer> hits = new HashMap<>();

        TurtleShell() {
            super("TURTLE", "Turtle Shell", "When under #% maximum HP, you take #% less damage. Gain +#♨ Vitality after taking # hits.");
        }

        @Override
        public double taken(Player player, Active a) {
            return PlayerHealth.get(player) < PlayerHealth.max(player) * a.n(0) / 100 ? 1 - a.n(1) / 100 : 1;
        }

        @Override
        public void hurt(Player player, Active a, Entity by, double taken) {
            if (a.n(3) <= 0 || hits.merge(player.getUniqueId(), 1, Integer::sum) % (int) a.n(3) != 0) return;
            PlayerSession.of(player).buff(source(player), new Stats().add(Stat.VITALITY, a.n(2)), SHELL_MILLIS);
        }

        @Override
        public void forget(UUID player) {
            hits.remove(player);
        }
    }

    /**
     * The Snowman's Blizzard: "Enemies within 16.0 blocks are slowed by 25% and deal 10.0% less damage": put on them once a
     * second, for a little longer.
     */
    static final class Blizzard extends Ability {
        Blizzard() {
            super("SNOWMAN", "Blizzard", "Enemies within # blocks are slowed by #% and deal #% less damage.");
        }

        @Override
        public void second(Player player, Active a) {
            for (LivingEntity mob : Hits.near(player.getLocation(), a.n(0))) {
                MobDebuffs.add(mob, new Debuffs.Spec(source(player), Debuffs.Kind.SLOW, a.n(1) / 100, 1, BLIZZARD_MILLIS), player);
                Protection.taunted(player, mob, 1 - a.n(2) / 100, BLIZZARD_MILLIS);
            }
        }
    }

    /**
     * The Golem's Ricochet: "Your iron plating causes 25.0% of attacks to ricochet and hit the attacker": what the hit took
     * from them, back at the mob behind it (UNKNOWN how much it hits for).
     */
    static final class Ricochet extends Ability {
        Ricochet() {
            super("GOLEM", "Ricochet", "Your iron plating causes #% of attacks to ricochet and hit the attacker");
        }

        @Override
        public void hurt(Player player, Active a, Entity by, double taken) {
            if (!(PlayerDamage.attacker(by) instanceof LivingEntity mob) || !mob(mob)) return;
            if (ThreadLocalRandom.current().nextDouble() * 100 >= a.n(0)) return;
            mob.getWorld().spawnParticle(Particle.CRIT, middle(mob), 8, 0.2, 0.2, 0.2, 0.1);
            MobHits.deal(player, mob, taken, DamageIndicators.Look.NORMAL, HitKind.OTHER, null);
        }
    }

    /**
     * The Phoenix's Rekindle: "Before death, become immune and gain 20.0 ❁ Strength for 4 seconds 60s cooldown": a hit
     * that would kill them does nothing, and for those seconds nothing hurts them.
     */
    static final class Rekindle extends Ability {
        private final Map<UUID, Long> ready = new HashMap<>();

        Rekindle() {
            super("PHOENIX", "Rekindle", "Before death, become immune and gain # ❁ Strength for # seconds #s cooldown");
        }

        @Override
        public double shield(Player player, Active a, double taken, Entity by) {
            long now = System.currentTimeMillis();
            if (taken < PlayerHealth.get(player) + Absorption.get(player) || now < ready.getOrDefault(player.getUniqueId(), 0L)) return taken;
            ready.put(player.getUniqueId(), now + (long) (a.n(2) * 1000));
            long lasts = (long) (a.n(1) * 1000);
            Protection.immunity(player, source(player), lasts);
            PlayerSession.of(player).buff(source(player), new Stats().add(Stat.STRENGTH, a.n(0)), lasts);
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
            return 0;
        }

        @Override
        public void forget(UUID player) {
            ready.remove(player);
        }
    }

    // ---------- stacks ----------

    /**
     * The Horses' Stampede: "Mob kills grant a stack of +20.0 ✦ Speed and +5.00 ❁ Strength for 5s. (Max 20 stacks)":
     * each stack its own time (UNKNOWN), the oldest going when there'd be more.
     */
    static final class Stampede extends Ability {
        private final Stat first;
        private final Map<UUID, Deque<Long>> stacks = new HashMap<>();

        Stampede(String pet, String text, Stat first) {
            super(pet, "Stampede", text);
            this.first = first;
        }

        private int stacks(Player player) {
            Deque<Long> ends = stacks.get(player.getUniqueId());
            if (ends == null) return 0;
            long now = System.currentTimeMillis();
            while (!ends.isEmpty() && ends.peekFirst() <= now) ends.pollFirst();
            return ends.size();
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            int n = stacks(player);
            if (n > 0) stats.add(first, a.n(0) * n).add(Stat.STRENGTH, a.n(1) * n);
        }

        @Override
        public void killed(Player player, Active a, net.icxd.dungeons.mob.SkyBlockMobDeathEvent event) {
            Deque<Long> ends = stacks.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>());
            ends.addLast(System.currentTimeMillis() + (long) (a.n(2) * 1000));
            while (ends.size() > a.n(3)) ends.pollFirst();
            PlayerSession.of(player).invalidateStats();
        }

        @Override
        public void forget(UUID player) {
            stacks.remove(player);
        }
    }

    /**
     * The Skeleton's Combo: "Gain a combo stack for every bow hit granting +3 ❁ Strength. Max 20.0 stacks, stacks disappear
     * after 8 seconds.": all of them, that long after the last bow hit.
     */
    static final class SkeletonCombo extends Ability {
        private final Map<UUID, Integer> stacks = new HashMap<>();
        private final Map<UUID, Long> lastHit = new HashMap<>();

        SkeletonCombo() {
            super("SKELETON", "Combo", "Gain a combo stack for every bow hit granting +# ❁ Strength. Max # stacks, stacks disappear after # seconds.");
        }

        @Override
        public void stats(Player player, Active a, Stats stats) {
            Long last = lastHit.get(player.getUniqueId());
            if (last == null) return;
            if (System.currentTimeMillis() - last > a.n(2) * 1000) {
                forget(player.getUniqueId());
                return;
            }
            stats.add(Stat.STRENGTH, a.n(0) * stacks.getOrDefault(player.getUniqueId(), 0));
        }

        @Override
        public void landed(Player player, Active a, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (landing.kind() != HitKind.ARROW) return;
            stacks.merge(player.getUniqueId(), 1, (s, one) -> Math.min((int) a.n(1), s + one));
            lastHit.put(player.getUniqueId(), System.currentTimeMillis());
            PlayerSession.of(player).invalidateStats();
        }

        @Override
        public void forget(UUID player) {
            stacks.remove(player);
            lastHit.remove(player);
        }
    }

    /**
     * The Horses' Trample: "After falling 20 or more blocks, absorb your fall damage and deal 5.0% of your weapon's ❁
     * Damage for every block fallen to mobs within 3 blocks.": the fall hurts them none, and what they hold's Damage
     * stat counts.
     */
    static final class Trample extends Ability {
        Trample(String pet) {
            super(pet, "Trample", "After falling # or more blocks, absorb your fall damage and deal #% of your weapon's ❁ Damage for every block fallen to mobs within # blocks.");
        }

        @Override
        public double vanillaDamage(Player player, Active a, VanillaDamage.Cause cause) {
            return cause == VanillaDamage.Cause.FALL && player.getFallDistance() >= a.n(0) ? 0 : 1;
        }

        @Override
        public void fell(Player player, Active a, float blocks) {
            if (blocks < a.n(0)) return;
            double weapon = ItemStats.of(player.getInventory().getItemInMainHand(), player).get(Stat.DAMAGE);
            double damage = a.n(1) / 100 * weapon * Math.floor(blocks);
            if (damage <= 0) return;
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 30, a.n(2) / 2, 0.1, a.n(2) / 2, 0,
                    Material.DIRT.createBlockData());
            for (LivingEntity mob : Hits.near(player.getLocation(), a.n(2))) {
                MobHits.deal(player, mob, damage, DamageIndicators.Look.NORMAL, HitKind.OTHER, Combat.skyBlockData(player.getInventory().getItemInMainHand()));
            }
        }
    }
}
