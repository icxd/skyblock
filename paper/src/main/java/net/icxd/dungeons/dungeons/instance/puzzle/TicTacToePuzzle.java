package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

/**
 * Tic Tac Toe, as recorded (2026_09_26_08_23_40 03:48.4-04:06.8, a lost game): the board is nine
 * stone buttons on a wall. The AI (X) takes the middle as soon as someone walks in. A player (O)
 * clicks a button: it disappears and an item frame with the O map hangs in its place, with a pling
 * for them; 3 seconds later the AI puts its X up the same way. Three in a row for the AI fails the
 * puzzle ("PUZZLE FAIL! ... lost Tic Tac Toe! Yikes!"); a full board with no row solves it
 * ("PUZZLE SOLVED! ... tied Tic Tac Toe! Good job!", SkyHanni's pattern).
 */
final class TicTacToePuzzle extends Puzzle {
    /** Ticks from a player's move to the AI's (04:01.7 and 04:06.4, each 3.0 s after the O). */
    static final long AI_DELAY = 60;
    /** The player's move cue: a pling at them, volume 8, pitch 4.05 as sent (clients stop at 2). */
    private static final float PLING_VOLUME = 8;
    private static final float PLING_PITCH = 2;

    private final PuzzleData.TicTacToe data;
    private final TicTacToeGame game = new TicTacToeGame();
    private final Random random = new Random();
    private final List<ItemFrame> frames = new ArrayList<>();
    private boolean aiToMove;
    private Player last;

    TicTacToePuzzle(PuzzleHost host, int room, PuzzleFrame frame, PuzzleData.TicTacToe data) {
        super(host, room, frame, "Tic Tac Toe");
        this.data = data;
    }

    @Override
    void start() {
        // The board as it was built: nine buttons (the captures have the middle taken already).
        for (int[] cell : data.cells()) {
            Block block = block(cell);
            Switch button = (Switch) Material.STONE_BUTTON.createBlockData();
            button.setAttachedFace(FaceAttachable.AttachedFace.WALL);
            button.setFacing(frame.face(data.facing()));
            block.setBlockData(button, false);
        }
        // Floors up to III: always in the middle first (the wiki).
        mark(TicTacToeGame.MIDDLE, TicTacToeGame.Mark.X);
    }

    @Override
    boolean click(Player player, Block block, boolean right) {
        int cell = data.cells().indexOf(local(block, data.cells()));
        if (cell < 0) return false;
        if (!right || isOver() || aiToMove || !game.isEmpty(cell) || game.turn() != TicTacToeGame.Mark.O) return true;
        Location at = block.getLocation().add(0.5, 0.5, 0.5);
        at.getWorld().playSound(at, Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 1f, 1f);
        mark(cell, TicTacToeGame.Mark.O);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.RECORDS, PLING_VOLUME, PLING_PITCH);
        last = player;
        if (!finished()) {
            aiToMove = true;
            host.later(AI_DELAY, this::aiMove);
        }
        return true;
    }

    private void aiMove() {
        aiToMove = false;
        if (isOver()) return;
        int cell = game.aiMove(random);
        if (cell >= 0) mark(cell, TicTacToeGame.Mark.X);
        finished();
    }

    /** Ends the game if it's over; whether it is. */
    private boolean finished() {
        TicTacToeGame.Result result = game.result();
        if (result == TicTacToeGame.Result.PLAYING || last == null) return false;
        if (result == TicTacToeGame.Result.AI_WON) {
            host.tell("&c&lPUZZLE FAIL! " + named(last) + " &elost Tic Tac Toe! &4Y&ci&6k&ee&as&2!");
            fail(last, PuzzleTab.FAILED);
        } else {
            // A tie (a win can't happen against this AI, but would count too).
            host.tell("&a&lPUZZLE SOLVED! " + named(last) + " &etied Tic Tac Toe! &4G&co&6o&ed&a &2j&bo&3b&5!");
            solve(PuzzleTab.FAILED);
            // How the blessing comes for Tic Tac Toe is UNKNOWN (never seen); the wiki only says a
            // solved puzzle gives the team a Tier V one, so it's given straight away.
            host.blessing(last, randomBlessing(), BLESSING_LEVEL);
        }
        return true;
    }

    /** A move: the button goes and an item frame with the mark's map hangs there. */
    private void mark(int cell, TicTacToeGame.Mark mark) {
        game.play(cell, mark);
        Block block = block(data.cells().get(cell));
        block.setType(Material.AIR, false);
        BlockFace facing = frame.face(data.facing());
        frames.add(block.getWorld().spawn(block.getLocation(), ItemFrame.class, f -> {
            f.setFacingDirection(facing, true);
            f.setItem(TicTacToeMaps.item(mark), false);
            f.setFixed(true);
            f.setInvulnerable(true);
            f.setPersistent(false);
        }));
    }

    @Override
    boolean owns(Entity entity) {
        return entity instanceof ItemFrame && frames.contains(entity);
    }

    @Override
    boolean click(Player player, Entity entity) {
        return owns(entity);
    }

    @Override
    void dispose() {
        frames.forEach(Entity::remove);
    }
}
