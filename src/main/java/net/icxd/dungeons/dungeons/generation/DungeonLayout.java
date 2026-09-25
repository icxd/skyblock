package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/** The finished dungeon map: which room is where, how it's rotated, and where the doors are. */
public final class DungeonLayout {

  /**
   * @param rotation clockwise quarter turns applied to the template (rotate the schematic by
   *                 {@code rotation * 90} degrees around Y before pasting it at {@link #origin()})
   * @param parent   room id this room is entered from, -1 for the entrance
   */
  public record PlacedRoom(
      int id,
      RoomType type,
      RoomShape shape,
      Room template,
      int rotation,
      List<Position> cells,
      int parent,
      int depth,
      List<Door> doors
  ) {
    /** Min corner of the footprint, i.e. where the rotated schematic's min corner goes. */
    public Position origin() {
      return RoomShape.minCorner(cells);
    }
  }

  /** A door on the wall between {@code parent} and {@code child} (child is entered through it). */
  public record Door(Edge edge, int parent, int child, DoorType type) {
  }

  private final int width;
  private final int height;
  private final int[][] grid;
  private final List<PlacedRoom> rooms;
  private final List<Door> doors;
  private final List<Integer> criticalPath;
  private final long seed;
  private final int attempts;
  private final Map<Edge, Door> doorsByEdge = new HashMap<>();

  DungeonLayout(int[][] grid, List<PlacedRoom> rooms, List<Door> doors, List<Integer> criticalPath, long seed, int attempts) {
    this.width = grid.length;
    this.height = grid[0].length;
    this.grid = grid;
    this.rooms = List.copyOf(rooms);
    this.doors = List.copyOf(doors);
    this.criticalPath = List.copyOf(criticalPath);
    this.seed = seed;
    this.attempts = attempts;
    for (Door d : doors) doorsByEdge.put(d.edge(), d);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public long getSeed() {
    return seed;
  }

  /** How many full retries generation needed (1 = first try worked). */
  public int getAttempts() {
    return attempts;
  }

  public List<PlacedRoom> getRooms() {
    return rooms;
  }

  public List<Door> getDoors() {
    return doors;
  }

  /** Room ids from the entrance to the blood room, in order. */
  public List<Integer> getCriticalPath() {
    return criticalPath;
  }

  public PlacedRoom roomAt(Position cell) {
    return rooms.get(grid[cell.x()][cell.y()]);
  }

  public Door doorAt(Edge edge) {
    return doorsByEdge.get(edge);
  }

  public List<PlacedRoom> roomsOfType(RoomType type) {
    List<PlacedRoom> out = new ArrayList<>();
    for (PlacedRoom r : rooms) if (r.type() == type) out.add(r);
    return out;
  }

  /**
   * Draws the map with ASCII: {@code #} normal door, {@code E} entrance door, {@code W} wither
   * door, {@code F} fairy door, {@code B} blood door, no wall between cells of the same room. Labels: S entrance,
   * F fairy, BL blood, P puzzle, T trap, M miniboss, R rare, r regular ({@code r*} = on the
   * critical path), followed by the room id.
   */
  public String render() {
    final int cw = 7; // inner width of a cell
    StringBuilder sb = new StringBuilder();
    for (int y = 0; y <= height; y++) {
      // Wall line above row y.
      for (int x = 0; x <= width; x++) {
        sb.append(corner(x, y));
        if (x == width) break;
        boolean open = y > 0 && y < height && grid[x][y] == grid[x][y - 1];
        char door = y > 0 && y < height ? doorChar(new Edge(new Position(x, y - 1), new Position(x, y))) : 0;
        String seg = (open ? " " : "-").repeat(cw);
        if (door != 0) seg = seg.substring(0, cw / 2) + door + seg.substring(cw / 2 + 1);
        sb.append(seg);
      }
      sb.append('\n');
      if (y == height) break;
      // Room line.
      for (int x = 0; x <= width; x++) {
        boolean open = x > 0 && x < width && grid[x][y] == grid[x - 1][y];
        char door = x > 0 && x < width ? doorChar(new Edge(new Position(x - 1, y), new Position(x, y))) : 0;
        sb.append(door != 0 ? door : open ? ' ' : '|');
        if (x == width) break;
        sb.append(center(label(roomAt(new Position(x, y)), new Position(x, y)), cw));
      }
      sb.append('\n');
    }
    return sb.toString();
  }

  /** '+' wherever walls meet, blank in the middle of a 2x2 room. */
  private char corner(int x, int y) {
    if (x == 0 || y == 0 || x == width || y == height) return '+';
    int id = grid[x][y];
    return grid[x - 1][y] == id && grid[x][y - 1] == id && grid[x - 1][y - 1] == id ? ' ' : '+';
  }

  private char doorChar(Edge edge) {
    Door d = doorsByEdge.get(edge);
    if (d == null) return 0;
    return switch (d.type()) {
      case NORMAL -> '#';
      case ENTRANCE -> 'E';
      case WITHER -> 'W';
      case FAIRY -> 'F';
      case BLOOD -> 'B';
    };
  }

  private String label(PlacedRoom room, Position cell) {
    // Label only the top-left cell of multi-cell rooms so the shape reads clearly.
    Position first = room.cells().get(0);
    for (Position c : room.cells()) {
      if (c.y() < first.y() || (c.y() == first.y() && c.x() < first.x())) first = c;
    }
    if (!first.equals(cell)) return "";
    String s = switch (room.type()) {
      case START -> "S";
      case BLOOD -> "BL";
      case FAIRY -> "F";
      case PUZZLE -> "P";
      case TRAP -> "T";
      case MINIBOSS -> "M";
      case REGULAR -> criticalPath.contains(room.id()) ? "r*" : "r";
      case RARE -> "R";
      case EMPTY -> "";
    };
    return s + room.id();
  }

  private static String center(String s, int width) {
    if (s.length() >= width) return s.substring(0, width);
    int left = (width - s.length()) / 2;
    return " ".repeat(left) + s + " ".repeat(width - s.length() - left);
  }
}
