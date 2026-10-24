package net.icxd.dungeons.island;

import org.bukkit.World;
import org.bukkit.entity.Player;

import net.icxd.dungeons.profile.ProfileActions;
import net.icxd.dungeons.quest.QuestProgress;
import net.icxd.dungeons.quest.QuestText;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Getting to your island: a new profile's welcome the first time (recorded as the player arrived), and
 * "Your profile was changed to" when that's how they got there (see ProfileActions).
 */
public final class IslandArrival implements IslandFeature {
    @Override
    public void arrived(Player player, World world) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null) return;
        if (user.profile().remove(QuestProgress.WELCOME) != null) {
            for (String line : QuestText.welcome(player.getName(), user.getRank().getColor().replace('§', '&'))) player.sendMessage(Text.line(line));
            user.save();
        }
        ProfileActions.arrivedOnIsland(player, user);
    }
}
