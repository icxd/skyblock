package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.gui.guis.ItemBrowser;
import net.icxd.dungeons.gui.guis.ItemBrowserGUI;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.common.Rank;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Stream;

/**
 * "/item" (or "/item list") opens the item browser; "/item HYPERION" gives one. A Sandbox tool: anyone
 * on a Sandbox profile may use it, staff anywhere.
 */
@CommandParameters(permission = Rank.STAFF, sandbox = true)
public class ItemCommand extends SCommand {
    /** At most this many ids are suggested: there are thousands. */
    private static final int SUGGESTIONS = 200;

    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        if (player == null) return;
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            ItemBrowserGUI.show(player);
            return;
        }

        SkyBlockItem sbItem = ItemRegistry.get(args[0]);
        if (sbItem == null) {
            send("&cItem not found.");
            return;
        }

        // Not into an inventory that's frozen for a hand-off: it wouldn't be saved.
        if (InventorySyncListener.frozen(player)) {
            send("&cYou can't take items right now.");
            return;
        }
        player.getInventory().addItem(ItemBuilder.build(sbItem));
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        if (args.length > 1) return null;
        String typed = args.length == 0 ? "" : args[0];
        return ItemBrowser.complete(Stream.concat(Stream.of("list"), ItemRegistry.getRegistry().keySet().stream()), typed, SUGGESTIONS);
    }
}
