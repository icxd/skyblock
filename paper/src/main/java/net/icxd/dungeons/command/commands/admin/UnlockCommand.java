package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandParameters(permission = Rank.STAFF, sandbox = true)
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
        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        int slots = sbItem == null ? 0 : GemSlots.of(sbItem, tag).size();
        if (slot >= slots) {
            send("&cThis item has " + slots + " gemstone slots.");
            return;
        }
        // As the Gemstone Grinder unlocks one, without its cost.
        GemSlots.unlock(tag, sbItem, slot);
        player.getInventory().setItemInMainHand(ItemBuilder.build(sbItem, tag));

        send("&aGemstone " + slot + " unlocked.");
    }
}
