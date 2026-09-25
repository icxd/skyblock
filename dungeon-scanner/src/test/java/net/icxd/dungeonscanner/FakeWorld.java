package net.icxd.dungeonscanner;

import java.util.HashMap;
import java.util.Map;

import net.icxd.dungeonscanner.scan.WorldView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Sparse in-memory world, everything loaded, y 0..255. */
public final class FakeWorld implements WorldView {
  private final Map<Long, BlockState> blocks = new HashMap<>();
  private final Map<Long, CompoundTag> blockEntities = new HashMap<>();

  private static long key(int x, int y, int z) {
    return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
  }

  public void set(int x, int y, int z, BlockState state) {
    blocks.put(key(x, y, z), state);
  }

  public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
    for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, state);
  }

  public void setBlockEntity(int x, int y, int z, CompoundTag tag) {
    blockEntities.put(key(x, y, z), tag);
  }

  @Override
  public BlockState getBlockState(int x, int y, int z) {
    return blocks.getOrDefault(key(x, y, z), Blocks.AIR.defaultBlockState());
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
    return 256;
  }
}
