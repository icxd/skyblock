package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.Random;

import net.icxd.dungeons.dungeons.instance.puzzle.TicTacToeGame.Mark;

/**
 * Whose move it is in a Tic Tac Toe puzzle, without the world ({@link TicTacToePuzzle} shows the
 * moves). The AI opens in the middle. A player may put an O on an empty cell when it's the players'
 * turn and the AI isn't thinking; after that the AI thinks (the puzzle gives it
 * {@link TicTacToePuzzle#AI_DELAY}) and answers once. The game ends once, lost or solved, and then
 * nobody moves again.
 */
final class TicTacToeTurns {
    /** How the game stands after a move. */
    enum Outcome {
        /** Still going. */
        PLAYING,
        /** Three in a row for the AI: the puzzle is failed. */
        LOST,
        /** A full board with no row for the AI (a tie; a win for the players can't happen, but would count): solved. */
        SOLVED,
        /** It had ended already, and that was told. */
        OVER
    }

    private final TicTacToeGame game = new TicTacToeGame();
    private boolean thinking;
    private boolean over;

    /** The AI's first move: the middle (the wiki: "In floors 0-3 it always starts in the middle"). */
    int open() {
        game.play(TicTacToeGame.MIDDLE, Mark.X);
        return TicTacToeGame.MIDDLE;
    }

    boolean mayPlay(int cell) {
        return !over && !thinking && game.isEmpty(cell) && game.turn() == Mark.O;
    }

    boolean isThinking() {
        return thinking;
    }

    boolean isOver() {
        return over;
    }

    /** A player's O there (only when {@link #mayPlay}); if the game goes on, the AI starts thinking. */
    Outcome play(int cell) {
        if (!mayPlay(cell)) throw new IllegalStateException("not the players' move at " + cell);
        game.play(cell, Mark.O);
        Outcome outcome = outcome();
        thinking = outcome == Outcome.PLAYING;
        return outcome;
    }

    /** The AI's X, done thinking: the cell it took, or -1 if it had nothing to answer. */
    int aiMove(Random random) {
        if (!thinking) return -1;
        thinking = false;
        int cell = game.aiMove(random);
        game.play(cell, Mark.X);
        return cell;
    }

    /** How the game stands: an ending is given once, and {@link Outcome#OVER} after that. */
    Outcome outcome() {
        if (over) return Outcome.OVER;
        TicTacToeGame.Result result = game.result();
        if (result == TicTacToeGame.Result.PLAYING) return Outcome.PLAYING;
        over = true;
        return result == TicTacToeGame.Result.AI_WON ? Outcome.LOST : Outcome.SOLVED;
    }

    Mark at(int cell) {
        return game.at(cell);
    }
}
