package net.icxd.dungeons.combat;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SkyBlock's own status effects on mobs (vanilla fire and poison on them stay cancelled, see Mobs): per mob,
 * the {@link Debuffs} players' effects put on it (Defense taken off, more damage taken, a slow) and its
 * damages over time, which tick through the same path as any damage (health, number, kill and drops for
 * the player who put it on) as {@link HitKind#DOT}. Hits read them where they're worked out: the target's
 * Defense ({@link #target}, through {@link MobHits#target}) and what the hit does ({@link #takenFactor}).
 * Main thread; {@link #tick} runs every tick with the mobs' own.
 */
public final class MobDebuffs {
    /** Transient, on the mob's speed while it's slowed. */
    private static final NamespacedKey SLOW = new NamespacedKey("dungeons", "debuff_slow");

    private static final class Tracked {
        final LivingEntity entity;
        final Debuffs debuffs = new Debuffs();
        double slowed;

        Tracked(LivingEntity entity) {
            this.entity = entity;
        }
    }

    private record Due(LivingEntity entity, UUID by, double damage, DamageIndicators.Look look) {
    }

    private static final Map<UUID, Tracked> MOBS = new HashMap<>();
    /** The damages over time due this tick, dealt once the mobs have all been looked at (a kill forgets its mob). */
    private static final List<Due> DUE = new ArrayList<>();
    /** The mob whose damages over time are ticking (so the sink needn't be made anew for each mob, each tick). */
    private static LivingEntity ticking;
    private static final Debuffs.DotSink SINK = (source, by, damage, look) -> DUE.add(new Due(ticking, by, damage, look));

    private MobDebuffs() {
    }

    private static Tracked tracked(LivingEntity mob) {
        return MOBS.computeIfAbsent(mob.getUniqueId(), id -> new Tracked(mob));
    }

    /** Puts a debuff on (again) a mob that can be hurt (see {@link Debuffs#add}); {@code by} is who did (null for nobody). */
    public static void add(LivingEntity mob, Debuffs.Spec spec, Player by) {
        if (!MobHits.hittable(mob)) return;
        Tracked tracked = tracked(mob);
        tracked.debuffs.add(spec, by == null ? null : by.getUniqueId(), System.currentTimeMillis());
        if (spec.kind() == Debuffs.Kind.SLOW) slow(tracked, System.currentTimeMillis());
    }

    /** Its stacks of this source's debuff; 0 for none. */
    public static int stacks(Entity mob, String source) {
        Tracked tracked = mob == null ? null : MOBS.get(mob.getUniqueId());
        return tracked == null ? 0 : tracked.debuffs.stacks(source, System.currentTimeMillis());
    }

    /** Takes this source's debuff and damage over time off it. */
    public static void remove(Entity mob, String source) {
        Tracked tracked = mob == null ? null : MOBS.get(mob.getUniqueId());
        if (tracked == null) return;
        tracked.debuffs.remove(source);
        slow(tracked, System.currentTimeMillis());
    }

    /** A hit's target as its debuffs make it: its Defense less what they take off. The same target if it has none. */
    public static Damage.Target target(Entity mob, Damage.Target raw) {
        Tracked tracked = raw == null || mob == null ? null : MOBS.get(mob.getUniqueId());
        if (tracked == null) return raw;
        double defense = tracked.debuffs.defense(raw.defense(), System.currentTimeMillis());
        if (defense == raw.defense()) return raw;
        return new Damage.Target(raw.health(), raw.maxHealth(), defense, raw.magicResistance(), raw.types(), raw.hitsTaken(), raw.caps());
    }

    /** What a player's hit on it is multiplied by: 1.1 while it's frozen, and so on (1 for none). */
    public static double takenFactor(Entity mob) {
        Tracked tracked = mob == null ? null : MOBS.get(mob.getUniqueId());
        return tracked == null ? 1 : tracked.debuffs.takenFactor(System.currentTimeMillis());
    }

    /**
     * Starts (or renews: see {@link Debuffs#dot}) a damage over time on a mob that can be hurt: {@code damage}
     * every {@code every} ticks, {@code times} times, credited to {@code by} (their kill, drops and damage
     * dealt), its numbers looking as {@code look} (gold for fire, dark green for poison: the wiki's Damage
     * Calculation). The damage is dealt as it's given: its Defense and the rest are the caller's to work in.
     */
    public static void dot(LivingEntity mob, String source, Player by, double damage, int every, int times, DamageIndicators.Look look) {
        if (by == null || !MobHits.hittable(mob)) return;
        tracked(mob).debuffs.dot(source, by.getUniqueId(), damage, every, times, look);
    }

    /** Whether this source's damage over time is still going on it. */
    public static boolean hasDot(Entity mob, String source) {
        Tracked tracked = mob == null ? null : MOBS.get(mob.getUniqueId());
        return tracked != null && tracked.debuffs.hasDot(source);
    }

    /** It died or went: nothing of it is kept. */
    public static void forget(Entity mob) {
        if (mob == null) return;
        Tracked tracked = MOBS.remove(mob.getUniqueId());
        if (tracked != null && tracked.slowed > 0) setSlow(tracked.entity, 0);
    }

    /** Every tick: damages over time that are due deal, what has run out comes off, and mobs that have gone are forgotten. */
    public static void tick() {
        if (MOBS.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Iterator<Tracked> it = MOBS.values().iterator(); it.hasNext(); ) {
            Tracked tracked = it.next();
            if (!MobHits.hittable(tracked.entity)) {
                if (tracked.slowed > 0 && tracked.entity.isValid()) setSlow(tracked.entity, 0);
                it.remove();
                continue;
            }
            ticking = tracked.entity;
            tracked.debuffs.tick(SINK);
            ticking = null;
            boolean empty = tracked.debuffs.expire(now);
            slow(tracked, now);
            if (empty) it.remove();
        }
        if (DUE.isEmpty()) return;
        List<Due> due = new ArrayList<>(DUE);
        DUE.clear();
        for (Due hit : due) {
            Player by = Bukkit.getPlayer(hit.by());
            if (by != null) MobHits.deal(by, hit.entity(), hit.damage(), hit.look(), HitKind.DOT, null);
        }
    }

    /** Its speed follows its slows: a share of it taken off while they last. */
    private static void slow(Tracked tracked, long now) {
        double share = tracked.debuffs.of(Debuffs.Kind.SLOW, now);
        if (share == tracked.slowed) return;
        tracked.slowed = share;
        setSlow(tracked.entity, share);
    }

    /**
     * Its movement speed attribute less {@code share} (none for 0). Mobs that steer themselves rather than walk
     * by it (the Blood Room's player-shaped undeads) aren't slowed: UNKNOWN how Hypixel slows those.
     */
    private static void setSlow(LivingEntity entity, double share) {
        AttributeInstance speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed == null) return;
        if (speed.getModifier(SLOW) != null) speed.removeModifier(SLOW);
        if (share > 0) speed.addTransientModifier(new AttributeModifier(SLOW, -share, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }
}
