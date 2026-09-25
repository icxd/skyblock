package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.ToIntFunction;

import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Stage 1b: fills every cell that isn't taken by a special room with regular rooms of random
 * shapes. Works like wave function collapse: always fill the empty cell with the fewest ways to
 * cover it next (ties broken randomly), picking a shape by weight among the ones that fit. That
 * keeps awkward leftover pockets from turning into a pile of 1x1s. It can't fail, a 1x1 always fits.
 *
 * <p>Placements that would wall a single empty cell in against just one room (e.g. the notch of an
 * L) are avoided when possible: that cell could only ever be a dead end, and regular 1x1 rooms are
 * never dead ends, so it would have to become a rare room.
 */
final class Tiler {
  record Piece(RoomShape shape, List<Position> cells) {
  }

  private final int[][] grid;
  private final int width;
  private final int height;
  private final Random random;
  private final Map<RoomShape, Double> weights;
  private final Map<RoomShape, Integer> remaining;
  /** Ids that can't be exited into (entrance, blood). */
  private final Set<Integer> blocked;
  /** Ids of 1x1 rooms placed by this tiler. */
  private final Set<Integer> singles = new HashSet<>();
  /** Most doors any template could have in a footprint (0 = no template has a usable wall). */
  private final ToIntFunction<Piece> doorCapacity;

  /**
   * @param grid   -1 = empty, anything else is taken
   * @param limits  max number of rooms per shape (missing = unlimited); 1x1 is always unlimited
   * @param blocked room ids already on the grid that can't be exited into
   * @param doorCapacity most doors a footprint could get from any template; pieces with none are
   *                     skipped and pieces that could only be dead ends are made less likely
   */
  Tiler(int[][] grid, Random random, Map<RoomShape, Double> weights, Map<RoomShape, Integer> limits,
        Set<Integer> blocked, ToIntFunction<Piece> doorCapacity) {
    this.blocked = blocked;
    this.doorCapacity = doorCapacity;
    this.grid = grid;
    this.width = grid.length;
    this.height = grid[0].length;
    this.random = random;
    this.weights = new EnumMap<>(RoomShape.class);
    this.weights.putAll(weights);
    this.weights.merge(RoomShape.ONE_BY_ONE, 0.01, Math::max); // the filler of last resort
    this.remaining = new EnumMap<>(RoomShape.class);
    this.remaining.putAll(limits);
    this.remaining.remove(RoomShape.ONE_BY_ONE);
  }

  List<Piece> fill(int firstId) {
    List<Piece> pieces = new ArrayList<>();
    while (true) {
      Position best = null;
      List<Piece> bestOptions = null;
      int ties = 0;
      for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
          if (grid[x][y] != -1) continue;
          Position p = new Position(x, y);
          List<Piece> options = piecesCovering(p);
          if (bestOptions == null || options.size() < bestOptions.size()) {
            best = p;
            bestOptions = options;
            ties = 1;
          } else if (options.size() == bestOptions.size() && random.nextInt(++ties) == 0) {
            best = p;
            bestOptions = options;
          }
        }
      }
      if (best == null) return pieces;

      List<Piece> clean = new ArrayList<>();
      for (Piece p : bestOptions) if (!createsPocket(p, firstId + pieces.size())) clean.add(p);
      Piece piece = pickWeighted(clean.isEmpty() ? bestOptions : clean);
      int id = firstId + pieces.size();
      for (Position c : piece.cells()) grid[c.x()][c.y()] = id;
      remaining.computeIfPresent(piece.shape(), (s, n) -> n - 1);
      if (piece.shape() == RoomShape.ONE_BY_ONE) singles.add(id);
      pieces.add(piece);
    }
  }

  /**
   * Would placing {@code piece} leave a cell (empty, or a 1x1 this tiler placed) walled in with at
   * most one room it could exit towards? The entrance and blood room don't count as exits, the
   * critical path already uses their only door.
   */
  private boolean createsPocket(Piece piece, int id) {
    for (Position c : piece.cells()) grid[c.x()][c.y()] = id;
    boolean pocket = false;
    for (Position c : piece.cells()) {
      for (Direction d : Direction.values()) {
        Position n = c.offset(d);
        if (!inside(n) || piece.cells().contains(n)) continue;
        int owner = grid[n.x()][n.y()];
        if ((owner == -1 || singles.contains(owner)) && isPocket(n)) pocket = true;
      }
    }
    for (Position c : piece.cells()) grid[c.x()][c.y()] = -1;
    return pocket;
  }

  private boolean isPocket(Position cell) {
    int self = grid[cell.x()][cell.y()];
    int exit = -2;
    for (Direction d : Direction.values()) {
      Position n = cell.offset(d);
      if (!inside(n)) continue;
      int id = grid[n.x()][n.y()];
      if (id == -1) return false;
      if (id == self || blocked.contains(id)) continue;
      if (exit != -2 && exit != id) return false;
      exit = id;
    }
    return true;
  }

  private boolean inside(Position p) {
    return p.x() >= 0 && p.y() >= 0 && p.x() < width && p.y() < height;
  }

  /** Every placement of every allowed shape that covers {@code cell} and only empty cells. */
  private List<Piece> piecesCovering(Position cell) {
    List<Piece> out = new ArrayList<>();
    for (RoomShape shape : RoomShape.values()) {
      if (weights.getOrDefault(shape, 0.0) <= 0) continue;
      if (remaining.containsKey(shape) && remaining.get(shape) <= 0) continue;
      for (List<Position> orientation : shape.orientations()) {
        for (Position pivot : orientation) {
          Position anchor = cell.minus(pivot);
          List<Position> cells = new ArrayList<>(orientation.size());
          boolean fits = true;
          for (Position o : orientation) {
            Position c = anchor.plus(o);
            if (c.x() < 0 || c.y() < 0 || c.x() >= width || c.y() >= height || grid[c.x()][c.y()] != -1) {
              fits = false;
              break;
            }
            cells.add(c);
          }
          if (fits) {
            Piece piece = new Piece(shape, cells);
            if (shape == RoomShape.ONE_BY_ONE || doorCapacity.applyAsInt(piece) > 0) out.add(piece);
          }
        }
      }
    }
    return out;
  }

  /**
   * Weight is per shape, not per placement: a 1x4 has far more placements covering a cell than a
   * 1x1, which would otherwise make big rooms dominate regardless of the configured weights.
   */
  private Piece pickWeighted(List<Piece> options) {
    // Footprints whose templates could only ever have one door (e.g. a corridor with an end
    // against the map border) are only used when nothing better fits.
    List<Piece> passable = new ArrayList<>();
    for (Piece p : options) if (p.shape() == RoomShape.ONE_BY_ONE || doorCapacity.applyAsInt(p) >= 2) passable.add(p);
    if (!passable.isEmpty() && random.nextDouble() < 0.9) options = passable;
    Map<RoomShape, List<Piece>> byShape = new EnumMap<>(RoomShape.class);
    for (Piece p : options) byShape.computeIfAbsent(p.shape(), k -> new ArrayList<>()).add(p);
    double total = 0;
    for (RoomShape s : byShape.keySet()) total += weights.get(s);
    double roll = random.nextDouble() * total;
    for (Map.Entry<RoomShape, List<Piece>> e : byShape.entrySet()) {
      roll -= weights.get(e.getKey());
      if (roll <= 0) return e.getValue().get(random.nextInt(e.getValue().size()));
    }
    List<Piece> last = byShape.get(RoomShape.ONE_BY_ONE);
    return last != null ? last.get(0) : options.get(options.size() - 1);
  }
}
