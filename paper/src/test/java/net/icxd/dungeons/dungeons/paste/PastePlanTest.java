package net.icxd.dungeons.dungeons.paste;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.DungeonConfig;
import net.icxd.dungeons.dungeons.generation.DungeonGenerator;
import net.icxd.dungeons.dungeons.generation.DungeonLayout;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.LayoutValidator;
import net.icxd.dungeons.dungeons.generation.room.DoorSlot;
import net.icxd.dungeons.dungeons.generation.room.HypixelRooms;
import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.dungeons.paste.PastePlan.Block;
import net.icxd.dungeons.dungeons.paste.PastePlan.Box;
import net.icxd.dungeons.dungeons.paste.PastePlan.DoorPaste;
import net.icxd.dungeons.dungeons.paste.PastePlan.Piece;
import net.icxd.dungeons.dungeons.paste.PastePlan.RoomPaste;
import net.icxd.dungeons.dungeons.paste.RoomLibrary.Doorway;

/**
 * Uses a made-up capture folder with a capture per Hypixel room, two for rooms bigger than 1x1
 * (empty schematic files; the plan never reads them), laid out like the scanner's output.
 */
class PastePlanTest {
  @TempDir
  static Path root;
  static RoomLibrary library;

  @BeforeAll
  static void writeFixtures() throws IOException {
    for (Room r : HypixelRooms.ALL) {
      if (r.getId().equals("blaze")) continue; // captured as lower/higher blaze below
      writeRoom(r.getId(), r, "aaaa", Direction.NORTH, Direction.EAST);
      if (r.getShape() != RoomShape.ONE_BY_ONE) writeRoom(r.getId(), r, "bbbb", Direction.SOUTH, Direction.WEST);
    }
    Room blaze = HypixelRooms.ALL.stream().filter(r -> r.getId().equals("blaze")).findFirst().orElseThrow();
    writeRoom("lower_blaze", blaze, "bbbb", Direction.NORTH, Direction.EAST);
    writeRoom("higher_blaze", blaze, "cccc", Direction.NORTH, Direction.EAST);
    library = RoomLibrary.load(root);
  }

  /** L rooms are captured with the north-east cell missing (canonical frame = template turned 3 times). */
  private static final List<Position> L_CAPTURED = List.of(new Position(0, 0), new Position(0, 1), new Position(1, 1));

  /** @param open for rooms without fixed doorways: the outer walls of the first and last cell with one */
  private static void writeRoom(String id, Room r, String hash, Direction... open) throws IOException {
    List<Position> cells = r.getShape() == RoomShape.L_SHAPE ? L_CAPTURED : r.getShape().cells();
    Map<DoorSlot, String> doors = new HashMap<>();
    if (r.isExactDoors()) {
      String type = switch (r.getType()) {
        case BLOOD -> "BLOOD";
        case START -> "ENTRANCE";
        default -> "NORMAL";
      };
      for (DoorSlot s : r.getDoorSlots()) doors.put(s, type);
    } else {
      // Some, so there is something to wall up and to open.
      Set<Position> own = new HashSet<>(cells);
      for (Position c : List.of(cells.get(0), cells.get(cells.size() - 1))) {
        for (Direction d : open) {
          if (!own.contains(c.offset(d))) doors.put(new DoorSlot(c, d), d == Direction.NORTH ? "WITHER" : "NORMAL");
        }
      }
    }
    int maxX = 0;
    int maxZ = 0;
    for (Position c : cells) {
      maxX = Math.max(maxX, c.x());
      maxZ = Math.max(maxZ, c.y());
    }
    StringBuilder json = new StringBuilder("{\"id\":\"" + id + "\",\"type\":\"" + type(r.getType()) + "\",\"shape\":\""
        + shape(r.getShape()) + "\",\"originY\":" + (r.getType() == RoomType.PUZZLE ? 30 : 66) + ",\"size\":["
        + (32 * (maxX + 1) - 1) + ",40," + (32 * (maxZ + 1) - 1) + "],\"cells\":[");
    for (int i = 0; i < cells.size(); i++) json.append(i > 0 ? "," : "").append(pair(cells.get(i)));
    json.append("],\"doors\":[");
    int i = 0;
    for (Map.Entry<DoorSlot, String> d : doors.entrySet()) {
      json.append(i++ > 0 ? "," : "").append("{\"cell\":").append(pair(d.getKey().cell())).append(",\"side\":\"")
          .append(d.getKey().side()).append("\",\"type\":\"").append(d.getValue()).append("\"}");
    }
    json.append("]}");
    Path dir = Files.createDirectories(root.resolve("rooms").resolve(id));
    Files.writeString(dir.resolve(id + "_" + hash + ".json"), json);
    Files.createFile(dir.resolve(id + "_" + hash + ".schem"));
  }

  private static String pair(Position p) {
    return "[" + p.x() + "," + p.y() + "]";
  }

  private static String type(RoomType type) {
    return switch (type) {
      case REGULAR -> "NORMAL";
      case MINIBOSS -> "CHAMPION";
      case START -> "ENTRANCE";
      default -> type.name();
    };
  }

  private static String shape(RoomShape shape) {
    return switch (shape) {
      case ONE_BY_ONE -> "1x1";
      case ONE_BY_TWO -> "1x2";
      case ONE_BY_THREE -> "1x3";
      case ONE_BY_FOUR -> "1x4";
      case TWO_BY_TWO -> "2x2";
      case L_SHAPE -> "L";
    };
  }

  @Test
  void everyCaptureBecomesATemplate() {
    assertTrue(library.problems().isEmpty(), library.problems().toString());
    assertEquals(HypixelRooms.ALL.size(), library.templates().size());
    for (Room hypixel : HypixelRooms.ALL) {
      Room captured = library.templates().stream().filter(r -> r.getId().equals(hypixel.getId())).findFirst().orElseThrow();
      assertEquals(hypixel.getType(), captured.getType(), hypixel.getId());
      assertEquals(hypixel.getShape(), captured.getShape(), hypixel.getId());
      assertEquals(hypixel.getMinimumFloor(), captured.getMinimumFloor(), hypixel.getId());
      assertEquals(hypixel.getRotations(), captured.getRotations(), hypixel.getId());
      assertEquals(hypixel.isExactDoors(), captured.isExactDoors(), hypixel.getId());
      if (hypixel.isExactDoors()) assertEquals(hypixel.getDoorSlots(), captured.getDoorSlots(), hypixel.getId());
    }
  }

  @Test
  void lowerAndHigherBlazeAreVariantsOfBlaze() {
    List<RoomCapture> blaze = library.captures("blaze");
    assertEquals(2, blaze.size());
    assertEquals(Set.of("lower_blaze", "higher_blaze"), Set.of(blaze.get(0).id(), blaze.get(1).id()));
  }

  @Test
  void lRoomsKnowTheirFrame() {
    for (RoomCapture c : library.captures("spider")) assertEquals(3, c.frameTurns());
    for (RoomCapture c : library.captures("mines")) assertEquals(0, c.frameTurns());
  }

  @Test
  void capturesWithOtherDoorwaysAreDropped(@TempDir Path other) throws IOException {
    Room hall = HypixelRooms.ALL.stream().filter(r -> r.getId().equals("hall")).findFirst().orElseThrow();
    Path dir = Files.createDirectories(other.resolve("rooms/hall"));
    for (String hash : new String[]{"aaaa", "bbbb"}) {
      Files.writeString(dir.resolve("hall_" + hash + ".json"), "{\"id\":\"hall\",\"type\":\"NORMAL\",\"shape\":\"1x1\","
          + "\"originY\":66,\"size\":[31,40,31],\"cells\":[[0,0]],\"doors\":[{\"cell\":[0,0],\"side\":\"EAST\",\"type\":\"NORMAL\"},"
          + "{\"cell\":[0,0],\"side\":\"WEST\",\"type\":\"NORMAL\"}]}");
      Files.createFile(dir.resolve("hall_" + hash + ".schem"));
    }
    Files.writeString(dir.resolve("hall_cccc.json"), "{\"id\":\"hall\",\"type\":\"NORMAL\",\"shape\":\"1x1\","
        + "\"originY\":66,\"size\":[31,40,31],\"cells\":[[0,0]],\"doors\":[{\"cell\":[0,0],\"side\":\"EAST\",\"type\":\"NORMAL\"}]}");
    Files.createFile(dir.resolve("hall_cccc.schem"));
    RoomLibrary lib = RoomLibrary.load(other);
    assertEquals(2, lib.captures("hall").size());
    assertEquals(1, lib.problems().stream().filter(p -> p.contains("hall_cccc")).count(), lib.problems().toString());
    assertEquals(hall.getDoorSlots(), lib.templates().get(0).getDoorSlots());
  }

  @Test
  void rotateTurnsClockwiseInsideTheArea() {
    // A 63x31 area (1x2 room) turned once is 31x63: its north-west corner goes to the north-east.
    assertArrayEquals(new int[]{30, 0}, PastePlan.rotate(0, 0, 63, 31, 1));
    assertArrayEquals(new int[]{30, 62}, PastePlan.rotate(62, 0, 63, 31, 1));
    assertArrayEquals(new int[]{62, 30}, PastePlan.rotate(0, 0, 63, 31, 2));
    assertArrayEquals(new int[]{5, 7}, PastePlan.rotate(5, 7, 63, 31, 4));
    // Same as WorldEdit turning around the centre block: (dx, dz) -> (-dz, dx).
    int[] p = PastePlan.rotate(40, 3, 63, 31, 1);
    assertArrayEquals(new int[]{15 - (3 - 15), 31 + (40 - 31)}, p);
  }

  @Test
  void partsLeaveOutTheMissingCornerOfAnL() {
    RoomCapture spider = library.captures("spider").get(0);
    List<Box> parts = PastePlan.parts(spider);
    int blocks = 0;
    for (Box b : parts) {
      blocks += (b.max().x() - b.min().x() + 1) * (b.max().z() - b.min().z() + 1);
      // Nothing of the north-east cell, the gaps around it or the corner between them.
      assertFalse(b.max().x() >= 31 && b.min().z() <= 31 && b.max().z() <= 31, b.toString());
    }
    // Three cells and the two gaps between them.
    assertEquals(3 * 31 * 31 + 2 * 31, blocks);

    List<Box> mines = PastePlan.parts(library.captures("mines").get(0));
    int all = 0;
    for (Box b : mines) all += (b.max().x() - b.min().x() + 1) * (b.max().z() - b.min().z() + 1);
    assertEquals(63 * 63, all);
  }

  @ParameterizedTest
  @EnumSource(value = DungeonFloor.class, names = {"ENTRANCE", "FLOOR_3", "FLOOR_6", "FLOOR_7"})
  void planPlacesEveryRoomAndDoor(DungeonFloor floor) {
    for (long seed = 0; seed < 40; seed++) {
      DungeonLayout layout = new DungeonGenerator(DungeonConfig.forFloor(floor), library.pool()).generate(seed);
      assertTrue(LayoutValidator.validate(layout).isEmpty(), floor + " seed " + seed);
      PastePlan plan = PastePlan.create(layout, library, PastePlan.HYPIXEL_BASE, PastePlan.HYPIXEL_BASE, seed);
      String where = floor + " seed " + seed + "\n" + layout.render();
      assertTrue(plan.problems().isEmpty(), plan.problems() + " " + where);
      assertEquals(layout.getRooms().size(), plan.rooms().size(), where);
      assertEquals(layout.getDoors().size(), plan.doors().size(), where);

      Set<Block> filled = new HashSet<>();
      int walledUp = 0;
      for (RoomPaste paste : plan.rooms()) {
        PlacedRoom room = paste.room();
        assertEquals(room.template().getId(), paste.capture().templateId(), where);
        Set<Position> footprint = new HashSet<>();
        for (Position c : paste.capture().cells()) footprint.add(plan.worldCell(room, paste.capture(), c, paste.turns()));
        assertEquals(new HashSet<>(room.cells()), footprint, room.template().getId() + " " + where);
        assertEquals(paste.capture().originY(), paste.center().y());

        // The turned schematic fits the footprint's bounding box exactly.
        Position min = RoomShape.minCorner(room.cells());
        int sizeX = paste.turns() % 2 == 0 ? paste.capture().size()[0] : paste.capture().size()[2];
        assertEquals(-200 + 32 * min.x() + (sizeX - 1) / 2, paste.center().x(), where);

        Set<Edge> open = plan.openEdges(room, paste.capture());
        Set<Edge> wanted = new HashSet<>();
        for (Door d : room.doors()) wanted.add(d.edge());
        if (paste.capture().shape() == RoomShape.ONE_BY_ONE && paste.capture().type() != RoomType.FAIRY) {
          assertEquals(wanted, open, room.template().getId() + " " + where);
        }
        // Doorways open in the capture without a door now: walled up the way another capture shows them.
        for (DoorSlot slot : paste.capture().doors().keySet()) {
          Edge e = plan.worldEdge(room, paste.capture(), slot);
          if (wanted.contains(e)) continue;
          Position cell = room.cells().contains(e.a()) ? e.a() : e.b();
          boolean shownWalledUp = library.captures(paste.capture().templateId()).stream()
              .anyMatch(c -> c.id().equals(paste.capture().id()) && !c.doors().containsKey(slot) && RoomLibrary.hasDoorways(c));
          if (shownWalledUp) {
            filled.addAll(doorway(-200 + 32 * cell.x(), -200 + 32 * cell.y(), 67, e.sideOf(cell), 0, 2));
          } else {
            walledUp++;
          }
        }
      }
      Set<Block> fillerBlocks = new HashSet<>();
      for (Piece piece : plan.fillers()) fillerBlocks.addAll(landing(piece));
      assertEquals(filled, fillerBlocks, where);
      // 5 blocks along the wall x 6 high per doorway walled up with the wall next to it.
      assertEquals(walledUp * 30, plan.closings().size(), where);

      for (DoorPaste door : plan.doors()) checkDoor(plan, door, where);
    }
  }

  /** The door fills both rooms' doorways and the gap between them, from a doorway of the right kind. */
  private static void checkDoor(PastePlan plan, DoorPaste door, String where) {
    Edge e = door.door().edge();
    Doorway donor = door.donor();
    assertEquals(PastePlan.look(door.door().type()), PastePlan.look(donor.type()), where);
    assertTrue(donor.capture().doors().containsKey(donor.slot()), where);

    // One of the two rooms' own doorway if either was captured with this kind of door there.
    List<Doorway> own = new ArrayList<>();
    for (Position cell : List.of(e.a(), e.b())) {
      RoomPaste paste = plan.rooms().stream().filter(r -> r.room().cells().contains(cell)).findFirst().orElseThrow();
      DoorSlot slot = plan.slotAt(paste, cell, e.sideOf(cell));
      assertEquals(e, plan.worldEdge(paste.room(), paste.capture(), slot), where);
      DoorType captured = paste.capture().doors().get(slot);
      if (captured == door.door().type() && RoomLibrary.hasDoorways(paste.capture())) own.add(new Doorway(paste.capture(), slot, captured));
    }
    if (!own.isEmpty()) assertTrue(own.contains(donor), donor + " instead of " + own + " " + where);

    int y = 67 - donor.capture().originY();
    Position s = donor.slot().cell();
    Set<Block> source = doorway(32 * s.x(), 32 * s.y(), y, donor.slot().side(), 0, 2);
    Set<Block> outerWall = doorway(32 * s.x(), 32 * s.y(), y, donor.slot().side(), 0, 0);
    assertEquals(3, door.pieces().size(), where);
    List<Position> cells = List.of(e.a(), e.b());
    for (int i = 0; i < 2; i++) {
      Piece piece = door.pieces().get(i);
      Position cell = cells.get(i);
      assertEquals(source, blocks(piece.box()), where);
      assertEquals(doorway(-200 + 32 * cell.x(), -200 + 32 * cell.y(), 67, e.sideOf(cell), 0, 2), landing(piece), where);
      // Outer wall to outer wall, not turned inside out.
      Set<Block> wall = new HashSet<>();
      for (Block b : outerWall) wall.add(land(piece, b));
      assertEquals(doorway(-200 + 32 * cell.x(), -200 + 32 * cell.y(), 67, e.sideOf(cell), 0, 0), wall, where);
    }
    Piece gap = door.pieces().get(2);
    assertEquals(outerWall, blocks(gap.box()), where);
    assertEquals(doorway(-200 + 32 * e.a().x(), -200 + 32 * e.a().y(), 67, e.sideOf(e.a()), -1, -1), landing(gap), where);
  }

  /**
   * Blocks of the doorway on {@code side} of the cell starting at {@code x0, z0}: 13-17 along the
   * wall, {@code fromDepth..toDepth} in from the outer wall (-1 is the gap), 7 high from {@code y0}.
   */
  private static Set<Block> doorway(int x0, int z0, int y0, Direction side, int fromDepth, int toDepth) {
    Set<Block> out = new HashSet<>();
    for (int y = y0; y < y0 + 7; y++) {
      for (int along = 13; along <= 17; along++) {
        for (int d = fromDepth; d <= toDepth; d++) {
          out.add(switch (side) {
            case NORTH -> new Block(x0 + along, y, z0 + d);
            case SOUTH -> new Block(x0 + along, y, z0 + 30 - d);
            case WEST -> new Block(x0 + d, y, z0 + along);
            case EAST -> new Block(x0 + 30 - d, y, z0 + along);
          });
        }
      }
    }
    return out;
  }

  private static Set<Block> blocks(Box box) {
    Set<Block> out = new HashSet<>();
    for (int x = box.min().x(); x <= box.max().x(); x++) {
      for (int y = box.min().y(); y <= box.max().y(); y++) {
        for (int z = box.min().z(); z <= box.max().z(); z++) out.add(new Block(x, y, z));
      }
    }
    return out;
  }

  /** Where the blocks of a piece end up in the world. */
  private static Set<Block> landing(Piece piece) {
    Set<Block> out = new HashSet<>();
    for (Block b : blocks(piece.box())) out.add(land(piece, b));
    return out;
  }

  /** Turned clockwise around {@code from} like WorldEdit does: (dx, dz) -> (-dz, dx). */
  private static Block land(Piece piece, Block b) {
    int dx = b.x() - piece.from().x();
    int dz = b.z() - piece.from().z();
    for (int i = 0; i < piece.turns(); i++) {
      int t = dx;
      dx = -dz;
      dz = t;
    }
    return new Block(piece.to().x() + dx, piece.to().y() + b.y() - piece.from().y(), piece.to().z() + dz);
  }

  @Test
  void clearsCoverGapsAndEmptyCells() {
    DungeonLayout layout = new DungeonGenerator(DungeonConfig.forFloor(DungeonFloor.FLOOR_6), library.pool()).generate(3);
    PastePlan plan = PastePlan.create(layout, library, PastePlan.HYPIXEL_BASE, PastePlan.HYPIXEL_BASE, 3);
    List<Box> clears = new ArrayList<>(plan.clears());
    // Every gap plane between cells, full height.
    for (int i = 0; i + 1 < layout.getWidth(); i++) {
      int x = -200 + 32 * i + 31;
      assertTrue(clears.stream().anyMatch(b -> b.min().x() == x && b.max().x() == x && b.min().y() == library.minY()
          && b.max().y() == library.maxY()), "gap x=" + x);
    }
    for (int x = 0; x < layout.getWidth(); x++) {
      for (int y = 0; y < layout.getHeight(); y++) {
        if (layout.roomAt(new Position(x, y)) != null) continue;
        int bx = -200 + 32 * x;
        int bz = -200 + 32 * y;
        assertTrue(clears.stream().anyMatch(b -> b.min().x() == bx && b.min().z() == bz && b.max().x() == bx + 30
            && b.max().z() == bz + 30), "empty cell " + x + "," + y);
      }
    }
    // Puzzles in the fixtures sit lower than the rest, so their columns get cleared above them.
    for (RoomPaste r : plan.rooms()) {
      if (r.capture().topY() >= library.maxY()) continue;
      Position min = RoomShape.minCorner(r.room().cells());
      int bx = -200 + 32 * min.x();
      assertTrue(clears.stream().anyMatch(b -> b.min().x() == bx && b.min().y() == r.capture().topY() + 1), r.capture().id());
    }
  }

  @Test
  void entranceIsTheStartRoomsCentre() {
    DungeonLayout layout = new DungeonGenerator(DungeonConfig.forFloor(DungeonFloor.ENTRANCE), library.pool()).generate(8);
    PastePlan plan = PastePlan.create(layout, library, PastePlan.HYPIXEL_BASE, PastePlan.HYPIXEL_BASE, 8);
    PlacedRoom start = layout.roomsOfType(RoomType.START).get(0);
    Position c = start.cells().get(0);
    assertEquals(new Block(-185 + 32 * c.x(), 69, -185 + 32 * c.y()), plan.entrance());
    // Its one door leads to the room on that side.
    Position next = c.offset(plan.entranceDoor());
    assertTrue(layout.getDoors().stream().anyMatch(d -> (d.edge().a().equals(c) || d.edge().b().equals(c))
        && d.edge().other(c).equals(next)));
  }
}
