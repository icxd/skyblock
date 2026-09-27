package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;

/**
 * The Water Board, as recorded (2026_09_26_08_23_40 03:11.9-03:31.0, a solve). When someone walks
 * in, three of the five gates in front of the chest close (redstone blocks behind their sticky
 * pistons, and the top middle wool put in) and the chest is put behind them. Everything on the board
 * is vanilla: the water lever puts a water source at the top and the water runs down as water does;
 * each board lever puts or takes away the redstone block behind every piston that moves a block of
 * its material. The tick water gets into a hole at the bottom, that hole's gate toggles. The puzzle
 * is solved by opening the chest (no chat line), which gives a Tier V blessing. It can't be failed.
 */
final class WaterBoardPuzzle extends Puzzle {
    private final PuzzleData.WaterBoard data;
    private final Random random = new Random();
    private WaterGates gates;
    /** Each board lever's pistons, and whether each is (to be) out; by lever material. */
    private Map<Material, List<WaterGates.Piston>> pistons;
    private final List<Boolean> leverOn = new ArrayList<>();
    private boolean waterOn;
    private BlockData plug;
    private PuzzleChest chest;

    WaterBoardPuzzle(PuzzleHost host, int room, PuzzleFrame frame, PuzzleData.WaterBoard data) {
        super(host, room, frame, "Water Board");
        this.data = data;
    }

    @Override
    void start() {
        WaterGates.Board board = board();
        pistons = WaterGates.pistons(data, board);
        int variant = WaterGates.variant(data, board);
        host.log("Water Board: board " + (variant < 0 ? "?" : variant + 1) + " of 4, " + pistons.values().stream().mapToInt(List::size).sum()
                + " pistons on its levers");
        // Levers start off (Hypixel resets every lever when the run starts).
        for (int i = 0; i < data.levers().size(); i++) leverOn.add(false);
        for (int[] lever : data.levers().values()) setLever(block(lever), false, false);
        setLever(block(data.waterLever()), false, false);
        plug = block(data.waterSource()).getBlockData();

        gates = new WaterGates(WaterGates.closedAtStart(data.gates().size(), random));
        for (int i = 0; i < data.gates().size(); i++) setGate(i, gates.isClosed(i));
        chest = PuzzleChest.place(block(data.chest()), frame.face(data.chestFacing()));
    }

    @Override
    void tick() {
        if (gates == null || isOver()) return;
        boolean[] wet = new boolean[data.gates().size()];
        for (int i = 0; i < wet.length; i++) wet[i] = block(data.gates().get(i).hole()).getType() == Material.WATER;
        for (int gate : gates.flow(wet)) setGate(gate, gates.isClosed(gate));
    }

    @Override
    boolean click(Player player, Block block, boolean right) {
        if (gates == null) return false;
        if (chest != null && chest.is(block)) {
            if (right && chest.open(host, player, randomBlessing(), BLESSING_LEVEL)) solve(PuzzleTab.AFTER_CHEST);
            return true;
        }
        int lever = 0;
        for (Map.Entry<Material, int[]> e : data.levers().entrySet()) {
            if (local(block, List.of(e.getValue())) != null) {
                if (right) flipBoardLever(lever, e.getKey(), block);
                return true;
            }
            lever++;
        }
        if (local(block, List.of(data.waterLever())) != null) {
            if (right) flipWater(block);
            return true;
        }
        return false;
    }

    private void flipBoardLever(int index, Material material, Block lever) {
        boolean on = !leverOn.get(index);
        leverOn.set(index, on);
        setLever(lever, on, true);
        List<WaterGates.Piston> moved = new ArrayList<>();
        for (WaterGates.Piston p : pistons.get(material)) {
            boolean out = !p.extended();
            block(p.x(), p.y(), data.boardZ() + 3).setType(out ? Material.REDSTONE_BLOCK : Material.AIR, true);
            moved.add(new WaterGates.Piston(p.x(), p.y(), out));
        }
        pistons.put(material, moved);
    }

    /** The water on or off. Off puts the block that was there back (what Hypixel does then is UNKNOWN: only "on" was recorded). */
    private void flipWater(Block lever) {
        waterOn = !waterOn;
        setLever(lever, waterOn, true);
        Block source = block(data.waterSource());
        if (waterOn) {
            Levelled water = (Levelled) Material.WATER.createBlockData();
            water.setLevel(0);
            source.setBlockData(water, true);
        } else {
            source.setBlockData(plug, true);
        }
    }

    /** A lever's look (and click), without powering anything: Hypixel moves the board itself. */
    private void setLever(Block block, boolean on, boolean click) {
        if (!(block.getBlockData() instanceof Switch lever)) return;
        if (click) {
            Location at = block.getLocation().add(0.5, 0.5, 0.5);
            at.getWorld().playSound(at, Sound.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3f, on ? 0.6f : 0.5f);
        }
        lever.setPowered(on);
        block.setBlockData(lever, false);
    }

    /**
     * Closes a gate the way Hypixel does: redstone blocks behind its pistons (stone when open) and its
     * wool in the top middle, which no piston moves.
     */
    private void setGate(int index, boolean closed) {
        PuzzleData.Gate gate = data.gates().get(index);
        for (int[] power : data.gatePower()) {
            block(power[0], power[1], gate.z()).setType(closed ? Material.REDSTONE_BLOCK : Material.STONE, true);
        }
        block(data.gateTop()[0], data.gateTop()[1], gate.z()).setType(closed ? gate.wool() : Material.AIR, false);
    }

    /** The board in the capture's frame, for finding the pistons and the variant. */
    private WaterGates.Board board() {
        BlockFace in = frame.face(BlockFace.NORTH);
        return new WaterGates.Board() {
            @Override
            public Material type(int x, int y, int z) {
                return block(x, y, z).getType();
            }

            @Override
            public boolean isPiston(int x, int y, int z) {
                return block(x, y, z).getBlockData() instanceof Piston piston && block(x, y, z).getType() == Material.STICKY_PISTON
                        && piston.getFacing() == in;
            }

            @Override
            public boolean isExtended(int x, int y, int z) {
                return block(x, y, z).getBlockData() instanceof Piston piston && piston.isExtended();
            }
        };
    }

    @Override
    void dispose() {
        if (chest != null) chest.dispose();
    }
}
