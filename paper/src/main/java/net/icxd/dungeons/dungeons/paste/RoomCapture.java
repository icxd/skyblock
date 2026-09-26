package net.icxd.dungeons.dungeons.paste;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.room.DoorSlot;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * One room captured by the dungeon scanner: a schematic plus the JSON next to it.
 *
 * <p>Captures are saved in a canonical frame (the roof marker in the north-west corner), so
 * {@link #cells} and {@link #doors} are in that frame too. The generator's template frame is
 * {@link RoomShape#cells()}; the canonical frame is that rotated {@link #frameTurns} clockwise. For
 * 1x1, straight and 2x2 rooms the two are the same; L rooms are captured with a different corner
 * missing than {@code RoomShape.L_SHAPE}.
 *
 * @param id         the scanner's id, e.g. {@code lower_blaze}
 * @param templateId the generator template this is a variant of, e.g. {@code blaze}
 * @param doors      doorways that were open when captured, and the door that was in them
 * @param size       schematic size {x, y, z}
 */
public record RoomCapture(
    String id,
    String templateId,
    RoomType type,
    RoomShape shape,
    int frameTurns,
    int originY,
    int[] size,
    List<Position> cells,
    Map<DoorSlot, DoorType> doors,
    Path schematic
) {
  /** Highest block y of the room. */
  public int topY() {
    return originY + size[1] - 1;
  }
}
