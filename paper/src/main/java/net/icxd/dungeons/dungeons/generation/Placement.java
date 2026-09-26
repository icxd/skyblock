package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.icxd.dungeons.dungeons.generation.room.DoorSlot;
import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * One concrete way to put a template into a footprint: the template, how far it is rotated, and
 * which of the footprint's walls (in world grid space) may or must have a door.
 */
public record Placement(Room template, int rotation, Set<Edge> allowedDoors, Set<Edge> requiredDoors) {

    public boolean allows(Edge edge) {
        return allowedDoors.contains(edge);
    }

    public int doorLimit() {
        return template.doorLimit();
    }

    /** Whether this placement is happy with exactly these doors. */
    public boolean fits(Collection<Edge> doors) {
        return doors.size() <= doorLimit() && allowedDoors.containsAll(doors) && doors.containsAll(requiredDoors);
    }

    /**
     * Every rotation of {@code template} that exactly covers {@code footprint}. Rotations that would
     * put a required door on the map border are left out.
     */
    public static List<Placement> enumerate(Room template, List<Position> footprint, int width, int height) {
        return enumerate(template, footprint, width, height, p -> false);
    }

    /** Same, on a grid where {@link DungeonLayout#EMPTY} cells have no room (a door can't lead there). */
    public static List<Placement> enumerate(Room template, List<Position> footprint, int[][] grid) {
        return enumerate(template, footprint, grid.length, grid[0].length, p -> grid[p.x()][p.y()] == DungeonLayout.EMPTY);
    }

    private static List<Placement> enumerate(Room template, List<Position> footprint, int width, int height,
                                                                                     java.util.function.Predicate<Position> noRoom) {
        List<Placement> out = new ArrayList<>(4);
        RoomShape shape = template.getShape();
        Position anchor = RoomShape.minCorner(footprint);
        Set<Position> cells = new HashSet<>(footprint);
        rotations:
        for (int rotation : shape.rotationsMatching(footprint)) {
            if (!template.getRotations().contains(rotation)) continue;
            Set<Edge> allowed = new HashSet<>();
            if (template.getDoorSlots().isEmpty()) {
                for (Position c : footprint) {
                    for (Direction d : Direction.values()) {
                        Edge e = external(cells, c, d, width, height, noRoom);
                        if (e != null) allowed.add(e);
                    }
                }
            } else {
                Position offset = shape.normalizeOffset(rotation);
                for (DoorSlot slot : template.getDoorSlots()) {
                    Position c = anchor.plus(slot.cell().rotateClockwise(rotation).minus(offset));
                    Edge e = external(cells, c, slot.side().rotateClockwise(rotation), width, height, noRoom);
                    if (e != null) allowed.add(e);
                    else if (template.isExactDoors()) continue rotations;
                }
            }
            Set<Edge> required = template.isExactDoors() ? allowed : Set.of();
            out.add(new Placement(template, rotation, Set.copyOf(allowed), Set.copyOf(required)));
        }
        return out;
    }

    private static Edge external(Set<Position> cells, Position c, Direction d, int width, int height,
                                                             java.util.function.Predicate<Position> noRoom) {
        Position n = c.offset(d);
        if (cells.contains(n)) return null; // interior wall of a multi-cell room, never a door
        if (n.x() < 0 || n.y() < 0 || n.x() >= width || n.y() >= height) return null; // map border
        if (noRoom.test(n)) return null; // empty special-column cell
        return Edge.of(c, d);
    }
}
