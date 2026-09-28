package net.icxd.dungeons.item.upgrade;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Text;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The books that go on an item ("Combinable in Anvil"; the Hex's Books): which kinds of item each goes on, how
 * many it takes, what they add, and the bracket that shows it in the item's stat lines. Each is kept in the item's
 * data under its own key (the plugin's for the two it had, Hypixel's for the rest where they're lowercase):
 * <ul>
 *   <li>Hot and Fuming Potato Books share one count, {@code hot_potato_books} (Hypixel's {@code hot_potato_count}):
 *       up to 10 Hot, then up to 5 Fuming. Each gives a weapon +2 Damage and +2 Strength, armor +4 Health and +2
 *       Defense: {@code &e(+N)}.</li>
 *   <li>The Art of War, {@code art_of_war}: +5 Strength once, {@code &6[+5]}. The Art of Peace, {@code
 *       art_of_peace} (Hypixel's {@code artOfPeaceApplied} can't be a key here): +40 Health on armor once,
 *       {@code &c[+40]}.</li>
 *   <li>Book of Stats, {@code stats_book}: the kills made with the weapon ({@link BookOfStats}), shown as
 *       "&fKills: &6N" after the item's text and abilities (the live Dark Claymore's and Terminator's).</li>
 *   <li>Farming for Dummies, {@code farming_for_dummies_count}: +1 Farming Fortune each, 5, {@code &a(+N)}.</li>
 *   <li>Bookworm's Favorite Book, {@code bookworm_books}: +20 Damage each, 5, {@code &6(+N)}. Its item's own
 *       text says +10, but the wiki and live vacuums say 20 (Infini-Vacuum Hooverius: 400 base, "+525 (+100)
 *       (+25)" with 5 and a reforge).</li>
 *   <li>Polarvoid Book, {@code polarvoid}: +10 Mining Speed each, 5, and +5 Mining Fortune once there's one,
 *       both {@code &9[+N]} (the live Divan's Drill's).</li>
 *   <li>Wet Book, {@code wet_book_count}: +1 Fishing Speed each, 5, {@code &b(+N)} (the live Hellfire Rod's).</li>
 * </ul>
 * Which kinds each goes on is what the live auction house's items carry (44,726 items, September 2026): potato
 * books and The Art of War on swords, longswords, bows, axes, the gauntlet and fishing rods (never a wand); the
 * Book of Stats on those and farming tools; The Art of Peace on armor; Farming for Dummies on farming tools and
 * vacuums; Bookworm's on vacuums; Polarvoid on drills; the Wet Book on fishing rods. What a book adds counts
 * wherever the data says it's on (the potato books by weapon or armor, as they differ), and its stats are in
 * both the item's stats (ItemStats) and its lore (ItemBuilder#statLines).
 */
public enum Book {
    // UNKNOWN: the order past the wiki's four and armor's three (the newer books after them); no screen shows them.
    HOT_POTATO("HOT_POTATO_BOOK", "&5Hot Potato Book", "hot_potato_books", 10, "&e(", ")"),
    FUMING_POTATO("FUMING_POTATO_BOOK", "&5Fuming Potato Book", "hot_potato_books", 5, null, null),
    STATS("BOOK_OF_STATS", "&5Book of Stats", "stats_book", 1, null, null),
    ART_OF_WAR("THE_ART_OF_WAR", "&6The Art of War", "art_of_war", 1, "&6[", "]"),
    ART_OF_PEACE("THE_ART_OF_PEACE", "&6The Art of Peace", "art_of_peace", 1, "&c[", "]"),
    FARMING_FOR_DUMMIES("FARMING_FOR_DUMMIES", "&5Farming for Dummies", "farming_for_dummies_count", 5, "&a(", ")"),
    BOOKWORM("BOOKWORM_BOOK", "&9Bookworm's Favorite Book", "bookworm_books", 5, "&6(", ")"),
    POLARVOID("POLARVOID_BOOK", "&9Polarvoid Book", "polarvoid", 5, "&9[", "]"),
    WET("WET_BOOK", "&5Wet Book", "wet_book_count", 5, "&b(", ")");

    /** Hot Potato Books an item takes before Fuming ones. */
    public static final int HOT_POTATO_BOOKS = 10;
    /**
     * The weapons that take potato books, The Art of War and the Book of Stats (live items). UNKNOWN: whether every
     * axe and rod does; live items have them on 6 kinds of axe and 3 of rod, so all of those types here.
     */
    private static final Set<SpecificItemType> WEAPONS = EnumSet.of(SpecificItemType.SWORD, SpecificItemType.LONGSWORD,
            SpecificItemType.BOW, SpecificItemType.AXE, SpecificItemType.GAUNTLET, SpecificItemType.FISHING_ROD);

    private final String itemId;
    private final String name;
    private final String key;
    private final int max;
    private final String open;
    private final String close;

    Book(String itemId, String name, String key, int max, String open, String close) {
        this.itemId = itemId;
        this.name = name;
        this.key = key;
        this.max = max;
        this.open = open;
        this.close = close;
    }

    /** Its item's id ("HOT_POTATO_BOOK"). */
    public String itemId() {
        return itemId;
    }

    /** Its name in its item's rarity colour, "&5Hot Potato Book" (the Hex's summary: the official screenshot's). */
    public String displayName() {
        return name;
    }

    /** How many an item takes: 10 Hot Potato Books, then 5 Fuming; 1 of those that go on once. */
    public int max() {
        return max;
    }

    /** Whether it goes on this kind of item at all. */
    public boolean fits(SkyBlockItem item) {
        return switch (this) {
            case HOT_POTATO, FUMING_POTATO -> weapon(item) || armor(item);
            case STATS -> weapon(item) || farmingTool(item);
            case ART_OF_WAR -> weapon(item);
            case ART_OF_PEACE -> armor(item);
            case FARMING_FOR_DUMMIES -> farmingTool(item) || vacuum(item);
            case BOOKWORM -> vacuum(item);
            case POLARVOID -> item.specificItemType() == SpecificItemType.DRILL;
            case WET -> item.specificItemType() == SpecificItemType.FISHING_ROD;
        };
    }

    /** How many of it are on the item: of the potato books' count, the first 10 are Hot, the next 5 Fuming. */
    public int count(NBTTagCompound tag) {
        return switch (this) {
            case HOT_POTATO -> Math.min(tag.getInt(key), HOT_POTATO_BOOKS);
            case FUMING_POTATO -> Math.max(0, Math.min(tag.getInt(key) - HOT_POTATO_BOOKS, max));
            case STATS -> tag.hasKey(key) ? 1 : 0;
            case ART_OF_WAR, ART_OF_PEACE -> tag.getBoolean(key) ? 1 : 0;
            default -> tag.getInt(key);
        };
    }

    /** Whether it's on the item as many times as it goes. */
    public boolean maxed(NBTTagCompound tag) {
        return count(tag) >= max;
    }

    /**
     * Whether one more can go on now: it fits and isn't maxed, and a Fuming Potato Book only after 10 Hot (the 20
     * January 2026 patch: "All 10 Hot Potato Books now must be applied before applying any Fuming Potato Books").
     */
    public boolean applicable(SkyBlockItem item, NBTTagCompound tag) {
        if (!fits(item) || maxed(tag)) return false;
        return this != FUMING_POTATO || tag.getInt(key) >= HOT_POTATO_BOOKS;
    }

    /** One more on the item's data (the Book of Stats starts counting from 0). */
    public void apply(NBTTagCompound tag) {
        switch (this) {
            case STATS -> tag.setInt(key, 0);
            case ART_OF_WAR, ART_OF_PEACE -> tag.setBoolean(key, true);
            default -> tag.setInt(key, tag.getInt(key) + 1);
        }
    }

    /** What it adds to the item's stats (the potato books' count is all Hot Potato Book's here: one bracket). */
    public Stats stats(SkyBlockItem item, NBTTagCompound tag) {
        Stats stats = new Stats();
        int n = tag.getInt(key);
        switch (this) {
            case HOT_POTATO -> {
                if (weapon(item)) stats.add(Stat.DAMAGE, n * 2).add(Stat.STRENGTH, n * 2);
                else if (armor(item)) stats.add(Stat.HEALTH, n * 4).add(Stat.DEFENSE, n * 2);
            }
            case ART_OF_WAR -> {
                if (tag.getBoolean(key)) stats.add(Stat.STRENGTH, 5);
            }
            case ART_OF_PEACE -> {
                if (tag.getBoolean(key)) stats.add(Stat.HEALTH, 40);
            }
            case FARMING_FOR_DUMMIES -> stats.add(Stat.FARMING_FORTUNE, n);
            // UNKNOWN which is current: its own text says +10; the wiki and live vacuums, +20 (followed).
            case BOOKWORM -> stats.add(Stat.DAMAGE, n * 20);
            case POLARVOID -> {
                if (n > 0) stats.add(Stat.MINING_SPEED, n * 10).add(Stat.MINING_FORTUNE, 5);
            }
            case WET -> stats.add(Stat.FISHING_SPEED, n);
            default -> {
            }
        }
        return stats;
    }

    /** "&e(+20)": what it adds to a stat, in its bracket. */
    public String bracket(double value) {
        return open + Text.signed(value) + close;
    }

    /** Every book's stats on the item that has any, in the lore's bracket order. */
    public static Map<Book, Stats> bonuses(SkyBlockItem item, NBTTagCompound tag) {
        Map<Book, Stats> bonuses = new LinkedHashMap<>();
        for (Book book : values()) {
            if (book.open == null) continue;
            Stats stats = book.stats(item, tag);
            if (!stats.equals(new Stats())) bonuses.put(book, stats);
        }
        return bonuses;
    }

    /** The books that go on this kind of item, in the Hex's order. */
    public static List<Book> on(SkyBlockItem item) {
        List<Book> books = new ArrayList<>();
        for (Book book : values()) if (book.fits(item)) books.add(book);
        return books;
    }

    /**
     * The Book of Stats' lore: "&fKills: &62,133" (live), or on a farming tool "&fCrops Harvested: &60" (live; LATER:
     * nothing counts crops, there's no farming); none without the book.
     */
    public static List<String> statsLines(SkyBlockItem item, NBTTagCompound tag) {
        if (!tag.hasKey(STATS.key)) return List.of();
        String what = farmingTool(item) ? "Crops Harvested" : "Kills";
        return List.of("&f" + what + ": &6" + Text.number(tag.getInt(STATS.key)));
    }

    /** Whether its Book of Stats counts kills: it has one, and isn't a farming tool (whose counts crops). */
    public static boolean countsKills(SkyBlockItem item, NBTTagCompound tag) {
        return tag.hasKey(STATS.key) && !farmingTool(item);
    }

    /** One more kill on its Book of Stats. */
    public static void addKill(NBTTagCompound tag) {
        tag.setInt(STATS.key, tag.getInt(STATS.key) + 1);
    }

    private static boolean weapon(SkyBlockItem item) {
        return WEAPONS.contains(item.specificItemType());
    }

    private static boolean armor(SkyBlockItem item) {
        return item.genericItemType() == GenericItemType.ARMOR;
    }

    /** Farming tools are only known by their Hypixel type (hoes, dicers, gardening tools). */
    private static boolean farmingTool(SkyBlockItem item) {
        return "FARMING_TOOL".equals(item.typeKey()) || item.specificItemType() == SpecificItemType.HOE;
    }

    private static boolean vacuum(SkyBlockItem item) {
        return "VACUUM".equals(item.typeKey());
    }
}
