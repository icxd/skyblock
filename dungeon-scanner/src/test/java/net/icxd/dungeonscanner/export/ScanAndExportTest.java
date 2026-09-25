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
