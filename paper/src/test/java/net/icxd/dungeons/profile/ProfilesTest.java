package net.icxd.dungeons.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.database.collections.UserCollection;

class ProfilesTest {
    private static final UserCollection USERS = new UserCollection();
    private static final long NOW = 1_790_000_000_000L;

    /** A user document as it was saved before profiles. */
    private static Document legacy() {
        Document storage = new Document("inventory", new ArrayList<>(List.of(new Binary(new byte[]{1, 2, 3}))))
                .append("armor", new ArrayList<>()).append("offhand", new ArrayList<>()).append("heldSlot", 4).append("dataVersion", 4671);
        return new Document("uuid", "0f8b...").append("username", "Old").append("ip", "127.0.0.1").append("rank", "DEFAULT")
                .append("coins", 1234).append("bits", 56).append("gems", 7)
                .append("skills", new Document("combat", 5000))
                .append("dungeons", new Document("essence", new Document("wither", 3)).append("floors", new Document("highest", 4))
                        .append("classExp", new Document("MAGE", 100)).append("selectedClass", "MAGE").append("autoReadyUp", true))
                .append("crimsonIsle", new Document("kuudra", new Document("highest", 2)))
                .append("dwarvenMines", new Document("hotm", new Document("tokens", 3)))
                .append("bank", new Document("balance", 900).append("transactions", new ArrayList<>()))
                .append("storage", storage)
                .append("minions", new Document("minions", new ArrayList<>()))
                .append("firstLogin", 1_700_000_000_000L).append("lastLogin", 1_700_000_500_000L)
                .append("session", new Document("server", "hub01"));
    }

    @Test
    void aDocumentFromBeforeProfilesBecomesOneSandboxProfile() {
        Document doc = legacy();
        Document before = legacy();
        assertTrue(Profiles.migrate(doc, new Random(1), NOW));

        List<Profiles.Entry> profiles = Profiles.ordered(doc);
        assertEquals(1, profiles.size());
        Profiles.Entry entry = profiles.getFirst();
        assertEquals(entry.id(), doc.getString(Profiles.SELECTED));
        assertSame(entry.profile(), Profiles.selected(doc));
        assertEquals(ProfileMode.SANDBOX, entry.mode());
        assertTrue(Profiles.NAMES.contains(entry.name()));
        assertEquals(new Date(1_700_000_000_000L), entry.profile().getDate(Profiles.CREATED));

        // Every profile field moved, as it was (the stored items too); none left at the top.
        for (String key : Profiles.PROFILE_KEYS) {
            assertFalse(doc.containsKey(key), key + " left at the top");
            assertTrue(entry.profile().containsKey(key), key + " not moved");
        }
        assertEquals(1234, entry.profile().get("coins"));
        assertEquals(56, entry.profile().get("bits"));
        assertEquals(5000, entry.profile().get("skills", Document.class).get("combat"));
        assertEquals(900, entry.profile().get("bank", Document.class).get("balance"));
        assertEquals(before.get("storage", Document.class).keySet(), entry.profile().get("storage", Document.class).keySet());
        Binary blob = (Binary) entry.profile().get("storage", Document.class).getList("inventory", Object.class).getFirst();
        assertEquals(3, blob.length());
        Document dungeons = entry.profile().get("dungeons", Document.class);
        assertEquals("MAGE", dungeons.getString("selectedClass"));
        assertEquals(4, dungeons.get("floors", Document.class).get("highest"));

        // Auto ready up is the account's.
        assertFalse(dungeons.containsKey("autoReadyUp"));
        assertEquals(true, doc.get(Profiles.SETTINGS, Document.class).get("autoReadyUp"));
        // The account stays where it was.
        for (String key : List.of("uuid", "username", "ip", "rank", "gems", "firstLogin", "lastLogin", "session")) {
            assertEquals(before.get(key), doc.get(key), key);
        }
    }

    @Test
    void migratingTwiceChangesNothing() {
        Document doc = legacy();
        Profiles.migrate(doc, new Random(1), NOW);
        String json = doc.toJson();
        assertFalse(Profiles.migrate(doc, new Random(2), NOW + 1000));
        assertEquals(json, doc.toJson());
    }

    /** What the bots' seeding leaves: the uuid, name and rank, nothing else. It's still someone who played before. */
    @Test
    void aBareDocumentGetsAnEmptySandboxProfile() {
        Document doc = new Document("uuid", "x").append("username", "Bot").append("rank", "STAFF");
        assertTrue(Profiles.migrate(doc, new Random(1), NOW));
        Profiles.withDefaults(doc, USERS.defaultDocument(), USERS::profileDefaults);
        Document profile = Profiles.selected(doc);
        assertEquals(ProfileMode.SANDBOX, Profiles.mode(profile));
        assertEquals(0, profile.get("coins"));
        assertEquals(new Date(NOW), profile.getDate(Profiles.CREATED));
        assertEquals(false, doc.get(Profiles.SETTINGS, Document.class).get("autoReadyUp"));
    }

    @Test
    void aNewPlayerGetsOneNormalProfile() {
        Document doc = USERS.defaultDocument().append("uuid", "new");
        assertFalse(doc.containsKey("coins"), "profile fields aren't account defaults");
        assertTrue(Profiles.repair(doc, USERS::profileDefaults, new Random(1), NOW));
        List<Profiles.Entry> profiles = Profiles.ordered(doc);
        assertEquals(1, profiles.size());
        assertEquals(ProfileMode.NORMAL, profiles.getFirst().mode());
        assertEquals(profiles.getFirst().id(), doc.getString(Profiles.SELECTED));
        assertEquals(0, profiles.getFirst().profile().get("coins"));
        assertNull(profiles.getFirst().profile().get("storage", Document.class).get("inventory"));
        // And it's left alone from then on.
        assertFalse(Profiles.migrate(doc, new Random(1), NOW));
        assertFalse(Profiles.repair(doc, USERS::profileDefaults, new Random(1), NOW));
    }

    @Test
    void aSelectionThatIsntAProfileMovesToTheOldest() {
        Document doc = new Document();
        Profiles.create(doc, "b", "Banana", ProfileMode.NORMAL, new Document(), NOW);
        Profiles.create(doc, "a", "Apple", ProfileMode.NORMAL, new Document(), NOW - 1000);
        doc.put(Profiles.SELECTED, "gone");
        assertTrue(Profiles.repair(doc, USERS::profileDefaults, new Random(1), NOW));
        assertEquals("a", doc.getString(Profiles.SELECTED));
        assertEquals(List.of("Apple", "Banana"), Profiles.names(doc));
    }

    @Test
    void defaultsFillTheAccountAndEachProfile() {
        Document doc = new Document("rank", "STAFF").append("gems", 12);
        Profiles.create(doc, "a", "Apple", ProfileMode.NORMAL, new Document("coins", 5), NOW);
        Profiles.create(doc, "b", "Banana", ProfileMode.SANDBOX, new Document(), NOW);
        doc.put(Profiles.SELECTED, "a");
        Profiles.withDefaults(doc, USERS.defaultDocument(), USERS::profileDefaults);

        assertEquals("STAFF", doc.get("rank"), "kept");
        assertEquals(12, doc.get("gems"), "kept");
        assertTrue(doc.containsKey(Profiles.SETTINGS) && doc.containsKey(Profiles.DELETED) && doc.containsKey(Profiles.LAST_ACTION));
        assertFalse(doc.containsKey("coins"), "no profile fields at the top");
        Document a = Profiles.profiles(doc).get("a", Document.class);
        Document b = Profiles.profiles(doc).get("b", Document.class);
        assertEquals(5, a.get("coins"), "kept");
        assertEquals(0, b.get("coins"));
        for (String key : USERS.profileDefaults().keySet()) {
            assertTrue(a.containsKey(key) && b.containsKey(key), key);
        }
        // Each has its own.
        assertNotSame(a.get("bank"), b.get("bank"));
        assertEquals("Apple", a.getString(Profiles.NAME));
        assertEquals("SANDBOX", b.getString(Profiles.MODE));
    }

    @Test
    void aDeletedProfileIsKeptAside() {
        Document doc = new Document();
        Profiles.create(doc, "a", "Apple", ProfileMode.NORMAL, new Document("coins", 9), NOW);
        Profiles.create(doc, "b", "Banana", ProfileMode.NORMAL, new Document(), NOW);
        assertNotNull(Profiles.delete(doc, "a", NOW + 5));
        assertNull(Profiles.delete(doc, "a", NOW + 6));
        assertEquals(List.of("Banana"), Profiles.names(doc));
        Document deleted = (Document) doc.getList(Profiles.DELETED, Object.class).getFirst();
        assertEquals("a", deleted.get("id"));
        assertEquals(new Date(NOW + 5), deleted.get("deletedAt"));
        assertEquals("Apple", deleted.get(Profiles.NAME));
        assertEquals(9, deleted.get("coins"));

        // Only the last ten.
        for (int i = 0; i < 12; i++) {
            Profiles.create(doc, "p" + i, "Kiwi", ProfileMode.NORMAL, new Document(), NOW);
            Profiles.delete(doc, "p" + i, NOW + i);
        }
        List<Object> kept = doc.getList(Profiles.DELETED, Object.class);
        assertEquals(Profiles.KEPT_DELETED, kept.size());
        assertEquals("p2", ((Document) kept.getFirst()).get("id"));
        assertEquals("p11", ((Document) kept.getLast()).get("id"));
    }

    @Test
    void namesAreFruitsNotInUse() {
        assertEquals(21, Profiles.NAMES.size());
        assertEquals(21, new HashSet<>(Profiles.NAMES).size());
        Random random = new Random(5);
        List<String> used = List.of("banana", "Zucchini", "PINEAPPLE");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String name = Profiles.pickName(used, random);
            assertTrue(Profiles.NAMES.contains(name));
            assertFalse(used.stream().anyMatch(name::equalsIgnoreCase), name);
            seen.add(name);
        }
        assertEquals(18, seen.size(), "every free one comes up");

        List<String> all = new ArrayList<>(Profiles.NAMES);
        String last = all.remove(7);
        assertEquals(last, Profiles.pickName(all, random));
        all.add(last);
        assertNull(Profiles.pickName(all, random), "none left");
    }

    @Test
    void slotsComeWithRanks() {
        assertEquals(2, Profiles.slots(Rank.DEFAULT));
        assertEquals(3, Profiles.slots(Rank.VIP_PLUS));
        assertEquals(4, Profiles.slots(Rank.MVP_PLUS));
        assertEquals(4, Profiles.slots(Rank.MVP_PLUS_PLUS));
        assertEquals(4, Profiles.slots(Rank.YOUTUBE));
        assertEquals(5, Profiles.slots(Rank.STAFF));
        assertEquals(Rank.DEFAULT, Profiles.slotRank(2));
        assertEquals(Rank.VIP_PLUS, Profiles.slotRank(3));
        assertEquals(Rank.MVP_PLUS, Profiles.slotRank(4));
        assertEquals(Rank.STAFF, Profiles.slotRank(5));
    }

    private static long at(int year, int month, int day, int hour) {
        return LocalDateTime.of(year, month, day, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Test
    void ages() {
        long created = at(2026, 1, 10, 12);
        assertEquals("&fLess than an hour", Profiles.age(created, created));
        assertEquals("&fLess than an hour", Profiles.age(created, created + 59 * 60_000));
        assertEquals("&fLess than an hour", Profiles.age(created, created - 5000), "a clock behind");
        assertEquals("&f1 hour", Profiles.age(created, at(2026, 1, 10, 13)));
        assertEquals("&f23 hours", Profiles.age(created, at(2026, 1, 11, 11)));
        assertEquals("&a1 day", Profiles.age(created, at(2026, 1, 11, 12)));
        assertEquals("&a3 days, 4 hours", Profiles.age(created, at(2026, 1, 13, 16)));
        assertEquals("&a30 days, 23 hours", Profiles.age(created, at(2026, 2, 10, 11)));
        assertEquals("&21 month", Profiles.age(created, at(2026, 2, 10, 12)));
        assertEquals("&25 months, 11 days", Profiles.age(created, at(2026, 6, 21, 12)));
        assertEquals("&211 months, 1 day", Profiles.age(created, at(2026, 12, 11, 13)));
        assertEquals("&51 year", Profiles.age(created, at(2027, 1, 10, 12)));
        assertEquals("&54 years, 7 months", Profiles.age(created, at(2030, 8, 20, 0)));
    }

    @Test
    void theCooldownIsSharedAndCountsDown() {
        Document doc = new Document();
        assertEquals(0, Profiles.cooldownLeft(doc, NOW), "never did anything");
        doc.put(Profiles.LAST_ACTION, new Date(NOW));
        assertEquals(30_000, Profiles.cooldownLeft(doc, NOW));
        assertEquals(21_000, Profiles.cooldownLeft(doc, NOW + 9_000));
        assertEquals(0, Profiles.cooldownLeft(doc, NOW + 30_000));
        assertEquals("00m21s", Profiles.cooldown(21_000));
        assertEquals("00m21s", Profiles.cooldown(20_001));
        assertEquals("00m30s", Profiles.cooldown(30_000));
        assertEquals("01m05s", Profiles.cooldown(65_000));
    }

    @Test
    void sandboxToolsAreForSandboxProfilesOrStaff() {
        // A Sandbox tool (/item): anyone on a Sandbox profile, staff anywhere.
        assertFalse(Profiles.mayUse(Rank.DEFAULT, ProfileMode.NORMAL, Rank.STAFF, true));
        assertFalse(Profiles.mayUse(Rank.MVP_PLUS_PLUS, ProfileMode.NORMAL, Rank.STAFF, true));
        assertTrue(Profiles.mayUse(Rank.DEFAULT, ProfileMode.SANDBOX, Rank.STAFF, true));
        assertTrue(Profiles.mayUse(Rank.STAFF, ProfileMode.NORMAL, Rank.STAFF, true));
        assertTrue(Profiles.mayUse(Rank.STAFF, ProfileMode.SANDBOX, Rank.STAFF, true));
        // A staff command that isn't one (/playerdata, /dungeon): staff only, whatever the profile.
        assertFalse(Profiles.mayUse(Rank.DEFAULT, ProfileMode.SANDBOX, Rank.STAFF, false));
        assertTrue(Profiles.mayUse(Rank.STAFF, ProfileMode.NORMAL, Rank.STAFF, false));
        // Everyone's commands.
        assertTrue(Profiles.mayUse(Rank.DEFAULT, ProfileMode.NORMAL, Rank.DEFAULT, false));
        assertTrue(Profiles.sandboxTools(Rank.DEFAULT, ProfileMode.SANDBOX));
        assertTrue(Profiles.sandboxTools(Rank.STAFF, ProfileMode.NORMAL));
        assertFalse(Profiles.sandboxTools(Rank.YOUTUBE, ProfileMode.NORMAL));
    }

    @Test
    void theModeIsNormalUnlessItSaysSandbox() {
        assertEquals(ProfileMode.SANDBOX, ProfileMode.parse("SANDBOX"));
        assertEquals(ProfileMode.SANDBOX, ProfileMode.parse("sandbox"));
        assertEquals(ProfileMode.NORMAL, ProfileMode.parse("NORMAL"));
        assertEquals(ProfileMode.NORMAL, ProfileMode.parse(null));
        assertEquals(ProfileMode.NORMAL, ProfileMode.parse("IRONMAN"));
        assertEquals(ProfileMode.NORMAL, Profiles.mode(null));
    }

    @Test
    void skillAndCoinLines() {
        assertEquals(List.of("&cNo skills yet!"), Profiles.skillLines(USERS.profileDefaults()));
        assertEquals(List.of("&cNo skills yet!"), Profiles.skillLines(new Document()));
        assertEquals(List.of("&7Combat: &eLevel XII"), Profiles.skillLines(new Document("skills", new Document("combat", 23_000))));

        assertEquals(List.of("&7Purse Coins: &60"), Profiles.coinLines(USERS.profileDefaults()));
        assertEquals(List.of("&7Bank Coins: &61,500", "&7Purse Coins: &679,208,878.1"),
                Profiles.coinLines(new Document("coins", 79_208_878.1).append("bank", new Document("balance", 1500))));
    }
}
