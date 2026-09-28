package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Putting on a stored set (the wiki's Wardrobe), with strings for the pieces ("" is an empty slot). */
class WardrobeTest {
    private static Wardrobe.Swap<String> swap(int worn, int target, List<String> wearing, List<String> set) {
        return Wardrobe.swap(worn, target, wearing, set, String::isEmpty);
    }

    @Test
    void fromNoSet() {
        // What they wore came from no set: into their inventory, and the set goes on.
        Wardrobe.Swap<String> swap = swap(-1, 2, Arrays.asList("hat", null, "", "boots"), List.of("helm", "chest", "legs", "shoes"));
        assertEquals(2, swap.worn());
        assertEquals(List.of("helm", "chest", "legs", "shoes"), swap.wearing());
        assertNull(swap.stored());
        assertEquals(List.of("hat", "boots"), swap.toInventory());
    }

    @Test
    void betweenSets() {
        // Back into the set it came from, as it is now (the boots were taken off, a cap put on).
        Wardrobe.Swap<String> swap = swap(0, 1, Arrays.asList("cap", "chest", "legs", null), Arrays.asList("helm", null, "legs2", "shoes"));
        assertEquals(1, swap.worn());
        assertEquals(Arrays.asList("helm", null, "legs2", "shoes"), swap.wearing());
        assertEquals(Arrays.asList("cap", "chest", "legs", null), swap.stored());
        assertEquals(List.of(), swap.toInventory());
    }

    @Test
    void sameSet() {
        Wardrobe.Swap<String> swap = swap(3, 3, List.of("a", "b", "c", "d"), List.of("", "", "", ""));
        assertEquals(3, swap.worn());
        assertEquals(List.of("a", "b", "c", "d"), swap.wearing());
        assertNull(swap.stored());
        assertEquals(List.of(), swap.toInventory());
    }

    @Test
    void nothingMadeOrLost() {
        List<String> wearing = Arrays.asList("a", "b", null, "d");
        List<String> set = Arrays.asList("e", null, "g", "h");
        for (int worn : new int[]{-1, 0}) {
            Wardrobe.Swap<String> swap = swap(worn, 1, wearing, set);
            List<String> after = new java.util.ArrayList<>(swap.wearing());
            if (swap.stored() != null) after.addAll(swap.stored());
            after.addAll(swap.toInventory());
            after.removeIf(p -> p == null || p.isEmpty());
            List<String> before = new java.util.ArrayList<>(wearing);
            before.addAll(set);
            before.removeIf(p -> p == null || p.isEmpty());
            after.sort(null);
            before.sort(null);
            assertEquals(before, after);
        }
    }
}
