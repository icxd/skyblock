package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandParameters(permission = Rank.STAFF)
public class UnlockCommand extends SCommand {

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
        if (args.length == 0 || !args[0].matches("\\d+")) {
            send("&cUsage: /unlock <gemstone slot>");
            return;
        }
        int slot = Integer.parseInt(args[0]);
        NBTTagCompound tag = nmsItem.getTag();
        NBTTagList list = tag.getList("gemstone_slots", 10);
        if (slot >= list.size()) {
            send("&cThis item has " + list.size() + " gemstone slots.");
            return;
        }
        NBTTagCompound gslot = list.get(slot);
        gslot.setBoolean("locked", false);
        gslot.remove("costs");
        list.set(slot, gslot);
        tag.set("gemstone_slots", list);

        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        player.getInventory().setItemInMainHand(ItemBuilder.build(sbItem, tag));

        send("&aGemstone " + slot + " unlocked.");
    }
}
