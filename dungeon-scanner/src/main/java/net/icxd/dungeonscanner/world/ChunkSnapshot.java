package net.icxd.dungeonscanner.world;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * One chunk as the client has it, copied on the client thread so it can be written on another:
 * every section's blocks (a copy of its palette and data) and biomes (packed already, by name), and
 * its block entities' NBT.
 */
public record ChunkSnapshot(int x, int z, List<Section> sections, List<CompoundTag> blockEntities) {
  /**
   * Section {@code y} (counted in sections): its blocks, and its biomes as names with their packed
   * indices ({@code biomeData} null when there's only one).
   */
  public record Section(int y, PalettedContainer<BlockState> blocks, List<String> biomes, long[] biomeData) {
  }
}
