package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@CommandParameters(description = "Add an enchantment to an item.", usage = "/<command> <enchantment> <level>", permission = Rank.STAFF)
public class AddEnchantmentCommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        Player player = source.getPlayer();
        if (player == null) return;
        if (args.length < 2) {
            source.send("Usage: " + this.getClass().getAnnotation(CommandParameters.class).usage());
            return;
        }
        EnchantmentType type = EnchantmentType.getByNamespace(args[0]);
        if (type == null) {
            send("&cThere's no enchantment called " + args[0] + ".");
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            level = 0;
        }
        if (level < 1) {
            send("&cThe level has to be a number, 1 or more.");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        ItemNBT nbt = ItemNBT.of(item);
        SkyBlockItem sbItem = nbt.hasTag() ? ItemRegistry.get(nbt.getTag().getString("id")) : null;
        if (sbItem == null) {
            send("&cHold a SkyBlock item first.");
            return;
        }
        NBTTagCompound tag = nbt.getTag();

        // An item has each enchantment once: this replaces its level if it's already there.
        NBTTagList enchantments = tag.getList("enchantments", 10);
        for (int i = enchantments.size() - 1; i >= 0; i--) {
            if (type.getNamespace().equals(enchantments.get(i).getString("name"))) enchantments.remove(i);
        }
        NBTTagCompound enchantment = new NBTTagCompound();
        enchantment.setString("name", type.getNamespace());
        enchantment.setShort("lvl", (short) level);
        enchantments.add(enchantment);
        tag.set("enchantments", enchantments);
        player.getInventory().setItemInMainHand(ItemBuilder.build(sbItem, tag, item.getAmount()));

        send("&aAdded " + type.getName() + " " + level + " to " + sbItem.name() + ".");
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        if (args.length > 1) return null;
        String typed = args.length == 0 ? "" : args[0].toLowerCase();
        return EnchantmentType.all().stream().map(EnchantmentType::getNamespace).filter(n -> n.startsWith(typed)).sorted().toList();
    }
}
