package net.icxd.dungeons.item.ability.utility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;

/**
 * Abilities' heals (0.26.1's Healing Revamp). A heal on someone else is times the healer's Mending / 100
 * ({@link PlayerHealth#healFrom}); in the Catacombs "all healing ... now scales with your Dungeon Stat
 * Boost – the boost that you primarily gain from leveling the Catacombs skill" (its release notes), taken
 * as the healer's Catacombs boost (the one dungeon items' stats get, {@link ItemBuilder#catacombsBoost}):
 * whose it is when the healer isn't the one healed is UNKNOWN. Healing others in a run counts for its
 * "Ally Healing". A ghost (dead in a run) isn't healed.
 *
 * <p>Heals over time ("Heal 60❤ per second for 5s") heal once a second; one of each kind at a time per
 * player (see {@link #overTime}). Main thread.
 */
final class Heals {
    /** A heal over time: {@code amount} each second from {@code healer}, {@code pulses} more times. */
    private static final class OverTime {
        final UUID healer;
        double amount;
        int pulses;
        int ticksToNext;

        OverTime(UUID healer, double amount, int pulses) {
            this.healer = healer;
            this.amount = amount;
            this.pulses = pulses;
            this.ticksToNext = 20;
        }
    }

    /** Heals over time running, by who's healed and then by kind ("wand"). */
    private static final Map<UUID, Map<String, OverTime>> RUNNING = new HashMap<>();

    private Heals() {
    }

    /**
     * What a heal of {@code base} comes to: times Mending / 100 when it's on someone else, and times 1 + the
     * Catacombs boost ({@code catacombsBoost} is 0 outside the Catacombs).
     */
    static double amount(double base, boolean other, double mending, double catacombsBoost) {
        double amount = other ? PlayerHealth.outgoing(base, mending) : Math.max(0, base);
        return amount * (1 + Math.max(0, catacombsBoost));
    }

    /** Heals {@code target} for {@code base} from {@code healer} (themselves too); what it healed, 0 for nobody. */
    static double give(Player healer, Player target, double base) {
        if (target == null || !target.isOnline() || target.isDead()) return 0;
        DungeonRun run = RunManager.of(target);
        if (run != null && run.isGhost(target.getUniqueId())) return 0;
        boolean other = healer != null && !healer.equals(target);
        double boost = healer != null && RunManager.inRun(healer) ? ItemBuilder.catacombsBoost(healer) : 0;
        double mending = healer == null ? 100 : PlayerSession.of(healer).stats().get(Stat.MENDING);
        double amount = amount(base, other, mending, boost);
        double before = PlayerHealth.get(target);
        PlayerHealth.heal(target, amount);
        double healed = PlayerHealth.get(target) - before;
        if (other && run != null) run.healedAllies(healer.getUniqueId(), healed);
        return healed;
    }

    /**
     * Starts or renews a heal over time on {@code target}: {@code amount} a second for {@code seconds}, the
     * first second's at once when {@code now}. There's one of each {@code kind} at a time: a new one takes the
     * old one's place but keeps its beat, so casting a wand again heals at once and the running heal ticks on
     * ("Wand heals don't stack", yet "the first healing tick is applied immediately. With enough Vitality,
     * repeatedly casting the ability can heal 120 per second": the wiki's Wand of Healing).
     */
    static void overTime(Player healer, Player target, String kind, double amount, int seconds, boolean now) {
        int pulses = pulses(seconds, now);
        if (now) give(healer, target, amount);
        if (pulses <= 0) return;
        Map<String, OverTime> kinds = RUNNING.computeIfAbsent(target.getUniqueId(), id -> new HashMap<>());
        OverTime running = kinds.get(kind);
        if (running != null && running.healer.equals(healer.getUniqueId())) {
            running.amount = amount;
            running.pulses = pulses;
        } else {
            kinds.put(kind, new OverTime(healer.getUniqueId(), amount, pulses));
        }
    }

    /** Every tick: heals over time that are due heal. */
    static void tick() {
        for (Iterator<Map.Entry<UUID, Map<String, OverTime>>> players = RUNNING.entrySet().iterator(); players.hasNext(); ) {
            Map.Entry<UUID, Map<String, OverTime>> entry = players.next();
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target == null) {
                players.remove();
                continue;
            }
            for (Iterator<OverTime> heals = entry.getValue().values().iterator(); heals.hasNext(); ) {
                OverTime heal = heals.next();
                if (--heal.ticksToNext > 0) continue;
                heal.ticksToNext = 20;
                give(Bukkit.getPlayer(heal.healer), target, heal.amount);
                if (--heal.pulses <= 0) heals.remove();
            }
            if (entry.getValue().isEmpty()) players.remove();
        }
    }

    /** Pulses of a heal over time that has {@code seconds}, the first at once when {@code now}: one a second in all. */
    static int pulses(int seconds, boolean now) {
        return now ? Math.max(0, seconds - 1) : Math.max(0, seconds);
    }
}
