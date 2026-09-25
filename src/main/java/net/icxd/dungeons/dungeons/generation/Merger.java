package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Stage 2b: a regular 1x1 room left with a single door can't be a regular room (none of Hypixel's
 * regular 1x1s is a dead end). If it and the room it hangs off together form a valid shape (1x1 +
 * 1x1 = 1x2, 1x2 + 1 = 1x3 or L, 1x3 + 1 = 1x4, L + its notch = 2x2), merge them: the merged room
 * keeps the parent's doors, so the tree is unchanged apart from one fewer leaf. Whatever can't be
 * merged becomes a rare room.
 */
final class Merger {
  private Merger() {
  }

  /** @return true if anything was merged (room ids and the grid are renumbered then). */
  static boolean mergeStranded(List<RoomNode> rooms, int[][] grid, List<Integer> criticalPath, RoomPool pool, DungeonConfig config) {
    int w = grid.length;
    int h = grid[0].length;
    boolean[] gone = new boolean[rooms.size()];
    boolean any = false;

    for (RoomNode leaf : rooms) {
      if (gone[leaf.id] || leaf.satisfied() || leaf.type != RoomType.REGULAR || leaf.shape != RoomShape.ONE_BY_ONE) continue;
      if (leaf.doors.size() != 1 || leaf.parent < 0) continue;
      RoomNode parent = rooms.get(leaf.parent);
      if (parent.type != RoomType.REGULAR) continue;

      List<Position> union = new ArrayList<>(parent.cells);
      union.addAll(leaf.cells);
      RoomShape shape = shapeOf(union);
      if (shape == null || !hasRoomFor(shape, rooms, gone, pool, config)) continue;

      Edge inner = leaf.doors.get(0);
      List<Edge> doors = new ArrayList<>(parent.doors);
      doors.remove(inner);
      List<Placement> all = new ArrayList<>();
      for (Room t : pool.templates(RoomType.REGULAR, shape, config.floor())) all.addAll(Placement.enumerate(t, union, w, h));
      List<Placement> fitting = new ArrayList<>();
      for (Placement p : all) if (p.fits(doors)) fitting.add(p);
      if (fitting.isEmpty()) continue;

      parent.cells = List.copyOf(union);
      parent.shape = shape;
      parent.doors.clear();
      parent.doors.addAll(doors);
      parent.allOptions = all;
      parent.options = fitting;
      for (Position c : leaf.cells) grid[c.x()][c.y()] = parent.id;
      gone[leaf.id] = true;
      any = true;
    }
    if (!any) return false;

    // Renumber so ids are list indices again.
    Map<Integer, Integer> newId = new HashMap<>();
    List<RoomNode> kept = new ArrayList<>();
    for (RoomNode r : rooms) {
      if (gone[r.id]) continue;
      newId.put(r.id, kept.size());
      kept.add(r);
    }
    for (RoomNode r : kept) {
      r.id = newId.get(r.id);
      if (r.parent >= 0) r.parent = newId.get(r.parent);
    }
    for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) grid[x][y] = newId.get(grid[x][y]);
    criticalPath.replaceAll(newId::get);
    rooms.clear();
    rooms.addAll(kept);
    return true;
  }

  private static RoomShape shapeOf(List<Position> cells) {
    for (RoomShape s : RoomShape.values()) {
      if (!s.rotationsMatching(cells).isEmpty()) return s;
    }
    return null;
  }

  /** With unique rooms, don't make more rooms of a shape than there are templates for it. */
  private static boolean hasRoomFor(RoomShape shape, List<RoomNode> rooms, boolean[] gone, RoomPool pool, DungeonConfig config) {
    int templates = pool.templates(RoomType.REGULAR, shape, config.floor()).size();
    if (templates == 0) return false;
    if (!pool.canAvoidRepeats()) return true;
    int used = 0;
    for (RoomNode r : rooms) if (!gone[r.id] && r.type == RoomType.REGULAR && r.shape == shape) used++;
    return used < templates;
  }
}
