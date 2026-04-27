package net.icxd.dungeons.mining;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
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
    BlockPosition position = new BlockPosition(location.getX(), location.getY(), location.getZ());
    Packet<?> packet = new PacketPlayOutBlockBreakAnimation(0, position, getBlockBreakProgress(location));
    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
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
