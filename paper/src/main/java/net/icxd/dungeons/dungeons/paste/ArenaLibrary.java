package net.icxd.dungeons.dungeons.paste;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

import io.papermc.paper.math.Position;
import net.icxd.dungeons.dungeons.paste.PastePlan.Box;
import net.icxd.dungeons.island.Captures;
import net.icxd.dungeons.utils.Worlds;
import net.kyori.adventure.util.TriState;

/**
 * The boss rooms' blocks ({@link BossArena}), read once when the server starts from the worlds saved
 * on Hypixel ({@code plugins/dungeons/worlds}, see {@link Captures}): each capture's region files are
 * copied into a world of their own, as a new island's are, the room's chunks loaded, and its box
 * copied into a clipboard a few layers a tick, so the server keeps running; then that world is
 * closed and deleted. A run pastes the clipboard where the room is ({@link WorldEditPaster}).
 *
 * <p>Only load this class when WorldEdit is installed. Main thread, but for the file copying.
 */
public final class ArenaLibrary {
    private static final String WORLD_PREFIX = "boss-";
    /** How long each tick may spend copying, as a floor's paste. */
    private static final long TICK_BUDGET_NANOS = 25_000_000;
    /** Layers a step, as a floor's paste. */
    private static final int SLICE = 4;

    private final Map<BossArena, Clipboard> clipboards = new HashMap<>();

    private ArenaLibrary() {
    }

    /** A boss room's blocks, at their own coordinates; null if it had no capture (or it couldn't be read). */
    public Clipboard clipboard(BossArena arena) {
        return clipboards.get(arena);
    }

    /**
     * Reads every boss room there's a capture of in {@code captures}, one after the other; {@code done}
     * gets them once they're all read (or couldn't be: that's logged, and that room is missing).
     */
    public static void load(Plugin plugin, Path captures, Consumer<ArenaLibrary> done) {
        ArenaLibrary library = new ArenaLibrary();
        Deque<BossArena> left = new ArrayDeque<>(BossArena.ALL);
        next(plugin, captures, library, left, done);
    }

    private static void next(Plugin plugin, Path captures, ArenaLibrary library, Deque<BossArena> left, Consumer<ArenaLibrary> done) {
        BossArena arena = left.poll();
        if (arena == null) {
            done.accept(library);
            return;
        }
        Logger log = plugin.getLogger();
        Path capture = Captures.find(captures, arena.area());
        if (capture == null) {
            log.warning("No capture of \"" + arena.area() + "\" in " + captures + " (save one with the dungeon scanner's /dscan world): "
                    + "without its boss room, those runs end with the Watcher");
            next(plugin, captures, library, left, done);
            return;
        }
        String name = WORLD_PREFIX + arena.id();
        Path folder = Worlds.folder(name);
        long start = System.currentTimeMillis();
        CompletableFuture.runAsync(() -> copyRegions(capture, folder))
                .whenCompleteAsync((ignored, error) -> {
                    if (error != null) {
                        log.log(Level.WARNING, "Couldn't copy the capture of " + arena.area() + " from " + capture, error);
                        next(plugin, captures, library, left, done);
                        return;
                    }
                    World world = open(name, arena);
                    if (world == null) {
                        log.warning("Couldn't open the capture of " + arena.area() + " as a world");
                        close(plugin, null, folder);
                        next(plugin, captures, library, left, done);
                        return;
                    }
                    loadChunks(world, arena.box()).whenComplete((loaded, chunkError) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (chunkError != null) {
                            log.log(Level.WARNING, "Couldn't load the boss room of " + arena.area(), chunkError);
                            close(plugin, world, folder);
                            next(plugin, captures, library, left, done);
                            return;
                        }
                        copy(plugin, world, arena.box(), (clipboard, copyError) -> {
                            if (copyError != null) log.log(Level.WARNING, "Couldn't read the boss room of " + arena.area(), copyError);
                            else {
                                library.clipboards.put(arena, clipboard);
                                log.info("Boss room " + arena.id() + ": read from " + capture.getFileName() + " in "
                                        + (System.currentTimeMillis() - start) + " ms");
                            }
                            close(plugin, world, folder);
                            next(plugin, captures, library, left, done);
                        });
                    }));
                }, Bukkit.getScheduler().getMainThreadExecutor(plugin));
    }

    /** The capture's blocks into a new world's folder (its armor stands and the like stay behind). Off the main thread. */
    private static void copyRegions(Path capture, Path folder) {
        try {
            Worlds.delete(folder);
            Path to = folder.resolve("region");
            Files.createDirectories(to);
            try (Stream<Path> files = Files.list(capture.resolve("region"))) {
                for (Path file : files.toList()) {
                    if (file.getFileName().toString().endsWith(".mca")) Files.copy(file, to.resolve(file.getFileName()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static World open(String name, BossArena arena) {
        double[] center = arena.center();
        World world = new WorldCreator(name)
                .generator(new Worlds.Void())
                .generateStructures(false)
                // A spawn point of its own, so opening it doesn't search for one.
                .forcedSpawnPosition(Position.block((int) Math.floor(center[0]), (int) center[1], (int) Math.floor(center[2])), 0, 0)
                .keepSpawnLoaded(TriState.FALSE)
                .createWorld();
        if (world != null) world.setAutoSave(false);
        return world;
    }

    private static CompletableFuture<Void> loadChunks(World world, Box box) {
        List<CompletableFuture<?>> chunks = new ArrayList<>();
        for (int cx = box.min().x() >> 4; cx <= box.max().x() >> 4; cx++) {
            for (int cz = box.min().z() >> 4; cz <= box.max().z() >> 4; cz++) chunks.add(world.getChunkAtAsync(cx, cz));
        }
        return CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new));
    }

    private interface Copied {
        void accept(Clipboard clipboard, Exception error);
    }

    /** The box's blocks into a clipboard at their own coordinates, {@link #SLICE} layers at a time over as many ticks as it takes. */
    private static void copy(Plugin plugin, World world, Box box, Copied done) {
        BlockVector3 min = BlockVector3.at(box.min().x(), box.min().y(), box.min().z());
        BlockVector3 max = BlockVector3.at(box.max().x(), box.max().y(), box.max().z());
        BlockArrayClipboard clipboard = new BlockArrayClipboard(new CuboidRegion(min, max));
        clipboard.setOrigin(min);
        com.sk89q.worldedit.world.World from = BukkitAdapter.adapt(world);
        new BukkitRunnable() {
            int y = box.min().y();

            @Override
            public void run() {
                long start = System.nanoTime();
                try {
                    while (y <= box.max().y() && System.nanoTime() - start < TICK_BUDGET_NANOS) {
                        int top = Math.min(y + SLICE - 1, box.max().y());
                        BlockVector3 sliceMin = BlockVector3.at(min.x(), y, min.z());
                        ForwardExtentCopy copy = new ForwardExtentCopy(from, new CuboidRegion(sliceMin, BlockVector3.at(max.x(), top, max.z())),
                                sliceMin, clipboard, sliceMin);
                        copy.setCopyingEntities(false);
                        copy.setCopyingBiomes(false);
                        Operations.complete(copy);
                        y = top + 1;
                    }
                } catch (WorldEditException | RuntimeException e) {
                    cancel();
                    done.accept(null, e);
                    return;
                }
                if (y <= box.max().y()) return;
                cancel();
                done.accept(clipboard, null);
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    /** The world closes unsaved, and its folder goes. */
    private static void close(Plugin plugin, World world, Path folder) {
        if (world != null) Bukkit.unloadWorld(world, false);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Worlds.delete(folder);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Couldn't delete " + folder, e);
            }
        });
    }
}
