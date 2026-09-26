package net.icxd.dungeons.gui.guis;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.SignInput;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Every SkyBlock item, to look through and take (/item, for staff): 45 a page, each as players get
 * it, with the page arrows, a sort, rarity and type filters and a search along the bottom, in the
 * style of Hypixel's auction house. Left click takes one, shift click a stack, right click puts the
 * id in chat to copy. The menu opens again after each change, as Hypixel's do, and each player's
 * choices are kept until they leave.
 */
public final class ItemBrowserGUI extends GUI {
    private static final int PREVIOUS = 45;
    private static final int SORT = 46;
    private static final int RARITY = 47;
    private static final int TYPE = 48;
    private static final int CLOSE = 49;
    private static final int SEARCH = 50;
    private static final int NEXT = 53;
    /** The middle of the item rows, for "No items match". */
    private static final int NOTHING = 22;
    /** How many of the type filter's options its lore lists at once (there are dozens with the data items). */
    private static final int TYPE_LINES = 12;
    /** Hypixel's search sign: the query goes on the first line. */
    private static final List<String> SEARCH_SIGN = List.of("", "^^^^^^^^^^^^^^^", "Enter query", "");

    private static final Map<UUID, ItemBrowser> SESSIONS = new HashMap<>();
    private static boolean listening;

    private final Player viewer;
    private final ItemBrowser browser;
    private final ItemBrowser.Page page;
    private final List<String> types;

    private ItemBrowserGUI(Player viewer, ItemBrowser browser, ItemBrowser.Page page, List<String> types) {
        super("SkyBlock Items (" + (page.page() + 1) + "/" + page.pages() + ")", Size.SIX);
        this.viewer = viewer;
        this.browser = browser;
        this.page = page;
        this.types = types;
        items();
    }

    /** Opens the browser where this player left it. */
    public static void show(Player player) {
        if (!listening) {
            Bukkit.getPluginManager().registerEvents(new Sessions(), Dungeons.getInstance());
            listening = true;
        }
        ItemBrowser browser = SESSIONS.computeIfAbsent(player.getUniqueId(), id -> new ItemBrowser());
        List<ItemBrowser.Entry> entries = new ArrayList<>();
        for (SkyBlockItem item : ItemRegistry.getRegistry().values())
            entries.add(new ItemBrowser.Entry(item.id(), item.name(), item.rarity(), item.typeKey()));
        new ItemBrowserGUI(player, browser, browser.view(entries), ItemBrowser.types(entries)).open(player);
    }

    private void items() {
        fill(filler());
        List<ItemBrowser.Entry> entries = page.entries();
        if (entries.isEmpty()) set(NOTHING, item(Material.BARRIER, "&cNo items match!", "&7Try another search or filter."));
        for (int i = 0; i < entries.size(); i++) {
            SkyBlockItem item = ItemRegistry.get(entries.get(i).id());
            if (item != null) set(itemButton(i, item));
        }

        if (page.page() > 0)
            set(button(PREVIOUS, item(Material.ARROW, "&aPrevious Page", "&ePage " + page.page()), forward -> browser.previousPage()));
        if (page.page() < page.pages() - 1)
            set(button(NEXT, item(Material.ARROW, "&aNext Page", "&ePage " + (page.page() + 2)), forward -> browser.nextPage()));

        List<String> sorts = new ArrayList<>();
        for (ItemBrowser.Sort sort : ItemBrowser.Sort.values()) sorts.add(sort.getLabel());
        set(button(SORT, options(Material.HOPPER, "&aSort", sorts, browser.getSort().ordinal(), 0, "&eClick to switch sort!"),
                browser::cycleSort));

        List<String> rarities = new ArrayList<>(List.of("No filter"));
        for (Rarity rarity : Rarity.values()) rarities.add(ItemBrowser.label(rarity.name()));
        int selectedRarity = browser.getRarity() == null ? 0 : browser.getRarity().ordinal() + 1;
        set(button(RARITY, options(Material.ENDER_EYE, "&aItem Tier", rarities, selectedRarity, 0, "&eClick to switch filter!"),
                browser::cycleRarity));

        List<String> typeLabels = new ArrayList<>(List.of("No filter"));
        for (String type : types) typeLabels.add(ItemBrowser.label(type));
        int selectedType = browser.getType() == null ? 0 : types.indexOf(browser.getType()) + 1;
        set(button(TYPE, options(Material.NAME_TAG, "&aItem Type", typeLabels, selectedType, TYPE_LINES, "&eClick to switch filter!"),
                forward -> browser.cycleType(types, forward)));

        set(GUIClickableItem.close(CLOSE));
        set(searchButton());
    }

    /** Left click: one. Shift click: a stack (1 if it doesn't stack). Right click: its id, to copy. */
    private GUIClickableItem itemButton(int slot, SkyBlockItem item) {
        ItemStack stack = ItemBuilder.build(item);
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                // By exact click: Bukkit counts a double click as a left one too.
                switch (event.getClick()) {
                    case LEFT -> give(item, false);
                    case SHIFT_LEFT, SHIFT_RIGHT -> give(item, true);
                    case RIGHT -> viewer.sendMessage(Text.line("&aItem ID: &b" + item.id() + " &e(click to copy)")
                            .hoverEvent(HoverEvent.showText(Text.line("&eClick to copy!")))
                            .clickEvent(ClickEvent.copyToClipboard(item.id())));
                    default -> {
                    }
                }
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

    /** A stack is what a slot holds of it: 64 of most things, 16 pearls, 1 sword. */
    private void give(SkyBlockItem item, boolean stack) {
        ItemStack given = ItemBuilder.build(item);
        if (stack) given.setAmount(item.unstackable() ? 1 : Math.min(64, given.getMaxStackSize()));
        if (!viewer.getInventory().addItem(given).isEmpty()) viewer.sendMessage(Text.line("&cYour inventory is full!"));
    }

    private GUIClickableItem searchButton() {
        String query = browser.getQuery();
        List<Component> lore = new ArrayList<>(Text.lines(List.of("&7Find items by name or ID.", "")));
        if (!query.isEmpty()) {
            // As typed: an & in the query isn't a colour.
            lore.add(Text.line("&7Filtered: ").append(Component.text(query, NamedTextColor.YELLOW)));
            lore.add(Component.empty());
            lore.add(Text.line("&bRight-click to clear!"));
        }
        lore.add(Text.line(query.isEmpty() ? "&eClick to search!" : "&eClick to edit filter!"));
        ItemStack sign = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = sign.getItemMeta();
        meta.displayName(Text.line("&aSearch"));
        meta.lore(lore);
        sign.setItemMeta(meta);

        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                ClickType click = event.getClick();
                if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                    browser.search("");
                    reopen();
                    return;
                }
                if (click != ClickType.LEFT && click != ClickType.SHIFT_LEFT) return;
                // Not from inside the click: the menu closes first, so the server doesn't think it's still open.
                Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
                    if (!viewer.isOnline()) return;
                    viewer.closeInventory();
                    SignInput.open(viewer, SEARCH_SIGN, lines -> {
                        browser.search(lines.isEmpty() ? "" : lines.getFirst());
                        if (viewer.isOnline()) show(viewer);
                    });
                });
            }

            @Override
            public int slot() {
                return SEARCH;
            }

            @Override
            public ItemStack stack() {
                return sign;
            }
        };
    }

    /**
     * A sort or filter: its options with the selected one marked, as the auction house's are. With
     * {@code lines} above 0, only that many options around the selected one are listed.
     */
    private static ItemStack options(Material material, String name, List<String> options, int selected, int lines, String click) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        int from = lines > 0 ? ItemBrowser.window(options.size(), selected, lines) : 0;
        int to = lines > 0 ? Math.min(options.size(), from + lines) : options.size();
        if (from > 0) lore.add("&8...");
        for (int i = from; i < to; i++) lore.add(i == selected ? "&b▶ " + options.get(i) : "&7" + options.get(i));
        if (to < options.size()) lore.add("&8...");
        lore.add("");
        lore.add("&bRight-click to go backwards!");
        lore.add(click);
        return item(material, name, lore.toArray(String[]::new));
    }

    /** On a left click ({@code true}) or right click ({@code false}) does something, then opens the menu again. */
    private GUIClickableItem button(int slot, ItemStack stack, Consumer<Boolean> action) {
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                ClickType click = event.getClick();
                if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) action.accept(true);
                else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) action.accept(false);
                else return;
                reopen();
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

    private void reopen() {
        // Not from inside the click.
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (viewer.isOnline()) show(viewer);
        });
    }

    private static ItemStack filler() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        pane.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return pane;
    }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.line(name));
        meta.lore(Text.lines(List.of(lore)));
        item.setItemMeta(meta);
        return item;
    }

    /** A player's choices last until they leave. */
    private static final class Sessions implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            SESSIONS.remove(event.getPlayer().getUniqueId());
        }
    }
}
