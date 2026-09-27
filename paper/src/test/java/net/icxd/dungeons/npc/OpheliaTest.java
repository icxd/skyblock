package net.icxd.dungeons.npc;

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
        assertEquals(List.of(Ophelia.ID), profile.getList(Ophelia.TALKED, String.class));
        assertTrue(Ophelia.firstTime(new Document()), "another profile hasn't");
    }

    @Test
    void herLinesAsTheWikiHasThem() {
        assertEquals(3, Ophelia.FIRST_TALK.size());
        assertEquals("&e[NPC] &cOphelia&f: My trick? I make &6coins&f from the others' greed.", Ophelia.FIRST_TALK.get(1));
    }
}
