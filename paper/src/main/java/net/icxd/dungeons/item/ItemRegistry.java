package net.icxd.dungeons.item;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Every SkyBlock item there is, by id: the items in items.json (see {@link #loadData}). None are written
 * in Java: code only adds what items do, by item id or ability name.
 */
public final class ItemRegistry {
    private static final Map<String, SkyBlockItem> registry = new LinkedHashMap<>();
    /** How many skipped items of a kind loadData names in the log. */
    private static final int LOGGED = 20;

    private ItemRegistry() {
    }

    /** Null if there's no such item. */
    public static SkyBlockItem get(String id) {
        return id == null ? null : registry.get(id.toUpperCase());
    }

    public static Map<String, SkyBlockItem> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    /**
     * What {@link #loadData} made of a file: how many items it added, the items it skipped and the names
     * it dropped (see {@link ItemData}), and how long it took. {@code failure} is why the whole file wasn't
     * read (it's missing, or isn't items.json), else null.
     */
    public record LoadReport(Path file, int loaded, List<ItemData.Problem> errors, List<ItemData.Problem> warnings,
                             String failure, long millis) {
        public Map<String, Integer> errorsByKind() {
            return byKind(errors);
        }

        public Map<String, Integer> warningsByKind() {
            return byKind(warnings);
        }

        private static Map<String, Integer> byKind(List<ItemData.Problem> problems) {
            Map<String, Integer> counts = new TreeMap<>();
            for (ItemData.Problem problem : problems) counts.merge(problem.kind(), 1, Integer::sum);
            return counts;
        }
    }

    /**
     * Makes the items in an items.json (see {@link ItemData}) the items there are. Items from an earlier
     * call are dropped first, so this also reloads. A file that's missing or can't be read leaves no
     * items. What it did is logged, and returned.
     */
    public static LoadReport loadData(Path file) {
        long start = System.nanoTime();
        clearData();
        Logger log = log();
        if (!Files.exists(file)) {
            log.warning("There's no " + file + ", so there are no SkyBlock items");
            return new LoadReport(file, 0, List.of(), List.of(), "missing", 0);
        }
        ItemData.Result result;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            result = ItemData.load(reader);
        } catch (IOException | RuntimeException e) {
            log.log(Level.WARNING, "Couldn't read " + file + ", so there are no SkyBlock items", e);
            return new LoadReport(file, 0, List.of(), List.of(), e.toString(), (System.nanoTime() - start) / 1_000_000);
        }
        // ItemData has refused ids that differ only in case, so none of these replaces another.
        for (DataItem item : result.items().values()) registry.put(item.id().toUpperCase(), item);
        LoadReport report = new LoadReport(file, result.items().size(), result.errors(), result.warnings(), null,
                (System.nanoTime() - start) / 1_000_000);

        log.info(report.loaded() + " items from " + file + " in " + report.millis() + " ms");
        // The skipped items with why (the first few of each kind); the names dropped once each, since many items share them.
        Map<String, List<ItemData.Problem>> skipped = new TreeMap<>();
        for (ItemData.Problem error : report.errors()) skipped.computeIfAbsent(error.kind(), kind -> new ArrayList<>()).add(error);
        skipped.forEach((kind, errors) -> {
            StringJoiner items = new StringJoiner(", ");
            for (ItemData.Problem error : errors.subList(0, Math.min(errors.size(), LOGGED))) items.add(error.id() + " (" + error.detail() + ")");
            if (errors.size() > LOGGED) items.add("and " + (errors.size() - LOGGED) + " more");
            log.warning("Skipped " + errors.size() + " items (" + kind + "): " + items);
        });
        if (!report.warnings().isEmpty()) {
            Map<String, TreeSet<String>> names = new TreeMap<>();
            for (ItemData.Problem warning : report.warnings()) names.computeIfAbsent(warning.kind(), kind -> new TreeSet<>()).add(warning.detail());
            StringJoiner dropped = new StringJoiner("; ");
            report.warningsByKind().forEach((kind, count) -> dropped.add(kind + " " + count + "x " + names.get(kind)));
            log.info("Names this plugin doesn't have, left out of the items: " + dropped);
        }
        return report;
    }

    /** Drops every item. */
    static void clearData() {
        registry.clear();
    }

    private static Logger log() {
        Dungeons plugin = Dungeons.getInstance();
        return plugin != null ? plugin.getLogger() : Logger.getLogger(ItemRegistry.class.getName());
    }
}
