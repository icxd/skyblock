package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Creeper Beams (never recorded; the wiki and the solver mods). A creeper stands on a sea lantern in
 * the middle of the room with more sea lanterns on the walls and ceiling; shooting two lanterns joins
 * them with a beam, and a beam that goes through the creeper turns both into prismarine (the state
 * Skyblocker's solver waits for). Four of those and the creeper blows up, "revealing a treasure
 * chest containing a level V Blessing" (the wiki). The puzzle is solved when the chest appears (Odin
 * takes that for solved; no chat line was ever seen, as with the Water Board), and the chest gives
 * the blessing when it's opened. It can't be failed.
 *
 * <p>How a beam is made in 26.2 (shooting or clicking) and what it looks like are UNKNOWN: here a
 * lantern is picked by shooting or clicking it, the second pick makes the beam if it goes through
 * the creeper and otherwise drops the first ({@link BeamPairs}), and a made beam is a line of
 * particles.
 */
final class CreeperBeamsPuzzle extends Puzzle {
    /** Beams it takes (the wiki: "four different beams"). */
    static final int BEAMS = 4;
    /** Lanterns around the middle, as far as the room goes (Skyblocker looks 15 blocks each way). */
    private static final int REACH = 15;
    /** How often the beams are drawn. */
    private static final int DRAW_EVERY = 10;
    private static final Particle.DustOptions BEAM = new Particle.DustOptions(Color.fromRGB(0x55FFFF), 1f);

    private final PuzzleData.Beams data;
    private final List<int[]> lanterns = new ArrayList<>();
    private final BeamPairs pairs;
    private Creeper creeper;
    private PuzzleChest chest;
    private int ticks;

    CreeperBeamsPuzzle(PuzzleHost host, int room, PuzzleFrame frame, PuzzleData.Beams data) {
        super(host, room, frame, "Creeper Beams");
        this.data = data;
        int[] c = data.creeper();
        this.pairs = new BeamPairs(new double[]{c[0] + 0.5, c[1], c[2] + 0.5}, BEAMS);
    }

    @Override
    void start() {
        int[] c = data.creeper();
        for (int y = data.lanternsY()[0]; y <= data.lanternsY()[1]; y++) {
            for (int x = c[0] - REACH; x <= c[0] + REACH + 1; x++) {
                for (int z = c[2] - REACH; z <= c[2] + REACH + 1; z++) {
                    if (x < 0 || z < 0 || x >= frame.sizeX() || z >= frame.sizeZ()) continue;
                    if (block(x, y, z).getType() == Material.SEA_LANTERN) lanterns.add(new int[]{x, y, z});
                }
            }
        }
        host.log("Creeper Beams: " + lanterns.size() + " sea lanterns");
        creeper = host.world().spawn(feet(c), Creeper.class, cr -> {
            cr.setAI(false);
            cr.setInvulnerable(true);
            cr.setSilent(true);
            cr.setGravity(false);
            cr.setPersistent(false);
            cr.setRemoveWhenFarAway(false);
        });
    }

    @Override
    void tick() {
        if (++ticks % DRAW_EVERY != 0 || pairs.beams().isEmpty() || chest != null) return;
        for (int[][] beam : pairs.beams()) draw(beam[0], beam[1]);
    }

    @Override
    boolean click(Player player, Block block, boolean right) {
        if (chest != null && chest.is(block)) {
            if (right) chest.open(host, player, randomBlessing(), BLESSING_LEVEL);
            return true;
        }
        int[] lantern = local(block, lanterns);
        if (lantern == null) return false;
        pick(player, lantern);
        return true;
    }

    @Override
    void shot(Player shooter, Block block) {
        int[] lantern = local(block, lanterns);
        if (lantern != null) pick(shooter, lantern);
    }

    private void pick(Player player, int[] lantern) {
        if (creeper == null) return;
        Location at = block(lantern).getLocation().add(0.5, 0.5, 0.5);
        switch (pairs.pick(player.getUniqueId(), lantern)) {
            case FIRST -> {
                player.playSound(at, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 1f, 1.5f);
                at.getWorld().spawnParticle(Particle.DUST, at, 12, 0.35, 0.35, 0.35, 0, BEAM);
            }
            case MISSED -> player.playSound(at, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.BLOCKS, 1f, 0.5f);
            case BEAM -> {
                int[][] beam = pairs.last();
                block(beam[0]).setType(Material.PRISMARINE, false);
                block(beam[1]).setType(Material.PRISMARINE, false);
                at.getWorld().playSound(at, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1f, 1.5f);
                draw(beam[0], beam[1]);
                if (pairs.isDone()) explode();
            }
            case NONE -> {
            }
        }
    }

    /** The creeper blows up and the chest is there: that's the puzzle solved. */
    private void explode() {
        Location at = creeper.getLocation().add(0, 0.85, 0);
        at.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 4f, 1f);
        creeper.remove();
        creeper = null;
        // The chest is walled in under the creeper's lantern in both captures; what the explosion does
        // to that is UNKNOWN, so it clears the data's box around it (the wiki: "revealing" the chest).
        if (!data.reveal().isEmpty()) {
            int[] from = data.reveal().get(0);
            int[] to = data.reveal().get(1);
            for (int x = from[0]; x <= to[0]; x++) {
                for (int y = from[1]; y <= to[1]; y++) {
                    for (int z = from[2]; z <= to[2]; z++) block(x, y, z).setType(Material.AIR, false);
                }
            }
        }
        chest = PuzzleChest.place(block(data.chest()), frame.face(data.chestFacing()));
        // How long its tab line takes is UNKNOWN (never recorded): as long as after a chest is opened.
        solve(PuzzleTab.AFTER_CHEST);
    }

    private void draw(int[] a, int[] b) {
        Location from = block(a).getLocation().add(0.5, 0.5, 0.5);
        Location to = block(b).getLocation().add(0.5, 0.5, 0.5);
        double length = from.distance(to);
        for (double d = 0; d <= length; d += 0.5) {
            Location p = from.clone().add(to.toVector().subtract(from.toVector()).multiply(d / length));
            p.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, BEAM);
        }
    }

    @Override
    boolean owns(Entity entity) {
        return creeper != null && creeper.equals(entity);
    }

    @Override
    boolean click(Player player, Entity entity) {
        return owns(entity);
    }

    @Override
    void dispose() {
        if (creeper != null) creeper.remove();
        if (chest != null) chest.dispose();
    }
}
