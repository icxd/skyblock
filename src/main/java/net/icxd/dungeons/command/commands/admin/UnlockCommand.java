package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.user.Rank;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandParameters(permission = Rank.ADMIN)
public class UnlockCommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();

        int slot = Integer.parseInt(args[0]);

        ItemStack item = player.getInventory().getItemInHand();
        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsItem.getTag();
        if (tag == null) {
            tag = new NBTTagCompound();
        }

        NBTTagList list = tag.getList("gemstone_slots", 10);
        NBTTagCompound gslot = list.get(slot);
        gslot.setBoolean("locked", false);
        gslot.remove("costs");
        send(gslot.toString());
        list.a(slot, gslot);
        send(list.toString());
        tag.set("gemstone_slots", list);
        send(tag.get("gemstone_slots").toString());

        nmsItem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsItem);

        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        player.getInventory().setItemInHand(ItemBuilder.build(sbItem, tag));

        send("&aGemstone " + slot + " unlocked.");
    }
}
