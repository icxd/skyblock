package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.entity.Player;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.DoubleFunction;

/**
 * Tiered bonuses, whose numbers grow with how many pieces are worn (the wiki's Tiered Bonus Values;
 * items' text has the first tier's): Arachne's Faithful, Berserk and Long Tuba's stats, and the Kuudra
 * sets' stacks (Crimson's Dominus and Terror's Hydra Strike: their stats; Fervor's, kept for the Fervor
 * Chestplate's Ground Pound; Aurora's Arcane Energy and Hollow's Spirit only in lore, what they do
 * waits for abilities' magic damage and the Hollow Wand). Tiered bonuses that are only text here, but
 * whose least pieces the wiki gives, are {@link LoreOnly} so their headers turn gold where they should.
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
        return List.of(new ArachnesFaithful(), new Berserk(), new LongTuba(), new Dominus(), new HydraStrike(),
                new LoreOnly("Arcane Energy", 2, new Grows("Every &a", new Tiers(2, 1, 0.7, 0.5))),
                new Fervor(),
                new LoreOnly("Spirit", 2, new Grows("Every &a", new Tiers(2, 3, 2, 1))),
                // The least pieces of the wiki's tiered bonuses that do nothing here yet (its tiered_bonus_required_pieces).
                new LoreOnly("Peace Treaty", 2), new LoreOnly("Fireproof", 2), new LoreOnly("Lord's Blessing", 4),
                new LoreOnly("Static Charge", 2), new LoreOnly("Unearthed", 2), new LoreOnly("Familiarity", 2),
                new LoreOnly("Mythological Greed", 2), new LoreOnly("Rekindle", 2), new LoreOnly("Shimmer", 2));
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
     * health-consuming abilities by 20%" (40% with 3 pieces, 60% with 4). The cost part isn't here:
     * abilities' health costs aren't taken yet.
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

        /** How many there are at {@code now}. */
        int at(long now, double lastsSeconds) {
            long lost = (long) ((now - gained) / (lastsSeconds * 1000));
            return (int) Math.max(0, stacks - Math.max(0, lost));
        }

        /** A hit at {@code now}: a stack if the last was gained at least {@code everySeconds} ago. */
        void hit(long now, double everySeconds, double lastsSeconds) {
            if (now - gained < everySeconds * 1000) return;
            stacks = Math.min(10, at(now, lastsSeconds) + 1);
            gained = now;
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
     * The swipe at 10 stacks isn't here.
     */
    static final class Dominus extends Tiered {
        private static final double[] SWING_RANGE = {0.05, 0.1, 0.1, 0.1, 0.1};
        private static final double[] FEROCITY = {0, 0, 1, 2, 2};
        private static final double[] DAMAGE = {0, 0, 0, 0, 10};
        private final Map<UUID, Stacks> stacks = new HashMap<>();

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

        /** The stacks it had before the hit count for it; the hit may gain one. */
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
        public List<String> text(List<String> text, int count) {
            return STACK_LASTS.text(STACK_EVERY.text(text, "Every &a", count, needs()), "after ", count, needs());
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
     * item data's "+2.8%" and "+4.2%"). Its Arrow Speed and the extra arrows at 10 stacks aren't here.
     */
    static final class HydraStrike extends Tiered {
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
            int before = s.at(now, lasts);
            s.hit(now, 0.2, lasts);
            double damage = perStack(lowestTier(active.pieces()), active.count()) * before;
            return damage > 0 ? new Combat.HitBuff(damage, 1) : null;
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
        }

        @Override
        public void forget(UUID player) {
            stacks.remove(player);
        }
    }
}
