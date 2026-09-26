package net.icxd.dungeons.utils;

import com.google.common.collect.Lists;
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
        this.stack = new ItemStack(material);
        this.meta = stack.getItemMeta();
    }

    public StackBuilder setDisplayName(String name) {
        meta.setDisplayName(Utils.color(name));
        return this;
    }

    public StackBuilder setLore(String... lore) {
        meta.setLore(Utils.colorList(Lists.newArrayList(lore)));
        return this;
    }

    public StackBuilder setLore(List<String> lore) {
        meta.setLore(Utils.colorList(lore));
        return this;
    }

    public StackBuilder setSkullTexture(String skin) {
        assert stack.getType() == Material.PLAYER_HEAD;
        this.skin = skin;
        return this;
    }

    public ItemStack build() {
        stack.setItemMeta(meta);
        if (this.skin != null)
            Utils.skull(stack, this.skin);
        return stack;
    }

    public static StackBuilder of(ItemStack stack) {
        return new StackBuilder(stack);
    }
}
