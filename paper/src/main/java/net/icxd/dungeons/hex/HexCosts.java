package net.icxd.dungeons.hex;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.storage.StoredItems;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * What an upgrade in the Hex costs: coins, items (by SkyBlock id), essence and Exp levels, shown in its entry's
 * "Cost" block (the wiki's Hex screens) and paid all at once or not at all.
 *
 * <p>Where it comes from, on a Normal profile: coins from the purse, items from the inventory and then Storage
 * (the Ender Chest's pages, then the backpacks; Hypixel's sacks too, but sacks hold nothing here), essence from the
 * profile, Exp levels from the vanilla ones. Hypixel buys what's missing from the Bazaar; there's no Bazaar here,
 * so an item they don't have is simply missing ("You don't have that in your inventories!"), and no Bazaar lines
 * show. On a Sandbox profile it's all free: nothing is checked or taken, and the block says so ("Free", our own
 * wording). Main thread.
 */
public final class HexCosts {
    /** One thing it costs. */
    public sealed interface Part permits Coins, Items, Essence, Levels {
    }

    /** Coins from the purse. */
    public record Coins(double amount) implements Part {
    }

    /** This many of an item, by its SkyBlock id. */
    public record Items(String id, int amount) implements Part {
    }

    public record Essence(EssenceType type, int amount) implements Part {
    }

    /** Vanilla Exp levels (enchanting). */
    public record Levels(int levels) implements Part {
    }

    /** A part and whether they have it. */
    public record Check(Part part, boolean owned) {
    }

    public static final HexCosts NOTHING = new HexCosts(List.of());

    private final List<Part> parts;

    private HexCosts(List<Part> parts) {
        this.parts = List.copyOf(parts);
    }

    /** In the order the block lists them: Hypixel's reforge stones show their coins first, then the stone. */
    public static HexCosts of(Part... parts) {
        return new HexCosts(List.of(parts));
    }

    public static HexCosts of(List<Part> parts) {
        return new HexCosts(parts);
    }

    public List<Part> parts() {
        return parts;
    }

    // Checking

    /**
     * Each part, and whether they have it (on a Sandbox profile, all of it). Parts of a kind count together (two
     * of an item's, coins twice), so what's checked is what's taken.
     */
    public List<Check> check(HexSession session) {
        boolean free = session.sandbox();
        Totals totals = totals();
        List<Check> checks = new ArrayList<>(parts.size());
        for (Part part : parts) checks.add(new Check(part, free || has(session, part, totals)));
        return checks;
    }

    /** Whether they can pay all of it. */
    public boolean affordable(HexSession session) {
        return check(session).stream().allMatch(Check::owned);
    }

    private static boolean has(HexSession session, Part part, Totals totals) {
        Player player = session.player();
        User user = session.user();
        if (user == null) return false;
        return switch (part) {
            case Coins c -> Purse.has(user, totals.coins());
            case Items i -> session.have(i.id()) >= totals.items().get(i.id().toUpperCase());
            case Essence e -> new EssenceCost(e.type(), totals.essence().get(e.type())).canPay(player, user);
            case Levels l -> player.getLevel() >= totals.levels();
        };
    }

    /** All of it added up by kind: coins, Exp levels, each essence, and each item by id (upper case). */
    record Totals(double coins, int levels, Map<EssenceType, Integer> essence, Map<String, Integer> items) {
    }

    Totals totals() {
        double coins = 0;
        int levels = 0;
        Map<EssenceType, Integer> essence = new EnumMap<>(EssenceType.class);
        Map<String, Integer> items = new LinkedHashMap<>();
        for (Part part : parts) {
            switch (part) {
                case Coins c -> coins += c.amount();
                case Items i -> items.merge(i.id().toUpperCase(), i.amount(), Integer::sum);
                case Essence e -> essence.merge(e.type(), e.amount(), Integer::sum);
                case Levels l -> levels += l.levels();
            }
        }
        return new Totals(coins, levels, essence, items);
    }

    // The block

    /**
     * The "Cost" block, then a blank line and {@code action} (what a click does, "&eClick to apply!", which each
     * page words) if they can pay, else what's missing. The entry's description and the blank line before are
     * the page's.
     */
    public List<String> lore(HexSession session, String action) {
        return lore(check(session), session.sandbox(), action, HexCosts::itemName);
    }

    /**
     * The block, for tests too:
     * <pre>
     * &7Cost
     * &610,000 Coins &a✔
     * &5Wither Blood &c✖
     *
     * &cYou don't have that in your
     * &cinventories!
     * </pre>
     * On a Sandbox profile, "&7Cost", "&aFree" (our own wording: UNKNOWN), a blank line and the action.
     */
    static List<String> lore(List<Check> checks, boolean sandbox, String action, Function<String, String> itemName) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Cost");
        if (sandbox) {
            lore.add("&aFree");
        } else {
            for (Check check : checks) lore.add(line(check.part(), itemName) + (check.owned() ? " &a✔" : " &c✖"));
        }
        lore.add("");
        lore.addAll(sandbox ? List.of(action) : actionLines(checks, action));
        return lore;
    }

    /**
     * A part's line. The coins' and an item's are the wiki's ("&610,000 Coins", "&5Wither Blood"); UNKNOWN: an
     * item's amount past one ("&8x4", Hypixel's usual), essence's and Exp levels' lines (NEU's "§3N Exp Levels").
     */
    static String line(Part part, Function<String, String> itemName) {
        return switch (part) {
            case Coins c -> "&6" + Text.number(c.amount()) + " Coins";
            case Items i -> itemName.apply(i.id()) + (i.amount() > 1 ? " &8x" + i.amount() : "");
            case Essence e -> "&d" + capitalized(e.type().name()) + " Essence &8x" + e.amount();
            case Levels l -> "&3" + l.levels() + " Exp Levels";
        };
    }

    /**
     * The action, or why it can't be paid: a missing item first (it can't be had here), then coins, as the wiki's
     * screens say them with the Bazaar gone (decision 3). UNKNOWN: essence's and Exp levels' lines.
     */
    static List<String> actionLines(List<Check> checks, String action) {
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
        return List.of(action);
    }

    /** "&5Hot Potato Book": in its rarity's colour; the id if there's no such item. */
    static String itemName(String id) {
        SkyBlockItem item = ItemRegistry.get(id);
        return item == null ? "&f" + id : item.rarity().getColor() + item.name();
    }

    private static String capitalized(String name) {
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    // Paying

    /**
     * Takes all of it and saves: for a cost that doesn't change the item (an upgrade that does is {@link
     * HexSession#upgrade}, which saves the two together). See {@link #take}.
     */
    public boolean pay(HexSession session) {
        if (!take(session)) return false;
        session.user().save();
        return true;
    }

    /**
     * Takes all of it, if they have all of it (checked again now); false, and nothing taken, if they don't (or
     * their items are frozen, a hand-off). On a Sandbox profile nothing is taken. Either way it counts as paid now
     * (for "Trying to pull a fast one, eh?!"). Doesn't save: the caller does, with what it bought.
     */
    boolean take(HexSession session) {
        Player player = session.player();
        User user = session.user();
        if (user == null || InventorySyncListener.frozen(player)) return false;
        if (session.sandbox()) {
            session.paid();
            return true;
        }
        session.recount();
        Totals totals = totals();
        for (Part part : parts) {
            if (!has(session, part, totals)) return false;
        }
        for (Part part : parts) {
            switch (part) {
                case Coins c -> Purse.take(user, c.amount());
                case Items i -> takeItems(player, user, i.id(), i.amount());
                case Essence e -> new EssenceCost(e.type(), e.amount()).pay(player, user);
                case Levels l -> player.setLevel(player.getLevel() - l.levels());
            }
        }
        session.paid();
        return true;
    }

    /** From the inventory first, then storage. */
    private static void takeItems(Player player, User user, String id, int amount) {
        int left = amount - takeFromInventory(player, id, amount);
        if (left > 0) StoredItems.take(player, user.profile(), id, left);
    }

    /** How many of an item (by id) their inventory has: its slots, armor and off-hand. */
    static int inventoryCount(Player player, String id) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) if (is(stack, id)) count += stack.getAmount();
        return count;
    }

    /** Takes up to {@code amount} from the inventory, first slot first; how many it took. */
    private static int takeFromInventory(Player player, String id, int amount) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        int taken = 0;
        for (int slot = 0; slot < contents.length && taken < amount; slot++) {
            ItemStack stack = contents[slot];
            if (!is(stack, id)) continue;
            int take = Math.min(stack.getAmount(), amount - taken);
            inventory.setItem(slot, take == stack.getAmount() ? null : stack.asQuantity(stack.getAmount() - take));
            taken += take;
        }
        return taken;
    }

    private static boolean is(ItemStack stack, String id) {
        if (stack == null || stack.isEmpty()) return false;
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag != null && id.equalsIgnoreCase(tag.getString("id"));
    }
}
