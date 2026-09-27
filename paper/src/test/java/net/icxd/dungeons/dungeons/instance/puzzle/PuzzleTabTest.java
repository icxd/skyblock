package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** The lines as recorded, and as Skyblocker reads them. */
class PuzzleTabTest {
    /** Skyblocker's pattern, on the plain text. */
    private static final Pattern SKYBLOCKER = Pattern.compile(".+?(?=:): \\[(?<state>.)](?: \\(\\w*\\))?");

    @Test
    void rows() {
        assertEquals(" ???: &7[&6&l✦&7]", PuzzleTab.row("Water Board", PuzzleTab.State.UNDISCOVERED, null));
        assertEquals(" Water Board: &7[&6&l✦&7] ", PuzzleTab.row("Water Board", PuzzleTab.State.DISCOVERED, null));
        assertEquals(" Water Board: &7[&a&l✔&7] ", PuzzleTab.row("Water Board", PuzzleTab.State.SOLVED, null));
        assertEquals(" Tic Tac Toe: &7[&c&l✖&7] &f(&bICoding&f)", PuzzleTab.row("Tic Tac Toe", PuzzleTab.State.FAILED, "ICoding"));
    }

    @Test
    void skyblockerReadsThem() {
        assertEquals("✖", state(PuzzleTab.row("Tic Tac Toe", PuzzleTab.State.FAILED, "Steve")));
        assertEquals("✔", state(PuzzleTab.row("Water Board", PuzzleTab.State.SOLVED, null)));
        assertEquals("✦", state(PuzzleTab.row("Water Board", PuzzleTab.State.DISCOVERED, null)));
        assertEquals("✦", state(PuzzleTab.row("Water Board", PuzzleTab.State.UNDISCOVERED, null)));
    }

    private static String state(String row) {
        Matcher m = SKYBLOCKER.matcher(row.replaceAll("&.", "").trim());
        assertTrue(m.matches(), row);
        return m.group("state");
    }
}
