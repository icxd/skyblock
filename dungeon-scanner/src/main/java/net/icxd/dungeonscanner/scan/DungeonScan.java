package net.icxd.dungeonscanner.scan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.icxd.dungeonscanner.scan.RoomDatabase.RoomInfo;
import net.icxd.dungeonscanner.scan.ScannedDungeon.Cell;
import net.icxd.dungeonscanner.scan.ScannedDungeon.Door;
import net.icxd.dungeonscanner.scan.ScannedDungeon.DoorType;
import net.icxd.dungeonscanner.scan.ScannedDungeon.Room;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reads the dungeon layout out of the world. The Catacombs always sit on the same grid: cells of
 * 31x31 blocks with a 1-block gap, the first cell's centre at -185,-185, up to 6x6 cells.
 *
 * <p>Between two neighbouring cells there's either nothing (different rooms, no door), a doorway
 * whose column tops out at y=73 (or 81 for Old Trap) with the door block at y=69, or the room
 * itself continuing (same room). Same rules as Skytils/FunnyMap's scanner.
 */
public final class DungeonScan {
  public static final int CENTER = -185;
  public static final int PITCH = 32;
  public static final int HALF = 15;
  public static final int MAX_CELLS = 6;
  private static final int DOOR_BLOCK_Y = 69;

  private enum Link { NONE, DOOR, SAME_ROOM, UNKNOWN }

  private DungeonScan() {
  }

  public static ScannedDungeon scan(WorldView world, RoomDatabase db) {
    RoomCore[][] cores = new RoomCore[MAX_CELLS][MAX_CELLS];
    boolean allCellsKnown = true;
    for (int x = 0; x < MAX_CELLS; x++) {
      for (int z = 0; z < MAX_CELLS; z++) {
        Cell c = new Cell(x, z);
        if (!world.isLoaded(c.centerX(), c.centerZ())) {
          allCellsKnown = false;
          continue;
        }
        RoomCore core = RoomCore.at(world, c.centerX(), c.centerZ());
        if (core.highest() > 0) cores[x][z] = core;
      }
    }

    // Link every pair of neighbouring cells, then group cells into rooms.
    int[] parent = new int[MAX_CELLS * MAX_CELLS];
    for (int i = 0; i < parent.length; i++) parent[i] = i;
    List<Door> doors = new ArrayList<>();
    Map<Long, Link> links = new HashMap<>();
    for (int x = 0; x < MAX_CELLS; x++) {
      for (int z = 0; z < MAX_CELLS; z++) {
        if (cores[x][z] == null) continue;
        for (int[] d : new int[][]{{1, 0}, {0, 1}}) {
          int nx = x + d[0];
          int nz = z + d[1];
          if (nx >= MAX_CELLS || nz >= MAX_CELLS || cores[nx][nz] == null) continue;
          Cell a = new Cell(x, z);
          Cell b = new Cell(nx, nz);
          int gx = a.centerX() + d[0] * (PITCH / 2);
          int gz = a.centerZ() + d[1] * (PITCH / 2);
          Link link = link(world, db, cores[x][z], cores[nx][nz], gx, gz);
          links.put(key(a, b), link);
          if (link == Link.SAME_ROOM) union(parent, x * MAX_CELLS + z, nx * MAX_CELLS + nz);
          if (link == Link.DOOR) doors.add(new Door(a, b, doorType(world, db, cores[x][z], cores[nx][nz], gx, gz), gx, gz));
        }
      }
    }

    Map<Integer, List<Cell>> groups = new HashMap<>();
    for (int x = 0; x < MAX_CELLS; x++) {
      for (int z = 0; z < MAX_CELLS; z++) {
        if (cores[x][z] != null) groups.computeIfAbsent(find(parent, x * MAX_CELLS + z), k -> new ArrayList<>()).add(new Cell(x, z));
      }
    }

    List<Room> rooms = new ArrayList<>();
    int width = 0;
    int height = 0;
    for (List<Cell> cells : groups.values()) {
      cells.sort(Comparator.comparingInt(Cell::z).thenComparingInt(Cell::x));
      rooms.add(room(world, db, cores, cells, links));
      for (Cell c : cells) {
        width = Math.max(width, c.x() + 1);
        height = Math.max(height, c.z() + 1);
      }
    }
    rooms.sort(Comparator.comparingInt((Room r) -> r.cells().get(0).z()).thenComparingInt(r -> r.cells().get(0).x()));
    return new ScannedDungeon(rooms, doors, width, height, allCellsKnown);
  }

  private static Link link(WorldView world, RoomDatabase db, RoomCore a, RoomCore b, int gx, int gz) {
    if (!world.isLoaded(gx, gz)) return Link.UNKNOWN;
    RoomInfo ia = db.byCore(a.hash());
    RoomInfo ib = db.byCore(b.hash());
    // Rooms are unique per dungeon: two cells of the same known room are the same room.
    if (ia != null && ia == ib) return Link.SAME_ROOM;

    int top = topBlock(world, gx, gz);
    if (top < world.minY()) return Link.NONE;
    int height = top + 1;
    if (height == 74 || height == 82) return Link.DOOR;
    // The entrance is never merged with a neighbour; a "connection" next to it is its door.
    if (isEntrance(ia) || isEntrance(ib)) return Link.DOOR;
    return Link.SAME_ROOM;
  }

  private static DoorType doorType(WorldView world, RoomDatabase db, RoomCore a, RoomCore b, int gx, int gz) {
    if (isEntrance(db.byCore(a.hash())) || isEntrance(db.byCore(b.hash()))) return DoorType.ENTRANCE;
    Block block = world.getBlockState(gx, DOOR_BLOCK_Y, gz).getBlock();
    String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
    if (block == Blocks.COAL_BLOCK) return DoorType.WITHER;
    if (id.startsWith("infested_")) return DoorType.ENTRANCE;
    if (id.endsWith("terracotta") && !id.contains("glazed")) return DoorType.BLOOD;
    return DoorType.NORMAL;
  }

  private static boolean isEntrance(RoomInfo info) {
    return info != null && info.type().equals("ENTRANCE");
  }

  private static Room room(WorldView world, RoomDatabase db, RoomCore[][] cores, List<Cell> cells, Map<Long, Link> links) {
    RoomInfo info = null;
    int roof = 0;
    for (Cell c : cells) {
      RoomCore core = cores[c.x()][c.z()];
      if (info == null) info = db.byCore(core.hash());
      roof = Math.max(roof, core.highest());
    }
    Cell first = cells.get(0);
    int core = cores[first.x()][first.z()].hash();
    String id = info != null ? info.id() : "unknown_" + Integer.toHexString(core);

    int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    for (Cell c : cells) {
      minX = Math.min(minX, c.centerX() - HALF);
      minZ = Math.min(minZ, c.centerZ() - HALF);
      maxX = Math.max(maxX, c.centerX() + HALF);
      maxZ = Math.max(maxZ, c.centerZ() + HALF);
    }

    // Blue terracotta marker on one corner of the roof; the fairy room has none. Straight rooms and
    // 2x2s only ever face one way per footprint (horizontal/2x2 SOUTH, vertical WEST; checked on
    // real captures), so for them the corner is known and only verified. Some rooms have more blue
    // terracotta on the roof edge, so the convention wins over a marker found elsewhere.
    int[][] corners = {{minX, minZ}, {maxX, minZ}, {maxX, maxZ}, {minX, maxZ}};
    RoomRotation[] forCorner = {RoomRotation.SOUTH, RoomRotation.WEST, RoomRotation.NORTH, RoomRotation.EAST};
    String shape = info != null ? info.shape() : shapeOf(cells);
    RoomRotation rotation = null;
    boolean markerFound = false;
    int clayX = minX, clayY = roof, clayZ = minZ;
    if (info != null && info.type().equals("FAIRY")) {
      rotation = RoomRotation.SOUTH;
      markerFound = true;
    } else if (!shape.equals("1x1") && !shape.equals("L")) {
      int corner = shape.equals("2x2") || maxX - minX > maxZ - minZ ? 0 : 1;
      rotation = forCorner[corner];
      clayX = corners[corner][0];
      clayZ = corners[corner][1];
      int y = markerY(world, clayX, clayZ, roof);
      markerFound = y != Integer.MIN_VALUE;
      if (markerFound) clayY = y;
    } else {
      for (int i = 0; i < 4 && rotation == null; i++) {
        int y = markerY(world, corners[i][0], corners[i][1], roof);
        if (y == Integer.MIN_VALUE) continue;
        rotation = forCorner[i];
        markerFound = true;
        clayX = corners[i][0];
        clayY = y;
        clayZ = corners[i][1];
      }
    }

    boolean complete = info == null || cells.size() == info.cellCount();
    for (int x = minX - 1; x <= maxX + 1 && complete; x += 16) {
      for (int z = minZ - 1; z <= maxZ + 1 && complete; z += 16) {
        if (!world.isLoaded(x, z)) complete = false;
      }
    }
    if (!world.isLoaded(maxX + 1, maxZ + 1)) complete = false;
    for (Cell c : cells) {
      for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
        Cell n = new Cell(c.x() + d[0], c.z() + d[1]);
        if (n.x() < 0 || n.z() < 0 || n.x() >= MAX_CELLS || n.z() >= MAX_CELLS) continue;
        if (cores[n.x()][n.z()] == null && !world.isLoaded(n.centerX(), n.centerZ())) complete = false;
        Link l = links.get(d[0] + d[1] > 0 ? key(c, n) : key(n, c));
        if (l == Link.UNKNOWN) complete = false;
      }
    }
    return new Room(id, info, List.copyOf(cells), core, roof, rotation, markerFound, clayX, clayY, clayZ, complete);
  }

  /**
   * Height of the roof marker in a corner column, or MIN_VALUE. Looks around the roof height from
   * the core scan first, then at the top of the column: the core scan stops at y=140 and some
   * rooms (Supertall, Cathedral) are taller.
   */
  private static int markerY(WorldView world, int x, int z, int roof) {
    Block clay = Blocks.DYED_TERRACOTTA.blue();
    for (int y = roof + 2; y >= roof - 4; y--) {
      if (world.getBlockState(x, y, z).getBlock() == clay) return y;
    }
    int top = topBlock(world, x, z);
    for (int y = top; y >= top - 3 && y > roof + 2; y--) {
      if (world.getBlockState(x, y, z).getBlock() == clay) return y;
    }
    return Integer.MIN_VALUE;
  }

  private static int topBlock(WorldView world, int x, int z) {
    for (int y = Math.min(255, world.maxY() - 1); y >= world.minY(); y--) {
      if (!world.getBlockState(x, y, z).isAir()) return y;
    }
    return world.minY() - 1;
  }

  /** "1x1", "1x2", "1x3", "1x4", "2x2" or "L" from a cell set. */
  public static String shapeOf(List<Cell> cells) {
    int minX = cells.stream().mapToInt(Cell::x).min().orElse(0);
    int maxX = cells.stream().mapToInt(Cell::x).max().orElse(0);
    int minZ = cells.stream().mapToInt(Cell::z).min().orElse(0);
    int maxZ = cells.stream().mapToInt(Cell::z).max().orElse(0);
    boolean square = maxX - minX == 1 && maxZ - minZ == 1;
    return switch (cells.size()) {
      case 1 -> "1x1";
      case 2 -> "1x2";
      case 3 -> square ? "L" : "1x3";
      case 4 -> square ? "2x2" : "1x4";
      default -> "?";
    };
  }

  private static long key(Cell a, Cell b) {
    return ((long) (a.x() * MAX_CELLS + a.z()) << 32) | (b.x() * MAX_CELLS + b.z());
  }

  private static int find(int[] parent, int i) {
    while (parent[i] != i) {
      parent[i] = parent[parent[i]];
      i = parent[i];
    }
    return i;
  }

  private static void union(int[] parent, int a, int b) {
    parent[find(parent, a)] = find(parent, b);
  }
}
