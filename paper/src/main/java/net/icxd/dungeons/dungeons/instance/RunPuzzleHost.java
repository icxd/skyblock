package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.instance.puzzle.PuzzleData;
import net.icxd.dungeons.dungeons.instance.puzzle.PuzzleFrame;
import net.icxd.dungeons.dungeons.instance.puzzle.PuzzleHost;
import net.icxd.dungeons.dungeons.instance.puzzle.RunPuzzles;

/** A run, as its puzzles see it. */
final class RunPuzzleHost implements PuzzleHost {
    private final DungeonRun run;
    private final Plugin plugin;
    private final RunLayout layout;

    private RunPuzzleHost(DungeonRun run, Plugin plugin, RunLayout layout) {
        this.run = run;
        this.plugin = plugin;
        this.layout = layout;
    }

    /** The run's puzzle rooms, in layout order. */
    static RunPuzzles puzzles(DungeonRun run, Plugin plugin, RunLayout layout, PuzzleData data) {
        List<RunPuzzles.Room> rooms = new ArrayList<>();
        for (PlacedRoom room : layout.rooms()) {
            if (room.type() != RoomType.PUZZLE) continue;
            RoomFrame f = layout.frame(room);
            PuzzleFrame frame = f == null ? null : new PuzzleFrame(f.minX(), f.minZ(), f.sizeX(), f.sizeZ(), f.turns());
            rooms.add(new RunPuzzles.Room(room.id(), room.template().getId(), frame));
        }
        return new RunPuzzles(new RunPuzzleHost(run, plugin, layout), rooms, data);
    }

    @Override
    public World world() {
        return run.world;
    }

    @Override
    public List<Player> players() {
        return run.players();
    }

    @Override
    public void tell(String message) {
        run.tell(message);
    }

    @Override
    public boolean running() {
        return run.phase() == DungeonRun.Phase.RUNNING;
    }

    @Override
    public void later(long ticks, Runnable task) {
        run.later(ticks, task);
    }

    @Override
    public void solved(int room) {
        run.clearedRoom(layout.room(room));
    }

    @Override
    public void failed(int room) {
        run.puzzleFailed(layout.room(room));
    }

    @Override
    public void log(String message) {
        plugin.getLogger().info("Run " + run.id + ": " + message);
    }

    @Override
    public void blessing(Player finder, String blessing, int level) {
        run.blessingFound(finder, blessing, level);
    }
}
