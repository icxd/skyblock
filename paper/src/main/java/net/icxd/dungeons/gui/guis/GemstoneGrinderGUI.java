package net.icxd.dungeons.gui.guis;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.gui.item.GUIItem;
import net.icxd.dungeons.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class GemstoneGrinderGUI extends GUI {
    public GemstoneGrinderGUI() {
        super("Gemstone Grinder", Size.SIX);

//        fill(new GUIItem(new ItemBuilder(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)).setDisplayName(" ").build()));
//        fill(new GUIItem(new ItemBuilder(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7)).setDisplayName("&dGemstone Slot")
//                .setLore("&7Place an item above to apply", "&7Gemstones to it!").build()), 28, 35);
//        addItem(13, new GUIClickableItem(13, null, true, player -> {
//            System.out.println("Clicked!");
//        }));
//        addItem(49, new GUIItem(new ItemBuilder(new ItemStack(Material.BARRIER)).setDisplayName("&cClose").build()));
//        addItem(50, new GUIItem(new ItemBuilder(new ItemStack(Material.REDSTONE_TORCH_ON)).setDisplayName("&aGemstone Guide")
//                .setLore(
//                        "&7Many items can have &dGemstones",
//                        "&7applied to them. Gemstones",
//                        "&7increase the stats of an item",
//                        "&7based on the type of Gemstone",
//                        "&7used.",
//                        "",
//                        "&7There are several &aqualities",
//                        "&7of Gemstones, ranging from",
//                        "&fRough &7to &dPerfect&7. The",
//                        "&7higher the quality, the better",
//                        "&7the stat!",
//                        "",
//                        "&7This guide shows the items that",
//                        "&7can have Gemstones applied to",
//                        "&7them."
//                ).build()));
    }
}
