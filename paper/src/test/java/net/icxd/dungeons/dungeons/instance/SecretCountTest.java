package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A room's secrets as the team finds them (research secrets_puzzles.md 1.6). */
class SecretCountTest {
    @Test
    void eachSecretCountsOnce() {
        SecretCount crypt = new SecretCount(5);
        assertTrue(crypt.find(2));
        assertFalse(crypt.find(2));
        assertEquals(1, crypt.shown());
        assertTrue(crypt.find(0));
        assertEquals(2, crypt.shown());
        assertFalse(crypt.allFound());
    }

    /** Golden Oasis shows "0/1 Secrets" but has two chests and a Redstone Key: the count stops at 1. */
    @Test
    void neverMoreThanHypixelCounts() {
        SecretCount oasis = new SecretCount(1);
        assertFalse(oasis.allFound());
        assertTrue(oasis.find(0));
        assertTrue(oasis.find(1));
        assertTrue(oasis.find(3));
        assertEquals(1, oasis.shown());
        assertEquals(1, oasis.total());
        assertTrue(oasis.allFound());
    }

    @Test
    void allFound() {
        SecretCount longHall = new SecretCount(3);
        longHall.find(4);
        longHall.find(7);
        assertFalse(longHall.allFound());
        longHall.find(9);
        assertTrue(longHall.allFound());
        // A room without secrets has them all.
        assertTrue(new SecretCount(0).allFound());
        assertEquals(0, new SecretCount(-1).total());
    }
}
