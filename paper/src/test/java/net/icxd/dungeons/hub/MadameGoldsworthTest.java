package net.icxd.dungeons.hub;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.npc.NpcTable;

/** Madame Goldsworth's first talk against the recording (2026-10-25, 00:41.4 to 00:58.4). */
class MadameGoldsworthTest {
    @Test
    void herLinesAsRecorded() {
        List<String> lines = MadameGoldsworth.firstTalk("ICoding");
        assertEquals(7, lines.size());
        assertEquals("&e[NPC] &6Madame Eleanor Q. Goldsworth III&f: &r&fWelcome, &bICoding&f!", lines.getFirst());
        assertEquals("&e[NPC] &6Madame Eleanor Q. Goldsworth III&f: &r&fHowever, once you donate an item, it will be come &cCo-op Soulbound &fto "
                + "you, meaning that you won't be able to trade off your profile, auction, or sell the item anymore.", lines.get(5));
        assertEquals("&e[NPC] &6Madame Eleanor Q. Goldsworth III&f: &r&fChoose carefully!", lines.getLast());
        assertArrayEquals(new long[]{0, 30, 90, 150, 200, 260, 340}, MadameGoldsworth.LINE_TICKS);
        assertEquals(2, MadameGoldsworth.VOICE.pitch());
    }

    /** Her id is her name's, as the Hub's table names her. */
    @Test
    void herId() {
        NpcTable.Entry entry = new NpcTable.Entry("Madame Eleanor Q. Goldsworth III", "player", 33.5, 73, 11.5, 90, 0, List.of(), new double[0],
                null, null, null, null, Map.of());
        assertEquals(MadameGoldsworth.ID, entry.id());
    }
}
