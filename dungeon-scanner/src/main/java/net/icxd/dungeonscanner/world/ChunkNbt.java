package net.icxd.dungeonscanner.world;

import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.Strategy;

/**
 * Chunks as Minecraft 26.2 saves them ({@code SerializableChunkData} for {@code region/},
 * {@code EntityStorage} for {@code entities/}), from what the client has: blocks, biomes and block
 * entities, marked fully generated so a server uses them as they are. Light is left for the server
 * to work out as it loads them ({@code isLightOn} false), and heightmaps too (they're made when
 * missing); nothing is waiting to tick.
 */
public final class ChunkNbt {
  private ChunkNbt() {
  }

  /** A chunk for a {@code region/} file. Blocks are packed with {@code blocks}, the world's own strategy. */
  public static CompoundTag chunk(ChunkSnapshot c, Strategy<BlockState> blocks) {
    CompoundTag tag = new CompoundTag();
    tag.putInt("DataVersion", SharedConstants.WORLD_VERSION);
    tag.putInt("xPos", c.x());
    tag.putInt("yPos", c.sections().isEmpty() ? 0 : c.sections().getFirst().y());
    tag.putInt("zPos", c.z());
    tag.putLong("LastUpdate", 0);
    tag.putLong("InhabitedTime", 0);
    tag.putString("Status", "minecraft:full");

    ListTag sections = new ListTag();
    for (ChunkSnapshot.Section s : c.sections()) {
      PalettedContainerRO.PackedData<BlockState> packed = s.blocks().pack(blocks);
      CompoundTag section = new CompoundTag();
      section.putByte("Y", (byte) s.y());
      section.put("block_states", palette(packed.paletteEntries().stream().map(NbtUtils::writeBlockState).toList(),
          packed.storage().map(LongStream::toArray).orElse(null)));
      section.put("biomes", palette(s.biomes().stream().map(StringTag::valueOf).toList(), s.biomeData()));
      sections.add(section);
    }
    tag.put("sections", sections);
    tag.putBoolean("isLightOn", false);
    ListTag blockEntities = new ListTag();
    blockEntities.addAll(c.blockEntities());
    tag.put("block_entities", blockEntities);
    tag.put("Heightmaps", new CompoundTag());
    CompoundTag structures = new CompoundTag();
    structures.put("starts", new CompoundTag());
    structures.put("References", new CompoundTag());
    tag.put("structures", structures);
    tag.put("block_ticks", new ListTag());
    tag.put("fluid_ticks", new ListTag());
    tag.put("PostProcessing", new ListTag());
    return tag;
  }

  /** The entities of chunk {@code x, z} for an {@code entities/} file, each saved with its "id". */
  public static CompoundTag entities(int x, int z, Collection<CompoundTag> entities) {
    CompoundTag tag = new CompoundTag();
    tag.putInt("DataVersion", SharedConstants.WORLD_VERSION);
    tag.putIntArray("Position", new int[]{x, z});
    ListTag list = new ListTag();
    list.addAll(entities);
    tag.put("Entities", list);
    return tag;
  }

  /** A paletted container as its codec writes it: the palette, and the packed indices unless there's one entry. */
  private static CompoundTag palette(List<? extends Tag> entries, long[] data) {
    CompoundTag tag = new CompoundTag();
    ListTag palette = new ListTag();
    palette.addAll(entries);
    tag.put("palette", palette);
    if (data != null) tag.putLongArray("data", data);
    return tag;
  }
}
