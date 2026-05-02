package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.Enchantment;
import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.user.Rank;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@CommandParameters(description = "Add an enchantment to an item.", usage = "/<command> <enchantment> <level>", permission = Rank.STAFF)
public class AddEnchantmentCommand extends SCommand {

    @Override
    public void run(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.send("Usage: " + this.getClass().getAnnotation(CommandParameters.class).usage());
            return;
        }
        Player player = source.getPlayer();

        String enchantment = args[0].toUpperCase();
        int level = Integer.parseInt(args[1]);
        Enchantment enchantment1 = new Enchantment(EnchantmentType.getByNamespace(enchantment), level);

        ItemStack item = player.getInventory().getItemInHand();
        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsItem.getTag();
        if (tag == null) {
            tag = new NBTTagCompound();
        }

        nmsItem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsItem);

        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        NBTTagList enchantments = tag.getList("enchantments", 10);
        List<Enchantment> enchantments1 = new ArrayList<>();
        if (sbItem.enchantments() != null) {
            enchantments1.addAll(sbItem.enchantments());
        }

        enchantments1.add(enchantment1);
        for (Enchantment enchantment2 : enchantments1) {
            NBTTagCompound enchantmentTag = new NBTTagCompound();
            enchantmentTag.setString("name", enchantment2.getType().getNamespace());
            enchantmentTag.setShort("lvl", (short) enchantment2.getLevel());
            enchantments.add(enchantmentTag);
        }

        tag.set("enchantments", enchantments);
        nmsItem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsItem);
        player.getInventory().setItemInHand(item);
        player.getInventory().setItemInHand(ItemBuilder.build(sbItem, tag));

        send("&aAdded enchantment " + enchantment1.getType().getNamespace() + " to item " + sbItem.name() + ".");
    }
}
