package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.chests.ChestLoot;
import net.icxd.dungeons.dungeons.chests.ChestLoot.Reward;
import net.icxd.dungeons.dungeons.chests.ChestType;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.utils.Text;

/**
 * A reward chest's menu, as recorded for Wood, Gold and Diamond (2026_10_27_06_28_42_1, 2026_10_26_00_59_04_1): titled
 * with the chest, the viewer's loot along the second row with "Click the chest below to purchase these rewards!" under
 * each, the chest below it listing them and their cost, Close, and the Kismet Feather's reroll.
 */
final class RewardChestMenu extends GUI {
    static final int OPEN = 31;
    static final int CLOSE = 49;
    static final int REROLL = 50;
    private static final List<String> PURCHASE = List.of("", "&eClick the chest below to purchase", "&ethese rewards!");

    private final RewardChests chests;
    private final Player viewer;
    private final ChestType type;

    RewardChestMenu(RewardChests chests, Player viewer, ChestType type) {
        super(type.title(), Size.SIX);
        this.chests = chests;
        this.viewer = viewer;
        this.type = type;
        items();
    }

    private void items() {
        fill(filler());
        List<Reward> rewards = chests.loot(viewer, type).rewards();
        int[] slots = contentSlots(rewards.size());
        int middle = 13;
        // The second row's middle five show the loot, grey where there's none (as recorded for three and four).
        for (int slot = middle - 2; slot <= middle + 2; slot++) set(slot, pane());
        for (int i = 0; i < rewards.size() && i < slots.length; i++) set(slots[i], shown(rewards.get(i)));
        set(button(OPEN, openButton(), event -> Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (viewer.isOnline()) chests.open(viewer, type);
        })));
        set(GUIClickableItem.close(CLOSE));
        set(button(REROLL, rerollButton(), event -> Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (viewer.isOnline() && chests.reroll(viewer, type)) new RewardChestMenu(chests, viewer, type).open(viewer, null);
        })));
    }

    /**
     * Where n rewards go: three on 11, 13 and 15, four on 11, 12, 14 and 15 (recorded); one in the middle, two either
     * side of it, five filling the five, and six or seven spreading to the row's ends (UNKNOWN, none recorded).
     */
    static int[] contentSlots(int n) {
        return switch (n) {
            case 0 -> new int[0];
            case 1 -> new int[]{13};
            case 2 -> new int[]{12, 14};
            case 3 -> new int[]{11, 13, 15};
            case 4 -> new int[]{11, 12, 14, 15};
            case 5 -> new int[]{11, 12, 13, 14, 15};
            case 6 -> new int[]{10, 11, 12, 14, 15, 16};
            default -> new int[]{10, 11, 12, 13, 14, 15, 16};
        };
    }

    /** The chest's button: what's in it, its cost, and (for coins) where they come from, as recorded. */
    static List<String> openLore(List<String> contents, long cost) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Contents");
        lore.addAll(contents);
        lore.add("");
        lore.add("&7Cost");
        lore.add(ChestLoot.costLine(cost));
        if (cost > 0) {
            lore.add("");
            lore.add("&cNOTE: Coins are withdrawn from your");
            lore.add("&cbank if you don't have enough in");
            lore.add("&cyour purse.");
        }
        lore.add("");
        lore.add("&eClick to open!");
        return lore;
    }

    /**
     * The feather's button: "Bring a Kismet Feather" without one (recorded); with one, or with the run's reroll used,
     * its last line is UNKNOWN.
     */
    static List<String> rerollLore(boolean hasFeather, boolean rerolled) {
        List<String> lore = new ArrayList<>(List.of("&7Consume a &9Kismet Feather &7to reroll", "&7the loot in this chest.", "",
                "&7You may only use a feather once", "&7per run/chest where applicable.", ""));
        lore.add(rerolled ? "&cYou already rerolled a chest!" : hasFeather ? "&eClick to reroll!" : "&eBring a Kismet Feather");
        return lore;
    }

    private ItemStack openButton() {
        RewardChests.Loot loot = chests.loot(viewer, type);
        List<String> contents = loot.rewards().stream().map(RewardChests::line).toList();
        return item(Material.CHEST, "&aOpen Reward Chest", openLore(contents, loot.cost()));
    }

    private ItemStack rerollButton() {
        return item(Material.FEATHER, "&aReroll Chest", rerollLore(chests.hasFeather(viewer), chests.rerolled(viewer)));
    }

    /** A reward as the menu shows it: the item with the purchase lines under it; essence as its head. */
    private static ItemStack shown(Reward reward) {
        if (reward.essence() != null) {
            String kind = net.icxd.dungeons.utils.Utils.title(reward.essence().name());
            ItemStack head = DungeonTextures.head(kind + " Essence");
            List<String> lore = new ArrayList<>(List.of("&7" + kind + " Essence can be used to", "&7convert some items into Dungeon",
                    "&7items and upgrade them!"));
            lore.addAll(PURCHASE);
            ItemMeta meta = head.getItemMeta();
            meta.displayName(Text.line(RewardChests.line(reward)));
            meta.lore(Text.lines(lore));
            head.setItemMeta(meta);
            return head;
        }
        ItemStack item = RewardChests.item(reward);
        if (item == null) return item(Material.BARRIER, RewardChests.line(reward), PURCHASE.subList(1, PURCHASE.size()));
        ItemMeta meta = item.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.addAll(Text.lines(PURCHASE));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack pane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        pane.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return pane;
    }

    private static GUIClickableItem button(int slot, ItemStack stack, Consumer<InventoryClickEvent> action) {
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                action.accept(event);
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
}
