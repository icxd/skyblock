package net.icxd.dungeons.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.database.collections.UserCollection;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.profile.Profiles;

class PurseTest {
    private static final UserCollection USERS = new UserCollection();

    @Test
    void addsAndTakes() {
        Document profile = USERS.profileDefaults();
        assertEquals(0, Purse.coins(profile));
        Purse.add(profile, 1_000);
        Purse.add(profile, 20);
        assertEquals(1_020, Purse.coins(profile));
        assertTrue(Purse.has(profile, 1_020));
        assertFalse(Purse.has(profile, 1_020.1));
        assertTrue(Purse.take(profile, 1_000));
        assertEquals(20, Purse.coins(profile));
        assertTrue(Purse.take(profile, 20));
        assertEquals(0, Purse.coins(profile));
    }

    @Test
    void takesNothingItDoesntHave() {
        Document profile = new Document("coins", 50.0);
        assertFalse(Purse.take(profile, 50.5));
        assertEquals(50.0, profile.get("coins"), "all or nothing");
        assertTrue(Purse.take(profile, 0));
    }

    @Test
    void refusesNegativeAndNonsenseAmounts() {
        Document profile = new Document("coins", 50.0);
        assertThrows(IllegalArgumentException.class, () -> Purse.add(profile, -1));
        assertThrows(IllegalArgumentException.class, () -> Purse.take(profile, -1));
        assertThrows(IllegalArgumentException.class, () -> Purse.add(profile, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Purse.add(profile, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Purse.has(profile, Double.NaN));
        assertEquals(50.0, profile.get("coins"));
    }

    @Test
    void keepsFractionsExact() {
        Document profile = new Document("coins", 0.0);
        for (int i = 0; i < 10; i++) Purse.add(profile, 0.1);
        assertEquals(1.0, Purse.coins(profile));
        // Three Torches, at 0.3 each.
        Purse.add(profile, 0.9);
        assertEquals(1.9, Purse.coins(profile));
        assertTrue(Purse.take(profile, 0.2));
        assertEquals(1.7, Purse.coins(profile));
    }

    @Test
    void readsAWholeNumberPurseAndWritesADouble() {
        Document profile = new Document("coins", 1234);
        assertEquals(1234, Purse.coins(profile));
        Purse.add(profile, 0.5);
        assertEquals(1234.5, profile.get("coins"));
        assertEquals(0, Purse.coins(new Document()), "no purse yet: empty");
        assertEquals(0, Purse.coins((Document) null));
    }

    @Test
    void migrationMakesEveryPurseADouble() {
        Document doc = new Document();
        Profiles.create(doc, "a", "Apple", ProfileMode.NORMAL, new Document("coins", 1234), 1);
        Profiles.create(doc, "b", "Banana", ProfileMode.SANDBOX, new Document("coins", 7L), 2);
        Profiles.create(doc, "c", "Coconut", ProfileMode.NORMAL, new Document("coins", 2.5), 3);
        assertTrue(Purse.migrate(doc));
        assertEquals(1234.0, Profiles.profiles(doc).get("a", Document.class).get("coins"));
        assertEquals(7.0, Profiles.profiles(doc).get("b", Document.class).get("coins"));
        assertEquals(2.5, Profiles.profiles(doc).get("c", Document.class).get("coins"));
        assertFalse(Purse.migrate(doc), "once");
    }

    @Test
    void aPlayerFromBeforeProfilesKeepsTheirCoins() {
        Document doc = new Document("uuid", "x").append("coins", 57_690_425);
        Profiles.migrate(doc, new Random(1), 0);
        Purse.migrate(doc);
        assertEquals(57_690_425.0, Profiles.selected(doc).get("coins"));
    }

    @Test
    void eachProfileHasItsOwn() {
        Document doc = new Document();
        Document a = Profiles.create(doc, "a", "Apple", ProfileMode.NORMAL, USERS.profileDefaults(), 1);
        Document b = Profiles.create(doc, "b", "Banana", ProfileMode.SANDBOX, USERS.profileDefaults(), 2);
        Purse.add(a, 5);
        assertEquals(5, Purse.coins(a));
        assertEquals(0, Purse.coins(b));
    }
}
