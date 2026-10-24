package net.icxd.dungeonscanner.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * {@code /dscan world}: saves the world as the server has already sent it, as region files a 26.2
 * server or singleplayer world loads. It only reads what the client has: it never asks for a chunk,
 * changes the view distance, moves you or sends anything, so what you walk past is what you get.
 * Each world visited (Hypixel switches worlds for every island and server) gets its own folder:
 *
 * <pre>
 * worlds/&lt;time&gt;[_name]/&lt;n&gt;_&lt;area&gt;/region/r.x.z.mca     blocks, biomes and block entities
 *                                  entities/r.x.z.mca   item frames, paintings, unnamed armor stands, block and item displays
 *                                  capture.json         areas, server, dimension, times, counts, biomes
 * </pre>
 *
 * A chunk is saved once, two seconds after it first shows up (signs' text and heads' skins can come
 * just after the chunk), or as it unloads if that's sooner: what's kept is the chunk as it arrived,
 * before anyone changed it. Decorations are kept as first seen. Files are written off the client
 * thread.
 */
public final class WorldCapture {
  private static final Logger LOG = LoggerFactory.getLogger("dungeonscanner");
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  /** Ticks a loaded chunk waits before it's saved. */
  private static final int SETTLE = 40;
  /** How often new decorations are written and the region files flushed. */
  private static final int FLUSH = 20 * 10;
  private static final Pattern CODES = Pattern.compile("§.?");

  private final Path folder;
  private final Consumer<String> info;
  private final Consumer<String> problem;
  private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "Dungeon Scanner world writer"));
  /** Messages from the writer thread, said on the client thread. */
  private final Queue<String> problems = new ConcurrentLinkedQueue<>();
  private final Queue<String> infos = new ConcurrentLinkedQueue<>();
  private World world;
  /** Worlds entered, and those with something saved (which are numbered). */
  private int entered;
  private int numbered;
  private int ticks;
  private int chunks;
  private int failures;

  public WorldCapture(Path folder, Consumer<String> info, Consumer<String> problem) {
    this.folder = folder;
    this.info = info;
    this.problem = problem;
  }

  public Path folder() {
    return folder;
  }

  /** Every client tick while saving. Never throws: it runs inside the game's own tick. */
  public void tick(Minecraft mc) {
    try {
      tell();
      ClientLevel level = mc.level;
      if (world != null && world.level != level) endWorld();
      if (level == null || mc.player == null) return;
      if (world == null) world = new World(level, ++entered);
      if (++ticks % 20 != 0) return;
      readSidebar(world);
      saveSettled(world, mc.player.chunkPosition(), mc.options.getEffectiveRenderDistance() + 3);
      keepDecorations(world);
      if (ticks % FLUSH == 0) writeDecorations(world);
    } catch (RuntimeException e) {
      broke("saving", e);
    }
  }

  /**
   * A chunk about to unload: saved now if it hasn't been yet, since it may not be seen again.
   * Never throws: it runs while the game handles the server's packets.
   */
  public void unloading(ClientLevel level, LevelChunk chunk) {
    try {
      World w = world;
      if (w != null && w.level == level && !w.saved.contains(chunk.getPos().pack())) save(w, chunk);
    } catch (RuntimeException e) {
      broke("saving an unloading chunk", e);
    }
  }

  /** Something failed on the client thread: logged, said the first few times, and the game goes on. */
  private void broke(String doing, RuntimeException e) {
    LOG.error("World capture failed while {}", doing, e);
    if (++failures <= 3) problem.accept("Something went wrong " + doing + ": " + e + " (see the log)");
  }

  /** Finishes the world being saved, and waits (half a minute at most) for its files to be written. */
  public void stop() {
    if (world != null) endWorld();
    io.shutdown();
    try {
      if (!io.awaitTermination(30, TimeUnit.SECONDS)) problems.add("Still writing after 30 s; the files may be incomplete.");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    tell();
  }

  public String status() {
    World w = world;
    if (w == null) return "Saving to " + folder + ", waiting for a world.";
    return "Saving to " + folder + ", this world" + (w.areas.isEmpty() ? "" : " (" + String.join(", ", w.areas) + ")") + ": "
        + w.saved.size() + " chunks and " + w.decorations.size() + " decorations, " + w.firstSeen.size() + " chunks settling; "
        + (chunks + w.saved.size()) + " chunks in all.";
  }

  /** Saves the chunks loaded long enough. Looks only in the client's own chunk cache, which never loads anything. */
  private void saveSettled(World w, ChunkPos center, int radius) {
    ClientChunkCache cache = w.level.getChunkSource();
    for (int x = center.x() - radius; x <= center.x() + radius; x++) {
      for (int z = center.z() - radius; z <= center.z() + radius; z++) {
        long key = ChunkPos.pack(x, z);
        if (w.saved.contains(key)) continue;
        LevelChunk chunk = cache.getChunk(x, z, ChunkStatus.FULL, false);
        if (chunk == null) {
          w.firstSeen.remove(key);
          continue;
        }
        int seen = w.firstSeen.computeIfAbsent(key, k -> ticks);
        if (ticks - seen >= SETTLE) save(w, chunk);
      }
    }
  }

  private void save(World w, LevelChunk chunk) {
    ChunkPos pos = chunk.getPos();
    // Marked first: a chunk that can't be copied isn't tried again every second.
    w.saved.add(pos.pack());
    w.firstSeen.remove(pos.pack());
    ChunkSnapshot snapshot = snapshot(w, chunk);
    io.execute(() -> {
      try {
        w.chunkFiles.write(pos, ChunkNbt.chunk(snapshot, w.blocks));
      } catch (Exception e) {
        failed("chunk " + pos.x() + ", " + pos.z(), e);
      }
    });
  }

  /** What's written of a chunk, copied now: the client keeps changing its own. */
  private static ChunkSnapshot snapshot(World w, LevelChunk chunk) {
    List<ChunkSnapshot.Section> sections = new ArrayList<>();
    LevelChunkSection[] all = chunk.getSections();
    for (int i = 0; i < all.length; i++) {
      PalettedContainerRO.PackedData<Holder<Biome>> biomes = all[i].getBiomes().pack(w.biomes);
      List<String> names = biomes.paletteEntries().stream().map(WorldCapture::biomeName).toList();
      w.biomeNames.addAll(names);
      sections.add(new ChunkSnapshot.Section(chunk.getSectionYFromSectionIndex(i), all[i].getStates().copy(), names,
          biomes.storage().map(LongStream::toArray).orElse(null)));
    }
    List<CompoundTag> blockEntities = new ArrayList<>();
    for (BlockEntity be : chunk.getBlockEntities().values()) {
      try {
        blockEntities.add(be.saveWithFullMetadata(w.level.registryAccess()));
      } catch (RuntimeException e) {
        // One the client can't save shouldn't cost the rest of the chunk.
        LOG.warn("Couldn't save the block entity at {}", be.getBlockPos(), e);
      }
    }
    return new ChunkSnapshot(chunk.getPos().x(), chunk.getPos().z(), sections, blockEntities);
  }

  /** Biomes keep the server's names, Hypixel's own ("hypixel:wilderness") included: a datapack can give them their colours. */
  private static String biomeName(Holder<Biome> biome) {
    return biome.unwrapKey().map(key -> key.identifier().toString()).orElse("minecraft:plains");
  }

  private static void keepDecorations(World w) {
    for (Entity e : w.level.entitiesForRendering()) {
      if (!isDecoration(e) || w.decorations.containsKey(e.getUUID())) continue;
      TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, w.level.registryAccess());
      if (!e.saveAsPassenger(out)) continue;
      long chunk = e.chunkPosition().pack();
      w.decorations.put(e.getUUID(), new Decoration(chunk, out.buildResult()));
      w.dirty.add(chunk);
    }
  }

  /**
   * Item frames, paintings, unnamed armor stands, block and item displays. Named stands and text
   * displays are name tags and holograms, mobs and NPCs are spawned by the game: the replay tool's
   * npcs.json has those.
   */
  static boolean isDecoration(Entity e) {
    if (e instanceof HangingEntity || e instanceof Display.BlockDisplay || e instanceof Display.ItemDisplay) return true;
    return e instanceof ArmorStand stand && !stand.hasCustomName();
  }

  /** Writes the decorations of the chunks that have new ones (all of each such chunk's), and flushes. */
  private void writeDecorations(World w) {
    Map<Long, List<CompoundTag>> byChunk = new HashMap<>();
    for (Decoration d : w.decorations.values()) {
      if (w.dirty.contains(d.chunk())) byChunk.computeIfAbsent(d.chunk(), k -> new ArrayList<>()).add(d.tag());
    }
    w.dirty.clear();
    io.execute(() -> {
      for (Map.Entry<Long, List<CompoundTag>> e : byChunk.entrySet()) {
        ChunkPos pos = ChunkPos.unpack(e.getKey());
        try {
          w.entityFiles.write(pos, ChunkNbt.entities(pos.x(), pos.z(), e.getValue()));
        } catch (Exception ex) {
          failed("the decorations of chunk " + pos.x() + ", " + pos.z(), ex);
        }
      }
      try {
        w.chunkFiles.flush();
        w.entityFiles.flush();
      } catch (IOException ex) {
        failed("region files", ex);
      }
    });
  }

  /** Writes what's left of the world, closes its files and names its folder: its number, then its area. */
  private void endWorld() {
    World w = world;
    world = null;
    writeDecorations(w);
    chunks += w.saved.size();
    if (w.saved.isEmpty() && w.decorations.isEmpty()) {
      // Nothing came: the dimension Hypixel switches through between servers, say.
      io.execute(() -> close(w));
      return;
    }
    int n = ++numbered;
    String area = w.areas.isEmpty() ? null : w.areas.getFirst();
    Path target = folder.resolve(n + (area == null ? "" : "_" + slug(area)));
    String saved = "Saved " + w.saved.size() + " chunks and " + w.decorations.size() + " decorations of "
        + (area == null ? "world " + n : area) + " in " + folder.getFileName() + "/" + target.getFileName();
    String json = GSON.toJson(info(w));
    io.execute(() -> {
      close(w);
      try {
        Files.createDirectories(w.dir);
        Files.writeString(w.dir.resolve("capture.json"), json);
        if (!target.equals(w.dir)) Files.move(w.dir, target);
        infos.add(saved);
      } catch (IOException e) {
        failed("finishing " + w.dir, e);
      }
    });
  }

  private void close(World w) {
    try {
      w.chunkFiles.close();
      w.entityFiles.close();
    } catch (IOException e) {
      failed("closing " + w.dir, e);
    }
  }

  private static JsonObject info(World w) {
    JsonObject o = new JsonObject();
    JsonArray areas = new JsonArray();
    w.areas.forEach(areas::add);
    o.add("areas", areas);
    o.addProperty("server", w.server);
    o.addProperty("dimension", w.level.dimension().identifier().toString());
    o.addProperty("minY", w.level.getMinY());
    o.addProperty("height", w.level.getHeight());
    o.addProperty("from", w.from.toString());
    o.addProperty("to", Instant.now().toString());
    o.addProperty("chunks", w.saved.size());
    o.addProperty("decorations", w.decorations.size());
    JsonArray biomes = new JsonArray();
    w.biomeNames.forEach(biomes::add);
    o.add("biomes", biomes);
    return o;
  }

  private static void readSidebar(World w) {
    List<String> lines = sidebar(w.level.getScoreboard());
    String area = Sidebar.area(lines);
    if (area != null && !w.areas.contains(area)) w.areas.add(area);
    if (w.server == null) w.server = Sidebar.server(lines);
  }

  /**
   * The sidebar's lines, top to bottom as the client draws them, without formatting: "§" and the
   * character after it go, as the client skips them, codes or not. Hypixel keeps its lines apart
   * with ones that aren't ("Your Isla§tnd", "m91§xAA"), which {@link ChatFormatting#stripFormatting}
   * leaves in.
   */
  static List<String> sidebar(Scoreboard board) {
    Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
    if (objective == null) return List.of();
    return board.listPlayerScores(objective).stream()
        .filter(e -> !e.isHidden())
        .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
            .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER))
        .map(e -> CODES.matcher(PlayerTeam.formatNameForTeam(board.getPlayersTeam(e.owner()), e.ownerName()).getString()).replaceAll(""))
        .toList();
  }

  private static String slug(String area) {
    return area.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
  }

  private void failed(String what, Exception e) {
    LOG.error("Couldn't save {}", what, e);
    problems.add("Couldn't save " + what + ": " + e);
  }

  private void tell() {
    for (String s; (s = problems.poll()) != null; ) problem.accept(s);
    for (String s; (s = infos.poll()) != null; ) info.accept(s);
  }

  private record Decoration(long chunk, CompoundTag tag) {
  }

  /** One world visited: where it's being saved, and what of it has been. */
  private final class World {
    final ClientLevel level;
    /** Until it's finished and named; an interrupted capture leaves it like this. */
    final Path dir;
    final RegionFileStorage chunkFiles;
    final RegionFileStorage entityFiles;
    final Strategy<BlockState> blocks;
    final Strategy<Holder<Biome>> biomes;
    final LongSet saved = new LongOpenHashSet();
    /** Loaded chunks not saved yet, with the tick they were first seen. */
    final Long2IntMap firstSeen = new Long2IntOpenHashMap();
    final Map<UUID, Decoration> decorations = new HashMap<>();
    /** Chunks with decorations not written yet. */
    final LongSet dirty = new LongOpenHashSet();
    final Set<String> biomeNames = new TreeSet<>();
    final List<String> areas = new ArrayList<>();
    final Instant from = Instant.now();
    String server;

    World(ClientLevel level, int entered) {
      this.level = level;
      this.dir = folder.resolve("saving-" + entered);
      String name = "Dungeon Scanner " + folder.getFileName() + "/" + dir.getFileName();
      this.chunkFiles = new RegionFileStorage(new RegionStorageInfo(name, level.dimension(), "chunk"), dir.resolve("region"), false);
      this.entityFiles = new RegionFileStorage(new RegionStorageInfo(name, level.dimension(), "entities"), dir.resolve("entities"), false);
      PalettedContainerFactory factory = level.palettedContainerFactory();
      this.blocks = factory.blockStatesStrategy();
      this.biomes = factory.biomeStrategy();
    }
  }
}
