package net.icxd.dungeons.command.commands.admin;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.yoink.HypixelApi;
import net.icxd.dungeons.yoink.HypixelProfile;
import net.icxd.dungeons.yoink.YoinkMenus;
import net.icxd.dungeons.yoink.Yoinks;

/**
 * {@code /yoinkprofile <player>}: copies parts of a real SkyBlock profile onto the one you play on. It asks Hypixel's
 * API for the player's profiles (with this server's key), opens a menu of them, then one of what to bring over (see
 * {@link YoinkMenus}).
 */
@CommandParameters(description = "Copy a real Hypixel SkyBlock profile onto yours", usage = "/<command> <player>", permission = Rank.STAFF)
public class YoinkProfileCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        if (player == null) {
            send("&cOnly a player can yoink a profile (it goes onto theirs).");
            return;
        }
        if (args.length != 1) {
            send("&cUsage: /yoinkprofile <player>");
            return;
        }
        HypixelApi api = Yoinks.api();
        if (!api.hasKey()) {
            player.sendMessage(Text.line(HypixelApi.reason(new HypixelApi.Failed(HypixelApi.NO_KEY))));
            return;
        }
        player.sendMessage(Text.line("&7Looking up " + args[0] + "'s profiles..."));
        api.player(args[0]).thenCompose(who -> api.profiles(who.id()).thenApply(answer -> new Found(who, HypixelProfile.of(answer, who.id()))))
                .whenComplete((found, error) -> Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
                    User user = User.ifLoaded(player.getUniqueId());
                    if (!player.isOnline() || user == null || user.isReleased()) return;
                    if (error != null) {
                        player.sendMessage(Text.line(HypixelApi.reason(error)));
                    } else if (found.profiles().isEmpty()) {
                        player.sendMessage(Text.line("&c" + found.player().name() + " has no SkyBlock profiles!"));
                    } else {
                        YoinkMenus.profiles(player, found.player().name(), found.profiles());
                    }
                }));
    }

    private record Found(HypixelApi.Player player, List<HypixelProfile> profiles) {
    }
}
