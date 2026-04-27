package net.icxd.dungeons.dungeons.generator.rooms;

import lombok.Setter;
import net.icxd.dungeons.dungeons.generator.utils.Coordinate;

import java.util.Objects;

public final class Room {
  private final RoomType type;
  private final Coordinate coordinate;
  @Setter
  private RoomSize size = RoomSize.ONE_BY_ONE;

  public Room(RoomType type, Coordinate coordinate) {
    this.type = type;
    this.coordinate = coordinate;
  }

  public RoomType type() { return type; }
  public Coordinate coordinate() { return coordinate; }
  public RoomSize size() { return size; }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (Room) obj;
    return Objects.equals(this.type, that.type) &&
        Objects.equals(this.coordinate, that.coordinate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, coordinate);
  }

  @Override
  public String toString() {
    return "Room[" +
        "type=" + type + ", " +
        "coordinate=" + coordinate + ']';
  }

}
