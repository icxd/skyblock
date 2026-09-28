package net.icxd.dungeons.collection;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.utils.Text;

/**
 * What a slot of the collection and recipe menus shows, as data so the menus can be checked without a
 * server: the glass between things ({@link #FILLER}), a menu item ({@link Icon}), or a SkyBlock item as
 * {@link ItemBuilder} makes it (its name, lore and look), {@code amount} of it, with {@code extra} lines under
 * its lore ("&eClick to view recipe!").
 */
public record MenuSlot(Icon icon, String item, int amount, List<String> extra) {
    /** Black glass with no tooltip, where Hypixel's menus have it. */
    public static final MenuSlot FILLER = new MenuSlot(null, null, 1, List.of());
    private static final Pattern SKIN_HASH = Pattern.compile("textures\\.minecraft\\.net/texture/([0-9a-fA-F]+)");

    public MenuSlot {
        extra = List.copyOf(extra);
    }

    public static MenuSlot of(Icon icon) {
        return new MenuSlot(icon, null, 1, List.of());
    }

    public static MenuSlot item(String itemId, int amount, List<String> extra) {
        return new MenuSlot(null, itemId, amount, extra);
    }

    /** The same slot, this many (a tier's pane shows its tier as how many). */
    public MenuSlot times(int count) {
        return new MenuSlot(icon, item, count, extra);
    }

    public boolean filler() {
        return icon == null && item == null;
    }

    /** The texture hash in a head's skin (the base64 "textures" value SkyBlock items keep); null for none. */
    public static String hash(String skin) {
        if (skin == null) return null;
        try {
            Matcher m = SKIN_HASH.matcher(new String(Base64.getDecoder().decode(skin), StandardCharsets.UTF_8));
            return m.find() ? m.group(1) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The item, or a barrier if the SkyBlock item isn't there (the items' data is missing). */
    public ItemStack stack() {
        ItemStack stack;
        if (filler()) {
            stack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            stack.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
            return stack;
        }
        if (icon != null) {
            stack = icon.stack();
        } else {
            SkyBlockItem sb = ItemRegistry.get(item);
            if (sb == null) return new Icon(Material.BARRIER, "&c" + item, List.of()).stack();
            stack = ItemBuilder.build(sb);
            ItemLore lore = stack.getData(DataComponentTypes.LORE);
            List<net.kyori.adventure.text.Component> lines = new ArrayList<>(lore == null ? List.of() : lore.lines());
            lines.addAll(Text.lines(extra));
            stack.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        }
        stack.setAmount(Math.max(1, Math.min(amount, stack.getMaxStackSize())));
        return stack;
    }
}
