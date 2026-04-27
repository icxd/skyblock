package net.icxd.dungeons.dungeons.generator;

import lombok.Getter;
import net.icxd.dungeons.dungeons.generator.rooms.Room;
import net.icxd.dungeons.dungeons.generator.rooms.RoomType;
import net.icxd.dungeons.dungeons.generator.utils.Coordinate;
import net.icxd.dungeons.dungeons.generator.utils.Random;

import java.util.Arrays;

@Getter
public class Grid {
  private final Room[][] rooms;
  private final int width, height;

  public Grid(int width, int height) {
    this.width = width;
    this.height = height;

    this.rooms = new Room[this.width][this.height];

    for (int i = 0; i < this.width * this.height; i++) {
      final int x = i / this.width, y = i - (x * this.width);
      this.rooms[x][y] = new Room(RoomType.NONE, new Coordinate(x, y));
    }
  }

  public void generate() {
    placeRedRooms();
    placeBlueAndGreenRooms();
    fillRemainingRooms();
  }

  private void fillRemainingRooms() {
    for (int i = 0; i < this.width * this.height; i++) {
      final int y = i / this.width, x = i - (y * this.width);

      if (rooms[x][y].type() == RoomType.NONE)
        rooms[x][y] = new Room(Random.random(Arrays.asList(RoomType.GREEN, RoomType.BLUE)), new Coordinate(x, y));
    }
  }

  private void placeBlueAndGreenRooms() {
    final int[][] directions = {{1, 0},{-1,0},{0,1},{0,-1}};
    for (int i = 0; i < this.width * this.height; i++) {
      final int y = i / this.width, x = i - (y* this.width);

      if (rooms[x][y].type() == RoomType.RED) {
        int blueCount = 0, greenCount = 0;

        for (int[] d : directions) {
          final Coordinate neighbor = new Coordinate(x + d[0], y + d[1]);

          if (blueCount > 0 && greenCount > 0) break;

          if (isValidCoordinate(neighbor) && rooms[neighbor.x()][neighbor.y()].type() == RoomType.NONE) {
            if (blueCount == 0) {
              rooms[neighbor.x()][neighbor.y()] = new Room(RoomType.BLUE, neighbor);
              blueCount++;
            }

            if (greenCount == 0) {
              rooms[neighbor.x()][neighbor.y()] = new Room(RoomType.GREEN, neighbor);
              greenCount++;
            }
          }
        }
      }
    }
  }


  private void placeRedRooms() {
    var random = new java.util.Random();
    int placed = 0;

    while (placed < 20) {
      final int x = random.nextInt(this.width), y = random.nextInt(this.height);
      final Coordinate coordinate = new Coordinate(x, y);

      if (!hasRedNeighbor(coordinate) && rooms[x][y].type() == RoomType.NONE) {
        rooms[x][y] = new Room(RoomType.RED, coordinate);
        placed++;
      }
    }
  }

  private boolean hasRedNeighbor(Coordinate c) {
    int[][] directions = {{1, 0},{-1,0},{0,1},{0,-1}};
    for (int[] d : directions) {
      Coordinate neighbor = new Coordinate(c.x() + d[0], c.y() + d[1]);
      if (!isValidCoordinate(neighbor)) continue;
      if (rooms[neighbor.x()][neighbor.y()].type() == RoomType.RED)
        return true;
    }

    return false;
  }

  public boolean isValidCoordinate(Coordinate c) {
    return c.x() >= 0 && c.x() < this.width && c.y() >= 0 && c.y() < this.height;
  }

  public void print() {
    for (Room[] value : rooms) {
      StringBuilder builder = new StringBuilder();
      for (final Room room : value)
        builder.append(room.type().color()).append("x ");
      System.out.print(builder.append("\033[0m"));
    }
  }

  public Room getRoom(Coordinate c) { return this.rooms[c.x()][c.y()]; }
}
