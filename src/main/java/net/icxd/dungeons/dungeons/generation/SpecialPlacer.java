package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Stage 1a: picks cells for the special rooms. They're all 1x1, so they go down before the
 * regular rooms are tiled around them.
 *
 * <p>Every special room except the fairy has exactly one door, so they can't be walked through.
 * Each placement is checked to keep every other cell connected and every dead end touching a free
 * cell. With that invariant the connector can always build a tree unless templates' door rules
 * forbid it.
 *
 * <p>On floors with a {@linkplain DungeonConfig#specialColumn() special column} the puzzles, trap
 * and miniboss room all go in the last column of the map.
 */
final class SpecialPlacer {
  record Special(RoomType type, Position cell) {
  }

  private final DungeonConfig config;
  private final int width;
  private final int height;
  private final Random random;
  private final List<Special> placed = new ArrayList<>();
  /** 0 = free, 1 = walkable special (entrance/fairy), 2 = dead end. */
  private final int[][] state;

  SpecialPlacer(DungeonConfig config, int width, int height, Random random) {
    this.config = config;
    this.width = width;
    this.height = height;
    this.random = random;
    this.state = new int[width][height];
  }

  List<Special> placed() {
    return placed;
  }

  boolean place() {
    Position entrance = pick(cell -> {
      if (inSpecialColumn(cell)) return 0;
      return config.entranceOnEdge() && !onEdge(cell) ? 0 : 1;
    });
    if (entrance == null || !add(RoomType.START, entrance, 2)) return false;

    // Blood: far away from the entrance (and on the edge, if configured).
    int far = Math.max(2, (int) Math.ceil((width + height - 2) * config.bloodDistance()));
    Position blood = pick(cell -> {
      if (inSpecialColumn(cell) || (config.bloodOnEdge() && !onEdge(cell))) return 0;
      return cell.manhattan(entrance) >= far ? 1 : 0;
    });
    if (blood == null || !add(RoomType.BLOOD, blood, 2)) return false;

    if (config.fairy()) {
      // Roughly between the two, and never next to either so each has a regular room in front of it.
      int direct = entrance.manhattan(blood);
      Position fairy = pick(cell -> {
        if (inSpecialColumn(cell)) return 0;
        int a = cell.manhattan(entrance);
        int b = cell.manhattan(blood);
        return a >= 2 && b >= 2 && a + b <= direct + 2 ? 1 : 0;
      });
      if (fairy == null || !add(RoomType.FAIRY, fairy, 1)) return false;
    }

    // Puzzles & co. hang off the ends of branches, which is mostly the edge of the map.
    int puzzles = config.minPuzzles() + random.nextInt(config.maxPuzzles() - config.minPuzzles() + 1);
    for (int i = 0; i < puzzles; i++) if (!placeDeadEnd(RoomType.PUZZLE)) return false;
    for (int i = 0; i < config.traps(); i++) if (!placeDeadEnd(RoomType.TRAP)) return false;
    for (int i = 0; i < config.minibosses(); i++) if (!placeDeadEnd(RoomType.MINIBOSS)) return false;
    return true;
  }

  private boolean placeDeadEnd(RoomType type) {
    for (int tries = 0; tries < 20; tries++) {
      Position cell = config.specialColumn()
          ? pick(c -> inSpecialColumn(c) ? 1 : 0)
          : pick(c -> onEdge(c) ? config.deadEndEdgeWeight() : 1);
      if (cell != null && add(type, cell, 2)) return true;
    }
    return false;
  }

  private interface Weight {
    double of(Position cell);
  }

  /** Random free cell, weighted. */
  private Position pick(Weight weight) {
    List<Position> cells = new ArrayList<>();
    List<Double> weights = new ArrayList<>();
    double total = 0;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (state[x][y] != 0) continue;
        Position p = new Position(x, y);
        double w = weight.of(p);
        if (w <= 0) continue;
        cells.add(p);
        weights.add(w);
        total += w;
      }
    }
    double roll = random.nextDouble() * total;
    for (int i = 0; i < cells.size(); i++) {
      roll -= weights.get(i);
      if (roll <= 0) return cells.get(i);
    }
    return cells.isEmpty() ? null : cells.get(cells.size() - 1);
  }

  /** Places the room if the map stays solvable, otherwise leaves everything as it was. */
  private boolean add(RoomType type, Position cell, int kind) {
    state[cell.x()][cell.y()] = kind;
    if (!solvable()) {
      state[cell.x()][cell.y()] = 0;
      return false;
    }
    placed.add(new Special(type, cell));
    return true;
  }

  /** Walkable cells form one region and every dead end has a walkable neighbour. */
  private boolean solvable() {
    Position start = null;
    int walkable = 0;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (state[x][y] == 2) continue;
        walkable++;
        if (start == null) start = new Position(x, y);
      }
    }
    if (start == null) return false;

    boolean[][] seen = new boolean[width][height];
    Deque<Position> queue = new ArrayDeque<>();
    queue.add(start);
    seen[start.x()][start.y()] = true;
    int reached = 0;
    while (!queue.isEmpty()) {
      Position p = queue.poll();
      reached++;
      for (Direction d : Direction.values()) {
        Position n = p.offset(d);
        if (inside(n) && !seen[n.x()][n.y()] && state[n.x()][n.y()] != 2) {
          seen[n.x()][n.y()] = true;
          queue.add(n);
        }
      }
    }
    if (reached != walkable) return false;

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (state[x][y] != 2) continue;
        boolean touches = false;
        for (Direction d : Direction.values()) {
          Position n = new Position(x, y).offset(d);
          // Must touch a free cell; don't rely on the fairy, it needs its doors for the path.
          if (inside(n) && state[n.x()][n.y()] == 0) touches = true;
        }
        if (!touches) return false;
      }
    }
    return true;
  }

  private boolean inSpecialColumn(Position p) {
    return config.specialColumn() && p.x() == width - 1;
  }

  private boolean onEdge(Position p) {
    return p.x() == 0 || p.y() == 0 || p.x() == width - 1 || p.y() == height - 1;
  }

  private boolean inside(Position p) {
    return p.x() >= 0 && p.y() >= 0 && p.x() < width && p.y() < height;
  }
}
