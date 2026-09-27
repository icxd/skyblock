package net.icxd.dungeons.profile;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * One of the profile menus, as Hypixel lays them out. Its items are made from the viewer's data each
 * time it opens, and buttons act on the next tick, not inside the click.
 */
abstract class Menu extends GUI {
    protected final Player viewer;

    Menu(String title, int size, Player viewer) {
        super(title, size);
        this.viewer = viewer;
    }

    /** The items, from their data as it is now. */
    abstract void items(User user);

    public void items() {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) fill(filler());
        else items(user);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        items();
    }

    /** Does {@code action} on a left or right click (shift or not). */
    GUIClickableItem button(int slot, ItemStack stack, Runnable action) {
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                ClickType click = event.getClick();
                if (click != ClickType.LEFT && click != ClickType.RIGHT && click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) return;
                Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
                    if (viewer.isOnline()) action.run();
                });
            }

            @Override
            public int slot() {
                return slot;
            }

            @Override
            public ItemStack stack() {
                return stack;
            }
        };
    }

    GUIClickableItem goBack(int slot, String to, java.util.function.Supplier<Menu> menu) {
        return button(slot, item(Material.ARROW, "&aGo Back", "&7To " + to), () -> menu.get().open(viewer));
    }

    static ItemStack item(Material material, String name, String... lore) {
        return item(material, name, List.of(lore));
    }

    static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.line(name));
        meta.lore(Text.lines(lore));
        item.setItemMeta(meta);
        return item;
    }

    /** Glass with no tooltip, where Hypixel's menus have it. */
    static ItemStack filler() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        pane.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return pane;
    }

    /**
     * What a profile's items say about it: its best skills, the armor it wears, its coins and age.
     * The viewer's own profile shows the armor they have on now.
     */
    List<String> summary(User user, Profiles.Entry entry) {
        Document profile = entry.profile();
        List<String> lore = new ArrayList<>(Profiles.skillLines(profile));
        lore.add("");
        ItemStack[] armor = entry.id().equals(user.profileId()) ? viewer.getInventory().getArmorContents()
                : StoredInventory.stored(profile, "armor", 4);
        boolean any = false;
        // Boots first, as Bukkit orders armor; Hypixel lists the helmet first.
        for (int i = armor.length - 1; i >= 0; i--) {
            if (armor[i] == null || armor[i].isEmpty()) continue;
            lore.add(name(armor[i]));
            any = true;
        }
        if (any) lore.add("");
        lore.addAll(Profiles.coinLines(profile));
        lore.add("&7Age: " + Profiles.age(Profiles.millis(profile.get(Profiles.CREATED), System.currentTimeMillis()), System.currentTimeMillis()));
        return lore;
    }

    /** A SkyBlock item's name as ItemBuilder gives it (rarity colour, reforge, stars); another item's own. */
    private static String name(ItemStack stack) {
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item != null) return ItemBuilder.name(item, tag);
        return "&f" + LegacyComponentSerializer.legacyAmpersand().serialize(stack.effectiveName());
    }
}
