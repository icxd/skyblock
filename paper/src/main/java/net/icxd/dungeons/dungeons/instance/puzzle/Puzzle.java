package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * One puzzle room of a run. It starts when someone first walks in (as recorded: the Tic Tac Toe AI
 * moves and the Water Board closes its gates right then) and ends solved or failed; its tab line
 * follows a moment later ({@link PuzzleTab}).
 */
abstract class Puzzle {
    /** Level of the blessing a puzzle gives (the wiki: "a Tier V Blessing"; recorded: Blessing of Life V from the Water Board). */
    static final int BLESSING_LEVEL = 5;
    /**
     * The blessings a puzzle can give below Floor IV (Time only comes from the Quiz). Which one is
     * random as far as anyone knows (UNKNOWN; the one recorded was Life).
     */
    static final List<String> BLESSINGS = List.of("Stone", "Life", "Power", "Wisdom");

    final PuzzleHost host;
    final int room;
    final PuzzleFrame frame;
    private final String name;
    private PuzzleTab.State state = PuzzleTab.State.UNDISCOVERED;
    private PuzzleTab.State shown = PuzzleTab.State.UNDISCOVERED;
    private String failedBy;

    Puzzle(PuzzleHost host, int room, PuzzleFrame frame, String name) {
        this.host = host;
        this.room = room;
        this.frame = frame;
        this.name = name;
    }

    String name() {
        return name;
    }

    PuzzleTab.State state() {
        return state;
    }

    boolean isDone() {
        return state == PuzzleTab.State.SOLVED;
    }

    boolean isOver() {
        return state == PuzzleTab.State.SOLVED || state == PuzzleTab.State.FAILED;
    }

    String tabRow() {
        return PuzzleTab.row(name, shown, failedBy);
    }

    /** Someone walked in for the first time. */
    final void discover() {
        if (state != PuzzleTab.State.UNDISCOVERED) return;
        state = PuzzleTab.State.DISCOVERED;
        start();
        host.later(PuzzleTab.FOUND, () -> {
            if (shown == PuzzleTab.State.UNDISCOVERED) shown = PuzzleTab.State.DISCOVERED;
        });
    }

    /** Sets the room up for the puzzle. */
    abstract void start();

    /** Every tick once it's started. */
    void tick() {
    }

    /**
     * A click on a block in the room.
     *
     * @param right a right click (else a left one)
     * @return whether it was this puzzle's (the click does nothing else then)
     */
    boolean click(Player player, Block block, boolean right) {
        return false;
    }

    /** A click on an entity; whether it was one of this puzzle's. */
    boolean click(Player player, Entity entity) {
        return false;
    }

    /** Something a player shot hit a block. */
    void shot(Player shooter, Block block) {
    }

    /** One of the puzzle's own entities (nobody hurts or moves them). */
    boolean owns(Entity entity) {
        return false;
    }

    /** Takes everything it put in the world away. */
    void dispose() {
    }

    void solve(long tabDelay) {
        if (isOver()) return;
        state = PuzzleTab.State.SOLVED;
        host.solved(room);
        host.later(tabDelay, () -> shown = PuzzleTab.State.SOLVED);
    }

    void fail(Player by, long tabDelay) {
        if (isOver()) return;
        state = PuzzleTab.State.FAILED;
        failedBy = by.getName();
        host.failed(room);
        host.later(tabDelay, () -> shown = PuzzleTab.State.FAILED);
    }

    Block block(int[] xyz) {
        int[] w = frame.block(xyz);
        return host.world().getBlockAt(w[0], w[1], w[2]);
    }

    Block block(int x, int y, int z) {
        return block(new int[]{x, y, z});
    }

    /** The middle of a block of the capture, at its bottom. */
    Location feet(int[] xyz) {
        double[] p = frame.point(xyz[0] + 0.5, xyz[1], xyz[2] + 0.5);
        return new Location(host.world(), p[0], p[1], p[2]);
    }

    /** The block's position in the capture, or null if it isn't one of these. */
    int[] local(Block block, List<int[]> candidates) {
        for (int[] c : candidates) {
            int[] w = frame.block(c);
            if (block.getX() == w[0] && block.getY() == w[1] && block.getZ() == w[2]) return c;
        }
        return null;
    }

    static String randomBlessing() {
        return BLESSINGS.get(ThreadLocalRandom.current().nextInt(BLESSINGS.size()));
    }

    /** "&bName": how players are named in puzzle lines (aqua as recorded; see {@link PuzzleTab}). */
    static String named(Player player) {
        return "&b" + player.getName();
    }
}
