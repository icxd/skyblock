package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;

/** All templates the generator may use. */
public final class RoomPool {
    private final List<Room> templates;
    private final boolean uniqueRooms;

    /**
     * @param uniqueRooms never use the same template twice in one dungeon (Hypixel behaviour). This
     *                    also caps how many rooms of each shape the tiler makes to the number of
     *                    templates of that shape. 1x1 regular rooms fall back to repeats if the pool
     *                    runs out, since they're the filler.
     */
    public RoomPool(List<Room> templates, boolean uniqueRooms) {
        this.templates = List.copyOf(templates);
        this.uniqueRooms = uniqueRooms;
    }

    public List<Room> all() {
        return templates;
    }

    public List<Room> templates(RoomType type, RoomShape shape, DungeonFloor floor) {
        List<Room> out = new ArrayList<>();
        for (Room r : templates) {
            if (r.getType() == type && r.getShape() == shape && r.getMinimumFloor().getNumber() <= floor.getNumber()) out.add(r);
        }
        return out;
    }

    /** Max regular rooms per shape; shapes without any template are disabled (limit 0). */
    public Map<RoomShape, Integer> shapeLimits(DungeonFloor floor) {
        Map<RoomShape, Integer> limits = new EnumMap<>(RoomShape.class);
        for (RoomShape shape : RoomShape.values()) {
            if (shape == RoomShape.ONE_BY_ONE) continue;
            int count = templates(RoomType.REGULAR, shape, floor).size();
            if (count == 0 || uniqueRooms) limits.put(shape, count);
        }
        return limits;
    }

    public boolean canAvoidRepeats() {
        return uniqueRooms;
    }
}
