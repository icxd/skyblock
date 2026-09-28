package net.icxd.dungeons.collection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import net.icxd.dungeons.collection.CollectionData.Category;
import net.icxd.dungeons.collection.CollectionData.Collection;
import net.icxd.dungeons.collection.CollectionData.Reward;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * A profile's collections: how many of each collection's items it has collected, a whole number under
 * {@code collections.<id>} ("collections.BLAZE_ROD": 35), and each boss's kill count under
 * {@code bossCollections.<id>} ("bossCollections.CATACOMBS_1": 5); the tiers those reach, and the boss
 * rewards claimed under {@code bossCollectionRewards.<id>} (the tiers, a list). A profile that has none of
 * these has collected nothing. Functions on the profile's document, with no server needed; what collections
 * there are is {@link #data}, loaded when the plugin starts (none until then). Main thread for changes.
 */
public final class Collections {
    public static final String COUNTS = "collections";
    public static final String BOSS_COUNTS = "bossCollections";
    public static final String CLAIMED = "bossCollectionRewards";

    private static volatile CollectionData data = CollectionData.empty();

    private Collections() {
    }

    /** What collections there are (none before they're loaded, or without the private data). */
    public static CollectionData data() {
        return data;
    }

    static void setData(CollectionData loaded) {
        data = loaded == null ? CollectionData.empty() : loaded;
    }

    /**
     * Whether a profile of this mode gains collections. UNKNOWN: Sandbox profiles get their items from the
     * item browser and no owner decision covers collections, so only Normal profiles count for now.
     */
    public static boolean counts(ProfileMode mode) {
        return mode == ProfileMode.NORMAL;
    }

    // Reading

    /** How many of a collection's items (or a boss's kills) the profile has; 0 for none. */
    public static long count(Document profile, String id) {
        Collection collection = data.collection(id);
        String path = collection != null && collection.boss() ? BOSS_COUNTS : COUNTS;
        Document counts = profile == null ? null : profile.get(path) instanceof Document d ? d : null;
        return counts != null && counts.get(id) instanceof Number n ? Math.max(0, n.longValue()) : 0;
    }

    /** The tier this many reach: how many of its tiers' amounts it has (0 before the first). */
    public static int tier(Collection collection, long count) {
        int tier = 0;
        for (CollectionData.Tier t : collection.tiers()) {
            if (count < t.amount()) break;
            tier++;
        }
        return tier;
    }

    /** The tier a profile has reached in a collection or a boss's ("CATACOMBS_1"): 0 for none, or no such collection. */
    public static int tier(Document profile, String collectionId) {
        Collection collection = data.collection(collectionId);
        return collection == null ? 0 : tier(collection, count(profile, collectionId));
    }

    /** Every collection and boss collection the profile has reached a tier of, with that tier, in the menus' order. */
    public static Map<String, Integer> unlockedTiers(Document profile) {
        Map<String, Integer> tiers = new LinkedHashMap<>();
        List<Collection> all = new ArrayList<>(data.collections().values());
        all.addAll(data.bosses());
        for (Collection collection : all) {
            int tier = tier(collection, count(profile, collection.id()));
            if (tier > 0) tiers.put(collection.id(), tier);
        }
        return tiers;
    }

    /** The SkyBlock XP the profile's collection tiers give, as their rewards say ("+4 SkyBlock XP"). */
    public static int skyBlockXp(Document profile) {
        int xp = 0;
        for (Map.Entry<String, Integer> e : unlockedTiers(profile).entrySet()) {
            Collection collection = data.collection(e.getKey());
            for (int n = 1; n <= e.getValue(); n++) {
                for (Reward reward : collection.tier(n).rewards()) if (reward.type() == Reward.Type.SKYBLOCK_XP) xp += (int) reward.amount();
            }
        }
        return xp;
    }

    /** The stats the profile's collection tiers give for good (Obsidian V's "+1☘ Mining Fortune"). */
    public static Stats stats(Document profile) {
        Stats stats = new Stats();
        for (Collection collection : data.withStats()) {
            int tier = tier(collection, count(profile, collection.id()));
            for (int n = 1; n <= tier; n++) {
                for (Reward reward : collection.tier(n).rewards()) {
                    Stat stat = reward.type() == Reward.Type.STAT ? stat(reward.name()) : null;
                    if (stat != null) stats.add(stat, reward.amount());
                }
            }
        }
        return stats;
    }

    private static Stat stat(String name) {
        try {
            return name == null ? null : Stat.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Whether the profile has found the collection's item (or killed the boss) at least once. */
    public static boolean found(Document profile, String id) {
        return count(profile, id) > 0;
    }

    /** How many of a category's collections the profile has found. */
    public static int found(Document profile, Category category) {
        int found = 0;
        for (String id : category.collections()) if (found(profile, id)) found++;
        return found;
    }

    /** How many collections of the Collections menu's categories there are, and how many of them the profile has found. */
    public static int[] foundOfAll(Document profile) {
        int found = 0, all = 0;
        for (Category category : data.categories()) {
            found += found(profile, category);
            all += category.collections().size();
        }
        return new int[] {found, all};
    }

    /** The boss collections the profile has killed the boss of at least once, and how many there are. */
    public static int[] bossesFound(Document profile) {
        int found = 0;
        for (Collection boss : data.bosses()) if (found(profile, boss.id())) found++;
        return new int[] {found, data.bosses().size()};
    }

    /** The boss reward tiers the profile has claimed. */
    public static List<Integer> claimed(Document profile, String bossId) {
        Document claimed = profile == null ? null : profile.get(CLAIMED) instanceof Document d ? d : null;
        List<Integer> tiers = new ArrayList<>();
        if (claimed != null && claimed.get(bossId) instanceof List<?> list) {
            for (Object o : list) if (o instanceof Number n) tiers.add(n.intValue());
        }
        return tiers;
    }

    // Changing

    /** What adding to a collection did: the count before and after, and the tiers they reach. */
    public record Gain(Collection collection, long before, long after) {
        public int oldTier() {
            return tier(collection, before);
        }

        public int newTier() {
            return tier(collection, after);
        }

        public boolean leveledUp() {
            return newTier() > oldTier();
        }
    }

    /** Adds to a collection (or a boss's kills) on this profile; nothing for none or less, or no such collection. */
    public static Gain add(Document profile, String id, long amount) {
        Collection collection = data.collection(id);
        if (collection == null) return null;
        long before = count(profile, id);
        if (amount <= 0) return new Gain(collection, before, before);
        String path = collection.boss() ? BOSS_COUNTS : COUNTS;
        Document counts = profile.get(path) instanceof Document d ? d : null;
        if (counts == null) {
            counts = new Document();
            profile.put(path, counts);
        }
        long after = before + amount;
        counts.put(id, after);
        return new Gain(collection, before, after);
    }

    /** Sets a collection's count (staff's {@code /setcollection}); what that did, as a gain from what it was. */
    public static Gain set(Document profile, String id, long amount) {
        Collection collection = data.collection(id);
        if (collection == null) return null;
        long before = count(profile, id);
        String path = collection.boss() ? BOSS_COUNTS : COUNTS;
        Document counts = profile.get(path) instanceof Document d ? d : null;
        if (counts == null) {
            counts = new Document();
            profile.put(path, counts);
        }
        counts.put(id, Math.max(0, amount));
        return new Gain(collection, before, Math.max(0, amount));
    }

    /** Marks a boss reward tier as claimed. */
    public static void claim(Document profile, String bossId, int tier) {
        Document claimed = profile.get(CLAIMED) instanceof Document d ? d : null;
        if (claimed == null) {
            claimed = new Document();
            profile.put(CLAIMED, claimed);
        }
        List<Integer> tiers = claimed(profile, bossId);
        if (!tiers.contains(tier)) tiers.add(tier);
        claimed.put(bossId, tiers);
    }
}
