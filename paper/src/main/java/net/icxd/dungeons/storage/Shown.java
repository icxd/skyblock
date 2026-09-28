package net.icxd.dungeons.storage;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.utils.Text;

/**
 * What a storage menu's slot shows, as data (see {@link Icon}), with how many: the Storage menu numbers
 * its pages and backpack slots by their stack size. A name of null is the item's own (the recorded Choose
 * an Icon's items have no name of their own), and {@link #hidden} ones show no tooltip at all (the glass).
 */
record Shown(Icon icon, int amount, boolean hidden) {
    static Shown of(Icon icon) {
        return new Shown(icon, 1, false);
    }

    static Shown of(Icon icon, int amount) {
        return new Shown(icon, amount, false);
    }

    /** No tooltip: the black glass, or an Ender Chest page's icon in its place. */
    static Shown blank(Material material) {
        return new Shown(new Icon(material, "", List.of()), 1, true);
    }

    ItemStack stack() {
        ItemStack stack;
        if (icon.name() != null) {
            stack = icon.stack();
        } else {
            stack = new ItemStack(icon.material());
            ItemMeta meta = stack.getItemMeta();
            meta.lore(Text.lines(icon.lore()));
            stack.setItemMeta(meta);
            stack.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                    .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS).build());
        }
        if (hidden) stack.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        stack.setAmount(Math.max(1, Math.min(amount, stack.getMaxStackSize())));
        return stack;
    }
}
