package net.icxd.dungeons.dungeons.instance.puzzle;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Where things are in each puzzle room, in the room's capture frame (see {@link PuzzleFrame}). It's
 * Hypixel's layout (recorded, or from the puzzle solver mods), so it lives with the room captures in
 * the private data, as {@code rooms/_puzzles/<room>.json}; a puzzle without its file can't be done.
 */
public record PuzzleData(TicTacToe ticTacToe, WaterBoard waterBoard, Weirdos weirdos, Beams beams) {
    public static final PuzzleData NONE = new PuzzleData(null, null, null, null);
    /** The folder under the rooms folder. */
    public static final String FOLDER = "_puzzles";

    /**
     * @param cells  the board's nine buttons in reading order, as the player facing the board sees it
     * @param facing the way the buttons (and the item frames that replace them) face
     */
    public record TicTacToe(List<int[]> cells, BlockFace facing) {
    }

    /**
     * @param levers      the six board levers, by the material of the blocks each one moves
     * @param waterLever  the lever that lets the water out
     * @param waterSource where the water starts
     * @param boardFrom   the board's lowest x and y; its blocks are at z {@code boardZ} (on the board) or
     *                    {@code boardZ + 1} (out of it), moved by sticky pistons at {@code boardZ + 2},
     *                    powered by redstone blocks at {@code boardZ + 3}
     * @param gatePower   x and y of the redstone blocks that close a gate, at the gate's z
     * @param gateTop     x and y of the gate's block no piston moves
     * @param variants    how to tell Hypixel's four boards apart (in order), for the log
     */
    public record WaterBoard(Map<Material, int[]> levers, int[] waterLever, int[] waterSource, int[] boardFrom,
                             int[] boardTo, int boardZ, List<Gate> gates, List<int[]> gatePower, int[] gateTop,
                             int[] chest, BlockFace chestFacing, List<Variant> variants) {
    }

    /** A gate (door) in front of the Water Board chest, and the hole on the board that toggles it. */
    public record Gate(Material wool, int[] hole, int z) {
    }

    public record Variant(int[] at, Material block) {
    }

    /**
     * @param npcs        where the three stand
     * @param chestOffset from each one to their chest
     * @param facing      the way they and their chests face
     */
    public record Weirdos(List<int[]> npcs, int[] chestOffset, BlockFace facing) {
    }

    /**
     * @param creeper    the block the creeper stands in
     * @param lanternsY  the heights the sea lanterns to shoot are in
     * @param chest      where the chest appears once it blows up
     */
    public record Beams(int[] creeper, int[] lanternsY, int[] chest, BlockFace chestFacing) {
    }

    /** The puzzle files in a rooms folder; the ones that aren't there are null. */
    public static PuzzleData load(Path rooms, List<String> problems) {
        Path folder = rooms.resolve(FOLDER);
        return new PuzzleData(
                read(folder, "tic_tac_toe", PuzzleData::ticTacToe, problems),
                read(folder, "water_board", PuzzleData::waterBoard, problems),
                read(folder, "three_weirdos", PuzzleData::weirdos, problems),
                read(folder, "creeper_beams", PuzzleData::beams, problems));
    }

    private interface Parser<T> {
        T parse(JsonObject o);
    }

    private static <T> T read(Path folder, String room, Parser<T> parser, List<String> problems) {
        Path file = folder.resolve(room + ".json");
        if (!Files.exists(file)) {
            problems.add("no " + FOLDER + "/" + room + ".json");
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parser.parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            problems.add(FOLDER + "/" + room + ".json: " + e);
            return null;
        }
    }

    static TicTacToe ticTacToe(JsonObject o) {
        List<int[]> cells = positions(o.getAsJsonArray("cells"));
        if (cells.size() != 9) throw new IllegalArgumentException("a board has 9 cells, not " + cells.size());
        return new TicTacToe(cells, face(o, "facing"));
    }

    static WaterBoard waterBoard(JsonObject o) {
        Map<Material, int[]> levers = new LinkedHashMap<>();
        JsonObject l = o.getAsJsonObject("levers");
        for (String material : l.keySet()) levers.put(material(material), ints(l.getAsJsonArray(material)));
        JsonObject board = o.getAsJsonObject("board");
        List<Gate> gates = new ArrayList<>();
        for (JsonElement g : o.getAsJsonArray("gates")) {
            JsonObject gate = g.getAsJsonObject();
            gates.add(new Gate(material(gate.get("wool").getAsString()), ints(gate.getAsJsonArray("hole")), gate.get("z").getAsInt()));
        }
        List<Variant> variants = new ArrayList<>();
        if (o.has("variants")) {
            for (JsonElement v : o.getAsJsonArray("variants")) {
                JsonObject variant = v.getAsJsonObject();
                variants.add(new Variant(ints(variant.getAsJsonArray("at")), material(variant.get("block").getAsString())));
            }
        }
        return new WaterBoard(levers, ints(o.getAsJsonArray("waterLever")), ints(o.getAsJsonArray("waterSource")),
                ints(board.getAsJsonArray("from")), ints(board.getAsJsonArray("to")), board.get("z").getAsInt(), gates,
                positions(o.getAsJsonArray("gatePower")), ints(o.getAsJsonArray("gateTop")), ints(o.getAsJsonArray("chest")),
                face(o, "chestFacing"), variants);
    }

    static Weirdos weirdos(JsonObject o) {
        List<int[]> npcs = positions(o.getAsJsonArray("npcs"));
        if (npcs.size() != 3) throw new IllegalArgumentException("three weirdos, not " + npcs.size());
        return new Weirdos(npcs, ints(o.getAsJsonArray("chestOffset")), face(o, "facing"));
    }

    static Beams beams(JsonObject o) {
        return new Beams(ints(o.getAsJsonArray("creeper")), ints(o.getAsJsonArray("lanternsY")), ints(o.getAsJsonArray("chest")),
                face(o, "chestFacing"));
    }

    private static Material material(String name) {
        Material material = Material.matchMaterial(name);
        if (material == null) throw new IllegalArgumentException("unknown block " + name);
        return material;
    }

    private static BlockFace face(JsonObject o, String key) {
        return BlockFace.valueOf(o.get(key).getAsString());
    }

    private static List<int[]> positions(JsonArray a) {
        List<int[]> out = new ArrayList<>();
        for (JsonElement e : a) out.add(ints(e.getAsJsonArray()));
        return out;
    }

    private static int[] ints(JsonArray a) {
        int[] out = new int[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsInt();
        return out;
    }
}
