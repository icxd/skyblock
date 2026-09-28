package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.item.ability.utility.Heals;
import net.icxd.dungeons.session.Mana;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The enchantments that turn what a player spends into something else: the four Vitality ones (their ids are
 * the old Mana ones': Hardened, Strong and Vivacious Vitality's "Convert 40% of Vitality used as Defense for
 * 10s (Max 25 Defense)", Vampiric Vitality's "Heal for 1❤ per Vitality used", 0.26.1's rework) and
 * Refrigerate's "Convert 6% of Mana used as Defense for 10s (Max 150 Defense)". Each piece converts for itself,
 * up to its own max (the wiki's Refrigerate: "up to 150 Defense per armor piece, up to a maximum of 600 (4
 * armor pieces)"; the same taken for the Vitality ones, UNKNOWN). Main thread.
 */
final class Conversions {
    static final String HARDENED = "hardened_mana";
    static final String STRONG = "strong_mana";
    static final String VIVACIOUS = "ferocious_mana";
    static final String VAMPIRIC = "mana_vampire";
    static final String REFRIGERATE = "refrigerate";

    /** A conversion's stat now on one piece: how much, and until when. */
    private static final class Pool {
        double amount;
        long endMillis;
    }

    /** By player, then by enchantment and piece ("hardened_mana:0"). */
    private static final Map<UUID, Map<String, Pool>> POOLS = new HashMap<>();

    private Conversions() {
    }

    static void register() {
        Vitality.addSpentListener((player, amount) -> {
            List<WornEnchants.Piece> pieces = WornEnchants.of(player);
            if (pieces.isEmpty()) return;
            convert(player, pieces, HARDENED, Stat.DEFENSE, amount);
            convert(player, pieces, STRONG, Stat.STRENGTH, amount);
            convert(player, pieces, VIVACIOUS, Stat.ATTACK_SPEED, amount);
            vampiric(player, pieces, amount);
        });
        Mana.addSpentListener((player, amount, source) -> {
            List<WornEnchants.Piece> pieces = WornEnchants.of(player);
            if (!pieces.isEmpty()) convert(player, pieces, REFRIGERATE, Stat.DEFENSE, amount);
        });
    }

    /**
     * Each piece with it turns its share of {@code spent} into the stat for its time, up to its max. Spending
     * again while it lasts adds to it (up to the max) and starts its time again (UNKNOWN whether Hypixel's add or
     * start again with the new amount). The numbers are its text's: the share, the seconds, the max.
     */
    private static void convert(Player player, List<WornEnchants.Piece> pieces, String id, Stat stat, double spent) {
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(id);
            if (level <= 0) continue;
            double[] n = EnchantNumbers.of(id, level);
            if (n.length < 3) continue;
            String key = id + ":" + piece.index();
            Pool pool = POOLS.computeIfAbsent(player.getUniqueId(), p -> new HashMap<>()).computeIfAbsent(key, k -> new Pool());
            long now = System.currentTimeMillis();
            long millis = (long) (n[1] * 1000);
            pool.amount = pooled(pool.amount, pool.endMillis, now, spent * n[0] / 100, n[2]);
            pool.endMillis = now + millis;
            PlayerSession.of(player).buff("enchant:" + key, new Stats().set(stat, pool.amount), millis);
        }
    }

    /**
     * What a pool comes to with {@code add} more at {@code now}: on top of what's left of it if it hasn't run out,
     * never past {@code max}.
     */
    static double pooled(double current, long endMillis, long now, double add, double max) {
        double left = now < endMillis ? current : 0;
        return Math.max(0, Math.min(max, left + Math.max(0, add)));
    }

    /**
     * Vampiric Vitality: "Heal for 1❤ per Vitality used", each piece's, as a heal of their own (so it grows with
     * the Catacombs boost in a run, as other heals do: see {@link Heals#give}; UNKNOWN whether it should).
     */
    private static void vampiric(Player player, List<WornEnchants.Piece> pieces, double spent) {
        double heal = 0;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(VAMPIRIC);
            if (level > 0) heal += spent * EnchantNumbers.get(VAMPIRIC, level, 0);
        }
        if (heal > 0) Heals.give(player, player, heal);
    }

    /** They've left. */
    static void forget(UUID player) {
        POOLS.remove(player);
    }
}
