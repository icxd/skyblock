package net.icxd.dungeonscanner.export;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.icxd.dungeonscanner.scan.RoomRotation;
import net.icxd.dungeonscanner.scan.WorldView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A box of blocks copied out of the world and turned into a canonical frame: rotated by
 * {@code rotation} around a pivot block, then shifted so its min corner is 0,0,0. Block states,
 * block entities and decorative entities all turn with it.
 */
public final class Capture {
  public final int width;
  public final int height;
  public final int length;
  /** World y of local y = 0. */
  public final int originY;
  /** Indexed (y * length + z) * width + x, like schematics. */
  public final BlockState[] blocks;
  /** Block entity NBT (still with its modern "id", without x/y/z) by block index. */
  public final Map<Integer, CompoundTag> blockEntities = new LinkedHashMap<>();
  /** Entity NBT with local "Pos"/"Rotation". */
  public final List<CompoundTag> entities = new ArrayList<>();

  private final RoomRotation rotation;
  private final int pivotX;
  private final int pivotZ;
  private final int shiftX;
  private final int shiftZ;

  /**
   * @param minX..maxZ inclusive world box
   * @param pivotX     world block the rotation turns around (the roof marker)
   * @param trimY      shrink the y range to the blocks actually present
   */
  public Capture(WorldView world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                 RoomRotation rotation, int pivotX, int pivotZ, boolean trimY, boolean withEntities) {
    this.rotation = rotation;
    this.pivotX = pivotX;
    this.pivotZ = pivotZ;

    if (trimY) {
      int lo = Integer.MAX_VALUE;
      int hi = Integer.MIN_VALUE;
      for (int y = minY; y <= maxY; y++) {
        boolean any = false;
        for (int x = minX; x <= maxX && !any; x++) {
          for (int z = minZ; z <= maxZ && !any; z++) {
            if (!world.getBlockState(x, y, z).isAir()) any = true;
          }
        }
        if (any) {
          lo = Math.min(lo, y);
          hi = y;
        }
      }
      if (lo == Integer.MAX_VALUE) {
        lo = minY;
        hi = minY;
      }
      minY = lo;
      maxY = hi;
    }
    this.originY = minY;

    int[] a = local(minX, minZ);
    int[] b = local(maxX, maxZ);
    this.shiftX = Math.min(a[0], b[0]);
    this.shiftZ = Math.min(a[1], b[1]);
    this.width = Math.abs(a[0] - b[0]) + 1;
    this.length = Math.abs(a[1] - b[1]) + 1;
    this.height = maxY - minY + 1;
    this.blocks = new BlockState[width * height * length];
    java.util.Arrays.fill(blocks, Blocks.AIR.defaultBlockState());

    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        int[] l = local(x, z);
        int lx = l[0] - shiftX;
        int lz = l[1] - shiftZ;
        for (int y = minY; y <= maxY; y++) {
          int index = ((y - minY) * length + lz) * width + lx;
          BlockState state = world.getBlockState(x, y, z);
          blocks[index] = state.rotate(rotation.blockRotation());
          CompoundTag be = world.getBlockEntity(x, y, z);
          if (be != null) {
            CompoundTag copy = be.copy();
            copy.remove("x");
            copy.remove("y");
            copy.remove("z");
            blockEntities.put(index, copy);
          }
        }
      }
    }

    if (withEntities) {
      for (CompoundTag e : world.getDecorations(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1)) {
        entities.add(localEntity(e));
      }
    }
  }

  public int index(int x, int y, int z) {
    return (y * length + z) * width + x;
  }

  public BlockState get(int x, int y, int z) {
    return blocks[index(x, y, z)];
  }

  /** Local (unshifted) x, z of a world block. */
  private int[] local(int x, int z) {
    int dx = x - pivotX;
    int dz = z - pivotZ;
    return new int[]{rotation.toLocalX(dx, dz), rotation.toLocalZ(dx, dz)};
  }

  /** Local position of a world block, as used for blocks in this capture. */
  public int[] toLocal(int x, int y, int z) {
    int[] l = local(x, z);
    return new int[]{l[0] - shiftX, y - originY, l[1] - shiftZ};
  }

  /**
   * Entities have continuous positions; turning around the pivot block's centre keeps them where
   * the blocks around them went.
   */
  private CompoundTag localEntity(CompoundTag tag) {
    CompoundTag e = tag.copy();
    e.remove("UUID");
    ListTag pos = e.getListOrEmpty("Pos");
    if (pos.size() == 3) {
      double dx = pos.getDoubleOr(0, 0) - pivotX - 0.5;
      double dz = pos.getDoubleOr(2, 0) - pivotZ - 0.5;
      double lx = localX(dx, dz) + 0.5 - shiftX;
      double lz = localZ(dx, dz) + 0.5 - shiftZ;
      ListTag p = new ListTag();
      p.add(DoubleTag.valueOf(lx));
      p.add(DoubleTag.valueOf(pos.getDoubleOr(1, 0) - originY));
      p.add(DoubleTag.valueOf(lz));
      e.put("Pos", p);
    }
    ListTag rot = e.getListOrEmpty("Rotation");
    if (rot.size() == 2) {
      ListTag r = new ListTag();
      r.add(FloatTag.valueOf(rot.getFloatOr(0, 0) + 90f * rotation.quarterTurns()));
      r.add(FloatTag.valueOf(rot.getFloatOr(1, 0)));
      e.put("Rotation", r);
    }
    e.getIntArray("block_pos").ifPresent(bp -> {
      if (bp.length == 3) {
        int[] l = toLocal(bp[0], bp[1], bp[2]);
        e.putIntArray("block_pos", l);
      }
    });
    // Item frames: 3D direction id (2 north, 3 south, 4 west, 5 east). Paintings: 2D (0 south, 1 west, 2 north, 3 east).
    Tag facing = e.get("Facing");
    if (facing != null) {
      int f = e.getByteOr("Facing", (byte) 0);
      int[] cw = {0, 1, 5, 4, 2, 3}; // north->east, south->west, west->north, east->south
      for (int i = 0; i < rotation.quarterTurns(); i++) f = cw[f];
      e.putByte("Facing", (byte) f);
    }
    if (e.get("facing") != null) {
      int f = e.getByteOr("facing", (byte) 0);
      e.putByte("facing", (byte) Math.floorMod(f + rotation.quarterTurns(), 4));
    }
    return e;
  }

  private double localX(double dx, double dz) {
    return switch (rotation) {
      case SOUTH -> dx;
      case EAST -> -dz;
      case NORTH -> -dx;
      case WEST -> dz;
    };
  }

  private double localZ(double dx, double dz) {
    return switch (rotation) {
      case SOUTH -> dz;
      case EAST -> dx;
      case NORTH -> -dz;
      case WEST -> -dx;
    };
  }
}
