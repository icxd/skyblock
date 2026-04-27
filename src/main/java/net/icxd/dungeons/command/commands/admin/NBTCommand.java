package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.user.Rank;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandParameters(permission = Rank.ADMIN)
public class NBTCommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();

        ItemStack item = player.getInventory().getItemInHand();
        if (item == null) {
            player.sendMessage("§cYou must be holding an item.");
            return;
        }

        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        for (String key : nmsItem.getTag().c()) {
            if (key.equals("display") || key.equals("SkullOwner")) continue;
            player.sendMessage("§f" + key + ": §a" + nmsItem.getTag().get(key));
        }
    }
}
