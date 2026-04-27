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
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.user.Rank;
import net.icxd.dungeons.user.User;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bson.Document;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.annotation.Documented;

@CommandParameters(aliases = "upgrade", description = "Upgrade an item", permission = Rank.ADMIN)
public class UpgradeCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        ItemStack item = player.getInventory().getItemInHand();
        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsItem.getTag();
        SkyBlockItem skyBlockItem = ItemRegistry.get(tag.getString("id"));
        if (skyBlockItem.upgradeCosts() == null) {
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
        for (Cost cost : upgradeCost.getCosts()) {
            if (cost instanceof CoinCost coinCost) {
                if (!coinCost.check().test(player)) {
                    player.sendMessage("§cYou don't have enough coins to upgrade this item");
                    return;
                }
                User.getUser(player.getUniqueId()).getDocument().append("coins", User.getUser(player.getUniqueId()).getDocument().getInteger("coins") - coinCost.getAmount());
                User.getUser(player.getUniqueId()).save();
                tag.setInt("upgrade_count", upgradeCount + 1);
            } else if (cost instanceof EssenceCost essenceCost) {
                if (!essenceCost.check().test(player)) {
                    player.sendMessage("§cYou don't have enough essence to upgrade this item");
                    return;
                }
                Document d = User.getUser(player.getUniqueId()).getDocument()
                        .get("dungeons", Document.class).get("essence", Document.class);
                d.append(essenceCost.getEssenceType().name().toLowerCase(),
                        d.getInteger(essenceCost.getEssenceType().name().toLowerCase()) - essenceCost.getAmount());
                User.getUser(player.getUniqueId()).save();
                tag.setInt("upgrade_count", upgradeCount + 1);
            } else if (cost instanceof ItemCost itemCost) {
                if (!itemCost.check().test(player)) {
                    player.sendMessage("§cYou don't have enough items to upgrade this item");
                    return;
                }
                ItemStack itemStack = ItemBuilder.build(itemCost.getItem());
                ItemStack[] contents = player.getInventory().getContents();
                int count = 0;
                for (ItemStack content : contents) {
                    if (content == null) continue;
                    if (content.getType() != itemStack.getType()) continue;
                    count += content.getAmount();
                }
                if (count > itemCost.getAmount()) {
                    for (ItemStack content : contents) {
                        if (content == null) continue;
                        if (content.getType() != itemStack.getType()) continue;
                        if (content.getAmount() > itemCost.getAmount()) {
                            content.setAmount(content.getAmount() - itemCost.getAmount());
                            break;
                        } else {
                            itemCost.setAmount(itemCost.getAmount() - content.getAmount());
                            content.setAmount(0);
                        }
                    }
                } else {
                    for (ItemStack content : contents) {
                        if (content == null) continue;
                        if (content.getType() != itemStack.getType()) continue;
                        content.setAmount(0);
                    }
                }
                tag.setInt("upgrade_count", upgradeCount + 1);
            }
        }

        ItemStack stack = ItemBuilder.build(skyBlockItem, tag);
        player.getInventory().setItemInHand(stack);
    }
}
