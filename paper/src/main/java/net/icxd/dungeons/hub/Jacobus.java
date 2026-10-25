package net.icxd.dungeons.hub;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.utils.Text;

/**
 * Jacobus, in the Hub's Thaumaturgist (from npcs/hub.json), as recorded (2026-10-25, 00:52.7): a click has him say
 * a line in a villager's voice at 0.7, and 0.8 s later his menu opens with a click (see {@link JacobusMenu}). His
 * lines are the wiki's nine, one at random (UNKNOWN: how he picks; the recorded one was "An investment in knowledge
 * pays the best interest."), written as the recorded one was. Main thread.
 */
final class Jacobus {
    static final String ID = "JACOBUS";
    static final List<String> LINES = List.of(
            "Empty pockets never held anyone back. Only empty heads and empty hearts can do that.",
            "Wealth consists not in having great possessions, but in having few wants.",
            "Coins isn't everything, but everything needs coins.",
            "An investment in knowledge pays the best interest.",
            "The real measure of your wealth is how much you'd be worth if you lost all your coins.",
            "It's not the Steve who has too little, but the Steve who craves more, who is poor.",
            "You must gain control over your coins or the lack of it will forever control you.",
            "If you live for having it all, what you have is never enough.",
            "Coins don't buy happiness, but it helps.");
    static final Voice VOICE = Voice.villager(0.7f);
    /** The menu came 0.8 s after his line (00:52.7, 00:53.5). */
    static final long MENU_TICKS = 16;
    private static final Random RANDOM = new Random();
    private static final Set<UUID> OPENING = new HashSet<>();

    private Jacobus() {
    }

    static void click(Plugin plugin, Player player) {
        UUID id = player.getUniqueId();
        if (!OPENING.add(id)) return;
        player.sendMessage(Text.line(line(LINES.get(RANDOM.nextInt(LINES.size())))));
        VOICE.speak(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            OPENING.remove(id);
            if (player.isOnline()) JacobusMenu.show(player, MenuSound.CLICK);
        }, MENU_TICKS);
    }

    /** "&e[NPC] Jacobus&f: &rAn investment in knowledge pays the best interest.", as recorded. */
    static String line(String said) {
        return "&e[NPC] Jacobus&f: &r" + said;
    }
}
