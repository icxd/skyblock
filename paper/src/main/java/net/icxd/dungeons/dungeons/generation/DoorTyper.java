package net.icxd.dungeons.dungeons.generation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Edge;

/** Stage 5: door types along the critical path. */
final class DoorTyper {
    private DoorTyper() {
    }

    static Map<Edge, DoorType> type(List<RoomNode> rooms, List<Integer> path) {
        Map<Edge, DoorType> types = new HashMap<>();
        for (int i = 1; i < path.size(); i++) {
            RoomNode room = rooms.get(path.get(i));
            Edge entry = room.doors.get(0); // the door a room was entered through is always its first
            if (room.type == RoomType.BLOOD) types.put(entry, DoorType.BLOOD);
            else if (i == 1) types.put(entry, DoorType.ENTRANCE);
            else if (room.type == RoomType.FAIRY) types.put(entry, DoorType.FAIRY);
            else types.put(entry, DoorType.WITHER);
        }
        return types;
    }
}
