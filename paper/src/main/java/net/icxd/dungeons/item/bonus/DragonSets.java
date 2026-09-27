package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dragon armor sets' full set bonuses (the "Blood" ones). Not here: Superior Blood's "Aspect of the
 * Dragons ability deals 50% more damage" (the ability's: it can ask {@link SetBonuses#active}), and Old
 * Blood's Feather Falling (fall damage isn't SkyBlock's yet).
 */
final class DragonSets {
    /** The Stats & Equipment menu's Combat Stats (as recorded), which Superior Blood raises with Magic Find. */
    static final List<Stat> COMBAT = List.of(Stat.HEALTH, Stat.DEFENSE, Stat.TRUE_DEFENSE, Stat.STRENGTH, Stat.CRIT_CHANCE, Stat.CRIT_DAMAGE,
            Stat.ATTACK_SPEED, Stat.FEROCITY, Stat.SWING_RANGE, Stat.INTELLIGENCE, Stat.ABILITY_DAMAGE, Stat.HEALTH_REGEN, Stat.VITALITY,
            Stat.MENDING);

    private DragonSets() {
    }

    static List<Bonus> all() {
        return List.of(new SuperiorBlood(), new StrongBlood(), new YoungBlood(), new OldBlood(), new WiseBlood(), new UnstableBlood(),
                new ProtectiveBlood(), new HolyBlood());
    }

    /** A dragon set's full set bonus. */
    abstract static class Blood implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }
    }

    /**
     * "Increases all Combat stats and Magic Find by 5%": the Combat Stats and Magic Find they have with
     * everything else in (since 2022 "additive with other stat boosts", of which there are none here yet).
     */
    static final class SuperiorBlood extends Blood {
        @Override
        public String name() {
            return "Superior Blood";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            raise(stats);
        }

        static void raise(Stats stats) {
            for (Stat stat : COMBAT) stats.set(stat, stats.get(stat) * 1.05);
            stats.set(Stat.MAGIC_FIND, stats.get(Stat.MAGIC_FIND) * 1.05);
        }
    }

    /** "Gain +70 Walk Speed while you are above 50% HP. +100 Walk Speed Cap." */
    static final class YoungBlood extends Blood {
        @Override
        public String name() {
            return "Young Blood";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            if (Bonuses.healthShare(player, stats) > 0.5) stats.add(Stat.SPEED, 70);
        }

        @Override
        public double speedCap(Player player, Active active) {
            return 100;
        }
    }

    /**
     * "Increases the strength of Growth, Protection, Feather Falling, Sugar Rush, and True Protection while
     * worn", on the set's own pieces (the wiki's trivia: it doesn't change other armor's). How much is
     * APPROX: the wiki's history's last numbers, from 2019 (Growth +25 Health a level, Protection +5
     * Defense, Sugar Rush +3 Speed, True Protection +8 True Defense), where they're more than the
     * enchantment's own.
     */
    static final class OldBlood extends Blood {
        private static final Map<String, Stat> STATS = Map.of("growth", Stat.HEALTH, "protection", Stat.DEFENSE, "sugar_rush", Stat.SPEED,
                "true_protection", Stat.TRUE_DEFENSE);
        private static final Map<String, Double> PER_LEVEL = Map.of("growth", 25.0, "protection", 5.0, "sugar_rush", 3.0, "true_protection", 8.0);

        @Override
        public String name() {
            return "Old Blood";
        }

        @Override
        public void stats(Player player, Active active, Stats stats) {
            for (Worn.Piece piece : active.pieces()) {
                NBTTagList enchantments = piece.tag().getList("enchantments", 10);
                for (int i = 0; i < enchantments.size(); i++) {
                    String name = enchantments.get(i).getString("name").toLowerCase();
                    Stat stat = STATS.get(name);
                    EnchantmentType type = EnchantmentType.getByNamespace(name);
                    if (stat == null || type == null) continue;
                    int level = enchantments.get(i).getInt("lvl");
                    stats.add(stat, extra(PER_LEVEL.get(name), level, type.getStats(level).get(stat)));
                }
            }
        }

        /** What it adds to an enchantment that gives {@code own} at this level: up to {@code perLevel} a level. */
        static double extra(double perLevel, int level, double own) {
            return Math.max(0, perLevel * level - own);
        }
    }

    /** "Abilities have 2/3 of the mana cost" ("the reduction is exactly 1/3", the wiki's Wise Dragon Armor). */
    static final class WiseBlood extends Blood {
        @Override
        public String name() {
            return "Wise Blood";
        }

        @Override
        public double manaCost(Player player, Active active) {
            return 2.0 / 3;
        }
    }

    /**
     * "Sometimes strikes nearby mobs with lightning": "Strikes all nearby mobs within an 8 block radius with
     * lightning every 15 seconds, dealing 3000 damage" (the wiki's Unstable Dragon Armor), the first 15
     * seconds after the set is on. The 3,000 is dealt as it is, as an aura's (UNKNOWN whether Defense
     * lessens it).
     */
    static final class UnstableBlood extends Blood {
        private final Map<UUID, Integer> seconds = new HashMap<>();

        @Override
        public String name() {
            return "Unstable Blood";
        }

        @Override
        public void second(Player player, Active active) {
            int s = seconds.merge(player.getUniqueId(), 1, Integer::sum);
            if (s % 15 != 0) return;
            for (LivingEntity mob : Bonuses.mobsNear(player, 8)) {
                mob.getWorld().strikeLightningEffect(mob.getLocation());
                Bonuses.damage(player, mob, 3000);
            }
        }

        @Override
        public void ended(Player player) {
            seconds.remove(player.getUniqueId());
        }

        @Override
        public void forget(UUID player) {
            seconds.remove(player);
        }
    }

    /**
     * "Increases the defense of each armor piece by +1% Defense for each missing percent of HP": the set's
     * pieces' Defense, for each whole percent of their health that's missing (UNKNOWN whether it's whole
     * percents).
     */
    static final class ProtectiveBlood extends Blood {
        @Override
        public String name() {
            return "Protective Blood";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            double defense = 0;
            for (Worn.Piece piece : active.pieces()) defense += ItemStats.of(piece.stack(), player).get(Stat.DEFENSE);
            stats.add(Stat.DEFENSE, extra(defense, Bonuses.healthShare(player, stats)));
        }

        /** What the pieces' {@code defense} gains at this share of health. */
        static double extra(double defense, double healthShare) {
            double missing = Math.floor((1 - Math.max(0, Math.min(1, healthShare))) * 100 + 1e-9);
            return Math.max(0, defense) * missing / 100;
        }
    }

    /** "Grants +75 Health Regen to you and all players within 10 blocks. Effect only applies once!" */
    static final class HolyBlood extends Blood {
        @Override
        public String name() {
            return "Holy Blood";
        }

        @Override
        public double auraRange() {
            return 10;
        }

        @Override
        public void aura(Player player, Stats stats) {
            stats.add(Stat.HEALTH_REGEN, 75);
        }
    }
}
