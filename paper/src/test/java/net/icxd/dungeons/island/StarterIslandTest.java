package net.icxd.dungeons.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class StarterIslandTest {
    private static List<String> under(int x, int y, int z, int dz) {
        return StarterIsland.bridgeUnder(x, y, z, dz).stream().map(b -> b[0] + "," + b[1] + "," + b[2]).toList();
    }

    /** As recorded: walking south off the island's edge (z 16) at x 7, a plank under them and the next. */
    @Test
    void theBridgeIsLaidUnderAPlayerWalkingAlongIt() {
        assertEquals(List.of("7,99,16", "7,99,17"), under(7, 100, 16, 1));
        assertEquals(List.of("7,99,26", "7,99,27"), under(7, 100, 26, 1));
        // Walking back north from the rock.
        assertEquals(List.of("7,99,30", "7,99,29"), under(7, 100, 30, -1));
        // Its ends: the island's edge and the rock are there already.
        assertEquals(List.of("7,99,16"), under(7, 100, 15, 1));
        assertEquals(List.of("7,99,30"), under(7, 100, 30, 1));
        // A player that wide over the line still stands on it.
        assertEquals(List.of("7,99,20", "7,99,21"), under(8, 100, 20, 1));
    }

    @Test
    void nowhereElse() {
        assertTrue(under(3, 100, 20, 1).isEmpty(), "off the line");
        assertTrue(under(7, 90, 20, 1).isEmpty(), "fallen");
        assertTrue(under(7, 100, 10, 1).isEmpty(), "on the island");
        assertTrue(under(7, 100, 35, 1).isEmpty(), "on the rock");
    }
}
