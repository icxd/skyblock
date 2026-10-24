package net.icxd.dungeons.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.quest.Place.Island;

class QuestsTest {
    private static final Place ISLAND = new Place(Island.PRIVATE_ISLAND, null);
    private static final Place HUB = new Place(Island.HUB, null);
    private static final Place VILLAGE = new Place(Island.HUB, "Village");
    private static final Place DUNGEON_HUB = new Place(Island.DUNGEON_HUB, "Dungeon Hub");
    private static final Place BANK = new Place(Island.HUB, "Bank");

    @Test
    void gettingStartedGoesStepByStepAndLeadsToMarco() {
        Document profile = new Document();
        QuestProgress.newProfile(profile, 1000);
        assertTrue(profile.getBoolean(QuestProgress.WELCOME));
        assertEquals(Quests.BREAK_A_LOG, Quests.current(profile, Quests.GETTING_STARTED));
        assertNull(Quests.complete(profile, Quests.GETTING_STARTED, Quests.CRAFT_A_WORKBENCH, 2000), "not there yet");
        for (int step = Quests.BREAK_A_LOG; step < Quests.USE_THE_TELEPORTER; step++) {
            Quests.Step done = Quests.complete(profile, Quests.GETTING_STARTED, step, 2000 + step);
            assertFalse(done.completed());
            assertEquals(step + 1, Quests.current(profile, Quests.GETTING_STARTED));
        }
        Quests.Step last = Quests.complete(profile, Quests.GETTING_STARTED, Quests.USE_THE_TELEPORTER, 3000);
        assertTrue(last.completed());
        assertSame(Quests.MARCO_THE_ARTIST, last.started());
        assertTrue(Quests.completed(profile, Quests.GETTING_STARTED));
        assertEquals(-1, Quests.current(profile, Quests.GETTING_STARTED));
        assertEquals(0, Quests.current(profile, Quests.MARCO_THE_ARTIST));
        assertEquals(List.of(Quests.MARCO_THE_ARTIST), Quests.active(profile));
        assertEquals(List.of(Quests.GETTING_STARTED), Quests.completed(profile));
        assertNull(Quests.complete(profile, Quests.GETTING_STARTED, Quests.USE_THE_TELEPORTER, 4000), "done once");
    }

    @Test
    void aQuestStartsOnce() {
        Document profile = new Document();
        assertTrue(Quests.start(profile, Quests.TIME_TO_STRIKE, 5));
        assertFalse(Quests.start(profile, Quests.TIME_TO_STRIKE, 6));
        assertEquals(5, Quests.startedAt(profile, Quests.TIME_TO_STRIKE));
        assertEquals(0, Quests.startedAt(profile, Quests.MARCO_THE_ARTIST));
    }

    /**
     * The boss bar through the recording (2026-10-24): Getting Started on the island; in the Hub "Talk to
     * Marco" before the area is known, "Talk to the Bartender" in the Village once it's found; back in the
     * Hub later, the Bartender at once; in the Dungeon Hub, Marco (its own quest isn't here).
     */
    @Test
    void theQuestFollowedIsTheOneMostAtHome() {
        Document profile = new Document();
        QuestProgress.newProfile(profile, 1000);
        assertSame(Quests.GETTING_STARTED, Quests.best(profile, ISLAND));
        assertNull(Quests.best(profile, HUB), "Getting Started is the island's");

        for (int step = 0; step <= Quests.USE_THE_TELEPORTER; step++) Quests.complete(profile, Quests.GETTING_STARTED, step, 2000);
        assertSame(Quests.MARCO_THE_ARTIST, Quests.best(profile, HUB));
        Quests.start(profile, Quests.TIME_TO_STRIKE, 3000);
        assertSame(Quests.TIME_TO_STRIKE, Quests.best(profile, VILLAGE));
        assertSame(Quests.TIME_TO_STRIKE, Quests.best(profile, HUB), "anywhere in the Hub");
        assertSame(Quests.MARCO_THE_ARTIST, Quests.best(profile, DUNGEON_HUB));
        assertSame(Quests.MARCO_THE_ARTIST, Quests.best(profile, ISLAND));
    }

    @Test
    void progressKeepsTheQuestFollowedWhileItsGoing() {
        Document profile = new Document();
        QuestProgress.newProfile(profile, 1000);
        Quest following = Quests.follow(profile, ISLAND, null, true);
        assertSame(Quests.GETTING_STARTED, following);
        Quests.complete(profile, Quests.GETTING_STARTED, Quests.BREAK_A_LOG, 2000);
        assertSame(Quests.GETTING_STARTED, Quests.follow(profile, ISLAND, following, false));

        // Arriving in the Hub after the portal: nothing was followed there, so the progress picks again.
        for (int step = Quests.CRAFT_A_WORKBENCH; step <= Quests.USE_THE_TELEPORTER; step++) Quests.complete(profile, Quests.GETTING_STARTED, step, 3000);
        assertSame(Quests.MARCO_THE_ARTIST, Quests.follow(profile, HUB, null, false));
        // The Village found: arriving there picks the Bartender.
        Quests.start(profile, Quests.TIME_TO_STRIKE, 4000);
        assertSame(Quests.TIME_TO_STRIKE, Quests.follow(profile, VILLAGE, Quests.MARCO_THE_ARTIST, true));
        // Progress elsewhere leaves the one followed (the Banker's quest didn't take the boss bar).
        assertSame(Quests.MARCO_THE_ARTIST, Quests.follow(profile, VILLAGE, Quests.MARCO_THE_ARTIST, false));
    }

    /**
     * The recording's Saving Up: the Banker starts it with its first objective done (+5 SkyBlock XP), the
     * boss bar staying on the Bartender; in the Bank it's followed, and the first deposit completes it.
     */
    @Test
    void savingUp() {
        Document profile = new Document();
        Quests.start(profile, Quests.TIME_TO_STRIKE, 1000);
        assertSame(Quests.TIME_TO_STRIKE, Quests.follow(profile, BANK, null, true));
        assertTrue(Quests.start(profile, Quests.SAVING_UP, 2000));
        assertFalse(Quests.complete(profile, Quests.SAVING_UP, Quests.TALK_TO_THE_BANKER, 2000).completed());
        assertEquals(5, Quests.skyBlockXp(profile));
        assertSame(Quests.TIME_TO_STRIKE, Quests.follow(profile, BANK, Quests.TIME_TO_STRIKE, false));
        // Back in the Bank: its own quest.
        assertSame(Quests.SAVING_UP, Quests.follow(profile, BANK, Quests.TIME_TO_STRIKE, true));
        assertSame(Quests.TIME_TO_STRIKE, Quests.follow(profile, VILLAGE, Quests.SAVING_UP, true));
        Quests.Step deposited = Quests.complete(profile, Quests.SAVING_UP, Quests.DEPOSIT_COINS, 3000);
        assertTrue(deposited.completed());
        assertNull(deposited.started());
        // The coins it gave aren't SkyBlock XP.
        assertEquals(5, Quests.skyBlockXp(profile));
        // Done, so the bar goes back to the Bartender.
        assertSame(Quests.TIME_TO_STRIKE, Quests.follow(profile, BANK, Quests.SAVING_UP, false));
        assertSame(Quests.SAVING_UP, Quests.byId("SAVING_UP"));
    }

    @Test
    void objectivesReadAsSentencesInTheLog() {
        assertEquals("Break a log.", Quests.GETTING_STARTED.objective(Quests.BREAK_A_LOG).logText());
        assertEquals("Talk to Farmer Rigby.", new Quest.Objective("Talk to Farmer Rigby.", true).logText());
        assertFalse(Quests.GETTING_STARTED.objective(Quests.TALK_TO_JERRY).linked());
        assertFalse(Quests.GETTING_STARTED.objective(Quests.USE_THE_TELEPORTER).linked());
        assertTrue(Quests.GETTING_STARTED.objective(Quests.CRAFT_A_WOODEN_PICKAXE).linked());
    }
}
