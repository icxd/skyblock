package net.icxd.dungeons.item.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A rarity one up, as a Recombobulator 3000 makes it (the wiki's Recombobulator 3000). */
class RarityTest {
    @Test
    void oneUp() {
        assertEquals(Rarity.UNCOMMON, Rarity.COMMON.upgrade());
        assertEquals(Rarity.MYTHIC, Rarity.LEGENDARY.upgrade());
        assertEquals(Rarity.DIVINE, Rarity.MYTHIC.upgrade());
        assertEquals(Rarity.SPECIAL, Rarity.DIVINE.upgrade());
        assertEquals(Rarity.VERY_SPECIAL, Rarity.SPECIAL.upgrade());
    }

    @Test
    void veryVerySpecial() {
        assertEquals(Rarity.VERY_SPECIAL, Rarity.VERY_SPECIAL.upgrade());
        // As it always was: UNKNOWN whether the wiki's cosmetics' rarity (Very Special once recombobulated) is this one.
        assertEquals(Rarity.UNOBTAINABLE, Rarity.UNOBTAINABLE.upgrade());
    }
}
