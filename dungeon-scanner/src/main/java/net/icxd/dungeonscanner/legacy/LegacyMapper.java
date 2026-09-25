package net.icxd.dungeonscanner.legacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Dynamic;

import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Turns modern block states back into 1.8 id:data values.
 *
 * <p>Minecraft still ships the table it uses to upgrade pre-1.13 worlds ({@link BlockStateData}:
 * legacy id:data to a flattened 1.13 state). Running every 1.8 id:data through it and then through
 * the normal data fixers gives the modern state each one becomes; inverting that gives the answer.
 * Hypixel's servers run 1.8 internally, so every block in a dungeon came from exactly such a state.
 *
 * <p>Several modern states come from the same legacy value (stair shapes, fence connections,
 * waterlogging... are computed, not stored in 1.8), so the candidate sharing the most property
 * values wins (see {@link #sharedProperties}). Blocks whose 1.8 form kept part of their state in a tile entity (skulls, flower
 * pots, banners) aren't in the table and are handled by {@link LegacyTileEntities}.
 */
public final class LegacyMapper {
  /** Highest block id in 1.8 (dark oak door). */
  public static final int MAX_LEGACY_ID = 197;
  /** Data version the flattening table is expressed in (17w47a). */
  private static final int FLATTENING_VERSION = 1451;

  private record Candidate(LegacyBlock legacy, BlockState state) {
  }

  private final Map<Block, List<Candidate>> byBlock = new HashMap<>();

  public LegacyMapper() {
    int current = SharedConstants.WORLD_VERSION;
    for (int id = 0; id <= MAX_LEGACY_ID; id++) {
      for (int data = 0; data < 16; data++) {
        Dynamic<?> flattened = BlockStateData.getTag(id << 4 | data);
        if (flattened == null) continue;
        Dynamic<?> upgraded = DataFixers.getDataFixer().update(References.BLOCK_STATE, flattened, FLATTENING_VERSION, current);
        Optional<BlockState> state = BlockState.CODEC.parse(upgraded).result();
        if (state.isEmpty()) continue;
        // Invalid data values fall back to the block's default; keep the first (valid) one only.
        List<Candidate> list = byBlock.computeIfAbsent(state.get().getBlock(), b -> new ArrayList<>());
        if (list.stream().noneMatch(c -> c.state() == state.get())) {
          list.add(new Candidate(new LegacyBlock(id, data), state.get()));
        }
      }
    }
  }

  /** The 1.8 block for {@code state}, or empty if nothing in 1.8 turns into this block. */
  public Optional<LegacyBlock> toLegacy(BlockState state) {
    if (state.isAir()) return Optional.of(LegacyBlock.AIR);
    Optional<LegacyBlock> special = LegacyTileEntities.specialBlock(state);
    if (special.isPresent()) return special;

    List<Candidate> candidates = byBlock.get(state.getBlock());
    if (candidates == null) return Optional.empty();
    Candidate best = null;
    int bestScore = -1;
    for (Candidate c : candidates) {
      if (c.state() == state) return Optional.of(c.legacy());
      int score = sharedProperties(state, c.state());
      if (score > bestScore) {
        best = c;
        bestScore = score;
      }
    }
    return Optional.of(best.legacy());
  }

  /**
   * Properties that pick which half of the 1.8 data value is used (door/plant halves, bed parts,
   * slab type) outweigh everything else: an upper door half must never map to a lower one just
   * because it shares more of the other properties.
   */
  private static int sharedProperties(BlockState a, BlockState b) {
    int same = 0;
    for (Property<?> p : a.getProperties()) {
      if (!b.hasProperty(p) || !a.getValue(p).equals(b.getValue(p))) continue;
      String name = p.getName();
      same += name.equals("half") || name.equals("part") || name.equals("type") ? 100 : 1;
    }
    return same;
  }

  /** How many modern blocks have a 1.8 equivalent (for tests / diagnostics). */
  public int size() {
    return byBlock.size();
  }
}
