package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;

/** As recorded (research secrets_puzzles.md 1.6, 2). */
class SecretTextTest {
    /** R1 had 21 secrets: 4.8%, 9.5%, 14.3%, then 19% (no ".0"). */
    @Test
    void shareOfTheFloor() {
        assertEquals("0", SecretText.percent(0, 21));
        assertEquals("4.8", SecretText.percent(1, 21));
        assertEquals("9.5", SecretText.percent(2, 21));
        assertEquals("14.3", SecretText.percent(3, 21));
        assertEquals("19", SecretText.percent(4, 21));
        assertEquals("100", SecretText.percent(21, 21));
        assertEquals("0", SecretText.percent(0, 0));
    }

    @Test
    void actionBar() {
        // "...Mana          &71/5 Secrets": ten spaces after the mana.
        assertEquals("          &71/5 Secrets", SecretText.actionBar(1, 5));
        assertEquals("          &70/6 Secrets", SecretText.actionBar(0, 6));
        assertEquals("", SecretText.actionBar(0, 0));
    }

    @Test
    void witherEssence() {
        assertEquals("&fYou found a &dWither Essence&f! Everyone gains an extra essence!", SecretText.witherEssence(null));
        assertEquals("&bAlice &ffound a &dWither Essence&f! Everyone gains an extra essence!", SecretText.witherEssence("&bAlice"));
    }

    @Test
    void rewards() {
        // Secrets give level I or II blessings of the four kinds rooms have, or an item of the source's.
        java.util.Random random = new java.util.Random(49);
        int blessings = 0;
        for (int i = 0; i < 4_000; i++) {
            SecretRewards.Reward reward = SecretRewards.roll(SecretRewards.Source.CHEST, DungeonFloor.ENTRANCE, random);
            if (reward.isBlessing()) {
                blessings++;
                assertTrue(reward.level() == 1 || reward.level() == 2);
                assertTrue(Blessing.FOUND.contains(reward.blessing()));
            } else {
                assertTrue(SecretRewards.items(SecretRewards.Source.CHEST, DungeonFloor.ENTRANCE).contains(reward.item()));
            }
            assertFalse(SecretRewards.roll(SecretRewards.Source.ITEM, DungeonFloor.ENTRANCE, random).isBlessing());
        }
        assertEquals(0.75, blessings / 4_000.0, 0.03);
        // The Dungeon Chest Key and the Treasure Talisman only from Floor IV; a bat never has a Candycomb or a First Draft.
        assertFalse(SecretRewards.items(SecretRewards.Source.CHEST, DungeonFloor.ENTRANCE).contains("DUNGEON_CHEST_KEY"));
        assertTrue(SecretRewards.items(SecretRewards.Source.ITEM, DungeonFloor.FLOOR_4).contains("TREASURE_TALISMAN"));
        assertFalse(SecretRewards.items(SecretRewards.Source.BAT, DungeonFloor.ENTRANCE).contains("CANDYCOMB"));
        assertTrue(SecretRewards.items(SecretRewards.Source.CHEST, DungeonFloor.ENTRANCE).contains("ARCHITECT_FIRST_DRAFT"));
        assertFalse(SecretRewards.items(SecretRewards.Source.ITEM, DungeonFloor.ENTRANCE).contains("ARCHITECT_FIRST_DRAFT"));
    }
}
