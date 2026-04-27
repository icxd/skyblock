package net.icxd.dungeons.rune;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class RuneRunnable implements Runnable {
    private final HashMap<Player, Location> lastLocation = new HashMap<>();
    int ticks = 0;
    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (lastLocation.containsKey(player) && (lastLocation.get(player).getX() != player.getLocation().getX() || lastLocation.get(player).getY() != player.getLocation().getY() || lastLocation.get(player).getZ() != player.getLocation().getZ())) {
                lastLocation.put(player, player.getLocation());
                return;
            }
            lastLocation.put(player, player.getLocation());
            if (player.getInventory().getItemInHand() != null && player.getInventory().getItemInHand().getType() != Material.AIR) {
                ItemStack item = player.getInventory().getItemInHand();
                net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                if (nmsItem.hasTag()) {
                    if (nmsItem.getTag().hasKey("rune") && !nmsItem.getTag().getString("rune").equals("")) {
                        Rune rune = Rune.valueOf(nmsItem.getTag().getString("rune"));
                        rune.getRuneFunctionality().apply(player, nmsItem.getTag().getInt("rune_level"), ticks);
                    }
                }
            } else if (player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getType() != Material.AIR) {
                ItemStack item = player.getInventory().getHelmet();
                net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                if (nmsItem.hasTag()) {
                    if (nmsItem.getTag().hasKey("rune") && !nmsItem.getTag().getString("rune").equals("")) {
                        Rune rune = Rune.valueOf(nmsItem.getTag().getString("rune"));
                        rune.getRuneFunctionality().apply(player, nmsItem.getTag().getInt("rune_level"), ticks);
                    }
                }
            } else if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() != Material.AIR) {
                ItemStack item = player.getInventory().getChestplate();
                net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                if (nmsItem.hasTag()) {
                    if (nmsItem.getTag().hasKey("rune") && !nmsItem.getTag().getString("rune").equals("")) {
                        Rune rune = Rune.valueOf(nmsItem.getTag().getString("rune"));
                        rune.getRuneFunctionality().apply(player, nmsItem.getTag().getInt("rune_level"), ticks);
                    }
                }
            } else if (player.getInventory().getLeggings() != null && player.getInventory().getLeggings().getType() != Material.AIR) {
                ItemStack item = player.getInventory().getLeggings();
                net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                if (nmsItem.hasTag()) {
                    if (nmsItem.getTag().hasKey("rune") && !nmsItem.getTag().getString("rune").equals("")) {
                        Rune rune = Rune.valueOf(nmsItem.getTag().getString("rune"));
                        rune.getRuneFunctionality().apply(player, nmsItem.getTag().getInt("rune_level"), ticks);
                    }
                }
            } else if (player.getInventory().getBoots() != null && player.getInventory().getBoots().getType() != Material.AIR) {
                ItemStack item = player.getInventory().getBoots();
                net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                if (nmsItem.hasTag()) {
                    if (nmsItem.getTag().hasKey("rune") && !nmsItem.getTag().getString("rune").equals("")) {
                        Rune rune = Rune.valueOf(nmsItem.getTag().getString("rune"));
                        rune.getRuneFunctionality().apply(player, nmsItem.getTag().getInt("rune_level"), ticks);
                    }
                }
            }
        });
        ticks++;
    }
}
