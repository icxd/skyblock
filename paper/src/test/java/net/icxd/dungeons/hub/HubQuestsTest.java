package net.icxd.dungeons.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.mob.MobKinds;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.region.RegionType;
import net.icxd.dungeons.utils.SkyBlockTime;

/** Marco's and the Bartender's quests against the recording (2026-10-25): their words and when, the flowers, the Graveyard. */
class HubQuestsTest {
    @Test
    void aLineForEveryMoment() {
        assertEquals(Marco.ASKS.size(), Marco.ASK_TICKS.length);
        assertEquals(Marco.THANKS.size(), Marco.THANKS_TICKS.length);
        assertEquals(Bartender.ASKS.size(), Bartender.ASK_TICKS.length);
        assertEquals(Bartender.thanks("&bICoding").size(), Bartender.THANKS_TICKS.length);
        assertEquals(Banker.LINES.size(), Banker.LINE_TICKS.length);
    }

    /** 00:37.3 and 03:16.2, as they came in chat. */
    @Test
    void asTheyCameInChat() {
        assertEquals("&e[NPC] Marco&f: &r&fHey there!", Marco.ASKS.getFirst());
        assertEquals("&e[NPC] Marco&f: &r&fThank you so much for your help. You should visit me in my &bCanvas Room&f sometime! "
                + "Step into the portal to the left over there!", Marco.THANKS.getLast());
        assertEquals("&e[NPC] Bartender&f: &r&fWords cannot describe how thankful I am, &bICoding&f!", Bartender.thanks("&bICoding").getFirst());
        assertEquals("&e[NPC] Bartender&f: &r&fHere, take this sword. It should help you take out the Zombies.", Bartender.ASKS.getLast());
        assertEquals(5, Quests.ROSE_RED);
    }

    /** He talks till "Talk to the Banker" is done (a profile that left mid-way hears him again), then opens the bank. */
    @Test
    void theBankerTalksFirst() {
        Document profile = new Document();
        assertTrue(Banker.talks(profile), "a profile from before the Village started Saving Up");
        Quests.start(profile, Quests.SAVING_UP, 1000);
        assertTrue(Banker.talks(profile));
        Quests.complete(profile, Quests.SAVING_UP, Quests.TALK_TO_THE_BANKER, 2000);
        assertFalse(Banker.talks(profile));
        Quests.complete(profile, Quests.SAVING_UP, Quests.DEPOSIT_COINS, 3000);
        assertFalse(Banker.talks(profile));
    }

    /** The six flowers picked (00:56.0 to 01:03.6) are in the beds. */
    @Test
    void theRecordedPicksAreInTheBeds() {
        int[][] picked = {{84, 75, 0}, {82, 75, 0}, {85, 75, 12}, {86, 75, 15}, {87, 75, 18}, {89, 75, 16}};
        for (int[] at : picked) assertTrue(FlowerGarden.BEDS.contains(at[0] + 0.5, at[1] + 0.5, at[2] + 0.5), at[0] + " " + at[2]);
        assertFalse(FlowerGarden.BEDS.contains(70.5, 75.5, 0.5));
        assertEquals(100, FlowerGarden.REGROW_TICKS);
    }

    /** Zombie Villagers from 8pm to 6am (the wiki's); every spot a recorded mob was first seen at is in the Graveyard. */
    @Test
    void theGraveyard() {
        assertTrue(Graveyard.night(new SkyBlockTime(521, 11, 22, 20, 0)));
        assertTrue(Graveyard.night(new SkyBlockTime(521, 11, 22, 0, 10)));
        assertTrue(Graveyard.night(new SkyBlockTime(521, 11, 22, 5, 59)));
        assertFalse(Graveyard.night(new SkyBlockTime(521, 11, 22, 6, 0)));
        assertFalse(Graveyard.night(new SkyBlockTime(521, 11, 22, 19, 59)));
        assertEquals(59, Graveyard.ZOMBIE_SPOTS.length);
        assertEquals(12, Graveyard.VILLAGER_SPOTS.length);
        for (double[][] spots : new double[][][]{Graveyard.ZOMBIE_SPOTS, Graveyard.VILLAGER_SPOTS}) {
            for (double[] at : spots) assertEquals(RegionType.GRAVEYARD, RegionType.getRegionType(at[0], at[1], at[2]), at[0] + " " + at[2]);
        }
        assertTrue(Graveyard.counts(MobKinds.GRAVEYARD_ZOMBIE));
        assertTrue(Graveyard.counts(MobKinds.ZOMBIE_VILLAGER));
        assertFalse(Graveyard.counts(MobKinds.ZOMBIE_GRUNT), "a dungeon's zombie isn't the Graveyard's");
    }
}
