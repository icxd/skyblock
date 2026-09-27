package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.bukkit.block.structure.StructureRotation;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.dungeons.paste.PastePlan;
import net.icxd.dungeons.dungeons.paste.RoomCapture;

/**
 * The capture frame in the world, checked against Hypixel's own rooms: each pair is a mob's spot in the
 * capture frame and where the recording had it (research0/mob_spawns_recorded.json, with the room's min
 * corner and turns).
 */
class RoomFrameTest {
    private static final double EPSILON = 1e-9;

    private static void assertPoint(RoomFrame frame, double[] capture, double[] world) {
        assertArrayEquals(world, frame.point(capture[0], capture[1], capture[2]), EPSILON);
    }

    @Test
    void recordedMobsLandWhereTheyWere() {
        // Crypt (1x2, 63 x 31, turned once), Altar (L, 63 x 63, twice), Tomioka (1x1, three times), Long Hall (not turned).
        assertPoint(new RoomFrame(-200, -168, 63, 31, 1), new double[]{52.5, 69, 15.5}, new double[]{-184.5, 69, -115.5});
        assertPoint(new RoomFrame(-136, -136, 63, 63, 2), new double[]{29.5, 44, 39.5}, new double[]{-102.5, 44, -112.5});
        assertPoint(new RoomFrame(-168, -200, 31, 31, 3), new double[]{19.5, 69, 12.5}, new double[]{-155.5, 69, -188.5});
        assertPoint(new RoomFrame(-104, -168, 95, 31, 1), new double[]{69.5, 53, 19.5}, new double[]{-92.5, 53, -98.5});
        assertPoint(new RoomFrame(-168, -136, 63, 63, 2), new double[]{27.5, 60, 5.5}, new double[]{-132.5, 60, -78.5});
        assertPoint(new RoomFrame(-136, -200, 31, 31, 0), new double[]{6.5, 76, 4.5}, new double[]{-129.5, 76, -195.5});
    }

    /** The recorded Crypt tomb: capture (31..34, 67..68, 14..16) was blown up at world (-186..-184, 67..68, -137..-134). */
    @Test
    void blocksTurnAsThePasteDoes() {
        RoomFrame crypt = new RoomFrame(-200, -168, 63, 31, 1);
        assertEquals(new PastePlan.Block(-184, 67, -137), crypt.block(31, 67, 14));
        assertEquals(new PastePlan.Block(-186, 68, -134), crypt.block(34, 68, 16));
        // A block's middle is the block turned, plus a half.
        double[] middle = crypt.point(31.5, 67, 14.5);
        assertArrayEquals(new double[]{-183.5, 67, -136.5}, middle, EPSILON);
        // The corners of a point's block swap round with it.
        assertArrayEquals(new double[]{-184, 67, -137}, crypt.point(31, 67, 15), EPSILON);
    }

    @Test
    void fromThePaste() {
        RoomCapture capture = new RoomCapture("crypt", "crypt", RoomType.REGULAR, RoomShape.ONE_BY_TWO, 0, 56, new int[]{63, 44, 31},
                List.of(new Position(0, 0), new Position(1, 0)), Map.of(), Path.of("crypt.schem"));
        // Turned once, it's 31 wide and 63 long, its middle column at the min corner + (15, 31).
        PastePlan.RoomPaste paste = new PastePlan.RoomPaste(null, capture, 1, new PastePlan.Block(-185, 56, -137), List.of());
        assertEquals(new RoomFrame(-200, -168, 63, 31, 1), RoomFrame.of(paste));
    }

    @Test
    void yawsAndBlockStatesTurnToo() {
        RoomFrame once = new RoomFrame(0, 0, 31, 31, 1);
        // South (0) turned clockwise faces west (90).
        assertEquals(90, once.yaw(0), EPSILON);
        assertEquals(0, new RoomFrame(0, 0, 31, 31, 3).yaw(90), EPSILON);
        assertEquals(StructureRotation.CLOCKWISE_90, once.rotation());
        assertEquals(StructureRotation.NONE, new RoomFrame(0, 0, 31, 31, 4).rotation());
    }
}
