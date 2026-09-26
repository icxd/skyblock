package net.icxd.dungeons.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DungeonFloorTest {
    @Test
    void hypixelInstanceNames() {
        assertEquals("CATACOMBS_ENTRANCE", DungeonFloor.ENTRANCE.getInstanceName());
        assertEquals("CATACOMBS_FLOOR_SEVEN", DungeonFloor.FLOOR_7.getInstanceName());
        assertEquals("MASTER_CATACOMBS_FLOOR_ONE", DungeonFloor.MASTER_FLOOR_1.getInstanceName());
    }

    @Test
    void parsesEveryWayOfWritingAFloor() {
        for (DungeonFloor floor : DungeonFloor.values()) {
            assertEquals(floor, DungeonFloor.parse(floor.name()));
            assertEquals(floor, DungeonFloor.parse(floor.getInstanceName()));
            assertEquals(floor, DungeonFloor.parse(floor.getShortName().toLowerCase()));
        }
        assertEquals(DungeonFloor.ENTRANCE, DungeonFloor.parse("entrance"));
        assertEquals(DungeonFloor.FLOOR_3, DungeonFloor.parse("3"));
        assertEquals(DungeonFloor.MASTER_FLOOR_3, DungeonFloor.parse("m3"));
        assertNull(DungeonFloor.parse("F8"));
        assertNull(DungeonFloor.parse("catacombs"));
    }
}
