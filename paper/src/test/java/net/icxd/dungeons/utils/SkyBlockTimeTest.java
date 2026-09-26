package net.icxd.dungeons.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyBlockTimeTest {
    private static SkyBlockTime at(String instant) {
        return SkyBlockTime.at(Instant.parse(instant).toEpochMilli());
    }

    /** What Hypixel's sidebar showed in the recordings, at the moments they were taken. */
    @Test
    void matchesHypixel() {
        SkyBlockTime hub = at("2026-09-26T06:23:50.600Z");
        assertEquals("Early Autumn 8th", hub.date());
        assertEquals("10:30am", hub.clock());
        assertTrue(hub.isDay());
        assertEquals(516, hub.year());

        SkyBlockTime dungeonHub = at("2026-09-26T06:49:30.100Z");
        assertEquals("Early Autumn 9th", dungeonHub.date());
        assertEquals("5:20pm", dungeonHub.clock());
        // 100 seconds later, in the dungeon: two hours on.
        assertEquals("7:20pm", at("2026-09-26T06:51:10.300Z").clock());
    }

    @Test
    void calendar() {
        assertEquals("Early Spring 1st", SkyBlockTime.at(SkyBlockTime.EPOCH).date());
        assertEquals("12:00am", SkyBlockTime.at(SkyBlockTime.EPOCH).clock());
        assertEquals(2, SkyBlockTime.at(SkyBlockTime.EPOCH + SkyBlockTime.YEAR_MS).year());
        assertEquals("Spring 1st", SkyBlockTime.at(SkyBlockTime.EPOCH + SkyBlockTime.MONTH_MS).date());
        assertEquals("Early Spring 2nd", SkyBlockTime.at(SkyBlockTime.EPOCH + SkyBlockTime.DAY_MS).date());
        assertEquals("Early Spring 11th", SkyBlockTime.at(SkyBlockTime.EPOCH + 10 * SkyBlockTime.DAY_MS).date());
        assertEquals("Early Spring 23rd", SkyBlockTime.at(SkyBlockTime.EPOCH + 22 * SkyBlockTime.DAY_MS).date());
        // Noon, and 7pm (nightfall).
        assertEquals("12:00pm", SkyBlockTime.at(SkyBlockTime.EPOCH + SkyBlockTime.DAY_MS / 2).clock());
        SkyBlockTime seven = SkyBlockTime.at(SkyBlockTime.EPOCH + SkyBlockTime.DAY_MS * 19 / 24);
        assertEquals("7:00pm", seven.clock());
        assertFalse(seven.isDay());
    }
}
