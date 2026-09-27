package net.icxd.dungeons.profile;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import org.bson.Document;

import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.utils.Utils;

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

    /** The Profile Management menu has room for five. */
    public static final int MAX_SLOTS = 5;
    /**
     * Deleted profiles kept for staff, newest last. Each keeps its items, and a document can't grow
     * past Mongo's 16 MB, so not every one is kept.
     */
    public static final int KEPT_DELETED = 10;
    /** Between making, switching and deleting profiles, shared by all three (Hypixel's, as seen in 2026). */
    public static final long COOLDOWN_MILLIS = 30_000;

    private static final ThreadLocal<DecimalFormat> COINS =
            ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.US)));

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

    /**
     * Takes a profile off the document into {@code deletedProfiles}, with its id and when, so staff can
     * still bring it back (the last {@link #KEPT_DELETED} of them).
     *
     * @return the profile, or null if there's none by that id
     */
    public static Document delete(Document doc, String id, long now) {
        if (!(profiles(doc).remove(id) instanceof Document profile)) return null;
        Document deleted = new Document("id", id).append("deletedAt", new Date(now));
        deleted.putAll(profile);
        List<Object> list = doc.get(DELETED) instanceof List<?> l ? new ArrayList<>(l) : new ArrayList<>();
        list.add(deleted);
        while (list.size() > KEPT_DELETED) list.removeFirst();
        doc.put(DELETED, list);
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

    // Names, slots, the cooldown

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

    /** How many profiles a rank may have: 2, a 3rd with VIP+, a 4th with MVP+ (the fandom wiki's), and all 5 for staff. */
    public static int slots(Rank rank) {
        if (rank.isEqualOrStrongerThan(Rank.STAFF)) return 5;
        if (rank.isEqualOrStrongerThan(Rank.MVP_PLUS)) return 4;
        if (rank.isEqualOrStrongerThan(Rank.VIP_PLUS)) return 3;
        return 2;
    }

    /** The lowest rank with profile slot {@code slot} (1 to 5). */
    public static Rank slotRank(int slot) {
        for (Rank rank : Rank.values()) {
            if (slots(rank) >= slot) return rank;
        }
        return Rank.STAFF;
    }

    /** Milliseconds until they may make, switch or delete a profile again; 0 if they may now. */
    public static long cooldownLeft(Document doc, long now) {
        long last = millis(doc.get(LAST_ACTION), 0);
        return last == 0 ? 0 : Math.max(0, last + COOLDOWN_MILLIS - now);
    }

    /** "00m21s", as Hypixel's menu counts it down (a second that has started counts). */
    public static String cooldown(long millis) {
        long seconds = (millis + 999) / 1000;
        return String.format(Locale.ROOT, "%02dm%02ds", seconds / 60, seconds % 60);
    }

    // What the menus say about a profile

    /**
     * "&25 months, 11 days": how old a profile is, coloured as Hypixel colours years and months
     * ({@code &5}, {@code &2}) and "Less than an hour" ({@code &f}). Days and hours are this plugin's
     * guesses in the same style: "3 days, 4 hours" ({@code &a}), "5 hours" ({@code &f}).
     */
    public static String age(long created, long now) {
        LocalDateTime from = LocalDateTime.ofInstant(Instant.ofEpochMilli(created), ZoneOffset.UTC);
        LocalDateTime to = LocalDateTime.ofInstant(Instant.ofEpochMilli(Math.max(created, now)), ZoneOffset.UTC);
        long months = ChronoUnit.MONTHS.between(from, to);
        if (months >= 12) return "&5" + count(months / 12, "year") + rest(months % 12, "month");
        if (months >= 1) return "&2" + count(months, "month") + rest(ChronoUnit.DAYS.between(from.plusMonths(months), to), "day");
        long hours = ChronoUnit.HOURS.between(from, to);
        if (hours >= 24) return "&a" + count(hours / 24, "day") + rest(hours % 24, "hour");
        if (hours >= 1) return "&f" + count(hours, "hour");
        return "&fLess than an hour";
    }

    private static String count(long n, String unit) {
        return n + " " + unit + (n == 1 ? "" : "s");
    }

    private static String rest(long n, String unit) {
        return n == 0 ? "" : ", " + count(n, unit);
    }

    /**
     * "&7Combat: &eLevel XXIV": their best three skills, highest first, or "&cNo skills yet!". Skills
     * are XP under {@code skills}, by the skill's name in lower case.
     */
    public static List<String> skillLines(Document profile) {
        record Level(Skill skill, int level) {
        }
        List<Level> levels = new ArrayList<>();
        Document skills = profile.get("skills") instanceof Document d ? d : new Document();
        for (Skill skill : Skill.values()) {
            int level = skills.get(skill.name().toLowerCase(Locale.ROOT)) instanceof Number xp ? Skill.getLevelFromXP(xp.intValue()) : 0;
            if (level > 0) levels.add(new Level(skill, level));
        }
        if (levels.isEmpty()) return List.of("&cNo skills yet!");
        levels.sort(Comparator.comparingInt(Level::level).reversed());
        return levels.stream().limit(3).map(l -> "&7" + l.skill().getName() + ": &eLevel " + Utils.getRomanNumeral(l.level())).toList();
    }

    /** "&7Bank Coins: &60" when the bank has any, then "&7Purse Coins: &679,208,878.1". */
    public static List<String> coinLines(Document profile) {
        double bank = profile.get("bank") instanceof Document b && b.get("balance") instanceof Number n ? n.doubleValue() : 0;
        double purse = profile.get("coins") instanceof Number n ? n.doubleValue() : 0;
        List<String> lines = new ArrayList<>();
        if (bank > 0) lines.add("&7Bank Coins: &6" + COINS.get().format(bank));
        lines.add("&7Purse Coins: &6" + COINS.get().format(purse));
        return lines;
    }

    // Who may use what

    /**
     * Whether a player may use a command that needs {@code needed}; a Sandbox tool ({@code sandbox})
     * is also free for anyone on a Sandbox profile.
     */
    public static boolean mayUse(Rank rank, ProfileMode mode, Rank needed, boolean sandbox) {
        return rank.isEqualOrStrongerThan(needed) || (sandbox && mode == ProfileMode.SANDBOX);
    }

    /** A time stored as a Date or as milliseconds; {@code otherwise} if it's neither. */
    static long millis(Object value, long otherwise) {
        if (value instanceof Date date) return date.getTime();
        if (value instanceof Number n) return n.longValue();
        return otherwise;
    }
}
