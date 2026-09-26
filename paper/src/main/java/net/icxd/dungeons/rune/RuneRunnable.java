package net.icxd.dungeons.rune;

import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Every tick: the runes on what a standing player holds and wears (each of them, not just the first). */
public class RuneRunnable implements Runnable {
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    int ticks = 0;

    @Override
    public void run() {
        lastLocation.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location now = player.getLocation();
            Location last = lastLocation.put(player.getUniqueId(), now);
            if (last == null || last.getX() != now.getX() || last.getY() != now.getY() || last.getZ() != now.getZ()) continue;
            PlayerInventory inventory = player.getInventory();
            for (ItemStack item : new ItemStack[]{inventory.getItemInMainHand(), inventory.getHelmet(), inventory.getChestplate(),
                    inventory.getLeggings(), inventory.getBoots()}) {
                apply(player, item);
            }
        }
        ticks++;
    }

    private void apply(Player player, ItemStack item) {
        if (item == null || item.isEmpty()) return;
        ItemNBT data = ItemNBT.of(item);
        if (!data.hasTag() || data.getTag().getString("rune").isEmpty()) return;
        Rune rune = Rune.valueOf(data.getTag().getString("rune"));
        rune.getRuneFunctionality().apply(player, data.getTag().getInt("rune_level"), ticks);
    }
}
