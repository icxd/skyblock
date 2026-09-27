package net.icxd.dungeons.dungeons;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Catacombs and class experience on a profile, and their levels and progress (research skills.md 3.5, 3.6). */
class DungeonProfileTest {
    @Test
    void experience() {
        Document profile = new Document();
        assertEquals(0, DungeonProfile.catacombsXp(profile));
        assertEquals(0, DungeonProfile.classXp(profile, DungeonClass.BERSERK));
        DungeonProfile.addCatacombsXp(profile, 131.1);
        DungeonProfile.addCatacombsXp(profile, 30.8);
        DungeonProfile.addClassXp(profile, DungeonClass.BERSERK, 126.2);
        DungeonProfile.addClassXp(profile, DungeonClass.BERSERK, -5);
        assertEquals(161.9, DungeonProfile.catacombsXp(profile), 1e-9);
        assertEquals(126.2, DungeonProfile.classXp(profile, DungeonClass.BERSERK));
        assertEquals(0, DungeonProfile.classXp(profile, DungeonClass.MAGE));
        assertEquals(2, DungeonLevels.level(DungeonProfile.catacombsXp(profile)));
    }

    /** Older profiles kept whole numbers: they read the same, and adding writes a double. */
    @Test
    void wholeNumbers() {
        Document profile = new Document("dungeons", new Document("catacombsExp", 100).append("classExp", new Document("MAGE", 50)));
        assertEquals(100, DungeonProfile.catacombsXp(profile));
        DungeonProfile.addClassXp(profile, DungeonClass.MAGE, 0.5);
        assertEquals(50.5, DungeonProfile.classXp(profile, DungeonClass.MAGE));
        assertInstanceOf(Double.class, profile.get("dungeons", Document.class).get("classExp", Document.class).get("MAGE"));
    }

    @Test
    void table() {
        assertEquals(50, DungeonLevels.xpFor(1));
        assertEquals(52_500, DungeonLevels.xpFor(21));
        assertEquals(116_250_000, DungeonLevels.xpFor(50));
        assertEquals(200_000_000, DungeonLevels.xpFor(51));
        assertEquals(200_000_000, DungeonLevels.xpFor(80));
        assertEquals(188_140, DungeonLevels.cumulative(21));
        assertEquals(569_809_640, DungeonLevels.cumulative(50));
        assertEquals(969_809_640, DungeonLevels.cumulative(52));
        assertEquals(0.22, DungeonLevels.progress(188_140 + 0.22 * 71_500), 1e-9);
        assertEquals(0, DungeonLevels.progress(0));
        assertEquals(0.5, DungeonLevels.progress(569_809_640 + 100_000_000), 1e-9);
    }
}
