package net.icxd.dungeons.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DungeonEntryTest {
    @Test
    void combatFifteen() {
        assertFalse(DungeonEntry.mayLead(0));
        assertFalse(DungeonEntry.mayLead(67_424.9));
        assertTrue(DungeonEntry.mayLead(67_425));
        assertTrue(DungeonEntry.mayLead(1_000_000));
    }

    /** Mort's three lines, as MCW has them. */
    @Test
    void mort() {
        assertEquals(3, DungeonEntry.MORT_REFUSAL.size());
        assertEquals("§e[NPC] §bMort§f: §7§oYou need Combat Level XV before leading a dungeon.", DungeonEntry.MORT_REFUSAL.get(2));
    }
}
