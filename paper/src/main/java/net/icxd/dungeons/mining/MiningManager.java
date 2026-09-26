package net.icxd.dungeons.mining;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class MiningManager {
  // nextPhases is a terrible name for this variable, really what it is, is basically just
  // the next time a break animation can start.
  private static final Map<UUID, Long> NEXT_PHASES = new HashMap<>();
  private static final Map<Location, Integer> BLOCK_BREAK_PROGRESS = new HashMap<>();

  public static boolean updatePhaseCooldown(Player player, int timeToBreakInTicks) {
    final List<UUID> toRemove = new ArrayList<>();
    NEXT_PHASES.forEach((uuid, phase) -> {
      if (phase <= System.currentTimeMillis())
        toRemove.add(uuid);
    });
    toRemove.forEach(NEXT_PHASES::remove);
    if (NEXT_PHASES.containsKey(player.getUniqueId())) return false;
    nextPhase(player, timeToBreakInTicks);
    return true;
  }

  public static void nextPhase(Player player, int ticks) {
    NEXT_PHASES.put(player.getUniqueId(), System.currentTimeMillis() + ticks);
  }

  public static boolean updateAndNextPhase(Player player, int timeToBreakInTicks) {
    if (updatePhaseCooldown(player, timeToBreakInTicks)) {
      nextPhase(player, timeToBreakInTicks);
      return true;
    }
    return false;
  }

  public static void sendBlockDamage(Player player, Location location) {
    player.sendBlockDamage(location, stageToProgress(getBlockBreakProgress(location)), 0);
  }

  /** Shows crack {@code stage} (0-9, anything else clears it) on a block to everyone online. */
  public static void sendBlockDamage(Block block, int stage) {
    for (Player p : Bukkit.getOnlinePlayers()) p.sendBlockDamage(block.getLocation(), stageToProgress(stage), 0);
  }

  /** The 1.8 packet's crack stage as Paper's 0-1 progress (Paper shows stage (int) (9 * progress), 0 clears). */
  private static float stageToProgress(int stage) {
    return stage < 0 || stage > 9 ? 0 : Math.min(1f, (stage + 0.5f) / 9f);
  }

  public static long getNextPhase(Player player) {
    return NEXT_PHASES.get(player.getUniqueId());
  }

  public static int getBlockBreakProgress(Location location) {
    return BLOCK_BREAK_PROGRESS.getOrDefault(location, 0);
  }

  public static void setBlockBreakProgress(Location location, int stage) {
    BLOCK_BREAK_PROGRESS.remove(location);
    BLOCK_BREAK_PROGRESS.put(location, stage);
  }

  public static void removeBlockBreakProgress(Location location) {
    BLOCK_BREAK_PROGRESS.remove(location);
  }
}
