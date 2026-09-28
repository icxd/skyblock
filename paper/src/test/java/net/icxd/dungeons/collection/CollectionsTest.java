package net.icxd.dungeons.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** A profile's collections, on made-up data in collections.json's format: tiers, gains, boss kills, what counts. */
class CollectionsTest {
    static final String DATA = """
            {"format": 1,
             "categories": [{"id": "COMBAT", "name": "Combat", "collections": ["BLAZE_ROD", "ROTTEN_FLESH"]},
                            {"id": "MINING", "name": "Mining", "collections": ["MITHRIL_ORE"]}],
             "collections": {
               "BLAZE_ROD": {"name": "Blaze Rod", "item": "BLAZE_ROD", "tiers": [
                 {"amount": 50, "rewards": [{"type": "MINION_RECIPES", "name": "Blaze", "item": "BLAZE_GENERATOR_1"}, {"type": "SKYBLOCK_XP", "amount": 4}]},
                 {"amount": 250, "rewards": [{"type": "EXP_DISCOUNT", "name": "Fire Aspect", "percent": 25}, {"type": "SKYBLOCK_XP", "amount": 4}]},
                 {"amount": 1000, "rewards": [{"type": "RECIPE", "name": "Enchanted Blaze Powder", "item": "ENCHANTED_BLAZE_POWDER"}, {"type": "SKYBLOCK_XP", "amount": 4}]}]},
               "ROTTEN_FLESH": {"name": "Rotten Flesh", "item": "ROTTEN_FLESH", "tiers": [
                 {"amount": 50, "rewards": [{"type": "SKYBLOCK_XP", "amount": 4}]},
                 {"amount": 100, "rewards": [{"type": "SKILL_XP", "skill": "COMBAT", "amount": 10000}, {"type": "SKYBLOCK_XP", "amount": 4}]}]},
               "MITHRIL_ORE": {"name": "Mithril", "item": "MITHRIL_ORE", "tiers": [
                 {"amount": 50, "rewards": [{"type": "STAT", "name": "MINING_FORTUNE", "amount": 1, "text": "+1☘ Mining Fortune"}, {"type": "SKYBLOCK_XP", "amount": 4}]},
                 {"amount": 100, "rewards": [{"type": "STAT", "name": "MINING_FORTUNE", "amount": 2, "text": "+2☘ Mining Fortune"}, {"type": "STAT", "name": "NO_SUCH_STAT", "amount": 5, "text": "+5 Nothing"}]}]}},
             "bosses": [
               {"id": "CATACOMBS_1", "name": "Bonzo", "floor": 1, "texture": "12716ecbf5b8da00b05f316ec6af61e8bd02805b21eb8e440151468dc656549c", "tiers": [
                 {"amount": 25, "rewards": [{"type": "ITEM", "name": "Red Nose", "item": "RED_NOSE"}, {"type": "SKYBLOCK_XP", "amount": 15}]},
                 {"amount": 50, "rewards": [{"type": "ITEM", "name": "Bonzo's Mask", "item": "BONZO_MASK"}, {"type": "SKYBLOCK_XP", "amount": 15}]}]},
               {"id": "KUUDRA", "name": "Kuudra", "floor": 0, "tiers": [{"amount": 10, "rewards": [{"type": "SKYBLOCK_XP", "amount": 10}]}]}],
             "items": {"BLAZE_ROD": ["BLAZE_ROD", 1], "ENCHANTED_BLAZE_POWDER": ["BLAZE_ROD", 160], "ROTTEN_FLESH": ["ROTTEN_FLESH", 1],
                       "ENCHANTED_ROTTEN_FLESH": ["ROTTEN_FLESH", 160], "MITHRIL_ORE": ["MITHRIL_ORE", 1], "NOT_A_COLLECTION": ["NONE", 5]}}
            """;

    private CollectionData before;

    static CollectionData data() {
        return CollectionData.parse(JsonParser.parseString(DATA).getAsJsonObject());
    }

    @BeforeEach
    void useData() {
        before = Collections.data();
        Collections.setData(data());
    }

    @AfterEach
    void restore() {
        Collections.setData(before);
    }

    private static Document profile(ProfileMode mode) {
        return new Document(Profiles.MODE, mode.name());
    }

    @Test
    void readsTheFile() {
        CollectionData data = Collections.data();
        assertEquals(List.of(), data.problems());
        assertEquals(3, data.size());
        assertEquals(List.of("BLAZE_ROD", "ROTTEN_FLESH"), data.category("COMBAT").collections());
        assertEquals(3, data.collection("BLAZE_ROD").maxTier());
        assertTrue(data.collection("CATACOMBS_1").boss());
        assertEquals(new CollectionData.Counted("BLAZE_ROD", 160), data.counted("ENCHANTED_BLAZE_POWDER"));
        // An item toward a collection there isn't isn't counted at all.
        assertNull(data.counted("NOT_A_COLLECTION"));
    }

    @Test
    void tierFromCount() {
        CollectionData.Collection blaze = Collections.data().collection("BLAZE_ROD");
        assertEquals(0, Collections.tier(blaze, 0));
        assertEquals(0, Collections.tier(blaze, 49));
        assertEquals(1, Collections.tier(blaze, 50));
        assertEquals(1, Collections.tier(blaze, 249));
        assertEquals(2, Collections.tier(blaze, 250));
        assertEquals(3, Collections.tier(blaze, 1000));
        assertEquals(3, Collections.tier(blaze, 1_000_000));
    }

    @Test
    void addingCrossesTiers() {
        Document profile = profile(ProfileMode.NORMAL);
        Collections.Gain first = Collections.add(profile, "BLAZE_ROD", 35);
        assertEquals(0, first.before());
        assertEquals(35, first.after());
        assertFalse(first.leveledUp());
        // 160 more: past I and II at once.
        Collections.Gain second = Collections.add(profile, "BLAZE_ROD", 260);
        assertEquals(0, second.oldTier());
        assertEquals(2, second.newTier());
        assertTrue(second.leveledUp());
        assertEquals(295, Collections.count(profile, "BLAZE_ROD"));
        assertEquals(295L, profile.get(Collections.COUNTS, Document.class).get("BLAZE_ROD"));
        assertEquals(2, Collections.tier(profile, "BLAZE_ROD"));
        assertNull(Collections.add(profile, "NO_SUCH", 5));
        assertEquals(295, Collections.add(profile, "BLAZE_ROD", 0).after());
    }

    @Test
    void whatsUnlockedAndItsSkyBlockXp() {
        Document profile = profile(ProfileMode.NORMAL);
        Collections.add(profile, "BLAZE_ROD", 300);
        Collections.add(profile, "ROTTEN_FLESH", 10);
        Collections.add(profile, "CATACOMBS_1", 25);
        assertEquals(Map.of("BLAZE_ROD", 2, "CATACOMBS_1", 1), Collections.unlockedTiers(profile));
        // Blaze Rod I and II's 4 each, Bonzo I's 15.
        assertEquals(23, Collections.skyBlockXp(profile));
        assertEquals(2, Collections.found(profile, Collections.data().category("COMBAT")));
        assertEquals(List.of(2, 3), List.of(Collections.foundOfAll(profile)[0], Collections.foundOfAll(profile)[1]));
        assertEquals(1, Collections.bossesFound(profile)[0]);
        assertEquals(2, Collections.bossesFound(profile)[1]);
    }

    /** A tier's stat counts for good once it's reached (a stat the plugin doesn't have, not at all). */
    @Test
    void statsForGood() {
        Document profile = profile(ProfileMode.NORMAL);
        assertEquals(new Stats(), Collections.stats(profile));
        assertEquals(List.of("MITHRIL_ORE"), Collections.data().withStats().stream().map(CollectionData.Collection::id).toList());
        Collections.add(profile, "MITHRIL_ORE", 60);
        assertEquals(new Stats().set(Stat.MINING_FORTUNE, 1), Collections.stats(profile));
        Collections.add(profile, "MITHRIL_ORE", 40);
        assertEquals(new Stats().set(Stat.MINING_FORTUNE, 3), Collections.stats(profile));
    }

    /** A boss's kills are kept apart from the items', and a finished floor counts one (two in Master Mode). */
    @Test
    void bossKills() {
        Document profile = profile(ProfileMode.NORMAL);
        assertNull(CollectionGains.bossDefeated(profile, DungeonFloor.ENTRANCE));
        assertEquals(1, CollectionGains.bossDefeated(profile, DungeonFloor.FLOOR_1).after());
        assertEquals(3, CollectionGains.bossDefeated(profile, DungeonFloor.MASTER_FLOOR_1).after());
        assertEquals(3, Collections.count(profile, "CATACOMBS_1"));
        assertEquals(3L, profile.get(Collections.BOSS_COUNTS, Document.class).get("CATACOMBS_1"));
        assertNull(profile.get(Collections.COUNTS));
        // No boss collection for Floor II here.
        assertNull(CollectionGains.bossDefeated(profile, DungeonFloor.FLOOR_2));
    }

    /** Only Normal profiles collect (UNKNOWN for Sandbox ones, whose items come from the browser). */
    @Test
    void sandboxProfilesDontCollect() {
        assertTrue(Collections.counts(ProfileMode.NORMAL));
        assertFalse(Collections.counts(ProfileMode.SANDBOX));
        Document sandbox = profile(ProfileMode.SANDBOX);
        assertNull(CollectionGains.bossDefeated(sandbox, DungeonFloor.FLOOR_1));
        assertEquals(0, Collections.count(sandbox, "CATACOMBS_1"));
    }

    @Test
    void claimedRewards() {
        Document profile = profile(ProfileMode.NORMAL);
        assertEquals(List.of(), Collections.claimed(profile, "CATACOMBS_1"));
        Collections.claim(profile, "CATACOMBS_1", 1);
        Collections.claim(profile, "CATACOMBS_1", 1);
        Collections.claim(profile, "CATACOMBS_1", 2);
        assertEquals(List.of(1, 2), Collections.claimed(profile, "CATACOMBS_1"));
    }

    @Test
    void settingForStaff() {
        Document profile = profile(ProfileMode.NORMAL);
        Collections.set(profile, "ROTTEN_FLESH", 120);
        assertEquals(2, Collections.tier(profile, "ROTTEN_FLESH"));
        Collections.set(profile, "ROTTEN_FLESH", -5);
        assertEquals(0, Collections.count(profile, "ROTTEN_FLESH"));
    }

    /** Older saves, or hand edits, may hold ints or odd values: read as numbers, anything else as none. */
    @Test
    void oddValues() {
        Document profile = profile(ProfileMode.NORMAL).append(Collections.COUNTS, new Document("BLAZE_ROD", 60).append("ROTTEN_FLESH", "x"));
        assertEquals(60, Collections.count(profile, "BLAZE_ROD"));
        assertEquals(0, Collections.count(profile, "ROTTEN_FLESH"));
        assertEquals(0, Collections.count(null, "BLAZE_ROD"));
    }
}
