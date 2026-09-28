package net.icxd.dungeons.session;

import lombok.Getter;
import lombok.Setter;
import net.icxd.dungeons.region.RegionType;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What this server keeps about an online player while they're on it: their stats (worked out at
 * most once a tick), health, mana, vitality, absorption, when they were last in combat, ability
 * cooldowns, the region they're in, what the action bar shows in place of defense or mana and the
 * skill that last gained XP. It ends when they leave (see
 * PlayerListener), so nothing of theirs stays behind, and a rejoin starts from full health and mana.
 * Main thread.
 */
public final class PlayerSession {
    private static final Map<UUID, PlayerSession> sessions = new HashMap<>();

    private final Player player;
    private Stats stats;
    private int statsTick = Integer.MIN_VALUE;
    /** SkyBlock health; -1 for full (see {@link PlayerHealth}). */
    @Getter @Setter private double health = -1;
    /** -1 until their mana pool is known, then full. */
    @Getter @Setter private int mana = -1;
    /** What's left of their Vitality pool; -1 for full (see {@link Vitality}). */
    @Getter @Setter private double vitality = -1;
    /** Their absorption, source by source (see {@link Absorption}). */
    @Getter private final Absorption absorption = new Absorption();
    private Replacement defenseReplacement;
    private Replacement manaReplacement;
    private final Map<String, Long> cooldownEnds = new HashMap<>();
    /** Stats they have for a while, by what gave them (see {@link #buff}), and shares more of stats (see {@link #buffPercent}). */
    private final Map<String, Buff> buffs = new HashMap<>();
    private final Map<String, PercentBuff> percentBuffs = new HashMap<>();
    /** Null until they've moved. */
    @Getter @Setter private RegionType region;
    /** When their next mining break animation may start (see MiningManager). */
    @Getter @Setter private long nextBreakPhase;
    /** The skill that last gained XP (the dungeon tab list shows it); null until one has. */
    @Getter @Setter private Skill lastSkill;
    /** When they last dealt a mob damage and last took a mob's hit (see CombatState); 0 for not yet. */
    @Getter @Setter private long lastDealtMillis;
    @Getter @Setter private long lastTakenMillis;

    private PlayerSession(Player player) {
        this.player = player;
    }

    /**
     * The player's session. For someone who has already left, a fresh one that isn't kept (so a late
     * task can't bring a session back after they've gone).
     */
    public static PlayerSession of(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session != null) return session;
        session = new PlayerSession(player);
        if (player.isOnline()) sessions.put(player.getUniqueId(), session);
        return session;
    }

    public static void end(UUID player) {
        sessions.remove(player);
    }

    /**
     * Their stats now: the base, their armor and held item and what they have for a while (flat buffs, then
     * the percent ones on what that comes to), worked out once per tick.
     */
    public Stats stats() {
        int tick = Bukkit.getCurrentTick();
        if (stats == null || statsTick != tick) {
            stats = PlayerStats.of(player);
            long now = System.currentTimeMillis();
            buffs.values().removeIf(buff -> buff.endMillis <= now);
            for (Buff buff : buffs.values()) stats.add(buff.stats);
            if (!percentBuffs.isEmpty()) {
                percentBuffs.values().removeIf(buff -> buff.endMillis <= now);
                Map<Stat, Double> percents = new EnumMap<>(Stat.class);
                for (PercentBuff buff : percentBuffs.values()) percents.merge(buff.stat, buff.percent, Double::sum);
                for (Map.Entry<Stat, Double> e : percents.entrySet()) stats.set(e.getKey(), percentBuffed(stats.get(e.getKey()), e.getValue()));
            }
            statsTick = tick;
        }
        return stats;
    }

    /**
     * Gives them {@code percent} more of a stat for this long (Last Stand's "+12.5% Defense for 10s"), in place of
     * what the same source gave them before; on the stat as their other stats and flat buffs make it. Percent
     * buffs on the same stat add up (UNKNOWN whether Hypixel's do).
     */
    public void buffPercent(String source, Stat stat, double percent, long millis) {
        percentBuffs.put(source, new PercentBuff(stat, percent, System.currentTimeMillis() + millis));
        this.stats = null;
    }

    /** A stat with this many percent more (never below none). */
    static double percentBuffed(double value, double percent) {
        return value * Math.max(0, 1 + percent / 100);
    }

    private record PercentBuff(Stat stat, double percent, long endMillis) {
    }

    /**
     * Their stats have changed since they were last worked out this tick (a dungeon ghost revived gets
     * its class back): the next {@link #stats()} works them out again.
     */
    public void invalidateStats() {
        stats = null;
    }

    /**
     * Gives them these stats for this long ("gain +50 ✦ Speed for 3 seconds"), in place of what the same
     * source gave them before: using it again starts it again, it doesn't stack. They count at once.
     */
    public void buff(String source, Stats stats, long millis) {
        buffs.put(source, new Buff(stats.copy(), System.currentTimeMillis() + millis));
        this.stats = null;
    }

    private record Buff(Stats stats, long endMillis) {
    }

    /** Their mana pool: 100, and 1 for each point of intelligence. */
    public int maxMana() {
        return Utils.doubleToInt(100.0 + stats().get(Stat.INTELLIGENCE));
    }

    /** Null once it has run out. */
    public Replacement getDefenseReplacement() {
        if (defenseReplacement != null && defenseReplacement.expired()) defenseReplacement = null;
        return defenseReplacement;
    }

    public void setDefenseReplacement(Replacement replacement) {
        this.defenseReplacement = replacement;
    }

    /** Null once it has run out. */
    public Replacement getManaReplacement() {
        if (manaReplacement != null && manaReplacement.expired()) manaReplacement = null;
        return manaReplacement;
    }

    public void setManaReplacement(Replacement replacement) {
        this.manaReplacement = replacement;
    }

    /** Milliseconds left on a cooldown; 0 if it's ready. */
    public long cooldownLeft(String key) {
        Long end = cooldownEnds.get(key);
        return end == null ? 0 : Math.max(0, end - System.currentTimeMillis());
    }

    public void startCooldown(String key, long millis) {
        cooldownEnds.put(key, System.currentTimeMillis() + millis);
    }
}
