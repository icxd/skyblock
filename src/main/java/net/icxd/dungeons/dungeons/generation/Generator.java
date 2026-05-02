package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.icxd.dungeons.dungeons.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomSize;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.utils.Utils;

public class Generator {

  /** Bump when placement logic changes (shows up in `/dungeon console`). */
  public static final String REVISION = "2026-05-02-blood-corner-reserved-v8";

  public static final int MAX_POLY_TILE_ATTEMPTS = 400;

  public record GenerationReport(
      Map map,
      boolean usedFallback1x1Tree,
      int attemptsUsed,
      String revision
  ) {
  }

  public Map generate(DungeonFloor floor) {
    return generateWithReport(floor).map();
  }

  public GenerationReport generateWithReport(DungeonFloor floor) {
    int w = floor.getMaxXSize();
    int h = floor.getMaxZSize();

    for (int attempt = 1; attempt <= MAX_POLY_TILE_ATTEMPTS; attempt++) {
      Random random = new Random();
      Map map = new Map(floor);
      clearEmpty(map, floor);
      Phase1Context ctx = new Phase1Context(map, w, h, random);
      if (ctx.runPolyominoTiling()) {
        phase2Doors(map, ctx.placements);
        return new GenerationReport(map, false, attempt, REVISION);
      }
    }

    Map map = new Map(floor);
    clearEmpty(map, floor);
    Phase1Context fallback = new Phase1Context(map, w, h, new Random());
    fallback.runFallback1x1Tree();
    phase2Doors(map, fallback.placements);
    return new GenerationReport(map, true, MAX_POLY_TILE_ATTEMPTS, REVISION);
  }

  private static void clearEmpty(Map map, DungeonFloor floor) {
    for (int x = 0; x < floor.getMaxXSize(); x++) {
      for (int z = 0; z < floor.getMaxZSize(); z++) {
        map.setRoom(new Position(x, z), Room.EMPTY, -1);
      }
    }
  }

  private static void phase2Doors(
      Map map,
      List<RoomPlacement> placements
  ) {
    java.util.Map<Integer, RoomPlacement> byId = new HashMap<>();
    for (RoomPlacement rp : placements) {
      byId.put(rp.id(), rp);
    }

    List<DoorEdge> doors = new ArrayList<>();
    HashSet<String> treeEdgeSeen = new HashSet<>();
    int w = map.getWidth();
    int h = map.getHeight();

    for (int x = 0; x < w; x++) {
      for (int z = 0; z < h; z++) {
        Position a = new Position(x, z);
        int ia = map.getRoomInstanceId(a);
        if (ia < 0) continue;

        if (x + 1 < w) {
          Position b = new Position(x + 1, z);
          int ib = map.getRoomInstanceId(b);
          if (ib >= 0 && ia != ib) {
            addDoorIfTreeEdge(ia, ib, a, b, byId, doors, treeEdgeSeen);
          }
        }
        if (z + 1 < h) {
          Position b = new Position(x, z + 1);
          int ib = map.getRoomInstanceId(b);
          if (ib >= 0 && ia != ib) {
            addDoorIfTreeEdge(ia, ib, a, b, byId, doors, treeEdgeSeen);
          }
        }
      }
    }

    map.setRoomPlacements(placements);
    map.setDoorEdges(doors);
  }

  /**
   * At most one logical door per parent–child pair; large footprints can share several grid edges
   * with the same neighbor instance.
   */
  private static void addDoorIfTreeEdge(
      int instA,
      int instB,
      Position cellA,
      Position cellB,
      java.util.Map<Integer, RoomPlacement> byId,
      List<DoorEdge> out,
      HashSet<String> treeEdgeSeen
  ) {
    RoomPlacement ra = byId.get(instA);
    RoomPlacement rb = byId.get(instB);
    if (rb != null && rb.parentId() == instA) {
      if (treeEdgeSeen.add(instA + ">" + instB)) {
        out.add(new DoorEdge(instA, instB, cellA, cellB));
      }
    } else if (ra != null && ra.parentId() == instB) {
      if (treeEdgeSeen.add(instB + ">" + instA)) {
        out.add(new DoorEdge(instB, instA, cellB, cellA));
      }
    }
  }

  /**
   * Picks a prefab for this footprint. When several adjacent cells were independent 1×1 placements,
   * each roll used to be uncorrelated—four neighbors could all randomly become {@link Room#LEAVES}
   * and look like one 2×2 room. We prefer templates that differ from orthogonally adjacent cells
   * when the pool still has alternatives.
   */
  private static Room pickTemplate(RoomSize size, Map dungeonMap, List<Position> footprintCells, int width, int height) {
    Room[] pool = switch (size) {
      case ONE_BY_ONE -> Room.ONE_BY_ONE_ROOMS;
      case TWO_BY_ONE -> Room.ONE_BY_TWO_ROOMS;
      case THREE_BY_ONE -> Room.ONE_BY_THREE_ROOMS;
      case FOUR_BY_ONE -> Room.ONE_BY_FOUR_ROOMS;
      case TWO_BY_TWO -> Room.TWO_BY_TWO_ROOMS;
      // case L_SHAPE -> Room.L_SHAPE_ROOMS;
    };

    Set<Room> neighborTemplates = new HashSet<>();
    Set<Position> fp = new HashSet<>(footprintCells);
    for (Position c : footprintCells) {
      int x = c.x();
      int y = c.y();
      addNeighborTemplate(dungeonMap, fp, width, height, x + 1, y, neighborTemplates);
      addNeighborTemplate(dungeonMap, fp, width, height, x - 1, y, neighborTemplates);
      addNeighborTemplate(dungeonMap, fp, width, height, x, y + 1, neighborTemplates);
      addNeighborTemplate(dungeonMap, fp, width, height, x, y - 1, neighborTemplates);
    }

    List<Room> preferred = new ArrayList<>();
    for (Room r : pool) {
      if (!neighborTemplates.contains(r)) {
        preferred.add(r);
      }
    }
    Room[] choices = preferred.isEmpty() ? pool : preferred.toArray(Room[]::new);
    return choices[Utils.random(0, choices.length - 1)];
  }

  private static void addNeighborTemplate(
      Map dungeonMap,
      Set<Position> footprint,
      int width,
      int height,
      int nx,
      int ny,
      Set<Room> out
  ) {
    if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
      return;
    }
    Position n = new Position(nx, ny);
    if (footprint.contains(n)) {
      return;
    }
    Room r = dungeonMap.getRoom(n);
    if (r != null && r.getType() != RoomType.EMPTY) {
      out.add(r);
    }
  }

  private static final class Phase1Context {
    private final Map map;
    private final int width;
    private final int height;
    private final Random random;
    private final List<RoomPlacement> placements = new ArrayList<>();

    private Phase1Context(
        Map map,
        int width,
        int height,
        Random random
    ) {
      this.map = map;
      this.width = width;
      this.height = height;
      this.random = random;
    }

    /**
     * Single left-to-right, top-to-bottom sweep. Parent for each new footprint is the same as
     * {@link #runFallback1x1Tree} (west cell if present, else north), which guarantees a tree and
     * always completes. The old A* spine + random fill pass almost always deadlocked on 7×7.
     * Skipping already-filled cells allows an earlier step’s 2×2 (etc.) to cover this cell.
     */
    private boolean runPolyominoTiling() {
      placeStart();
      for (int z = 0; z < height; z++) {
        for (int x = 0; x < width; x++) {
          Position p = new Position(x, z);
          if (!isEmptyTile(p)) {
            continue;
          }
          int parentId = rowMajorParentInstanceId(x, z);
          if (parentId < 0) {
            return false;
          }
          if (!tryPlaceRoom(parentId, p)) {
            return false;
          }
        }
      }
      return true;
    }

    private int rowMajorParentInstanceId(int x, int z) {
      if (x > 0) {
        return map.getRoomInstanceId(new Position(x - 1, z));
      }
      if (z > 0) {
        return map.getRoomInstanceId(new Position(x, z - 1));
      }
      return -1;
    }

    private void placeStart() {
      Position p = new Position(0, 0);
      map.setRoom(p, Room.START, 0);
      placements.add(new RoomPlacement(0, -1, Room.START, RoomSize.ONE_BY_ONE, 0, List.of(p)));
    }

    private List<Position> neighbors4(Position p) {
      List<Position> list = new ArrayList<>(4);
      int x = p.x();
      int y = p.y();
      if (x + 1 < width) list.add(new Position(x + 1, y));
      if (x - 1 >= 0) list.add(new Position(x - 1, y));
      if (y + 1 < height) list.add(new Position(x, y + 1));
      if (y - 1 >= 0) list.add(new Position(x, y - 1));
      return list;
    }

    private record FootprintCandidate(RoomSize size, int rotationIndex, List<Position> cells) {
    }

    /**
     * Enumerates every rotation/pivot for every {@link RoomSize}, keeps all footprints that satisfy
     * the tree rule, then picks **uniformly at random** among **distinct** footprints (same set of
     * cells from different pivots counts once). Previously we always took maximum area, which
     * heavily favored 1×4 whenever it fit.
     */
    private boolean tryPlaceRoom(int parentInstanceId, Position mustInclude) {
      List<FootprintCandidate> candidates = new ArrayList<>();
      for (RoomSize size : RoomSize.values()) {
        Position[][] variants = size.getPositions();
        for (int rotIndex = 0; rotIndex < variants.length; rotIndex++) {
          Position[] shape = variants[rotIndex];
          for (Position pivotInShape : shape) {
            Position anchor = new Position(
                mustInclude.x() - pivotInShape.x(),
                mustInclude.y() - pivotInShape.y()
            );
            List<Position> cells = new ArrayList<>(shape.length);
            for (Position part : shape) {
              cells.add(new Position(anchor.x() + part.x(), anchor.y() + part.y()));
            }
            if (isValidFootprint(cells, parentInstanceId)) {
              candidates.add(new FootprintCandidate(size, rotIndex, List.copyOf(cells)));
            }
          }
        }
      }
      filterBloodCornerFootprints(mustInclude, candidates);
      if (candidates.isEmpty()) {
        return false;
      }
      HashMap<Set<Position>, FootprintCandidate> uniqueByCells = new HashMap<>();
      for (FootprintCandidate c : candidates) {
        uniqueByCells.putIfAbsent(new HashSet<>(c.cells()), c);
      }
      List<FootprintCandidate> unique = new ArrayList<>(uniqueByCells.values());
      FootprintCandidate pick = unique.get(random.nextInt(unique.size()));
      commitPlacement(parentInstanceId, pick.cells(), pick.size(), pick.rotationIndex());
      return true;
    }

    /** Prefer type check: unset slots or reference quirks vs {@link Room#EMPTY} must still count as empty. */
    private boolean isEmptyTile(Position p) {
      Room r = map.getRoom(p);
      return r == null || r.getType() == RoomType.EMPTY;
    }

    private boolean isValidFootprint(List<Position> cells, int parentInstanceId) {
      Position endCorner = new Position(width - 1, height - 1);
      if (width > 1 || height > 1) {
        boolean coversEnd = false;
        for (Position c : cells) {
          if (c.equals(endCorner)) {
            coversEnd = true;
            break;
          }
        }
        if (coversEnd && cells.size() != 1) {
          return false;
        }
      }

      Set<Position> footprint = new HashSet<>(cells);
      for (Position c : cells) {
        if (c.x() < 0 || c.y() < 0 || c.x() >= width || c.y() >= height) {
          return false;
        }
        if (!isEmptyTile(c)) {
          return false;
        }
      }

      boolean touchesParent = false;
      for (Position c : cells) {
        for (Position n : neighbors4(c)) {
          if (footprint.contains(n)) {
            continue;
          }
          int owner = map.getRoomInstanceId(n);
          if (owner < 0) {
            continue;
          }
          if (owner == parentInstanceId) {
            touchesParent = true;
          }
          // Other placed neighbors (e.g. "uncle" cells in row-major order) are allowed:
          // the tree is defined by parent pointers; Phase 2 only opens parent–child edges.
        }
      }
      return touchesParent;
    }

    private void commitPlacement(
        int parentInstanceId,
        List<Position> cells,
        RoomSize size,
        int rotationIndex
    ) {
      int newId = placements.size();
      Room template =
          isBloodEndCornerPlacement(cells)
              ? Room.BLOOD
              : pickTemplate(size, map, cells, width, height);
      List<Position> copy = List.copyOf(cells);
      for (Position c : copy) {
        map.setRoom(c, template, newId);
      }
      placements.add(new RoomPlacement(newId, parentInstanceId, template, size, rotationIndex, copy));
    }

    /** Bottom-right map cell is always the blood room: 1×1 only, {@link Room#BLOOD}. */
    private boolean isBloodEndCornerPlacement(List<Position> cells) {
      if (cells.size() != 1) {
        return false;
      }
      Position c = cells.get(0);
      return c.x() == width - 1 && c.y() == height - 1;
    }

    /**
     * The end corner must match {@link Room#BLOOD}'s {@link RoomSize#ONE_BY_ONE} — never absorb it
     * into a larger polyomino.
     */
    private void filterBloodCornerFootprints(Position mustInclude, List<FootprintCandidate> candidates) {
      if (mustInclude.x() != width - 1 || mustInclude.y() != height - 1) {
        return;
      }
      candidates.removeIf(c -> c.size() != RoomSize.ONE_BY_ONE);
    }

    private void runFallback1x1Tree() {
      placements.clear();
      placeStart();
      for (int z = 0; z < height; z++) {
        for (int x = 0; x < width; x++) {
          Position p = new Position(x, z);
          if (!isEmptyTile(p)) {
            continue;
          }
          int parentId = -1;
          if (x > 0) {
            parentId = map.getRoomInstanceId(new Position(x - 1, z));
          }
          if (parentId < 0 && z > 0) {
            parentId = map.getRoomInstanceId(new Position(x, z - 1));
          }
          if (parentId < 0) {
            continue;
          }
          boolean bloodCorner = x == width - 1 && z == height - 1;
          Room template =
              bloodCorner
                  ? Room.BLOOD
                  : pickTemplate(RoomSize.ONE_BY_ONE, map, List.of(p), width, height);
          int newId = placements.size();
          map.setRoom(p, template, newId);
          placements.add(
              new RoomPlacement(
                  newId,
                  parentId,
                  template,
                  RoomSize.ONE_BY_ONE,
                  0,
                  List.of(p)
              )
          );
        }
      }
    }
  }
}
