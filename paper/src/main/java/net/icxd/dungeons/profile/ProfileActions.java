package net.icxd.dungeons.profile;

import org.bukkit.entity.Player;

import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/** What players are told about their profiles. Main thread. */
public final class ProfileActions {
    private ProfileActions() {
    }

    /** "You are playing on profile: Kiwi (Sandbox)" and its id, as Hypixel says when you join a server. */
    public static void announce(Player player, User user) {
        tell(player, user, "&aYou are playing on profile: &e");
    }

    private static void tell(Player player, User user, String text) {
        player.sendMessage(Text.line(text + user.profileName() + user.mode().suffix()));
        player.sendMessage(Text.line("&8Profile ID: " + user.profileId()));
    }
}
