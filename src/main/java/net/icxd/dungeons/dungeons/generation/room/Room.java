package net.icxd.dungeons.dungeons.generation.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import net.icxd.dungeons.dungeons.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.secrets.Secret;

/**
 * A room template (prefab). The generator decides where rooms go and which walls get doors, then
 * picks a template + rotation for each footprint that allows exactly those doors.
 *
 * <p>Door rules are expressed in the template's own rotation-0 coordinates (see
 * {@link RoomShape#cells()}):
 * <ul>
 *   <li>{@link #doorSlots}: walls a door may be on. Empty means every outside wall.
 *   <li>{@link #exactDoors}: every slot <em>must</em> be a door. This is how Hypixel's 1x1 rooms
 *       work: each one is built with a fixed set of doorways (straight, corner, T, cross or dead
 *       end), and the generator has to rotate it so they line up with its neighbours.
 *   <li>{@link #maxDoors}: cap on the number of doors for rooms without exact slots.
 * </ul>
 */
@Builder
@Getter
public class Room {
  private final String id;
  private final RoomType type;
  private final RoomShape shape;
  @Builder.Default
  private final Set<DoorSlot> doorSlots = Set.of();
  @Builder.Default
  private final boolean exactDoors = false;
  @Builder.Default
  private final int maxDoors = Integer.MAX_VALUE;
  @Builder.Default
  private final DungeonFloor minimumFloor = DungeonFloor.ENTRANCE;
  @Builder.Default
  private final List<Secret> secrets = new ArrayList<>();

  /** Most doors this template can ever have. */
  public int doorLimit() {
    return exactDoors ? doorSlots.size() : maxDoors;
  }

  @Override
  public String toString() {
    return id;
  }
}
