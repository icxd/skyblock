package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import net.icxd.dungeons.dungeons.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.utils.Position;

@Getter
public class Map {
  private final Room[] rooms;
  private final Position[] roomPositions;
  private final int[] roomInstanceIds;
  private final int width, height;

  private List<DoorEdge> doorEdges = List.of();
  private List<RoomPlacement> roomPlacements = List.of();

  public Map(DungeonFloor floor) {
    this.rooms = new Room[floor.getMaxXSize() * floor.getMaxZSize()];
    this.roomPositions = new Position[rooms.length];
    this.roomInstanceIds = new int[rooms.length];
    java.util.Arrays.fill(this.roomInstanceIds, -1);
    this.width = floor.getMaxXSize();
    this.height = floor.getMaxZSize();
  }

  private int index(Position position) {
    return position.x() + position.y() * width;
  }

  /**
   * Legacy overload: instance id is always set to {@code -1}. Prefer
   * {@link #setRoom(Position, Room, int)} when placing rooms during generation.
   */
  public void setRoom(Position position, Room room) {
    setRoom(position, room, -1);
  }

  public void setRoom(Position position, Room room, int instanceId) {
    int i = index(position);
    this.rooms[i] = room;
    this.roomPositions[i] = position;
    this.roomInstanceIds[i] = instanceId;
  }

  public int getRoomInstanceId(Position position) {
    return roomInstanceIds[index(position)];
  }

  public void replaceRoomTemplate(int instanceId, Room newTemplate) {
    for (int i = 0; i < rooms.length; i++) {
      if (roomInstanceIds[i] == instanceId) {
        rooms[i] = newTemplate;
      }
    }
  }

  void setDoorEdges(List<DoorEdge> doorEdges) {
    this.doorEdges = Collections.unmodifiableList(new ArrayList<>(doorEdges));
  }

  void setRoomPlacements(List<RoomPlacement> roomPlacements) {
    this.roomPlacements = Collections.unmodifiableList(new ArrayList<>(roomPlacements));
  }

  public Room getRoom(Position position) {
    return this.rooms[position.x() + position.y() * width];
  }
}
