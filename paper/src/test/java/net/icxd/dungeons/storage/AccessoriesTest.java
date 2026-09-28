package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;

/** Which accessories count, and their Accessory Power (the wiki's Accessories and Accessory Power). */
class AccessoriesTest {
    /** A line of three, and two that "upgrade" into each other (NEU's Abicases). */
    private static final StorageTables TABLES = PrivateTables.madeUp(Map.of(
            "LUCK_TALISMAN", List.of("LUCK_RING", "LUCK_ARTIFACT"),
            "LUCK_RING", List.of("LUCK_ARTIFACT"),
            "CASE_RED", List.of("CASE_BLUE"),
            "CASE_BLUE", List.of("CASE_RED")));

    private static Accessories.Held held(String id, Rarity rarity) {
        return new Accessories.Held(id, rarity, false);
    }

    @Test
    void lines() {
        assertEquals(TABLES.line("LUCK_TALISMAN"), TABLES.line("LUCK_ARTIFACT"));
        assertEquals(TABLES.line("LUCK_RING"), TABLES.line("LUCK_ARTIFACT"));
        assertEquals(TABLES.line("CASE_RED"), TABLES.line("CASE_BLUE"));
        assertNotEquals(TABLES.line("CASE_RED"), TABLES.line("LUCK_RING"));
        // In no line: its own.
        assertEquals("LONE_ORB", TABLES.line("LONE_ORB"));
    }

    @Test
    void onlyTheBestOfALine() {
        List<Accessories.Held> held = List.of(held("LUCK_ARTIFACT", Rarity.EPIC), held("LUCK_TALISMAN", Rarity.MYTHIC),
                held("LUCK_RING", Rarity.RARE), held("LONE_ORB", Rarity.COMMON));
        // The artifact, though the talisman (recombobulated a lot) gives more Accessory Power.
        assertEquals(List.of(0, 3), Accessories.counted(held, TABLES));
        // Whatever the order.
        List<Accessories.Held> reversed = List.of(held.get(3), held.get(2), held.get(1), held.get(0));
        assertEquals(List.of(0, 3), Accessories.counted(reversed, TABLES));
    }

    @Test
    void duplicates() {
        // Two of the same: the recombobulated one (a rarity up) counts, else the first.
        List<Accessories.Held> held = List.of(held("LONE_ORB", Rarity.COMMON), held("LONE_ORB", Rarity.UNCOMMON), held("LONE_ORB", Rarity.COMMON));
        assertEquals(List.of(1), Accessories.counted(held, TABLES));
        assertEquals(List.of(0), Accessories.counted(List.of(held("LONE_ORB", Rarity.RARE), held("LONE_ORB", Rarity.RARE)), TABLES));
    }

    @Test
    void cycle() {
        // Each "upgrades" into the other: the one that gives more Accessory Power.
        List<Accessories.Held> held = List.of(held("CASE_RED", Rarity.UNCOMMON), held("CASE_BLUE", Rarity.EPIC));
        assertEquals(List.of(1), Accessories.counted(held, TABLES));
    }

    @Test
    void accessoryPower() {
        List<Accessories.Held> held = List.of(held("LUCK_ARTIFACT", Rarity.EPIC), held("LUCK_TALISMAN", Rarity.MYTHIC),
                new Accessories.Held("DUNGEON_ORB", Rarity.RARE, true), held(Accessories.HEGEMONY, Rarity.LEGENDARY));
        List<Integer> counted = Accessories.counted(held, TABLES);
        // 4 + 3 + twice 5; the talisman doesn't count.
        assertEquals(17, Accessories.power(held, counted, false, TABLES));
        // In a dungeon a dungeon accessory's is twice as much.
        assertEquals(20, Accessories.power(held, counted, true, TABLES));
        assertEquals(0, Accessories.power(List.of(), List.of(), false, TABLES));
    }

    @Test
    void tuningPoints() {
        assertEquals(57, Accessories.tuningPoints(571));
        assertEquals(0, Accessories.tuningPoints(9));
    }

    @Test
    void recordedLines() {
        StorageTables tables = PrivateTables.load();
        assertEquals(tables.line("SCAVENGER_TALISMAN"), tables.line("SCAVENGER_ARTIFACT"));
        List<Accessories.Held> held = List.of(held("SCAVENGER_TALISMAN", Rarity.COMMON), held("SCAVENGER_ARTIFACT", Rarity.RARE),
                held("SCAVENGER_RING", Rarity.UNCOMMON));
        assertEquals(List.of(1), Accessories.counted(held, tables));
        // The artifact's alone: a Rare's.
        assertEquals(tables.accessoryPower(Rarity.RARE), Accessories.power(held, Accessories.counted(held, tables), false, tables));
    }
}
