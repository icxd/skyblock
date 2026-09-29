package net.icxd.dungeons.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeathCoinsTest {
    @Test
    void halfThePurseWithItsDecimals() {
        assertEquals(2.5, DeathCoins.lost(5, 0));
        assertEquals(0, DeathCoins.lost(0, 0));
        assertEquals(0, DeathCoins.lost(-3, 0));
    }

    @Test
    void savedSharesComeOffTheHalf() {
        // Bank V saves half of the loss; two pieces all of it, and more can't give coins back.
        assertEquals(25, DeathCoins.lost(100, 0.5));
        assertEquals(0, DeathCoins.lost(100, 1));
        assertEquals(0, DeathCoins.lost(100, 1.5));
        assertEquals(50, DeathCoins.lost(100, -1));
    }

    @Test
    void theLineRoundsDown() {
        // "If the player dies with 5 in the purse, it will say that the player lost 2" (the wiki's Death).
        assertEquals("2", DeathCoins.wholeCoins(2.5));
        assertEquals("1,234,567", DeathCoins.wholeCoins(1234567.9));
    }
}
