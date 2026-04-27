package net.icxd.dungeons.utils;

import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemBuilder {
  private final ItemStack stack;
  private final ItemMeta meta;
  private String skin = null;

  public ItemBuilder(ItemStack stack) {
    this.stack = stack;
    this.meta = stack.getItemMeta();
  }

  public ItemBuilder(Material material) {
    this.stack = new ItemStack(material);
    this.meta = stack.getItemMeta();
  }

  public ItemBuilder setDisplayName(String name) {
    meta.setDisplayName(Utils.color(name));
    return this;
  }

  public ItemBuilder setLore(String... lore) {
    meta.setLore(Utils.colorList(Lists.newArrayList(lore)));
    return this;
  }

  public ItemBuilder setLore(List<String> lore) {
    meta.setLore(Utils.colorList(lore));
    return this;
  }

  public ItemBuilder setSkullTexture(String skin) {
    assert stack.getType() == Material.SKULL_ITEM;
    assert stack.getDurability() == 3;
    this.skin = skin;
    return this;
  }

  public ItemStack build() {
    stack.setItemMeta(meta);
    if (this.skin != null)
      Utils.skull(stack, this.skin);
    return stack;
  }

  public static ItemBuilder of(ItemStack stack) {
    return new ItemBuilder(stack);
  }
}
