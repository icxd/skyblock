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

        // The SkyBlock Menu tour (recorded from 00:15:16 local, UTC+2): its calendar item at 00:18.2.
        assertEquals("25th Autumn 516", at("2026-09-26T22:15:34.200Z").calendarDate());
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
        assertEquals("1st Early Spring 1", SkyBlockTime.at(SkyBlockTime.EPOCH).calendarDate());
        assertEquals("12th Late Winter 3", SkyBlockTime.at(SkyBlockTime.EPOCH + 2 * SkyBlockTime.YEAR_MS + 11 * SkyBlockTime.MONTH_MS
                + 11 * SkyBlockTime.DAY_MS).calendarDate());
        // Noon, and 7pm (nightfall).
        assertEquals("12:00pm", SkyBlockTime.at(SkyBlockTime.EPOCH + SkyBlockTime.DAY_MS / 2).clock());
        SkyBlockTime seven = SkyBlockTime.at(SkyBlockTime.EPOCH + SkyBlockTime.DAY_MS * 19 / 24);
        assertEquals("7:00pm", seven.clock());
        assertFalse(seven.isDay());
    }

    /** The Quest Log's "Started:" on the new profile's Getting Started (2026-10-24, 00:18.0), in Late Winter. */
    @Test
    void questLogStart() {
        SkyBlockTime started = new SkyBlockTime(521, 11, 17, 3, 8);
        assertEquals("Late Winter 17th", started.date());
        assertEquals("Winter 17th, Year 521", started.seasonDate());
        assertEquals("3:08am", started.exactClock());
        assertEquals("3:00am", started.clock());
        assertEquals("Spring 9th, Year 485", new SkyBlockTime(485, 1, 9, 4, 21).seasonDate());
        assertEquals("Summer 1st, Year 489", new SkyBlockTime(489, 3, 1, 23, 30).seasonDate());
        assertEquals("11:30pm", new SkyBlockTime(489, 3, 1, 23, 30).exactClock());
        assertEquals("12:05am", new SkyBlockTime(489, 3, 1, 0, 5).exactClock());
    }
}
