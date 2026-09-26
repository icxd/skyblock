package net.icxd.dungeons.mining;

import net.icxd.dungeons.session.PlayerSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MiningManager {
    private static final Map<Location, Integer> BLOCK_BREAK_PROGRESS = new HashMap<>();

    /**
     * Whether the player's next crack stage may start now; if so, the one after waits {@code ticks}.
     * (The wait is kept in their session.)
     */
    public static boolean updatePhaseCooldown(Player player, int ticks) {
        PlayerSession session = PlayerSession.of(player);
        long now = System.currentTimeMillis();
        if (session.getNextBreakPhase() > now) return false;
        session.setNextBreakPhase(now + ticks * 50L);
        return true;
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
