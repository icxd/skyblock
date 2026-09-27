package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;

/** What the puzzles need from the run they're in. */
public interface PuzzleHost {
    World world();

    /** The members in the run's world. */
    List<Player> players();

    /** A chat line (with {@code &} colour codes) to everyone in the run. */
    void tell(String message);

    /** Runs a task later, unless the run is over by then. */
    void later(long ticks, Runnable task);

    /** A puzzle room was solved: it's done on the map (and counts as a cleared room). */
    void solved(int room);

    /** A puzzle room was failed: its red cross on the map. */
    void failed(int room);

    /** A line for the server log. */
    void log(String message);

    /** A blessing found: {@code blessing} is "Stone", "Life", "Power", "Wisdom" or "Time". */
    void blessing(Player finder, String blessing, int level);
}
