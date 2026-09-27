package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

/**
 * Hypixel's door opening, the same for the entrance, wither and blood doors: every block of the
 * door becomes a falling block riding an invisible bat, and the bats sink into the floor. The
 * doorway is barrier while they do, so nobody walks through before it's open, then air. Only the
 * players in the run see it (the entities are client-side packets, so they can't land or drop).
 *
 * <p>Timings and offsets are Hypixel's, from recordings: bats spawn 0.65625 below each block, start
 * sinking on the 5th tick at 0.3125 blocks a tick, the barrier goes on the 12th and the entities on
 * the 22nd.
 */
final class DoorAnimation {
    private static final double BAT_BELOW = 0.65625;
    private static final byte INVISIBLE = 0x20;

    /**
     * When the entities start sinking and how fast (blocks a tick), when the barrier goes and when the
     * entities do, in ticks from the start.
     */
    record Timing(int sinkFrom, double sinkPerTick, int barrierGone, int entitiesGone) {
    }

    /** A door's. */
    static final Timing DOOR = new Timing(5, 0.3125, 12, 22);

    private DoorAnimation() {
    }

    /** Opens a door: {@code blocks} are its x, y, z, {@code look} what it's made of, {@code viewers} who see it. */
    static void play(Plugin plugin, World world, List<int[]> blocks, Material look, List<Player> viewers) {
        play(plugin, world, blocks, Collections.nCopies(blocks.size(), look.createBlockData()), viewers, DOOR);
    }

    /** Moves blocks away the same way, each one looking as {@code looks} says, with its own timing (a lever's wall). */
    static void play(Plugin plugin, World world, List<int[]> blocks, List<BlockData> looks, List<Player> viewers, Timing timing) {
        boolean packets = Bukkit.getPluginManager().isPluginEnabled("packetevents");
        List<Integer> bats = new ArrayList<>();
        List<Integer> all = new ArrayList<>();
        if (packets) {
            for (int i = 0; i < blocks.size(); i++) {
                int[] b = blocks.get(i);
                int state = SpigotConversionUtil.fromBukkitBlockData(looks.get(i)).getGlobalId();
                int bat = Bukkit.getUnsafe().nextEntityId(world);
                int block = Bukkit.getUnsafe().nextEntityId(world);
                Vector3d at = new Vector3d(b[0] + 0.5, b[1] - BAT_BELOW, b[2] + 0.5);
                send(viewers, new WrapperPlayServerSpawnEntity(bat, Optional.of(UUID.randomUUID()), EntityTypes.BAT, at, 0, 0, 0, 0,
                        Optional.of(Vector3d.zero())));
                send(viewers, new WrapperPlayServerEntityMetadata(bat, List.of(new EntityData<>(0, EntityDataTypes.BYTE, INVISIBLE))));
                send(viewers, new WrapperPlayServerSpawnEntity(block, Optional.of(UUID.randomUUID()), EntityTypes.FALLING_BLOCK, at, 0, 0, 0,
                        state, Optional.of(Vector3d.zero())));
                send(viewers, new WrapperPlayServerSetPassengers(bat, new int[]{block}));
                bats.add(bat);
                all.add(bat);
                all.add(block);
            }
        }
        for (int[] b : blocks) world.getBlockAt(b[0], b[1], b[2]).setType(Material.BARRIER, false);

        int[] ids = all.stream().mapToInt(Integer::intValue).toArray();
        BukkitTask[] task = new BukkitTask[1];
        int[] tick = {0};
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tick[0]++;
            if (packets && tick[0] >= timing.sinkFrom() && tick[0] < timing.entitiesGone()) {
                for (int bat : bats) send(viewers, new WrapperPlayServerEntityRelativeMove(bat, 0, -timing.sinkPerTick(), 0, false));
            }
            if (tick[0] == timing.barrierGone()) {
                for (int[] b : blocks) world.getBlockAt(b[0], b[1], b[2]).setType(Material.AIR, false);
            }
            if (tick[0] >= timing.entitiesGone()) {
                if (packets) send(viewers, new WrapperPlayServerDestroyEntities(ids));
                task[0].cancel();
            }
        }, 1, 1);
    }

    private static void send(List<Player> viewers, PacketWrapper<?> packet) {
        var manager = PacketEvents.getAPI().getPlayerManager();
        for (Player viewer : viewers) {
            if (viewer.isOnline()) manager.sendPacket(viewer, packet);
        }
    }
}
