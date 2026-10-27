package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Axis;
import org.junit.jupiter.api.Test;

class BloodPortalTest {
    /**
     * The captured Blood Rooms' frames: 14 obsidian round a 3 wide, 4 high opening (corners open). The recorded portal
     * of 2026-10-27 filled x -90 to -88, y 69 to 72, at z -199.
     */
    static List<int[]> frame(int x0, int y0, int z0, boolean alongX) {
        List<int[]> out = new ArrayList<>();
        for (int a = 0; a < 5; a++) {
            for (int y = 0; y < 6; y++) {
                boolean edge = a == 0 || a == 4 || y == 0 || y == 5;
                boolean corner = (a == 0 || a == 4) && (y == 0 || y == 5);
                if (!edge || corner) continue;
                out.add(alongX ? new int[]{x0 + a, y0 + y, z0} : new int[]{x0, y0 + y, z0 + a});
            }
        }
        return out;
    }

    @Test
    void findsTheRecordedOpening() {
        List<int[]> obsidian = frame(-91, 68, -199, true);
        assertEquals(14, obsidian.size());
        BloodPortal.Frame frame = BloodPortal.find(obsidian);
        assertEquals(Axis.X, frame.axis());
        assertEquals(12, frame.opening().size());
        for (int[] b : frame.opening()) {
            assertTrue(b[0] >= -90 && b[0] <= -88 && b[1] >= 69 && b[1] <= 72 && b[2] == -199, b[0] + " " + b[1] + " " + b[2]);
        }
    }

    @Test
    void alongZ() {
        BloodPortal.Frame frame = BloodPortal.find(frame(-150, 68, -170, false));
        assertEquals(Axis.Z, frame.axis());
        assertEquals(12, frame.opening().size());
    }

    @Test
    void notAFrame() {
        assertNull(BloodPortal.find(List.of()));
        List<int[]> broken = frame(-91, 68, -199, true);
        // Its bottom's middle block gone.
        broken.removeIf(b -> b[0] == -89 && b[1] == 68);
        assertNull(BloodPortal.find(broken));
        // Obsidian all over the place, not in one wall.
        List<int[]> scattered = new ArrayList<>(frame(-91, 68, -199, true));
        scattered.add(new int[]{-80, 70, -190});
        assertNull(BloodPortal.find(scattered));
    }
}
