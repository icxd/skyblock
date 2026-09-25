package net.icxd.dungeonscanner.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;

import net.icxd.dungeonscanner.FakeWorld;
import net.icxd.dungeonscanner.legacy.LegacyMapper;
import net.icxd.dungeonscanner.scan.DungeonScan;
import net.icxd.dungeonscanner.scan.RoomCore;
import net.icxd.dungeonscanner.scan.RoomDatabase;
import net.icxd.dungeonscanner.scan.RoomRotation;
import net.icxd.dungeonscanner.scan.ScannedDungeon;
import net.icxd.dungeonscanner.scan.ScannedDungeon.Cell;
import net.icxd.dungeonscanner.scan.ScannedDungeon.DoorType;
import net.icxd.dungeonscanner.scan.ScannedDungeon.Room;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A made-up 2x2 corner of a dungeon:
 *
 * <pre>
 *   A (0,0) 1x1, marker SW (EAST)   --wither door--   B (1,0)+(1,1) vertical 1x2, marker NE (WEST)
 *   C (0,1) 1x1, marker NW (SOUTH)  --normal door--   B
 *   A and C: no connection.
 * </pre>
 */
class ScanAndExportTest {
  static final int ROOF = 90;
  static FakeWorld world;
  static final RoomDatabase EMPTY_DB = new RoomDatabase(List.of());

  @BeforeAll
  static void build() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    world = new FakeWorld();
    BlockState stone = Blocks.STONE.defaultBlockState();

    // Floors and roofs; cell centres get a distinct block so the cores differ per room.
    for (Cell c : List.of(new Cell(0, 0), new Cell(1, 0), new Cell(1, 1), new Cell(0, 1))) {
      world.fill(c.centerX() - 15, 68, c.centerZ() - 15, c.centerX() + 15, 68, c.centerZ() + 15, stone);
      world.fill(c.centerX() - 15, ROOF, c.centerZ() - 15, c.centerX() + 15, ROOF, c.centerZ() + 15, stone);
    }
    world.set(new Cell(0, 0).centerX(), 70, new Cell(0, 0).centerZ(), Blocks.GLASS.defaultBlockState());
    world.set(new Cell(0, 1).centerX(), 70, new Cell(0, 1).centerZ(), Blocks.SAND.defaultBlockState());

    // B's two cells are one room: the gap between them is filled up to the roof.
    Cell b0 = new Cell(1, 0);
    world.fill(b0.centerX() - 15, 68, b0.centerZ() + 16, b0.centerX() + 15, ROOF, b0.centerZ() + 16, stone);

    // Wither door A-B (gap column tops out at 73, coal at 69); normal door C-B (air at 69).
    Cell a = new Cell(0, 0);
    world.set(a.centerX() + 16, 68, a.centerZ(), stone);
    world.set(a.centerX() + 16, 69, a.centerZ(), Blocks.COAL_BLOCK.defaultBlockState());
    world.set(a.centerX() + 16, 73, a.centerZ(), stone);
    Cell c = new Cell(0, 1);
    world.set(c.centerX() + 16, 68, c.centerZ(), stone);
    world.set(c.centerX() + 16, 73, c.centerZ(), stone);

    // Roof markers.
    BlockState clay = Blocks.DYED_TERRACOTTA.blue().defaultBlockState();
    world.set(a.centerX() - 15, ROOF, a.centerZ() + 15, clay); // SW -> EAST
    world.set(c.centerX() - 15, ROOF, c.centerZ() - 15, clay); // NW -> SOUTH
    world.set(b0.centerX() + 15, ROOF, b0.centerZ() - 15, clay); // NE of B -> WEST

    // Something asymmetric in A: stairs facing north, one block east of the marker.
    world.set(a.centerX() - 14, 70, a.centerZ() + 15, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
  }

  @Test
  void emptyColumnCoreMatchesOdin() {
    // Odin skips this exact value: it's the hash of a column with no blocks.
    assertEquals(-318865360, RoomCore.at(new FakeWorld(), 0, 0).hash());
  }

  @Test
  void findsRoomsDoorsAndRotations() {
    ScannedDungeon d = DungeonScan.scan(world, EMPTY_DB);
    assertEquals(3, d.rooms().size());
    Room a = roomAt(d, 0, 0);
    Room b = roomAt(d, 1, 0);
    Room c = roomAt(d, 0, 1);
    assertEquals(List.of(new Cell(1, 0), new Cell(1, 1)), b.cells());
    assertEquals("1x2", b.shape());
    assertEquals(RoomRotation.EAST, a.rotation());
    assertEquals(RoomRotation.WEST, b.rotation());
    assertEquals(RoomRotation.SOUTH, c.rotation());
    assertTrue(a.complete() && b.complete() && c.complete());

    assertEquals(2, d.doors().size());
    assertEquals(DoorType.WITHER, doorBetween(d, new Cell(0, 0), new Cell(1, 0)));
    assertEquals(DoorType.NORMAL, doorBetween(d, new Cell(0, 1), new Cell(1, 1)));
    assertEquals(2, d.width());
    assertEquals(2, d.height());
  }

  @Test
  void captureTurnsBlocksIntoTheRoomsFrame() {
    ScannedDungeon d = DungeonScan.scan(world, EMPTY_DB);
    Room a = roomAt(d, 0, 0);
    Capture cap = new Capture(world, -200, 0, -200, -170, 255, -170, a.rotation(), a.clayX(), a.clayZ(), true, false);
    assertEquals(31, cap.width);
    assertEquals(31, cap.length);
    assertEquals(68, cap.originY);
    // Marker ends up in the north-west corner of the roof, the stairs one block south of it, turned east.
    assertEquals(Blocks.DYED_TERRACOTTA.blue(), cap.get(0, ROOF - 68, 0).getBlock());
    BlockState stairs = cap.get(0, 70 - 68, 1);
    assertEquals(Blocks.OAK_STAIRS, stairs.getBlock());
    assertEquals(Direction.EAST, stairs.getValue(StairBlock.FACING));
  }

  @Test
  void doorSlotsAreInTheRoomsFrame() {
    ScannedDungeon d = DungeonScan.scan(world, EMPTY_DB);
    Room b = roomAt(d, 1, 0);
    Capture cap = new Capture(world, -168, 0, -200, -138, 255, -138, b.rotation(), b.clayX(), b.clayZ(), true, false);
    JsonObject json = Exporter.roomJson(b, b.rotation(), cap, d);
    // B stands upright in the world; saved with its marker north-west it lies east-west.
    assertEquals("[[0,0],[1,0]]", json.get("cells").toString());
    assertEquals(63, cap.width);
    assertEquals(31, cap.length);
    String doors = json.get("doors").toString();
    assertTrue(doors.contains("{\"cell\":[0,0],\"side\":\"SOUTH\",\"type\":\"WITHER\"}"), doors);
    assertTrue(doors.contains("{\"cell\":[1,0],\"side\":\"SOUTH\",\"type\":\"NORMAL\"}"), doors);
  }

  @Test
  void exportWritesBothSchematicFormats(@TempDir Path dir) throws Exception {
    Exporter exporter = new Exporter(dir, new LegacyMapper(), "F7");
    Exporter.Result result = exporter.export(world, DungeonScan.scan(world, EMPTY_DB));
    assertEquals(3, result.newRooms().size(), result.toString());

    Path roomDir;
    try (var s = Files.list(dir.resolve("rooms"))) {
      roomDir = s.filter(p -> {
        try (var f = Files.list(p)) {
          return f.anyMatch(x -> x.toString().endsWith(".json") && readString(x).contains("\"capturedRotation\": \"EAST\""));
        } catch (Exception e) {
          return false;
        }
      }).findFirst().orElseThrow();
    }
    Path legacy;
    try (var f = Files.list(roomDir)) {
      legacy = f.filter(p -> p.toString().endsWith(".schematic")).findFirst().orElseThrow();
    }
    CompoundTag tag = NbtIo.readCompressed(legacy, NbtAccounter.unlimitedHeap());
    int w = tag.getShortOr("Width", (short) 0);
    int l = tag.getShortOr("Length", (short) 0);
    byte[] blocks = tag.getByteArray("Blocks").orElseThrow();
    byte[] data = tag.getByteArray("Data").orElseThrow();
    int stairs = ((70 - 68) * l + 1) * w;
    assertEquals(53, blocks[stairs] & 0xFF);
    assertEquals(0, data[stairs]); // 1.8: 0 = facing east
    int clay = ((ROOF - 68) * l) * w;
    assertEquals(159, blocks[clay] & 0xFF);
    assertEquals(11, data[clay]);

    // Scanning the same dungeon again saves nothing new.
    Exporter again = new Exporter(dir, new LegacyMapper(), "F7");
    assertEquals(0, again.export(world, DungeonScan.scan(world, EMPTY_DB)).newRooms().size());
    assertTrue(Files.list(dir.resolve("runs")).findAny().isPresent());
    assertTrue(Files.list(dir.resolve("doors").resolve("wither")).findAny().isPresent());
  }

  @Test
  void markerAboveTheCoreScanIsFound() {
    // A 1x1 whose roof is above y=140, where the core scan stops.
    FakeWorld tall = new FakeWorld();
    Cell cell = new Cell(0, 0);
    tall.fill(cell.centerX() - 15, 68, cell.centerZ() - 15, cell.centerX() + 15, 68, cell.centerZ() + 15, Blocks.STONE.defaultBlockState());
    tall.fill(cell.centerX() - 15, 150, cell.centerZ() - 15, cell.centerX() + 15, 150, cell.centerZ() + 15, Blocks.STONE.defaultBlockState());
    tall.set(cell.centerX() + 15, 150, cell.centerZ() + 15, Blocks.DYED_TERRACOTTA.blue().defaultBlockState()); // SE -> NORTH
    Room room = DungeonScan.scan(tall, EMPTY_DB).rooms().get(0);
    assertEquals(RoomRotation.NORTH, room.rotation());
    assertTrue(room.markerFound());
  }

  @Test
  void straightRoomsFollowTheShapeConvention() {
    // Horizontal 1x2 with blue terracotta on every roof corner: still SOUTH (marker north-west).
    FakeWorld w = new FakeWorld();
    Cell a = new Cell(2, 2);
    Cell b = new Cell(3, 2);
    BlockState stone = Blocks.STONE.defaultBlockState();
    w.fill(a.centerX() - 15, 68, a.centerZ() - 15, b.centerX() + 15, 68, b.centerZ() + 15, stone);
    w.fill(a.centerX() - 15, ROOF, a.centerZ() - 15, b.centerX() + 15, ROOF, b.centerZ() + 15, stone);
    for (int x : new int[]{a.centerX() - 15, b.centerX() + 15}) {
      for (int z : new int[]{a.centerZ() - 15, a.centerZ() + 15}) w.set(x, ROOF, z, Blocks.DYED_TERRACOTTA.blue().defaultBlockState());
    }
    Room room = DungeonScan.scan(w, EMPTY_DB).rooms().get(0);
    assertEquals(2, room.cells().size());
    assertEquals(RoomRotation.SOUTH, room.rotation());
    assertEquals(a.centerX() - 15, room.clayX());
    assertEquals(a.centerZ() - 15, room.clayZ());
  }

  @Test
  void openedWitherDoorStaysAWitherDoor(@TempDir Path dir) throws Exception {
    FakeWorld w = new FakeWorld();
    Cell a = new Cell(0, 0);
    Cell b = new Cell(1, 0);
    BlockState stone = Blocks.STONE.defaultBlockState();
    for (Cell c : List.of(a, b)) {
      w.fill(c.centerX() - 15, 68, c.centerZ() - 15, c.centerX() + 15, 68, c.centerZ() + 15, stone);
      w.fill(c.centerX() - 15, ROOF, c.centerZ() - 15, c.centerX() + 15, ROOF, c.centerZ() + 15, stone);
    }
    w.set(a.centerX(), 70, a.centerZ(), Blocks.GLASS.defaultBlockState());
    w.set(a.centerX() + 16, 68, a.centerZ(), stone);
    w.set(a.centerX() + 16, 73, a.centerZ(), stone);
    w.set(a.centerX() + 16, 69, a.centerZ(), Blocks.COAL_BLOCK.defaultBlockState());
    Exporter exporter = new Exporter(dir, new LegacyMapper(), "F1");
    exporter.export(w, DungeonScan.scan(w, EMPTY_DB));
    w.set(a.centerX() + 16, 69, a.centerZ(), Blocks.AIR.defaultBlockState()); // someone opened it
    exporter.export(w, DungeonScan.scan(w, EMPTY_DB));
    Path run;
    try (var s = Files.list(dir.resolve("runs"))) {
      run = s.findFirst().orElseThrow();
    }
    assertTrue(Files.readString(run).contains("\"type\": \"WITHER\""), Files.readString(run));
  }

  @Test
  void reconvertRebuildsTheSameLegacySchematic(@TempDir Path dir) throws Exception {
    LegacyMapper mapper = new LegacyMapper();
    new Exporter(dir, mapper, "F7").export(world, DungeonScan.scan(world, EMPTY_DB));
    List<Path> legacy;
    try (var s = Files.walk(dir.resolve("rooms"))) {
      legacy = s.filter(p -> p.toString().endsWith(".schematic")).toList();
    }
    java.util.Map<Path, byte[]> before = new java.util.HashMap<>();
    for (Path p : legacy) before.put(p, Files.readAllBytes(p));
    for (Path p : legacy) Files.delete(p);
    assertTrue(Reconverter.reconvertAll(dir, mapper).rebuilt() >= legacy.size());
    for (Path p : legacy) {
      CompoundTag a = NbtIo.readCompressed(new java.io.ByteArrayInputStream(before.get(p)), NbtAccounter.unlimitedHeap());
      CompoundTag b = NbtIo.readCompressed(p, NbtAccounter.unlimitedHeap());
      assertEquals(a, b, p.toString());
    }
  }

  /** An L room (north-east cell missing) next to a 1x1 that sits in that missing cell. */
  private static FakeWorld lRoomWorld() {
    FakeWorld w = new FakeWorld();
    BlockState stone = Blocks.STONE.defaultBlockState();
    Cell a = new Cell(0, 0);
    Cell b = new Cell(0, 1);
    Cell c = new Cell(1, 1);
    Cell other = new Cell(1, 0);
    for (Cell cell : List.of(a, b, c, other)) {
      w.fill(cell.centerX() - 15, 68, cell.centerZ() - 15, cell.centerX() + 15, 68, cell.centerZ() + 15, stone);
      w.fill(cell.centerX() - 15, ROOF, cell.centerZ() - 15, cell.centerX() + 15, ROOF, cell.centerZ() + 15, stone);
    }
    // Gaps inside the L are filled to the roof.
    w.fill(a.centerX() - 15, 68, a.centerZ() + 16, a.centerX() + 15, ROOF, a.centerZ() + 16, stone);
    w.fill(b.centerX() + 16, 68, b.centerZ() - 15, b.centerX() + 16, ROOF, b.centerZ() + 15, stone);
    w.set(a.centerX(), 70, a.centerZ(), Blocks.GLASS.defaultBlockState());
    w.set(a.centerX() - 15, ROOF, a.centerZ() - 15, Blocks.DYED_TERRACOTTA.blue().defaultBlockState()); // NW -> SOUTH
    // The neighbour: something tall so it would also stretch the L's height if it were saved.
    w.fill(other.centerX(), 69, other.centerZ(), other.centerX(), ROOF + 20, other.centerZ(), Blocks.GLOWSTONE.defaultBlockState());
    return w;
  }

  private static Path onlyFile(Path dir, String id, String ext) throws Exception {
    try (var s = Files.list(dir.resolve("rooms").resolve(id))) {
      List<Path> files = s.filter(p -> p.toString().endsWith(ext)).toList();
      assertEquals(1, files.size(), files.toString());
      return files.get(0);
    }
  }

  private static void assertOnlyOwnBlocks(Path schem) throws Exception {
    CompoundTag file = NbtIo.readCompressed(schem, NbtAccounter.unlimitedHeap()).getCompoundOrEmpty("Schematic");
    assertEquals(63, file.getShortOr("Width", (short) 0));
    assertEquals(ROOF - 68 + 1, file.getShortOr("Height", (short) 0));
    var view = Reconverter.view(file);
    for (int y = 0; y < file.getShortOr("Height", (short) 0); y++) {
      for (int x = 31; x < 63; x++) {
        for (int z = 0; z < 32; z++) assertTrue(view.getBlockState(x, y, z).isAir(), x + "," + y + "," + z);
      }
    }
    assertEquals(Blocks.STONE, view.getBlockState(31, 10, 40).getBlock()); // gap between two of its own cells
  }

  @Test
  void lRoomCaptureLeavesOutItsMissingCorner(@TempDir Path dir) throws Exception {
    FakeWorld w = lRoomWorld();
    ScannedDungeon scanned = DungeonScan.scan(w, EMPTY_DB);
    Room l = roomAt(scanned, 0, 0);
    assertEquals(3, l.cells().size());
    new Exporter(dir, new LegacyMapper(), "F1").export(w, scanned);
    assertOnlyOwnBlocks(onlyFile(dir, l.id(), ".schem"));
  }

  @Test
  void reconvertCutsOldLRoomCaptures(@TempDir Path dir) throws Exception {
    FakeWorld w = lRoomWorld();
    ScannedDungeon scanned = DungeonScan.scan(w, EMPTY_DB);
    Room l = roomAt(scanned, 0, 0);
    new Exporter(dir, new LegacyMapper(), "F1").export(w, scanned);
    Path good = onlyFile(dir, l.id(), ".schem");
    String goodName = good.getFileName().toString();
    JsonObject goodJson = com.google.gson.JsonParser.parseString(Files.readString(onlyFile(dir, l.id(), ".json"))).getAsJsonObject();

    // What an older scanner saved: the whole bounding box, neighbour included, under its own hash.
    for (Path p : List.of(good, onlyFile(dir, l.id(), ".schematic"), onlyFile(dir, l.id(), ".json"))) Files.delete(p);
    int[] box = {new Cell(0, 0).centerX() - 15, new Cell(0, 0).centerZ() - 15, new Cell(1, 1).centerX() + 15, new Cell(1, 1).centerZ() + 15};
    Capture old = new Capture(w, box[0], w.minY(), box[1], box[2], w.maxY() - 1, box[3], RoomRotation.SOUTH, box[0], box[1], true, true);
    Path base = dir.resolve("rooms").resolve(l.id()).resolve(l.id() + "_" + SchematicWriter.hash(old).substring(0, 10));
    SchematicWriter.writeSponge(old, base.resolveSibling(base.getFileName() + ".schem"), new CompoundTag());
    JsonObject oldJson = goodJson.deepCopy();
    oldJson.addProperty("originY", old.originY);
    com.google.gson.JsonArray size = new com.google.gson.JsonArray();
    size.add(old.width);
    size.add(old.height);
    size.add(old.length);
    oldJson.add("size", size);
    Files.writeString(base.resolveSibling(base.getFileName() + ".json"), oldJson.toString());
    // A second old capture of the same room that only differs in the neighbour.
    w.set(new Cell(1, 0).centerX() + 3, 70, new Cell(1, 0).centerZ(), Blocks.GOLD_BLOCK.defaultBlockState());
    Capture old2 = new Capture(w, box[0], w.minY(), box[1], box[2], w.maxY() - 1, box[3], RoomRotation.SOUTH, box[0], box[1], true, true);
    Path base2 = base.resolveSibling(l.id() + "_" + SchematicWriter.hash(old2).substring(0, 10));
    SchematicWriter.writeSponge(old2, base2.resolveSibling(base2.getFileName() + ".schem"), new CompoundTag());
    Files.writeString(base2.resolveSibling(base2.getFileName() + ".json"), oldJson.toString());

    Reconverter.Result result = Reconverter.reconvertAll(dir, new LegacyMapper());
    assertEquals(2, result.trimmed());
    assertEquals(1, result.merged());
    Path now = onlyFile(dir, l.id(), ".schem");
    assertEquals(goodName, now.getFileName().toString());
    assertOnlyOwnBlocks(now);
    JsonObject json = com.google.gson.JsonParser.parseString(Files.readString(onlyFile(dir, l.id(), ".json"))).getAsJsonObject();
    assertEquals(goodJson.get("originY"), json.get("originY"));
    assertEquals(goodJson.get("size"), json.get("size"));
    onlyFile(dir, l.id(), ".schematic");
  }

  private static String readString(Path p) {
    try {
      return Files.readString(p);
    } catch (Exception e) {
      return "";
    }
  }

  private static Room roomAt(ScannedDungeon d, int x, int z) {
    return d.rooms().stream().filter(r -> r.cells().contains(new Cell(x, z))).findFirst().orElseThrow();
  }

  private static DoorType doorBetween(ScannedDungeon d, Cell a, Cell b) {
    return d.doors().stream()
        .filter(door -> (door.a().equals(a) && door.b().equals(b)) || (door.a().equals(b) && door.b().equals(a)))
        .map(ScannedDungeon.Door::type).findFirst().orElse(null);
  }
}
