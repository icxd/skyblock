package net.icxd.dungeons.dungeons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DungeonLevelsTest {
    @Test
    void levelsFromExperience() {
        assertEquals(0, DungeonLevels.level(0));
        assertEquals(0, DungeonLevels.level(49.9));
        assertEquals(1, DungeonLevels.level(50));
        assertEquals(2, DungeonLevels.level(125));
        // Levels 1-50 take 569,809,640 in all; 51 takes 200,000,000 more, and so does each one after it.
        assertEquals(49, DungeonLevels.level(569_809_639));
        assertEquals(50, DungeonLevels.level(569_809_640));
        assertEquals(51, DungeonLevels.level(769_809_640));
        assertEquals(52, DungeonLevels.level(969_809_640));
    }

    @Test
    void romanNumerals() {
        assertEquals("0", DungeonLevels.roman(0));
        assertEquals("XX", DungeonLevels.roman(20));
        assertEquals("XIV", DungeonLevels.roman(14));
        assertEquals("XLIX", DungeonLevels.roman(49));
    }

    @Test
    void classesByName() {
        assertEquals(DungeonClass.BERSERK, DungeonClass.parse("berserk"));
        assertEquals(DungeonClass.HEALER, DungeonClass.parse(null));
        assertEquals(DungeonClass.HEALER, DungeonClass.parse("WIZARD"));
        assertEquals("B", DungeonClass.BERSERK.getLetter());
    }
}
