package net.icxd.dungeons.economy;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * The chime Hypixel makes as coins change hands, recorded with each deposit, withdrawal and sale
 * (2026-10-24): a note block's pling at volume 8 and Hypixel's highest pitch, 255/63, which the client
 * plays as 2.
 */
public final class CoinSound {
    static final float VOLUME = 8;
    static final float PITCH = 255 / 63f;

    private CoinSound() {
    }

    public static void play(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, VOLUME, PITCH);
    }
}
