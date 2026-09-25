package net.icxd.dungeonscanner.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.icxd.dungeonscanner.legacy.LegacyMapper;
import net.icxd.dungeonscanner.scan.DungeonScan;
import net.icxd.dungeonscanner.scan.RoomRotation;
import net.icxd.dungeonscanner.scan.ScannedDungeon;
import net.icxd.dungeonscanner.scan.ScannedDungeon.Cell;
import net.icxd.dungeonscanner.scan.ScannedDungeon.Door;
import net.icxd.dungeonscanner.scan.ScannedDungeon.Room;
import net.icxd.dungeonscanner.scan.WorldView;
import net.minecraft.nbt.CompoundTag;

/**
 * Saves rooms, doors and run layouts under {@code <game dir>/dungeon-scanner}:
 *
 * <pre>
 * rooms/&lt;room&gt;/&lt;room&gt;_&lt;hash&gt;.schem       Sponge v3, modern block states
 * rooms/&lt;room&gt;/&lt;room&gt;_&lt;hash&gt;.schematic  MCEdit, 1.8 ids (for the 1.8 server)
 * rooms/&lt;room&gt;/&lt;room&gt;_&lt;hash&gt;.json       door slots etc., see {@link #roomJson}
 * doors/&lt;type&gt;/&lt;type&gt;_&lt;hash&gt;.*           the door structures
 * runs/&lt;time&gt;_&lt;floor&gt;.json                  the whole layout of one run
 * </pre>
 *
 * Everything is saved in the room's own frame (roof marker in the north-west corner), so the same
 * room captured in different runs and rotations comes out identical and is only kept once.
 */
public final class Exporter {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final String[] SIDES = {"NORTH", "EAST", "SOUTH", "WEST"};

  private final Path root;
  private final LegacyMapper mapper;
  private final String floor;
  private final String runName;
  /** Rooms (by id + first cell) already captured in this run. */
  private final Set<String> captured = new HashSet<>();
  private final Set<String> capturedDoors = new HashSet<>();

  public Exporter(Path root, LegacyMapper mapper, String floor) {
    this.root = root;
    this.mapper = mapper;
    this.floor = floor;
    this.runName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "_" + floor;
  }

  public record Result(List<String> newRooms, List<String> knownRooms, List<String> problems) {
  }

  /** Captures every complete room (and its doors) not captured yet in this run. */
  public Result export(WorldView world, ScannedDungeon dungeon) throws IOException {
    List<String> fresh = new ArrayList<>();
    List<String> known = new ArrayList<>();
    List<String> problems = new ArrayList<>();

    for (Room room : dungeon.rooms()) {
      String key = room.id() + "@" + room.cells().get(0);
      if (!room.complete() || !captured.add(key)) continue;
      RoomRotation rotation = room.rotation() != null ? room.rotation() : RoomRotation.SOUTH;
      if (room.rotation() == null) problems.add(room.id() + ": roof marker not found, saved unrotated");

      int[] box = box(room);
      Capture capture = new Capture(world, box[0], world.minY(), box[1], box[2], Math.min(world.maxY(), 256) - 1, box[3],
          rotation, room.clayX(), room.clayZ(), true, true);
      String hash = SchematicWriter.hash(capture).substring(0, 10);
      Path dir = root.resolve("rooms").resolve(room.id());
      Path base = dir.resolve(room.id() + "_" + hash);
      if (Files.exists(base.resolveSibling(base.getFileName() + ".schem"))) {
        known.add(room.id());
        continue;
      }

      JsonObject json = roomJson(room, rotation, capture, dungeon);
      SchematicWriter.writeSponge(capture, base.resolveSibling(base.getFileName() + ".schem"), spongeMetadata(room));
      Map<String, Integer> missing = SchematicWriter.writeLegacy(capture, base.resolveSibling(base.getFileName() + ".schematic"), mapper);
      if (!missing.isEmpty()) {
        JsonObject m = new JsonObject();
        missing.forEach(m::addProperty);
        json.add("noLegacyEquivalent", m);
        problems.add(room.id() + ": " + missing.size() + " block types have no 1.8 equivalent (saved as air in .schematic)");
      }
      Files.writeString(base.resolveSibling(base.getFileName() + ".json"), GSON.toJson(json), StandardCharsets.UTF_8);
      fresh.add(room.id());
    }

    for (Door door : dungeon.doors()) exportDoor(world, door);
    writeRun(dungeon);
    return new Result(fresh, known, problems);
  }

  /** Inclusive world x/z box of a room: {minX, minZ, maxX, maxZ}. */
  private static int[] box(Room room) {
    int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    for (Cell c : room.cells()) {
      minX = Math.min(minX, c.centerX() - DungeonScan.HALF);
      minZ = Math.min(minZ, c.centerZ() - DungeonScan.HALF);
      maxX = Math.max(maxX, c.centerX() + DungeonScan.HALF);
      maxZ = Math.max(maxZ, c.centerZ() + DungeonScan.HALF);
    }
    return new int[]{minX, minZ, maxX, maxZ};
  }

  /**
   * Room metadata. {@code cells} and {@code doors} are in the saved (local) frame: cell (0,0) is
   * the one at the schematic's min corner, x east, z south; each door is the cell it's in plus the
   * wall it's on. These are what the generator's door rules need.
   */
  static JsonObject roomJson(Room room, RoomRotation rotation, Capture capture, ScannedDungeon dungeon) {
    JsonObject json = new JsonObject();
    json.addProperty("id", room.id());
    json.addProperty("name", room.info() != null ? room.info().name() : room.id());
    json.addProperty("type", room.type());
    json.addProperty("shape", room.shape());
    json.addProperty("core", room.core());
    json.addProperty("capturedRotation", rotation.name());
    json.addProperty("rotationVerified", room.rotation() != null);
    json.addProperty("originY", capture.originY);
    JsonArray size = new JsonArray();
    size.add(capture.width);
    size.add(capture.height);
    size.add(capture.length);
    json.add("size", size);

    int minCellX = Integer.MAX_VALUE;
    int minCellZ = Integer.MAX_VALUE;
    List<int[]> cells = new ArrayList<>();
    for (Cell c : room.cells()) {
      int[] l = capture.toLocal(c.centerX(), 0, c.centerZ());
      int[] cell = {Math.floorDiv(l[0], DungeonScan.PITCH), Math.floorDiv(l[2], DungeonScan.PITCH)};
      cells.add(cell);
      minCellX = Math.min(minCellX, cell[0]);
      minCellZ = Math.min(minCellZ, cell[1]);
    }
    JsonArray cellsJson = new JsonArray();
    for (int[] cell : cells) cellsJson.add(pair(cell[0] - minCellX, cell[1] - minCellZ));
    json.add("cells", cellsJson);

    JsonArray doors = new JsonArray();
    for (Door d : dungeon.doors()) {
      Cell mine = room.cells().contains(d.a()) ? d.a() : room.cells().contains(d.b()) ? d.b() : null;
      if (mine == null) continue;
      Cell other = mine == d.a() ? d.b() : d.a();
      int[] l = capture.toLocal(mine.centerX(), 0, mine.centerZ());
      int dx = other.x() - mine.x();
      int dz = other.z() - mine.z();
      JsonObject door = new JsonObject();
      door.add("cell", pair(Math.floorDiv(l[0], DungeonScan.PITCH) - minCellX, Math.floorDiv(l[2], DungeonScan.PITCH) - minCellZ));
      door.addProperty("side", side(rotation.toLocalX(dx, dz), rotation.toLocalZ(dx, dz)));
      door.addProperty("type", d.type().name());
      doors.add(door);
    }
    json.add("doors", doors);
    return json;
  }

  private static String side(int dx, int dz) {
    if (dz < 0) return SIDES[0];
    if (dx > 0) return SIDES[1];
    if (dz > 0) return SIDES[2];
    return SIDES[3];
  }

  private static JsonArray pair(int a, int b) {
    JsonArray p = new JsonArray();
    p.add(a);
    p.add(b);
    return p;
  }

  private CompoundTag spongeMetadata(Room room) {
    CompoundTag meta = new CompoundTag();
    meta.putString("Name", room.info() != null ? room.info().name() : room.id());
    meta.putString("Author", "Hypixel (captured by Dungeon Scanner)");
    meta.putLong("Date", System.currentTimeMillis());
    meta.putString("DungeonScannerFloor", floor);
    return meta;
  }

  /**
   * A door and a bit of the walls around it: 5 wide along the wall, the gap plus one block into
   * each room, y 67..75. Always saved with the doorway running east-west.
   */
  private void exportDoor(WorldView world, Door door) throws IOException {
    boolean acrossX = door.a().x() != door.b().x();
    int minX = acrossX ? door.x() - 1 : door.x() - 2;
    int maxX = acrossX ? door.x() + 1 : door.x() + 2;
    int minZ = acrossX ? door.z() - 2 : door.z() - 1;
    int maxZ = acrossX ? door.z() + 2 : door.z() + 1;
    if (!world.isLoaded(minX, minZ) || !world.isLoaded(maxX, maxZ)) return;
    String key = door.x() + "," + door.z();
    if (!capturedDoors.add(key)) return;
    RoomRotation turn = acrossX ? RoomRotation.EAST : RoomRotation.SOUTH;
    Capture capture = new Capture(world, minX, 67, minZ, maxX, 75, maxZ, turn, door.x(), door.z(), false, false);
    String type = door.type().name().toLowerCase();
    String hash = SchematicWriter.hash(capture).substring(0, 10);
    Path base = root.resolve("doors").resolve(type).resolve(type + "_" + hash);
    if (Files.exists(base.resolveSibling(base.getFileName() + ".schem"))) return;
    CompoundTag meta = new CompoundTag();
    meta.putString("Name", type + " door");
    SchematicWriter.writeSponge(capture, base.resolveSibling(base.getFileName() + ".schem"), meta);
    SchematicWriter.writeLegacy(capture, base.resolveSibling(base.getFileName() + ".schematic"), mapper);
  }

  /** The whole layout of this run, in world grid coordinates (for checking the generator against). */
  private void writeRun(ScannedDungeon dungeon) throws IOException {
    JsonObject run = new JsonObject();
    run.addProperty("floor", floor);
    run.addProperty("complete", dungeon.complete());
    run.addProperty("width", dungeon.width());
    run.addProperty("height", dungeon.height());
    JsonArray rooms = new JsonArray();
    for (Room r : dungeon.rooms()) {
      JsonObject o = new JsonObject();
      o.addProperty("id", r.id());
      o.addProperty("type", r.type());
      o.addProperty("shape", r.shape());
      o.addProperty("rotation", r.rotation() != null ? r.rotation().name() : null);
      JsonArray cells = new JsonArray();
      for (Cell c : r.cells()) cells.add(pair(c.x(), c.z()));
      o.add("cells", cells);
      rooms.add(o);
    }
    run.add("rooms", rooms);
    JsonArray doors = new JsonArray();
    for (Door d : dungeon.doors()) {
      JsonObject o = new JsonObject();
      o.add("a", pair(d.a().x(), d.a().z()));
      o.add("b", pair(d.b().x(), d.b().z()));
      o.addProperty("type", d.type().name());
      doors.add(o);
    }
    run.add("doors", doors);
    Path file = root.resolve("runs").resolve(runName + ".json");
    Files.createDirectories(file.getParent());
    Files.writeString(file, GSON.toJson(run), StandardCharsets.UTF_8);
  }

  /** Distinct room ids saved so far (over all runs). */
  public static Set<String> savedRooms(Path root) throws IOException {
    Set<String> out = new HashSet<>();
    Path rooms = root.resolve("rooms");
    if (!Files.isDirectory(rooms)) return out;
    try (Stream<Path> dirs = Files.list(rooms)) {
      dirs.filter(Files::isDirectory).forEach(d -> out.add(d.getFileName().toString()));
    }
    return out;
  }
}
