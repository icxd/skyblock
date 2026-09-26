package net.icxd.dungeons.dungeons.instance;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.set;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;

import com.mongodb.client.MongoCollection;

import io.papermc.paper.math.Position;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.common.Runs;
import net.icxd.dungeons.dungeons.generation.DungeonConfig;
import net.icxd.dungeons.dungeons.generation.DungeonGenerator;
import net.icxd.dungeons.dungeons.paste.PastePlan;
import net.icxd.dungeons.dungeons.paste.RoomLibrary;
import net.icxd.dungeons.dungeons.paste.WorldEditPaster;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.network.ProxyLink;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/**
 * The dungeon runs on this (DUNGEONS) server. The proxy writes a run to the {@code runs} collection
 * (see {@link Runs}) and sends its party here; each run gets a world of its own with a freshly
 * generated floor pasted where Hypixel has it, so several share the server. Members who arrive
 * before it's built wait on a platform above it; once it's built, {@link DungeonRun} takes the run
 * from waiting in the entrance room to the end. A run nobody is in any more for a minute ends.
 *
 * <p>Run worlds ({@code run-1}, {@code run-2}, ...) are kept and reused: generating the chunks of a
 * new one takes seconds, emptying an old one doesn't. One spare is always generated ahead.
 *
 * <p>Main thread, apart from what's marked otherwise.
 */
public final class RunManager {
    private static final String WORLD_PREFIX = "run-";
    private static final Pattern WORLD_NAME = Pattern.compile("run-\\d+");
    private static final long EMPTY_MILLIS = 60_000;
    /** How far above the tallest room the waiting platform is (so clearing an old floor leaves it be). */
    private static final int WAITING_ABOVE = 10;

    private static final class Run {
        final String id;
        final DungeonFloor floor;
        final List<UUID> members;
        /** How long each step of setting it up took, for the log. */
        final StringBuilder timings = new StringBuilder();
        long stepStart = System.currentTimeMillis();
        World world;
        Location waiting;
        Location entrance;
        /** Once the floor is built. */
        DungeonRun lifecycle;
        MapView map;
        /** Members told it's being prepared, so they're told once. */
        final Set<UUID> told = new HashSet<>();
        boolean ended;
        /** When the last member left (or it was set up, until someone arrives); 0 while someone's here. */
        long emptySince = System.currentTimeMillis();

        Run(String id, DungeonFloor floor, List<UUID> members) {
            this.id = id;
            this.floor = floor;
            this.members = members;
        }
    }

    private final Plugin plugin;
    private final Logger log;
    private final String server;
    private final MongoCollection<Document> runs;
    private final ProxyLink proxy;
    private final Map<String, Run> byId = new HashMap<>();
    private final Map<UUID, Run> byMember = new HashMap<>();
    /** Runs found for players while they log in (async), for when they join. */
    private final Map<UUID, Document> arriving = new ConcurrentHashMap<>();
    /** Run worlds not in use, loaded or not. */
    private final Deque<String> idle = new ArrayDeque<>();
    /** Run worlds nothing has been pasted in yet. */
    private final Set<String> empty = new HashSet<>();
    /** Maps of finished runs, for the next ones (each new one is saved with the main world for good). */
    private final Deque<MapView> spareMaps = new ArrayDeque<>();
    private CompletableFuture<RoomLibrary> library;
    /** Everything a floor can take up, and the height of the waiting platform; known once the rooms are loaded. */
    private PastePlan.Box largest;
    private int waitingY;
    private boolean polling;
    private boolean warming;

    public RunManager(Plugin plugin, String server, MongoCollection<Document> runs, ProxyLink proxy) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.server = server;
        this.runs = runs;
        this.proxy = proxy;
    }

    public void start() {
        findRunWorlds();
        long stale = runs.updateMany(and(eq(Runs.SERVER, server), ne(Runs.STATE, Runs.ENDED)), set(Runs.STATE, Runs.ENDED)).getModifiedCount();
        if (stale > 0) log.info("Ended " + stale + " dungeon runs left over from before this server stopped");
        Path folder = new File(plugin.getDataFolder(), "dungeon-rooms").toPath();
        library = CompletableFuture.supplyAsync(() -> {
            try {
                RoomLibrary loaded = RoomLibrary.load(folder);
                loaded.problems().forEach(p -> log.warning("Room library: " + p));
                if (loaded.templates().isEmpty()) throw new IllegalStateException("no rooms in " + folder);
                log.info("Dungeon rooms: " + loaded.summary());
                return loaded;
            } catch (IOException e) {
                throw new IllegalStateException("couldn't read the rooms in " + folder + ": " + e.getMessage(), e);
            }
        });
        library.whenComplete((rooms, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) {
                log.severe("No dungeon runs on this server: " + (error.getCause() != null ? error.getCause() : error).getMessage());
                return;
            }
            largest = PastePlan.largestArea(rooms, PastePlan.HYPIXEL_BASE, PastePlan.HYPIXEL_BASE);
            waitingY = largest.max().y() + WAITING_ABOVE;
            prepareSpare();
        }));
        Bukkit.getPluginManager().registerEvents(new Events(), plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20, 20);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickRuns, 1, 1);
    }

    /** Ends every run; players still in one go back to the main world. */
    public void stop() {
        for (Run run : List.copyOf(byId.values())) end(run, true);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Run run : List.copyOf(byId.values())) {
            if (run.lifecycle != null) run.lifecycle.second();
            if (run.ended) continue;
            boolean anyone = run.members.stream().anyMatch(m -> isHere(m, run));
            if (anyone) run.emptySince = 0;
            else if (run.emptySince == 0) run.emptySince = now;
            else if (now - run.emptySince > EMPTY_MILLIS) end(run, false);
        }
        if (polling) return;
        polling = true;
        // New runs the proxy has sent here, so they're being built before the party arrives.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> assigned = new ArrayList<>();
            try {
                runs.find(and(eq(Runs.SERVER, server), eq(Runs.STATE, Runs.ASSIGNED))).into(assigned);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Couldn't check for new dungeon runs", e);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                polling = false;
                for (Document doc : assigned) run(doc);
            });
        });
    }

    /** Keys, the map, the Watcher. */
    private void tickRuns() {
        for (Run run : List.copyOf(byId.values())) {
            if (run.lifecycle != null && !run.ended) run.lifecycle.tick();
        }
    }

    /** Online on this server, and still in this run. */
    private boolean isHere(UUID member, Run run) {
        return Bukkit.getPlayer(member) != null && byMember.get(member) == run;
    }

    /** The run for this document, setting it up if it's new. */
    private Run run(Document doc) {
        String id = doc.getString("_id");
        Run run = byId.get(id);
        if (run != null) return run;
        DungeonFloor floor;
        try {
            floor = DungeonFloor.valueOf(doc.getString(Runs.FLOOR));
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warning("Dungeon run " + id + " has an unknown floor " + doc.getString(Runs.FLOOR));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runs.updateOne(eq("_id", id), set(Runs.STATE, Runs.ENDED)));
            return null;
        }
        List<UUID> members = doc.getList(Runs.MEMBERS, String.class).stream().map(UUID::fromString).toList();
        run = new Run(id, floor, members);
        byId.put(id, run);
        for (UUID member : members) byMember.put(member, run);
        build(run);
        return run;
    }

    private void build(Run run) {
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            fail(run, "this server doesn't have WorldEdit");
            return;
        }
        long seed = ThreadLocalRandom.current().nextLong();
        run.stepStart = System.currentTimeMillis();
        library.thenApplyAsync(rooms -> {
            var layout = new DungeonGenerator(DungeonConfig.forFloor(run.floor), rooms.pool()).generate(seed);
            PastePlan plan = PastePlan.create(layout, rooms, PastePlan.HYPIXEL_BASE, PastePlan.HYPIXEL_BASE, seed);
            plan.problems().forEach(p -> log.warning("Run " + run.id + ": " + p));
            return plan;
        }).whenComplete((plan, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (run.ended) return;
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                log.log(Level.SEVERE, "Couldn't generate dungeon run " + run.id, cause);
                fail(run, cause.getMessage());
                return;
            }
            step(run, "layout");
            place(run, plan, seed);
        }));
    }

    /** Takes a world for the run, puts up the waiting platform, and pastes the floor once its chunks are loaded. */
    private void place(Run run, PastePlan plan, long seed) {
        World world = takeWorld();
        if (world == null) {
            fail(run, "couldn't create its world");
            return;
        }
        run.world = world;
        boolean wasEmpty = empty.remove(world.getName());
        step(run, "world");

        PastePlan.Block entrance = plan.entrance();
        platform(world, entrance, Material.GLASS);
        run.waiting = new Location(world, entrance.x() + 0.5, waitingY, entrance.z() + 0.5);
        for (Player player : online(run)) send(player, run);

        // Loaded off the main thread first, or the paste's first steps would load them all at once
        // and hold up the server, logins included.
        // A world nothing's been pasted in only needs this floor's chunks; any other may have an
        // earlier floor, of any size, to clear.
        PastePlan.Box area = wasEmpty ? plan.area() : largest;
        loadChunks(world, area, true).whenComplete((loaded, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (run.ended) return;
            if (error != null) {
                fail(run, "couldn't load its world: " + error.getMessage());
                return;
            }
            step(run, "chunks");
            List<PastePlan.Box> clear = wasEmpty ? List.of() : List.of(largest);
            new WorldEditPaster(world, clear).paste(plugin, plan, result -> {
                // The floor's chunks stay loaded (the tickets) until the run ends: keys, doors and
                // the Watcher can be anywhere on it.
                // Only now, so generating it doesn't slow this run's chunks down.
                prepareSpare();
                if (run.ended) return;
                if (result.error() != null) {
                    log.log(Level.SEVERE, "Couldn't paste dungeon run " + run.id, result.error());
                    fail(run, result.error().getMessage());
                    return;
                }
                step(run, "paste");
                log.info("Dungeon run " + run.id + " (" + run.floor.getName() + ", seed " + seed + ") ready in "
                        + world.getName() + ": " + run.timings);
                run.entrance = standingSpot(world, entrance.x(), entrance.y(), entrance.z());
                platform(world, entrance, Material.AIR);
                run.map = takeMap(world);
                run.lifecycle = new DungeonRun(this, plugin, run.id, run.floor, run.members, world, entrance, plan.entranceDoor(),
                        new RunLayout(plan.layout()), plan.roomCount(), plan.puzzleCount(), run.map);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runs.updateOne(eq("_id", run.id), set(Runs.STATE, Runs.RUNNING)));
                for (Player player : online(run)) send(player, run);
            });
        }));
    }

    private void platform(World world, PastePlan.Block at, Material material) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) world.getBlockAt(at.x() + x, waitingY - 1, at.z() + z).setType(material);
        }
    }

    private static void step(Run run, String name) {
        long now = System.currentTimeMillis();
        if (!run.timings.isEmpty()) run.timings.append(", ");
        run.timings.append(name).append(' ').append(now - run.stepStart).append(" ms");
        run.stepStart = now;
    }

    /** To wherever the run is at: the platform until it's built, then wherever {@link DungeonRun} has them. */
    private void send(Player player, Run run) {
        User user = User.cached(player.getUniqueId());
        if (user != null) user.setInDungeon(true);
        if (run.lifecycle != null) {
            run.lifecycle.arrive(player);
            return;
        }
        if (run.waiting != null) player.teleport(run.waiting);
        if (run.told.add(player.getUniqueId())) player.sendMessage(Utils.color("&ePreparing " + run.floor.getName() + "..."));
    }

    private void fail(Run run, String reason) {
        for (Player player : online(run)) {
            player.sendMessage(Utils.color("&cCouldn't set up the dungeon" + (reason == null ? "." : ": " + reason)));
            leave(player);
        }
        end(run, false);
    }

    private void end(Run run, boolean stopping) {
        if (run.ended) return;
        run.ended = true;
        byId.remove(run.id);
        for (UUID member : run.members) byMember.remove(member, run);
        if (run.lifecycle != null) run.lifecycle.dispose();
        if (run.map != null) spareMaps.add(run.map);
        Runnable ended = () -> {
            try {
                runs.updateOne(eq("_id", run.id), set(Runs.STATE, Runs.ENDED));
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Couldn't mark dungeon run " + run.id + " ended", e);
            }
        };
        if (stopping) ended.run();
        else Bukkit.getScheduler().runTaskAsynchronously(plugin, ended);
        if (run.world == null) return;
        World main = Bukkit.getWorlds().get(0);
        for (Player player : run.world.getPlayers()) {
            DungeonRun.takeRunItems(player);
            player.teleport(main.getSpawnLocation());
        }
        run.world.removePluginChunkTickets(plugin);
        // Emptied when the next run takes it.
        idle.add(run.world.getName());
    }

    /** Off to the Dungeon Hub (a hub if there's none). */
    void leave(Player player) {
        User user = User.cached(player.getUniqueId());
        if (user != null) user.setInDungeon(false);
        proxy.send(player, "DUNGEON_HUB");
    }

    /** A run that's over (or closed before it started). */
    void finish(DungeonRun lifecycle) {
        Run run = byId.get(lifecycle.id);
        if (run != null && run.lifecycle == lifecycle) end(run, false);
    }

    /** Whether the player is (still) in this run: after a re-queue they're in the next one. */
    boolean belongs(UUID player, DungeonRun lifecycle) {
        Run run = byMember.get(player);
        return run != null && run.lifecycle == lifecycle;
    }

    /** The run a player is in, once its floor is built; null if none. */
    public DungeonRun runOf(Player player) {
        Run run = byMember.get(player.getUniqueId());
        return run == null || run.lifecycle == null || !player.getWorld().equals(run.world) ? null : run.lifecycle;
    }

    private DungeonRun runIn(World world) {
        for (Run run : byId.values()) {
            if (run.lifecycle != null && world.equals(run.world)) return run.lifecycle;
        }
        return null;
    }

    private MapView takeMap(World world) {
        MapView map = spareMaps.poll();
        if (map == null) map = Bukkit.createMap(world);
        map.setTrackingPosition(false);
        map.setUnlimitedTracking(false);
        map.setLocked(false);
        ScoreCard.blank(map);
        return map;
    }

    private List<Player> online(Run run) {
        return run.members.stream().filter(m -> isHere(m, run)).map(Bukkit::getPlayer).toList();
    }

    // Run worlds

    /** An idle run world, or a new one if there's none (see {@link #prepareSpare}). */
    private World takeWorld() {
        String name = idle.poll();
        World world = name == null ? null : Bukkit.getWorld(name);
        if (world == null) {
            if (name == null) {
                name = newWorldName();
                empty.add(name);
            }
            world = createWorld(name);
        }
        return world;
    }

    /** Makes a run world and generates its chunks in the background, so a run needn't wait for that. */
    private void prepareSpare() {
        if (warming || largest == null || !idle.isEmpty()) return;
        String name = newWorldName();
        World world = createWorld(name);
        if (world == null) return;
        warming = true;
        long start = System.currentTimeMillis();
        loadChunks(world, largest, false).whenComplete((done, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            warming = false;
            if (error != null) {
                log.log(Level.WARNING, "Couldn't generate " + name, error);
                return;
            }
            log.info("Generated spare dungeon world " + name + " in " + (System.currentTimeMillis() - start) + " ms");
            empty.add(name);
            idle.add(name);
        }));
    }

    private String newWorldName() {
        Path dimensions = dimensionsFolder();
        for (int i = 1; ; i++) {
            String name = WORLD_PREFIX + i;
            if (!idle.contains(name) && Bukkit.getWorld(name) == null && !Files.exists(dimensions.resolve(name))) return name;
        }
    }

    private World createWorld(String name) {
        World world = new WorldCreator(name)
                .generator(new VoidWorld())
                .generateStructures(false)
                // A spawn point of our own, so making the world doesn't search for one.
                .forcedSpawnPosition(Position.block(PastePlan.HYPIXEL_BASE, waitingY, PastePlan.HYPIXEL_BASE), 0, 0)
                .createWorld();
        if (world == null) return null;
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        world.setGameRule(GameRules.MOB_GRIEFING, false);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setTime(6000);
        return world;
    }

    /**
     * Loads (or generates) every chunk of an area without holding up the main thread; with
     * {@code keep} they stay loaded until this plugin's chunk tickets are removed.
     */
    private CompletableFuture<Void> loadChunks(World world, PastePlan.Box area, boolean keep) {
        List<CompletableFuture<?>> chunks = new ArrayList<>();
        for (int cx = area.min().x() >> 4; cx <= area.max().x() >> 4; cx++) {
            for (int cz = area.min().z() >> 4; cz <= area.max().z() >> 4; cz++) {
                chunks.add(world.getChunkAtAsync(cx, cz).thenAccept(chunk -> {
                    if (keep) chunk.addPluginChunkTicket(plugin);
                }));
            }
        }
        return CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new));
    }

    /** Where Paper keeps the worlds: Paper 26 has them all in {@code world/dimensions/minecraft}, the main one as {@code overworld}. */
    private static Path dimensionsFolder() {
        Path main = Bukkit.getWorlds().get(0).getWorldPath();
        return main.getFileName().toString().equals("overworld") ? main.getParent() : Bukkit.getWorldContainer().toPath();
    }

    /** Run worlds from before are reused; ones named some other way (earlier versions) are deleted. */
    private void findRunWorlds() {
        File[] files = dimensionsFolder().toFile().listFiles((dir, name) -> name.startsWith(WORLD_PREFIX));
        if (files == null) return;
        for (File file : files) {
            if (WORLD_NAME.matcher(file.getName()).matches()) idle.add(file.getName());
            else if (Bukkit.getWorld(file.getName()) == null) delete(file.toPath());
        }
        if (!idle.isEmpty()) log.info("Reusing dungeon worlds " + String.join(", ", idle));
    }

    private void delete(Path folder) {
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException e) {
            log.log(Level.WARNING, "Couldn't delete " + folder, e);
        }
    }

    /** First spot at or above y with two free blocks over something to stand on. */
    public static Location standingSpot(World world, int x, int y, int z) {
        for (int top = Math.min(y + 60, world.getMaxHeight() - 2); y < top; y++) {
            if (world.getBlockAt(x, y - 1, z).getType().isSolid() && !world.getBlockAt(x, y, z).getType().isSolid()
                    && !world.getBlockAt(x, y + 1, z).getType().isSolid()) {
                break;
            }
        }
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    /** Nothing at all: the floor is pasted in. */
    private static final class VoidWorld extends ChunkGenerator {
    }

    /** Inner, so the plugin's listener scan doesn't register a second copy. */
    private final class Events implements Listener {
        /** Which run they're here for; the document is picked up when they join. Async. */
        @EventHandler(priority = EventPriority.MONITOR)
        public void onPreLogin(AsyncPlayerPreLoginEvent event) {
            if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
            try {
                Document doc = runs.find(and(eq(Runs.SERVER, server), eq(Runs.MEMBERS, event.getUniqueId().toString()),
                        in(Runs.STATE, Runs.ASSIGNED, Runs.RUNNING))).sort(descending(Runs.CREATED)).first();
                if (doc != null) arriving.put(event.getUniqueId(), doc);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Couldn't look up " + event.getName() + "'s dungeon run", e);
            }
        }

        /** After their data and inventory are in place (PlayerListener). */
        @EventHandler(priority = EventPriority.MONITOR)
        public void onJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            Document doc = arriving.remove(player.getUniqueId());
            Run run = doc == null ? byMember.get(player.getUniqueId()) : run(doc);
            if (run == null || run.ended || !player.isOnline()) return;
            byMember.put(player.getUniqueId(), run);
            run.emptySince = 0;
            send(player, run);
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            arriving.remove(event.getPlayer().getUniqueId());
        }

        /** Clicking Mort (or his name tags) before the start opens the Ready Up menu. */
        @EventHandler
        public void onInteract(PlayerInteractEntityEvent event) {
            DungeonRun run = runIn(event.getRightClicked().getWorld());
            if (run == null || !run.isMort(event.getRightClicked())) return;
            event.setCancelled(true);
            if (event.getHand() != EquipmentSlot.HAND) return;
            Player player = event.getPlayer();
            if (runOf(player) == run && (run.phase() == DungeonRun.Phase.WAITING || run.phase() == DungeonRun.Phase.STARTING)) {
                new ReadyUpMenu(run, player).open(player);
            }
        }

        @EventHandler
        public void onArmorStand(PlayerArmorStandManipulateEvent event) {
            DungeonRun run = runIn(event.getRightClicked().getWorld());
            if (run != null && (run.isMort(event.getRightClicked()) || run.isKey(event.getRightClicked()))) event.setCancelled(true);
        }

        /** Right-clicking a wither door or the Blood Door, with the key or without. */
        @EventHandler
        public void onClickBlock(PlayerInteractEvent event) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getHand() != EquipmentSlot.HAND) return;
            DungeonRun run = runOf(event.getPlayer());
            if (run != null && run.clickBlock(event.getPlayer(), event.getClickedBlock())) event.setCancelled(true);
        }

        /**
         * Hits on dungeon mobs that {@code PlayerListener} didn't deal with (fists, other items,
         * arrows) do the player's fist damage; nothing else hurts them.
         */
        @EventHandler(priority = EventPriority.HIGH)
        public void onMobHurt(EntityDamageEvent event) {
            DungeonMobs.Mob mob = DungeonMobs.of(event.getEntity());
            if (mob == null || DungeonMobs.isHandled(event)) return;
            if (event instanceof EntityDamageByEntityEvent hit && playerBehind(hit.getDamager()) instanceof Player player) {
                if (!event.isCancelled()) DungeonMobs.playerHit(hit, player, mob, DungeonMobs.fistDamage(player), false);
                return;
            }
            if (event.getCause() != EntityDamageEvent.DamageCause.KILL) event.setCancelled(true);
        }

        /** Their own attacks (the Parasites' silverfish) do SkyBlock damage. */
        @EventHandler(priority = EventPriority.LOW)
        public void onMobAttack(EntityDamageByEntityEvent event) {
            DungeonMobs.Mob mob = DungeonMobs.of(event.getDamager());
            if (mob == null || !(event.getEntity() instanceof Player player)) return;
            event.setCancelled(true);
            if (mob.attackDamage() > 0) DungeonMobs.hit(player, mob.attackDamage(), event.getDamager());
        }

        /** They arrive a few blocks above the back of the entrance room, as on Hypixel; the drop doesn't hurt. */
        @EventHandler(ignoreCancelled = true)
        public void onFall(EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) return;
            DungeonRun run = runOf(player);
            if (run != null && !run.isStarted()) event.setCancelled(true);
        }

        @EventHandler
        public void onDrop(PlayerDropItemEvent event) {
            if (StoredInventory.isNotSaved(event.getItemDrop().getItemStack())) event.setCancelled(true);
        }

        /** For EXTRA STATS and the tab list. */
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onDamage(EntityDamageByEntityEvent event) {
            // Dungeon mobs count their own.
            if (event.getEntity() instanceof Player || event.getEntity().getScoreboardTags().contains(DungeonMobs.TAG)) return;
            Player damager = playerBehind(event.getDamager());
            DungeonRun run = damager == null ? null : runOf(damager);
            if (run != null) run.damageDealt(damager.getUniqueId(), event.getFinalDamage());
        }

        /** Dungeon mobs drop nothing (their kills are counted by the mobs themselves). */
        @EventHandler
        public void onMobDeath(EntityDeathEvent event) {
            if (!event.getEntity().getScoreboardTags().contains(DungeonMobs.TAG)) return;
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onKill(EntityDeathEvent event) {
            if (event.getEntity() instanceof Player || event.getEntity().getScoreboardTags().contains(DungeonMobs.TAG)) return;
            Player killer = event.getEntity().getKiller();
            DungeonRun run = killer == null ? null : runOf(killer);
            if (run != null) run.killed(killer.getUniqueId());
        }

        /** Until there are ghosts, dying in a run brings you back in its entrance room rather than out of the run. */
        @EventHandler
        public void onRespawn(PlayerRespawnEvent event) {
            if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH) return;
            Run run = byMember.get(event.getPlayer().getUniqueId());
            if (run != null && !run.ended && run.lifecycle != null && run.entrance != null) event.setRespawnLocation(run.entrance);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onDeath(PlayerDeathEvent event) {
            DungeonRun run = runOf(event.getEntity());
            if (run != null) run.died(event.getEntity().getUniqueId());
        }

        private Player playerBehind(Entity damager) {
            if (damager instanceof Player player) return player;
            if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
            return null;
        }
    }
}
