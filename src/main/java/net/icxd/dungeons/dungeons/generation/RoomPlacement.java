package net.icxd.dungeons.dungeons.generation;

import java.util.List;

import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomSize;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * One placed room instance in phase 1 (tree node). {@link #parentId()} is -1 for the start room.
 */
public record RoomPlacement(
    int id,
    int parentId,
    Room template,
    RoomSize size,
    int rotationIndex,
    List<Position> cells
) {
}
