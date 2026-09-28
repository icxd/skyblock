package net.icxd.dungeons.command.commands.user;

import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;

import net.icxd.dungeons.collection.CollectionMenu;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;

/**
 * {@code /viewcollection <item id>}: a collection's tiers (the wiki's Commands: "an all-uppercase item ID of a
 * collection item"). What Hypixel says for one never found, or no such collection, is UNKNOWN.
 */
@CommandParameters(description = "Opens a collection", usage = "/<command> <item id>")
public class ViewCollectionCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        String id = args.length == 0 ? "" : args[0].toUpperCase(Locale.ROOT);
        if (Collections.data().collections().get(id) == null) {
            send("&cThere's no collection called " + (args.length == 0 ? "that" : args[0]) + "!");
            return;
        }
        new CollectionMenu(source.getPlayer(), id).open(source.getPlayer());
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        String typed = args.length == 0 ? "" : args[args.length - 1].toUpperCase(Locale.ROOT);
        return Collections.data().collections().keySet().stream().filter(id -> id.startsWith(typed)).toList();
    }
}
