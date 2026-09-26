package net.icxd.dungeons.dungeons.generation.room;

import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * A wall of one of a template's cells where a door is allowed, in the template's own (rotation 0)
 * coordinates, i.e. relative to {@link RoomShape#cells()}.
 */
public record DoorSlot(Position cell, Direction side) {

    public static DoorSlot of(int x, int y, Direction side) {
        return new DoorSlot(new Position(x, y), side);
    }
}
