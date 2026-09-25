package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Checks every rule a finished layout has to obey. Returns a list of problems (empty = valid), so
 * it can be used from tests and from the debug command alike.
 */
public final class LayoutValidator {
  private LayoutValidator() {
  }

  public static List<String> validate(DungeonLayout layout) {
    List<String> errors = new ArrayList<>();
    int w = layout.getWidth();
    int h = layout.getHeight();
    List<PlacedRoom> rooms = layout.getRooms();

    // Full coverage, no overlaps, footprint matches the template in the chosen rotation.
    int[][] owner = new int[w][h];
    for (int[] col : owner) Arrays.fill(col, -1);
    for (PlacedRoom r : rooms) {
      for (Position c : r.cells()) {
        if (owner[c.x()][c.y()] != -1) errors.add("cell " + c + " covered twice");
        owner[c.x()][c.y()] = r.id();
      }
      if (r.template().getShape() != r.shape()) errors.add("room " + r.id() + " template shape mismatch");
      if (!r.shape().rotationsMatching(r.cells()).contains(r.rotation())) {
        errors.add("room " + r.id() + " rotation " + r.rotation() + " doesn't fit its footprint");
      }
      if (r.template().getType() != r.type()) errors.add("room " + r.id() + " template type mismatch");
      if (!isConnected(r.cells())) errors.add("room " + r.id() + " footprint not connected");
    }
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        if (owner[x][y] == -1) errors.add("cell " + x + "," + y + " empty");
      }
    }

    // Doors: between two different adjacent rooms, allowed by both templates, one per room pair.
    Set<String> pairs = new HashSet<>();
    Map<Integer, List<Integer>> tree = new HashMap<>();
    for (Door d : layout.getDoors()) {
      Edge e = d.edge();
      int a = owner[e.a().x()][e.a().y()];
      int b = owner[e.b().x()][e.b().y()];
      if (a == b) errors.add("door " + e + " inside a room");
      if (!((a == d.parent() && b == d.child()) || (b == d.parent() && a == d.child()))) {
        errors.add("door " + e + " doesn't join rooms " + d.parent() + " and " + d.child());
      }
      if (!pairs.add(Math.min(a, b) + "-" + Math.max(a, b))) errors.add("second door between " + a + " and " + b);
      for (int id : new int[]{a, b}) {
        PlacedRoom r = rooms.get(id);
        boolean allowed = Placement.enumerate(r.template(), r.cells(), w, h).stream()
            .filter(p -> p.rotation() == r.rotation())
            .anyMatch(p -> p.allows(e));
        if (!allowed) errors.add("door " + e + " not allowed by " + r.template().getId() + " rot " + r.rotation());
      }
      tree.computeIfAbsent(d.parent(), k -> new ArrayList<>()).add(d.child());
      tree.computeIfAbsent(d.child(), k -> new ArrayList<>()).add(d.parent());
    }
    for (PlacedRoom r : rooms) {
      List<Edge> edges = r.doors().stream().map(Door::edge).toList();
      boolean fits = Placement.enumerate(r.template(), r.cells(), w, h).stream()
          .anyMatch(p -> p.rotation() == r.rotation() && p.fits(edges));
      if (!fits) {
        errors.add("room " + r.id() + " (" + r.template().getId() + " rot " + r.rotation() + ") can't have doors " + edges);
      }
    }

    // Tree: n - 1 doors and everything reachable from the entrance.
    if (layout.getDoors().size() != rooms.size() - 1) {
      errors.add(layout.getDoors().size() + " doors for " + rooms.size() + " rooms (tree needs n-1)");
    }
    List<PlacedRoom> entrances = layout.roomsOfType(RoomType.START);
    List<PlacedRoom> bloods = layout.roomsOfType(RoomType.BLOOD);
    if (entrances.size() != 1 || bloods.size() != 1) {
      errors.add("expected one entrance and one blood room");
      return errors;
    }
    int start = entrances.get(0).id();
    Map<Integer, Integer> parent = new HashMap<>();
    Deque<Integer> queue = new ArrayDeque<>();
    parent.put(start, -1);
    queue.add(start);
    while (!queue.isEmpty()) {
      int id = queue.poll();
      for (int n : tree.getOrDefault(id, List.of())) {
        if (parent.containsKey(n)) {
          if (parent.get(id) != n) errors.add("loop through room " + n);
          continue;
        }
        parent.put(n, id);
        queue.add(n);
      }
    }
    if (parent.size() != rooms.size()) errors.add((rooms.size() - parent.size()) + " rooms unreachable");

    // Critical path: the tree path entrance -> blood, and the fairy is on it.
    List<Integer> path = new ArrayList<>();
    for (Integer cur = bloods.get(0).id(); cur != null && cur != -1; cur = parent.get(cur)) path.add(0, cur);
    if (!path.equals(layout.getCriticalPath())) errors.add("critical path " + layout.getCriticalPath() + " != tree path " + path);
    for (PlacedRoom f : layout.roomsOfType(RoomType.FAIRY)) {
      if (!path.contains(f.id())) errors.add("fairy room not between entrance and blood");
    }
    for (Door d : layout.getDoors()) {
      boolean onPath = path.contains(d.parent()) && path.contains(d.child());
      DoorType expected = !onPath ? DoorType.NORMAL
          : rooms.get(d.child()).type() == RoomType.BLOOD ? DoorType.BLOOD
          : rooms.get(d.parent()).type() == RoomType.START ? DoorType.ENTRANCE
          : rooms.get(d.child()).type() == RoomType.FAIRY ? DoorType.FAIRY
          : DoorType.WITHER;
      if (d.type() != expected) errors.add("door " + d.edge() + " is " + d.type() + ", expected " + expected);
    }
    for (PlacedRoom r : rooms) {
      if (r.type() == RoomType.REGULAR && r.shape() == RoomShape.ONE_BY_ONE
          && r.template().isExactDoors() && r.doors().size() < 2) {
        errors.add("regular 1x1 room " + r.id() + " is a dead end");
      }
    }
    return errors;
  }

  private static boolean isConnected(List<Position> cells) {
    Set<Position> set = new HashSet<>(cells);
    Set<Position> seen = new HashSet<>();
    Deque<Position> queue = new ArrayDeque<>();
    queue.add(cells.get(0));
    seen.add(cells.get(0));
    while (!queue.isEmpty()) {
      Position p = queue.poll();
      for (Direction d : Direction.values()) {
        Position n = p.offset(d);
        if (set.contains(n) && seen.add(n)) queue.add(n);
      }
    }
    return seen.size() == set.size();
  }
}
