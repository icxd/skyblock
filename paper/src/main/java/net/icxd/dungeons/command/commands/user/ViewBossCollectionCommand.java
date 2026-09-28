package net.icxd.dungeons.command.commands.user;

import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;

import net.icxd.dungeons.collection.CollectionData;
import net.icxd.dungeons.collection.CollectionMenu;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;

/** {@code /viewbosscollection <boss id>}: a boss collection's tiers ("catacombs_1" for Bonzo ... "kuudra", the wiki's Commands). */
@CommandParameters(description = "Opens a boss collection", usage = "/<command> <boss id>")
public class ViewBossCollectionCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        String id = args.length == 0 ? "" : args[0].toUpperCase(Locale.ROOT);
        CollectionData.Collection boss = Collections.data().collection(id);
        if (boss == null || !boss.boss()) {
            send("&cThere's no boss collection called " + (args.length == 0 ? "that" : args[0]) + "!");
            return;
        }
        new CollectionMenu(source.getPlayer(), id).open(source.getPlayer());
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return Collections.data().bosses().stream().map(b -> b.id().toLowerCase(Locale.ROOT)).filter(id -> id.startsWith(typed)).toList();
    }
}
