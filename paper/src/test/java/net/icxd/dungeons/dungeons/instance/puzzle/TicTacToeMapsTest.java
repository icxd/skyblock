package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TicTacToeMapsTest {
    /** Skyblocker tells the marks apart by the middle pixel: 114 for X, 33 for O. */
    @Test
    void theMiddlePixelSaysWhichMark() {
        assertEquals(114, TicTacToeMaps.draw(TicTacToeGame.Mark.X)[8256]);
        assertEquals(33, TicTacToeMaps.draw(TicTacToeGame.Mark.O)[8256]);
    }

    @Test
    void redOnGrey() {
        for (TicTacToeGame.Mark mark : new TicTacToeGame.Mark[]{TicTacToeGame.Mark.X, TicTacToeGame.Mark.O}) {
            byte[] pixels = TicTacToeMaps.draw(mark);
            int red = 0;
            for (byte p : pixels) {
                assertTrue(p == TicTacToeMaps.RED || p == TicTacToeMaps.GREY);
                if (p == TicTacToeMaps.RED) red++;
            }
            assertTrue(red > 1000 && red < 8000, mark + ": " + red);
            // The corners are grey.
            assertEquals(TicTacToeMaps.GREY, pixels[0]);
            assertEquals(TicTacToeMaps.GREY, pixels[128 * 128 - 1]);
        }
    }
}
