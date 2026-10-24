package net.icxd.dungeons.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.npc.NpcTable;

/** The Hub NPCs' words against the recording (2026-10-24). */
class HubTalkTest {
    private static NpcTable.Entry npc(String name, double x, double y, double z) {
        return new NpcTable.Entry(name, "player", x, y, z, 0, 0, List.of(name), new double[]{0}, "skin", null, null, null, Map.of());
    }

    @Test
    void aLineForEveryMoment() {
        for (HubTalk talk : HubTalk.ALL) assertEquals(talk.lines().size(), talk.ticks().length, talk.id());
    }

    @Test
    void asTheyCameInChat() {
        HubTalk nancy = HubTalk.of(npc("Minister Nancy", 8.5, 79, 17.5)).orElseThrow();
        assertEquals("&e[NPC] Nancy&f: &rHi!", nancy.line(0));
        HubTalk elizabeth = HubTalk.of(npc("Elizabeth", -6.25, 79, 19.25)).orElseThrow();
        assertEquals("&e[NPC] &dElizabeth&f: &rHello! Welcome to &bSkyBlock&r!", elizabeth.line(0));
    }

    /** Two Clerk Seraphines: the recording talked to the Community Center's, not the Election Room's. */
    @Test
    void theRightSeraphine() {
        assertTrue(HubTalk.of(npc("Clerk Seraphine", -1.5, 79, 10.5)).isPresent());
        assertTrue(HubTalk.of(npc("Clerk Seraphine", 4.5, 49, 34.5)).isEmpty());
        assertTrue(HubTalk.of(npc("Biblio", 8.5, 79, 10.5)).isEmpty(), "not talked to");
    }
}
