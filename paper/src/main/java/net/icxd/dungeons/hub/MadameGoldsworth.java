package net.icxd.dungeons.hub;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.museum.Museum;
import net.icxd.dungeons.museum.MuseumMenu;
import net.icxd.dungeons.npc.Lines;
import net.icxd.dungeons.npc.Npc;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Madame Eleanor Q. Goldsworth III, in the Hub's Museum (from npcs/hub.json), as recorded (2026-10-25, 00:41.4): the
 * first time a profile talks to her she says her seven lines in a villager's voice at pitch 2, and Your Museum opens
 * with the last (see {@link MuseumMenu}); after that a click opens it at once, with a click (02:21.8, 02:27.9). Without
 * the Museum's data she says the wiki's line for a Museum that's disabled. Main thread.
 */
final class MadameGoldsworth {
    static final String ID = "MADAME_ELEANOR_Q_GOLDSWORTH_III";
    /** As recorded, {@code %s} their name. */
    static final List<String> FIRST_TALK = List.of(
            "Welcome, &b%s&f!",
            "This world has an &oincredible &famount of interesting gear and rare items, which I would like to showcase.",
            "Would you like to help me fill the &9Museum&f? I can even reward you for your contributions!",
            "I'll show you the list of items that I'm currently looking for.",
            "You can donate any item from the list and you'll still be able to use those items whenever you want!",
            "However, once you donate an item, it will be come &cCo-op Soulbound &fto you, meaning that you won't be able to trade off "
                    + "your profile, auction, or sell the item anymore.",
            "Choose carefully!");
    /** 00:41.4, 00:42.9, 00:45.9, 00:48.9, 00:51.4, 00:54.4, 00:58.4. */
    static final long[] LINE_TICKS = {0, 30, 90, 150, 200, 260, 340};
    static final Voice VOICE = Voice.villager(2);
    /** The wiki's (Madame Eleanor Q. Goldsworth III, "Museum Disabled"). */
    static final String DISABLED = "&cThis Museum is temporarily disabled. Check back again later!";

    private MadameGoldsworth() {
    }

    static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || Lines.talking(player)) return;
        if (Museum.data().count() == 0) {
            player.sendMessage(Text.line(DISABLED));
            return;
        }
        if (Npc.firstTalk(user.profile(), ID)) {
            Lines.say(plugin, player, firstTalk(player.getName()), LINE_TICKS, VOICE, () -> MuseumMenu.show(player, MenuSound.CLICK));
            return;
        }
        MuseumMenu.show(player, MenuSound.CLICK);
    }

    /** "&e[NPC] &6Madame Eleanor Q. Goldsworth III&f: &r&fWelcome, &bICoding&f!", as recorded. */
    static List<String> firstTalk(String name) {
        return FIRST_TALK.stream().map(line -> "&e[NPC] &6Madame Eleanor Q. Goldsworth III&f: &r&f" + (line.contains("%s") ? line.formatted(name) : line))
                .toList();
    }
}
