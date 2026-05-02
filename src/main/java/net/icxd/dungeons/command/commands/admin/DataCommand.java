package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.user.Rank;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;

@CommandParameters(permission = Rank.STAFF)
public class DataCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();

        String key = args[0];
        String value = args[args.length - 1];

        ItemStack item = player.getInventory().getItemInHand();
        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsItem.getTag();
        if (tag == null) {
            tag = new NBTTagCompound();
        }

        String[] keys = key.split("\\.");
        if (keys.length == 1) {
            switch (tag.get(key).getTypeId()) {
                case 1 -> tag.setByte(key, Byte.parseByte(value));
                case 2 -> tag.setShort(key, Short.parseShort(value));
                case 3 -> tag.setInt(key, Integer.parseInt(value));
                case 4 -> tag.setLong(key, Long.parseLong(value));
                case 5 -> tag.setFloat(key, Float.parseFloat(value));
                case 6 -> tag.setDouble(key, Double.parseDouble(value));
                case 7 -> tag.setByteArray(key, value.getBytes());
                case 8 -> tag.setString(key, value);
                case 9, 11, 12 -> tag.setIntArray(key, new int[]{Integer.parseInt(value)});
                case 10 -> tag.set(key, new NBTTagCompound());
            }
        } else {
            NBTTagCompound subTag = tag.getCompound(keys[0]);
            if (subTag == null) {
                subTag = new NBTTagCompound();
            }
            switch (subTag.get(keys[1]).getTypeId()) {
                case 1 -> subTag.setByte(keys[1], Byte.parseByte(value));
                case 2 -> subTag.setShort(keys[1], Short.parseShort(value));
                case 3 -> subTag.setInt(keys[1], Integer.parseInt(value));
                case 4 -> subTag.setLong(keys[1], Long.parseLong(value));
                case 5 -> subTag.setFloat(keys[1], Float.parseFloat(value));
                case 6 -> subTag.setDouble(keys[1], Double.parseDouble(value));
                case 7 -> subTag.setByteArray(keys[1], value.getBytes());
                case 8 -> subTag.setString(keys[1], value);
                case 9, 11, 12 -> subTag.setIntArray(keys[1], new int[]{Integer.parseInt(value)});
                case 10 -> subTag.set(keys[1], new NBTTagCompound());
            }
            tag.set(keys[0], subTag);
        }

        nmsItem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsItem);

        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        player.getInventory().setItemInHand(ItemBuilder.build(sbItem, tag));

        player.sendMessage("§aSet " + key + " to " + value);
    }
}
