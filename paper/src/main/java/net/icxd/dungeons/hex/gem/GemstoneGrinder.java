package net.icxd.dungeons.hex.gem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexCosts.Check;
import net.icxd.dungeons.hex.HexCosts.Coins;
import net.icxd.dungeons.hex.HexCosts.Essence;
import net.icxd.dungeons.hex.HexCosts.Items;
import net.icxd.dungeons.hex.HexCosts.Levels;
import net.icxd.dungeons.hex.HexCosts.Part;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexMenu;
import net.icxd.dungeons.hex.HexScreen;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.gemstone.Gem;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.item.gemstone.GemSlots.Slot;
import net.icxd.dungeons.item.gemstone.GemstoneTable;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Geo's "Gemstone Grinder", which puts gemstones in an item's slots, unlocks the slots and takes gems out again.
 * The wiki's Geo/UI has it empty; two screenshots of the real menu with an item in it (a Hyperion's locked slots, a
 * Ring of Power's gems), Skyblock-Tweaks' reading of its lore and two recreations (Swofty's, CarsCupcake's) give
 * the rest (GEMSTONES.md): 6 rows of black glass, the item in 13 (put in and taken out as in a chest, see
 * HexScreen; only items with gemstone slots), the item's slots centred in the fourth row (twelve go on in the
 * fifth), Close in 49 and the Gemstone Guide in 50. Opened from the Hex (the owner's wish: Hypixel's Hex has its own
 * gem page), Go Back in 48 takes the item back to it.
 *
 * <p>A slot: locked, it says what unlocking it costs, and a click pays it all (nothing on a Sandbox profile) and
 * opens it; open and empty, a gem clicked in their own inventory goes in the first one that takes it (free; on a
 * Sandbox profile a click on the slot also picks one, GemPicker, our own); with a gem, the gem itself, with what
 * taking it out costs, which a click confirms (GemRemoval). Main thread.
 */
public final class GemstoneGrinder extends HexScreen {
    public static final String TITLE = "Gemstone Grinder";
    public static final int ITEM = 13;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int GUIDE = 50;
    /** The empty grinder's gray panes: the fourth row's middle seven and the fifth's middle five (the wiki's). */
    static final int[] PLACEHOLDERS = {28, 29, 30, 31, 32, 33, 34, 38, 39, 40, 41, 42};
    /** The same words in both recreations (Swofty's, CarsCupcake's). */
    static final String REFUSED = "&cOnly items that can have Gemstones applied to them can be put in the Grinder!";
    /** UNKNOWN: Swofty's recreation's words, for a gem no open slot takes. */
    static final String NO_SLOT = "&cYou cannot apply that to this item!";
    /** Ours: while the gemstone table (with the removal fees) isn't read. */
    static final String UNAVAILABLE = "&cYou can't remove Gemstones right now!";

    static final Icon PLACEHOLDER = new Icon(Material.GRAY_STAINED_GLASS_PANE, "&dGemstone Slot", "&7Place an item above to apply",
            "&7Gemstones to it!");
    /** The wiki's (Geo/UI), which Swofty's recreation has word for word. */
    static final Icon GUIDE_BUTTON = new Icon(Material.REDSTONE_TORCH, "&aGemstone Guide", guideLore(true));
    /** A design choice, not Hypixel's (whose grinder is only Geo's): the usual Go Back of a menu reached from another. */
    static final Icon BACK_TO_HEX = new Icon(Material.ARROW, "&aGo Back", "&7To The Hex");

    private final boolean fromHex;

    /** @param fromHex opened from the Hex: Go Back goes back to it */
    public GemstoneGrinder(HexSession session, boolean fromHex) {
        super(session, TITLE);
        this.fromHex = fromHex;
    }

    /** Geo's menu on its own, empty (for Geo, when there is one: here it's opened from the Hex). */
    public static void openFor(Player player) {
        if (User.ifLoaded(player.getUniqueId()) == null || InventorySyncListener.frozen(player) || player.isDead()) return;
        HexSession.open(player, session -> new GemstoneGrinder(session, false));
    }

    @Override
    public int inputSlot() {
        return ITEM;
    }

    @Override
    protected String refuses(ItemStack item) {
        HexItem hex = HexItem.of(item, viewer);
        return hex != null && hex.item().gemstoneSlots() != null ? null : REFUSED;
    }

    @Override
    protected void draw() {
        fill(filler());
        HexItem item = session.hexItem();
        List<Slot> slots = item == null ? List.of() : GemSlots.of(item.item(), item.tag());
        if (slots.isEmpty()) {
            for (int slot : PLACEHOLDERS) set(slot, PLACEHOLDER.stack());
        } else {
            List<Integer> places = places(slots.size());
            for (Slot slot : slots) put(places.get(slot.index()), slot);
        }
        if (fromHex) set(button(BACK, BACK_TO_HEX.stack(), () -> session.open(new HexMenu(session))));
        set(GUIClickableItem.close(CLOSE));
        set(button(GUIDE, GUIDE_BUTTON.stack(), () -> session.open(GemstoneGuide.of(session, 0, fromHex))));
    }

    private void put(int place, Slot slot) {
        int index = slot.index();
        if (slot.locked()) {
            HexCosts cost = unlockCost(slot);
            List<String> block = costBlock("&7Cost", cost.check(session), session.sandbox() || cost.parts().isEmpty(), "&eClick to unlock!");
            set(button(place, locked(slot, block).stack(), () -> clicked(index)));
        } else if (slot.gem() == null) {
            Icon icon = empty(slot, session.sandbox());
            if (session.sandbox()) set(button(place, icon.stack(), () -> clicked(index)));
            else set(place, icon.stack());
        } else {
            set(button(place, withLore(gemStack(slot.gem()), removal(removalLore(slot))), () -> clicked(index)));
        }
    }

    /** A slot's button, on the next tick: unlocks it, picks a gem for it (Sandbox) or asks to take its gem out. */
    private void clicked(int index) {
        HexItem item = session.hexItem();
        List<Slot> slots = item == null ? List.of() : GemSlots.of(item.item(), item.tag());
        if (index >= slots.size()) {
            redraw();
            return;
        }
        Slot slot = slots.get(index);
        if (slot.locked()) unlock(item, slot);
        else if (slot.gem() != null) session.open(new GemRemoval(session, index, fromHex));
        else if (session.sandbox()) session.open(new GemPicker(session, index, fromHex, 0));
    }

    /** Pays for a slot (all of it, or nothing) and opens it. UNKNOWN: Hypixel's words, if it says any; ours. */
    private void unlock(HexItem item, Slot slot) {
        HexCosts cost = unlockCost(slot);
        List<Check> checks = cost.check(session);
        if (!checks.stream().allMatch(Check::owned)) {
            say(String.join(" ", missing(checks)));
            return;
        }
        NBTTagCompound tag = session.tag();
        GemSlots.unlock(tag, item.item(), slot.index());
        if (session.upgrade(cost, tag, null)) say("&aYou unlocked the " + slotName(slot.type()) + "&a!");
    }

    // A gem clicked in their inventory

    /**
     * A gem clicked in their own inventory (a left or right click, shift or not), with an item in the grinder: it
     * goes in the item on the next tick. Anything else is as on any input screen (see HexScreen), which refuses a
     * gem shift-clicked into the empty grinder.
     */
    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        Gem gem = gem(event.getCurrentItem());
        if (gem == null || session.item() == null || !GUIClickableItem.pressed(event.getClick())) return super.onPlayerInventoryClick(event);
        event.setCancelled(true);
        int from = event.getSlot();
        later(() -> apply(from, gem));
        return true;
    }

    /**
     * One gem from that inventory slot into the first open slot that takes it, for nothing (the wiki's Gemstone
     * Slot: "The apply process is otherwise free of charge"). The gem leaves their inventory in the same moment the
     * item gets it, and comes back if the item couldn't be made.
     */
    private void apply(int from, Gem gem) {
        HexItem item = session.hexItem();
        PlayerInventory inventory = viewer.getInventory();
        ItemStack stack = inventory.getItem(from);
        if (item == null || !gem.equals(gem(stack))) return;
        int index = GemSlots.target(GemSlots.of(item.item(), item.tag()), gem.type());
        if (index < 0) {
            say(NO_SLOT);
            return;
        }
        NBTTagCompound tag = session.tag();
        GemSlots.apply(tag, item.item(), index, gem);
        ItemStack before = stack.clone();
        inventory.setItem(from, before.getAmount() > 1 ? before.asQuantity(before.getAmount() - 1) : null);
        if (session.upgrade(HexCosts.NOTHING, tag, null)) session.applied(gemName(gem));
        else inventory.setItem(from, before);
    }

    /** The gem a stack is; null for none (or not a SkyBlock item). */
    static Gem gem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag == null ? null : Gem.of(tag.getString("id"));
    }

    // Where the slots go

    /**
     * Where an item's {@code count} slots go, in its slots' order: centred in the fourth row, the middle left empty
     * for an even count (Swofty's and CarsCupcake's recreations; the screenshots' Hyperion 30, 32 and Ring of Power
     * 29, 30, 32, 33), all seven for seven; past seven, the rest centred the same way in the fifth row (twelve, the
     * Relic of Power's, in its middle five, as the empty grinder's panes are; UNKNOWN for 8 to 11: no item has them).
     */
    static List<Integer> places(int count) {
        List<Integer> places = new ArrayList<>(row(Math.min(count, 7), 27));
        if (count > 7) places.addAll(row(Math.min(count - 7, 7), 36));
        return places;
    }

    /** {@code count} (1 to 7) centred in the row that starts at {@code start}. */
    static List<Integer> row(int count, int start) {
        int[] columns = switch (count) {
            case 0 -> new int[0];
            case 1 -> new int[]{4};
            case 2 -> new int[]{3, 5};
            case 3 -> new int[]{3, 4, 5};
            case 4 -> new int[]{2, 3, 5, 6};
            case 5 -> new int[]{2, 3, 4, 5, 6};
            case 6 -> new int[]{1, 2, 3, 5, 6, 7};
            default -> new int[]{1, 2, 3, 4, 5, 6, 7};
        };
        List<Integer> row = new ArrayList<>(columns.length);
        for (int column : columns) row.add(start + column);
        return row;
    }

    // The slots' items

    /** "&b✎ Sapphire Gemstone Slot": its icon and type in its colour. */
    static String slotName(GemstoneType type) {
        return "&" + type.getColor() + type.getIcon() + " " + type.getName() + " Gemstone Slot";
    }

    /** "&9❁ Fine Jasper Gemstone": in its quality's colour, which is its item's rarity's (Rough Common to Perfect Legendary). */
    static String gemName(Gem gem) {
        return "&" + gem.quality().getColor() + gem.name();
    }

    /**
     * A locked slot: its name in red whatever its type (the screenshot), a gray pane (Swofty's; UNKNOWN: Hypixel's
     * item, which the screenshot's texture pack draws as a padlock), and the lore the recreations have above the cost
     * (UNKNOWN: its words and wrap), with the gems a special slot takes; then the cost block (see {@link #costBlock}).
     */
    static Icon locked(Slot slot, List<String> costBlock) {
        GemstoneType type = slot.type();
        List<String> lore = new ArrayList<>(List.of("&7This slot is locked! Purchasing this", "&7slot allows you to apply a",
                "&" + type.getColor() + type.getIcon() + " " + type.getName() + " Gemstone &7to it!", ""));
        if (!type.gem()) {
            for (GemstoneType gem : type.accepted()) lore.add("&" + gem.getColor() + gem.getName() + " Gemstone");
            lore.add("");
        }
        lore.addAll(costBlock);
        return new Icon(Material.GRAY_STAINED_GLASS_PANE, "&c" + type.getIcon() + " " + slot.name(), lore);
    }

    /**
     * An open, empty slot: its name in its colour (the screenshot's "Amethyst Slot" in purple), a pane of its colour
     * and what to do (Swofty's; UNKNOWN: Hypixel's item and words). On a Sandbox profile a click picks a gem (ours).
     */
    static Icon empty(Slot slot, boolean sandbox) {
        GemstoneType type = slot.type();
        List<String> lore = new ArrayList<>();
        if (type.gem()) {
            lore.addAll(List.of("&7Click a &" + type.getColor() + type.getName() + " Gemstone &7of any", "&7quality in your inventory to apply it",
                    "&7to this item!"));
        } else {
            lore.addAll(List.of("&7Click &aany Gemstone &7of any quality in", "&7your inventory to apply it to this item!", "",
                    "&7Applicable Gemstones"));
            for (GemstoneType gem : type.accepted()) lore.add("&" + gem.getColor() + gem.getName() + " Gemstone");
        }
        if (sandbox) lore.addAll(List.of("", "&eClick to choose a Gemstone!"));
        return new Icon(pane(type), slotName(type), lore);
    }

    /** Both recreations' panes by the slot's colour (UNKNOWN: Hypixel's; its screenshot has a texture pack's gem outline). */
    static Material pane(GemstoneType type) {
        return switch (type) {
            case RUBY, CITRINE, COMBAT -> Material.RED_STAINED_GLASS_PANE;
            case AMETHYST, MINING -> Material.PURPLE_STAINED_GLASS_PANE;
            case JADE, DEFENSIVE -> Material.LIME_STAINED_GLASS_PANE;
            case SAPPHIRE, AQUAMARINE -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case AMBER, CHISEL -> Material.ORANGE_STAINED_GLASS_PANE;
            case TOPAZ -> Material.YELLOW_STAINED_GLASS_PANE;
            case JASPER -> Material.PINK_STAINED_GLASS_PANE;
            case OPAL, UNIVERSAL -> Material.WHITE_STAINED_GLASS_PANE;
            case PERIDOT -> Material.GREEN_STAINED_GLASS_PANE;
            case ONYX -> Material.BLACK_STAINED_GLASS_PANE;
            case OFFENSIVE -> Material.BLUE_STAINED_GLASS_PANE;
        };
    }

    /** What's under a filled slot's gem: a blank line and the removal's cost block (Swofty's; Skyblock-Tweaks reads "Cost" and "Remove"). */
    static List<String> removal(List<String> costBlock) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(costBlock);
        return lore;
    }

    /** A filled slot's removal block, for them now. */
    private List<String> removalLore(Slot slot) {
        return removalBlock(removalCost(slot), session, "&eClick to remove!");
    }

    /**
     * The removal's cost block: free on a Sandbox profile and for a gem its slot no longer takes; without the table
     * (so without the fees), it can't be taken out (our own words).
     */
    static List<String> removalBlock(HexCosts cost, HexSession session, String action) {
        boolean free = session.sandbox() || cost != null && cost.parts().isEmpty();
        if (cost == null && !free) return List.of("&7Cost to Remove", "&cUnknown", "", UNAVAILABLE);
        return costBlock("&7Cost to Remove", free ? List.of() : cost.check(session), free, action);
    }

    // Costs

    /**
     * What unlocking a slot costs, as the item data has it: coins first (the screenshot's Hyperion, and Swofty's;
     * UNKNOWN whether always: the data lists some slots' gems first), then the items and essence in its order.
     */
    static HexCosts unlockCost(Slot slot) {
        List<Part> coins = new ArrayList<>(), rest = new ArrayList<>();
        for (Cost cost : slot.costs()) {
            switch (cost) {
                case CoinCost c -> coins.add(new Coins(c.getAmount()));
                case ItemCost c -> rest.add(new Items(c.getItemId(), c.getAmount()));
                case EssenceCost c -> rest.add(new Essence(c.getEssenceType(), c.getAmount()));
                default -> throw new IllegalStateException("a gemstone slot can't cost " + cost.getClass().getSimpleName());
            }
        }
        coins.addAll(rest);
        return HexCosts.of(coins);
    }

    /**
     * What taking a slot's gem out costs: its quality's fee (Rough 1 coin to Perfect 500,000, the table's), nothing
     * for a gem its slot no longer takes (the wiki's Gemstone Slot); null while the table isn't read.
     */
    static HexCosts removalCost(Slot slot) {
        if (!slot.fits()) return HexCosts.NOTHING;
        GemstoneTable table = GemstoneTable.get();
        return table == null ? null : HexCosts.of(new Coins(table.removalCost(slot.gem().quality())));
    }

    /**
     * A cost block as the grinder's lore has it (the screenshot and Skyblock-Tweaks): {@code header}, each part on a
     * line ("&6250,000 Coins", "&5✎ Flawless Sapphire Gemstone &8x4", no "x1"), a blank line and {@code action}; or,
     * when they can't pay, what's missing in its place (the Hex's lines, decision 3: there's no Bazaar here). Free
     * (a Sandbox profile), "&aFree" for the parts (our own wording, as the Hex's).
     */
    static List<String> costBlock(String header, List<Check> checks, boolean free, String action) {
        return costBlock(header, checks, free, action, GemstoneGrinder::itemName);
    }

    static List<String> costBlock(String header, List<Check> checks, boolean free, String action, Function<String, String> itemName) {
        List<String> lore = new ArrayList<>();
        lore.add(header);
        if (free) lore.add("&aFree");
        else for (Check check : checks) lore.add(line(check.part(), itemName));
        lore.add("");
        if (free || checks.stream().allMatch(Check::owned)) lore.add(action);
        else lore.addAll(missing(checks));
        return lore;
    }

    static String line(Part part, Function<String, String> itemName) {
        return switch (part) {
            case Coins c -> "&6" + Text.number(c.amount()) + " Coins";
            case Items i -> itemName.apply(i.id()) + (i.amount() > 1 ? " &8x" + i.amount() : "");
            case Essence e -> "&d" + capitalized(e.type().name()) + " Essence &8x" + e.amount();
            case Levels l -> "&3" + l.levels() + " Exp Levels";
        };
    }

    /** What they lack, a missing item first (none can be bought here), then coins, then essence. */
    static List<String> missing(List<Check> checks) {
        for (Class<? extends Part> kind : List.of(Items.class, Coins.class, Essence.class, Levels.class)) {
            for (Check check : checks) {
                if (check.owned() || !kind.isInstance(check.part())) continue;
                return switch (check.part()) {
                    case Items _ -> List.of("&cYou don't have that in your", "&cinventories!");
                    case Coins _ -> List.of("&cYou don't have enough Coins!");
                    case Essence e -> List.of("&cYou don't have enough " + capitalized(e.type().name()) + " Essence!");
                    case Levels _ -> List.of("&cYou don't have enough Exp Levels!");
                };
            }
        }
        return List.of();
    }

    /** "&5✎ Flawless Sapphire Gemstone": an item in its rarity's colour; its id if there's no such item. */
    static String itemName(String id) {
        SkyBlockItem item = ItemRegistry.get(id);
        return item == null ? "&f" + id : item.rarity().getColor() + item.name();
    }

    private static String capitalized(String name) {
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    // Stacks

    /** A gem's own item, as the item data makes it; a sheet of paper with its name if the data has no such item. */
    static ItemStack gemStack(Gem gem) {
        SkyBlockItem item = ItemRegistry.get(gem.id());
        return item == null ? new Icon(Material.PAPER, gemName(gem)).stack() : ItemBuilder.build(item);
    }

    /** The stack with these lines after its lore. */
    static ItemStack withLore(ItemStack stack, List<String> more) {
        ItemLore lore = stack.getData(DataComponentTypes.LORE);
        List<net.kyori.adventure.text.Component> lines = new ArrayList<>(lore == null ? List.of() : lore.lines());
        lines.addAll(Text.lines(more));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        return stack;
    }

    /** The Gemstone Guide button's lore (the wiki's), and without "Click to view!" its header's. */
    static List<String> guideLore(boolean click) {
        List<String> lore = new ArrayList<>(List.of("&7Many items can have &dGemstones", "&d&7applied to them. Gemstones increase",
                "&7the stats of an item based on the", "&7type of Gemstone used.", "", "&7There are several &aqualities &7of",
                "&7Gemstones, ranging from &fRough &7to", "&7&6Perfect&7. The higher the quality, the", "&7better the stat!", "",
                "&7This guide shows the items that can", "&7have Gemstones applied to them."));
        if (click) lore.addAll(List.of("", "&eClick to view!"));
        return lore;
    }
}
