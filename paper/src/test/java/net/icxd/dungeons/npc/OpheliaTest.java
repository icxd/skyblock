package net.icxd.dungeons.npc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

class OpheliaTest {
    @Test
    void sheTalksOncePerProfile() {
        Document profile = new Document();
        assertTrue(Ophelia.firstTime(profile));
        assertFalse(Ophelia.firstTime(profile));
        assertEquals(List.of(Ophelia.ID), profile.getList(Npc.TALKED, String.class));
        assertTrue(Ophelia.firstTime(new Document()), "another profile hasn't");
    }

    /** As recorded (2026-10-24): 08:24.6, 08:27.1 and 08:29.1, her shop at 08:31.2. */
    @Test
    void herLinesAsRecorded() {
        assertEquals(3, Ophelia.FIRST_TALK.size());
        assertEquals("&e[NPC] &cOphelia&f: &rMy trick? I make &6coins &rfrom the others' greed.", Ophelia.FIRST_TALK.get(1));
        assertArrayEquals(new long[]{0, 50, 90}, Ophelia.LINE_TICKS);
        assertEquals(132, Ophelia.SHOP_TICKS);
    }
}
