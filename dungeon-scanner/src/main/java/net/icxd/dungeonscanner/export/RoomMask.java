package net.icxd.dungeonscanner.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.icxd.dungeonscanner.scan.DungeonScan;
import net.icxd.dungeonscanner.scan.WorldView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A room's own blocks and nothing else. The bounding box of an L room also holds the cell it's
 * missing, which belongs to whatever room was next to it, plus the gaps around that cell. Saving
 * those would put a random neighbour into the schematic (and give every capture of the room a
 * different hash). Owned: the room's cells, gaps between two of its cells, and gap corners with
 * its cells on all four sides.
 */
final class RoomMask implements WorldView {
  private final WorldView world;
  private final Set<Long> cells;
  private final int base;

  private RoomMask(WorldView world, Set<Long> cells, int base) {
    this.world = world;
    this.cells = cells;
    this.base = base;
  }

  /**
   * @param cells grid cells as {x, z}
   * @param base  block coordinate where cell 0 starts, on both axes
   * @return {@code world} itself if the cells fill their bounding box (every shape but L)
   */
  static WorldView of(WorldView world, Collection<int[]> cells, int base) {
    Set<Long> set = new HashSet<>();
    int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    for (int[] c : cells) {
      set.add(key(c[0], c[1]));
      minX = Math.min(minX, c[0]);
      minZ = Math.min(minZ, c[1]);
      maxX = Math.max(maxX, c[0]);
      maxZ = Math.max(maxZ, c[1]);
    }
    if (set.size() == (maxX - minX + 1) * (maxZ - minZ + 1)) return world;
    return new RoomMask(world, set, base);
  }

  boolean owns(int x, int z) {
    int gx = Math.floorDiv(x - base, DungeonScan.PITCH);
    int gz = Math.floorDiv(z - base, DungeonScan.PITCH);
    boolean gapX = Math.floorMod(x - base, DungeonScan.PITCH) == DungeonScan.PITCH - 1;
    boolean gapZ = Math.floorMod(z - base, DungeonScan.PITCH) == DungeonScan.PITCH - 1;
    if (!has(gx, gz)) return false;
    if (gapX && !has(gx + 1, gz)) return false;
    if (gapZ && !has(gx, gz + 1)) return false;
    return !(gapX && gapZ) || has(gx + 1, gz + 1);
  }

  private boolean has(int x, int z) {
    return cells.contains(key(x, z));
  }

  private static long key(int x, int z) {
    return ((long) x << 32) ^ (z & 0xffffffffL);
  }

  @Override
  public BlockState getBlockState(int x, int y, int z) {
    return owns(x, z) ? world.getBlockState(x, y, z) : Blocks.AIR.defaultBlockState();
  }

  @Override
  public CompoundTag getBlockEntity(int x, int y, int z) {
    return owns(x, z) ? world.getBlockEntity(x, y, z) : null;
  }

  @Override
  public boolean isChunkLoaded(int chunkX, int chunkZ) {
    return world.isChunkLoaded(chunkX, chunkZ);
  }

  @Override
  public int minY() {
    return world.minY();
  }

  @Override
  public int maxY() {
    return world.maxY();
  }

  @Override
  public List<CompoundTag> getDecorations(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    List<CompoundTag> out = new ArrayList<>();
    for (CompoundTag e : world.getDecorations(minX, minY, minZ, maxX, maxY, maxZ)) {
      ListTag pos = e.getListOrEmpty("Pos");
      if (pos.size() == 3 && owns((int) Math.floor(pos.getDoubleOr(0, 0)), (int) Math.floor(pos.getDoubleOr(2, 0)))) out.add(e);
    }
    return out;
  }
}
