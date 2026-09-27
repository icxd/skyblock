package net.icxd.dungeons.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The standard skill table (research skills.md 3.3): cumulative XP for levels 1, 25, 50 and 60. */
class SkillsTest {
    @Test
    void levels() {
        assertEquals(0, Skills.level(0));
        assertEquals(0, Skills.level(49.9));
        assertEquals(1, Skills.level(50));
        assertEquals(2, Skills.level(175));
        assertEquals(24, Skills.level(3_022_424.9));
        assertEquals(25, Skills.level(3_022_425));
        assertEquals(50, Skills.level(55_172_425));
        assertEquals(60, Skills.level(111_672_425));
        assertEquals(60, Skills.level(1e12));
    }
}
