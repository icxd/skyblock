package net.icxd.dungeons.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import lombok.Getter;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.event.GUIOpenEvent;
import net.icxd.dungeons.gui.item.GUIItem;
import net.icxd.dungeons.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        final Inventory inventory = Bukkit.createInventory(player, this.size, net.icxd.dungeons.utils.Text.line(this.title));
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

    /**
     * A click in the player's own inventory while this menu is open (a shop sells what's clicked
     * there). Return true if it's been dealt with, cancelling it if it should do nothing more; false
     * lets it go on as it would, except that a shift-click still can't move an item into the menu.
     */
    public boolean onPlayerInventoryClick(InventoryClickEvent event) { return false; }

    /** Glass with no tooltip, where Hypixel's menus have it. */
    protected static ItemStack filler() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        pane.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return pane;
    }

    /** A menu's item: its name and lore in {@code &} codes. */
    protected static ItemStack item(Material material, String name, String... lore) {
        return item(material, name, List.of(lore));
    }

    protected static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.line(name));
        meta.lore(Text.lines(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static class Size {
        public static final int ONE = 9;
        public static final int TWO = 18;
        public static final int THREE = 27;
        public static final int FOUR = 36;
        public static final int FIVE = 45;
        public static final int SIX = 54;
    }
}
