package net.icxd.dungeons.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

/** The rewards waiting in Claim Reward, and what claiming one says (recorded 2026-10-25, 01:19.4 and 02:24.6). */
class ClaimsTest {
    @Test
    void aClaimWaitsTillItsTaken() {
        Document profile = new Document();
        assertEquals(List.of(), Claims.unclaimed(profile));
        Claims.add(profile, "artist:SPRAY_CAN");
        Claims.add(profile, "bartender:UNDEAD_SWORD");
        Claims.add(profile, "artist:SPRAY_CAN");
        assertEquals(List.of("artist:SPRAY_CAN", "bartender:UNDEAD_SWORD"), Claims.unclaimed(profile), "once each");
        assertTrue(Claims.has(profile, "artist:SPRAY_CAN"));
        assertTrue(Claims.take(profile, "artist:SPRAY_CAN"));
        assertFalse(Claims.take(profile, "artist:SPRAY_CAN"), "claimed already");
        assertFalse(Claims.has(profile, "artist:SPRAY_CAN"));
        assertEquals(List.of("bartender:UNDEAD_SWORD"), profile.getList(Claims.KEY, String.class));
    }

    @Test
    void theItemIsAfterTheColon() {
        assertEquals("SPRAY_CAN", Claims.item("artist:SPRAY_CAN"));
        assertEquals("UNDEAD_SWORD", Claims.item("bartender:UNDEAD_SWORD"));
    }

    @Test
    void whatClaimingSays() {
        assertEquals("&aYou claimed a &fSpray Can&a!", Claims.claimed("&fSpray Can"));
        assertEquals("&aYou claimed an &fUndead Sword&a!", Claims.claimed("&fUndead Sword"));
    }
}
