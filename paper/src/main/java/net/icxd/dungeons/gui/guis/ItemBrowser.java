package net.icxd.dungeons.gui.guis;

import lombok.Getter;
import net.icxd.dungeons.item.enums.Rarity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * What the item browser ({@link ItemBrowserGUI}) shows one player: their search, sort, filters and
 * page, and the items those pick out. No Bukkit in here, so it can be tested on its own.
 */
@Getter
public final class ItemBrowser {
    /** The top five rows. */
    public static final int PAGE_SIZE = 45;

    public enum Sort {
        RARITY_DESCENDING("Rarity (high to low)"),
        RARITY_ASCENDING("Rarity (low to high)"),
        NAME_ASCENDING("Name (A-Z)"),
        NAME_DESCENDING("Name (Z-A)"),
        TYPE("Type");

        @Getter
        private final String label;

        Sort(String label) {
            this.label = label;
        }
    }

    /** An item as the browser sees it: {@code name} as the item has it (it may have colour codes), {@code type} its type key. */
    public record Entry(String id, String name, Rarity rarity, String type) {
    }

    /** One page of what matches: {@code page} counts from 0, and there's always at least one page. */
    public record Page(List<Entry> entries, int page, int pages, int total) {
    }

    /** An entry with its name as searched (no colours, lower case) and as sorted (from its first letter), worked out once. */
    private record Keyed(Entry entry, String plain, String sortName) {
    }

    private String query = "";
    private Sort sort = Sort.RARITY_DESCENDING;
    /** Null for every rarity. */
    private Rarity rarity;
    /** Null for every type. */
    private String type;
    private int page;

    /** Blank shows everything again. Any change goes back to the first page. */
    public void search(String query) {
        this.query = query == null ? "" : query.trim();
        this.page = 0;
    }

    public void cycleSort(boolean forward) {
        Sort[] sorts = Sort.values();
        sort = sorts[step(sort.ordinal(), sorts.length, forward)];
        page = 0;
    }

    public void cycleRarity(boolean forward) {
        Rarity[] rarities = Rarity.values();
        // 0 is every rarity, then each in order.
        int next = step(rarity == null ? 0 : rarity.ordinal() + 1, rarities.length + 1, forward);
        rarity = next == 0 ? null : rarities[next - 1];
        page = 0;
    }

    /** Through {@code types} (see {@link #types}), after every type. */
    public void cycleType(List<String> types, boolean forward) {
        int next = step(type == null ? 0 : types.indexOf(type) + 1, types.size() + 1, forward);
        type = next == 0 ? null : types.get(next - 1);
        page = 0;
    }

    public void nextPage() {
        page++;
    }

    public void previousPage() {
        page = Math.max(0, page - 1);
    }

    /** Everything that matches the search and filters, sorted. */
    public List<Entry> matches(Collection<Entry> items) {
        String q = query.toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(e -> rarity == null || e.rarity() == rarity)
                .filter(e -> type == null || type.equals(e.type()))
                .map(e -> {
                    String plain = plain(e.name()).toLowerCase(Locale.ROOT);
                    return new Keyed(e, plain, fromFirstLetter(plain));
                })
                .filter(k -> q.isEmpty() || k.plain().contains(q) || k.entry().id().toLowerCase(Locale.ROOT).contains(q))
                .sorted(comparator(sort))
                .map(Keyed::entry)
                .toList();
    }

    /** This player's page of what matches; a page past the end (the results shrank) becomes the last one. */
    public Page view(Collection<Entry> items) {
        List<Entry> matches = matches(items);
        int pages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.clamp(page, 0, pages - 1);
        int from = page * PAGE_SIZE;
        return new Page(matches.subList(from, Math.min(from + PAGE_SIZE, matches.size())), page, pages, matches.size());
    }

    /** The type filter's options after "every type": each type the items have, A-Z. */
    public static List<String> types(Collection<Entry> items) {
        return items.stream().map(Entry::type).distinct().sorted().toList();
    }

    /** Ties go by name, then id, whatever the sort. */
    private static Comparator<Keyed> comparator(Sort sort) {
        Comparator<Keyed> byId = Comparator.comparing(k -> k.entry().id());
        Comparator<Keyed> byName = Comparator.comparing(Keyed::sortName).thenComparing(byId);
        Comparator<Keyed> byRarity = Comparator.comparingInt(k -> k.entry().rarity().ordinal());
        return switch (sort) {
            case RARITY_DESCENDING -> byRarity.reversed().thenComparing(byName);
            case RARITY_ASCENDING -> byRarity.thenComparing(byName);
            case NAME_ASCENDING -> byName;
            case NAME_DESCENDING -> Comparator.comparing(Keyed::sortName).reversed().thenComparing(byId);
            case TYPE -> Comparator.comparing((Keyed k) -> k.entry().type()).thenComparing(byName);
        };
    }

    private static int step(int index, int count, boolean forward) {
        return Math.floorMod(index + (forward ? 1 : -1), count);
    }

    /** The name without its colour codes. A loop, not a regex: this runs for every item on every click. */
    public static String plain(String name) {
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < name.length() && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(name.charAt(i + 1)) >= 0) i++;
            else out.append(c);
        }
        return out.toString();
    }

    /** What names sort by starts at their first letter or digit, so "❤ Fine Ruby Gemstone" goes under F. */
    private static String fromFirstLetter(String name) {
        int i = 0;
        while (i < name.length() && !Character.isLetterOrDigit(name.codePointAt(i))) i += Character.charCount(name.codePointAt(i));
        return name.substring(i);
    }

    /** "REFORGE_STONE" as "Reforge Stone". */
    public static String label(String key) {
        List<String> words = new ArrayList<>();
        for (String word : key.toLowerCase(Locale.ROOT).split("_")) {
            if (!word.isEmpty()) words.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return String.join(" ", words);
    }

    /**
     * Where a lore list of {@code count} options starts so the selected one is shown when only
     * {@code lines} fit: in the middle, or as near as the ends allow.
     */
    public static int window(int count, int selected, int lines) {
        if (count <= lines) return 0;
        return Math.clamp(selected - lines / 2, 0, count - lines);
    }

    /** Tab completion: the ids (and anything else in {@code options}) starting with what's typed, any case, A-Z, at most {@code limit}. */
    public static List<String> complete(Stream<String> options, String typed, int limit) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.filter(o -> o.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(limit)
                .toList();
    }
}
