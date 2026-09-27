package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lidded;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import net.icxd.dungeons.utils.Text;

/**
 * A puzzle's reward chest. Opening it doesn't open a menu: as recorded for the Water Board chest
 * (2026_09_26_08_23_40 03:29.7-03:31.0, the same as a secret chest with a blessing) the lid goes up
 * and stays up, the chest sound plays, then five harp notes going up, and 1.3 seconds after the
 * click the blessing: its name over the chest and the DUNGEON BUFF lines.
 */
final class PuzzleChest {
    /** Ticks after the click, and pitches, of the harp notes (03:30.0, 30.2, 30.4, 30.7, 31.0). */
    private static final long[] NOTE_TICKS = {6, 10, 14, 20, 26};
    private static final float[] NOTE_PITCHES = {0.794f, 0.889f, 1.0f, 1.095f, 1.19f};
    /** When the blessing comes (03:31.0). */
    static final long BLESSING = 26;

    final Block block;
    private boolean opened;
    private ArmorStand label;

    private PuzzleChest(Block block) {
        this.block = block;
    }

    /** Puts a chest there, facing that way (in the world). */
    static PuzzleChest place(Block block, BlockFace facing) {
        block.setType(Material.CHEST, false);
        if (block.getBlockData() instanceof Directional directional) {
            directional.setFacing(facing);
            block.setBlockData(directional, false);
        }
        return new PuzzleChest(block);
    }

    boolean isOpened() {
        return opened;
    }

    boolean is(Block other) {
        return block.equals(other);
    }

    /**
     * Opens it for this player, with a blessing for them.
     *
     * @return false if it was open already ("This chest has already been searched!")
     */
    boolean open(PuzzleHost host, Player player, String blessing, int level) {
        if (opened) {
            player.sendMessage(Text.line("&cThis chest has already been searched!"));
            return false;
        }
        opened = true;
        lid();
        Location at = block.getLocation().add(0.5, 0.5, 0.5);
        // Volume 0.5, pitch between 0.9 and 1 as recorded (it's random, like vanilla's).
        at.getWorld().playSound(at, Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5f, 0.9f + ThreadLocalRandom.current().nextFloat() * 0.1f);
        for (int i = 0; i < NOTE_TICKS.length; i++) {
            float pitch = NOTE_PITCHES[i];
            host.later(NOTE_TICKS[i], () -> {
                if (host.running()) at.getWorld().playSound(at, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 1f, pitch);
            });
        }
        host.later(BLESSING, () -> {
            // A chest opened just before the end gives nothing after it (the score is told).
            if (!host.running()) return;
            // Where it was recorded: the middle of the chest, an eighth of a block down.
            Location stand = block.getLocation().add(0.5, -0.125, 0.5);
            label = stand.getWorld().spawn(stand, ArmorStand.class, s -> {
                s.setVisible(false);
                s.setGravity(false);
                s.setInvulnerable(true);
                s.setMarker(true);
                s.setPersistent(false);
                s.customName(Text.line("&dBlessing of " + blessing));
                s.setCustomNameVisible(true);
            });
            host.blessing(player, blessing, level);
        });
        return true;
    }

    /** Lid up for good (a chest someone opened stays open). */
    private void lid() {
        if (block.getState(false) instanceof Lidded lidded) lidded.open();
    }

    void dispose() {
        if (label != null) label.remove();
    }
}
