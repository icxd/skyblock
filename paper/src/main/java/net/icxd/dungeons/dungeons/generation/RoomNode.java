package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.List;

import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/**
 * Working state for one room while a dungeon is being generated.
 *
 * <p>{@link #options} is the heart of the room-placement problem: instead of picking a template
 * up front and hoping its door rules fit, every room keeps <em>all</em> (template, rotation) pairs
 * that are still consistent with the doors given to it so far. A door is only ever added through
 * a wall that at least one remaining option allows, and adding it drops the options that don't.
 */
final class RoomNode {
    int id;
    RoomType type;
    RoomShape shape;
    List<Position> cells;

    /** Every placement of every template for this footprint, before any door was decided. */
    List<Placement> allOptions;
    List<Placement> options;
    final List<Edge> doors = new ArrayList<>();
    int parent = -1;
    int depth;

    RoomNode(int id, RoomType type, RoomShape shape, List<Position> cells) {
        this.id = id;
        this.type = type;
        this.shape = shape;
        this.cells = List.copyOf(cells);
    }

    /** Could a door go through {@code edge} without leaving this room with no legal template? */
    boolean accepts(Edge edge) {
        for (Placement p : options) {
            if (p.doorLimit() > doors.size() && p.allows(edge)) return true;
        }
        return false;
    }

    boolean canTakeMoreDoors() {
        for (Placement p : options) {
            if (p.doorLimit() > doors.size()) return true;
        }
        return false;
    }

    /** Some option is complete with the doors this room has right now. */
    boolean satisfied() {
        for (Placement p : options) {
            if (p.fits(doors)) return true;
        }
        return false;
    }

    /** Some placement from {@link #allOptions} would be happy with exactly {@code doors}. */
    boolean couldHave(List<Edge> doors) {
        for (Placement p : allOptions) {
            if (p.fits(doors)) return true;
        }
        return false;
    }

    List<Placement> optionsAfter(Edge edge) {
        List<Placement> next = new ArrayList<>(options.size());
        for (Placement p : options) {
            if (p.doorLimit() > doors.size() && p.allows(edge)) next.add(p);
        }
        return next;
    }
}
