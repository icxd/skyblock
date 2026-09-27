package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A game of Tic Tac Toe against Hypixel's AI. The AI is X and moves first, in the middle on the
 * Entrance to Floor III (the wiki: "In floors 0-3 it always starts in the middle"); the players are
 * O. The wiki and the forum say it can't be beaten, only tied, and in the one recorded game it took
 * the win it was given; whether it's a perfect player is UNKNOWN, so this one is (minimax, the
 * quickest win or the slowest loss, a random one of the moves that are equally good).
 *
 * <p>Cells are numbered 0-8 in reading order, as the players facing the board see it.
 */
public final class TicTacToeGame {
    public enum Mark { NONE, X, O }

    public enum Result { PLAYING, AI_WON, PLAYERS_WON, TIE }

    public static final int MIDDLE = 4;
    static final int[][] LINES = {{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

    private final Mark[] cells = new Mark[9];

    public TicTacToeGame() {
        Arrays.fill(cells, Mark.NONE);
    }

    public Mark at(int cell) {
        return cells[cell];
    }

    public boolean isEmpty(int cell) {
        return cells[cell] == Mark.NONE;
    }

    public void play(int cell, Mark mark) {
        if (!isEmpty(cell)) throw new IllegalStateException("cell " + cell + " is taken");
        cells[cell] = mark;
    }

    /** Whose turn it is: X when both have as many marks (X starts). */
    public Mark turn() {
        int x = 0;
        int o = 0;
        for (Mark m : cells) {
            if (m == Mark.X) x++;
            else if (m == Mark.O) o++;
        }
        return x == o ? Mark.X : Mark.O;
    }

    public Result result() {
        Mark winner = winner(cells);
        if (winner == Mark.X) return Result.AI_WON;
        if (winner == Mark.O) return Result.PLAYERS_WON;
        return full(cells) ? Result.TIE : Result.PLAYING;
    }

    /** The AI's move (as X): the best there is, or -1 if the game is over. */
    public int aiMove(Random random) {
        if (result() != Result.PLAYING) return -1;
        int best = Integer.MIN_VALUE;
        List<Integer> moves = new ArrayList<>();
        for (int cell = 0; cell < 9; cell++) {
            if (cells[cell] != Mark.NONE) continue;
            cells[cell] = Mark.X;
            int score = minimax(cells, Mark.O, 1);
            cells[cell] = Mark.NONE;
            if (score > best) {
                best = score;
                moves.clear();
            }
            if (score == best) moves.add(cell);
        }
        return moves.get(random.nextInt(moves.size()));
    }

    /** From X's side: 10 - depth for a win, depth - 10 for a loss, 0 for a tie. */
    private static int minimax(Mark[] board, Mark toMove, int depth) {
        Mark winner = winner(board);
        if (winner == Mark.X) return 10 - depth;
        if (winner == Mark.O) return depth - 10;
        if (full(board)) return 0;
        int best = toMove == Mark.X ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int cell = 0; cell < 9; cell++) {
            if (board[cell] != Mark.NONE) continue;
            board[cell] = toMove;
            int score = minimax(board, toMove == Mark.X ? Mark.O : Mark.X, depth + 1);
            board[cell] = Mark.NONE;
            best = toMove == Mark.X ? Math.max(best, score) : Math.min(best, score);
        }
        return best;
    }

    static Mark winner(Mark[] board) {
        for (int[] line : LINES) {
            Mark m = board[line[0]];
            if (m != Mark.NONE && m == board[line[1]] && m == board[line[2]]) return m;
        }
        return Mark.NONE;
    }

    private static boolean full(Mark[] board) {
        for (Mark m : board) if (m == Mark.NONE) return false;
        return true;
    }
}
