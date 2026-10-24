package net.icxd.dungeons.hub;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.bank.BankMenu;
import net.icxd.dungeons.npc.Npc;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.quest.QuestProgress;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * The Banker, in the Hub's Bank (from npcs/hub.json), as recorded (2026-10-24): the first time, his
 * three lines in a villager's voice, and with the last, Saving Up starts with "Talk to the Banker" done
 * (its +5 SkyBlock XP); the boss bar stays where it was. After that he opens the Personal Bank Account,
 * speaking as it opens. Main thread.
 */
final class Banker {
    static final String ID = "BANKER";
    static final List<String> LINES = List.of(
            "&e[NPC] Banker&f: &r&fHello there!",
            "&e[NPC] Banker&f: &r&fYou may want to store your &6Coins &fin a safe place while you are off adventuring.",
            "&e[NPC] Banker&f: &r&fStoring them in your &6Bank &fkeeps them safe and allows you to earn interest at the start of every season!");
    /** Ticks before each line (recorded at 05:14.3, 05:15.7 and 05:17.8). */
    static final long[] LINE_TICKS = {0, 28, 70};

    private static final Set<UUID> TALKING = new HashSet<>();

    private Banker() {
    }

    static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || TALKING.contains(player.getUniqueId())) return;
        // A profile that heard him without Saving Up starting (it left mid-way) hears him again.
        if (!Npc.firstTalk(user.profile(), ID) && Quests.started(user.profile(), Quests.SAVING_UP)) {
            BankMenu.account(player, null);
            Voice.VILLAGER.speak(player);
            return;
        }
        UUID id = player.getUniqueId();
        TALKING.add(id);
        for (int i = 0; i < LINES.size(); i++) {
            String line = LINES.get(i);
            boolean last = i == LINES.size() - 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (last) TALKING.remove(id);
                if (!player.isOnline()) return;
                player.sendMessage(Text.line(line));
                Voice.VILLAGER.speak(player);
                if (last) QuestProgress.startedBy(player, Quests.SAVING_UP, Quests.TALK_TO_THE_BANKER);
            }, LINE_TICKS[i]);
        }
    }
}
