package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;

@CommandParameters(permission = Rank.STAFF)
public class DataCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        if (player == null) return;
        if (args.length < 2) {
            player.sendMessage("§cUsage: /data <key> <value>");
            return;
        }

        String key = args[0];
        String value = args[args.length - 1];

        ItemStack item = player.getInventory().getItemInMainHand();
        ItemNBT nmsItem = ItemNBT.of(item);
        NBTTagCompound tag = nmsItem.getTag();
        if (tag == null) {
            tag = new NBTTagCompound();
        }

        String[] keys = key.split("\\.");
        NBTTagCompound target = tag;
        String name = key;
        if (keys.length > 1) {
            target = tag.getCompound(keys[0]);
            if (target == null) target = new NBTTagCompound();
            name = keys[1];
        }
        if (!set(target, name, value)) {
            player.sendMessage("§c" + key + " is a list or compound; /data only sets plain values.");
            return;
        }
        if (keys.length > 1) tag.set(keys[0], target);

        nmsItem.setTag(tag);
        item = nmsItem.toItemStack();

        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        player.getInventory().setItemInMainHand(ItemBuilder.build(sbItem, tag));

        player.sendMessage("§aSet " + key + " to " + value);
    }

    /** Sets a value as the type already there (a string if it's new); false for lists and compounds. */
    private static boolean set(NBTTagCompound tag, String key, String value) {
        int type = tag.get(key) == null ? 8 : tag.get(key).getTypeId();
        switch (type) {
            case 1 -> tag.setByte(key, Byte.parseByte(value));
            case 2 -> tag.setShort(key, Short.parseShort(value));
            case 3 -> tag.setInt(key, Integer.parseInt(value));
            case 4 -> tag.setLong(key, Long.parseLong(value));
            case 5 -> tag.setFloat(key, Float.parseFloat(value));
            case 6 -> tag.setDouble(key, Double.parseDouble(value));
            case 8 -> tag.setString(key, value);
            default -> {
                return false;
            }
        }
        return true;
    }
}
