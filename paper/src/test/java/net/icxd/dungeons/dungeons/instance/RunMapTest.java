package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.DungeonConfig;
import net.icxd.dungeons.dungeons.generation.DungeonGenerator;
import net.icxd.dungeons.dungeons.generation.DungeonLayout;
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
            assertEquals(0, map.foundCells());
            assertEquals(0, map.completedCells());

            PlacedRoom done = layout.getRooms().stream()
                    .filter(r -> r.type() != RoomType.START && r.cells().size() > 1)
                    .filter(r -> layout.getDoors().stream().noneMatch(d -> d.type() == DoorType.WITHER
                            && (d.parent() == r.id() || d.child() == r.id())))
                    .findFirst().orElse(null);
            if (done == null) continue;
            map.complete(done);
            assertEquals(done.cells().size(), map.completedCells(), "seed " + seed);
            assertEquals(done.cells().size(), map.foundCells(), "seed " + seed);
            assertEquals(1, map.completedRooms());
        }
    }
}
