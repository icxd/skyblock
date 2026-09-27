package net.icxd.dungeons.mob;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A SkyBlock mob of some {@link MobKind} died, after its drops. Combat XP, coins, bestiary kills and a
 * room's starred mobs are for listeners to count from it; nothing is awarded before. {@code killer}
 * is null when no player killed it (/kill). Main thread.
 */
public final class SkyBlockMobDeathEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player killer;
    private final DataMob mob;
    private final Location location;

    public SkyBlockMobDeathEvent(Player killer, DataMob mob, Location location) {
        this.killer = killer;
        this.mob = mob;
        this.location = location.clone();
    }

    /** Who killed it; null if nobody did. */
    public Player killer() {
        return killer;
    }

    public MobKind kind() {
        return mob.kind();
    }

    /** Its level and stats where it spawned (combatXp and coins are what a kill is worth, before any bonus). */
    public MobKind.Variant variant() {
        return mob.variant();
    }

    public boolean starred() {
        return mob.starred();
    }

    /** Its modifier; null for none. */
    public Modifier modifier() {
        return mob.modifier();
    }

    /** Where it died. */
    public Location location() {
        return location.clone();
    }

    /** The mob itself (its max health and damage, with the room's multiplier). */
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
