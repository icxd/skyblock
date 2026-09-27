package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

class PuzzleFrameTest {
    @Test
    void blocksTurnLikeThePaste() {
        // A 1x1 room at (-104, -200).
        assertArrayEquals(new int[]{-104 + 3, 70, -200 + 5}, new PuzzleFrame(-104, -200, 31, 31, 0).block(3, 70, 5));
        // A quarter turn: x' = 30 - z, z' = x.
        assertArrayEquals(new int[]{-104 + 25, 70, -200 + 3}, new PuzzleFrame(-104, -200, 31, 31, 1).block(3, 70, 5));
        // Half: the opposite corner.
        assertArrayEquals(new int[]{-104 + 27, 70, -200 + 25}, new PuzzleFrame(-104, -200, 31, 31, 2).block(3, 70, 5));
        assertArrayEquals(new int[]{-104 + 5, 70, -200 + 27}, new PuzzleFrame(-104, -200, 31, 31, 3).block(3, 70, 5));
        // A 1x2 turned upright: 63 long in z afterwards.
        assertArrayEquals(new int[]{30, 0, 62}, new PuzzleFrame(0, 0, 63, 31, 1).block(62, 0, 0));
    }

    /** A block's middle turns with the block. */
    @Test
    void pointsTurnWithTheirBlocks() {
        for (int turns = 0; turns < 4; turns++) {
            PuzzleFrame frame = new PuzzleFrame(-40, 8, 63, 31, turns);
            int[] block = frame.block(10, 69, 4);
            double[] middle = frame.point(10.5, 69, 4.5);
            assertArrayEquals(new double[]{block[0] + 0.5, 69, block[2] + 0.5}, middle, 1e-9, "turns " + turns);
        }
    }

    @Test
    void facesAndYaws() {
        PuzzleFrame frame = new PuzzleFrame(0, 0, 31, 31, 1);
        assertEquals(BlockFace.SOUTH, frame.face(BlockFace.EAST));
        assertEquals(BlockFace.EAST, frame.face(BlockFace.NORTH));
        assertEquals(BlockFace.UP, frame.face(BlockFace.UP));
        assertEquals(BlockFace.WEST, new PuzzleFrame(0, 0, 31, 31, 2).face(BlockFace.EAST));
        assertEquals(BlockFace.NORTH, new PuzzleFrame(0, 0, 31, 31, 3).face(BlockFace.EAST));
        // North (180) a quarter turn clockwise is east (270).
        assertEquals(270f, frame.yaw(180f));
    }

    @Test
    void footprint() {
        PuzzleFrame upright = new PuzzleFrame(100, 200, 63, 31, 1);
        assertTrue(upright.contains(100, 200));
        assertTrue(upright.contains(130.9, 262.9));
        assertFalse(upright.contains(131, 210));
        assertFalse(upright.contains(110, 263));
        assertFalse(upright.contains(99.9, 210));
    }
}
