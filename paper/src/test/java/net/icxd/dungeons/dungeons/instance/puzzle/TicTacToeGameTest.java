package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.instance.puzzle.TicTacToeGame.Mark;
import net.icxd.dungeons.dungeons.instance.puzzle.TicTacToeGame.Result;

class TicTacToeGameTest {
    /** The AI in the middle, the way every game starts. */
    private static TicTacToeGame started() {
        TicTacToeGame game = new TicTacToeGame();
        game.play(TicTacToeGame.MIDDLE, Mark.X);
        return game;
    }

    private static TicTacToeGame copy(TicTacToeGame game) {
        TicTacToeGame out = new TicTacToeGame();
        for (int i = 0; i < 9; i++) if (!game.isEmpty(i)) out.play(i, game.at(i));
        return out;
    }

    /** Every game the players can play against it, with its tie-breaks from this seed: how they end. */
    private static void everyGame(TicTacToeGame game, Random random, List<Result> results) {
        for (int cell = 0; cell < 9; cell++) {
            if (!game.isEmpty(cell)) continue;
            TicTacToeGame next = copy(game);
            next.play(cell, Mark.O);
            if (next.result() == Result.PLAYING) next.play(next.aiMove(random), Mark.X);
            if (next.result() == Result.PLAYING) everyGame(next, random, results);
            else results.add(next.result());
        }
    }

    @Test
    void nobodyBeatsIt() {
        for (long seed = 0; seed < 5; seed++) {
            List<Result> results = new ArrayList<>();
            everyGame(started(), new Random(seed), results);
            assertTrue(results.size() > 50, "games: " + results.size());
            assertTrue(results.stream().noneMatch(r -> r == Result.PLAYERS_WON), "a player won with seed " + seed);
            assertTrue(results.contains(Result.TIE), "a tie is possible");
            assertTrue(results.contains(Result.AI_WON));
        }
    }

    /** The recorded game: X middle, O bottom middle, X middle left, O bottom left; the AI takes its row. */
    @Test
    void takesTheWinItIsGiven() {
        TicTacToeGame game = started();
        game.play(7, Mark.O);
        game.play(3, Mark.X);
        game.play(6, Mark.O);
        for (long seed = 0; seed < 20; seed++) assertEquals(5, game.aiMove(new Random(seed)));
        game.play(5, Mark.X);
        assertEquals(Result.AI_WON, game.result());
        assertEquals(-1, game.aiMove(new Random(0)));
    }

    @Test
    void blocksARow() {
        TicTacToeGame game = started();
        game.play(0, Mark.O);
        game.play(2, Mark.X);
        game.play(6, Mark.O);
        // O has 0 and 6: the AI must take 3 (it can't win at once: 2-4-6 is blocked by O at 6).
        assertEquals(3, game.aiMove(new Random(1)));
    }

    @Test
    void aPerfectPlayerTies() {
        Random random = new Random(7);
        TicTacToeGame game = started();
        while (game.result() == Result.PLAYING) {
            // The player's side of the same search: the move that's best for O is the AI's worst.
            int best = -1;
            int bestScore = Integer.MAX_VALUE;
            for (int cell = 0; cell < 9; cell++) {
                if (!game.isEmpty(cell)) continue;
                TicTacToeGame next = copy(game);
                next.play(cell, Mark.O);
                int score = worstFor(next);
                if (score < bestScore) {
                    bestScore = score;
                    best = cell;
                }
            }
            game.play(best, Mark.O);
            if (game.result() == Result.PLAYING) game.play(game.aiMove(random), Mark.X);
        }
        assertEquals(Result.TIE, game.result());
    }

    /** 1 if the AI wins this position with best play, 0 for a tie, -1 if it loses. */
    private static int worstFor(TicTacToeGame game) {
        Result r = game.result();
        if (r != Result.PLAYING) return r == Result.AI_WON ? 1 : r == Result.TIE ? 0 : -1;
        Mark turn = game.turn();
        int best = turn == Mark.X ? -2 : 2;
        for (int cell = 0; cell < 9; cell++) {
            if (!game.isEmpty(cell)) continue;
            TicTacToeGame next = copy(game);
            next.play(cell, turn);
            int s = worstFor(next);
            best = turn == Mark.X ? Math.max(best, s) : Math.min(best, s);
        }
        return best;
    }

    @Test
    void turnsAndTakenCells() {
        TicTacToeGame game = new TicTacToeGame();
        assertEquals(Mark.X, game.turn());
        game.play(4, Mark.X);
        assertEquals(Mark.O, game.turn());
        assertThrows(IllegalStateException.class, () -> game.play(4, Mark.O));
        assertNotEquals(4, game.aiMove(new Random(0)));
    }
}
