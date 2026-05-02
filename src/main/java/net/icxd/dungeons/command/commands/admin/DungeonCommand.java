package net.icxd.dungeons.command.commands.admin;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.dungeons.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DoorEdge;
import net.icxd.dungeons.dungeons.generation.Generator;
import net.icxd.dungeons.dungeons.generation.Generator.GenerationReport;
import net.icxd.dungeons.dungeons.generation.Map;
import net.icxd.dungeons.dungeons.generation.RoomPlacement;
import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomSize;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.user.Rank;

@CommandParameters(aliases = "dungeon", permission = Rank.STAFF)
public class DungeonCommand extends SCommand {

  /** Blocks per logical grid cell (floor space); larger than 3 makes layouts easier to read. */
  private static final int WORLD_CELL = 5;
  private static final int MAP_WOOL_Y = 100;
  private static final int ROOM_SIGN_Y = 101;
  /** Wool data: lime — rooms on the tree path from {@link RoomType#BLOOD} up to start. */
  private static final byte SPINE_WOOL_DATA = 5;

  @Override
  public void run(CommandSource source, String[] args) {
    GenerationReport gen = new Generator().generateWithReport(DungeonFloor.FLOOR_7);
    Map map = gen.map();

    printDungeonDebugReport(map, DungeonFloor.FLOOR_7, gen, instance.getLogger());
    source.send(ChatColor.GREEN + "Dungeon debug written to the server log (console / latest.log).");

    renderMap(map, source);
    source.send(
        ChatColor.GRAY
            + "Map preview: "
            + WORLD_CELL
            + "x"
            + WORLD_CELL
            + " blocks/cell. "
            + ChatColor.GREEN
            + "Lime wool"
            + ChatColor.GRAY
            + " = spine (start->blood). "
            + ChatColor.GOLD
            + "Gold block"
            + ChatColor.GRAY
            + " = door (one tick per seam). "
            + ChatColor.DARK_PURPLE
            + "Obsidian"
            + ChatColor.GRAY
            + " = wall (different room, not a tree door)."
    );
  }

  /**
   * Copy-paste friendly layout for debugging generation (instance ids, sizes, placements, doors).
   */
  private static void printDungeonDebugReport(
      Map map,
      DungeonFloor floor,
      GenerationReport gen,
      Logger log
  ) {
    int w = map.getWidth();
    int h = map.getHeight();

    log.info("========== /dungeon console ==========");
    log.info("Generator revision: " + gen.revision());
    log.info("Used 1x1-only fallback tree: " + gen.usedFallback1x1Tree());
    log.info(
        "Poly tiling attempts used: "
            + gen.attemptsUsed()
            + " (cap "
            + Generator.MAX_POLY_TILE_ATTEMPTS
            + " before forced fallback)"
    );
    log.info("Floor: " + floor.name() + "  grid: " + w + " x " + h);
    log.info("Door edges (phase 2): " + map.getDoorEdges().size());

    List<RoomPlacement> placements = map.getRoomPlacements();
    boolean allOneByOne = !placements.isEmpty()
        && placements.stream().allMatch(p -> p.size() == RoomSize.ONE_BY_ONE);
    log.info(
        "Footprint summary: "
            + (allOneByOne
            ? "all placements are 1x1."
            : "at least one multi-cell footprint.")
            + (gen.usedFallback1x1Tree()
            ? " (expected when fallback tree ran)"
            : ""));

    long multiCell = placements.stream().filter(p -> p.size() != RoomSize.ONE_BY_ONE).count();
    log.info("Placements: " + placements.size() + "  (multi-cell footprints: " + multiCell + ")");

    log.info("--- Instance id grid (x ->, each row is z increasing) ---");
    StringBuilder header = new StringBuilder("     ");
    for (int x = 0; x < w; x++) {
      header.append(String.format("%3d ", x));
    }
    log.info(header.toString());
    for (int z = 0; z < h; z++) {
      StringBuilder row = new StringBuilder(String.format("%3d| ", z));
      for (int x = 0; x < w; x++) {
        int id = map.getRoomInstanceId(new Position(x, z));
        row.append(String.format("%3d ", id));
      }
      log.info(row.toString());
    }

    java.util.Map<Integer, RoomSize> sizeById = new HashMap<>();
    for (RoomPlacement rp : placements) {
      sizeById.put(rp.id(), rp.size());
    }

    log.info("--- Footprint size key: 1=1x1 2=1x2 3=1x3 4=1x4 T=2x2 ---");
    for (int z = 0; z < h; z++) {
      StringBuilder row = new StringBuilder("    | ");
      for (int x = 0; x < w; x++) {
        int id = map.getRoomInstanceId(new Position(x, z));
        RoomSize sz = sizeById.get(id);
        row.append(sz == null ? " ? " : " " + sizeLetter(sz) + " ");
      }
      log.info(row.toString());
    }

    log.info("--- Placements (sorted by id) ---");
    placements.stream()
        .sorted(Comparator.comparingInt(RoomPlacement::id))
        .forEach(rp -> {
          Position c0 = rp.cells().get(0);
          Room live = map.getRoom(c0);
          log.info(
              String.format(
                  " id=%d parent=%d size=%-12s rot=%d template=%s type=%s cells=%s",
                  rp.id(),
                  rp.parentId(),
                  rp.size().name(),
                  rp.rotationIndex(),
                  live.getId(),
                  live.getType().name(),
                  formatCells(rp.cells())
              )
          );
        });

    log.info("========== end /dungeon console ==========");
  }

  private static char sizeLetter(RoomSize s) {
    return switch (s) {
      case ONE_BY_ONE -> '1';
      case TWO_BY_ONE -> '2';
      case THREE_BY_ONE -> '3';
      case FOUR_BY_ONE -> '4';
      case TWO_BY_TWO -> 'T';
    };
  }

  private static String formatCells(List<Position> cells) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < cells.size(); i++) {
      if (i > 0) {
        sb.append(";");
      }
      Position p = cells.get(i);
      sb.append('(').append(p.x()).append(',').append(p.y()).append(')');
    }
    return sb.toString();
  }

  /**
   * Preview layout: wool hue per instance; lime wool for the spine (blood → start); gold marks
   * phase-2 tree doors; obsidian marks other cross-instance seams (no door).
   */
  private void renderMap(Map map, CommandSource source) {
    World world = source.getPlayer().getWorld();
    int w = map.getWidth();
    int h = map.getHeight();
    Set<String> doorKeys = doorEdgeKeys(map.getDoorEdges());
    Set<Integer> spineInstances = spineInstanceIds(map);

    clearPreviewPlane(world, w, h, MAP_WOOL_Y);
    clearPreviewPlane(world, w, h, ROOM_SIGN_Y);

    for (int gx = 0; gx < w; gx++) {
      for (int gz = 0; gz < h; gz++) {
        Position gp = new Position(gx, gz);
        Room room = map.getRoom(gp);
        if (room == null || room == Room.EMPTY) {
          continue;
        }

        int instanceId = map.getRoomInstanceId(gp);
        boolean spineCell = spineInstances.contains(instanceId);

        for (int i = 0; i < WORLD_CELL; i++) {
          for (int j = 0; j < WORLD_CELL; j++) {
            int wx = gx * WORLD_CELL + i;
            int wz = gz * WORLD_CELL + j;
            Block block = world.getBlockAt(wx, MAP_WOOL_Y, wz);
            paintPreviewSubBlock(
                block,
                map,
                w,
                h,
                gx,
                gz,
                i,
                j,
                instanceId,
                gp,
                doorKeys,
                spineCell
            );
          }
        }
      }
    }

    placeRoomInstanceIdSigns(map, source, spineInstances);
  }

  private static void clearPreviewPlane(World world, int gridW, int gridH, int y) {
    int span = gridW * WORLD_CELL;
    int depth = gridH * WORLD_CELL;
    for (int wx = 0; wx < span; wx++) {
      for (int wz = 0; wz < depth; wz++) {
        world.getBlockAt(wx, y, wz).setType(Material.AIR);
      }
    }
  }

  /** Undirected key for an edge between two adjacent grid cells. */
  private static String doorEdgeKey(Position a, Position b) {
    int ax = a.x();
    int ay = a.y();
    int bx = b.x();
    int by = b.y();
    if (ax > bx || (ax == bx && ay > by)) {
      return doorEdgeKey(b, a);
    }
    return ax + "," + ay + ";" + bx + "," + by;
  }

  private static Set<String> doorEdgeKeys(List<DoorEdge> edges) {
    HashSet<String> keys = new HashSet<>();
    for (DoorEdge e : edges) {
      keys.add(doorEdgeKey(e.parentCell(), e.childCell()));
    }
    return keys;
  }

  private static boolean isDoorEdgeBetween(Position a, Position b, Set<String> doorKeys) {
    return doorKeys.contains(doorEdgeKey(a, b));
  }

  /**
   * Instance ids on the unique tree path from the blood room up to the start (both endpoints
   * inclusive).
   */
  private static Set<Integer> spineInstanceIds(Map map) {
    HashMap<Integer, RoomPlacement> byId = new HashMap<>();
    for (RoomPlacement rp : map.getRoomPlacements()) {
      byId.put(rp.id(), rp);
    }
    int bloodId = -1;
    for (RoomPlacement rp : map.getRoomPlacements()) {
      if (rp.template().getType() == RoomType.BLOOD) {
        bloodId = rp.id();
        break;
      }
    }
    if (bloodId < 0) {
      return Set.of();
    }
    HashSet<Integer> spine = new HashSet<>();
    int cur = bloodId;
    while (cur >= 0) {
      spine.add(cur);
      RoomPlacement rp = byId.get(cur);
      if (rp == null) {
        break;
      }
      cur = rp.parentId();
    }
    return spine;
  }

  /**
   * Single-block door marker on the canonical side of each seam (east edge of left cell, south
   * edge of upper cell). Opposite side stays wool so we do not draw a long gold strip or obsidian
   * next to the tick.
   */
  private static void paintPreviewSubBlock(
      Block block,
      Map map,
      int w,
      int h,
      int gx,
      int gz,
      int i,
      int j,
      int instanceId,
      Position herePos,
      Set<String> doorKeys,
      boolean spineCell
  ) {
    byte interiorWool = spineCell ? SPINE_WOOL_DATA : instanceToWoolData(instanceId);
    int mid = WORLD_CELL / 2;

    if (i == WORLD_CELL - 1 && gx + 1 < w && j == mid) {
      Position n = new Position(gx + 1, gz);
      int nid = map.getRoomInstanceId(n);
      if (nid >= 0
          && nid != instanceId
          && isDoorEdgeBetween(herePos, n, doorKeys)) {
        block.setType(Material.GOLD_BLOCK);
        return;
      }
    }
    if (j == WORLD_CELL - 1 && gz + 1 < h && i == mid) {
      Position n = new Position(gx, gz + 1);
      int nid = map.getRoomInstanceId(n);
      if (nid >= 0
          && nid != instanceId
          && isDoorEdgeBetween(herePos, n, doorKeys)) {
        block.setType(Material.GOLD_BLOCK);
        return;
      }
    }

    if (i == WORLD_CELL - 1 && gx + 1 < w) {
      Position n = new Position(gx + 1, gz);
      int nid = map.getRoomInstanceId(n);
      if (nid >= 0 && nid != instanceId && !isDoorEdgeBetween(herePos, n, doorKeys)) {
        block.setType(Material.OBSIDIAN);
        return;
      }
    }
    if (j == WORLD_CELL - 1 && gz + 1 < h) {
      Position n = new Position(gx, gz + 1);
      int nid = map.getRoomInstanceId(n);
      if (nid >= 0 && nid != instanceId && !isDoorEdgeBetween(herePos, n, doorKeys)) {
        block.setType(Material.OBSIDIAN);
        return;
      }
    }
    if (i == 0 && gx > 0) {
      Position n = new Position(gx - 1, gz);
      int nid = map.getRoomInstanceId(n);
      if (nid >= 0 && nid != instanceId && !isDoorEdgeBetween(herePos, n, doorKeys)) {
        block.setType(Material.OBSIDIAN);
        return;
      }
    }
    if (j == 0 && gz > 0) {
      Position n = new Position(gx, gz - 1);
      int nid = map.getRoomInstanceId(n);
      if (nid >= 0 && nid != instanceId && !isDoorEdgeBetween(herePos, n, doorKeys)) {
        block.setType(Material.OBSIDIAN);
        return;
      }
    }

    block.setType(Material.WOOL);
    block.setData(interiorWool);
  }

  /** Spread hues so sequential ids are less likely to look identical side-by-side. */
  private static byte instanceToWoolData(int instanceId) {
    return (byte) Math.floorMod(instanceId * 7 + 3, 16);
  }

  /**
   * One standing sign per room instance: id, template id, short type hint, spine marker.
   */
  private void placeRoomInstanceIdSigns(Map map, CommandSource source, Set<Integer> spineInstances) {
    World world = source.getPlayer().getWorld();
    HashMap<Integer, List<Position>> cellsByInstance = new HashMap<>();
    for (int x = 0; x < map.getWidth(); x++) {
      for (int z = 0; z < map.getHeight(); z++) {
        Position p = new Position(x, z);
        Room cellRoom = map.getRoom(p);
        if (cellRoom == null || cellRoom == Room.EMPTY) {
          continue;
        }
        int instanceId = map.getRoomInstanceId(p);
        if (instanceId < 0) {
          continue;
        }
        cellsByInstance.computeIfAbsent(instanceId, k -> new ArrayList<>()).add(p);
      }
    }

    for (var entry : cellsByInstance.entrySet()) {
      int roomInstanceId = entry.getKey();
      List<Position> cells = entry.getValue();
      double wx = 0;
      double wz = 0;
      int mid = WORLD_CELL / 2;
      for (Position c : cells) {
        wx += c.x() * WORLD_CELL + mid;
        wz += c.y() * WORLD_CELL + mid;
      }
      int n = cells.size();
      int signX = (int) Math.round(wx / n);
      int signZ = (int) Math.round(wz / n);
      Block signBlock = world.getBlockAt(signX, ROOM_SIGN_Y, signZ);
      signBlock.setType(Material.SIGN_POST);
      Sign sign = (Sign) signBlock.getState();

      Room template = map.getRoom(cells.get(0));
      sign.setLine(0, "#" + roomInstanceId);
      sign.setLine(1, truncate(template.getId(), 15));
      sign.setLine(2, truncate(roomTypeShort(template.getType()), 15));
      sign.setLine(3, spineInstances.contains(roomInstanceId) ? "~ spine ~" : "");

      sign.update();
    }
  }

  private static String roomTypeShort(RoomType type) {
    return switch (type) {
      case EMPTY -> "";
      case REGULAR -> "regular";
      case START -> "START";
      case FAIRY -> "fairy";
      case PUZZLE -> "puzzle";
      case MINIBOSS -> "miniboss";
      case TRAP -> "trap";
      case BLOOD -> "BLOOD";
    };
  }

  private static String truncate(String s, int max) {
    if (s.length() <= max) {
      return s;
    }
    return s.substring(0, max);
  }
}
