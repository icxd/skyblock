package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.DungeonConfig;
import net.icxd.dungeons.dungeons.generation.DungeonGenerator;
import net.icxd.dungeons.dungeons.generation.DungeonLayout;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.HypixelRooms;
import net.icxd.dungeons.dungeons.generation.room.RoomType;

/** Rooms counted as Hypixel counts them for the score: cells, without the Entrance room's (research score_rewards.md 1.3). */
class RunMapTest {
    @Test
    void cellsWithoutTheEntranceRoom() {
        DungeonGenerator generator = new DungeonGenerator(DungeonConfig.forFloor(DungeonFloor.ENTRANCE), HypixelRooms.pool());
        for (long seed = 0; seed < 50; seed++) {
            DungeonLayout layout = generator.generate(seed);
            // No run: nothing here draws a wither door, the only thing that asks the run.
            RunMap map = new RunMap(null, new RunLayout(layout));
            int cells = layout.getRooms().stream().mapToInt(r -> r.cells().size()).sum();
            PlacedRoom start = layout.roomsOfType(RoomType.START).get(0);
            assertEquals(cells - start.cells().size(), map.totalCells(), "seed " + seed);

            map.find(start);
            assertEquals(0, map.openedCells(door -> true));
            assertEquals(0, map.completedCells());

            PlacedRoom done = layout.getRooms().stream()
                    .filter(r -> r.type() != RoomType.START && r.cells().size() > 1)
                    .filter(r -> layout.getDoors().stream().noneMatch(d -> d.type() == DoorType.WITHER
                            && (d.parent() == r.id() || d.child() == r.id())))
                    .findFirst().orElse(null);
            if (done == null) continue;
            map.complete(done);
            assertEquals(done.cells().size(), map.completedCells(), "seed " + seed);
            assertEquals(done.cells().size(), map.openedCells(door -> true), "seed " + seed);
            assertEquals(1, map.completedRooms());
        }
    }

    /**
     * "Opened Rooms": a room behind a door that starts shut counts once that door opens, any other
     * once someone walks in (R2: 3 as the entrance door opens, +1 at each wither door and the Blood
     * Door, the fairy room when walked into; research0/srscore).
     */
    @Test
    void openedByTheirDoorOrWalkedInto() {
        DungeonGenerator generator = new DungeonGenerator(DungeonConfig.forFloor(DungeonFloor.ENTRANCE), HypixelRooms.pool());
        int checked = 0;
        for (long seed = 0; seed < 50; seed++) {
            DungeonLayout layout = generator.generate(seed);
            RunLayout runLayout = new RunLayout(layout);
            RunMap map = new RunMap(null, runLayout);
            map.find(layout.roomsOfType(RoomType.START).get(0));
            Set<Door> shut = new HashSet<>();
            for (Door door : layout.getDoors()) {
                if (RunDoors.startsShut(door.type())) shut.add(door);
            }
            // Every door shut: only the Entrance room is in, and it doesn't count.
            assertEquals(0, map.openedCells(shut::contains), "seed " + seed);

            Door entranceDoor = runLayout.doorsOfType(DoorType.ENTRANCE).get(0);
            shut.remove(entranceDoor);
            int opened = runLayout.room(entranceDoor.child()).cells().size();
            assertEquals(opened, map.openedCells(shut::contains), "seed " + seed);

            Door bloodDoor = runLayout.doorsOfType(DoorType.BLOOD).get(0);
            if (bloodDoor.child() == entranceDoor.child()) continue;
            shut.remove(bloodDoor);
            opened += runLayout.room(bloodDoor.child()).cells().size();
            assertEquals(opened, map.openedCells(shut::contains), "seed " + seed);

            // A room behind a normal door (and no wither door, which would ask the run how to draw it): only once walked into.
            Set<Integer> counted = Set.of(entranceDoor.child(), bloodDoor.child());
            PlacedRoom behindNormal = layout.getDoors().stream()
                    .filter(d -> d.type() == DoorType.NORMAL && !counted.contains(d.child()))
                    .map(d -> runLayout.room(d.child()))
                    .filter(r -> layout.getDoors().stream().noneMatch(d -> d.type() == DoorType.WITHER
                            && (d.parent() == r.id() || d.child() == r.id())))
                    .findFirst().orElse(null);
            if (behindNormal == null) continue;
            map.find(behindNormal);
            assertEquals(opened + behindNormal.cells().size(), map.openedCells(shut::contains), "seed " + seed);
            checked++;
        }
        assertTrue(checked > 10, "only " + checked + " layouts had a room behind a normal door");
    }
}
