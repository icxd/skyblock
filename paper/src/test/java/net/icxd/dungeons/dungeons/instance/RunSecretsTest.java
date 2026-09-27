package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.generation.utils.Direction;

/** What RunSecrets works out without a world (research secrets_puzzles.md 1.4, 1.7). */
class RunSecretsTest {
    /** A head turns with its room, a quarter turn (4 sixteenths) for each of the room's turns. */
    @Test
    void headsTurnWithTheRoom() {
        // R1's Pirate Wither Essence: 3 in the capture frame, 11 in a room turned twice.
        BlockFace[] essence = {BlockFace.WEST_SOUTH_WEST, BlockFace.NORTH_NORTH_WEST, BlockFace.EAST_NORTH_EAST, BlockFace.SOUTH_SOUTH_EAST};
        for (int turns = 0; turns < 4; turns++) assertEquals(essence[turns], RunSecrets.headFacing(3, turns), "turns " + turns);
        assertEquals(BlockFace.EAST_NORTH_EAST, RunSecrets.headFacing(11, 0));
        assertEquals(BlockFace.WEST_SOUTH_WEST, RunSecrets.headFacing(3, 4));
        assertEquals(BlockFace.SOUTH_SOUTH_EAST, RunSecrets.headFacing(-1, 0));
        // The same way chests turn.
        Direction[] quarters = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        for (int q = 0; q < 4; q++) {
            for (int turns = 0; turns < 4; turns++) {
                assertEquals(quarters[q].rotateClockwise(turns).name(), RunSecrets.headFacing(4 * q, turns).name(), q + " turned " + turns);
            }
        }
    }

    /** Under the lowest north-west corner of the wall, as both recorded walls' clicks were (R1 01:46.5, 03:52.0). */
    @Test
    void platesUnderTheWall() {
        assertEquals(new Vector(-83.5, 69.5, -191.5), RunSecrets.platesAt(wall(-84, -82, 70, 71, -192, -192)));
        assertEquals(new Vector(-126.5, 83.5, -179.5), RunSecrets.platesAt(wall(-127, -125, 84, 88, -180, -180)));
        assertEquals(new Vector(10.5, 59.5, -4.5), RunSecrets.platesAt(wall(10, 10, 60, 62, -5, -3)));
    }

    private static List<int[]> wall(int x0, int x1, int y0, int y1, int z0, int z1) {
        List<int[]> blocks = new ArrayList<>();
        // Highest first, as a capture lists them in no particular order.
        for (int y = y1; y >= y0; y--) {
            for (int x = x1; x >= x0; x--) {
                for (int z = z1; z >= z0; z--) blocks.add(new int[]{x, y, z});
            }
        }
        return blocks;
    }
}
