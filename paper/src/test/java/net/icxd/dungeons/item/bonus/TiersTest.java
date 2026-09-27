package net.icxd.dungeons.item.bonus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tiered bonus numbers by how many pieces are worn, and swapping them in text. */
class TiersTest {
    /** The wiki's example: +5, 10, 15 Defense from 2 pieces. */
    private static final Tiers DEFENSE = new Tiers(2, 5, 10, 15);

    @Test
    void numbersByPieces() {
        assertEquals(5, DEFENSE.at(0));
        assertEquals(5, DEFENSE.at(2));
        assertEquals(10, DEFENSE.at(3));
        assertEquals(15, DEFENSE.at(4));
        assertEquals(15, DEFENSE.at(8));
    }

    /** Snorkeling Armor's 0, 2, 5 and 10 from 1 piece count from 2: its lore shows 2 until then. */
    @Test
    void shownFromTheLeastThatCounts() {
        Tiers respiration = new Tiers(1, 0, 2, 5, 10);
        assertEquals(2, respiration.shown(0, 2));
        assertEquals(2, respiration.shown(1, 2));
        assertEquals(5, respiration.shown(3, 2));
        assertEquals(0, respiration.at(1));
    }

    @Test
    void textForPieces() {
        List<String> text = List.of("&7Test line.", "&7Grants &a+5 Defense&7 and &a+5 more.");
        assertEquals(List.of("&7Test line.", "&7Grants &a+15 Defense&7 and &a+5 more."), DEFENSE.text(text, "&a+", 4, 2));
        assertEquals(text, DEFENSE.text(text, "&a+", 2, 2));
        assertEquals(text, DEFENSE.text(text, "&c+", 4, 2));
    }

    /** Not the start of a longer number: "+5" isn't in "+50" or "+5.5". */
    @Test
    void wholeNumbersOnly() {
        List<String> text = List.of("&7Gain &a+50 and &a+5.5 and &a+5.");
        assertEquals(List.of("&7Gain &a+50 and &a+5.5 and &a+10."), DEFENSE.text(text, "&a+", 3, 2));
    }
}
