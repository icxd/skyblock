package net.icxd.dungeons.dungeons.paste;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.DungeonLayout;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.DoorSlot;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Where every schematic of a generated dungeon goes, worked out without touching a world so it can
 * be tested. {@link WorldEditPaster} carries it out in this order: {@link #clears}, {@link #rooms},
 * {@link #doors}, {@link #closings}, {@link #carves}.
 *
 * <p>World layout is Hypixel's: cells are 31x31 blocks with a 1 block gap, so cell {@code (x, y)}
 * starts at {@code base + 32 * (x, y)}. With the default base of -200 the cell centres are at
 * -185 + 32i like on Hypixel. Between two different rooms the gap is air, a door fills it.
 *
 * <p>Doors: a door schematic is 5 blocks along the wall, 3 across (the gap plus the outermost
 * layer of both rooms), from y=67 up. Pasting one therefore also cuts the doorway into both rooms'
 * walls, so a room can get a door where its capture had a wall. The reverse, a doorway that was
 * open in the capture but has no door now, is walled up by copying the wall next to it
 * ({@link #closings}). Behind a doorway that was walled up in the capture there can be more wall;
 * {@link #carves} lists the blocks to clear so the door isn't blocked.
 */
public final class PastePlan {
  public static final int CELL = 31;
  public static final int PITCH = CELL + 1;
  /** Hypixel's first cell starts here on both axes. */
  public static final int HYPIXEL_BASE = -200;
  /** Lowest block of a door (bedrock), and how many layers of the door schematic are pasted. */
  public static final int DOOR_Y = 67;
  public static final int DOOR_HEIGHT = 7;
  /** Along the wall, measured from the cell's corner: a door covers 13-17, you walk through 14-16. */
  static final int DOOR_FROM = 13;
  static final int DOOR_TO = 17;
  static final int OPENING_FROM = 14;
  static final int OPENING_TO = 16;
  /** Walkable height of a doorway: floor at 68, frame top at 73. */
  static final int OPENING_BOTTOM = 69;
  static final int OPENING_TOP = 72;
  /** How far behind a new doorway walls are cleared. */
  static final int CARVE_DEPTH = 2;

  public record Block(int x, int y, int z) {
  }

  /** Inclusive box of blocks. */
  public record Box(Block min, Block max) {
  }

  /**
   * @param turns  clockwise quarter turns from the capture's frame to the world
   * @param center world position the centre column of the schematic goes to, at its lowest y
   * @param parts  the parts of the schematic that belong to the room, in its own coordinates; the
   *               rest (the missing corner of an L) is whatever was next to it when captured
   */
  public record RoomPaste(PlacedRoom room, RoomCapture capture, int turns, Block center, List<Box> parts) {
  }

  /** @param center world position of the door schematic's centre block (in the gap) at {@link #DOOR_Y} */
  public record DoorPaste(Door door, Path schematic, int turns, Block center) {
  }

  /** Copy the block at {@code from} to {@code to}. */
  public record Copy(Block from, Block to) {
  }

  /** Layers of blocks behind a new doorway, outermost first: clear the solid ones, stop at the first layer without any. */
  public record Carve(List<List<Block>> layers) {
  }

  private final DungeonLayout layout;
  private final List<Box> clears = new ArrayList<>();
  private final List<RoomPaste> rooms = new ArrayList<>();
  private final List<DoorPaste> doors = new ArrayList<>();
  private final List<Copy> closings = new ArrayList<>();
  private final List<Carve> carves = new ArrayList<>();
  private final List<String> problems = new ArrayList<>();
  private final int baseX;
  private final int baseZ;
  /** Height range every room fits in. */
  private final int minY;
  private final int maxY;

  private PastePlan(DungeonLayout layout, int baseX, int baseZ, int minY, int maxY) {
    this.layout = layout;
    this.baseX = baseX;
    this.baseZ = baseZ;
    this.minY = minY;
    this.maxY = maxY;
  }

  /**
   * @param seed picks between captured variants of rooms and doors
   */
  public static PastePlan create(DungeonLayout layout, RoomLibrary library, int baseX, int baseZ, long seed) {
    PastePlan plan = new PastePlan(layout, baseX, baseZ, library.minY(), library.maxY());
    Random random = new Random(seed);
    plan.planClears();
    for (PlacedRoom room : layout.getRooms()) plan.planRoom(room, library, random);
    for (Door door : layout.getDoors()) plan.planDoor(door, library, random);
    return plan;
  }

  public List<Box> clears() {
    return clears;
  }

  public List<RoomPaste> rooms() {
    return rooms;
  }

  public List<DoorPaste> doors() {
    return doors;
  }

  public List<Copy> closings() {
    return closings;
  }

  public List<Carve> carves() {
    return carves;
  }

  /** Rooms or doors that couldn't be planned; they're left out. */
  public List<String> problems() {
    return problems;
  }

  /** Centre of the entrance room at y=69 (the floor of the doorways is at 68). */
  public Block entrance() {
    for (PlacedRoom r : layout.getRooms()) {
      if (r.type() == RoomType.START) {
        Position c = r.cells().get(0);
        return new Block(cellMinX(c) + CELL / 2, OPENING_BOTTOM, cellMinZ(c) + CELL / 2);
      }
    }
    return new Block(baseX, OPENING_BOTTOM, baseZ);
  }

  private int cellMinX(Position cell) {
    return baseX + PITCH * cell.x();
  }

  private int cellMinZ(Position cell) {
    return baseZ + PITCH * cell.y();
  }

  /**
   * Everything the rooms don't cover: the gaps between cells, empty special-column cells, and
   * the parts of each room's column above and below it (left over from an earlier paste).
   */
  private void planClears() {
    int w = layout.getWidth();
    int h = layout.getHeight();
    int maxX = baseX + PITCH * w - 2;
    int maxZ = baseZ + PITCH * h - 2;
    for (int i = 0; i + 1 < w; i++) {
      int x = baseX + PITCH * i + CELL;
      clears.add(new Box(new Block(x, minY, baseZ), new Block(x, maxY, maxZ)));
    }
    for (int j = 0; j + 1 < h; j++) {
      int z = baseZ + PITCH * j + CELL;
      clears.add(new Box(new Block(baseX, minY, z), new Block(maxX, maxY, z)));
    }
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        Position cell = new Position(x, y);
        if (layout.roomAt(cell) == null) clears.add(cellBox(cell, cell, minY, maxY));
      }
    }
    // Room columns above/below are added per room once its capture is known (see planRoom).
  }

  private Box cellBox(Position minCell, Position maxCell, int fromY, int toY) {
    return new Box(new Block(cellMinX(minCell), fromY, cellMinZ(minCell)),
        new Block(cellMinX(maxCell) + CELL - 1, toY, cellMinZ(maxCell) + CELL - 1));
  }

  private void planRoom(PlacedRoom room, RoomLibrary library, Random random) {
    List<RoomCapture> variants = library.captures(room.template().getId());
    if (variants.isEmpty()) {
      problems.add("#" + room.id() + " " + room.template().getId() + ": never captured");
      return;
    }
    Set<Edge> wanted = new LinkedHashSet<>();
    for (Door d : room.doors()) wanted.add(d.edge());

    // The variant needing the fewest doorways walled up or cut open; ties at random.
    RoomCapture best = null;
    Set<Edge> bestOpen = null;
    int bestScore = Integer.MAX_VALUE;
    int ties = 0;
    for (RoomCapture capture : variants) {
      Set<Edge> open = openEdges(room, capture);
      int score = 0;
      for (Edge e : open) if (!wanted.contains(e)) score++;
      for (Edge e : wanted) if (!open.contains(e)) score++;
      if (score < bestScore) {
        best = capture;
        bestOpen = open;
        bestScore = score;
        ties = 1;
      } else if (score == bestScore && random.nextInt(++ties) == 0) {
        best = capture;
        bestOpen = open;
      }
    }

    int turns = turns(room, best);
    Position min = RoomShape.minCorner(room.cells());
    Position max = new Position(Integer.MIN_VALUE, Integer.MIN_VALUE);
    for (Position c : room.cells()) max = new Position(Math.max(max.x(), c.x()), Math.max(max.y(), c.y()));
    int sizeX = turns % 2 == 0 ? best.size()[0] : best.size()[2];
    int sizeZ = turns % 2 == 0 ? best.size()[2] : best.size()[0];
    rooms.add(new RoomPaste(room, best, turns,
        new Block(cellMinX(min) + (sizeX - 1) / 2, best.originY(), cellMinZ(min) + (sizeZ - 1) / 2), parts(best)));

    if (best.originY() > minY) clears.add(cellBox(min, max, minY, best.originY() - 1));
    if (best.topY() < maxY) clears.add(cellBox(min, max, best.topY() + 1, maxY));

    for (Edge e : bestOpen) {
      if (!wanted.contains(e)) closings.addAll(closing(room, e));
    }
    for (Edge e : wanted) {
      if (!bestOpen.contains(e)) carves.add(carve(room, e));
    }
  }

  /**
   * The capture's own blocks as boxes in schematic coordinates: its cells, the gaps between two
   * of its cells, and the gap corners with its cells on all four sides.
   */
  static List<Box> parts(RoomCapture capture) {
    Set<Position> cells = new HashSet<>(capture.cells());
    int top = capture.size()[1] - 1;
    List<Box> out = new ArrayList<>();
    for (Position c : capture.cells()) {
      int x = PITCH * c.x();
      int z = PITCH * c.y();
      boolean east = cells.contains(new Position(c.x() + 1, c.y()));
      boolean south = cells.contains(new Position(c.x(), c.y() + 1));
      out.add(new Box(new Block(x, 0, z), new Block(x + CELL - 1, top, z + CELL - 1)));
      if (east) out.add(new Box(new Block(x + CELL, 0, z), new Block(x + CELL, top, z + CELL - 1)));
      if (south) out.add(new Box(new Block(x, 0, z + CELL), new Block(x + CELL - 1, top, z + CELL)));
      if (east && south && cells.contains(new Position(c.x() + 1, c.y() + 1))) {
        out.add(new Box(new Block(x + CELL, 0, z + CELL), new Block(x + CELL, top, z + CELL)));
      }
    }
    return out;
  }

  /** Clockwise quarter turns from the capture's frame to the world. */
  static int turns(PlacedRoom room, RoomCapture capture) {
    return Math.floorMod(room.rotation() - capture.frameTurns(), 4);
  }

  /** Doorways that were open in the capture, as edges of the placed room. */
  Set<Edge> openEdges(PlacedRoom room, RoomCapture capture) {
    Set<Edge> out = new LinkedHashSet<>();
    for (DoorSlot slot : capture.doors().keySet()) out.add(worldEdge(room, capture, slot));
    return out;
  }

  /** Where a doorway of the capture ends up in the world. */
  Edge worldEdge(PlacedRoom room, RoomCapture capture, DoorSlot slot) {
    int turns = turns(room, capture);
    Position cell = worldCell(room, capture, slot.cell(), turns);
    return Edge.of(cell, slot.side().rotateClockwise(turns));
  }

  /** The world cell a cell of the capture ends up in. */
  Position worldCell(PlacedRoom room, RoomCapture capture, Position local, int turns) {
    int[] p = rotate(PITCH * local.x() + CELL / 2, PITCH * local.y() + CELL / 2, capture.size()[0], capture.size()[2], turns);
    Position min = RoomShape.minCorner(room.cells());
    Position cell = new Position(min.x() + Math.floorDiv(p[0], PITCH), min.y() + Math.floorDiv(p[1], PITCH));
    if (!room.cells().contains(cell)) {
      throw new IllegalStateException(capture.id() + " cell " + local + " turned " + turns + " lands on " + cell
          + ", outside " + room.cells());
    }
    return cell;
  }

  /**
   * Turns a block position inside a {@code sizeX} x {@code sizeZ} area clockwise {@code turns}
   * times, keeping it inside the (turned) area. Same as WorldEdit rotating around the centre.
   */
  static int[] rotate(int x, int z, int sizeX, int sizeZ, int turns) {
    for (int i = 0; i < Math.floorMod(turns, 4); i++) {
      int t = x;
      x = sizeZ - 1 - z;
      z = t;
      int s = sizeX;
      sizeX = sizeZ;
      sizeZ = s;
    }
    return new int[]{x, z};
  }

  /** Block of {@code cell}'s outer wall on {@code side}: {@code along} blocks from the corner, {@code depth} blocks in. */
  private Block wall(Position cell, Direction side, int along, int depth, int y) {
    int minX = cellMinX(cell);
    int minZ = cellMinZ(cell);
    return switch (side) {
      case NORTH -> new Block(minX + along, y, minZ + depth);
      case SOUTH -> new Block(minX + along, y, minZ + CELL - 1 - depth);
      case WEST -> new Block(minX + depth, y, minZ + along);
      case EAST -> new Block(minX + CELL - 1 - depth, y, minZ + along);
    };
  }

  /** Walls up a doorway: the door part of the outer wall becomes copies of the wall on either side. */
  private List<Copy> closing(PlacedRoom room, Edge edge) {
    Position cell = room.cells().contains(edge.a()) ? edge.a() : edge.b();
    Direction side = edge.sideOf(cell);
    List<Copy> out = new ArrayList<>();
    for (int y = DOOR_Y + 1; y < DOOR_Y + DOOR_HEIGHT; y++) {
      for (int along = DOOR_FROM; along <= DOOR_TO; along++) {
        int source = along <= CELL / 2 ? DOOR_FROM - 1 : DOOR_TO + 1;
        out.add(new Copy(wall(cell, side, source, 0, y), wall(cell, side, along, 0, y)));
      }
    }
    return out;
  }

  private Carve carve(PlacedRoom room, Edge edge) {
    Position cell = room.cells().contains(edge.a()) ? edge.a() : edge.b();
    Direction side = edge.sideOf(cell);
    List<List<Block>> layers = new ArrayList<>();
    for (int depth = 1; depth <= CARVE_DEPTH; depth++) {
      List<Block> layer = new ArrayList<>();
      for (int y = OPENING_BOTTOM; y <= OPENING_TOP; y++) {
        for (int along = OPENING_FROM; along <= OPENING_TO; along++) layer.add(wall(cell, side, along, depth, y));
      }
      layers.add(layer);
    }
    return new Carve(layers);
  }

  private void planDoor(Door door, RoomLibrary library, Random random) {
    // No key needed for the fairy room, its door is a normal one.
    DoorType type = door.type() == DoorType.FAIRY ? DoorType.NORMAL : door.type();
    List<Path> variants = library.doors(type);
    if (variants.isEmpty()) {
      problems.add("no " + type.name().toLowerCase() + " door captured, using a normal one at " + door.edge());
      variants = library.doors(DoorType.NORMAL);
      if (variants.isEmpty()) return;
    }
    Position a = door.edge().a();
    boolean sideBySide = door.edge().a().y() == door.edge().b().y();
    // Saved with the rooms north and south of the door; side by side rooms need it turned.
    Block center = sideBySide
        ? new Block(cellMinX(a) + CELL, DOOR_Y, cellMinZ(a) + CELL / 2)
        : new Block(cellMinX(a) + CELL / 2, DOOR_Y, cellMinZ(a) + CELL);
    doors.add(new DoorPaste(door, variants.get(random.nextInt(variants.size())), sideBySide ? 3 : 0, center));
  }
}
