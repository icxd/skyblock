package net.icxd.dungeons.mob;

import net.icxd.dungeons.combat.HitKind;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A SkyBlock mob of some {@link MobKind} died, after its drops: one {@link Mobs} spawned, or one of the
 * Watcher's undeads ({@link MobKinds#WATCHER_UNDEAD}). Combat XP, coins, bestiary kills and a room's
 * starred mobs are for listeners to count from it; nothing is awarded before. {@code killer} is null
 * when no player killed it (/kill); the {@link #killingBlow} says how they did. Main thread.
 */
public final class SkyBlockMobDeathEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player killer;
    private final MobKind kind;
    private final MobKind.Variant variant;
    private final boolean starred;
    private final Modifier modifier;
    private final Location location;
    private final DataMob mob;
    private final KillingBlow blow;

    public SkyBlockMobDeathEvent(Player killer, DataMob mob, Location location) {
        this(killer, mob, location, null);
    }

    public SkyBlockMobDeathEvent(Player killer, DataMob mob, Location location, KillingBlow blow) {
        this(killer, mob.kind(), mob.variant(), mob.starred(), mob.modifier(), location, mob, blow);
    }

    /** A mob of this kind that {@link Mobs} didn't spawn. */
    public SkyBlockMobDeathEvent(Player killer, MobKind kind, MobKind.Variant variant, boolean starred, Modifier modifier, Location location) {
        this(killer, kind, variant, starred, modifier, location, null, null);
    }

    public SkyBlockMobDeathEvent(Player killer, MobKind kind, MobKind.Variant variant, boolean starred, Modifier modifier, Location location,
                                 KillingBlow blow) {
        this(killer, kind, variant, starred, modifier, location, null, blow);
    }

    private SkyBlockMobDeathEvent(Player killer, MobKind kind, MobKind.Variant variant, boolean starred, Modifier modifier, Location location,
                                  DataMob mob, KillingBlow blow) {
        this.killer = killer;
        this.kind = kind;
        this.variant = variant;
        this.starred = starred;
        this.modifier = modifier;
        this.location = location.clone();
        this.mob = mob;
        this.blow = killer == null ? null : blow;
    }

    /**
     * The damage that killed it: a melee hit, an arrow, a Ferocity strike, an ability, a damage over time or
     * another effect, with the item behind it and the overkill; null when nobody killed it, or it isn't known.
     */
    public KillingBlow killingBlow() {
        return blow;
    }

    /** Whether the killing blow was of this kind (false when it isn't known). */
    public boolean killedBy(HitKind kind) {
        return blow != null && blow.kind() == kind;
    }

    /** Who killed it; null if nobody did. */
    public Player killer() {
        return killer;
    }

    public MobKind kind() {
        return kind;
    }

    /** Its level and stats where it spawned (combatXp and coins are what a kill is worth, before any bonus). */
    public MobKind.Variant variant() {
        return variant;
    }

    public boolean starred() {
        return starred;
    }

    /** Its modifier; null for none. */
    public Modifier modifier() {
        return modifier;
    }

    /** Where it died. */
    public Location location() {
        return location.clone();
    }

    /** The mob itself (its max health and damage, with the room's multiplier); null if {@link Mobs} didn't spawn it. */
    public DataMob mob() {
        return mob;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
