package net.icxd.dungeons.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.event.GUIOpenEvent;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.menu.SkyBlockMenu;
import net.icxd.dungeons.recipe.Crafting.Cell;
import net.icxd.dungeons.recipe.Crafting.Match;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.User;

/**
 * The crafting table ("Craft Item": {@code /craft}, or the SkyBlock Menu's Crafting Table), as recorded
 * (02:57.7): a 3x3 grid to put items in, the result beside it (a barrier, "Recipe Required", while the
 * grid makes nothing the player has unlocked), three Quick Crafting slots, and red glass along the bottom.
 * Taking the result crafts it once (onto the cursor); a shift-click crafts as many as the grid and the
 * inventory have room for. Crafting takes each slot's share of the recipe and gives Carpentry XP. What's
 * left in the grid goes back to the player when it closes, however it closes (their stash takes what
 * doesn't fit). Items can be shift-clicked in, as the wiki's Crafting Table says of Hypixel's.
 *
 * <p>UNKNOWN, not recorded: the bottom glass turning lime while there's a result (Hypixel's, as players know
 * it); the result item has only its own lore. Quick Crafting itself (VIP and up, Carpentry III) isn't here
 * yet (LATER): its slots show as the recording's empty ones. Main thread.
 */
public final class CraftingTable extends GUI {
    public static final String TITLE = "Craft Item";
    public static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int RESULT = 23;
    static final int[] QUICK = {16, 25, 34};
    static final int[] BOTTOM = {45, 46, 47, 50, 51, 52, 53};
    static final int BACK = 48;
    static final int CLOSE = 49;
    /** "The XP gained is 3% of the combined NPC sell price of the ingredients used" (the wiki's Carpentry). */
    static final double CARPENTRY_SHARE = 0.03;
    private static final Set<Integer> GRID_SLOTS = Set.of(10, 11, 12, 19, 20, 21, 28, 29, 30);

    private final Player viewer;
    private Inventory inventory;
    private boolean refreshing;

    public CraftingTable(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    public static boolean grid(int slot) {
        return GRID_SLOTS.contains(slot);
    }

    // What it shows

    static Icon recipeRequired() {
        return new Icon(Material.BARRIER, "&cRecipe Required", "&7Add the items for a valid recipe in", "&7the crafting grid to the left!");
    }

    /** The recorded empty slot (a VIP+ player's); without a rank, the wiki's red one that says what it needs. */
    static Icon quickCraftingSlot(Rank rank) {
        if (rank == Rank.DEFAULT) {
            return new Icon(Material.RED_STAINED_GLASS_PANE, "&cQuick Crafting Slot", "&7Quick crafting allows you to craft",
                    "&7items without assembling the recipe.", "", "&cRequires &aVIP &cor above.");
        }
        return new Icon(Material.GRAY_STAINED_GLASS_PANE, "&cQuick Crafting Slot", "&7Quick crafting allows you to craft",
                "&7items without assembling the recipe.");
    }

    private static ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
        pane.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return pane;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        for (int slot = 0; slot < Size.SIX; slot++) {
            if (!grid(slot)) set(slot, filler());
        }
        for (int slot : BOTTOM) set(slot, pane(Material.RED_STAINED_GLASS_PANE));
        for (int slot : QUICK) set(slot, quickCraftingSlot(User.rankOf(viewer.getUniqueId())).stack());
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Menu").stack(), viewer,
                () -> new SkyBlockMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
        set(result(null));
    }

    @Override
    public void afterOpen(GUIOpenEvent event) {
        inventory = event.getInventory();
    }

    /** Number keys may swap items in and out of the grid (a menu item's slot still stops them). */
    @Override
    public boolean allowHotkeying() {
        return true;
    }

    /** After every click: the grid may have changed, so the result is worked out again once the click is done. */
    @Override
    public void update(Inventory top) {
        refreshLater();
    }

    private void refreshLater() {
        if (refreshing) return;
        refreshing = true;
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            refreshing = false;
            refresh();
        });
    }

    /** The grid's items as cells, by their SkyBlock id. */
    Cell[] cells() {
        Cell[] cells = new Cell[9];
        if (inventory == null) return cells;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(GRID[i]);
            if (stack == null || stack.isEmpty()) continue;
            NBTTagCompound tag = ItemNBT.read(stack);
            cells[i] = new Cell(tag == null ? null : tag.getString("id"), stack.getAmount());
        }
        return cells;
    }

    /** What the grid makes for this player: a recipe they've unlocked; null for none. */
    private Match match() {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return null;
        Document profile = user.profile();
        return Recipes.index().find(cells(), recipe -> Recipes.unlocked(profile, recipe));
    }

    private void refresh() {
        if (inventory == null || GUI.GUI_MAP.get(viewer.getUniqueId()) != this) return;
        Match match = match();
        GUIClickableItem result = result(match);
        set(result);
        inventory.setItem(RESULT, result.stack());
        Material bottom = match == null ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
        for (int slot : BOTTOM) {
            set(slot, pane(bottom));
            inventory.setItem(slot, pane(bottom));
        }
    }

    /** The result slot: what the grid makes, a click away; "Recipe Required" while it makes nothing. */
    private GUIClickableItem result(Match match) {
        ItemStack shown = match == null ? recipeRequired().stack() : made(match, 1);
        return GUIClickableItem.button(RESULT, shown, viewer, click -> craft(click));
    }

    /** {@code times} crafts' worth of what the match makes; a barrier if the item isn't there. */
    private static ItemStack made(Match match, int times) {
        SkyBlockItem item = ItemRegistry.get(match.recipe().result());
        if (item == null) return recipeRequired().stack();
        return ItemBuilder.build(item, match.count() * times);
    }

    // Crafting

    private void craft(ClickType click) {
        if (inventory == null || GUI.GUI_MAP.get(viewer.getUniqueId()) != this || InventorySyncListener.frozen(viewer)) return;
        Match match = match();
        if (match == null) return;
        ItemStack one = made(match, 1);
        if (one.getType() == Material.BARRIER) return;
        int times;
        if (click.isShiftClick()) {
            times = Math.min(match.times(), room(one));
            if (times <= 0) return;
            List<ItemStack> results = new ArrayList<>();
            int left = one.getAmount() * times;
            while (left > 0) {
                int amount = Math.min(left, one.getMaxStackSize());
                results.add(made(match, 1).asQuantity(amount));
                left -= amount;
            }
            take(match, times);
            ItemStash.give(viewer, results.toArray(new ItemStack[0]));
        } else {
            ItemStack cursor = viewer.getItemOnCursor();
            if (cursor.isEmpty()) {
                viewer.setItemOnCursor(one);
            } else if (cursor.isSimilar(one) && cursor.getAmount() + one.getAmount() <= cursor.getMaxStackSize()) {
                cursor.setAmount(cursor.getAmount() + one.getAmount());
                viewer.setItemOnCursor(cursor);
            } else {
                return;
            }
            times = 1;
            take(match, times);
        }
        carpentry(match, times);
        refresh();
    }

    /** How many crafts of this result fit in the player's inventory (their storage slots, not armor). */
    private int room(ItemStack one) {
        int space = 0;
        ItemStack[] contents = viewer.getInventory().getStorageContents();
        for (ItemStack stack : contents) {
            if (stack == null || stack.isEmpty()) space += one.getMaxStackSize();
            else if (stack.isSimilar(one)) space += Math.max(0, stack.getMaxStackSize() - stack.getAmount());
        }
        return space / one.getAmount();
    }

    /** Takes {@code times} crafts' ingredients from the grid: what {@link Crafting#consume} leaves in each slot. */
    private void take(Match match, int times) {
        int[] taken = match.taken();
        Cell[] left = Crafting.consume(cells(), match, times);
        for (int i = 0; i < 9; i++) {
            if (taken[i] == 0) continue;
            ItemStack stack = inventory.getItem(GRID[i]);
            if (stack == null) continue;
            inventory.setItem(GRID[i], left[i] == null ? null : stack.asQuantity(left[i].amount()));
        }
    }

    /** Carpentry XP for what was used: 3% of its NPC sell price. */
    private void carpentry(Match match, int times) {
        double xp = xp(match, times);
        if (xp > 0) SkillGains.give(viewer, Skill.CARPENTRY, xp);
    }

    /** The Carpentry XP {@code times} crafts of the match give. */
    static double xp(Match match, int times) {
        double price = 0;
        for (RecipeData.Ingredient ingredient : match.shape().cells()) {
            if (ingredient == null) continue;
            SkyBlockItem item = ItemRegistry.get(ingredient.item());
            if (item != null) price += item.npcSellPrice() * ingredient.amount();
        }
        return price * times * CARPENTRY_SHARE;
    }

    // Items in and out

    /** A shift-click in their inventory puts the stack in the grid: onto the same items first, then into an empty slot. */
    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY || inventory == null) return false;
        event.setCancelled(true);
        ItemStack moving = event.getCurrentItem();
        if (moving == null || moving.isEmpty()) return true;
        int left = moving.getAmount();
        for (int slot : GRID) {
            ItemStack there = inventory.getItem(slot);
            if (left <= 0 || there == null || !there.isSimilar(moving)) continue;
            int add = Math.min(left, there.getMaxStackSize() - there.getAmount());
            if (add <= 0) continue;
            inventory.setItem(slot, there.asQuantity(there.getAmount() + add));
            left -= add;
        }
        for (int slot : GRID) {
            if (left <= 0) break;
            ItemStack there = inventory.getItem(slot);
            if (there != null && !there.isEmpty()) continue;
            inventory.setItem(slot, moving.asQuantity(left));
            left = 0;
        }
        event.setCurrentItem(left > 0 ? moving.asQuantity(left) : null);
        return true;
    }

    /** What's left in the grid goes back to them (a disconnect's too: before their items are saved). */
    @Override
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getInventory();
        List<ItemStack> left = new ArrayList<>();
        for (int slot : GRID) {
            ItemStack stack = top.getItem(slot);
            if (stack == null || stack.isEmpty()) continue;
            left.add(stack);
            top.setItem(slot, null);
        }
        if (!left.isEmpty() && event.getPlayer() instanceof Player player) ItemStash.give(player, left.toArray(new ItemStack[0]));
    }

    /** Dragging items across the grid spreads them there, as in any crafting grid: GUIListener stops drags over a menu. */
    public static final class Drags implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        public void onDrag(InventoryDragEvent event) {
            if (!(event.getWhoClicked() instanceof Player player) || !(GUI.GUI_MAP.get(player.getUniqueId()) instanceof CraftingTable table)) return;
            if (InventorySyncListener.frozen(player)) return;
            int top = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) if (slot < top && !grid(slot)) return;
            event.setCancelled(false);
            table.refreshLater();
        }
    }
}
