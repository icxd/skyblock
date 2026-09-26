package net.icxd.dungeons.gui.item;

import lombok.Getter;
import net.icxd.dungeons.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public interface GUIClickableItem extends GUIItem {
  void run(InventoryClickEvent event);

  static GUIClickableItem close(int slot) {
    return new GUIClickableItem() {
      @Override public void run(InventoryClickEvent event) { event.getWhoClicked().closeInventory(); }
      @Override public int slot() { return slot; }
      @Override public ItemStack stack() {
        return new ItemBuilder(Material.BARRIER)
            .setDisplayName("&cClose")
            .build();
      }
    };
  }
}
