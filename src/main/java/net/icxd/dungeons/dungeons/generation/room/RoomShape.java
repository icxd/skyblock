package net.icxd.dungeons.dungeons.generation.room;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Footprints a room can have, in grid cells. Every shape is defined once in its "rotation 0" form;
 * the other orientations are derived by rotating it clockwise.
 */
public enum RoomShape {
  ONE_BY_ONE(new Position(0, 0)),
  ONE_BY_TWO(new Position(0, 0), new Position(1, 0)),
  ONE_BY_THREE(new Position(0, 0), new Position(1, 0), new Position(2, 0)),
  ONE_BY_FOUR(new Position(0, 0), new Position(1, 0), new Position(2, 0), new Position(3, 0)),
  TWO_BY_TWO(new Position(0, 0), new Position(1, 0), new Position(0, 1), new Position(1, 1)),
  /** 2x2 with the south-east cell missing. */
  L_SHAPE(new Position(0, 0), new Position(1, 0), new Position(0, 1));

  private final List<Position> cells;
  /** Rotated + normalized cells, indexed by clockwise quarter turns. */
  private final List<Set<Position>> rotated = new ArrayList<>(4);
  /**
   * Distinct footprints (a 2x2 has one, a 1x4 has two, an L has four), as sorted lists so that
   * iterating them is deterministic (the order of {@code Set.of} changes between JVM runs, which
   * would make the same seed generate different dungeons).
   */
  private final List<List<Position>> orientations;

  RoomShape(Position... cells) {
    this.cells = List.of(cells);
    List<List<Position>> distinct = new ArrayList<>();
    for (int r = 0; r < 4; r++) {
      List<Position> sorted = List.copyOf(normalize(rotate(this.cells, r)));
      rotated.add(Set.copyOf(sorted));
      if (!distinct.contains(sorted)) distinct.add(sorted);
    }
    this.orientations = List.copyOf(distinct);
  }

  public int size() {
    return cells.size();
  }

  public List<Position> cells() {
    return cells;
  }

  /** Cells of this shape rotated {@code quarterTurns} clockwise, moved so the min corner is 0,0. */
  public Set<Position> cells(int quarterTurns) {
    return rotated.get(Math.floorMod(quarterTurns, 4));
  }

  public List<List<Position>> orientations() {
    return orientations;
  }

  /**
   * Offset that {@link #cells(int)} applies after rotating. Needed to rotate anything that is
   * expressed in the template's local coordinates (door slots) the same way the cells were.
   */
  public Position normalizeOffset(int quarterTurns) {
    return minCorner(rotate(cells, quarterTurns));
  }

  /** Which rotations of this shape produce exactly {@code footprint} (in world cells). */
  public List<Integer> rotationsMatching(Collection<Position> footprint) {
    List<Integer> result = new ArrayList<>(4);
    if (footprint.size() != size()) return result;
    Set<Position> normalized = new HashSet<>(normalize(footprint));
    for (int r = 0; r < 4; r++) {
      if (rotated.get(r).equals(normalized)) result.add(r);
    }
    return result;
  }

  public static List<Position> rotate(Collection<Position> cells, int quarterTurns) {
    List<Position> out = new ArrayList<>(cells.size());
    for (Position p : cells) out.add(p.rotateClockwise(quarterTurns));
    return out;
  }

  public static List<Position> normalize(Collection<Position> cells) {
    Position min = minCorner(cells);
    List<Position> out = new ArrayList<>(cells.size());
    for (Position p : cells) out.add(p.minus(min));
    out.sort(Comparator.comparingInt(Position::y).thenComparingInt(Position::x));
    return out;
  }

  public static Position minCorner(Collection<Position> cells) {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    for (Position p : cells) {
      minX = Math.min(minX, p.x());
      minY = Math.min(minY, p.y());
    }
    return new Position(minX, minY);
  }
}
