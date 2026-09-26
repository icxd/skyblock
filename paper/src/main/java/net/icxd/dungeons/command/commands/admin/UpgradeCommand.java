package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandParameters(aliases = "upgrade", description = "Upgrade an item", permission = Rank.STAFF)
public class UpgradeCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        if (player == null || source.getUser() == null) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemNBT nmsItem = ItemNBT.of(item);
        NBTTagCompound tag = nmsItem.getTag();
        SkyBlockItem skyBlockItem = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (skyBlockItem == null || skyBlockItem.upgradeCosts() == null) {
            player.sendMessage("§cThis item cannot be upgraded");
            return;
        }
        if (tag.getInt("upgrade_count") >= skyBlockItem.upgradeCosts().getCosts().size()) {
            // TODO: prestige the item
            player.sendMessage("§cThis item is already fully upgraded");
            return;
        }
        int upgradeCount = tag.getInt("upgrade_count");
        UpgradeCost upgradeCost = skyBlockItem.upgradeCosts().getCosts().get(upgradeCount);
        User user = source.getUser();
        // All of it or nothing: check every cost before taking any.
        for (Cost cost : upgradeCost.getCosts()) {
            if (cost.canPay(player, user)) continue;
            String what = cost instanceof CoinCost ? "coins" : cost instanceof EssenceCost ? "essence" : "items";
            player.sendMessage("§cYou don't have enough " + what + " to upgrade this item");
            return;
        }
        for (Cost cost : upgradeCost.getCosts()) cost.pay(player, user);
        user.save();
        tag.setInt("upgrade_count", upgradeCount + 1);

        ItemStack stack = ItemBuilder.build(skyBlockItem, tag);
        player.getInventory().setItemInMainHand(stack);
    }
}
