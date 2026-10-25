package net.icxd.dungeons.pet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.user.User;

/**
 * A pet item right-clicked goes into the Pets menu ("&eRight-click to add this pet to your pet menu!"); it's never
 * placed as a head. A player's pet leaves with them. Main thread.
 */
public final class PetListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        OwnedPet pet = PetItems.pet(ItemNBT.read(event.getItem()));
        if (pet == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player) || Pets.data().pet(pet.type()) == null) return;
        if (PetActions.add(player, user, pet)) player.getInventory().setItemInMainHand(null);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent event) {
        if (PetItems.pet(ItemNBT.read(event.getItemInHand())) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PetFollowers.despawn(event.getPlayer().getUniqueId());
    }
}
