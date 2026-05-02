package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.user.Rank;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandParameters(description = "Recombobulates an item.", usage = "/<command>", permission = Rank.STAFF)
public class RecombobulateCommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();

        ItemStack item = player.getInventory().getItemInHand();
        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsItem.getTag();
        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        tag.setBoolean("recombobulated", !tag.getBoolean("recombobulated"));
        if (tag.getBoolean("recombobulated")) tag.setString("rarity", Rarity.valueOf(tag.getString("rarity")).upgrade().name());
        else tag.setString("rarity", Rarity.valueOf(tag.getString("rarity")).downgrade().name());
        nmsItem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsItem);
        player.getInventory().setItemInHand(item);
        player.getInventory().setItemInHand(ItemBuilder.build(sbItem, tag));

        source.send("§aRecombobulated " + sbItem.name() + " to " + tag.getBoolean("recombobulated") + ".");
    }
}
