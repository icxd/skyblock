package net.icxd.dungeons.dungeons.generator;

import net.icxd.dungeons.dungeons.generator.rooms.RoomType;
import net.icxd.dungeons.dungeons.generator.utils.Coordinate;

import java.util.*;

public class Pathfinder {

  private int manhattanDistance(Coordinate a, Coordinate b) {
    return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
  }

  public Coordinate[] chooseFarApartNodes(Grid grid, int minDistance) {
    Random random = new Random();
    Coordinate nodeA = null, nodeB = null;

    while (nodeA == null || nodeB == null || manhattanDistance(nodeA, nodeB) < minDistance) {
      int x1 = random.nextInt(grid.getWidth()), y1 = random.nextInt(grid.getHeight());
      int x2 = random.nextInt(grid.getWidth()), y2 = random.nextInt(grid.getHeight());

      nodeA = new Coordinate(x1, y1);
      nodeB = new Coordinate(x2, y2);
    }

    return new Coordinate[]{nodeA, nodeB};
  }

  private int heuristic(Coordinate current, Coordinate goal, Grid grid) {
    int baseCost = manhattanDistance(current, goal);
    int penalty = grid.getRoom(current).type() == RoomType.RED ? 10 : 0;
    return baseCost + penalty;
  }

  public List<Coordinate> findPath(Grid grid, Coordinate start, Coordinate goal) {
    var openSet = new PriorityQueue<>(Comparator.comparingInt(Node::fCost));
    Set<Coordinate> closedSet = new HashSet<>();
    Map<Coordinate, Coordinate> cameFrom = new HashMap<>();
    Map<Coordinate, Integer> gScore = new HashMap<>(), fScore = new HashMap<>();

    gScore.put(start, 0);
    fScore.put(start, heuristic(start, goal, grid));

    openSet.add(new Node(start, fScore.get(start)));

    while (!openSet.isEmpty()) {
      Node currentNode = openSet.poll();
      Coordinate current = currentNode.coordinate();

      if (current.equals(goal))
        return reconstructPath(cameFrom, current);

      closedSet.add(current);

      for (Coordinate neighbor : getNeighbors(current, grid)) {
        if (closedSet.contains(neighbor)) continue;

        int tentativeGScore = gScore.getOrDefault(current, Integer.MAX_VALUE) + 1;

        if (tentativeGScore < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
          cameFrom.put(neighbor, current);
          gScore.put(neighbor, tentativeGScore);
          fScore.put(neighbor, tentativeGScore + heuristic(neighbor, goal, grid));

          if (openSet.stream().noneMatch(n -> n.coordinate().equals(neighbor)))
            openSet.add(new Node(neighbor, fScore.get(neighbor)));
        }
      }
    }

    return Collections.emptyList();
  }

  private List<Coordinate> getNeighbors(Coordinate current, Grid grid) {
    List<Coordinate> neighbors = new ArrayList<>();
    int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // Right, Left, Down, Up

    for (int[] dir : directions) {
      Coordinate neighbor = new Coordinate(current.x() + dir[0], current.y() + dir[1]);
      if (grid.isValidCoordinate(neighbor) && grid.getRoom(neighbor).type() != RoomType.RED)
        neighbors.add(neighbor);
    }

    return neighbors;
  }

  private List<Coordinate> reconstructPath(Map<Coordinate, Coordinate> cameFrom, Coordinate current) {
    List<Coordinate> path = new ArrayList<>();
    while (cameFrom.containsKey(current)) {
      path.add(current);
      current = cameFrom.get(current);
    }
    Collections.reverse(path);
    return path;
  }

  record Node(Coordinate coordinate, int fCost) {}
}
