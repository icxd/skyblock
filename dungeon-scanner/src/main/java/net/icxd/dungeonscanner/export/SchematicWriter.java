package net.icxd.dungeonscanner.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import net.icxd.dungeonscanner.legacy.LegacyBlock;
import net.icxd.dungeonscanner.legacy.LegacyMapper;
import net.icxd.dungeonscanner.legacy.LegacyTileEntities;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.state.BlockState;

/** Writes a {@link Capture} as a Sponge v3 {@code .schem} (modern) and an MCEdit {@code .schematic} (1.8). */
public final class SchematicWriter {
  private SchematicWriter() {
  }

  /** Content hash, so the same room captured twice is only saved once. */
  public static String hash(Capture c) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update((c.width + "x" + c.height + "x" + c.length).getBytes());
      Map<BlockState, String> names = new HashMap<>();
      for (BlockState s : c.blocks) md.update(names.computeIfAbsent(s, BlockStateParser::serialize).getBytes());
      c.blockEntities.forEach((i, t) -> md.update((i + t.toString()).getBytes()));
      return HexFormat.of().formatHex(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Sponge schematic v3, readable by WorldEdit 7 / FAWE on modern servers. */
  public static void writeSponge(Capture c, Path file, CompoundTag metadata) throws IOException {
    Map<String, Integer> palette = new LinkedHashMap<>();
    ByteArrayOutputStream data = new ByteArrayOutputStream(c.blocks.length);
    Map<BlockState, Integer> ids = new HashMap<>();
    for (BlockState s : c.blocks) {
      int id = ids.computeIfAbsent(s, k -> {
        String name = BlockStateParser.serialize(k);
        return palette.computeIfAbsent(name, n -> palette.size());
      });
      writeVarInt(data, id);
    }

    CompoundTag paletteTag = new CompoundTag();
    palette.forEach(paletteTag::putInt);
    ListTag blockEntities = new ListTag();
    c.blockEntities.forEach((index, tag) -> {
      CompoundTag be = new CompoundTag();
      int x = index % c.width;
      int z = (index / c.width) % c.length;
      int y = index / (c.width * c.length);
      be.putIntArray("Pos", new int[]{x, y, z});
      be.putString("Id", tag.getStringOr("id", "minecraft:unknown"));
      CompoundTag rest = tag.copy();
      rest.remove("id");
      be.put("Data", rest);
      blockEntities.add(be);
    });
    CompoundTag blocks = new CompoundTag();
    blocks.put("Palette", paletteTag);
    blocks.putByteArray("Data", data.toByteArray());
    blocks.put("BlockEntities", blockEntities);

    ListTag entities = new ListTag();
    for (CompoundTag e : c.entities) {
      CompoundTag entry = new CompoundTag();
      ListTag pos = e.getListOrEmpty("Pos");
      entry.put("Pos", pos.copy());
      entry.putString("Id", e.getStringOr("id", "minecraft:unknown"));
      CompoundTag rest = e.copy();
      rest.remove("id");
      entry.put("Data", rest);
      entities.add(entry);
    }

    CompoundTag schem = new CompoundTag();
    schem.putInt("Version", 3);
    schem.putInt("DataVersion", SharedConstants.WORLD_VERSION);
    schem.putShort("Width", (short) c.width);
    schem.putShort("Height", (short) c.height);
    schem.putShort("Length", (short) c.length);
    schem.putIntArray("Offset", new int[]{0, 0, 0});
    schem.put("Metadata", metadata);
    schem.put("Blocks", blocks);
    schem.put("Entities", entities);
    CompoundTag root = new CompoundTag();
    root.put("Schematic", schem);
    Files.createDirectories(file.getParent());
    NbtIo.writeCompressed(root, file);
  }

  /**
   * MCEdit/WorldEdit 6 schematic with 1.8 block ids, for a 1.8 server.
   *
   * @return modern states that have no 1.8 equivalent (written as air), with counts
   */
  public static Map<String, Integer> writeLegacy(Capture c, Path file, LegacyMapper mapper) throws IOException {
    byte[] ids = new byte[c.blocks.length];
    byte[] data = new byte[c.blocks.length];
    Map<String, Integer> missing = new TreeMap<>();
    ListTag tileEntities = new ListTag();
    for (int y = 0; y < c.height; y++) {
      for (int z = 0; z < c.length; z++) {
        for (int x = 0; x < c.width; x++) {
          int i = c.index(x, y, z);
          BlockState state = c.blocks[i];
          LegacyBlock legacy = mapper.toLegacy(state).orElse(null);
          if (legacy == null) {
            missing.merge(BlockStateParser.serialize(state), 1, Integer::sum);
            continue;
          }
          ids[i] = (byte) legacy.id();
          data[i] = (byte) legacy.data();
          LegacyTileEntities.convert(state, c.blockEntities.get(i), x, y, z).ifPresent(tileEntities::add);
        }
      }
    }

    CompoundTag root = new CompoundTag();
    root.putShort("Width", (short) c.width);
    root.putShort("Height", (short) c.height);
    root.putShort("Length", (short) c.length);
    root.putString("Materials", "Alpha");
    root.putByteArray("Blocks", ids);
    root.putByteArray("Data", data);
    root.put("TileEntities", tileEntities);
    root.put("Entities", new ListTag());
    root.putInt("WEOriginX", 0);
    root.putInt("WEOriginY", 0);
    root.putInt("WEOriginZ", 0);
    root.putInt("WEOffsetX", 0);
    root.putInt("WEOffsetY", 0);
    root.putInt("WEOffsetZ", 0);
    Files.createDirectories(file.getParent());
    NbtIo.writeCompressed(root, file);
    return missing;
  }

  private static void writeVarInt(ByteArrayOutputStream out, int value) {
    while ((value & ~0x7F) != 0) {
      out.write((value & 0x7F) | 0x80);
      value >>>= 7;
    }
    out.write(value);
  }
}
