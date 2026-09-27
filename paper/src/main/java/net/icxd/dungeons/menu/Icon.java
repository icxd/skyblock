package net.icxd.dungeons.menu;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * What a menu slot shows, as data, so a menu can be built and checked without a server: the item's
 * material, its name and lore in {@code &} codes, and for a player head the hash of its skin on
 * textures.minecraft.net (null for none).
 */
public record Icon(Material material, String name, List<String> lore, String texture) {
    public Icon {
        lore = List.copyOf(lore);
    }

    public Icon(Material material, String name, List<String> lore) {
        this(material, name, lore, null);
    }

    public Icon(Material material, String name, String... lore) {
        this(material, name, List.of(lore), null);
    }

    /**
     * The item. A sword's attack damage and a potion's effects aren't listed under the lore: Hypixel's
     * menu items have only their lore.
     */
    public ItemStack stack() {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.line(name));
        meta.lore(Text.lines(lore));
        stack.setItemMeta(meta);
        if (texture != null) Utils.skull(stack, Utils.texture(texture));
        stack.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS, DataComponentTypes.POTION_CONTENTS).build());
        return stack;
    }
}
