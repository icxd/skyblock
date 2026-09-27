package net.icxd.dungeons.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import org.bson.Document;

/**
 * SkyBlock profiles in a user document, as plain functions on it (no server needed). Everything a
 * server saves for a player stays in their one document, so it's still claimed, saved and handed
 * off whole: the account (rank, gems, settings) at the top, each profile under
 * {@code profiles.<id>}, and {@code selectedProfile} naming the one they play on.
 *
 * <pre>
 * profiles:          { "&lt;uuid&gt;": { name, mode, created, coins, bits, bank, skills, dungeons, ... } }
 * selectedProfile:   "&lt;uuid&gt;"
 * lastProfileAction: when they last made, switched or deleted one (the cooldown)
 * settings:          { autoReadyUp }
 * deletedProfiles:   [ { id, deletedAt, ...the profile } ]
 * </pre>
 */
public final class Profiles {
    public static final String PROFILES = "profiles";
    public static final String SELECTED = "selectedProfile";
    public static final String LAST_ACTION = "lastProfileAction";
    public static final String SETTINGS = "settings";
    public static final String DELETED = "deletedProfiles";
    public static final String NAME = "name";
    public static final String MODE = "mode";
    public static final String CREATED = "created";

    /** What a profile has of its own, which was at the top of the document before profiles. */
    public static final List<String> PROFILE_KEYS = List.of("coins", "bits", "skills", "dungeons", "crimsonIsle", "dwarvenMines", "bank",
            "storage", "minions");

    /** Hypixel's profile names, in the fandom wiki's order. */
    public static final List<String> NAMES = List.of("Apple", "Banana", "Blueberry", "Coconut", "Cucumber", "Grapes", "Kiwi", "Lemon",
            "Lime", "Mango", "Orange", "Papaya", "Pear", "Peach", "Pineapple", "Pomegranate", "Raspberry", "Strawberry", "Tomato",
            "Watermelon", "Zucchini");

    private Profiles() {
    }

    /** A profile and its id. */
    public record Entry(String id, Document profile) {
        public String name() {
            return profile.getString(NAME);
        }

        public ProfileMode mode() {
            return Profiles.mode(profile);
        }
    }

    // The document

    /**
     * Makes a document from before profiles into one with a single profile: everything a profile has
     * of its own moves into it, and auto ready up into the settings. That profile is a Sandbox one:
     * what players had then came mostly from the item browser. A document that has profiles is left
     * as it is.
     *
     * @return whether it changed
     */
    public static boolean migrate(Document doc, Random random, long now) {
        if (doc.containsKey(PROFILES)) return false;
        Document profile = new Document(NAME, pickName(List.of(), random)).append(MODE, ProfileMode.SANDBOX.name())
                .append(CREATED, new Date(millis(doc.get("firstLogin"), now)));
        for (String key : PROFILE_KEYS) {
            if (doc.containsKey(key)) profile.put(key, doc.remove(key));
        }
        if (profile.get("dungeons") instanceof Document dungeons && dungeons.containsKey("autoReadyUp")) {
            boolean auto = Boolean.TRUE.equals(dungeons.remove("autoReadyUp"));
            Document settings = doc.get(SETTINGS) instanceof Document d ? d : new Document();
            if (!settings.containsKey("autoReadyUp")) settings.put("autoReadyUp", auto);
            doc.put(SETTINGS, settings);
        }
        String id = UUID.randomUUID().toString();
        doc.put(PROFILES, new Document(id, profile));
        doc.put(SELECTED, id);
        return true;
    }

    /**
     * Keys added to the defaults since the document (or a profile) was made. Nested documents aren't
     * shared: each profile gets its own from {@code profile}.
     */
    public static Document withDefaults(Document doc, Document account, Supplier<Document> profile) {
        fill(doc, account);
        if (doc.get(PROFILES) instanceof Document profiles) {
            for (Object value : profiles.values()) {
                if (value instanceof Document p) fill(p, profile.get());
            }
        }
        return doc;
    }

    /**
     * Makes sure one of their profiles is selected: a document with none gets a new Normal one (a new
     * player, or someone whose profiles were all taken away), and a selection that isn't one of them
     * moves to the oldest.
     *
     * @return whether it changed
     */
    public static boolean repair(Document doc, Supplier<Document> defaults, Random random, long now) {
        List<Entry> profiles = ordered(doc);
        if (profiles.isEmpty()) {
            String id = UUID.randomUUID().toString();
            create(doc, id, pickName(List.of(), random), ProfileMode.NORMAL, defaults.get(), now);
            doc.put(SELECTED, id);
            return true;
        }
        if (selected(doc) != null) return false;
        doc.put(SELECTED, profiles.getFirst().id());
        return true;
    }

    /** Adds what {@code doc} doesn't have of {@code defaults} (a key set to null counts as had). */
    private static void fill(Document doc, Document defaults) {
        for (Map.Entry<String, Object> e : defaults.entrySet()) {
            if (!doc.containsKey(e.getKey())) doc.put(e.getKey(), e.getValue());
        }
    }

    /** Adds a profile (not selected yet) with the defaults' fields. */
    public static Document create(Document doc, String id, String name, ProfileMode mode, Document defaults, long now) {
        Document profile = new Document(NAME, name).append(MODE, mode.name()).append(CREATED, new Date(now));
        fill(profile, defaults);
        profiles(doc).put(id, profile);
        return profile;
    }

    /** The document's profiles by id, made empty if it has none. */
    public static Document profiles(Document doc) {
        if (doc.get(PROFILES) instanceof Document profiles) return profiles;
        Document profiles = new Document();
        doc.put(PROFILES, profiles);
        return profiles;
    }

    /** The profile they play on; null if the selection isn't one of theirs. */
    public static Document selected(Document doc) {
        Object id = doc.get(SELECTED);
        return id instanceof String s && doc.get(PROFILES) instanceof Document profiles && profiles.get(s) instanceof Document p ? p : null;
    }

    /** Oldest first, as the menu lists them (Hypixel's order). */
    public static List<Entry> ordered(Document doc) {
        List<Entry> out = new ArrayList<>();
        if (!(doc.get(PROFILES) instanceof Document profiles)) return out;
        for (Map.Entry<String, Object> e : profiles.entrySet()) {
            if (e.getValue() instanceof Document p) out.add(new Entry(e.getKey(), p));
        }
        out.sort(Comparator.comparingLong((Entry e) -> millis(e.profile().get(CREATED), Long.MAX_VALUE)).thenComparing(Entry::id));
        return out;
    }

    public static ProfileMode mode(Document profile) {
        return profile == null ? ProfileMode.NORMAL : ProfileMode.parse(profile.get(MODE));
    }

    // Names

    /** A random name none of {@code used} has (in any case); null when all are taken. */
    public static String pickName(Collection<String> used, Random random) {
        List<String> free = new ArrayList<>();
        for (String name : NAMES) {
            if (used.stream().noneMatch(name::equalsIgnoreCase)) free.add(name);
        }
        return free.isEmpty() ? null : free.get(random.nextInt(free.size()));
    }

    /** The names of their profiles now (deleted ones are free again). */
    public static List<String> names(Document doc) {
        return ordered(doc).stream().map(Entry::name).filter(n -> n != null).toList();
    }

    /** A time stored as a Date or as milliseconds; {@code otherwise} if it's neither. */
    static long millis(Object value, long otherwise) {
        if (value instanceof Date date) return date.getTime();
        if (value instanceof Number n) return n.longValue();
        return otherwise;
    }
}
