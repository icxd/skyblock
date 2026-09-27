package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

class WaterGatesTest {
    @Test
    void threeOfFiveStartClosed() {
        Set<String> seen = new HashSet<>();
        for (long seed = 0; seed < 400; seed++) {
            boolean[] closed = WaterGates.closedAtStart(5, new Random(seed));
            StringBuilder key = new StringBuilder();
            int n = 0;
            for (int i = 0; i < 5; i++) {
                if (closed[i]) {
                    n++;
                    key.append(i);
                }
            }
            assertEquals(3, n);
            seen.add(key.toString());
        }
        assertEquals(10, seen.size(), "every way of closing three: " + seen);
    }

    @Test
    void aGateTogglesWhenWaterGetsToItsHole() {
        WaterGates gates = new WaterGates(new boolean[]{true, false, true, false, true});
        assertEquals(List.of(), gates.flow(new boolean[5]));
        assertEquals(List.of(4), gates.flow(new boolean[]{false, false, false, false, true}));
        assertFalse(gates.isClosed(4));
        // Still running into it: nothing more.
        assertEquals(List.of(), gates.flow(new boolean[]{false, false, false, false, true}));
        // Into an open gate's hole: it closes.
        assertEquals(List.of(1), gates.flow(new boolean[]{false, true, false, false, true}));
        assertTrue(gates.isClosed(1));
        // The water goes and comes back: it opens again.
        gates.flow(new boolean[]{false, false, false, false, false});
        assertEquals(List.of(1, 4), gates.flow(new boolean[]{false, true, false, false, true}));
        assertFalse(gates.isClosed(1));
        assertTrue(gates.isClosed(4));
        assertFalse(gates.allOpen());
        gates.flow(new boolean[5]);
        gates.flow(new boolean[]{true, false, true, false, true});
        assertTrue(gates.allOpen());
    }

    /** A made-up board 3 wide: blocks on it at z 0, out of it at z 1, pistons at z 2. */
    private static final class FakeBoard implements WaterGates.Board {
        final Map<String, Material> blocks = new HashMap<>();
        final Map<String, Boolean> pistons = new HashMap<>();

        void piston(int x, int y, boolean extended, Material moves) {
            pistons.put(x + "," + y, extended);
            blocks.put(x + "," + y + "," + (extended ? 0 : 1), moves);
        }

        @Override
        public Material type(int x, int y, int z) {
            return blocks.getOrDefault(x + "," + y + "," + z, Material.AIR);
        }

        @Override
        public boolean isPiston(int x, int y, int z) {
            return z == 2 && pistons.containsKey(x + "," + y);
        }

        @Override
        public boolean isExtended(int x, int y, int z) {
            return pistons.getOrDefault(x + "," + y, false);
        }
    }

    private static PuzzleData.WaterBoard data() {
        Map<Material, int[]> levers = new LinkedHashMap<>();
        levers.put(Material.EMERALD_BLOCK, new int[]{0, 0, 0});
        levers.put(Material.QUARTZ_BLOCK, new int[]{0, 0, 0});
        return new PuzzleData.WaterBoard(levers, new int[3], new int[3], new int[]{0, 0}, new int[]{2, 2}, 0, List.of(), List.of(),
                new int[2], new int[3], BlockFace.SOUTH,
                List.of(new PuzzleData.Variant(new int[]{0, 0, 1}, Material.TERRACOTTA), new PuzzleData.Variant(new int[]{1, 1, 1}, Material.QUARTZ_BLOCK)));
    }

    @Test
    void leversMoveTheirMaterial() {
        FakeBoard board = new FakeBoard();
        board.piston(0, 0, true, Material.EMERALD_BLOCK);
        board.piston(2, 1, false, Material.EMERALD_BLOCK);
        board.piston(1, 1, false, Material.QUARTZ_BLOCK);
        board.piston(1, 2, true, Material.GOLD_BLOCK);
        Map<Material, List<WaterGates.Piston>> pistons = WaterGates.pistons(data(), board);
        assertEquals(List.of(new WaterGates.Piston(0, 0, true), new WaterGates.Piston(2, 1, false)), pistons.get(Material.EMERALD_BLOCK));
        assertEquals(List.of(new WaterGates.Piston(1, 1, false)), pistons.get(Material.QUARTZ_BLOCK));
        assertEquals(2, pistons.size(), "gold has no lever here");
        assertEquals(1, WaterGates.variant(data(), board));
    }
}
