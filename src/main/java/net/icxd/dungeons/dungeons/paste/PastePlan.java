package net.icxd.dungeons.dungeons.paste;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

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
import net.icxd.dungeons.dungeons.paste.RoomLibrary.Doorway;

/**
 * Where every schematic of a generated dungeon goes, worked out without touching a world so it can
 * be tested. {@link WorldEditPaster} carries it out in this order: {@link #clears}, {@link #rooms},
 * {@link #doors}, {@link #fillers}, {@link #closings}.
 *
 * <p>World layout is Hypixel's: cells are 31x31 blocks with a 1 block gap, so cell {@code (x, y)}
 * starts at {@code base + 32 * (x, y)}. With the default base of -200 the cell centres are at
 * -185 + 32i like on Hypixel. Between two different rooms the gap is air, a door fills it.
 *
 * <p>Doorways: every outer wall of a cell has one in the middle, 5 blocks along the wall (13-17),
 * 3 deep (the outer wall and two layers inside) and 7 high (67-73). On Hypixel it holds either the
 * door's frame, the same on both sides and in the gap between, or, without a door, something
 * built for that wall of that room (often a fireplace). The captures show what each doorway held
 * then. A door is copied from a captured open doorway, preferably one of its own two rooms, into
 * both rooms and the gap ({@link #doors}). A doorway that was open in the capture but has no door
 * now gets what another capture of the room shows there ({@link #fillers}), or, if no capture has
 * it walled up, copies of the wall next to it ({@link #closings}).
 */
public final class PastePlan {
  public static final int CELL = 31;
  public static final int PITCH = CELL + 1;
  /** Hypixel's first cell starts here on both axes. */
  public static final int HYPIXEL_BASE = -200;
  /** Lowest block of a doorway (bedrock), and its height. */
  public static final int DOOR_Y = 67;
  public static final int DOOR_HEIGHT = 7;
  /** Along the wall, measured from the cell's corner: a doorway covers 13-17, you walk through 14-16. */
  static final int DOOR_FROM = 13;
  static final int DOOR_TO = 17;
  static final int DOOR_MIDDLE = 15;
  /** Blocks from the outer wall inwards that belong to a doorway. */
  static final int DOORWAY_DEPTH = 3;
  /** Lowest walkable block of a doorway (its floor is at 68). */
  static final int OPENING_BOTTOM = 69;

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

  /**
   * Blocks copied out of a schematic: {@code box} (schematic coordinates) turned {@code turns}
   * times clockwise around {@code from}, which lands on {@code to} in the world.
   */
  public record Piece(Path schematic, Box box, Block from, int turns, Block to) {
  }

  /**
   * @param donor  the captured doorway the door is copied from
   * @param pieces the doorway in each room, and the donor's outer wall layer in the gap
   */
  public record DoorPaste(Door door, Doorway donor, List<Piece> pieces) {
  }

  /** Copy the block at {@code from} to {@code to}. */
  public record Copy(Block from, Block to) {
  }

  private final DungeonLayout layout;
  private final List<Box> clears = new ArrayList<>();
  private final List<RoomPaste> rooms = new ArrayList<>();
  private final List<DoorPaste> doors = new ArrayList<>();
  private final List<Piece> fillers = new ArrayList<>();
  private final List<Copy> closings = new ArrayList<>();
  /** By {@link PlacedRoom#id()}. */
  private final Map<Integer, RoomPaste> pasted = new HashMap<>();
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
   * @param seed picks between captured variants of rooms and doorways
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

  /** Doorways walled up the way another capture of the room shows them. */
  public List<Piece> fillers() {
    return fillers;
  }

  /** Doorways no capture shows walled up: the outer wall next to them, copied across. */
  public List<Copy> closings() {
    return closings;
  }

  /** Rooms or doors that couldn't be planned; they're left out. */
  public List<String> problems() {
    return problems;
  }

  /** The whole map, from the lowest to the highest block any room has. */
  public Box area() {
    return new Box(new Block(baseX, minY, baseZ),
        new Block(baseX + PITCH * layout.getWidth() - 2, maxY, baseZ + PITCH * layout.getHeight() - 2));
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

    // The variant needing the fewest doorways walled up or opened; ties at random.
    RoomCapture best = null;
    int bestScore = Integer.MAX_VALUE;
    int ties = 0;
    for (RoomCapture capture : variants) {
      Set<Edge> open = openEdges(room, capture);
      int score = 0;
      for (Edge e : open) if (!wanted.contains(e)) score++;
      for (Edge e : wanted) if (!open.contains(e)) score++;
      if (score < bestScore) {
        best = capture;
        bestScore = score;
        ties = 1;
      } else if (score == bestScore && random.nextInt(++ties) == 0) {
        best = capture;
      }
    }

    int turns = turns(room, best);
    Position min = RoomShape.minCorner(room.cells());
    Position max = new Position(Integer.MIN_VALUE, Integer.MIN_VALUE);
    for (Position c : room.cells()) max = new Position(Math.max(max.x(), c.x()), Math.max(max.y(), c.y()));
    int sizeX = turns % 2 == 0 ? best.size()[0] : best.size()[2];
    int sizeZ = turns % 2 == 0 ? best.size()[2] : best.size()[0];
    RoomPaste paste = new RoomPaste(room, best, turns,
        new Block(cellMinX(min) + (sizeX - 1) / 2, best.originY(), cellMinZ(min) + (sizeZ - 1) / 2), parts(best));
    rooms.add(paste);
    pasted.put(room.id(), paste);

    if (best.originY() > minY) clears.add(cellBox(min, max, minY, best.originY() - 1));
    if (best.topY() < maxY) clears.add(cellBox(min, max, best.topY() + 1, maxY));

    // Doorways that need a door are done by planDoor.
    for (DoorSlot slot : best.doors().keySet()) {
      if (!wanted.contains(worldEdge(room, best, slot))) close(paste, slot, library, random);
    }
  }

  /** Walls up a doorway that was open in the room's capture. */
  private void close(RoomPaste paste, DoorSlot slot, RoomLibrary library, Random random) {
    RoomCapture capture = paste.capture();
    Position cell = worldCell(paste.room(), capture, slot.cell(), paste.turns());
    Direction side = slot.side().rotateClockwise(paste.turns());
    List<RoomCapture> walledUp = new ArrayList<>();
    for (RoomCapture other : library.captures(capture.templateId())) {
      if (other.id().equals(capture.id()) && other.cells().equals(capture.cells()) && !other.doors().containsKey(slot)
          && RoomLibrary.hasDoorways(other)) {
        walledUp.add(other);
      }
    }
    if (walledUp.isEmpty()) {
      closings.addAll(closing(cell, side));
    } else {
      fillers.add(piece(walledUp.get(random.nextInt(walledUp.size())), slot, DOORWAY_DEPTH, cell, side, 0));
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

  /**
   * Block of the outer wall on {@code side} of the cell starting at {@code minX, minZ}:
   * {@code along} blocks from the corner, {@code depth} blocks in (-1 is the gap outside).
   */
  static Block wall(int minX, int minZ, Direction side, int along, int depth, int y) {
    return switch (side) {
      case NORTH -> new Block(minX + along, y, minZ + depth);
      case SOUTH -> new Block(minX + along, y, minZ + CELL - 1 - depth);
      case WEST -> new Block(minX + depth, y, minZ + along);
      case EAST -> new Block(minX + CELL - 1 - depth, y, minZ + along);
    };
  }

  private Block wall(Position cell, Direction side, int along, int depth, int y) {
    return wall(cellMinX(cell), cellMinZ(cell), side, along, depth, y);
  }

  /**
   * The first {@code depths} layers of a captured doorway, from its outer wall inwards, copied to
   * the doorway on {@code side} of a world cell with the outer wall {@code depth} blocks in.
   */
  Piece piece(RoomCapture source, DoorSlot slot, int depths, Position cell, Direction side, int depth) {
    int minX = PITCH * slot.cell().x();
    int minZ = PITCH * slot.cell().y();
    int y = DOOR_Y - source.originY();
    Block a = wall(minX, minZ, slot.side(), DOOR_FROM, 0, y);
    Block b = wall(minX, minZ, slot.side(), DOOR_TO, depths - 1, y + DOOR_HEIGHT - 1);
    Box box = new Box(new Block(Math.min(a.x(), b.x()), y, Math.min(a.z(), b.z())),
        new Block(Math.max(a.x(), b.x()), b.y(), Math.max(a.z(), b.z())));
    return new Piece(source.schematic(), box, wall(minX, minZ, slot.side(), DOOR_MIDDLE, 0, y),
        Math.floorMod(side.ordinal() - slot.side().ordinal(), 4), wall(cell, side, DOOR_MIDDLE, depth, DOOR_Y));
  }

  /** Walls up a doorway: its outer wall becomes copies of the wall on either side. */
  private List<Copy> closing(Position cell, Direction side) {
    List<Copy> out = new ArrayList<>();
    for (int y = DOOR_Y + 1; y < DOOR_Y + DOOR_HEIGHT; y++) {
      for (int along = DOOR_FROM; along <= DOOR_TO; along++) {
        int source = along <= CELL / 2 ? DOOR_FROM - 1 : DOOR_TO + 1;
        out.add(new Copy(wall(cell, side, source, 0, y), wall(cell, side, along, 0, y)));
      }
    }
    return out;
  }

  /** The door's look: entrance and fairy doors are normal ones. */
  static DoorType look(DoorType type) {
    return type == DoorType.WITHER || type == DoorType.BLOOD ? type : DoorType.NORMAL;
  }

  private void planDoor(Door door, RoomLibrary library, Random random) {
    DoorType type = door.type();
    List<Position> cells = new ArrayList<>();
    List<Doorway> own = new ArrayList<>();
    for (Position cell : List.of(door.edge().a(), door.edge().b())) {
      PlacedRoom room = layout.roomAt(cell);
      RoomPaste paste = room == null ? null : pasted.get(room.id());
      if (paste == null) continue;
      cells.add(cell);
      DoorSlot slot = slotAt(paste, cell, door.edge().sideOf(cell));
      DoorType captured = paste.capture().doors().get(slot);
      if (captured != null && RoomLibrary.hasDoorways(paste.capture())) own.add(new Doorway(paste.capture(), slot, captured));
    }

    // The doorway as one of the two rooms was captured with it, else any captured one.
    Doorway donor = choose(own, t -> t == type, random);
    if (donor == null) donor = choose(own, t -> look(t) == look(type), random);
    if (donor == null) donor = choose(library.doorways(), t -> t == type, random);
    if (donor == null) donor = choose(library.doorways(), t -> look(t) == look(type), random);
    if (donor == null) {
      donor = choose(library.doorways(), t -> look(t) == DoorType.NORMAL, random);
      if (donor == null) {
        problems.add("no doorway captured for the door at " + door.edge());
        return;
      }
      problems.add("no " + type.name().toLowerCase() + " doorway captured, using a normal one at " + door.edge());
    }

    List<Piece> pieces = new ArrayList<>();
    for (Position cell : cells) pieces.add(piece(donor.capture(), donor.slot(), DOORWAY_DEPTH, cell, door.edge().sideOf(cell), 0));
    Position a = door.edge().a();
    pieces.add(piece(donor.capture(), donor.slot(), 1, a, door.edge().sideOf(a), -1));
    doors.add(new DoorPaste(door, donor, pieces));
  }

  private static Doorway choose(List<Doorway> doorways, Predicate<DoorType> fits, Random random) {
    List<Doorway> matching = doorways.stream().filter(d -> fits.test(d.type())).toList();
    return matching.isEmpty() ? null : matching.get(random.nextInt(matching.size()));
  }

  /** The capture's doorway that ends up on {@code side} of world cell {@code cell}. */
  DoorSlot slotAt(RoomPaste paste, Position cell, Direction side) {
    for (Position local : paste.capture().cells()) {
      if (worldCell(paste.room(), paste.capture(), local, paste.turns()).equals(cell)) {
        return new DoorSlot(local, side.rotateClockwise(-paste.turns()));
      }
    }
    throw new IllegalArgumentException(cell + " is not part of room #" + paste.room().id());
  }
}
