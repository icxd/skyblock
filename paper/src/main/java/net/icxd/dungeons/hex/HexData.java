package net.icxd.dungeons.hex;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Hypixel's tables the Hex's categories need (enchantments, reforges, gemstones, ...), kept out of this repository
 * in the private data folder's {@code hex/} (servermgr links it into every server as the plugin's {@code hex}).
 * Each category reads its own files: it {@linkplain #add adds} a source when it's made, and when the plugin starts
 * every source is read off the main thread, what's wrong said in the log, and each is handed what it read on the
 * main thread. Until then, and without the folder, a category has nothing (it says so in its own way). Tests read
 * the folder in {@code -Dhex.dir} (see the tests' PrivateHex).
 */
public final class HexData {
    public static final String FOLDER = "hex";

    /** One category's files: read from the folder (problems into the list), then used. */
    private record Source<T>(String name, BiFunction<Path, List<String>, T> read, Consumer<T> use) {
        /** Reads it; a read that fails is a problem, and it gets nothing. */
        Loaded<T> load(Path folder) {
            List<String> problems = new ArrayList<>();
            T value = null;
            try {
                value = read.apply(folder, problems);
            } catch (RuntimeException e) {
                problems.add("couldn't read: " + e);
            }
            return new Loaded<>(this, value, problems);
        }
    }

    private record Loaded<T>(Source<T> source, T value, List<String> problems) {
        void use() {
            if (value != null) source.use().accept(value);
        }
    }

    private static final List<Source<?>> SOURCES = new ArrayList<>();

    private HexData() {
    }

    /**
     * A category's files, read when the plugin starts: {@code read} gets the folder and a list to put what's wrong
     * in (off the main thread: no Bukkit), {@code use} gets what it returned (on the main thread; not called if it
     * returned null or failed). Before {@link #start}: from a category's constructor, say.
     */
    public static synchronized <T> void add(String name, BiFunction<Path, List<String>, T> read, Consumer<T> use) {
        SOURCES.add(new Source<>(name, read, use));
    }

    /** Once, at startup: every source is read off the main thread. */
    public static void start(JavaPlugin plugin) {
        // The categories are made, and add their sources.
        HexCategories.all();
        Path folder = plugin.getDataFolder().toPath().resolve(FOLDER);
        Logger log = plugin.getLogger();
        List<Source<?>> sources;
        synchronized (HexData.class) {
            sources = List.copyOf(SOURCES);
        }
        if (sources.isEmpty()) return;
        if (!Files.isDirectory(folder)) {
            log.warning("Hex: there's no " + folder + ", so the Hex has no tables");
            return;
        }
        CompletableFuture.supplyAsync(() -> load(sources, folder)).whenComplete((loaded, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) {
                log.log(Level.SEVERE, "Hex: couldn't read " + folder, error);
                return;
            }
            for (Loaded<?> l : loaded) {
                l.problems().forEach(p -> log.warning("Hex " + l.source().name() + ": " + p));
                l.use();
            }
            log.info("Hex: read " + loaded.stream().filter(l -> l.value() != null).count() + " of " + loaded.size() + " tables from " + folder);
        }));
    }

    private static List<Loaded<?>> load(List<Source<?>> sources, Path folder) {
        List<Loaded<?>> loaded = new ArrayList<>();
        for (Source<?> source : sources) loaded.add(source.load(folder));
        return loaded;
    }

    /** A JSON file in the folder; null, with a problem, if it isn't there or can't be read. */
    public static JsonElement json(Path folder, String file, List<String> problems) {
        Path path = folder.resolve(file);
        if (!Files.isRegularFile(path)) {
            problems.add("no " + path);
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (IOException | RuntimeException e) {
            problems.add(file + ": " + e);
            return null;
        }
    }
}
