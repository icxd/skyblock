package net.icxd.dungeons.dungeons.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.room.HypixelRooms;
import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;

class HypixelRoomsPuzzlesTest {
    private static Set<String> puzzles(DungeonFloor floor) {
        return HypixelRooms.pool().templates(RoomType.PUZZLE, RoomShape.ONE_BY_ONE, floor).stream().map(Room::getId).collect(Collectors.toSet());
    }

    /** The Entrance's puzzles, less the Teleport Maze (never captured, so it can't be pasted). */
    @Test
    void entrancePuzzles() {
        assertEquals(Set.of("creeper_beams", "three_weirdos", "tic_tac_toe", "water_board"), puzzles(DungeonFloor.ENTRANCE));
        assertTrue(puzzles(DungeonFloor.FLOOR_1).contains("teleport_maze"));
    }
}
