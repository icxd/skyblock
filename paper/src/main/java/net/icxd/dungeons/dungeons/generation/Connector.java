package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Stage 2: decides which walls become doors. The result is a spanning tree over the rooms (so no
 * loops and every room has exactly one way in), built in these steps:
 *
 * <ol>
 *   <li>the critical path Entrance -> Fairy -> Blood, found with a depth-first search that is
 *       biased towards the next waypoint but randomised, so it's direct-ish without being a
 *       straight line. Only regular rooms can be walked through, so the rooms right before the
 *       fairy and blood rooms are always regular rooms (that's where the keys for the doors after
 *       the fairy and into blood drop);
 *   <li>everything else hung off that path with a "growing tree" (the maze algorithm): keep a list
 *       of rooms that can still take doors, pick one (usually the newest, sometimes a random one),
 *       open a door from it into an unvisited neighbour. Rooms that still <em>need</em> doors (a
 *       1x1 straight/corner/T/cross room that only has its entrance so far) always go first, and a
 *       one-step lookahead avoids walling such a room in. Dead ends (puzzles, trap, miniboss) are
 *       attached last, preferably to rooms that still need a door;
 *   <li>a repair pass that re-hangs subtrees off rooms that still need a door (see {@link #repair}).
 * </ol>
 *
 * <p>Every door is checked against both rooms' remaining template options ({@link RoomNode#accepts})
 * so door rules and max door counts (dead ends like puzzles/blood) are respected as the tree grows.
 */
final class Connector {
    private static final int PATH_SEARCH_BUDGET = 20_000;

    private record Move(RoomNode to, Edge edge, double score) {
    }

    private final List<RoomNode> rooms;
    private final int[][] grid;
    private final int width;
    private final int height;
    private final Random random;
    private final double newestBias;
    private final double pathNoise;

    /** For each room: neighbour room id -> the walls shared with it. */
    private final List<Map<Integer, List<Edge>>> adjacency = new ArrayList<>();
    private final boolean[] visited;
    private final List<Integer> criticalPath = new ArrayList<>();
    private int budget;
    /** Why {@link #connect} returned false, for tuning. */
    String failure;

    Connector(List<RoomNode> rooms, int[][] grid, Random random, double newestBias, double pathNoise) {
        this.rooms = rooms;
        this.grid = grid;
        this.width = grid.length;
        this.height = grid[0].length;
        this.random = random;
        this.newestBias = newestBias;
        this.pathNoise = pathNoise;
        this.visited = new boolean[rooms.size()];
        buildAdjacency();
    }

    List<Integer> criticalPath() {
        return criticalPath;
    }

    boolean connect(RoomNode entrance, RoomNode fairy, RoomNode blood) {
        visited[entrance.id] = true;
        entrance.depth = 0;
        criticalPath.add(entrance.id);

        List<RoomNode> waypoints = fairy != null ? List.of(fairy, blood) : List.of(blood);
        budget = PATH_SEARCH_BUDGET;
        if (!walk(entrance, waypoints, 0, distancesTo(waypoints.get(0)))) {
            failure = "critical path";
            return false;
        }
        if (!growTree()) return false;
        repair();
        return true;
    }

    /**
     * Local search for rooms that ended up needing a door they didn't get (a 1x1 regular room with
     * only its entrance). For such a room L, look for a neighbour Q that isn't in L's own branch and
     * move Q's whole subtree so it hangs off L instead of Q's current parent Z. That swaps one tree
     * edge for another, so it's still a tree. Only done when L, Q and Z all still have a legal
     * template afterwards and Q isn't on the critical path.
     */
    private void repair() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (RoomNode l : rooms) {
                if (l.satisfied() || l.parent < 0) continue;
                if (tryRehang(l)) changed = true;
            }
        }
        // Recompute depths, and leave only the options that fit each room's final doors.
        for (RoomNode r : rooms) r.depth = depthOf(r);
        for (RoomNode r : rooms) {
            List<Placement> fitting = new ArrayList<>();
            for (Placement p : r.allOptions) if (p.fits(r.doors)) fitting.add(p);
            if (!fitting.isEmpty()) r.options = fitting;
        }
    }

    private boolean tryRehang(RoomNode l) {
        List<Move> candidates = new ArrayList<>();
        for (Map.Entry<Integer, List<Edge>> e : adjacency.get(l.id).entrySet()) {
            RoomNode q = rooms.get(e.getKey());
            if (q.id == l.parent || q.parent < 0 || criticalPath.contains(q.id) || isAncestor(q, l)) continue;
            for (Edge edge : e.getValue()) candidates.add(new Move(q, edge, 0));
        }
        Collections.shuffle(candidates, random);
        for (Move m : candidates) {
            RoomNode q = m.to();
            RoomNode z = rooms.get(q.parent);
            Edge oldEdge = q.doors.get(0);

            List<Edge> lDoors = new ArrayList<>(l.doors);
            lDoors.add(m.edge());
            List<Edge> qDoors = new ArrayList<>(q.doors);
            qDoors.remove(oldEdge);
            qDoors.add(0, m.edge());
            List<Edge> zDoors = new ArrayList<>(z.doors);
            zDoors.remove(oldEdge);
            if (!l.couldHave(lDoors) || !q.couldHave(qDoors) || !z.couldHave(zDoors)) continue;

            l.doors.add(m.edge());
            q.doors.clear();
            q.doors.addAll(qDoors);
            z.doors.remove(oldEdge);
            q.parent = l.id;
            for (RoomNode r : List.of(l, q, z)) {
                List<Placement> fitting = new ArrayList<>();
                for (Placement p : r.allOptions) if (p.fits(r.doors)) fitting.add(p);
                r.options = fitting;
            }
            return true;
        }
        return false;
    }

    /** Is {@code ancestor} on the way from {@code room} up to the entrance (or the room itself)? */
    private boolean isAncestor(RoomNode ancestor, RoomNode room) {
        for (int id = room.id; id >= 0; id = rooms.get(id).parent) {
            if (id == ancestor.id) return true;
        }
        return false;
    }

    private int depthOf(RoomNode room) {
        int depth = 0;
        for (int id = room.parent; id >= 0; id = rooms.get(id).parent) depth++;
        return depth;
    }

    /**
     * Grows the rest of the tree off the critical path, in two phases:
     *
     * <ol>
     *   <li>every room that can be walked through, with a growing tree (newest-first makes long
     *       winding branches, random picks make it bushy);
     *   <li>the dead ends (puzzles, trap, miniboss) last, each hung off a neighbour, preferring
     *       neighbours that still need a door. Attaching them early would take away the only exit
     *       some 1x1 pocket had.
     * </ol>
     */
    private boolean growTree() {
        boolean[] deadEnd = new boolean[rooms.size()];
        for (RoomNode r : rooms) deadEnd[r.id] = !r.canTakeMoreDoors() || maxDoors(r) <= 1;

        List<RoomNode> active = new ArrayList<>();
        for (int id : criticalPath) {
            prune(rooms.get(id));
            if (rooms.get(id).canTakeMoreDoors()) active.add(rooms.get(id));
        }
        Collections.shuffle(active, random);

        while (!active.isEmpty()) {
            // Hungry rooms (not satisfied with their doors yet) go first, otherwise the growing-tree pick.
            int index = hungriest(active);
            boolean hungry = index >= 0;
            if (!hungry) index = random.nextDouble() < newestBias ? active.size() - 1 : random.nextInt(active.size());
            RoomNode from = active.get(index);
            List<Move> frontier = moves(from, deadEnd);
            if (frontier.isEmpty()) {
                active.remove(index);
                continue;
            }

            // Look one step ahead: a move is bad if it leaves a room that needs two or more doors (a
            // regular 1x1) with no unvisited neighbour to put its next door towards; that room would
            // have to become a dead end. If the chosen room only has bad moves, take the least bad move
            // any active room has.
            int best = bestPenalty(from, frontier);
            if (best > 0 && !hungry) {
                for (RoomNode other : active) {
                    if (other == from) continue;
                    List<Move> m = moves(other, deadEnd);
                    if (m.isEmpty()) continue;
                    int p = bestPenalty(other, m);
                    if (p < best) {
                        best = p;
                        from = other;
                        frontier = m;
                    }
                }
            }
            final RoomNode source = from;
            final int threshold = best;
            frontier.removeIf(m -> penalty(source, m) > threshold);

            Move pick = frontier.get(random.nextInt(frontier.size()));
            attach(from, pick);
            active.removeIf(r -> !r.canTakeMoreDoors());
            if (pick.to().canTakeMoreDoors()) active.add(pick.to());
        }

        List<RoomNode> pending = new ArrayList<>();
        for (RoomNode r : rooms) {
            if (visited[r.id]) continue;
            if (!deadEnd[r.id]) {
                failure = "rooms unreachable";
                return false;
            }
            pending.add(r);
        }
        Collections.shuffle(pending, random);

        while (!pending.isEmpty()) {
            // Most constrained dead end first; give it to a hungry neighbour if there is one.
            RoomNode pickRoom = null;
            List<Object[]> pickOptions = null;
            for (RoomNode d : pending) {
                List<Object[]> options = new ArrayList<>();
                for (Map.Entry<Integer, List<Edge>> e : adjacency.get(d.id).entrySet()) {
                    RoomNode parent = rooms.get(e.getKey());
                    if (!visited[parent.id]) continue;
                    for (Edge edge : e.getValue()) {
                        if (parent.accepts(edge) && d.accepts(edge)) options.add(new Object[]{parent, edge});
                    }
                }
                if (pickOptions == null || options.size() < pickOptions.size()) {
                    pickRoom = d;
                    pickOptions = options;
                }
            }
            if (pickOptions.isEmpty()) {
                failure = "dead end unreachable";
                return false;
            }
            List<Object[]> hungryParents = new ArrayList<>();
            for (Object[] o : pickOptions) if (!((RoomNode) o[0]).satisfied()) hungryParents.add(o);
            List<Object[]> from = hungryParents.isEmpty() ? pickOptions : hungryParents;
            Object[] choice = from.get(random.nextInt(from.size()));
            attach((RoomNode) choice[0], new Move(pickRoom, (Edge) choice[1], 0));
            pending.remove(pickRoom);
        }
        return true;
    }

    private void attach(RoomNode from, Move move) {
        open(from, move.to(), move.edge());
        visited[move.to().id] = true;
        // The new room can't take doors from its other visited neighbours any more (that would be a
        // loop), so drop their options that were counting on one.
        prune(move.to());
        for (int n : adjacency.get(move.to().id).keySet()) {
            if (visited[n]) prune(rooms.get(n));
        }
    }

    private static int maxDoors(RoomNode room) {
        int max = 0;
        for (Placement p : room.options) max = Math.max(max, p.doorLimit());
        return max;
    }

    private List<Move> moves(RoomNode from, boolean[] skip) {
        List<Move> out = new ArrayList<>();
        for (Map.Entry<Integer, List<Edge>> e : adjacency.get(from.id).entrySet()) {
            RoomNode to = rooms.get(e.getKey());
            if (visited[to.id] || skip[to.id]) continue;
            for (Edge edge : e.getValue()) {
                if (from.accepts(edge) && to.accepts(edge)) out.add(new Move(to, edge, 0));
            }
        }
        return out;
    }

    private int bestPenalty(RoomNode from, List<Move> moves) {
        int best = Integer.MAX_VALUE;
        for (Move m : moves) best = Math.min(best, penalty(from, m));
        return best;
    }

    /** How many rooms this move would doom to being a dead end they can't be. */
    private int penalty(RoomNode from, Move move) {
        RoomNode to = move.to();
        visited[to.id] = true;
        int penalty = 0;
        if (needsTwoDoors(to) && unvisitedNeighbours(to) == 0) penalty++;
        for (int n : adjacency.get(to.id).keySet()) {
            RoomNode other = rooms.get(n);
            if (other == from || unvisitedNeighbours(other) > 0) continue;
            if (visited[n] ? !other.satisfied() : needsTwoDoors(other)) penalty++;
        }
        visited[to.id] = false;
        return penalty;
    }

    /** Every remaining option for this room needs at least two doors. */
    private static boolean needsTwoDoors(RoomNode room) {
        for (Placement p : room.options) {
            if (p.requiredDoors().size() < 2) return false;
        }
        return true;
    }

    private int unvisitedNeighbours(RoomNode room) {
        int n = 0;
        for (int other : adjacency.get(room.id).keySet()) if (!visited[other]) n++;
        return n;
    }

    /** Newest active room that isn't satisfied with its doors yet, or -1. */
    private int hungriest(List<RoomNode> active) {
        for (int i = active.size() - 1; i >= 0; i--) {
            if (!active.get(i).satisfied()) return i;
        }
        return -1;
    }

    /**
     * Removes options that need a door into a room that's already in the tree (other than through
     * doors this room already has). If nothing would be left, the room is stuck; it keeps its
     * options so the caller can see it's unsatisfied and deal with it (see {@code RARE} rooms).
     */
    private void prune(RoomNode room) {
        List<Placement> kept = new ArrayList<>(room.options.size());
        for (Placement p : room.options) {
            boolean dead = false;
            for (Edge e : p.requiredDoors()) {
                if (room.doors.contains(e)) continue;
                int other = otherRoom(room, e);
                if (visited[other]) {
                    dead = true;
                    break;
                }
            }
            if (!dead) kept.add(p);
        }
        if (!kept.isEmpty()) room.options = kept;
    }

    private int otherRoom(RoomNode room, Edge edge) {
        Position a = edge.a();
        Position b = edge.b();
        int ia = grid[a.x()][a.y()];
        return ia == room.id ? grid[b.x()][b.y()] : ia;
    }

    /**
     * Depth-first search from {@code current} through {@code waypoints[next..]} in order, over
     * unvisited rooms. Backtracks across waypoints too: if the fairy can't reach the blood room, the
     * entrance -> fairy leg is re-routed. Only regular rooms can be walked through.
     */
    private boolean walk(RoomNode current, List<RoomNode> waypoints, int next, int[] distance) {
        if (current == waypoints.get(next)) {
            if (next == waypoints.size() - 1) return true;
            return walk(current, waypoints, next + 1, distancesTo(waypoints.get(next + 1)));
        }
        if (--budget < 0) return false;

        RoomNode target = waypoints.get(next);
        List<Move> moves = new ArrayList<>();
        for (Map.Entry<Integer, List<Edge>> e : adjacency.get(current.id).entrySet()) {
            RoomNode to = rooms.get(e.getKey());
            if (visited[to.id] || distance[to.id] < 0) continue;
            if (to != target && to.type != RoomType.REGULAR) continue;
            for (Edge edge : e.getValue()) {
                if (current.accepts(edge) && to.accepts(edge)) {
                    // Noise lets the path wander a bit instead of always taking a shortest route; walling in
                    // a 1x1 room (see penalty) is avoided unless there's no other way.
                    double score = distance[to.id] + random.nextDouble() * pathNoise;
                    moves.add(new Move(to, edge, score + 10 * penalty(current, new Move(to, edge, 0))));
                }
            }
        }
        moves.sort(Comparator.comparingDouble(Move::score));

        for (Move move : moves) {
            RoomNode to = move.to();
            List<Placement> currentBefore = current.options;
            List<Placement> toBefore = to.options;

            open(current, to, move.edge());
            visited[to.id] = true;
            criticalPath.add(to.id);
            if (walk(to, waypoints, next, distance)) return true;
            if (budget < 0) return false;

            // Undo.
            criticalPath.remove(criticalPath.size() - 1);
            visited[to.id] = false;
            current.doors.remove(current.doors.size() - 1);
            to.doors.remove(to.doors.size() - 1);
            to.parent = -1;
            current.options = currentBefore;
            to.options = toBefore;
        }
        return false;
    }

    private void open(RoomNode parent, RoomNode child, Edge edge) {
        parent.options = parent.optionsAfter(edge);
        child.options = child.optionsAfter(edge);
        parent.doors.add(edge);
        child.doors.add(edge);
        child.parent = parent.id;
        child.depth = parent.depth + 1;
    }

    /**
     * BFS distance (in rooms) to {@code target} through unvisited regular rooms; -1 = can't get
     * there at all, which also prunes dead branches from the path search.
     */
    private int[] distancesTo(RoomNode target) {
        int[] distance = new int[rooms.size()];
        Arrays.fill(distance, -1);
        Deque<Integer> queue = new ArrayDeque<>();
        distance[target.id] = 0;
        queue.add(target.id);
        while (!queue.isEmpty()) {
            int id = queue.poll();
            for (int n : adjacency.get(id).keySet()) {
                if (distance[n] >= 0) continue;
                distance[n] = distance[id] + 1;
                if (!visited[n] && rooms.get(n).type == RoomType.REGULAR) queue.add(n);
            }
        }
        return distance;
    }

    private void buildAdjacency() {
        for (int i = 0; i < rooms.size(); i++) adjacency.add(new LinkedHashMap<>());
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Position p = new Position(x, y);
                for (Direction d : new Direction[]{Direction.EAST, Direction.SOUTH}) {
                    Position n = p.offset(d);
                    if (n.x() >= width || n.y() >= height) continue;
                    int a = grid[x][y];
                    int b = grid[n.x()][n.y()];
                    if (a == b || a < 0 || b < 0) continue;
                    Edge edge = new Edge(p, n);
                    adjacency.get(a).computeIfAbsent(b, k -> new ArrayList<>()).add(edge);
                    adjacency.get(b).computeIfAbsent(a, k -> new ArrayList<>()).add(edge);
                }
            }
        }
    }
}
