package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * A run's puzzle rooms: each starts when someone first walks in, and they give the tab list its
 * Puzzles lines and the score its puzzles not done. Main thread.
 */
public final class RunPuzzles {
    /** By run world, for {@link PuzzleEvents} (every run has a world of its own). */
    private static final Map<World, RunPuzzles> BY_WORLD = new ConcurrentHashMap<>();
    /** The tab list's names (the wiki's), by room template. */
    private static final Map<String, String> NAMES = Map.of(
            "tic_tac_toe", "Tic Tac Toe",
            "water_board", "Water Board",
            "three_weirdos", "Three Weirdos",
            "creeper_beams", "Creeper Beams",
            "teleport_maze", "Teleport Maze",
            "blaze", "Higher Or Lower",
            "boulder", "Boulder",
            "ice_path", "Ice Path",
            "ice_fill", "Ice Fill",
            "quiz", "Quiz");

    /** A puzzle room of the run: its id in the layout, its template, and where it is. */
    public record Room(int id, String template, PuzzleFrame frame) {
    }

    private final PuzzleHost host;
    private final List<Puzzle> puzzles = new ArrayList<>();

    public RunPuzzles(PuzzleHost host, List<Room> rooms, PuzzleData data) {
        this.host = host;
        for (Room room : rooms) puzzles.add(create(room, data));
        BY_WORLD.put(host.world(), this);
    }

    private Puzzle create(Room room, PuzzleData data) {
        if (room.frame() == null) {
            host.log("The " + room.template() + " puzzle wasn't pasted");
            return new Undoable(host, room, NAMES.getOrDefault(room.template(), room.template()));
        }
        Puzzle puzzle = switch (room.template()) {
            case "tic_tac_toe" -> data.ticTacToe() == null ? null : new TicTacToePuzzle(host, room.id(), room.frame(), data.ticTacToe());
            default -> null;
        };
        if (puzzle != null) return puzzle;
        host.log("The " + room.template() + " puzzle can't be done in this run (" + (NAMES.containsKey(room.template())
                ? "no data for it" : "not a known puzzle") + ")");
        return new Undoable(host, room, NAMES.getOrDefault(room.template(), room.template()));
    }

    /** The puzzles of the run in that world, if it's a run's. */
    static RunPuzzles in(World world) {
        return world == null ? null : BY_WORLD.get(world);
    }

    public void tick(List<Player> players) {
        for (Puzzle puzzle : puzzles) {
            if (puzzle.frame == null) continue;
            if (puzzle.state() == PuzzleTab.State.UNDISCOVERED) {
                for (Player player : players) {
                    if (puzzle.frame.contains(player.getLocation().getX(), player.getLocation().getZ())) {
                        puzzle.discover();
                        break;
                    }
                }
            }
            if (puzzle.state() != PuzzleTab.State.UNDISCOVERED) puzzle.tick();
        }
    }

    /** The tab list's lines under "Puzzles: (n)", in the order of the rooms. */
    public List<String> tabRows() {
        List<String> out = new ArrayList<>();
        for (Puzzle puzzle : puzzles) out.add(puzzle.tabRow());
        return out;
    }

    public int count() {
        return puzzles.size();
    }

    /** Puzzles failed or not solved yet (or never found): each costs the score 10. */
    public int notDone() {
        int n = 0;
        for (Puzzle puzzle : puzzles) if (!puzzle.isDone()) n++;
        return n;
    }

    /** A click on a block; whether a puzzle took it. */
    boolean click(Player player, Block block, boolean right) {
        for (Puzzle puzzle : puzzles) {
            if (inside(puzzle, block) && puzzle.click(player, block, right)) {
                return true;
            }
        }
        return false;
    }

    boolean click(Player player, Entity entity) {
        for (Puzzle puzzle : puzzles) if (puzzle.click(player, entity)) return true;
        return false;
    }

    void shot(Player shooter, Block block) {
        for (Puzzle puzzle : puzzles) {
            if (inside(puzzle, block)) puzzle.shot(shooter, block);
        }
    }

    /** A lever, button or chest in a started puzzle's room (the off hand's clicks on them are stopped). */
    boolean isPuzzleBlock(Block block) {
        Material type = block.getType();
        if (type != Material.LEVER && type != Material.STONE_BUTTON && type != Material.CHEST) return false;
        for (Puzzle puzzle : puzzles) if (inside(puzzle, block)) return true;
        return false;
    }

    /** A started puzzle, and the block is in its room. */
    private static boolean inside(Puzzle puzzle, Block block) {
        return puzzle.frame != null && puzzle.state() != PuzzleTab.State.UNDISCOVERED
                && puzzle.frame.contains(block.getX() + 0.5, block.getZ() + 0.5);
    }

    boolean owns(Entity entity) {
        for (Puzzle puzzle : puzzles) if (puzzle.owns(entity)) return true;
        return false;
    }

    public void dispose() {
        BY_WORLD.remove(host.world(), this);
        for (Puzzle puzzle : puzzles) puzzle.dispose();
    }

    /** A puzzle this plugin can't run (not built yet, or its data is missing): found, never done. */
    private static final class Undoable extends Puzzle {
        Undoable(PuzzleHost host, Room room, String name) {
            super(host, room.id(), room.frame(), name);
        }

        @Override
        void start() {
        }
    }
}
