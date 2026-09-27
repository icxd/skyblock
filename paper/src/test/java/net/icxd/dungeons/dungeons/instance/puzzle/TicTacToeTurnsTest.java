package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.instance.puzzle.TicTacToeGame.Mark;
import net.icxd.dungeons.dungeons.instance.puzzle.TicTacToeTurns.Outcome;

class TicTacToeTurnsTest {
    private static final long SEED = 49;

    private static TicTacToeTurns opened() {
        TicTacToeTurns turns = new TicTacToeTurns();
        assertEquals(TicTacToeGame.MIDDLE, turns.open());
        return turns;
    }

    @Test
    void theAiOpensInTheMiddle() {
        TicTacToeTurns turns = opened();
        assertEquals(Mark.X, turns.at(TicTacToeGame.MIDDLE));
        assertFalse(turns.mayPlay(TicTacToeGame.MIDDLE));
        for (int cell = 0; cell < 9; cell++) if (cell != TicTacToeGame.MIDDLE) assertTrue(turns.mayPlay(cell), "cell " + cell);
    }

    /** While the AI thinks (3 seconds), nobody can move; then it answers once. */
    @Test
    void noMoveWhileItThinks() {
        TicTacToeTurns turns = opened();
        Random random = new Random(SEED);
        assertEquals(Outcome.PLAYING, turns.play(0));
        assertTrue(turns.isThinking());
        for (int cell = 0; cell < 9; cell++) assertFalse(turns.mayPlay(cell), "cell " + cell);
        assertThrows(IllegalStateException.class, () -> turns.play(1));
        int answer = turns.aiMove(random);
        assertNotEquals(-1, answer);
        assertEquals(Mark.X, turns.at(answer));
        assertFalse(turns.isThinking());
        assertEquals(-1, turns.aiMove(random), "a second answer to the same move");
        assertEquals(Outcome.PLAYING, turns.outcome());
        assertTrue(turns.mayPlay(firstFree(turns)));
    }

    @Test
    void theAiOnlyMovesWhenItsItsTurn() {
        assertEquals(-1, opened().aiMove(new Random(SEED)));
    }

    /** An O on an edge first: the AI can force its three in a row, and does. */
    @Test
    void aLostGameEndsOnce() {
        TicTacToeTurns turns = opened();
        Random random = new Random(SEED);
        Outcome outcome = turns.play(1);
        while (outcome == Outcome.PLAYING) {
            turns.aiMove(random);
            outcome = turns.outcome();
            if (outcome == Outcome.PLAYING) outcome = turns.play(firstFree(turns));
        }
        assertEquals(Outcome.LOST, outcome);
        assertEquals(Outcome.OVER, turns.outcome(), "the end is told once");
        assertOver(turns, random);
    }

    /**
     * Every game the players can play (with this seed's tie-breaks): each ends exactly once, lost or
     * solved, and after that nobody moves.
     */
    @Test
    void everyGameEndsOnce() {
        List<Outcome> ends = new ArrayList<>();
        everyGame(new ArrayList<>(), ends);
        assertTrue(ends.size() > 50, ends.size() + " games");
        assertTrue(ends.contains(Outcome.LOST));
        assertTrue(ends.contains(Outcome.SOLVED));
        assertFalse(ends.contains(Outcome.OVER));
    }

    /** Plays the players' moves so far again from the start (the same seed, so the same answers), then each next move. */
    private static void everyGame(List<Integer> moves, List<Outcome> ends) {
        for (int cell = 0; cell < 9; cell++) {
            TicTacToeTurns turns = opened();
            Random random = new Random(SEED);
            for (int move : moves) {
                assertEquals(Outcome.PLAYING, turns.play(move));
                turns.aiMove(random);
                assertEquals(Outcome.PLAYING, turns.outcome());
            }
            if (!turns.mayPlay(cell)) continue;
            Outcome outcome = turns.play(cell);
            if (outcome == Outcome.PLAYING) {
                assertNotEquals(-1, turns.aiMove(random));
                outcome = turns.outcome();
            }
            if (outcome == Outcome.PLAYING) {
                List<Integer> next = new ArrayList<>(moves);
                next.add(cell);
                everyGame(next, ends);
                continue;
            }
            ends.add(outcome);
            assertEquals(Outcome.OVER, turns.outcome());
            assertOver(turns, random);
        }
    }

    private static void assertOver(TicTacToeTurns turns, Random random) {
        assertTrue(turns.isOver());
        for (int cell = 0; cell < 9; cell++) assertFalse(turns.mayPlay(cell), "cell " + cell);
        assertEquals(-1, turns.aiMove(random));
    }

    private static int firstFree(TicTacToeTurns turns) {
        for (int cell = 0; cell < 9; cell++) if (turns.at(cell) == Mark.NONE) return cell;
        throw new AssertionError("the board is full");
    }
}
