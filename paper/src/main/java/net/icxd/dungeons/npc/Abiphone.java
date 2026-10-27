package net.icxd.dungeons.npc;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.command.Callbacks;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

/**
 * An NPC calling a player, as recorded (Kat, 2026-10-25, 02:05.4): "&e✆ &bKat&e ✆ ", then "&a✆ RING...  &2&l[PICK
 * UP]" three times, six ticks apart and a RING longer each time, over twelve plings a tick apart (record, 0.8, 1.67).
 * [PICK UP] runs {@code /cb} with the call's id ({@link Callbacks}), which answers it. Whether an unanswered call
 * rings again, UNKNOWN: it doesn't. Main thread.
 */
public final class Abiphone {
    static final int RINGS = 3;
    static final long RING_TICKS = 6;
    static final int PLINGS = 12;
    static final float PLING_VOLUME = 0.8f;
    static final float PLING_PITCH = 1.67f;
    static final String PICK_UP = "&2&l[PICK UP]";

    private Abiphone() {
    }

    /** {@code caller} ("&bKat") rings them; {@code answer} is what picking up does. */
    public static void ring(Plugin plugin, Player player, String caller, Runnable answer) {
        UUID id = Callbacks.add(player, answer);
        player.sendMessage(Text.line(header(caller)));
        for (int i = 0; i < RINGS; i++) {
            Component line = Text.line(ringing(i + 1)).append(Component.text(" "))
                    .append(Text.line(PICK_UP).clickEvent(ClickEvent.runCommand("/cb " + id)));
            later(plugin, player, i * RING_TICKS, () -> player.sendMessage(line));
        }
        for (int i = 0; i < PLINGS; i++) {
            later(plugin, player, i, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.RECORDS,
                    PLING_VOLUME, PLING_PITCH));
        }
    }

    /** "&e✆ &bKat&e ✆ ". */
    static String header(String caller) {
        return "&e✆ " + caller + "&e ✆ ";
    }

    /** "&a✆ RING... RING... " for two (the space before [PICK UP] is the line's own). */
    static String ringing(int rings) {
        return "&a✆ " + "RING... ".repeat(rings);
    }

    private static void later(Plugin plugin, Player player, long ticks, Runnable run) {
        if (ticks == 0) {
            run.run();
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) run.run();
        }, ticks);
    }
}
