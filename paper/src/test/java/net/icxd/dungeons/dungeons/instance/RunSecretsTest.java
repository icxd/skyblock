package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.generation.utils.Direction;

/** What RunSecrets works out without a world (research secrets_puzzles.md 1.4). */
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
}
