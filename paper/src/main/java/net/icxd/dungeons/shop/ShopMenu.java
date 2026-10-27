package net.icxd.dungeons.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.icxd.dungeons.economy.CoinSound;
import net.icxd.dungeons.economy.Coins;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Soulbound;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.pet.OwnedPet;
import net.icxd.dungeons.pet.PetItems;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;
import net.kyori.adventure.text.Component;

/**
 * An NPC's shop menu, as Hypixel lays them out (research coins.md 2.1): 54 slots with a black glass
 * border, the wares in the middle 7 by 4 (a page of 28, with Previous and Next Page arrows at 45 and
 * 53 when there's more), and at 49, where other menus close, the Sell Item hopper, or once they've
 * sold something, the last thing sold, to buy back.
 *
 * <p>Clicking an item in their own inventory sells the whole stack, if a shop takes it (see
 * {@link Selling}); so does putting it on the hopper. The coins go into the purse, the stack onto the
 * profile's buyback list (see {@link Buyback}), and today's sales towards the daily limit (see
 * {@link SellLimit}), with the coins' chime ({@link CoinSound}, recorded). Whether Hypixel sells on a
 * plain click or only a shift-click isn't known; both sell here, and what can't be sold clicks as it
 * would anywhere.
 *
 * <p>A ware's lore is the recorded Farm Merchant's and Ophelia's (2026-10-24): its costs, its stock if
 * it has one ({@link ShopStock}), and "Click to trade!" (or "Not unlocked!"); one that stacks adds
 * "Right-click for more trading options!". Those options (buying several at once) weren't recorded,
 * so a right-click buys one trade, as a left-click does. Left out too: the sell price lore Hypixel
 * adds to their items while a shop is open.
 */
public final class ShopMenu extends GUI {
    static final int SELL = 49;
    static final int PREVIOUS = 45;
    static final int NEXT = 53;
    /** Where the wares go, row by row. */
    static final int[] WARE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40,
            41, 42, 43};
    private static final Set<ClickType> CLICKS = Set.of(ClickType.LEFT, ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT);

    private final Shop shop;
    private final Player viewer;
    private int page;

    public ShopMenu(Shop shop, Player viewer) {
        super(shop.name(), Size.SIX);
        this.shop = shop;
        this.viewer = viewer;
    }

    @Override
    public void beforeOpen(Player player) {
        items();
    }

    private void items() {
        getItems().clear();
        border();
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        List<Shop.Ware> wares = new ArrayList<>();
        Number highest = user.profileValue("dungeons.floors.highest", Number.class);
        for (Shop.Ware ware : shop.wares(highest == null ? 0 : highest.intValue())) {
            if (ItemRegistry.get(ware.item()) != null) wares.add(ware);
        }
        int pages = Math.max(1, (wares.size() + WARE_SLOTS.length - 1) / WARE_SLOTS.length);
        page = Math.clamp(page, 0, pages - 1);
        long now = System.currentTimeMillis();
        for (int i = 0; i < WARE_SLOTS.length && page * WARE_SLOTS.length + i < wares.size(); i++) {
            set(wareButton(WARE_SLOTS[i], wares.get(page * WARE_SLOTS.length + i), user.profile(), now));
        }
        if (page > 0) set(button(PREVIOUS, item(Material.ARROW, "&aPrevious Page", "&ePage " + page), () -> turn(-1)));
        if (page < pages - 1) set(button(NEXT, item(Material.ARROW, "&aNext Page", "&ePage " + (page + 2)), () -> turn(1)));
        set(sellSlot(user));
    }

    /**
     * Black glass around the edge, and inside, nothing but the wares (research coins.md 2.1: the wiki
     * template's {@code fill = 'border'}). The slots inside are empty menu items all the same, so a
     * click on one does nothing (an item on the cursor isn't put there), and a page's wares replace
     * all of the last one's.
     */
    private void border() {
        ItemStack glass = filler();
        for (int slot = 0; slot < getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;
            boolean edge = row == 0 || row == getSize() / 9 - 1 || column == 0 || column == 8;
            set(slot, edge ? glass : ItemStack.empty());
        }
    }

    private void turn(int by) {
        page += by;
        items();
        refresh(viewer.getOpenInventory().getTopInventory());
    }

    /** Runs on a left or right click, shift or not. */
    private static GUIClickableItem button(int slot, ItemStack stack, Runnable action) {
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                if (CLICKS.contains(event.getClick())) action.run();
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

    // Buying

    private GUIClickableItem wareButton(int slot, Shop.Ware ware, Document profile, long now) {
        SkyBlockItem item = ItemRegistry.get(ware.item());
        // As theirs, so a requirement they don't meet shows ("Requires Farming Skill 1", as recorded); a pet, as its level 100.
        ItemStack preview = ware.pet() == null ? null : PetItems.preview(ware.pet());
        ItemStack stack = preview != null ? preview : ItemBuilder.shownTo(item, ware.amount(), viewer);
        List<String> costs = new ArrayList<>();
        for (Cost cost : Shop.costs(ware, viewer)) costs.add(costLine(cost));
        boolean locked = locked(ware, item);
        int stock = ware.stock() > 0 ? ShopStock.remaining(profile, shop, ware, now) : -1;
        boolean bulk = bulk(item.unstackable(), item.genericItemType(), item.material().getMaxStackSize());
        if (ware.amount() > 1) stack.setData(DataComponentTypes.CUSTOM_NAME, Text.line(name(item, stack) + " &8x" + ware.amount()));
        return button(slot, withLore(stack, wareLore(costs, stock, locked, bulk)), () -> buy(ware));
    }

    /**
     * Under the item's own lore: a blank, "&7Cost" and its costs, a blank, the stock if it has one
     * ({@code stock} left today, -1 for none: "&7Stock", "&6640 &7remaining", a blank), then "&eClick to
     * trade!" and for one that stacks "&eRight-click for more trading options!", or "&cNot unlocked!"
     * (the wiki's third tier of Ophelia's, the recorded Rookie Hoe's).
     */
    static List<String> wareLore(List<String> costs, int stock, boolean locked, boolean bulk) {
        List<String> lore = new ArrayList<>(List.of("", "&7Cost"));
        lore.addAll(costs);
        lore.add("");
        if (stock >= 0) {
            lore.add("&7Stock");
            lore.add("&6" + Text.number(stock) + " &7remaining");
            lore.add("");
        }
        if (locked) {
            lore.add("&cNot unlocked!");
        } else {
            lore.add("&eClick to trade!");
            if (bulk) lore.add("&eRight-click for more trading options!");
        }
        return lore;
    }

    /**
     * Whether a ware's item is one Hypixel offers more of at a time: one that stacks (the recorded Flint
     * Arrow, Superboom TNT and the Farm Merchant's crops did; the gear, a potion and a sack didn't), so not
     * a weapon, armor, a tool or equipment, nor one the API calls unstackable.
     */
    static boolean bulk(boolean unstackable, GenericItemType type, int maxStack) {
        return !unstackable && maxStack > 1 && type != GenericItemType.WEAPON && type != GenericItemType.ARMOR && type != GenericItemType.TOOL
                && type != GenericItemType.EQUIPMENT;
    }

    /** Locked in the shop, or its item needs what they haven't done (a requirement that can't be checked yet doesn't lock it). */
    private boolean locked(Shop.Ware ware, SkyBlockItem item) {
        if (ware.locked()) return true;
        if (item.requirements() == null) return false;
        for (Requirement requirement : item.requirements().getRequirements()) {
            if (requirement.requirement() != null && !requirement.requirement().test(viewer)) return true;
        }
        return false;
    }

    /** "&680,000 Coins", "&9Super Cleaver", "&dWither Essence &8x500", as a ware's lore lists its costs. */
    static String costLine(Cost cost) {
        return switch (cost) {
            case CoinCost coins -> "&6" + Coins.format(coins.getAmount()) + " Coins";
            case ItemCost items -> {
                SkyBlockItem item = items.getItem();
                yield (item == null ? "&f" + items.getItemId() : item.rarity().getColor() + item.name())
                        + (items.getAmount() > 1 ? " &8x" + items.getAmount() : "");
            }
            case EssenceCost essence -> "&d" + Utils.title(essence.getEssenceType().name()) + " Essence &8x" + essence.getAmount();
            default -> "&f?";
        };
    }

    private void buy(Shop.Ware ware) {
        User user = customer();
        SkyBlockItem item = ItemRegistry.get(ware.item());
        if (user == null || item == null || locked(ware, item)) return;
        long now = System.currentTimeMillis();
        if (!ShopStock.allows(user.profile(), shop, ware, now)) {
            tell(Selling.OUT_OF_STOCK);
            return;
        }
        List<Cost> costs = Shop.costs(ware, viewer);
        for (Cost cost : costs) {
            if (cost.canPay(viewer, user)) continue;
            tell(cost instanceof CoinCost ? Selling.NOT_ENOUGH_COINS : cost instanceof EssenceCost ? Selling.NOT_ENOUGH_ESSENCE
                    : Selling.NOT_ENOUGH_ITEMS);
            return;
        }
        OwnedPet pet = ware.pet() == null ? null : PetItems.newPet(ware.pet());
        if (ware.pet() != null && pet == null) return;
        ItemStack bought = pet != null ? PetItems.item(pet) : ItemBuilder.build(item, ware.amount());
        // What they hold on the cursor goes back into their inventory when the menu closes, or
        // falls at their feet if it has no room: it needs its place as much as what they buy.
        if (!fits(viewer.getInventory(), viewer.getItemOnCursor(), bought)) {
            tell(Selling.FULL);
            return;
        }
        for (Cost cost : costs) cost.pay(viewer, user);
        ShopStock.record(user.profile(), shop, ware, now);
        viewer.getInventory().addItem(bought);
        double coins = 0;
        for (Cost cost : costs) if (cost instanceof CoinCost c) coins += c.getAmount();
        tell(Selling.bought(pet != null ? PetItems.shopName(pet) : name(item, bought), ware.amount(), coins));
        // The stock it shows is one trade less.
        if (ware.stock() > 0) {
            items();
            refresh(viewer.getOpenInventory().getTopInventory());
        }
    }

    // Selling and buying back

    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        ItemStack stack = event.getCurrentItem();
        if (!CLICKS.contains(event.getClick()) || !empty(event.getCursor()) || sellable(stack) == null) return false;
        event.setCancelled(true);
        if (sell(stack)) event.getClickedInventory().setItem(event.getSlot(), null);
        showSellSlot(event.getView().getTopInventory());
        return true;
    }

    /** The hopper: a stack put on it sells; with nothing in hand, it buys back the last one sold. */
    private GUIClickableItem sellSlot(User user) {
        Buyback.Entry latest = Buyback.latest(user.profile(), System.currentTimeMillis());
        ItemStack back = latest == null ? null : deserialize(latest.item());
        ItemStack shown = back == null
                ? item(Material.HOPPER, "&aSell Item", "&7Click items in your inventory to sell", "&7them to this Shop!")
                // The lines above "Click to buyback!" aren't known; these are a ware's.
                : withLore(back, List.of("", "&7Cost", "&6" + Coins.format(latest.price()) + " Coins", "", "&eClick to buyback!"));
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                ItemStack cursor = event.getCursor();
                if (!empty(cursor)) {
                    if (sellable(cursor) != null && sell(cursor)) event.getView().setCursor(null);
                } else if (latest != null && CLICKS.contains(event.getClick())) {
                    buyBack(latest);
                }
                showSellSlot(event.getView().getTopInventory());
            }

            @Override
            public int slot() {
                return SELL;
            }

            @Override
            public ItemStack stack() {
                return shown;
            }
        };
    }

    private void showSellSlot(Inventory top) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        set(sellSlot(user));
        top.setItem(SELL, get(SELL).stack());
    }

    /** The SkyBlock item a shop would take this stack as; null if it wouldn't (one donated to a Museum is soulbound). */
    private static SkyBlockItem sellable(ItemStack stack) {
        if (stack == null || stack.isEmpty() || StoredInventory.isNotSaved(stack)) return null;
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        return Selling.sellable(item) && ItemBuilder.soulbound(item, tag) == Soulbound.NONE ? item : null;
    }

    /** Sells the stack, which the caller then takes away. @return false if it wasn't sold */
    private boolean sell(ItemStack stack) {
        User user = customer();
        SkyBlockItem item = sellable(stack);
        if (user == null || item == null) return false;
        Document profile = user.profile();
        long now = System.currentTimeMillis();
        int amount = stack.getAmount();
        double total = Selling.total(item.npcSellPrice(), amount);
        if (!SellLimit.allows(profile, total, now)) {
            tell(Selling.LIMIT_REACHED);
            return false;
        }
        Buyback.add(profile, new Buyback.Entry(stack.serializeAsBytes(), total, now), now);
        SellLimit.record(profile, total, now);
        Purse.add(user, total);
        CoinSound.play(viewer);
        tell(Selling.sold(name(item, stack), amount, total));
        return true;
    }

    private void buyBack(Buyback.Entry entry) {
        User user = customer();
        ItemStack back = deserialize(entry.item());
        if (user == null || back == null) return;
        if (!Purse.has(user, entry.price())) {
            tell(Selling.NOT_ENOUGH_COINS);
            return;
        }
        if (!fits(viewer.getInventory(), back)) {
            tell(Selling.FULL);
            return;
        }
        if (!Buyback.remove(user.profile(), entry, System.currentTimeMillis())) return;
        Purse.take(user, entry.price());
        viewer.getInventory().addItem(back);
    }

    // Helpers

    /** Who's using the shop, if their data is here to change (not while it's being handed off). */
    private User customer() {
        User user = User.ifLoaded(viewer.getUniqueId());
        return user == null || user.isReleased() || InventorySyncListener.frozen(viewer) ? null : user;
    }

    private void tell(String line) {
        viewer.sendMessage(Text.line(line));
    }

    /** As the item's name shows it: rarity colour, reforge, stars. */
    private static String name(SkyBlockItem item, ItemStack stack) {
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag == null ? item.rarity().getColor() + item.name() : ItemBuilder.name(item, tag);
    }

    private static ItemStack withLore(ItemStack stack, List<String> more) {
        ItemLore lore = stack.getData(DataComponentTypes.LORE);
        List<Component> lines = new ArrayList<>(lore == null ? List.of() : lore.lines());
        lines.addAll(Text.lines(more));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        return stack;
    }

    private static boolean empty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    private static ItemStack deserialize(byte[] bytes) {
        try {
            return ItemStack.deserializeBytes(bytes);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Whether all of these stacks fit in their inventory together (armor and off hand slots aside):
     * onto stacks like them first, then into empty slots, as they'd go in. Nothing is changed.
     */
    static boolean fits(PlayerInventory inventory, ItemStack... stacks) {
        ItemStack[] slots = inventory.getStorageContents();
        // What each slot would hold, and how many; the inventory's own stacks aren't touched.
        ItemStack[] held = new ItemStack[slots.length];
        int[] amounts = new int[slots.length];
        for (int i = 0; i < slots.length; i++) {
            if (empty(slots[i])) continue;
            held[i] = slots[i];
            amounts[i] = slots[i].getAmount();
        }
        for (ItemStack stack : stacks) {
            if (empty(stack)) continue;
            int max = stack.getMaxStackSize();
            int left = stack.getAmount();
            for (int i = 0; i < slots.length && left > 0; i++) {
                if (held[i] == null || !held[i].isSimilar(stack)) continue;
                int put = Math.clamp(max - amounts[i], 0, left);
                amounts[i] += put;
                left -= put;
            }
            for (int i = 0; i < slots.length && left > 0; i++) {
                if (held[i] != null) continue;
                held[i] = stack;
                amounts[i] = Math.min(max, left);
                left -= amounts[i];
            }
            if (left > 0) return false;
        }
        return true;
    }
}
