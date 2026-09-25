package net.icxd.dungeonscanner.scan;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** What the scanner needs from a world. Lets the scan run against a fake world in tests. */
public interface WorldView {
  BlockState getBlockState(int x, int y, int z);

  /** Block entity NBT with full metadata (id, x, y, z), or null. */
  CompoundTag getBlockEntity(int x, int y, int z);

  boolean isChunkLoaded(int chunkX, int chunkZ);

  int minY();

  /** Exclusive. */
  int maxY();

  /**
   * Decorative entities (unnamed armor stands, item frames, paintings) inside the box, saved as
   * NBT with an "id". Mobs and name-tag stands are left out; they're spawned by the game.
   */
  default List<CompoundTag> getDecorations(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    return List.of();
  }

  default boolean isLoaded(int blockX, int blockZ) {
    return isChunkLoaded(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
  }
}
