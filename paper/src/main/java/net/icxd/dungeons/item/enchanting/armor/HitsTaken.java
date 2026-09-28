package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The armor enchantments that answer a hit a player takes ({@link PlayerDamage#addHurtListener}): Thorns,
 * Reflection, Counter-Strike, Last Stand and No Pain No Gain, each with its book's numbers (see {@link
 * EnchantNumbers}). Last Stand hears of vanilla damage too (see {@link ArmorEnchants}). Main thread.
 */
final class HitsTaken {
    static final String THORNS = "thorns";
    static final String REFLECTION = "reflection";
    static final String COUNTER_STRIKE = "counter_strike";
    static final String LAST_STAND = "last_stand";
    static final String NO_PAIN_NO_GAIN = "no_pain_no_gain";
    private static final String LAST_STAND_KEY = "enchant:" + LAST_STAND;

    /** The mobs that have hit each player who wears Counter-Strike (its "first hit from an enemy"). */
    private static final Map<UUID, Set<UUID>> HIT_BY = new HashMap<>();
    /** Whose health was {@link #before} as the hit being taken got to it (see {@link #register}'s shield). */
    private static Player beforeOf;
    private static double before;

    private HitsTaken() {
    }

    static void register() {
        // It takes nothing: it notes their health as the hit reaches it (after Defense and the other shields,
        // before their absorption), for Last Stand's "When falling below 40% Health".
        PlayerDamage.addShield((player, taken, by) -> {
            beforeOf = player;
            before = PlayerHealth.get(player);
            return taken;
        });
        PlayerDamage.addHurtListener(HitsTaken::hurt);
    }

    private static void hurt(Player player, Entity by, PlayerDamage.Kind kind, double taken) {
        List<WornEnchants.Piece> pieces = WornEnchants.of(player);
        if (pieces.isEmpty()) return;
        double after = PlayerHealth.get(player);
        lastStand(player, pieces, beforeOf == player ? before : after + taken, after);
        beforeOf = null;
        Entity attacker = PlayerDamage.attacker(by);
        // Traps' hits (nothing behind them) and anything but SkyBlock's mobs: none of these.
        if (attacker == null || (Mobs.of(attacker) == null && DungeonMobs.of(attacker) == null)) return;
        thorns(player, pieces, attacker, taken);
        if (by instanceof AbstractArrow) reflection(player, pieces, attacker);
        counterStrike(player, pieces, attacker);
        noPainNoGain(player, pieces);
    }

    // ---------- Thorns and Reflection ----------

    /**
     * "Grants a 50% chance to rebound 12% of damage dealt back at the attacker": each piece rolls its own
     * chance, and what they rebound together hits the mob behind the hit (a projectile's shooter) as an
     * effect's damage ({@link MobHits#deal}: its number, and its kill is theirs: the wiki's "Fixed Magma Armor
     * not counting kills with Thorns"). UNKNOWN: "damage dealt" is taken as what the hit took from them, after
     * their Defense; whether an arrow's hit counts (here it does), how pieces combine (here each rolls), and
     * whether the mob's Defense lessens it (here it's dealt as it is).
     */
    private static void thorns(Player player, List<WornEnchants.Piece> pieces, Entity attacker, double taken) {
        if (!(attacker instanceof LivingEntity mob)) return;
        double rebound = 0;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(THORNS);
            if (level <= 0) continue;
            double[] n = EnchantNumbers.of(THORNS, level);
            if (n.length >= 2) rebound += thorns(taken, n[0], n[1], ThreadLocalRandom.current().nextDouble());
        }
        if (rebound > 0) MobHits.deal(player, mob, rebound, DamageIndicators.Look.NORMAL, HitKind.OTHER, null);
    }

    /**
     * What one piece's Thorns rebounds of a hit that took {@code taken}: {@code share}% of it if {@code roll} (0 to 1)
     * is under {@code chance}%.
     */
    static double thorns(double taken, double chance, double share, double roll) {
        return roll * 100 < chance ? Math.max(0, taken) * share / 100 : 0;
    }

    /**
     * "When damaged by an arrow, deal 30x your Intelligence to its shooter" (the chestplate's; its stats are
     * its text's, see EnchantmentType), as an effect's damage. UNKNOWN whether anything multiplies it (here
     * nothing does) and whether every arrow of a volley counts (here each does).
     */
    private static void reflection(Player player, List<WornEnchants.Piece> pieces, Entity shooter) {
        if (!(shooter instanceof LivingEntity mob)) return;
        double times = 0;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(REFLECTION);
            if (level > 0) times += EnchantNumbers.get(REFLECTION, level, 2);
        }
        if (times <= 0) return;
        double damage = reflection(PlayerSession.of(player).stats().get(Stat.INTELLIGENCE), times);
        if (damage > 0) MobHits.deal(player, mob, damage, DamageIndicators.Look.NORMAL, HitKind.OTHER, null);
    }

    /** Reflection's damage: {@code times} their Intelligence (none for less than none). */
    static double reflection(double intelligence, double times) {
        return Math.max(0, intelligence) * Math.max(0, times);
    }

    // ---------- Counter-Strike ----------

    /**
     * "Gain +10 Defense for 7s on the first hit from an enemy": each mob's first hit on them gives it again
     * (a new one in place of the last, from its start). UNKNOWN: "first" is taken as each mob's first, for as
     * long as that mob is alive; what several pieces do (Counter-Strike is the chestplate's alone).
     */
    private static void counterStrike(Player player, List<WornEnchants.Piece> pieces, Entity attacker) {
        double defense = 0;
        double seconds = 0;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(COUNTER_STRIKE);
            if (level <= 0) continue;
            double[] n = EnchantNumbers.of(COUNTER_STRIKE, level);
            if (n.length < 2) continue;
            defense += n[0];
            seconds = Math.max(seconds, n[1]);
        }
        if (defense <= 0 || !HIT_BY.computeIfAbsent(player.getUniqueId(), id -> new HashSet<>()).add(attacker.getUniqueId())) return;
        PlayerSession.of(player).buff("enchant:" + COUNTER_STRIKE, new Stats().set(Stat.DEFENSE, defense), (long) (seconds * 1000));
    }

    /** Once a second: the mobs that are gone are forgotten. */
    static void second() {
        for (Iterator<Map.Entry<UUID, Set<UUID>>> players = HIT_BY.entrySet().iterator(); players.hasNext(); ) {
            Map.Entry<UUID, Set<UUID>> e = players.next();
            if (Bukkit.getPlayer(e.getKey()) == null) {
                players.remove();
                continue;
            }
            e.getValue().removeIf(mob -> {
                Entity entity = Bukkit.getEntity(mob);
                return entity == null || !entity.isValid();
            });
            if (e.getValue().isEmpty()) players.remove();
        }
    }

    // ---------- Last Stand ----------

    /**
     * "When falling below 40% Health: Gain +12.5% Defense for 10s. Regen 10 Vitality. Cooldown: 30s", from any
     * hit or vanilla damage that takes them from 40% or more to below it (not a killing one). The pieces' add up
     * (the wiki: it "will stack across multiple armor pieces"); one cooldown for them all (UNKNOWN), kept with
     * their ability cooldowns. The Defense is a share of their Defense as the rest makes it
     * ({@link PlayerSession#buffPercent}).
     */
    static void lastStand(Player player, List<WornEnchants.Piece> pieces, double before, double after) {
        double defense = 0;
        double vitality = 0;
        double[] rules = null;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(LAST_STAND);
            if (level <= 0) continue;
            double[] n = EnchantNumbers.of(LAST_STAND, level);
            if (n.length < 5) continue;
            defense += n[1];
            vitality += n[3];
            rules = n;
        }
        if (rules == null || !fellBelow(before, after, PlayerHealth.max(player), rules[0] / 100)) return;
        PlayerSession session = PlayerSession.of(player);
        if (session.cooldownLeft(LAST_STAND_KEY) > 0) return;
        session.startCooldown(LAST_STAND_KEY, (long) (rules[4] * 1000));
        session.buffPercent(LAST_STAND_KEY, Stat.DEFENSE, defense, (long) (rules[2] * 1000));
        session.setVitality(Math.min(Vitality.max(player), Vitality.get(player) + vitality));
    }

    /** Whether their health went from at least {@code share} of {@code max} to below it, and they're alive. */
    static boolean fellBelow(double before, double after, double max, double share) {
        return max > 0 && after > 0 && before >= share * max && after < share * max;
    }

    // ---------- No Pain No Gain ----------

    /**
     * "You have 100% chance to gain 10 experience orbs every time you take hits from mobs": each piece rolls
     * its own, straight to them (as a dungeon mob's orbs are, see {@link ExpOrbs}). A mob's arrow is one of its
     * hits (UNKNOWN).
     */
    private static void noPainNoGain(Player player, List<WornEnchants.Piece> pieces) {
        double orbs = 0;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(NO_PAIN_NO_GAIN);
            if (level <= 0) continue;
            double[] n = EnchantNumbers.of(NO_PAIN_NO_GAIN, level);
            if (n.length >= 2 && ThreadLocalRandom.current().nextDouble() * 100 < n[0]) orbs += n[1];
        }
        if (orbs > 0) ExpOrbs.grant(player, orbs, ExpOrbs.Source.OTHER, null, true);
    }

    /** They've left. */
    static void forget(UUID player) {
        HIT_BY.remove(player);
    }
}
