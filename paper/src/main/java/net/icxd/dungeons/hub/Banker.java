package net.icxd.dungeons.hub;

import java.util.List;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.bank.BankMenu;
import net.icxd.dungeons.npc.Lines;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.quest.QuestProgress;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.user.User;

/**
 * The Banker, in the Hub's Bank (from npcs/hub.json), as recorded (2026-10-24): at Saving Up's "Talk
 * to the Banker", his three lines in a villager's voice, and with the last that's done (its +5 SkyBlock
 * XP); the boss bar stays where it was. After that he opens the Personal Bank Account, speaking as it
 * opens. Saving Up starts as the Village is found (see Areas); a profile that found it before then
 * starts it with his last line. Main thread.
 */
final class Banker {
    static final String ID = "BANKER";
    static final List<String> LINES = List.of(
            "&e[NPC] Banker&f: &r&fHello there!",
            "&e[NPC] Banker&f: &r&fYou may want to store your &6Coins &fin a safe place while you are off adventuring.",
            "&e[NPC] Banker&f: &r&fStoring them in your &6Bank &fkeeps them safe and allows you to earn interest at the start of every season!");
    /** Ticks before each line (recorded at 05:14.3, 05:15.7 and 05:17.8). */
    static final long[] LINE_TICKS = {0, 28, 70};

    private Banker() {
    }

    static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || Lines.talking(player)) return;
        // Until "Talk to the Banker" is done, he talks (again, if they left before his last line).
        if (!talks(user.profile())) {
            BankMenu.account(player, null);
            Voice.VILLAGER.speak(player);
            return;
        }
        Lines.say(plugin, player, LINES, LINE_TICKS, Voice.VILLAGER,
                () -> QuestProgress.startedBy(player, Quests.SAVING_UP, Quests.TALK_TO_THE_BANKER));
    }

    /** Whether a click has him talk, rather than open the bank: Saving Up not started, or at its first objective. */
    static boolean talks(Document profile) {
        return !Quests.started(profile, Quests.SAVING_UP) || Quests.current(profile, Quests.SAVING_UP) == Quests.TALK_TO_THE_BANKER;
    }
}
