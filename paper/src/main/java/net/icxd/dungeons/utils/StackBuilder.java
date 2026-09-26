package net.icxd.dungeons.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** A plain (menu) item stack; SkyBlock items are built by {@link net.icxd.dungeons.item.ItemBuilder}. */
public class StackBuilder {
    private final ItemStack stack;
    private final ItemMeta meta;
    private String skin = null;

    public StackBuilder(ItemStack stack) {
        this.stack = stack;
        this.meta = stack.getItemMeta();
    }

    public StackBuilder(Material material) {
        this(new ItemStack(material));
    }

    /** With {@code &} colours, not italic (see {@link Text#line}). */
    public StackBuilder setDisplayName(String name) {
        meta.displayName(Text.line(name));
        return this;
    }

    public StackBuilder setLore(String... lore) {
        return setLore(List.of(lore));
    }

    public StackBuilder setLore(List<String> lore) {
        meta.lore(Text.lines(lore));
        return this;
    }

    public StackBuilder setSkullTexture(String skin) {
        this.skin = skin;
        return this;
    }

    public ItemStack build() {
        stack.setItemMeta(meta);
        if (this.skin != null && stack.getType() == Material.PLAYER_HEAD) Utils.skull(stack, this.skin);
        return stack;
    }

    public static StackBuilder of(ItemStack stack) {
        return new StackBuilder(stack);
    }
}
