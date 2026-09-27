package net.icxd.dungeons.dungeons.instance;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;

/**
 * A room of a run was cleared: its starred mobs are dead (it has its tick on the map, and its loot is on the
 * floor). Once per room; not for the rooms with nothing to clear (the fairy room) or the Blood Room. Main
 * thread.
 */
public final class RoomClearedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final DungeonRun run;
    private final PlacedRoom room;

    public RoomClearedEvent(DungeonRun run, PlacedRoom room) {
        this.run = run;
        this.room = room;
    }

    public DungeonRun run() {
        return run;
    }

    public PlacedRoom room() {
        return room;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
