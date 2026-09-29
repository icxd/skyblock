package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.combat.Shots;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleFunction;

/**
 * Tiered bonuses, whose numbers grow with how many pieces are worn (the wiki's Tiered Bonus Values;
 * items' text has the first tier's): Arachne's Faithful, Berserk and Long Tuba's stats, Thunder's Static
 * Charge, and the Kuudra sets' stacks (Crimson's Dominus and its swipe, Terror's Hydra Strike and its extra
 * arrows; Fervor's, kept for the Fervor Chestplate's Ground Pound; Aurora's Arcane Energy for its Homing
 * Missiles and Hollow's Spirit for the Hollow Wand, both of them abilities). Tiered bonuses that are only
 * text here, but whose least pieces the wiki gives, are {@link LoreOnly} so their headers turn gold where
 * they should.
 */
final class TieredSets {
    /** Kuudra armor's tiers, lowest first, by their items' prefix ("" for the basic one). */
    private static final List<String> KUUDRA_TIERS = List.of("", "HOT_", "BURNING_", "FIERY_", "INFERNAL_");
    /** Stacks gained, and lost after this long without gaining one, for 2, 3 and 4 pieces. */
    static final Tiers STACK_EVERY = new Tiers(2, 1.5, 1, 0.5);
    static final Tiers STACK_LASTS = new Tiers(2, 4, 7, 10);

    private TieredSets() {
    }

    static List<Bonus> all() {
        return List.of(new ArachnesFaithful(), new Berserk(), new LongTuba(), new Dominus(), new HydraStrike(), new ArcaneEnergy(),
                new Fervor(), new Spirit(), new StaticCharge(),
                // The least pieces of the wiki's tiered bonuses that do nothing here yet (its tiered_bonus_required_pieces).
                new LoreOnly("Peace Treaty", 2), new LoreOnly("Lord's Blessing", 4), new LoreOnly("Unearthed", 2),
                new LoreOnly("Familiarity", 2), new LoreOnly("Mythological Greed", 2), new Rekindle());
    }

    /** A tiered bonus. */
    abstract static class Tiered implements Bonus {
        private final int needs;

        Tiered(int needs) {
            this.needs = needs;
        }

        @Override
        public String kind() {
            return SetKey.TIERED;
        }

        @Override
        public int needs(SetKey set) {
            return needs;
        }

        int needs() {
            return needs;
        }
    }

    /** A number in a bonus's text, after {@code before} ("Every &a" in "Every &a1.5s"), that grows as {@code tiers} say. */
    record Grows(String before, Tiers tiers) {
    }

    /** A tiered bonus that's only text here: it counts from {@code needs} pieces, and these numbers in its text grow. */
    static final class LoreOnly extends Tiered {
        private final String name;
        private final List<Grows> numbers;

        LoreOnly(String name, int needs, Grows... numbers) {
            super(needs);
            this.name = name;
            this.numbers = List.of(numbers);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<String> text(List<String> text, int count) {
            for (Grows number : numbers) text = number.tiers().text(text, number.before(), count, needs());
            return text;
        }
    }

    /** Arachne's Armor (and equipment): "Grants +5 Health and +5 Defense", +5, 10, 20, 35, 50, 70 and 100 from 2 to 8 pieces. */
    static final class ArachnesFaithful extends Tiered {
        static final Tiers BONUS = new Tiers(2, 5, 10, 20, 35, 50, 70, 100);

        ArachnesFaithful() {
            super(2);
        }

        @Override
        public String name() {
            return "Arachne's Faithful";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            stats.add(Stat.HEALTH, BONUS.at(active.count())).add(Stat.DEFENSE, BONUS.at(active.count()));
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return BONUS.text(BONUS.text(text, "&c+", count, needs()), "&a+", count, needs());
        }
    }

    /**
     * Berserker Armor's Berserk: "Reduces your health and defense by half and reduces the cost of
     * health-consuming abilities by 20%" (40% with 3 pieces, 60% with 4): a factor on the health costs
     * abilities take (see Abilities#addHealthCostFactor).
     */
    static final class Berserk extends Tiered {
        static final Tiers COST = new Tiers(2, 20, 40, 60);

        Berserk() {
            super(2);
        }

        @Override
        public String name() {
            return "Berserk";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            stats.set(Stat.HEALTH, stats.get(Stat.HEALTH) / 2).set(Stat.DEFENSE, stats.get(Stat.DEFENSE) / 2);
        }

        @Override
        public double healthCost(Player player, Active active) {
            return cost(active.count());
        }

        /** What's left of a health cost with this many pieces on. */
        static double cost(int pieces) {
            return Math.max(0, 1 - COST.at(pieces) / 100);
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return COST.text(text, "by &c", count, needs());
        }
    }

    /** Snorkeling Armor's Long Tuba: "Grants +2 Respiration", +0, 2, 5 and 10 from 1 to 4 pieces, from 2. */
    static final class LongTuba extends Tiered {
        static final Tiers RESPIRATION = new Tiers(1, 0, 2, 5, 10);

        LongTuba() {
            super(2);
        }

        @Override
        public String name() {
            return "Long Tuba";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            stats.add(Stat.RESPIRATION, RESPIRATION.at(active.count()));
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return RESPIRATION.text(text, "&3+", count, needs());
        }
    }

    /**
     * Stacks gained by hits, at most one each {@code every} and up to 10, one lost for each
     * {@code lasts} without gaining one (the Kuudra sets': "Lose 1 stack after 4s of not gaining a stack").
     */
    static final class Stacks {
        private int stacks;
        private long gained = Long.MIN_VALUE / 2;
        private double lasts = 4;

        /** How many there are at {@code now}. */
        int at(long now, double lastsSeconds) {
            long lost = (long) ((now - gained) / (lastsSeconds * 1000));
            return (int) Math.max(0, stacks - Math.max(0, lost));
        }

        /** How many there are at {@code now}, each lasting as long as it did at the last hit. */
        int at(long now) {
            return at(now, lasts);
        }

        /** A hit at {@code now}: a stack if the last was gained at least {@code everySeconds} ago. */
        void hit(long now, double everySeconds, double lastsSeconds) {
            hit(now, everySeconds, lastsSeconds, 10);
        }

        /** The same, up to {@code most}; whether it gained one. */
        boolean hit(long now, double everySeconds, double lastsSeconds, int most) {
            lasts = lastsSeconds;
            if (now - gained < everySeconds * 1000) return false;
            stacks = Math.min(most, at(now, lastsSeconds) + 1);
            gained = now;
            return true;
        }

        /** Spends up to {@code n} of them at {@code now}; the rest go on losing one after {@code lasts} again. Returns how many. */
        int spend(long now, int n) {
            int have = at(now);
            int spent = Math.max(0, Math.min(n, have));
            stacks = have - spent;
            gained = now;
            return spent;
        }
    }

    /** The lowest Kuudra tier of these pieces (0 for the basic one, 4 for Infernal). */
    static int lowestTier(List<Worn.Piece> pieces) {
        int lowest = KUUDRA_TIERS.size() - 1;
        for (Worn.Piece piece : pieces) {
            int tier = 0;
            for (int t = KUUDRA_TIERS.size() - 1; t > 0; t--) {
                if (piece.id().startsWith(KUUDRA_TIERS.get(t))) {
                    tier = t;
                    break;
                }
            }
            lowest = Math.min(lowest, tier);
        }
        return lowest;
    }

    /**
     * Crimson Armor's Dominus (2+ pieces): melee hits gain a stack at most every 1.5s (1s with 3 pieces,
     * 0.5s with 4), lost one at a time after 4s (7s, 10s) without gaining one; each stack grants +0.05
     * Swing Range (basic), +0.1 (Hot), +0.1 and +1 Ferocity (Burning), +0.1 and +2 Ferocity (Fiery), and
     * with those +10% Damage, additive (Infernal), of the lowest tier worn (the wiki's Crimson Armor).
     * At 10 stacks a melee hit also swipes (see {@link #swipe}), at most once each time a stack could be gained
     * (UNKNOWN how often Hypixel's does).
     */
    static final class Dominus extends Tiered {
        private static final double[] SWING_RANGE = {0.05, 0.1, 0.1, 0.1, 0.1};
        private static final double[] FEROCITY = {0, 0, 1, 2, 2};
        private static final double[] DAMAGE = {0, 0, 0, 0, 10};
        /** How far a swipe goes and how far from its line it hits: UNKNOWN (the wiki's Crimson Armor doesn't say). */
        static final double SWIPE_LENGTH = 5;
        static final double SWIPE_WIDTH = 1.2;
        private final Map<UUID, Stacks> stacks = new HashMap<>();
        private final Map<UUID, Long> swiped = new HashMap<>();

        Dominus() {
            super(2);
        }

        @Override
        public String name() {
            return "Dominus";
        }

        private int stacks(Player player, Active active) {
            Stacks s = stacks.get(player.getUniqueId());
            return s == null ? 0 : s.at(System.currentTimeMillis(), STACK_LASTS.at(active.count()));
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            int n = stacks(player, active);
            if (n == 0) return;
            int tier = lowestTier(active.pieces());
            stats.add(Stat.SWING_RANGE, SWING_RANGE[tier] * n).add(Stat.FEROCITY, FEROCITY[tier] * n);
        }

        /** The stacks it had before the hit count for it; the hit may gain one (UNKNOWN which Hypixel counts). */
        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            if (ranged) return null;
            int before = stacks(player, active);
            stacks.computeIfAbsent(player.getUniqueId(), id -> new Stacks())
                    .hit(System.currentTimeMillis(), STACK_EVERY.at(active.count()), STACK_LASTS.at(active.count()));
            double damage = DAMAGE[lowestTier(active.pieces())] * before;
            return damage > 0 ? new Combat.HitBuff(damage, 1) : null;
        }

        @Override
        public void landed(Player player, Active active, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (landing.kind() != HitKind.MELEE || damage <= 0 || stacks(player, active) < 10) return;
            long now = System.currentTimeMillis();
            Long last = swiped.get(player.getUniqueId());
            if (last != null && now - last < STACK_EVERY.at(active.count()) * 1000) return;
            swiped.put(player.getUniqueId(), now);
            double additive = Damage.additive(Combat.attacker(player, landing.weapon(), false, 0), target);
            swipe(player, landing.weapon(), swipeDamage(damage, strength(lowestTier(active.pieces()), active.count()), additive));
        }

        /**
         * Swipe Strength: the tier's "0.5x Swipe Damage" (0.5, 0.625, 0.75, 0.875 and 1: the text rounds them to
         * 0.62 and 0.88), times 1, 2 or 3 for 2, 3 or 4 pieces (the wiki's Crimson Armor's table).
         */
        static double strength(int tier, int pieces) {
            return 0.5 * (1 + tier / 4.0) * Math.max(1, Math.min(pieces, 4) - 1);
        }

        /**
         * A swipe's damage: the hit's, times Swipe Strength x 100 over the hit's Additive Multiplier (100 and its
         * additive percent: the wiki's Additive Sources, "higher % bonus decreasing the swipe's damage"). The
         * additive buffs are the base ones, the weapon's enchantments' and Combat's (UNKNOWN whether armor's count).
         */
        static double swipeDamage(double hit, double strength, double additive) {
            return hit * strength * 100 / (100 + Math.max(0, additive));
        }

        /**
         * "Swipe in a random direction hitting every enemy in the path of the swipe. The first time an enemy is hit
         * by a swipe, it takes double the usual damage" (the wiki): a line of {@link #SWIPE_LENGTH} blocks from them
         * the way it goes, the nearest mob on it hit twice as hard; it's the swipe's own damage (it sets off no
         * hits of its own).
         */
        static void swipe(Player player, NBTTagCompound weapon, double damage) {
            double yaw = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            Vector way = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
            Location from = player.getLocation().add(0, 1, 0);
            List<LivingEntity> hit = new ArrayList<>();
            for (Entity entity : player.getNearbyEntities(SWIPE_LENGTH, SWIPE_LENGTH, SWIPE_LENGTH)) {
                if (!(entity instanceof LivingEntity living) || entity instanceof Player || !MobHits.hittable(living)) continue;
                Vector to = living.getLocation().add(0, living.getHeight() / 2, 0).toVector().subtract(from.toVector());
                double along = to.dot(way);
                if (along < 0 || along > SWIPE_LENGTH || to.clone().subtract(way.clone().multiply(along)).length() > SWIPE_WIDTH) continue;
                hit.add(living);
            }
            hit.sort(Comparator.comparingDouble(m -> m.getLocation().distanceSquared(player.getLocation())));
            for (int i = 0; i < hit.size(); i++) {
                MobHits.deal(player, hit.get(i), i == 0 ? damage * 2 : damage, DamageIndicators.Look.NORMAL, HitKind.OTHER, weapon);
            }
            for (double d = 0.5; d <= SWIPE_LENGTH; d += 0.5) {
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, from.clone().add(way.clone().multiply(d)), 1, 0, 0, 0, 0);
            }
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return STACK_LASTS.text(STACK_EVERY.text(text, "Every &a", count, needs()), "after ", count, needs());
        }

        @Override
        public void ended(Player player) {
            stacks.remove(player.getUniqueId());
            swiped.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            stacks.remove(player);
            swiped.remove(player);
        }
    }

    /**
     * Fervor Armor's Fervor (2+ pieces): "attacking a mob grants 1 stack", at most every 1.5s (1s with 3
     * pieces, 0.5s with 4), lost one at a time after 4s (7s, 10s) without gaining one. The stacks do
     * nothing by themselves: the Fervor Chestplate's Ground Pound, an ability ("At 10 stacks, sneak to
     * reset your stacks and perform a Ground Pound"), spends them through {@link SetBonuses#fervor} and
     * {@link SetBonuses#spendFervor}. Arrows' hits count as attacks, abilities' damage doesn't come here
     * (UNKNOWN both).
     */
    static final class Fervor extends Tiered {
        private final Map<UUID, Stacks> stacks = new HashMap<>();

        Fervor() {
            super(2);
        }

        @Override
        public String name() {
            return "Fervor";
        }

        /** Their stacks at {@code now}, with this many pieces on. */
        int stacks(UUID player, long now, int pieces) {
            Stacks s = stacks.get(player);
            return s == null ? 0 : s.at(now, STACK_LASTS.at(pieces));
        }

        /** They attacked a mob at {@code now}: a stack, if the last was long enough ago. */
        void attacked(UUID player, long now, int pieces) {
            stacks.computeIfAbsent(player, id -> new Stacks()).hit(now, STACK_EVERY.at(pieces), STACK_LASTS.at(pieces));
        }

        /** Their stacks are spent. */
        void spend(UUID player) {
            stacks.remove(player);
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            attacked(player.getUniqueId(), System.currentTimeMillis(), active.count());
            return null;
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return STACK_LASTS.text(STACK_EVERY.text(text, "Every &a", count, needs()), "after ", count, needs());
        }

        @Override
        public void ended(Player player) {
            spend(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            spend(player);
        }
    }

    /**
     * Terror Armor's Hydra Strike (2+ pieces): arrow hits gain a stack at most every 0.2s, lost one at a
     * time after 4s (7s, 10s) without gaining one; each stack grants +2% Damage, additive, twice that with
     * 3 pieces and three times with 4, and 2.75, 3.5, 4.25 and 5 for the Hot to Infernal tiers (the
     * lowest worn counts, the wiki's Terror Armor), which lore shows to one decimal, halves to even (the
     * item data's "+2.8%" and "+4.2%"). Each stack makes their arrows faster by the lowest tier's "+1% Arrow
     * Speed" (their speed as they leave the bow), and at 10 stacks each shot has two more arrows beside it
     * that "deal 20% Arrow Damage" (the lowest tier's, 20% to 60%): "two additional arrows are fired", once a
     * shot (a Terminator's three arrows fire two, not six), 10 degrees either side (UNKNOWN).
     */
    static final class HydraStrike extends Tiered {
        static final double SIDE_DEGREES = 10;
        private final Map<UUID, Integer> sideTick = new HashMap<>();
        private static final double[] PER_STACK = {2, 2.75, 3.5, 4.25, 5};
        private static final ThreadLocal<DecimalFormat> ONE_DECIMAL = ThreadLocal.withInitial(() -> {
            DecimalFormat format = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT));
            format.setRoundingMode(RoundingMode.HALF_EVEN);
            return format;
        });
        private final Map<UUID, Stacks> stacks = new HashMap<>();

        HydraStrike() {
            super(2);
        }

        @Override
        public String name() {
            return "Hydra Strike";
        }

        /** Each stack's Damage for this tier and many pieces. */
        static double perStack(int tier, int pieces) {
            return PER_STACK[tier] * Math.max(1, Math.min(pieces, 4) - 1);
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            if (!ranged) return null;
            long now = System.currentTimeMillis();
            double lasts = STACK_LASTS.at(active.count());
            Stacks s = stacks.computeIfAbsent(player.getUniqueId(), id -> new Stacks());
            // As Dominus: the stacks before the hit count (UNKNOWN).
            int before = s.at(now, lasts);
            s.hit(now, 0.2, lasts);
            double damage = perStack(lowestTier(active.pieces()), active.count()) * before;
            return damage > 0 ? new Combat.HitBuff(damage, 1) : null;
        }

        @Override
        public void arrowShot(Player player, Active active, Projectile projectile, NBTTagCompound bow, boolean shortbow) {
            Stacks s = stacks.get(player.getUniqueId());
            int n = s == null ? 0 : s.at(System.currentTimeMillis(), STACK_LASTS.at(active.count()));
            if (n <= 0) return;
            ItemBlock block = lowestBlock(active.pieces(), name());
            projectile.setVelocity(projectile.getVelocity().multiply(1 + BonusText.after(block, "Damage and", 1) * n / 100));
            if (n < 10) return;
            int tick = Bukkit.getCurrentTick();
            Integer before = sideTick.put(player.getUniqueId(), tick);
            if (before != null && before == tick) return;
            double share = BonusText.after(block, "deal", 20) / 100;
            for (double degrees : new double[] {-SIDE_DEGREES, SIDE_DEGREES}) {
                Arrow side = player.launchProjectile(Arrow.class, projectile.getVelocity().clone().rotateAroundY(Math.toRadians(degrees)));
                side.setPersistent(false);
                Shots.record(side, player, bow, true, share);
            }
        }

        @Override
        public List<String> text(List<String> text, int count) {
            text = STACK_LASTS.text(text, "after ", count, needs());
            DoubleFunction<String> shown = v -> ONE_DECIMAL.get().format(v);
            for (int tier = 0; tier < PER_STACK.length; tier++) {
                String from = "&c+" + shown.apply(perStack(tier, needs())) + "%";
                String to = "&c+" + shown.apply(perStack(tier, Math.max(count, needs()))) + "%";
                List<String> changed = Tiers.replace(text, from, to);
                if (!changed.equals(text) || text.stream().anyMatch(line -> line.contains(from))) return changed;
            }
            return text;
        }

        @Override
        public void ended(Player player) {
            stacks.remove(player.getUniqueId());
            sideTick.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            stacks.remove(player);
            sideTick.remove(player);
        }
    }

    /** Of these Kuudra pieces, the block of the lowest tier's (its numbers are the ones that count); null for none. */
    static ItemBlock lowestBlock(List<Worn.Piece> pieces, String name) {
        int lowest = lowestTier(pieces);
        ItemBlock any = null;
        for (Worn.Piece piece : pieces) {
            ItemBlock block = BonusText.block(piece, SetKey.TIERED, name);
            if (block == null) continue;
            if (lowestTier(List.of(piece)) == lowest) return block;
            any = block;
        }
        return any;
    }

    /**
     * Aurora Armor's Arcane Energy (2+ pieces): "Every 1s, dealing Magic Damage from an ability grants 1 stack of
     * Arcane Energy" (0.7s with 3 pieces, 0.5s with 4: the wiki), up to 10, "Lose 1 stack after 4s of not gaining
     * a stack". An ability's hit that doesn't crit is taken as its magic damage (abilities' magic never crits;
     * those worked out as a melee hit or an arrow can: UNKNOWN whether Hypixel counts them). The stacks do nothing
     * by themselves: the Aurora Chestplate's Homing Missiles (an ability: "At 10 stacks, sneak to reset your stacks
     * and shoot 3 homing missiles", the wiki) read them with {@link SetBonuses#arcaneEnergy} and spend them with
     * {@link SetBonuses#spendArcaneEnergy}.
     */
    static final class ArcaneEnergy extends Tiered {
        static final Tiers EVERY = new Tiers(2, 1, 0.7, 0.5);
        private final Map<UUID, Stacks> stacks = new HashMap<>();

        ArcaneEnergy() {
            super(2);
        }

        @Override
        public String name() {
            return "Arcane Energy";
        }

        /** Their stacks at {@code now}. */
        int stacks(UUID player, long now) {
            Stacks s = stacks.get(player);
            return s == null ? 0 : s.at(now);
        }

        /** They dealt an ability's magic damage at {@code now}, with this many pieces on and stacks lasting {@code lasts}. */
        void dealt(UUID player, long now, int pieces, double lasts) {
            stacks.computeIfAbsent(player, id -> new Stacks()).hit(now, EVERY.at(pieces), lasts, 10);
        }

        void spend(UUID player) {
            stacks.remove(player);
        }

        @Override
        public void landed(Player player, Active active, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
            if (landing.kind() != HitKind.ABILITY || landing.critical()) return;
            dealt(player.getUniqueId(), System.currentTimeMillis(), active.count(), BonusText.after(BonusText.block(active), "after", 4));
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return EVERY.text(text, "Every &a", count, needs());
        }

        @Override
        public void ended(Player player) {
            spend(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            spend(player);
        }
    }

    /**
     * Hollow Armor's Spirit (2+ pieces): "Every 3s, up to 5 nearby player's attacks grant 1 stack of Spirit (Max 10
     * Stacks). Lose 1 after 4s of not gaining a stack": "Hitting a mob grants 1 stack of Spirit ⚶. Up to 4 other
     * players within 25 blocks can also grant you stacks by hitting mobs. Stacks can be gained from each player at
     * most once every 3s (2s with 3 pieces, 1s with 4). The maximum number of stacks is determined by the lowest
     * prestige tier of the equipped Hollow Armor pieces" (the wiki): the text's "Max 10" to "Max 40". Melee hits,
     * arrows and abilities' hits count ("Fixed magic damage not granting Spirit stacks"); Ferocity strikes don't
     * (UNKNOWN). The four others are the first four to grant one while its stacks last (UNKNOWN). They do nothing
     * by themselves: the Hollow Wand spends them ({@link SetBonuses#spirit}, {@link SetBonuses#spendSpirit}).
     */
    static final class Spirit extends Tiered {
        static final Tiers EVERY = new Tiers(2, 3, 2, 1);
        static final double RANGE = 25;
        static final int OTHERS = 4;
        /** Who wears it, as the last second's tick saw them: a hit asks only if there's anyone. */
        private static final Set<UUID> WEARING = new HashSet<>();
        private final Map<UUID, Gained> gained = new HashMap<>();

        /** One wearer's stacks, and when each player last granted one. */
        static final class Gained {
            final Stacks stacks = new Stacks();
            final Map<UUID, Long> from = new LinkedHashMap<>();

            /**
             * {@code hitter} hit a mob at {@code now}: a stack if they may grant one ({@code every} seconds since their
             * last, and they're the wearer or one of the {@link #OTHERS} others), up to {@code most}. Whether it did.
             */
            boolean hit(UUID wearer, UUID hitter, long now, double every, double lasts, int most) {
                from.values().removeIf(at -> now - at > lasts * 1000);
                Long last = from.get(hitter);
                if (last != null && now - last < every * 1000) return false;
                if (last == null && !hitter.equals(wearer)) {
                    int others = 0;
                    for (UUID id : from.keySet()) if (!id.equals(wearer)) others++;
                    if (others >= OTHERS) return false;
                }
                from.put(hitter, now);
                // Each player's own cooldown is its limit, so the stack itself has none.
                return stacks.hit(now, 0, lasts, most);
            }
        }

        Spirit() {
            super(2);
        }

        @Override
        public String name() {
            return "Spirit";
        }

        /** Their stacks at {@code now}. */
        int stacks(UUID player, long now) {
            Gained g = gained.get(player);
            return g == null ? 0 : g.stacks.at(now);
        }

        /** Spends up to {@code n} of their stacks at {@code now}; returns how many. */
        int spend(UUID player, long now, int n) {
            Gained g = gained.get(player);
            return g == null ? 0 : g.stacks.spend(now, n);
        }

        /** The most stacks these pieces hold: the lowest tier's "Max 10 Stacks". */
        static int most(List<Worn.Piece> pieces) {
            return (int) BonusText.after(lowestBlock(pieces, "Spirit"), "Max", 10);
        }

        /** Someone hit a mob: they and each wearer within 25 blocks of them may gain a stack. */
        static void heard(Player hitter, Combat.Landing landing) {
            if (WEARING.isEmpty()) return;
            HitKind kind = landing.kind();
            if (kind != HitKind.MELEE && kind != HitKind.ARROW && kind != HitKind.ABILITY) return;
            long now = System.currentTimeMillis();
            for (UUID wearing : WEARING) {
                Player wearer = Bukkit.getPlayer(wearing);
                if (wearer == null || !wearer.getWorld().equals(hitter.getWorld())) continue;
                if (!wearer.equals(hitter) && (!SetBonuses.inPlay(wearer) || wearer.getLocation().distanceSquared(hitter.getLocation()) > RANGE * RANGE)) {
                    continue;
                }
                for (Active a : SetBonuses.active(wearer)) {
                    if (!(a.bonus() instanceof Spirit spirit)) continue;
                    spirit.gained.computeIfAbsent(wearer.getUniqueId(), id -> new Gained()).hit(wearer.getUniqueId(), hitter.getUniqueId(), now,
                            EVERY.at(a.count()), BonusText.after(BonusText.block(a), "after", 4), most(a.pieces()));
                }
            }
        }

        @Override
        public void second(Player player, Active active) {
            WEARING.add(player.getUniqueId());
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return EVERY.text(text, "Every &a", count, needs());
        }

        @Override
        public void ended(Player player) {
            gained.remove(player.getUniqueId());
            WEARING.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            gained.remove(player);
            WEARING.remove(player);
        }
    }

    /**
     * Rekindled Ember Armor's Rekindle (2+): "Increase all of your outgoing burning damage by 200%. For each second you
     * are on fire additionally increase all burning damage by 1.2% up to 50%. When wearing 2 or more armor pieces of
     * this set, melee attacks from ignited mobs will set you on fire!", 400%, 2.5% and 100% with 3 pieces, 600%, 5% and
     * 200% with 4 (the wiki's Rekindled Ember Armor, which says it's "bugged to not do anything" on Hypixel). Their
     * damages over time that burn (Fire Aspect's) are that much more, the parts added up; the seconds on fire count up
     * while they burn and start again when they don't (UNKNOWN both). A burning mob's melee hit sets them on fire for
     * 3 seconds (UNKNOWN how long).
     */
    static final class Rekindle extends Tiered {
        static final Tiers OUTGOING = new Tiers(2, 200, 400, 600);
        static final Tiers PER_SECOND = new Tiers(2, 1.2, 2.5, 5);
        static final Tiers MOST = new Tiers(2, 50, 100, 200);
        static final int IGNITE_TICKS = 60;
        private final Map<UUID, Integer> burning = new HashMap<>();

        Rekindle() {
            super(2);
        }

        @Override
        public String name() {
            return "Rekindle";
        }

        /** The factor on their burning damage with this many pieces on, after this many seconds on fire. */
        static double factor(int pieces, int secondsOnFire) {
            double extra = Math.min(MOST.at(pieces), PER_SECOND.at(pieces) * Math.max(0, secondsOnFire));
            return 1 + (OUTGOING.at(pieces) + extra) / 100;
        }

        @Override
        public double dotFactor(Player player, Active active, DamageIndicators.Look look) {
            return look == DamageIndicators.Look.FIRE ? factor(active.count(), burning.getOrDefault(player.getUniqueId(), 0)) : 1;
        }

        @Override
        public void second(Player player, Active active) {
            if (player.getFireTicks() > 0) burning.merge(player.getUniqueId(), 1, Integer::sum);
            else burning.remove(player.getUniqueId());
        }

        @Override
        public void hurt(Player player, Active active, Entity by, double taken) {
            if (by instanceof LivingEntity mob && !(by instanceof Player) && mob.getFireTicks() > 0) {
                player.setFireTicks(Math.max(player.getFireTicks(), IGNITE_TICKS));
            }
        }

        @Override
        public List<String> text(List<String> text, int count) {
            text = OUTGOING.text(text, "by &c", count, needs());
            text = PER_SECOND.text(text, "by &c", count, needs());
            return MOST.text(text, "up to &c", count, needs());
        }

        @Override
        public void ended(Player player) {
            burning.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            burning.remove(player);
        }
    }

    /**
     * Thunder Armor's (and the Thunderbolt Necklace's) Static Charge (2+ of its 5): "Gains 1 static charge every 30
     * seconds. Hitting a target will cause a discharge, adding 15% damage to that hit for each charge collected.
     * Maximum Charge Capacity: 2"; every 25, 20 and 10 seconds, 20%, 25% and 40%, and 3, 4 and 5 charges with 3, 4
     * and 5 pieces (the wiki's Thunder Armor). A melee hit discharges ("This does not trigger with magic or ranged
     * damage"), lightning striking its target (a look: it hurts nothing); the damage is additive (UNKNOWN). Charges
     * come every so long from when the pieces went on. The Magmatic part waits for Magmatic mobs.
     */
    static final class StaticCharge extends Tiered {
        static final Tiers EVERY = new Tiers(2, 30, 25, 20, 10);
        static final Tiers PER_CHARGE = new Tiers(2, 15, 20, 25, 40);
        static final Tiers CAPACITY = new Tiers(2, 2, 3, 4, 5);

        /** One wearer's charges, and when the last came. */
        static final class Charges {
            int charges;
            long since;

            /** At {@code now}: one more if {@code every} seconds have gone since the last, up to {@code capacity}. */
            void tick(long now, double every, int capacity) {
                if (now - since < every * 1000) return;
                since = now;
                charges = Math.min(capacity, charges + 1);
            }
        }

        private final Map<UUID, Charges> charges = new HashMap<>();

        StaticCharge() {
            super(2);
        }

        @Override
        public String name() {
            return "Static Charge";
        }

        @Override
        public void second(Player player, Active active) {
            long now = System.currentTimeMillis();
            charges.computeIfAbsent(player.getUniqueId(), id -> {
                Charges c = new Charges();
                c.since = now;
                return c;
            }).tick(now, EVERY.at(active.count()), (int) CAPACITY.at(active.count()));
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
            if (landing.kind() != HitKind.MELEE) return null;
            Charges c = charges.get(player.getUniqueId());
            if (c == null || c.charges <= 0) return null;
            double additive = PER_CHARGE.at(active.count()) * c.charges;
            c.charges = 0;
            landing.entity().getWorld().strikeLightningEffect(landing.entity().getLocation());
            return new Combat.HitBuff(additive, 1);
        }

        @Override
        public List<String> text(List<String> text, int count) {
            text = EVERY.text(text, "every &e", count, needs());
            text = PER_CHARGE.text(text, "adding &a", count, needs());
            return CAPACITY.text(text, "Capacity: &c", count, needs());
        }

        @Override
        public void ended(Player player) {
            charges.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            charges.remove(player);
        }
    }
}
