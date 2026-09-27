package net.icxd.dungeons.shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.Binary;

/**
 * What a profile sold to NPC shops and can buy back: "The NPCs will now remember up to 10 items that
 * you sold. You can buyback those items across merchants and servers. The items are saved for up to
 * 1 hour." (Hypixel, 2020/May 15). Kept on the profile ({@code buyback}), so it goes where they go.
 * Buying one back costs what it sold for (assumed: Hypixel's buyback price isn't known).
 */
public final class Buyback {
    public static final String FIELD = "buyback";
    public static final int KEPT = 10;
    public static final long KEPT_MILLIS = 60 * 60 * 1000L;

    private Buyback() {
    }

    /** One sold stack: the item (its bytes, as the stored inventory keeps items), what it sold for, and when. */
    public record Entry(byte[] item, double price, long soldAt) {
        Document toDocument() {
            return new Document("item", new Binary(item)).append("price", price).append("soldAt", new Date(soldAt));
        }

        static Entry of(Object value) {
            if (!(value instanceof Document doc)) return null;
            byte[] item = doc.get("item") instanceof Binary b ? b.getData() : doc.get("item") instanceof byte[] raw ? raw : null;
            Object at = doc.get("soldAt");
            long soldAt = at instanceof Date date ? date.getTime() : at instanceof Number n ? n.longValue() : -1;
            if (item == null || soldAt < 0 || !(doc.get("price") instanceof Number price)) return null;
            return new Entry(item, price.doubleValue(), soldAt);
        }

        boolean same(Entry other) {
            return other != null && soldAt == other.soldAt && price == other.price && Arrays.equals(item, other.item);
        }
    }

    /** A sale to remember: it goes last, and only the newest {@link #KEPT} of the last hour are kept. */
    public static void add(Document profile, Entry entry, long now) {
        List<Entry> kept = new ArrayList<>(oldestFirst(profile, now));
        kept.add(entry);
        while (kept.size() > KEPT) kept.removeFirst();
        write(profile, kept);
    }

    /** What they can buy back now, newest first. */
    public static List<Entry> entries(Document profile, long now) {
        return oldestFirst(profile, now).reversed();
    }

    /** The last thing they sold, if it's still to be had; else null. */
    public static Entry latest(Document profile, long now) {
        List<Entry> entries = oldestFirst(profile, now);
        return entries.isEmpty() ? null : entries.getLast();
    }

    /**
     * Takes an entry off the list, once it's bought back.
     *
     * @return false if it wasn't there (gone past the hour, or bought back already)
     */
    public static boolean remove(Document profile, Entry entry, long now) {
        List<Entry> kept = new ArrayList<>(oldestFirst(profile, now));
        boolean removed = kept.removeIf(entry::same);
        write(profile, kept);
        return removed;
    }

    private static List<Entry> oldestFirst(Document profile, long now) {
        List<Entry> out = new ArrayList<>();
        if (!(profile.get(FIELD) instanceof List<?> list)) return out;
        for (Object value : list) {
            Entry entry = Entry.of(value);
            if (entry != null && now - entry.soldAt() < KEPT_MILLIS) out.add(entry);
        }
        return out;
    }

    private static void write(Document profile, List<Entry> entries) {
        List<Document> docs = new ArrayList<>(entries.size());
        for (Entry entry : entries) docs.add(entry.toDocument());
        profile.put(FIELD, docs);
    }
}
