package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.DungeonLayout;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.dungeons.paste.PastePlan;

/**
 * A run's floor in world coordinates: which cell and room a block is in, where each door is, and
 * which room holds the key for each door. Cells are {@link PastePlan#CELL} blocks with a one block
 * gap, starting at {@link PastePlan#HYPIXEL_BASE} on both axes; doors fill the gap between two
 * cells, three wide and four high.
 */
final class RunLayout {
    static final int DOOR_BOTTOM = 69;
    static final int DOOR_TOP = 72;
    /** From a cell's corner to its middle. */
    private static final int HALF = PastePlan.CELL / 2;

    final DungeonLayout layout;
    private final int base = PastePlan.HYPIXEL_BASE;

    RunLayout(DungeonLayout layout) {
        this.layout = layout;
    }

    List<PlacedRoom> rooms() {
        return layout.getRooms();
    }

    List<Door> doors() {
        return layout.getDoors();
    }

    PlacedRoom room(int id) {
        return layout.getRooms().get(id);
    }

    /** The cell a block column is in, or null in a gap between cells or outside the floor. */
    Position cellAt(double x, double z) {
        int bx = (int) Math.floor(x) - base;
        int bz = (int) Math.floor(z) - base;
        if (bx < 0 || bz < 0) return null;
        int cx = bx / PastePlan.PITCH;
        int cz = bz / PastePlan.PITCH;
        if (bx % PastePlan.PITCH == PastePlan.CELL || bz % PastePlan.PITCH == PastePlan.CELL) return null;
        if (cx >= layout.getWidth() || cz >= layout.getHeight()) return null;
        return new Position(cx, cz);
    }

    /** The room a player standing here is in (not counting doorways). */
    PlacedRoom roomAt(Location at) {
        Position cell = cellAt(at.getX(), at.getZ());
        return cell == null ? null : layout.roomAt(cell);
    }

    /** Middle of a cell, at the doorways' floor. */
    Location center(World world, Position cell) {
        return new Location(world, base + PastePlan.PITCH * cell.x() + HALF + 0.5, DOOR_BOTTOM,
                base + PastePlan.PITCH * cell.y() + HALF + 0.5);
    }

    /** The door's 3x4x3 blocks: across the gap and one block into each room, three wide. */
    List<int[]> doorBlocks(Door door) {
        Position a = door.edge().a();
        Direction side = door.edge().sideOf(a);
        int gx = base + PastePlan.PITCH * a.x() + HALF + (HALF + 1) * side.dx;
        int gz = base + PastePlan.PITCH * a.y() + HALF + (HALF + 1) * side.dy;
        int acrossX = Math.abs(side.dy);
        int acrossZ = Math.abs(side.dx);
        List<int[]> out = new ArrayList<>();
        for (int along = -1; along <= 1; along++) {
            for (int across = -1; across <= 1; across++) {
                for (int y = DOOR_BOTTOM; y <= DOOR_TOP; y++) {
                    out.add(new int[]{gx + along * side.dx + across * acrossX, y, gz + along * side.dy + across * acrossZ});
                }
            }
        }
        return out;
    }

    /** The door whose blocks include this one, if any. */
    Door doorAt(int x, int y, int z) {
        if (y < DOOR_BOTTOM || y > DOOR_TOP) return null;
        for (Door door : layout.getDoors()) {
            for (int[] b : doorBlocks(door)) {
                if (b[0] == x && b[1] == y && b[2] == z) return door;
            }
        }
        return null;
    }

    /**
     * Where the key for a wither or blood door waits: the room it's entered from, or the one before
     * that if it's the fairy room (which has nothing to kill, so no key comes from it).
     */
    PlacedRoom keyRoom(Door door) {
        PlacedRoom room = room(door.parent());
        while (room.type() == RoomType.FAIRY && room.parent() >= 0) room = room(room.parent());
        return room;
    }

    List<Door> doorsOfType(DoorType type) {
        return layout.getDoors().stream().filter(d -> d.type() == type).toList();
    }

    PlacedRoom bloodRoom() {
        List<PlacedRoom> blood = layout.roomsOfType(RoomType.BLOOD);
        return blood.isEmpty() ? null : blood.get(0);
    }

    /** The first of a room's cells in reading order (topmost, then leftmost), where the map puts its checkmark. */
    static Position firstCell(PlacedRoom room) {
        Position first = null;
        for (Position c : room.cells()) {
            if (first == null || c.y() < first.y() || (c.y() == first.y() && c.x() < first.x())) first = c;
        }
        return first;
    }

    /** Whether a point is inside a room's cells (any height). */
    boolean inside(PlacedRoom room, Location at) {
        Position cell = cellAt(at.getX(), at.getZ());
        return cell != null && room.cells().contains(cell);
    }
}
