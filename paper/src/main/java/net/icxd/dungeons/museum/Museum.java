package net.icxd.dungeons.museum;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.icxd.dungeons.museum.MuseumData.Category;
import net.icxd.dungeons.museum.MuseumData.Entry;

/**
 * A profile's Museum: what it has donated, as Hypixel's museum API keeps it (an entry's items, when it was donated, and
 * whether they've been retrieved since: "borrowing"), under {@code museum.donations} by entry id, with the menus' sort
 * and Extra Details toggle. An entry counts towards the Museum's progress and its SkyBlock XP once donated, retrieved
 * or not ("It still counts towards your Museum progress"), or once a higher tier that counts as it does ("Donating a
 * Yellow Belt will count as donating this item."). The data ({@link MuseumData}) is read off the main thread when the
 * plugin starts. What counts needs no server.
 */
public final class Museum {
    static final String FIELD = "museum";
    static final String DONATIONS = "donations";
    static final String AT = "at";
    static final String RETRIEVED = "retrieved";
    static final String ITEMS = "items";
    static final String SORT = "sort";
    static final String DETAILS = "details";
    /** "Every 100 SkyBlock XP obtained from your Museum, Eleanor will reward you." */
    public static final int MILESTONE_XP = 100;
    /** "Milestone: 0/40". */
    public static final int MILESTONES = 40;
    /** How far up a chain of higher tiers counts (they're two or three long; a loop in the data mustn't hang). */
    private static final int MOST_TIERS = 16;

    private static volatile MuseumData data = MuseumData.empty();

    private Museum() {
    }

    /** Reads the data off the main thread. Once, when the plugin starts. */
    public static void start(JavaPlugin plugin) {
        Logger log = plugin.getLogger();
        Path folder = plugin.getDataFolder().toPath();
        CompletableFuture.supplyAsync(() -> MuseumData.load(folder)).thenAccept(loaded -> Bukkit.getScheduler().runTask(plugin, () -> {
            loaded.problems().forEach(p -> log.warning("Museum: " + p));
            data = loaded;
            if (loaded.count() > 0) {
                log.info("Museum: " + loaded.count() + " entries in " + loaded.categories().size() + " categories, "
                        + loaded.totalXp() + " SkyBlock XP, " + loaded.milestones().size() + " milestones");
            }
        }));
    }

    public static MuseumData data() {
        return data;
    }

    /** For tests: the data they count with. */
    static void data(MuseumData loaded) {
        data = loaded;
    }

    // Donations

    /**
     * One entry's donation: when, whether it's been retrieved, and its items as stored slots (still kept once
     * retrieved, to show; not given out again).
     */
    public record Donation(long at, boolean retrieved, List<?> items) {
    }

    /** The profile's {@code museum} (made if it has none). */
    static Document part(Document profile) {
        Document museum = profile.get(FIELD, Document.class);
        if (museum == null) {
            museum = new Document();
            profile.put(FIELD, museum);
        }
        return museum;
    }

    private static Document donations(Document profile, boolean make) {
        Document museum = make ? part(profile) : profile.get(FIELD, Document.class);
        if (museum == null) return null;
        Document donations = museum.get(DONATIONS, Document.class);
        if (donations == null && make) {
            donations = new Document();
            museum.put(DONATIONS, donations);
        }
        return donations;
    }

    /** Its donation of this entry; null if it never donated it. */
    public static Donation donation(Document profile, String entry) {
        Document donations = profile == null ? null : donations(profile, false);
        Document one = donations == null ? null : donations.get(entry, Document.class);
        if (one == null) return null;
        long at = one.get(AT) instanceof Number n ? n.longValue() : 0;
        List<?> items = one.get(ITEMS) instanceof List<?> list ? list : List.of();
        return new Donation(at, Boolean.TRUE.equals(one.get(RETRIEVED)), items);
    }

    /** Whether it has donated this entry (and maybe retrieved it since). */
    public static boolean donated(Document profile, String entry) {
        return donation(profile, entry) != null;
    }

    /** Whether the entry counts: donated, or a higher tier that counts as it does. */
    public static boolean counts(Document profile, MuseumData data, Entry entry) {
        for (int i = 0; entry != null && i < MOST_TIERS; i++) {
            if (donated(profile, entry.id())) return true;
            entry = entry.parent() == null ? null : data.entry(entry.parent());
        }
        return false;
    }

    /** Whether it counts only by a higher tier's donation. */
    public static boolean credited(Document profile, MuseumData data, Entry entry) {
        return !donated(profile, entry.id()) && counts(profile, data, entry);
    }

    /** The SkyBlock XP its Museum gives: every entry that counts. */
    public static int xp(Document profile, MuseumData data) {
        int xp = 0;
        for (Category category : data.categories()) for (Entry entry : category.entries()) if (counts(profile, data, entry)) xp += entry.xp();
        return xp;
    }

    /** {@link #xp(Document, MuseumData)} with the data read when the plugin started. */
    public static int xp(Document profile) {
        return profile == null ? 0 : xp(profile, data);
    }

    /** How many of a category's entries count. */
    public static int counted(Document profile, MuseumData data, Category category) {
        int counted = 0;
        for (Entry entry : category.entries()) if (counts(profile, data, entry)) counted++;
        return counted;
    }

    /** How many of all the entries count. */
    public static int counted(Document profile, MuseumData data) {
        int counted = 0;
        for (Category category : data.categories()) counted += counted(profile, data, category);
        return counted;
    }

    /** The milestone this much Museum XP has reached: one every 100, up to 40. */
    public static int milestone(int xp) {
        return Math.max(0, Math.min(MILESTONES, xp / MILESTONE_XP));
    }

    /** Keeps these items as its donation of the entry, now (again, if it was retrieved). */
    static void donate(Document profile, String entry, long at, List<Binary> items) {
        donations(profile, true).put(entry, new Document(AT, at).append(RETRIEVED, false).append(ITEMS, items));
    }

    /** Its items have gone back to it: the entry still counts, and they're kept to show. */
    static void retrieved(Document profile, String entry) {
        Document donations = donations(profile, false);
        Document one = donations == null ? null : donations.get(entry, Document.class);
        if (one != null) one.put(RETRIEVED, true);
    }

    // The menus' settings: the profile's, as Hypixel's are kept is UNKNOWN.

    /** How the category pages are sorted, in the Sort button's order: Ascending XP first, as recorded. */
    public enum Sort {
        ASCENDING_XP("Ascending XP"), DESCENDING_XP("Descending XP"), MISSING_FIRST("Missing First"), A_TO_Z("A to Z"), Z_TO_A("Z to A");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** The next one (a left click), or the one before (a right click), round. */
        public Sort step(boolean back) {
            Sort[] all = values();
            return all[(ordinal() + (back ? all.length - 1 : 1)) % all.length];
        }
    }

    static Sort sort(Document profile) {
        Document museum = profile.get(FIELD, Document.class);
        String name = museum == null ? null : museum.getString(SORT);
        if (name != null) {
            try {
                return Sort.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // A sort since removed: the first.
            }
        }
        return Sort.ASCENDING_XP;
    }

    static void sort(Document profile, Sort sort) {
        part(profile).put(SORT, sort.name());
    }

    /** Extra Details: on, as recorded, unless it's been turned off. */
    static boolean details(Document profile) {
        Document museum = profile.get(FIELD, Document.class);
        return museum == null || !Boolean.FALSE.equals(museum.get(DETAILS));
    }

    static void details(Document profile, boolean on) {
        part(profile).put(DETAILS, on);
    }
}
