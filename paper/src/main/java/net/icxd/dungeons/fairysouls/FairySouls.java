package net.icxd.dungeons.fairysouls;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import net.icxd.dungeons.fairysouls.FairySoulTable.Soul;

/**
 * A profile's Fairy Souls ({@code fairySouls} on it): the ones found here, by id ("hub:-233,86,84"), how many were
 * found where this plugin can't say which (/yoinkprofile's: the API counts them, not where), and how many times
 * Tia took five, as the API keeps them ({@code fairy_soul.total_collected} and {@code fairy_exchanges}). A soul is
 * found once a profile; exchanged ones stay found. No server needed.
 */
public final class FairySouls {
    public static final String FIELD = "fairySouls";
    static final String FOUND = "found";
    static final String ELSEWHERE = "elsewhere";
    static final String EXCHANGES = "exchanges";
    /** The Fairy Soul's head (textures.minecraft.net), recorded on the souls and the Quest Log's entry. */
    public static final String HEAD = "299ea120bd83d0c81a3c4627f5bce1b12fb03bcb57779c63dcc77e3f4ae8a793";
    /** Hypixel's, as the Quest Log says it ("Found: 0/289", recorded). */
    public static final int TOTAL = 289;
    /** "Fairy Souls may be exchanged in groups of 5" (the wiki's Fairy Souls). */
    public static final int PER_EXCHANGE = 5;
    /** The exchanges that unlock a backpack slot (the wiki's Fairy Souls): the first unlocks slot 2, the last slot 18. */
    static final int[] BACKPACK_EXCHANGES = {1, 2, 4, 6, 8, 10, 13, 16, 19, 22, 25, 28, 31, 34, 37, 40, 44};

    private FairySouls() {
    }

    /** Whether the profile has found this one. */
    public static boolean found(Document profile, Soul soul) {
        return ids(profile).contains(soul.id());
    }

    /** Finds it, if the profile hasn't yet: whether it's new. */
    public static boolean find(Document profile, Soul soul) {
        Document souls = souls(profile);
        List<String> found = ids(profile);
        if (found.contains(soul.id())) return false;
        found = new ArrayList<>(found);
        found.add(soul.id());
        souls.put(FOUND, found);
        return true;
    }

    /** How many it has found, wherever (no more than there are). */
    public static int found(Document profile) {
        return Math.min(TOTAL, ids(profile).size() + number(profile, ELSEWHERE));
    }

    /** How many of an island's it has found (NEU's key, "hub"); those found elsewhere are on no island. */
    public static int found(Document profile, String island) {
        int found = 0;
        for (String id : ids(profile)) if (id.startsWith(island + ":")) found++;
        return found;
    }

    public static int exchanges(Document profile) {
        return number(profile, EXCHANGES);
    }

    /** The souls found and not given to Tia yet. */
    public static int unspent(Document profile) {
        return Math.max(0, found(profile) - exchanges(profile) * PER_EXCHANGE);
    }

    /** Tia takes five, if it has them ("Tia will still only take five at a time", the wiki's Tia): whether she did. */
    public static boolean exchange(Document profile) {
        if (unspent(profile) < PER_EXCHANGE) return false;
        souls(profile).put(EXCHANGES, exchanges(profile) + 1);
        return true;
    }

    /** The backpack slot this exchange (from 1) unlocks, 0 for none. */
    public static int backpackSlot(int exchange) {
        for (int i = 0; i < BACKPACK_EXCHANGES.length; i++) if (BACKPACK_EXCHANGES[i] == exchange) return i + 2;
        return 0;
    }

    /** /yoinkprofile's: how many were found and exchanged, none of them here. */
    public static void yoinked(Document profile, int collected, int exchanges) {
        profile.put(FIELD, new Document(FOUND, new ArrayList<String>()).append(ELSEWHERE, Math.max(0, Math.min(TOTAL, collected)))
                .append(EXCHANGES, Math.max(0, Math.min(exchanges, collected / PER_EXCHANGE))));
    }

    private static Document souls(Document profile) {
        if (profile.get(FIELD) instanceof Document souls) return souls;
        Document souls = new Document();
        profile.put(FIELD, souls);
        return souls;
    }

    private static List<String> ids(Document profile) {
        List<String> ids = new ArrayList<>();
        if (profile.get(FIELD) instanceof Document souls && souls.get(FOUND) instanceof List<?> list) {
            for (Object id : list) if (id instanceof String s) ids.add(s);
        }
        return ids;
    }

    private static int number(Document profile, String key) {
        return profile.get(FIELD) instanceof Document souls && souls.get(key) instanceof Number n ? Math.max(0, n.intValue()) : 0;
    }
}
