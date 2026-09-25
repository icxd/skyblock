package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.user.Rank;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandParameters(permission = Rank.STAFF)
public class NBTCommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();

        ItemStack item = player.getInventory().getItemInHand();
        if (item == null) {
            player.sendMessage("§cYou must be holding an item.");
            return;
        }

        ItemNBT nmsItem = ItemNBT.of(item);
        for (String key : nmsItem.getTag().c()) {
            if (key.equals("display") || key.equals("SkullOwner")) continue;
            player.sendMessage("§f" + key + ": §a" + nmsItem.getTag().get(key));
        }
    }
}
