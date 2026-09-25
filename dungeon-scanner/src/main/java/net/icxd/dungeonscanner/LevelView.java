package net.icxd.dungeonscanner;

import java.util.ArrayList;
import java.util.List;

import net.icxd.dungeonscanner.scan.WorldView;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** {@link WorldView} over the client's world: only reads what the server already sent. */
final class LevelView implements WorldView {
  private final ClientLevel level;
  private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

  LevelView(ClientLevel level) {
    this.level = level;
  }

  @Override
  public BlockState getBlockState(int x, int y, int z) {
    return level.getBlockState(pos.set(x, y, z));
  }

  @Override
  public CompoundTag getBlockEntity(int x, int y, int z) {
    BlockEntity be = level.getBlockEntity(pos.set(x, y, z));
    return be == null ? null : be.saveWithFullMetadata(level.registryAccess());
  }

  @Override
  public boolean isChunkLoaded(int chunkX, int chunkZ) {
    return level.getChunkSource().hasChunk(chunkX, chunkZ);
  }

  @Override
  public int minY() {
    return level.getMinY();
  }

  @Override
  public int maxY() {
    return level.getMinY() + level.getHeight();
  }

  @Override
  public List<CompoundTag> getDecorations(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    List<CompoundTag> out = new ArrayList<>();
    AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    for (Entity e : level.getEntities((Entity) null, box, LevelView::isDecoration)) {
      TagValueOutput tag = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
      if (e.saveAsPassenger(tag)) out.add(tag.buildResult());
    }
    return out;
  }

  /** Item frames, paintings and unnamed armor stands; named stands are mob name tags. */
  private static boolean isDecoration(Entity e) {
    if (e instanceof HangingEntity) return true;
    return e instanceof ArmorStand stand && !stand.hasCustomName();
  }
}
