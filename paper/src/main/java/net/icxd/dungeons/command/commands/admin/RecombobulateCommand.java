package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandParameters(description = "Recombobulates an item.", usage = "/<command>", permission = Rank.STAFF)
public class RecombobulateCommand extends SCommand {

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
        NBTTagCompound tag = nmsItem.getTag();
        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        if (sbItem == null) return;
        if (tag.getString("rarity").isEmpty()) tag.setString("rarity", sbItem.rarity().name());
        tag.setBoolean("recombobulated", !tag.getBoolean("recombobulated"));
        if (tag.getBoolean("recombobulated")) tag.setString("rarity", Rarity.valueOf(tag.getString("rarity")).upgrade().name());
        else tag.setString("rarity", Rarity.valueOf(tag.getString("rarity")).downgrade().name());
        player.getInventory().setItemInMainHand(ItemBuilder.build(sbItem, tag));

        source.send("§aRecombobulated " + sbItem.name() + " to " + tag.getBoolean("recombobulated") + ".");
    }
}
