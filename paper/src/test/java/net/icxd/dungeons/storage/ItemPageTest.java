package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** When a storage menu writes its items into the profile (see {@link ItemPage#writes}). */
class ItemPageTest {
    @Test
    void writes() {
        // While their data is here and their inventory is saved: after a click, on close, with any save.
        assertTrue(ItemPage.writes(true, false, false));
        assertTrue(ItemPage.writes(true, false, true));
        // Their inventory isn't theirs here yet: nothing is saved, so nothing is written.
        assertFalse(ItemPage.writes(false, false, false));
        assertFalse(ItemPage.writes(false, false, true));
        // Handed off: only the save that lets the data go (the server stopping, with the menu open).
        assertFalse(ItemPage.writes(true, true, false));
        assertTrue(ItemPage.writes(true, true, true));
    }
}
