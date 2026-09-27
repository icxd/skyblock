package net.icxd.dungeons.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonEntry;

/** The proxy's Combat XV for entering dungeons is the skill table's. */
class DungeonEntryCombatTest {
    @Test
    void sameAsTheSkillTable() {
        assertEquals(Skill.COMBAT.cumulative(DungeonEntry.COMBAT_LEVEL), DungeonEntry.COMBAT_XP);
        assertEquals(DungeonEntry.COMBAT_LEVEL, Skill.COMBAT.level(DungeonEntry.COMBAT_XP));
        assertEquals(DungeonEntry.COMBAT_LEVEL - 1, Skill.COMBAT.level(DungeonEntry.COMBAT_XP - 0.1));
    }
}
