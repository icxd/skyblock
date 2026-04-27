package net.icxd.dungeons.gui;

import net.icxd.dungeons.event.GUIOpenEvent;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.gui.item.GUIItem;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {
  private static final Map<UUID, Long> GUI_COOLDOWN = new HashMap<>();

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    Player player = (Player) event.getWhoClicked();
    GUI gui = GUI.GUI_MAP.get(player.getUniqueId());
    if (gui == null) return;

    if ((event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP) &&
        (event.getHotbarButton() == 8 || GUI.GUI_MAP.containsKey(player.getUniqueId()) && !(gui.allowHotkeying())))
      event.setCancelled(true);

    if (event.getClick() == ClickType.DOUBLE_CLICK)
      event.setCancelled(true);

    if (GUI_COOLDOWN.containsKey(player.getUniqueId()) && System.currentTimeMillis() - GUI_COOLDOWN.get(player.getUniqueId()) < 100L) {
      event.setCancelled(true);
      player.sendMessage(Utils.color("&cYou must wait a bit before doing this!"));
      return;
    }

    GUI_COOLDOWN.remove(player.getUniqueId());
    GUI_COOLDOWN.put(player.getUniqueId(), System.currentTimeMillis());
    if (event.getClickedInventory() == event.getView().getTopInventory()) {
      int slot = event.getSlot();
      GUIItem item = gui.get(slot);
      if (item != null) {
        if (!item.pickup())
          event.setCancelled(true);

        if (item instanceof GUIClickableItem clickable)
          clickable.run(event);
      }
    }

    gui.update(event.getView().getTopInventory());
  }

  @EventHandler
  public void onGUIOpen(GUIOpenEvent event) {
    event.getOpened().onOpen(event);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    Player player = (Player)event.getPlayer();
    GUI gui = GUI.GUI_MAP.get(player.getUniqueId());
    if (gui == null) return;
    gui.onClose(event);
    GUI.GUI_MAP.remove(player.getUniqueId());
    Utils.delay(player::updateInventory, 1L);
  }
}
