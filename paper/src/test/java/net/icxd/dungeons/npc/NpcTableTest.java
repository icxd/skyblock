package net.icxd.dungeons.npc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

/** npcs/hub.json as the replay tool's `npcs` writes it (entries cut down from the 2026-10-24 recording's). */
class NpcTableTest {
    private static final String HUB = """
            {"area": "Village", "recordings": ["2026_10_24_10_56_23_1.mcpr"], "npcs": [
             {"name": "Biblio", "type": "player", "x": 8.5, "y": 79, "z": 10.5, "yaw": 74.53, "pitch": 0,
              "lines": ["&a&l⚑ GUIDES", "Biblio", "&e&lCLICK"], "heights": [0.25, 0, -0.25], "skin": {"value": "eyJ0", "signature": "vV2/"}},
             {"name": "Mayor Jerry", "type": "villager", "x": 6.5, "y": 79, "z": 19.5, "yaw": 135, "pitch": 0,
              "lines": ["Mayor Jerry", "&e&lCLICK"], "heights": [0.09, -0.16], "villager": {"type": "plains", "profession": "farmer", "level": 1}},
             {"name": "Fisherwoman Enid", "type": "player", "x": 41.5, "y": 70, "z": -22.25, "yaw": 0, "pitch": 0,
              "lines": ["Fisherwoman Enid", "&e&lCLICK"], "heights": [0, -0.25], "skin": {"value": "eyJ1", "signature": null},
              "equipment": {"mainhand": {"id": "fishing_rod", "name": null}}},
             {"name": "Broken", "type": "player", "x": 0, "y": 0, "z": 0, "lines": ["Broken"], "heights": []}
            ]}""";

    @Test
    void asRecorded() {
        NpcTable table = NpcTable.parse(JsonParser.parseString(HUB).getAsJsonObject());
        assertEquals(3, table.npcs().size());
        NpcTable.Entry biblio = table.npcs().get(0);
        assertEquals("BIBLIO", biblio.id());
        assertEquals(List.of("&a&l⚑ GUIDES", "Biblio", "&e&lCLICK"), biblio.lines());
        assertArrayEquals(new double[]{0.25, 0, -0.25}, biblio.heights());
        assertEquals(74.53f, biblio.yaw());
        assertEquals("vV2/", biblio.signature());
        NpcTable.Entry jerry = table.npcs().get(1);
        assertEquals("MAYOR_JERRY", jerry.id());
        assertEquals("plains", jerry.villagerType());
        assertEquals("farmer", jerry.villagerProfession());
        assertNull(jerry.skin());
        NpcTable.Entry enid = table.npcs().get(2);
        assertNull(enid.signature());
        assertEquals(Map.of("mainhand", "fishing_rod"), enid.equipment());
        assertEquals(List.of("NPC 4 left out: its tags' heights don't go with its 1 lines"), table.problems());
    }

    @Test
    void idsAsOphelias() {
        NpcTable.Entry madame = new NpcTable.Entry("Madame Eleanor Q. Goldsworth III", "player", 0, 0, 0, 0, 0, List.of(), new double[0],
                null, null, null, null, Map.of());
        assertEquals("MADAME_ELEANOR_Q_GOLDSWORTH_III", madame.id());
    }
}
