package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.Absorption;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Text;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Combat sets whose bonuses do more than stats, their numbers from their text: Fervor's Intimidate, Golem's
 * Absorption, Bouncy's Bouncing Arrow, Thermodynamic's Newton's Demise, Werewolf's Regenerative Howl, Crystal's
 * Refraction, Starlight's Starpower (the Starlight Wand's Starfall asks for it, see {@link SetBonuses#starfallDuration}),
 * Nutcracker's Battalion (its aura; the gifts and Jerry's Workshop aren't here) and Great Spook's Fearsome.
 */
final class CombatSets {
    private CombatSets() {
    }

    static List<Bonus> all() {
        return List.of(new Intimidate(), new GolemAbsorption(), new BouncingArrow(), new NewtonsDemise(), new RegenerativeHowl(),
                new Refraction(), new Starpower(), new Battalion(), new Fearsome());
    }

    /** A full set bonus. */
    abstract static class FullSet implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }
    }

    /** One of SkyBlock's mobs that has a mind of its own to change (the Blood Room's undead steer themselves). */
    static boolean aggroable(Entity entity) {
        return entity instanceof Mob mob && !mob.isDead() && (Mobs.of(mob) != null || DungeonMobs.of(mob) != null);
    }

    /**
     * Fervor Armor's Intimidate: "Attacking a mob has a 50% chance to direct aggro from 3 mobs to you": a melee
     * hit or an arrow (UNKNOWN whether abilities' hits count), then the nearest mobs within 10 blocks that aren't
     * after them already (UNKNOWN both: the wiki doesn't say which or how far; 10 is the Enrager's taunt's).
     */
    static final class Intimidate extends FullSet {
        static final double RANGE = 10;

        @Override
        public String name() {
            return "Intimidate";
        }

        @Override
        public void landed(Player player, Active active, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (landing.kind() != HitKind.MELEE && landing.kind() != HitKind.ARROW) return;
            ItemBlock block = BonusText.block(active);
            if (ThreadLocalRandom.current().nextDouble() * 100 >= BonusText.after(block, "has a", 50)) return;
            int most = (int) BonusText.after(block, "from", 3);
            List<Mob> mobs = new ArrayList<>();
            for (Entity entity : player.getNearbyEntities(RANGE, RANGE, RANGE)) {
                if (aggroable(entity) && ((Mob) entity).getTarget() != player
                        && entity.getLocation().distanceSquared(player.getLocation()) <= RANGE * RANGE) mobs.add((Mob) entity);
            }
            mobs.sort(Comparator.comparingDouble(m -> m.getLocation().distanceSquared(player.getLocation())));
            for (int i = 0; i < Math.min(most, mobs.size()); i++) mobs.get(i).setTarget(player);
        }
    }

    /**
     * Golem Armor's Absorption: "Grants the wearer Absorption 3 for 20 seconds when they kill an enemy":
     * Absorption III is 60 of absorption (the wiki's Golem Armor history: "Ability gives Absorption 3 instead of 2
     * (60 HP instead of 40)"), given again (not added) by the next kill.
     */
    static final class GolemAbsorption extends FullSet {
        /** Absorption a level of it gives, the wiki's 60 for III. */
        static final double PER_LEVEL = 20;

        @Override
        public String name() {
            return "Absorption";
        }

        @Override
        public void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
            ItemBlock block = BonusText.block(active);
            Absorption.give(player, "Golem Armor", PER_LEVEL * BonusText.after(block, "Absorption", 3), (long) BonusText.millis(block, 20_000));
        }
    }

    /**
     * Bouncy Armor's Bouncing Arrow: "Your arrows have a 25% chance to bounce to another target after it hits
     * something": the damage the arrow did, again, to the nearest other mob within 10 blocks of the one it hit, as
     * the bounce's own (not an arrow's hit again: it doesn't bounce on). How far and how hard are UNKNOWN (the
     * wiki's Bouncy Armor doesn't say); a trail of crits shows the way.
     */
    static final class BouncingArrow extends FullSet {
        static final double RANGE = 10;

        @Override
        public String name() {
            return "Bouncing Arrow";
        }

        @Override
        public void landed(Player player, Active active, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (landing.kind() != HitKind.ARROW || damage <= 0) return;
            if (ThreadLocalRandom.current().nextDouble() * 100 >= BonusText.after(BonusText.block(active), "have a", 25)) return;
            LivingEntity from = landing.entity();
            LivingEntity to = nearestOther(from, RANGE);
            if (to == null) return;
            trail(from.getLocation().add(0, from.getHeight() / 2, 0), to.getLocation().add(0, to.getHeight() / 2, 0));
            MobHits.deal(player, to, damage, landing.critical() ? DamageIndicators.Look.CRITICAL : DamageIndicators.Look.NORMAL, HitKind.OTHER,
                    landing.weapon());
        }

        /** The nearest mob that can be hurt within {@code range} of this one (not it); null for none. */
        static LivingEntity nearestOther(LivingEntity from, double range) {
            LivingEntity nearest = null;
            double best = range * range;
            for (Entity entity : from.getNearbyEntities(range, range, range)) {
                if (!(entity instanceof LivingEntity living) || entity instanceof Player || !MobHits.hittable(living)) continue;
                double distance = living.getLocation().distanceSquared(from.getLocation());
                if (distance <= best) {
                    best = distance;
                    nearest = living;
                }
            }
            return nearest;
        }

        private static void trail(Location from, Location to) {
            Vector step = to.toVector().subtract(from.toVector());
            int points = (int) Math.max(1, step.length() * 2);
            step.multiply(1.0 / points);
            Location at = from.clone();
            for (int i = 0; i <= points; i++) {
                at.getWorld().spawnParticle(Particle.CRIT, at, 1, 0, 0, 0, 0);
                at.add(step);
            }
        }
    }

    /** Thermodynamic Armor's Newton's Demise: "Grants +50⚔ Attack Speed Cap." */
    static final class NewtonsDemise extends FullSet {
        @Override
        public String name() {
            return "Newton's Demise";
        }

        @Override
        public double attackSpeedCap(Player player, Active active) {
            return BonusText.after(BonusText.block(active), "Grants", 50);
        }
    }

    /**
     * Werewolf Armor's Regenerative Howl: "Upon activating ⫽ Ferocity gain 50❈ Defense for 5s (up to 10 stacks)":
     * each Ferocity strike is an activation (UNKNOWN whether a hit's several strikes are one), each stack its own 5
     * seconds, the newest 10 counting. With each, "heal players within 25 blocks for 1% of your Defense" (the
     * wiki's Werewolf Armor, nerfed from 10% on 2022/Dec 8; the item's text doesn't have it: UNKNOWN whether it's
     * still there), not the wearer ("does not heal the wearer, only players around them", its trivia), as a heal
     * from them (their Mending counts).
     */
    static final class RegenerativeHowl extends FullSet {
        static final double HEAL_RANGE = 25;
        static final double HEAL_SHARE = 0.01;
        private final Map<UUID, Deque<Long>> stacks = new HashMap<>();

        @Override
        public String name() {
            return "Regenerative Howl";
        }

        @Override
        public void landed(Player player, Active active, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (landing.kind() != HitKind.FEROCITY) return;
            ItemBlock block = BonusText.block(active);
            long now = System.currentTimeMillis();
            add(stacks.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>()), now + (long) BonusText.millis(block, 5_000),
                    (int) BonusText.after(block, "up to", 10));
            double heal = HEAL_SHARE * PlayerSession.of(player).stats().get(Stat.DEFENSE);
            if (heal <= 0) return;
            for (Player other : player.getWorld().getPlayers()) {
                if (other.equals(player) || !SetBonuses.inPlay(other)) continue;
                if (other.getLocation().distanceSquared(player.getLocation()) <= HEAL_RANGE * HEAL_RANGE) PlayerHealth.healFrom(player, other, heal);
            }
        }

        /** A stack until {@code until}; the oldest go past {@code most}. */
        static void add(Deque<Long> stacks, long until, int most) {
            stacks.addLast(until);
            while (stacks.size() > Math.max(1, most)) stacks.removeFirst();
        }

        /** How many are still going at {@code now}; the rest go. */
        static int count(Deque<Long> stacks, long now) {
            stacks.removeIf(until -> until <= now);
            return stacks.size();
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            Deque<Long> own = stacks.get(player.getUniqueId());
            if (own == null) return;
            int n = count(own, System.currentTimeMillis());
            if (n > 0) stats.add(Stat.DEFENSE, n * BonusText.after(BonusText.block(active), "gain", 50));
        }

        @Override
        public void ended(Player player) {
            stacks.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            stacks.remove(player);
        }
    }

    /**
     * Crystal Armor's Refraction: "The stats of this armor change from 0 to 200% depending on the current light
     * level": each piece's Defense and Intelligence (the wiki's "Light Levels": 12.5% at light 0, 12.5% more a
     * level, 200% at 15; UNKNOWN whether the text's "from 0" means none at 0), at the light where their head is.
     * Its "Current Light Level" line shows theirs, refreshed when it changes; the armor's colour doesn't change
     * (cosmetic).
     */
    static final class Refraction extends FullSet {
        private final Map<UUID, Integer> shown = new HashMap<>();

        @Override
        public String name() {
            return "Refraction";
        }

        /** The share of the pieces' stats they have at this light level. */
        static double share(int light) {
            return (Math.max(0, Math.min(light, 15)) + 1) * 0.125;
        }

        static int light(Player player) {
            return player.getEyeLocation().getBlock().getLightLevel();
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            double more = share(light(player)) - 1;
            for (Worn.Piece piece : active.pieces()) {
                Stats own = ItemStats.of(piece.stack(), player);
                stats.add(Stat.DEFENSE, own.get(Stat.DEFENSE) * more).add(Stat.INTELLIGENCE, own.get(Stat.INTELLIGENCE) * more);
            }
        }

        @Override
        public List<String> text(List<String> text, int count, Player holder) {
            return lightLine(text, light(holder));
        }

        /** The "0 (0%)" line under "Current Light Level" for this light level: "&c7&8 (100%)". */
        static List<String> lightLine(List<String> text, int light) {
            List<String> out = new ArrayList<>(text);
            for (int i = 1; i < out.size(); i++) {
                if (!out.get(i - 1).contains("Current Light Level")) continue;
                out.set(i, "&c" + light + "&8 (" + Text.number(share(light) * 100) + "%)");
                break;
            }
            return out;
        }

        @Override
        public void second(Player player, Active active) {
            int light = light(player);
            Integer before = shown.put(player.getUniqueId(), light);
            if (before != null && before != light) SetBonuses.refreshLore(player);
        }

        @Override
        public void ended(Player player) {
            shown.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            shown.remove(player);
        }
    }

    /**
     * Starlight Armor's Starpower: "Increases the duration of Starfall by 2x and its range by +1". The Starlight
     * Wand's Starfall asks for both ({@link SetBonuses#starfallDuration}, {@link SetBonuses#starfallRange}); the
     * range is how far away it can be put down (UNKNOWN: it could be the area's size).
     */
    static final class Starpower extends FullSet {
        @Override
        public String name() {
            return "Starpower";
        }

        double duration(Active active) {
            return BonusText.after(BonusText.block(active), "by", 2);
        }

        double range(Active active) {
            return BonusText.after(BonusText.block(active), "range by", 1);
        }
    }

    /**
     * Great Spook Armor's Fearsome: "Monsters at or below your total Fear level will run away from you": SkyBlock's
     * mobs whose level (as their name shows it) is at most their Fear don't go for them (see
     * TargetNearestPlayerGoal#addIgnored), and those within 10 blocks run 8 blocks further off, looked at again each
     * second (UNKNOWN both distances: the wiki's Great Spook Armor gives none).
     */
    static final class Fearsome extends FullSet {
        static final double NEAR = 10;
        static final double AWAY = 8;

        @Override
        public String name() {
            return "Fearsome";
        }

        /** Whether a mob of this level runs from someone with this much Fear. */
        static boolean afraid(int level, double fear) {
            return fear > 0 && level <= fear;
        }

        private static int level(Entity entity) {
            Mobs.Live live = Mobs.of(entity);
            return live == null ? Integer.MAX_VALUE : live.type().getLevel();
        }

        /** Whether this mob leaves them alone: they wear the set and it's afraid of them. */
        static boolean ignores(LivingEntity mob, Player player) {
            return SetBonuses.active(player, "Fearsome") && afraid(level(mob), PlayerSession.of(player).stats().get(Stat.FEAR));
        }

        @Override
        public void second(Player player, Active active) {
            double fear = PlayerSession.of(player).stats().get(Stat.FEAR);
            if (fear <= 0) return;
            for (Entity entity : player.getNearbyEntities(NEAR, NEAR, NEAR)) {
                if (!(entity instanceof Mob mob) || Mobs.of(mob) == null || !afraid(level(mob), fear)) continue;
                Vector away = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
                if (away.lengthSquared() == 0) away = new Vector(1, 0, 0);
                if (mob.getTarget() == player) mob.setTarget(null);
                mob.getPathfinder().moveTo(mob.getLocation().add(away.normalize().multiply(AWAY)), 1.2);
            }
        }
    }

    /**
     * Nutcracker Armor's Battalion: "Grants +1✯ Magic Find and +50❈ Defense to everyone within 30 blocks. Stacks
     * up to 5 times": for each wearer near them, them included (UNKNOWN, as for Shoal), up to 5. Its gifts and
     * Jerry's Workshop aren't here. The numbers are the text's, as the last wearer's second read it.
     */
    static final class Battalion extends FullSet {
        private double range = 30;
        private int most = 5;
        private Stats each = new Stats().set(Stat.MAGIC_FIND, 1).set(Stat.DEFENSE, 50);

        @Override
        public String name() {
            return "Battalion";
        }

        @Override
        public double auraRange() {
            return range;
        }

        @Override
        public int auraStacks() {
            return most;
        }

        @Override
        public void aura(Player player, Stats stats, int wearers) {
            for (Stat stat : Stat.values()) if (each.has(stat)) stats.add(stat, each.get(stat) * wearers);
        }

        @Override
        public void second(Player player, Active active) {
            ItemBlock block = BonusText.block(active);
            if (block == null) return;
            range = BonusText.blocks(block, 30);
            most = (int) BonusText.after(block, "up to", 5);
            Stats read = new Stats();
            read.add(Stat.MAGIC_FIND, BonusText.after(block, "Grants", 1)).add(Stat.DEFENSE, BonusText.after(block, "and", 50));
            each = read;
        }
    }
}
