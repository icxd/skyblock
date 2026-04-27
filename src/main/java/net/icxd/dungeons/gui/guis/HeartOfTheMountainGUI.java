package net.icxd.dungeons.gui.guis;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dwarven.HeartOfTheMountain;
import net.icxd.dungeons.dwarven.Perk;
import net.icxd.dungeons.dwarven.PowderType;
import net.icxd.dungeons.event.GUIOpenEvent;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.gui.item.GUIItem;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.ItemBuilder;
import net.icxd.dungeons.utils.Tree;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HeartOfTheMountainGUI extends GUI {
  private final User user;
  private final HeartOfTheMountain hotm;
  private int slot = 40;

  public HeartOfTheMountainGUI(User user, HeartOfTheMountain hotm) {
    super("Heart of the Mountain", Size.SIX);

    this.user = user;
    this.hotm = hotm;

    this.fill(new ItemBuilder(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)).setDisplayName(" ").build());
    this.set(GUIClickableItem.close(45));
    this.set(new GUIItem() {
      @Override
      public int slot() {
        return 49;
      }

      @Override
      public ItemStack stack() {
        return new ItemBuilder(new ItemStack(Material.SKULL_ITEM, 1, (short)3))
            .setDisplayName("&5Heart of the Mountain")
            .setLore(
                "&7Token of the Mountain: &5"+user.getHOTMTokens(),
                "",
                "&8Use &5Token of the Mountain &8to unlock",
                "&8perks and abilities above!",
                "",
                "&9❖ Powder",
                "&9Powders &8are dropped from mining",
                "&8ores in the &2Dwarven Mines &8and are",
                "&8used to upgrade the perks you've",
                "&8unlocked",
                "",
                "&7Mithril Powder: &2"+Utils.getFormattedNumber(user.getHOTMPowder(PowderType.MITHRIL)),
                "&7Gemstone Powder: &d"+Utils.getFormattedNumber(user.getHOTMPowder(PowderType.GEMSTONE)),
                "&7Glacite Powder: &b"+Utils.getFormattedNumber(user.getHOTMPowder(PowderType.GLACITE)),
                "",
                "&8Increase your chance to gain extra",
                "&8Powder by unlocking perks, equipping",
                "&8the &2Mithril Golem Pet&8, and more!"
            )
            .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODZmMDZlYWEzMDA0YWVlZDA5YjNkNWI0NWQ5NzZkZTU4NGU2OTFjMGU5Y2FkZTEzMzYzNWRlOTNkMjNiOWVkYiJ9fX0=")
            .build();
      }
    });
  }

  @Override
  public void onOpen(GUIOpenEvent event) {
    Tree.Node<Perk> root = this.hotm.getTree().root();
    Queue<Tree.Node<Perk>> queue = new LinkedList<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      Tree.Node<Perk> node = queue.remove();
      this.set(this.buildNodeItem(node));
      this.incrementSlot(node.direction());
      queue.addAll(node.children());
      this.refresh(event.getInventory());
    }
  }

  private GUIItem buildNodeItem(Tree.Node<Perk> node) {
    Perk perk = node.value();
    List<String> lore = new ArrayList<>();

    int level = user.getHOTMPerkLevel(perk);
    boolean unlocked = level > 0;

    lore.add("&7Level " + Math.max(level, 1) + "&8/" + perk.getMaxLevel());
    lore.add("");
    lore.addAll(Utils.split(perk.getDescription().apply(Math.max(level, 1)), 30));
    lore.add("");
    if (unlocked) {
      lore.add("&a&l=====[ UPGRADE ]=====");
      lore.add("&7Level " + (level + 1) + "&8/" + perk.getMaxLevel());
      lore.add("");
      lore.addAll(Utils.split(perk.getDescription().apply(level + 1), 30));
      lore.add("");
      lore.add("&7Cost");
      lore.add(perk.getType().getColor()+Utils.getFormattedNumber(perk.getCostFormula().apply(level + 1))+" "+Utils.title(perk.getType().name())+" Powder");
      lore.add("");
      lore.add("&a&lENABLED");
    } else {
      lore.add("&7Cost");
      lore.add("&51 Token of the Mountain");
    }
    lore.add("");
    if (unlocked) {
      lore.add("&eRight-click to &cdisable&e!");
      lore.add("&eLeft-click to upgrade!");
      lore.add("&eShift Left-click to upgrade 10 levels!");
    } else {
      if (user.getHOTMTokens() > 1)
        lore.add("&eClick to unlock!");
      else
        lore.add("&cYou don't have enough Token of the Mountain!");
    }

    return new GUIClickableItem() {
      @Override
      public void run(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        player.sendMessage(
            ChatColor.stripColor(event.getClickedInventory().getItem(slot()).getItemMeta().getDisplayName())
                .replace("'", "")
                .replace(" ", "_")
                .toUpperCase()
        );
      }

      @Override public int slot() { return slot; }
      @Override public ItemStack stack() {
        return new ItemBuilder(Material.COAL)
            .setDisplayName((unlocked ? "&e" : "&c") + perk.getName())
            .setLore(lore)
            .build();
      }
    };
  }

  private void incrementSlot(Tree.GrowthDirection direction) {
    switch (direction) {
      case UP -> slot -= 9;
      case DOWN -> slot += 9;
      case LEFT -> slot -= 1;
      case RIGHT -> slot += 1;
    }
  }
}
