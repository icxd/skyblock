package net.icxd.dungeons.island;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import com.mongodb.client.MongoDatabase;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import io.papermc.paper.math.Position;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Worlds;
import net.kyori.adventure.util.TriState;

/**
 * Private islands, on the servers that host them (ISLAND servers, and NONE ones in development, next
 * to their hub). Each profile has one: a world of its own ({@code island-<profile id>}), made from the
 * starter island's capture the first time and stored in MongoDB after that (see {@link IslandStore}),
 * so whichever island server a player is sent to loads it. It's loaded while its player is on it and
 * saved and unloaded when they leave; before a player's data is released to another server (a move or
 * leaving), their island is saved first, so the next server always loads it as they left it. Only the
 * server that holds a player's data loads their island, which keeps one island on one server at a
 * time (visitors and co-ops, which would share one, aren't here). Every five minutes the islands are
 * saved anyway, against a crash.
 *
 * <p>Main thread, apart from what's marked otherwise. Loads and saves run one at a time, in order, on
 * a thread of their own, so a save and the next load of the same island can't cross.
 */
public final class Islands implements Listener {
    public static final String AREA = "Your Island";
    static final String PREFIX = "island-";
    private static final long AUTOSAVE_TICKS = 5 * 60 * 20;

    private static Islands instance;
    /** What else happens on an island: its NPCs and signs when it loads, a player's arrival (see {@link #feature}). */
    private static final List<IslandFeature> FEATURES = new ArrayList<>();

    private final Plugin plugin;
    private final Logger log;
    private final String server;
    private final IslandStore store;
    /** The data checkout's captured worlds, for the starter island. */
    private final Path captures;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "island-saves"));
    private final Executor main;
    private final Map<String, Island> loaded = new HashMap<>();
    private final Map<String, CompletableFuture<World>> loading = new HashMap<>();

    /** A loaded island. */
    static final class Island {
        final String id;
        final World world;

        Island(String id, World world) {
            this.id = id;
            this.world = world;
        }
    }

    private Islands(Plugin plugin, String server, MongoDatabase database, Path captures) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.server = server;
        this.store = new IslandStore(database);
        this.captures = captures;
        this.main = Bukkit.getScheduler().getMainThreadExecutor(plugin);
    }

    /** Whether this kind of server has players' islands. */
    public static boolean hostedOn(ServerType type) {
        return type == ServerType.ISLAND || type == ServerType.NONE;
    }

    /** Starts hosting islands on this server. */
    public static Islands start(Plugin plugin, String server, MongoDatabase database, Path captures) {
        Islands islands = new Islands(plugin, server, database, captures);
        islands.removeLeftovers();
        if (Captures.find(captures, AREA) == null) {
            islands.log.warning("No capture of \"" + AREA + "\" in " + captures + ": new islands start as a platform of grass");
        }
        Bukkit.getPluginManager().registerEvents(islands, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, islands::autosave, AUTOSAVE_TICKS, AUTOSAVE_TICKS);
        instance = islands;
        return islands;
    }

    /** Something islands have: added before the first island loads. */
    public static void feature(IslandFeature feature) {
        FEATURES.add(feature);
    }

    // Where islands are

    /** Whether this world is a private island. */
    public static boolean isIsland(World world) {
        return world != null && world.getName().startsWith(PREFIX);
    }

    /** The id of the island (its profile's) this world is; null if it isn't one. */
    public static String idOf(World world) {
        return isIsland(world) ? world.getName().substring(PREFIX.length()) : null;
    }

    /** Whether this world is the island of the profile the player plays on. */
    public static boolean isOwnIsland(Player player, World world) {
        User user = User.ifLoaded(player.getUniqueId());
        return user != null && isIsland(world) && world.getName().equals(PREFIX + user.profileId());
    }

    /** Where a player arrives on an island. */
    public static Location spawn(World world) {
        double[] at = StarterIsland.SPAWN;
        return new Location(world, at[0], at[1], at[2], StarterIsland.SPAWN_YAW, 0);
    }

    /**
     * To their island: here if this server has islands, else to an island server through the proxy
     * (which says so if there's none).
     */
    public static void send(Player player) {
        if (instance != null) instance.open(player);
        else Dungeons.getProxyLink().send(player, ServerType.ISLAND.name());
    }

    // Loading

    /** Loads the island of the profile they play on (if it isn't loaded) and takes them there. */
    public void open(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null) return;
        String id = user.profileId();
        UUID uuid = player.getUniqueId();
        load(id).whenComplete((world, error) -> {
            if (error != null) {
                log.log(Level.SEVERE, "Couldn't load island " + id, error);
                if (player.isOnline()) player.sendMessage(Text.line("&cCouldn't load your island, try again in a moment."));
                return;
            }
            // Gone, or on another profile, while it loaded: nobody's coming.
            if (!player.isOnline() || User.ifLoaded(uuid) != user || !id.equals(user.profileId())) {
                unloadIfEmpty(loaded.get(id));
                return;
            }
            player.setFallDistance(0);
            player.teleport(spawn(world));
            arrived(player, world);
        });
    }

    private void arrived(Player player, World world) {
        for (IslandFeature feature : FEATURES) {
            try {
                feature.arrived(player, world);
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Island feature " + feature.getClass().getSimpleName() + " failed for " + player.getName(), e);
            }
        }
    }

    /**
     * On an island server, a player joins on their island, as on Hypixel: it's loaded while they log in
     * (their data is here by then), and they spawn on it. Async, waiting on the main thread's part.
     */
    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        if (Dungeons.getSkyBlockServer().getServerType() != ServerType.ISLAND) return;
        User user = User.ifLoaded(event.getConnection().getProfile().getId());
        if (user == null) return;
        String id = user.profileId();
        try {
            World world = CompletableFuture.supplyAsync(() -> load(id), main).thenCompose(w -> w).get(20, TimeUnit.SECONDS);
            event.setSpawnLocation(spawn(world));
        } catch (ExecutionException | TimeoutException e) {
            // They join in the main world, and are sent on when they're in (onJoin).
            log.log(Level.WARNING, "Couldn't load island " + id + " for " + user.getDocument().getString("username") + " in time", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** After their data and items are in place (PlayerListener): on their island, it's an arrival; anywhere else on an island server, they're taken there. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (User.ifLoaded(player.getUniqueId()) == null) return;
        if (isOwnIsland(player, player.getWorld())) arrived(player, player.getWorld());
        else if (Dungeons.getSkyBlockServer().getServerType() == ServerType.ISLAND) open(player);
    }

    /** The island's world, loading it if it isn't: its last save, or the starter island for one never saved. */
    CompletableFuture<World> load(String id) {
        Island island = loaded.get(id);
        if (island != null) return CompletableFuture.completedFuture(island.world);
        CompletableFuture<World> pending = loading.get(id);
        if (pending != null) return pending;
        String name = PREFIX + id;
        Path folder = Worlds.folder(name);
        CompletableFuture<World> world = CompletableFuture.supplyAsync(() -> {
            try {
                // A folder left from before (a crash, or a delete that failed) isn't what was last saved.
                Worlds.delete(folder);
                byte[] saved = store.load(id);
                if (saved != null) {
                    IslandFiles.unzip(saved, folder);
                    return Made.SAVED;
                }
                Path starter = Captures.find(captures, AREA);
                if (starter == null) {
                    Files.createDirectories(folder);
                    return Made.EMPTY;
                }
                IslandFiles.copyCapture(starter, folder);
                return Made.STARTER;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, io).thenApplyAsync(made -> create(id, name, made), main);
        loading.put(id, world);
        world.whenCompleteAsync((w, e) -> loading.remove(id, world), main);
        return world;
    }

    /** What a loading island's world is made from. */
    private enum Made { SAVED, STARTER, EMPTY }

    private World create(String id, String name, Made made) {
        double[] spawn = StarterIsland.SPAWN;
        World world = new WorldCreator(name)
                .generator(new Worlds.Void())
                .generateStructures(false)
                .forcedSpawnPosition(Position.block((int) Math.floor(spawn[0]), (int) spawn[1], (int) Math.floor(spawn[2])), StarterIsland.SPAWN_YAW, 0)
                .keepSpawnLoaded(TriState.FALSE)
                .createWorld();
        if (world == null) throw new IllegalStateException("Paper didn't make the world " + name);
        // Crops grow and leaves fall from a cut tree (recorded: an Oak Sapling dropped there), so random ticks stay on.
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        world.setGameRule(GameRules.MOB_GRIEFING, false);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
        world.setTime(6000);
        world.setAutoSave(false);
        if (made == Made.EMPTY) platform(world);
        loaded.put(id, new Island(id, world));
        for (IslandFeature feature : FEATURES) {
            try {
                feature.loaded(world);
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Island feature " + feature.getClass().getSimpleName() + " failed on " + name, e);
            }
        }
        log.info("Loaded island " + id + (made == Made.SAVED ? "" : " (new)"));
        return world;
    }

    /** Without the starter island's capture: somewhere to stand. */
    private static void platform(World world) {
        double[] spawn = StarterIsland.SPAWN;
        int x = (int) Math.floor(spawn[0]);
        int y = (int) spawn[1] - 1;
        int z = (int) Math.floor(spawn[2]);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) world.getBlockAt(x + dx, y, z + dz).setType(Material.GRASS_BLOCK);
    }

    // Saving and unloading

    /**
     * Saves the island as it is now: written to its folder, read, then zipped and stored on the island
     * thread. Completes once it's stored.
     */
    private CompletableFuture<Void> save(Island island) {
        Map<String, byte[]> files;
        try {
            island.world.save(true);
            files = IslandFiles.read(island.world.getWorldPath());
        } catch (IOException | RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                store.save(island.id, IslandFiles.zip(files), server);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, io);
    }

    /**
     * Before a player's data goes to another server (a move, or leaving): their island is saved, and
     * their data is released once it's stored (see UserStore). If the save fails, the last one stands.
     */
    public CompletableFuture<Void> beforeRelease(User user) {
        Island island = loaded.get(user.profileId());
        if (island == null) return CompletableFuture.completedFuture(null);
        return save(island).exceptionally(e -> {
            log.log(Level.SEVERE, "Couldn't save island " + island.id + " before its player's data went; its last save stands", e);
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Island island = loaded.get(idOf(event.getFrom()));
        if (island != null) Bukkit.getScheduler().runTask(plugin, () -> unloadIfEmpty(island));
    }

    /** Once they're gone from it (a tick later). Their data's release has saved it already. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Island island = loaded.get(idOf(event.getPlayer().getWorld()));
        if (island != null) Bukkit.getScheduler().runTask(plugin, () -> unloadIfEmpty(island));
    }

    /**
     * Unloads an island nobody is on. It's saved first if its player's data is still held here (they
     * went to another world on this server, or changed profile); if it isn't, its release saved it, and
     * a save now could land after the next server's.
     */
    private void unloadIfEmpty(Island island) {
        if (island == null || loaded.get(island.id) != island || !island.world.getPlayers().isEmpty()) return;
        CompletableFuture<Void> saved = ownerHeld(island.id) ? save(island) : CompletableFuture.completedFuture(null);
        loaded.remove(island.id);
        for (IslandFeature feature : FEATURES) {
            try {
                feature.unloading(island.world);
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Island feature " + feature.getClass().getSimpleName() + " failed unloading " + island.id, e);
            }
        }
        if (!Bukkit.unloadWorld(island.world, false)) log.warning("Paper wouldn't unload island " + island.id);
        Path folder = island.world.getWorldPath();
        saved.whenCompleteAsync((done, error) -> {
            if (error != null) log.log(Level.SEVERE, "Couldn't save island " + island.id + " as it unloaded; its last save stands", error);
            try {
                Worlds.delete(folder);
            } catch (IOException e) {
                log.log(Level.WARNING, "Couldn't delete " + folder + " (it goes when the server next starts)", e);
            }
        }, io);
    }

    /** Whether the data of the player whose profile this island is is held here still: they're on this server and not leaving it. */
    private static boolean ownerHeld(String id) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            User user = User.ifLoaded(player.getUniqueId());
            if (user != null && !user.isReleased() && Profiles.profiles(user.getDocument()).containsKey(id)) return true;
        }
        return false;
    }

    private void autosave() {
        for (Island island : List.copyOf(loaded.values())) {
            if (island.world.getPlayers().isEmpty()) continue;
            save(island).exceptionally(e -> {
                log.log(Level.WARNING, "Couldn't autosave island " + island.id, e);
                return null;
            });
        }
    }

    /** When the plugin stops: every island saved (the server unloads the worlds itself), waiting for the saves. */
    public void stop() {
        for (Island island : List.copyOf(loaded.values())) {
            save(island).exceptionally(e -> {
                log.log(Level.SEVERE, "Couldn't save island " + island.id + " while stopping; its last save stands", e);
                return null;
            });
        }
        io.shutdown();
        try {
            if (!io.awaitTermination(30, TimeUnit.SECONDS)) log.severe("Gave up waiting for islands to save");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        instance = null;
    }

    /** Island folders from before this start (it stopped without unloading them): their last save is what counts. */
    private void removeLeftovers() {
        File[] folders = Worlds.dimensionsFolder().toFile().listFiles((dir, name) -> name.startsWith(PREFIX));
        if (folders == null) return;
        for (File folder : folders) {
            try {
                Worlds.delete(folder.toPath());
            } catch (IOException e) {
                log.log(Level.WARNING, "Couldn't delete the old island folder " + folder, e);
            }
        }
    }
}
