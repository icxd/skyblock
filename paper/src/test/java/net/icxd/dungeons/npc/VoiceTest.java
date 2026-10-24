package net.icxd.dungeons.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** NPC voices against the recording (2026-10-24): Jerry's and the Banker's yes or hum, Ophelia's haggling. */
class VoiceTest {
    @Test
    void aVillagersYesOrHumAtItsPitch() {
        Random random = new Random(1);
        Set<String> heard = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            Voice.Said said = Voice.VILLAGER.next(random);
            heard.add(said.sound());
            assertEquals(1f, said.pitch());
        }
        assertEquals(Set.of("entity.villager.yes", "entity.villager.ambient"), heard);
        assertEquals(2f, Voice.villager(2).next(random).pitch(), "Elizabeth's, Clerk Seraphine's");
    }

    /** Recorded at 1.70, 1.86, 1.79 and 1.90. */
    @Test
    void opheliaHaggles() {
        Random random = new Random(2);
        for (int i = 0; i < 40; i++) {
            Voice.Said said = Voice.OPHELIA.next(random);
            assertEquals("entity.villager.trade", said.sound());
            assertTrue(said.pitch() >= 1.7f && said.pitch() <= 1.9f, "pitch " + said.pitch());
        }
    }
}
