package net.icxd.dungeons.dungeons.generation.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

public class AStar {
  private static final int[][] CARDINAL_DIRECTIONS = {
      {1, 0},
      {-1, 0},
      {0, 1},
      {0, -1}
  };

  public List<Position> findPath(Position start, Position goal, int width, int height, Set<Position> blocked) {
    Objects.requireNonNull(blocked, "blocked");
    return findPath(start, goal, width, height, position -> !blocked.contains(position));
  }

  public List<Position> findPath(Position start, Position goal, int width, int height, Predicate<Position> isWalkable) {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(goal, "goal");
    Objects.requireNonNull(isWalkable, "isWalkable");

    if (width <= 0 || height <= 0)
      return Collections.emptyList();
    if (!isInBounds(start, width, height) || !isInBounds(goal, width, height))
      return Collections.emptyList();
    if (!isWalkable.test(start) || !isWalkable.test(goal))
      return Collections.emptyList();

    PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(Node::fScore));
    Set<Position> closedSet = new HashSet<>();
    Set<Position> openPositions = new HashSet<>();
    Map<Position, Position> cameFrom = new HashMap<>();
    Map<Position, Integer> gScore = new HashMap<>();

    gScore.put(start, 0);
    openSet.add(new Node(start, heuristic(start, goal)));
    openPositions.add(start);

    while (!openSet.isEmpty()) {
      Node currentNode = openSet.poll();
      Position current = currentNode.position();
      openPositions.remove(current);

      if (current.equals(goal))
        return reconstructPath(cameFrom, current);

      if (!closedSet.add(current))
        continue;

      for (Position neighbor : getNeighbors(current, width, height)) {
        if (closedSet.contains(neighbor) || !isWalkable.test(neighbor))
          continue;

        int tentativeGScore = gScore.getOrDefault(current, Integer.MAX_VALUE - 1) + 1;
        if (tentativeGScore >= gScore.getOrDefault(neighbor, Integer.MAX_VALUE))
          continue;

        cameFrom.put(neighbor, current);
        gScore.put(neighbor, tentativeGScore);
        int fScore = tentativeGScore + heuristic(neighbor, goal);

        if (!openPositions.contains(neighbor)) {
          openSet.add(new Node(neighbor, fScore));
          openPositions.add(neighbor);
        } else {
          openSet.add(new Node(neighbor, fScore));
        }
      }
    }

    return Collections.emptyList();
  }

  private int heuristic(Position current, Position goal) {
    return Math.abs(current.x() - goal.x()) + Math.abs(current.y() - goal.y());
  }

  private boolean isInBounds(Position position, int width, int height) {
    return position.x() >= 0 && position.x() < width && position.y() >= 0 && position.y() < height;
  }

  private List<Position> getNeighbors(Position current, int width, int height) {
    List<Position> neighbors = new ArrayList<>(4);
    for (int[] direction : CARDINAL_DIRECTIONS) {
      Position neighbor = new Position(current.x() + direction[0], current.y() + direction[1]);
      if (isInBounds(neighbor, width, height)) {
        neighbors.add(neighbor);
      }
    }
    return neighbors;
  }

  private List<Position> reconstructPath(Map<Position, Position> cameFrom, Position current) {
    List<Position> path = new ArrayList<>();
    path.add(current);

    while (cameFrom.containsKey(current)) {
      current = cameFrom.get(current);
      path.add(current);
    }

    Collections.reverse(path);
    return path;
  }

  private record Node(Position position, int fScore) {
  }
}
