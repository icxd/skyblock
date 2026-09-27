package net.icxd.dungeons.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.cost.essence.EssenceType;

class EssencesTest {
    /** The wiki's rows: Crypt Undead 1 + 1 (50%); Lost Adventurer and Angry Archaeologist 3 + 1 (70%) + 1 (50%). */
    @Test
    void rowsAsTheWikiHasThem() {
        assertEquals(List.of(new Essences.Roll(EssenceType.UNDEAD, 1, 100), new Essences.Roll(EssenceType.UNDEAD, 1, 50)),
                Essences.rolls("CRYPT_UNDEAD"));
        assertEquals(3, Essences.rolls("LOST_ADVENTURER").size());
        assertEquals(EssenceType.DIAMOND, Essences.rolls("ANGRY_ARCHAEOLOGIST").get(0).type());
        assertTrue(Essences.rolls("ZOMBIE_GRUNT").isEmpty());
    }

    @Test
    void theSureOnesAlwaysCome() {
        Random random = new Random(2);
        int both = 0;
        for (int i = 0; i < 2_000; i++) {
            List<Essences.Roll> got = Essences.roll(Essences.rolls("CRYPT_UNDEAD"), random);
            assertEquals(1, got.get(0).amount());
            if (got.size() == 2) both++;
        }
        assertEquals(1_000, both, 100);
    }

    /** "&d+1 Undead Essence" (R1 01:21.9). */
    @Test
    void actionBar() {
        assertEquals("&d+1 Undead Essence", Essences.actionBar(EssenceType.UNDEAD, 1));
        assertEquals("&d+3 Dragon Essence", Essences.actionBar(EssenceType.DRAGON, 3));
    }

    /** Onto the profile's dungeons.essence, as UserCollection keeps it. */
    @Test
    void onTheProfile() {
        Document profile = new Document("dungeons", new Document("essence", new Document("undead", 4)));
        Essences.add(profile, EssenceType.UNDEAD, 2);
        assertEquals(6, Essences.essence(profile, EssenceType.UNDEAD));
        Document empty = new Document();
        Essences.add(empty, EssenceType.DIAMOND, 3);
        assertEquals(3, Essences.essence(empty, EssenceType.DIAMOND));
        assertEquals(0, Essences.essence(empty, EssenceType.DRAGON));
    }
}
