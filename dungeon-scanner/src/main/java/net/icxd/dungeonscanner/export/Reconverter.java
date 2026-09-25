package net.icxd.dungeonscanner.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.icxd.dungeonscanner.legacy.LegacyMapper;
import net.icxd.dungeonscanner.scan.RoomRotation;
import net.icxd.dungeonscanner.scan.WorldView;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rebuilds every 1.8 {@code .schematic} from its {@code .schem}, so improvements to the 1.8
 * conversion apply to rooms captured earlier without going back into the dungeon.
 *
 * <p>Also cuts L rooms captured before {@link RoomMask} down to their own blocks. Those get a new
 * hash, so they're renamed; two captures that only differed in the neighbour next to them become
 * the same file.
 */
public final class Reconverter {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  /**
   * @param rebuilt .schematic files written
   * @param trimmed L rooms cut down to their own blocks (and renamed)
   * @param merged  of those, how many turned out to be a copy of another capture and were removed
   */
  public record Result(int rebuilt, int trimmed, int merged) {
  }

  private Reconverter() {
  }

  public static Result reconvertAll(Path root, LegacyMapper mapper) throws IOException {
    int rebuilt = 0;
    int trimmed = 0;
    int merged = 0;
    for (String dir : new String[]{"rooms", "doors"}) {
      Path d = root.resolve(dir);
      if (!Files.isDirectory(d)) continue;
      List<Path> schems = new ArrayList<>();
      try (Stream<Path> files = Files.walk(d)) {
        files.filter(p -> p.toString().endsWith(".schem")).sorted().forEach(schems::add);
      }
      for (Path schem : schems) {
        if (!Files.exists(schem)) continue;
        String base = schem.getFileName().toString().replaceAll("\\.schem$", "");
        Path json = schem.resolveSibling(base + ".json");
        CompoundTag file = NbtIo.readCompressed(schem, NbtAccounter.unlimitedHeap()).getCompoundOrEmpty("Schematic");
        WorldView view = view(file);
        Capture capture = capture(view, file, false);
        JsonObject meta = Files.exists(json) ? GSON.fromJson(Files.readString(json, StandardCharsets.UTF_8), JsonObject.class) : null;

        WorldView own = meta != null && meta.has("cells") ? RoomMask.of(view, cells(meta), 0) : view;
        if (own != view) {
          Capture cut = capture(own, file, true);
          if (!SchematicWriter.hash(cut).equals(SchematicWriter.hash(capture))) {
            trimmed++;
            String id = base.substring(0, base.lastIndexOf('_'));
            Path target = schem.resolveSibling(id + "_" + SchematicWriter.hash(cut).substring(0, 10));
            boolean duplicate = !target.getFileName().toString().equals(base) && Files.exists(target.resolveSibling(target.getFileName() + ".schem"));
            if (duplicate) {
              merged++;
            } else {
              meta.addProperty("originY", meta.get("originY").getAsInt() + cut.originY);
              JsonArray size = new JsonArray();
              size.add(cut.width);
              size.add(cut.height);
              size.add(cut.length);
              meta.add("size", size);
              SchematicWriter.writeSponge(cut, target.resolveSibling(target.getFileName() + ".schem"), file.getCompoundOrEmpty("Metadata"));
              write(cut, target, meta, mapper);
              rebuilt++;
            }
            if (!target.getFileName().toString().equals(base)) {
              for (String ext : new String[]{".schem", ".schematic", ".json"}) Files.deleteIfExists(schem.resolveSibling(base + ext));
            }
            continue;
          }
        }
        write(capture, schem.resolveSibling(base), meta, mapper);
        rebuilt++;
      }
    }
    return new Result(rebuilt, trimmed, merged);
  }

  /** Writes {@code base}.schematic and, if there is metadata, {@code base}.json. */
  private static void write(Capture capture, Path base, JsonObject meta, LegacyMapper mapper) throws IOException {
    Map<String, Integer> missing = SchematicWriter.writeLegacy(capture, base.resolveSibling(base.getFileName() + ".schematic"), mapper);
    if (meta == null) return;
    meta.remove("noLegacyEquivalent");
    if (!missing.isEmpty()) {
      JsonObject m = new JsonObject();
      missing.forEach(m::addProperty);
      meta.add("noLegacyEquivalent", m);
    }
    Files.writeString(base.resolveSibling(base.getFileName() + ".json"), GSON.toJson(meta), StandardCharsets.UTF_8);
  }

  private static List<int[]> cells(JsonObject meta) {
    List<int[]> out = new ArrayList<>();
    for (JsonElement c : meta.getAsJsonArray("cells")) {
      JsonArray a = c.getAsJsonArray();
      out.add(new int[]{a.get(0).getAsInt(), a.get(1).getAsInt()});
    }
    return out;
  }

  private static Capture capture(WorldView view, CompoundTag schem, boolean trimY) {
    int w = schem.getShortOr("Width", (short) 0);
    int h = schem.getShortOr("Height", (short) 0);
    int l = schem.getShortOr("Length", (short) 0);
    return new Capture(view, 0, 0, 0, w - 1, h - 1, l - 1, RoomRotation.SOUTH, 0, 0, trimY, true);
  }

  /** A Sponge v3 schematic written by {@link SchematicWriter} as a world: blocks, block entities, entities. */
  static WorldView view(CompoundTag schem) {
    int w = schem.getShortOr("Width", (short) 0);
    int h = schem.getShortOr("Height", (short) 0);
    int l = schem.getShortOr("Length", (short) 0);
    CompoundTag blocks = schem.getCompoundOrEmpty("Blocks");

    Map<Integer, BlockState> palette = new HashMap<>();
    CompoundTag paletteTag = blocks.getCompoundOrEmpty("Palette");
    for (String key : paletteTag.keySet()) {
      BlockState state;
      try {
        state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, key, false).blockState();
      } catch (Exception e) {
        state = Blocks.AIR.defaultBlockState();
      }
      palette.put(paletteTag.getIntOr(key, 0), state);
    }
    byte[] data = blocks.getByteArray("Data").orElse(new byte[0]);
    BlockState[] states = new BlockState[w * h * l];
    int pos = 0;
    for (int i = 0; i < states.length; i++) {
      int value = 0;
      int shift = 0;
      byte b;
      do {
        b = data[pos++];
        value |= (b & 0x7F) << shift;
        shift += 7;
      } while ((b & 0x80) != 0);
      states[i] = palette.getOrDefault(value, Blocks.AIR.defaultBlockState());
    }

    Map<Long, CompoundTag> blockEntities = new HashMap<>();
    for (Tag t : blocks.getListOrEmpty("BlockEntities")) {
      if (!(t instanceof CompoundTag be)) continue;
      int[] p = be.getIntArray("Pos").orElse(null);
      if (p == null || p.length != 3) continue;
      CompoundTag full = be.getCompoundOrEmpty("Data").copy();
      full.putString("id", be.getStringOr("Id", ""));
      blockEntities.put(key(p[0], p[1], p[2]), full);
    }

    List<CompoundTag> entities = new ArrayList<>();
    for (Tag t : schem.getListOrEmpty("Entities")) {
      if (!(t instanceof CompoundTag e)) continue;
      CompoundTag full = e.getCompoundOrEmpty("Data").copy();
      full.putString("id", e.getStringOr("Id", "minecraft:unknown"));
      full.put("Pos", e.getListOrEmpty("Pos").copy());
      entities.add(full);
    }

    return new WorldView() {
      @Override
      public BlockState getBlockState(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= l) return Blocks.AIR.defaultBlockState();
        return states[(y * l + z) * w + x];
      }

      @Override
      public CompoundTag getBlockEntity(int x, int y, int z) {
        return blockEntities.get(key(x, y, z));
      }

      @Override
      public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return true;
      }

      @Override
      public int minY() {
        return 0;
      }

      @Override
      public int maxY() {
        return h;
      }

      @Override
      public List<CompoundTag> getDecorations(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return entities;
      }
    };
  }

  private static long key(int x, int y, int z) {
    return ((long) x << 40) | ((long) y << 20) | z;
  }
}
