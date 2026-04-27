package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.user.Rank;
import org.bukkit.entity.Player;

@CommandParameters(permission = Rank.ADMIN)
public class ItemCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        String itemName = args[0];

        if (itemName.equalsIgnoreCase("list")) {
            player.sendMessage("§aItems:");
            for (String key : ItemRegistry.getRegistry().keySet()) {
                player.sendMessage("§a" + key);
            }
            return;
        }

        SkyBlockItem sbItem = ItemRegistry.get(itemName);
        if (sbItem == null) {
            send("&cItem not found.");
            return;
        }

        player.getInventory().addItem(ItemBuilder.build(sbItem));
    }
}
