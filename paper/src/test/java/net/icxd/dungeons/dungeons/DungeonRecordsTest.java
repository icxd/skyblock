package net.icxd.dungeons.dungeons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;

/** Completions, records and today's runs on a profile. */
class DungeonRecordsTest {
    private static final LocalDate DAY = LocalDate.of(2026, 9, 26);

    @Test
    void firstCompletionSetsTheRecords() {
        Document profile = new Document();
        assertEquals(0, DungeonRecords.completions(profile, DungeonFloor.ENTRANCE));
        DungeonRecords.Completion first = DungeonRecords.complete(profile, DungeonFloor.ENTRANCE, 109, "C", 109_000, DAY);
        assertEquals(new DungeonRecords.Completion(0, 0, true, true), first);
        assertEquals(1, DungeonRecords.completions(profile, DungeonFloor.ENTRANCE));
        assertEquals(109, DungeonRecords.bestScore(profile, DungeonFloor.ENTRANCE));
        assertEquals(109_000, DungeonRecords.fastest(profile, DungeonFloor.ENTRANCE));
        assertEquals(1, DungeonRecords.completionsOn(profile, DAY));
    }

    @Test
    void recordsOnlyWhenBeaten() {
        Document profile = new Document();
        DungeonRecords.complete(profile, DungeonFloor.ENTRANCE, 182, "B", 235_000, DAY);
        // Faster, lower score.
        assertEquals(new DungeonRecords.Completion(1, 1, false, true),
                DungeonRecords.complete(profile, DungeonFloor.ENTRANCE, 109, "C", 109_000, DAY));
        // Slower, higher score.
        assertEquals(new DungeonRecords.Completion(2, 2, true, false),
                DungeonRecords.complete(profile, DungeonFloor.ENTRANCE, 189, "B", 300_000, DAY));
        // Neither.
        assertEquals(new DungeonRecords.Completion(3, 3, false, false),
                DungeonRecords.complete(profile, DungeonFloor.ENTRANCE, 189, "B", 109_000, DAY));
        assertEquals(189, DungeonRecords.bestScore(profile, DungeonFloor.ENTRANCE));
        assertEquals(109_000, DungeonRecords.fastest(profile, DungeonFloor.ENTRANCE));
        assertEquals(4, DungeonRecords.completions(profile, DungeonFloor.ENTRANCE));
        // Other floors are their own.
        assertEquals(0, DungeonRecords.completions(profile, DungeonFloor.FLOOR_1));
    }

    /** Today's count starts over on another day, and covers every floor. */
    @Test
    void today() {
        Document profile = new Document();
        DungeonRecords.complete(profile, DungeonFloor.ENTRANCE, 100, "C", 100_000, DAY);
        DungeonRecords.complete(profile, DungeonFloor.FLOOR_1, 100, "C", 100_000, DAY);
        assertEquals(2, DungeonRecords.completionsOn(profile, DAY));
        assertEquals(0, DungeonRecords.completionsOn(profile, DAY.plusDays(1)));
        DungeonRecords.Completion next = DungeonRecords.complete(profile, DungeonFloor.ENTRANCE, 100, "C", 100_000, DAY.plusDays(1));
        assertEquals(0, next.todayBefore());
        assertEquals(1, DungeonRecords.completionsOn(profile, DAY.plusDays(1)));
        assertEquals(0, DungeonRecords.completionsOn(profile, DAY));
    }

    /** The highest floor completed, an int as item requirements read it; the Entrance is 0. */
    @Test
    void highestFloor() {
        Document profile = new Document("dungeons", new Document("floors", new Document("highest", 0).append("masterHighest", 0)));
        Document floors = profile.get("dungeons", Document.class).get("floors", Document.class);
        DungeonRecords.complete(profile, DungeonFloor.FLOOR_3, 250, "A", 400_000, DAY);
        DungeonRecords.complete(profile, DungeonFloor.FLOOR_1, 250, "A", 400_000, DAY);
        assertEquals(3, floors.get("highest"));
        assertInstanceOf(Integer.class, floors.get("highest"));
        assertEquals(0, floors.get("masterHighest"));
        DungeonRecords.complete(profile, DungeonFloor.MASTER_FLOOR_2, 250, "A", 400_000, DAY);
        assertEquals(2, floors.get("masterHighest"));
    }

    /** The fastest S and S+ times are kept apart from the fastest of all. */
    @Test
    void fastestByGrade() {
        Document profile = new Document();
        DungeonRecords.complete(profile, DungeonFloor.FLOOR_7, 305, "S+", 400_000, DAY);
        DungeonRecords.complete(profile, DungeonFloor.FLOOR_7, 280, "S", 350_000, DAY);
        DungeonRecords.complete(profile, DungeonFloor.FLOOR_7, 200, "B", 300_000, DAY);
        Document f7 = profile.get("dungeons", Document.class).get("floors", Document.class).get("FLOOR_7", Document.class);
        assertEquals(300_000L, f7.get("fastest"));
        assertEquals(350_000L, f7.get("fastestS"));
        assertEquals(400_000L, f7.get("fastestSPlus"));
        assertTrue(DungeonRecords.bestScore(profile, DungeonFloor.FLOOR_7) == 305);
        assertFalse(DungeonRecords.complete(profile, DungeonFloor.FLOOR_7, 305, "S+", 500_000, DAY).bestScore());
    }
}
