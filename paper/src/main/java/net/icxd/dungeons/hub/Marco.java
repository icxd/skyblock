package net.icxd.dungeons.hub;

import java.util.List;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.npc.Lines;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.quest.QuestProgress;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.user.User;

/**
 * Marco, in the Hub's Flower House (from npcs/hub.json), as recorded (2026-10-25): at "Talk to Marco"
 * he asks for 5 Rose Red, and with his last line the objective is done; given the dye, he thanks them,
 * and with his last line Marco the Artist is complete and its Spray Can is theirs to claim. Each line
 * in a villager's voice, timed as recorded. Main thread.
 *
 * <p>Not recorded, so this plugin's: clicked at "Give Rose Red Dye" without 5 Rose Red, he asks again;
 * once it's done, he says his last thanks. He takes the dye with his last line (the recording's went
 * with his first), so nothing is lost if they leave while he talks.
 */
final class Marco {
    static final String ID = "MARCO";
    static final List<String> ASKS = List.of(
            "&e[NPC] Marco&f: &r&fHey there!",
            "&e[NPC] Marco&f: &r&fI'm a world-famous artist to be. Most people just call me \"the new da Vinci\".",
            "&e[NPC] Marco&f: &r&fUnfortunately I ran out of dye this morning...",
            "&e[NPC] Marco&f: &r&fIn my garden are some flowers. Could you pick &e5 Poppies&f and turn them into &eRose Red Dye&f, then bring them back to me?");
    /** Ticks before each line (recorded at 00:37.3, 00:38.8, 00:40.8 and 00:43.8). */
    static final long[] ASK_TICKS = {0, 30, 70, 130};
    static final List<String> THANKS = List.of(
            "&e[NPC] Marco&f: &r&fWow! That's the most beautiful red dye I have ever seen!",
            "&e[NPC] Marco&f: &r&fTake this &fSpray Can&f!",
            "&e[NPC] Marco&f: &r&fYou can select a spray by left-clicking your Spray Can!",
            "&e[NPC] Marco&f: &r&fRight-click any wall on your private island to spray it. If you ever lose your Spray Can, you can craft another one using the recipe in your &aRecipe Book&f!",
            "&e[NPC] Marco&f: &r&fThank you so much for your help. You should visit me in my &bCanvas Room&f sometime! Step into the portal to the left over there!");
    /** Recorded at 01:08.7, 01:10.7, 01:12.7, 01:14.7 and 01:17.7. */
    static final long[] THANKS_TICKS = {0, 40, 80, 120, 180};
    private static final long[] ONE_LINE = {0};

    private Marco() {
    }

    static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Document profile = user.profile();
        int at = Quests.current(profile, Quests.MARCO_THE_ARTIST);
        if (at == Quests.TALK_TO_MARCO) {
            Lines.say(plugin, player, ASKS, ASK_TICKS, Voice.VILLAGER,
                    () -> QuestProgress.complete(player, Quests.MARCO_THE_ARTIST, Quests.TALK_TO_MARCO));
        } else if (at == Quests.GIVE_ROSE_RED) {
            ItemCost dye = roseRed();
            if (!dye.canPay(player, user)) {
                Lines.say(plugin, player, List.of(ASKS.getLast()), ONE_LINE, Voice.VILLAGER, null);
                return;
            }
            Lines.say(plugin, player, THANKS, THANKS_TICKS, Voice.VILLAGER, () -> {
                User now = User.ifLoaded(player.getUniqueId());
                if (now == null || !dye.canPay(player, now)) return;
                dye.pay(player, now);
                QuestProgress.complete(player, Quests.MARCO_THE_ARTIST, Quests.GIVE_ROSE_RED);
            });
        } else if (Quests.completed(profile, Quests.MARCO_THE_ARTIST)) {
            Lines.say(plugin, player, List.of(THANKS.getLast()), ONE_LINE, Voice.VILLAGER, null);
        }
    }

    /** The 5 Rose Red he wants. */
    static ItemCost roseRed() {
        return new ItemCost("INK_SACK:1", Quests.ROSE_RED);
    }
}
