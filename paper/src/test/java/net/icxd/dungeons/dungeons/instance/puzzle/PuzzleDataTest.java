package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;

/** Made-up numbers here; the real files are Hypixel's layout and live in the private data repository. */
class PuzzleDataTest {
    @TempDir
    Path rooms;

    @Test
    void readsWhatIsThere() throws IOException {
        Path folder = Files.createDirectories(rooms.resolve(PuzzleData.FOLDER));
        Files.writeString(folder.resolve("tic_tac_toe.json"), """
                {"cells": [[1,2,3],[1,2,4],[1,2,5],[1,1,3],[1,1,4],[1,1,5],[1,0,3],[1,0,4],[1,0,5]], "facing": "WEST"}""");
        Files.writeString(folder.resolve("creeper_beams.json"), """
                {"creeper": [5,6,5], "lanternsY": [1,9], "chest": [5,1,5], "chestFacing": "NORTH"}""");
        Files.writeString(folder.resolve("three_weirdos.json"), "{\"npcs\": [[1,1,1]]}");
        List<String> problems = new ArrayList<>();
        PuzzleData data = PuzzleData.load(rooms, problems);
        assertNotNull(data.ticTacToe());
        assertEquals(BlockFace.WEST, data.ticTacToe().facing());
        assertArrayEquals(new int[]{1, 0, 5}, data.ticTacToe().cells().get(8));
        assertArrayEquals(new int[]{1, 9}, data.beams().lanternsY());
        assertNull(data.waterBoard());
        assertNull(data.weirdos(), "three weirdos, not one");
        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("water_board")));
    }

    @Test
    void waterBoard() {
        PuzzleData.WaterBoard board = PuzzleData.waterBoard(JsonParser.parseString("""
                {"levers": {"COAL_BLOCK": [1,2,3], "TERRACOTTA": [3,2,1]}, "waterLever": [0,0,0], "waterSource": [9,9,9],
                 "board": {"from": [1,2], "to": [7,8], "z": 20},
                 "gates": [{"wool": "RED_WOOL", "hole": [1,2,20], "z": 5}], "gatePower": [[1,2],[3,4]], "gateTop": [2,3],
                 "chest": [2,2,2], "chestFacing": "SOUTH", "variants": [{"at": [1,1,1], "block": "QUARTZ_BLOCK"}]}""").getAsJsonObject());
        assertEquals(List.of(Material.COAL_BLOCK, Material.TERRACOTTA), List.copyOf(board.levers().keySet()));
        assertEquals(Material.RED_WOOL, board.gates().get(0).wool());
        assertEquals(20, board.boardZ());
        assertEquals(2, board.gatePower().size());
        assertEquals(Material.QUARTZ_BLOCK, board.variants().get(0).block());
        assertThrows(IllegalArgumentException.class, () -> PuzzleData.waterBoard(JsonParser.parseString("""
                {"levers": {"NOT_A_BLOCK": [1,2,3]}}""").getAsJsonObject()));
    }

    @Test
    void nothingThere() {
        List<String> problems = new ArrayList<>();
        PuzzleData data = PuzzleData.load(rooms, problems);
        assertEquals(PuzzleData.NONE, data);
        assertEquals(4, problems.size());
    }

    /**
     * The real files, when they're here: -Dpuzzles.rooms (a rooms folder), else the private data
     * repository next to this one.
     */
    @Test
    void theRealOnesLoad() {
        String property = System.getProperty("puzzles.rooms");
        Path real = property != null ? Path.of(property)
                : Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent().resolveSibling("skyblock-dungeon-data/rooms");
        assumeTrue(Files.isDirectory(real.resolve(PuzzleData.FOLDER)), "no " + real.resolve(PuzzleData.FOLDER));
        List<String> problems = new ArrayList<>();
        PuzzleData data = PuzzleData.load(real, problems);
        assertEquals(List.of(), problems);
        assertEquals(9, data.ticTacToe().cells().size());
        assertEquals(6, data.waterBoard().levers().size());
        assertEquals(5, data.waterBoard().gates().size());
        assertEquals(4, data.waterBoard().variants().size());
        assertEquals(3, data.weirdos().npcs().size());
        assertNotNull(data.beams());
    }
}
