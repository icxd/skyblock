package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandParameters(permission = Rank.STAFF)
public class NBTCommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        if (player == null) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemNBT nmsItem = ItemNBT.of(item);
        if (item.isEmpty() || !nmsItem.hasTag()) {
            player.sendMessage("§cHold a SkyBlock item first.");
            return;
        }
        for (String key : nmsItem.getTag().c()) {
            player.sendMessage("§f" + key + ": §a" + nmsItem.getTag().get(key));
        }
    }
}
