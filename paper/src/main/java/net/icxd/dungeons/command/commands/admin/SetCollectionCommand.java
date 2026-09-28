package net.icxd.dungeons.command.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.icxd.dungeons.collection.CollectionData;
import net.icxd.dungeons.collection.CollectionGains;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.user.User;

/**
 * For testing: sets a collection's count (or a boss's kills) on the profile a player plays on, or with
 * {@code +N} adds to it the way collecting does (level-up messages and rewards, on a profile that collects).
 */
@CommandParameters(description = "Set a collection's count", usage = "/<command> <collection> <amount|+amount> [player]", permission = Rank.STAFF)
public class SetCollectionCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (args.length < 2) {
            send("&cUsage: /setcollection <collection> <amount|+amount> [player]");
            return;
        }
        String id = args[0].toUpperCase(Locale.ROOT);
        CollectionData.Collection collection = Collections.data().collection(id);
        if (collection == null) {
            send("&cNo collection called " + args[0] + ".");
            return;
        }
        Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : source.getPlayer();
        User user = target == null ? null : User.ifLoaded(target.getUniqueId());
        if (user == null || user.isReleased()) {
            send("&cThat player isn't here.");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(args[1].startsWith("+") ? args[1].substring(1) : args[1]);
        } catch (NumberFormatException e) {
            send("&cWhat's " + args[1] + "?");
            return;
        }
        if (args[1].startsWith("+")) {
            if (CollectionGains.give(target, id, amount) == null) {
                send("&cTheir profile doesn't collect (a Sandbox one).");
                return;
            }
        } else {
            Collections.set(user.profile(), id, amount);
            user.save();
        }
        send("&a" + target.getName() + "'s " + collection.name() + ": " + Collections.count(user.profile(), id)
                + " (tier " + Collections.tier(user.profile(), id) + ")");
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        String typed = args.length == 0 ? "" : args[args.length - 1].toUpperCase(Locale.ROOT);
        if (args.length > 1) return null;
        List<String> ids = new ArrayList<>(Collections.data().collections().keySet());
        for (CollectionData.Collection boss : Collections.data().bosses()) ids.add(boss.id());
        return ids.stream().filter(id -> id.startsWith(typed)).toList();
    }
}
