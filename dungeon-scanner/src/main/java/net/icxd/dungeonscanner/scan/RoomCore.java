package net.icxd.dungeonscanner.scan;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cell's "core": the hash of the blocks in the vertical column through its middle, from y=140
 * down to y=12. It identifies the room without caring about rotation (the middle column doesn't
 * move when a room is turned). This is a straight port of Odin's {@code WorldScan.getRoomCore} so
 * the hashes match Odin's {@code rooms.json}.
 *
 * @param highest the highest block in that column (the roof), 0 if the column is empty
 */
public record RoomCore(int hash, int highest) {

  public static RoomCore at(WorldView world, int x, int z) {
    StringBuilder sb = new StringBuilder(1024);
    boolean foundHighest = false;
    int highest = 0;
    int bedrock = 0;

    for (int y = 140; y >= 12; y--) {
      BlockState state = world.getBlockState(x, y, z);
      Block block = state.getBlock();

      if (!foundHighest) {
        if (!state.isAir() && block != Blocks.GOLD_BLOCK) {
          foundHighest = true;
          highest = y;
        } else {
          sb.append('0');
        }
      }

      if (foundHighest) {
        if (state.isAir() && bedrock >= 2 && y < 69) {
          sb.append("0".repeat(y - 11));
          break;
        }
        if (block == Blocks.BEDROCK) {
          bedrock++;
        } else {
          bedrock = 0;
          // Secrets and doors change these; leave them out so the hash stays stable.
          if (block == Blocks.OAK_PLANKS || block == Blocks.TRAPPED_CHEST || block == Blocks.CHEST) continue;
        }
        sb.append(block);
      }
    }
    return new RoomCore(sb.toString().hashCode(), highest);
  }
}
