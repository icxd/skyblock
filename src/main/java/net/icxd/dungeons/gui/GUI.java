package net.icxd.dungeons.gui;

import lombok.Getter;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.event.GUIOpenEvent;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.gui.item.GUIItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Getter
public abstract class GUI {
  public static final Map<UUID, GUI> GUI_MAP = new HashMap<>();
  private final String title;
  private final int size;
  private final List<GUIItem> items = new ArrayList<>();

  public GUI(String title, int size) {
    this.title = title;
    this.size = size;
  }

  public void set(GUIItem item) {
    this.items.removeIf(i -> i.slot() == item.slot());
    this.items.add(item);
  }

  public void set(int slot, ItemStack stack, boolean pickup) {
    if (stack == null)
      this.items.removeIf(i -> i.slot() == slot);
    else {
      this.set(new GUIItem() {
        @Override public int slot() { return slot; }
        @Override public ItemStack stack() { return stack; }
        @Override public boolean pickup() { return pickup; }
      });
    }
  }

  public void set(int slot, ItemStack stack) { this.set(slot, stack, false); }

  public GUIItem get(int slot) {
    Iterator<GUIItem> it = this.items.iterator();

    GUIItem item;
    do {
      if (!it.hasNext()) return null;
      item = it.next();
    } while (item.slot() != slot);

    return item;
  }

  public void fill(ItemStack stack) {
    for (int i = 0; i < size; i++)
      this.set(i, stack);
  }

  public void open(final Player player) {
    this.beforeOpen(player);
    final Inventory inventory = Bukkit.createInventory(player, this.size, this.title);
    GUIOpenEvent openEvent = new GUIOpenEvent(player, this, inventory);
    Dungeons.getInstance().getServer().getPluginManager().callEvent(openEvent);
    if (!openEvent.isCancelled()) {
      for (GUIItem item : this.items)
        inventory.setItem(item.slot(), item.stack());

      player.openInventory(inventory);
      GUI_MAP.remove(player.getUniqueId());
      GUI_MAP.put(player.getUniqueId(), this);
      this.afterOpen(openEvent);

      if (this instanceof RefreshingGUI gui) {
        (new BukkitRunnable() {
          @Override
          public void run() {
            if (GUI.GUI_MAP.get(player.getUniqueId()) != GUI.this)
              this.cancel();
            else {
              GUI.this.items.clear();
              gui.items();
              GUI.this.refresh(inventory);
            }
          }
        }).runTaskTimer(Dungeons.getInstance(), 0L, gui.refreshRate());
      }
    }
  }

  public void refresh(Inventory inventory) {
    for (GUIItem item : this.items)
      inventory.setItem(item.slot(), item.stack());
  }

  public void beforeOpen(final Player player) {}
  public void afterOpen(GUIOpenEvent event) {}

  public void onOpen(GUIOpenEvent event) {}
  public void onClose(InventoryCloseEvent event) {}
  public void update(Inventory inventory) {}

  public boolean allowHotkeying() { return false; }

  public static class Size {
    public static final int ONE = 9;
    public static final int TWO = 18;
    public static final int THREE = 27;
    public static final int FOUR = 36;
    public static final int FIVE = 45;
    public static final int SIX = 54;
  }
}
