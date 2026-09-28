package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The slayer armor: Tarantula's and Primordial's Octodexterity and Radioactive, Revenant's and Reaper's
 * Trolling The Reaper and Enrage, Mastiff's Absolute Unit. Not here: the Bulwarks (defense from kills
 * of their kind of mob, kept on each piece), Venom (mobs don't heal yet), Anti-Toxin and Brood (the
 * Broodfather and pets aren't here), and Trolling The Reaper's "Healing Wands heal +50%" (the wands'
 * own: they can ask {@link SetBonuses#active}).
 */
final class SlayerSets {
    private SlayerSets() {
    }

    static List<Bonus> all() {
        return List.of(new Octodexterity(SetKey.FULL_SET, 4, 2), new Octodexterity(SetKey.TIERED, 3, 1.5), new Radioactive(),
                new TrollingTheReaper(), new Enrage(), new AbsoluteUnit());
    }

    /**
     * Tarantula Armor's "Every 4th strike, deal +100% Damage" and Primordial Armor's (tiered) "Every 3rd
     * strike, deal +50% Damage": a multiplicative 2x or 1.5x (the wiki's Multiplicative Sources) on every
     * {@code every}th melee hit that lands (UNKNOWN whether arrows count). What Primordial's tiers change
     * isn't on the wiki: it counts with the whole set, as Tarantula's does (UNKNOWN).
     */
    static final class Octodexterity implements Bonus {
        private final String kind;
        private final int every;
        private final double multiplier;
        private final Map<UUID, Integer> strikes = new HashMap<>();

        Octodexterity(String kind, int every, double multiplier) {
            this.kind = kind;
            this.every = every;
            this.multiplier = multiplier;
        }

        @Override
        public String kind() {
            return kind;
        }

        @Override
        public String name() {
            return "Octodexterity";
        }

        @Override
        public int needs(SetKey set) {
            return 4;
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            if (ranged) return null;
            int strike = strikes.merge(player.getUniqueId(), 1, Integer::sum);
            if (strike < every) return null;
            strikes.remove(player.getUniqueId());
            return new Combat.HitBuff(0, multiplier);
        }

        @Override
        public void forget(UUID player) {
            strikes.remove(player);
        }
    }

    /**
     * The Tarantula and Primordial Helmets' Radioactive: "Grants +1 Crit Damage per 10 Strength (Max
     * 1,000 Strength)", the Primordial's +1.5, for every whole 10 (UNKNOWN).
     */
    static final class Radioactive implements Bonus {
        @Override
        public String kind() {
            return "EXTRA";
        }

        @Override
        public String name() {
            return "Radioactive";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            for (Worn.Piece piece : active.pieces()) {
                stats.add(Stat.CRIT_DAMAGE, critDamage(stats.get(Stat.STRENGTH), piece.id().startsWith("PRIMORDIAL") ? 1.5 : 1));
            }
        }

        static double critDamage(double strength, double perTen) {
            return Math.floor(Math.max(0, Math.min(strength, 1000)) / 10) * perTen;
        }
    }

    /**
     * Revenant and Reaper Armor's Trolling The Reaper: "Gain +100 Defense against Undead mobs", and the
     * Reaper's "Deal +100% damage to Undead mobs but 1% to all other mobs": +100 in the additive buffs
     * against the Undead, a multiplicative 0.01 against the rest (the wiki's Additive Sources), when the
     * set is all Reaper pieces (UNKNOWN with Revenant pieces among them: it's only the Reaper's text).
     */
    static final class TrollingTheReaper implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Trolling The Reaper";
        }

        @Override
        public double defenseAgainst(Player player, Active active, Entity by) {
            return SetBonuses.types(by).contains(MobType.UNDEAD) ? 100 : 0;
        }

        @Override
        public Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
            for (Worn.Piece piece : active.pieces()) if (!piece.id().startsWith("REAPER_")) return null;
            return target.types().contains(MobType.UNDEAD) ? new Combat.HitBuff(100, 1) : new Combat.HitBuff(0, 0.01);
        }
    }

    /**
     * Reaper Armor's Enrage (SNEAK, 25s cooldown, its block's): "Enrage for 6s gaining 100 Speed, 100
     * Damage, and 100 Strength." Its header has no count: it takes the set's three pieces. What Hypixel
     * says or shows when it starts (the leggings and boots turn red, the wiki says) is UNKNOWN: nothing.
     */
    static final class Enrage implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Enrage SNEAK";
        }

        @Override
        public int needs(SetKey set) {
            return 3;
        }

        @Override
        public void sneaked(Player player, Active active, boolean sneaking) {
            if (!sneaking || !Bonuses.ready(player, name(), Bonuses.cooldown(active))) return;
            Bonuses.buff(player, name(), new Stats().set(Stat.SPEED, 100).set(Stat.DAMAGE, 100).set(Stat.STRENGTH, 100), 6_000);
        }
    }

    /**
     * Mastiff Armor's Absolute Unit: "Receive -20% damage from Animal mobs. Gain +1% Health per 25 Crit
     * Damage (Max 40% HP). Gain +1 Crit Damage per 2 Defense. Heal 150 when hit, consuming 5 Vitality (1s
     * cooldown). Your Defense is capped at 300." Defense is capped first, then gives Crit Damage (at most
     * 150, "with effective cap of 150", the wiki), which then gives Health; in whole steps (UNKNOWN).
     */
    static final class AbsoluteUnit implements Bonus {
        @Override
        public String kind() {
            return SetKey.FULL_SET;
        }

        @Override
        public String name() {
            return "Absolute Unit";
        }

        @Override
        public void derivedStats(Player player, Active active, Stats stats) {
            apply(stats);
        }

        static void apply(Stats stats) {
            double defense = Math.min(stats.get(Stat.DEFENSE), 300);
            stats.set(Stat.DEFENSE, defense);
            stats.add(Stat.CRIT_DAMAGE, Math.floor(Math.max(0, defense) / 2));
            double percent = Math.min(Math.floor(Math.max(0, stats.get(Stat.CRIT_DAMAGE)) / 25), 40);
            stats.set(Stat.HEALTH, stats.get(Stat.HEALTH) * (1 + percent / 100));
        }

        @Override
        public double takenFrom(Player player, Active active, Entity by) {
            return SetBonuses.types(by).contains(MobType.ANIMAL) ? 0.8 : 1;
        }

        @Override
        public void hurt(Player player, Active active, Entity by, double taken) {
            if (!Vitality.has(player, 5) || !Bonuses.ready(player, name(), 1)) return;
            Vitality.spend(player, 5);
            PlayerHealth.heal(player, 150);
        }
    }
}
