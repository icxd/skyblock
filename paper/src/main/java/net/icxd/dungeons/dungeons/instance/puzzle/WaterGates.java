package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;

/**
 * The Water Board's rules, apart from the world (as recorded, 2026_09_26_08_23_40 03:11.9-03:31.0,
 * and the wiki): three of the five gates in front of the chest start closed; a gate toggles the
 * moment water reaches its hole at the bottom of the board ("If the water flows into a hole that
 * corresponds to an already opened door, the door will close"); each board lever moves every block of
 * its material on the board in or out.
 */
public final class WaterGates {
    /** How many gates start closed. */
    static final int CLOSED_AT_START = 3;

    private final boolean[] closed;
    private final boolean[] wet;

    public WaterGates(boolean[] closed) {
        this.closed = closed.clone();
        this.wet = new boolean[closed.length];
    }

    /** Three of the gates, at random (the 10 ways all happen: Odin has a solution for each). */
    public static boolean[] closedAtStart(int gates, Random random) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < gates; i++) order.add(i);
        Collections.shuffle(order, random);
        boolean[] closed = new boolean[gates];
        for (int i = 0; i < Math.min(CLOSED_AT_START, gates); i++) closed[order.get(i)] = true;
        return closed;
    }

    public boolean isClosed(int gate) {
        return closed[gate];
    }

    public boolean allOpen() {
        for (boolean c : closed) if (c) return false;
        return true;
    }

    /**
     * Where the water is now at each hole; the gates of the holes it just got into toggle.
     *
     * @return those gates
     */
    public List<Integer> flow(boolean[] wetNow) {
        List<Integer> toggled = new ArrayList<>();
        for (int i = 0; i < wet.length; i++) {
            if (wetNow[i] && !wet[i]) {
                closed[i] = !closed[i];
                toggled.add(i);
            }
            wet[i] = wetNow[i];
        }
        return toggled;
    }

    /** What the board is made of, read in the room's capture frame. */
    public interface Board {
        Material type(int x, int y, int z);

        /** A sticky piston at this block pushing towards the board (to lower z). */
        boolean isPiston(int x, int y, int z);

        boolean isExtended(int x, int y, int z);
    }

    /** A sticky piston behind the board, at (x, y), and whether it's pushed out now. */
    public record Piston(int x, int y, boolean extended) {
    }

    /**
     * The board's pistons by the material of the block each moves (the block is on the board when the
     * piston is out, one further back when it's in). Pistons moving anything else aren't any lever's.
     */
    public static Map<Material, List<Piston>> pistons(PuzzleData.WaterBoard data, Board board) {
        Map<Material, List<Piston>> out = new LinkedHashMap<>();
        for (Material m : data.levers().keySet()) out.put(m, new ArrayList<>());
        int z = data.boardZ();
        for (int y = data.boardFrom()[1]; y <= data.boardTo()[1]; y++) {
            for (int x = data.boardFrom()[0]; x <= data.boardTo()[0]; x++) {
                if (!board.isPiston(x, y, z + 2)) continue;
                boolean extended = board.isExtended(x, y, z + 2);
                List<Piston> moving = out.get(board.type(x, y, extended ? z : z + 1));
                if (moving != null) moving.add(new Piston(x, y, extended));
            }
        }
        return out;
    }

    /** Which of Hypixel's four boards this is (Odin's order), or -1. */
    public static int variant(PuzzleData.WaterBoard data, Board board) {
        for (int i = 0; i < data.variants().size(); i++) {
            PuzzleData.Variant v = data.variants().get(i);
            if (board.type(v.at()[0], v.at()[1], v.at()[2]) == v.block()) return i;
        }
        return -1;
    }
}
