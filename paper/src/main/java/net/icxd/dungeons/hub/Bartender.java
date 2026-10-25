package net.icxd.dungeons.hub;

import java.util.List;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.npc.Lines;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.quest.QuestProgress;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.user.User;

/**
 * The Bartender, in the Hub's Combat Settlement (from npcs/hub.json), as recorded (2026-10-25): at Time
 * to Strike's first "Talk to the Bartender" he asks for help with the Graveyard's zombies, and with his
 * last line gives an Undead Sword (to claim); at the second he thanks them by name and pays 100 coins.
 * Each line in a villager's voice, timed as recorded. The zombies are counted by {@link Graveyard}.
 * Main thread.
 *
 * <p>Not recorded, so this plugin's: while the zombies are being killed he asks again for help, and
 * after that he says his last advice again.
 */
final class Bartender {
    static final String ID = "BARTENDER";
    static final List<String> ASKS = List.of(
            "&e[NPC] Bartender&f: &r&fAh! You look like someone strong.",
            "&e[NPC] Bartender&f: &r&fThese are trying times, indeed. The &cGraveyard &fup ahead is overflowing with monsters! Anyone who comes in is spooked off by the grunts of Zombies in the distance.",
            "&e[NPC] Bartender&f: &r&fCould you give me a hand? If you help clear out some of these monsters, I'll pay you for it!",
            "&e[NPC] Bartender&f: &r&fHere, take this sword. It should help you take out the Zombies.");
    /** Ticks before each line (recorded at 02:04.4, 02:07.1, 02:16.1 and 02:21.7). */
    static final long[] ASK_TICKS = {0, 54, 234, 346};
    /** Recorded at 03:16.2, 03:19.6, 03:27.7 and 03:30.8. */
    static final long[] THANKS_TICKS = {0, 68, 230, 292};
    private static final long[] ONE_LINE = {0};

    private Bartender() {
    }

    /** His thanks, their name in their rank's colour ("&bICoding", as recorded). */
    static List<String> thanks(String name) {
        return List.of(
                "&e[NPC] Bartender&f: &r&fWords cannot describe how thankful I am, " + name + "&f!",
                "&e[NPC] Bartender&f: &r&fIf you're up for the challenge, both the &cGraveyard &fand the &cSpider's Den &fbeyond it are great training grounds for improving your &aCombat Skill&f.",
                "&e[NPC] Bartender&f: &r&fFor now, here's a reward for helping me out!",
                "&e[NPC] Bartender&f: &r&fI recommend you gear up in the &cCombat Settlement&f. You should talk to Rosetta&f, she has some great armor for sale!");
    }

    static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Document profile = user.profile();
        int at = Quests.current(profile, Quests.TIME_TO_STRIKE);
        List<String> thanks = thanks(user.getRank().getColor().replace('§', '&') + player.getName());
        if (at == Quests.TALK_TO_THE_BARTENDER) {
            Lines.say(plugin, player, ASKS, ASK_TICKS, Voice.VILLAGER,
                    () -> QuestProgress.complete(player, Quests.TIME_TO_STRIKE, Quests.TALK_TO_THE_BARTENDER));
        } else if (at == Quests.TALK_TO_THE_BARTENDER_AGAIN) {
            Lines.say(plugin, player, thanks, THANKS_TICKS, Voice.VILLAGER,
                    () -> QuestProgress.complete(player, Quests.TIME_TO_STRIKE, Quests.TALK_TO_THE_BARTENDER_AGAIN));
        } else if (at == Quests.KILL_ZOMBIES) {
            Lines.say(plugin, player, List.of(ASKS.get(2)), ONE_LINE, Voice.VILLAGER, null);
        } else if (at > Quests.TALK_TO_THE_BARTENDER_AGAIN || Quests.completed(profile, Quests.TIME_TO_STRIKE)) {
            Lines.say(plugin, player, List.of(thanks.getLast()), ONE_LINE, Voice.VILLAGER, null);
        }
    }
}
