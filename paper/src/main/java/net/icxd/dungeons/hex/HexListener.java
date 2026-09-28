package net.icxd.dungeons.hex;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.menu.SkyBlockMenuItem;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.utils.Text;

/**
 * The Hex's screens in the server's events: what an input slot takes (see {@link HexScreen}), and the item when
 * they die or leave with it in the Hex (see {@link HexSession}).
 *
 * <p>An input slot takes one item: what storage would keep (not an item that belongs to where they are, like a
 * dungeon's map, nor the SkyBlock Menu) and the screen takes (HexScreen#refuses: the grinder only items with
 * gemstone slots), one at a time. Hypixel's Hex upgrades one item; a stack is refused
 * rather than split (UNKNOWN what Hypixel does with one), so nothing is left over to go anywhere. Taking it out is
 * always fine; nothing goes in from or into a bundle.
 */
public final class HexListener implements Listener {
    /** UNKNOWN: Hypixel's words, if it says anything. */
    static final String ONE_ITEM = "&cYou can only put one item in The Hex at a time!";

    /** Why an item can't go in the input slot: a line to say, or null to say nothing. */
    record Refusal(String message) {
    }

    private static final Refusal SILENT = new Refusal(null);

    /** After GUIListener, which has cancelled a click on a button. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(GUI.GUI_MAP.get(player.getUniqueId()) instanceof HexScreen screen)) return;
        int input = screen.inputSlot();
        if (input < 0 || event.getClickedInventory() != event.getView().getTopInventory() || event.getSlot() != input) return;
        ItemStack there = event.getCurrentItem();
        int inSlot = there == null || there.isEmpty() ? 0 : there.getAmount();
        ItemStack coming = switch (event.getAction()) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> event.getCursor();
            // 0 to 8, or 40 for the off-hand.
            case HOTBAR_SWAP -> player.getInventory().getItem(event.getHotbarButton());
            default -> null;
        };
        String refused = amount(coming) == 0 ? null : screen.refuses(coming);
        Refusal refusal = switch (event.getAction()) {
            case PICKUP_FROM_BUNDLE, PICKUP_ALL_INTO_BUNDLE, PICKUP_SOME_INTO_BUNDLE, PLACE_FROM_BUNDLE, PLACE_ALL_INTO_BUNDLE,
                 PLACE_SOME_INTO_BUNDLE, UNKNOWN -> SILENT;
            case PLACE_ALL, SWAP_WITH_CURSOR, HOTBAR_SWAP -> refusal(event.getAction() == InventoryAction.PLACE_ALL ? inSlot : 0,
                    amount(coming), takes(coming), refused);
            case PLACE_ONE -> refusal(inSlot, amount(coming) == 0 ? 0 : 1, takes(coming), refused);
            // Onto what's there already, so more than one.
            case PLACE_SOME -> refusal(inSlot, Math.max(1, amount(coming)), takes(coming), refused);
            default -> null;
        };
        if (refusal == null) return;
        event.setCancelled(true);
        if (refusal.message() != null) player.sendMessage(Text.line(refusal.message()));
    }

    private static int amount(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0 : stack.getAmount();
    }

    /** What storage would keep: not an item that's never saved, nor the SkyBlock Menu. */
    static boolean takes(ItemStack item) {
        return item == null || item.isEmpty() || StoredInventory.saved(item) && !SkyBlockMenuItem.is(item);
    }

    /**
     * Whether {@code coming} more items may go on the {@code inSlot} in the input slot: null if so. Nothing coming
     * (taking it out) is always fine; an item the slot doesn't take is refused quietly (as storage refuses it);
     * then what the screen itself refuses ({@code refused}, HexScreen#refuses: its line, "" for none); more than one
     * in the slot after is refused with {@link #ONE_ITEM}.
     */
    static Refusal refusal(int inSlot, int coming, boolean takes, String refused) {
        if (coming <= 0) return null;
        if (!takes) return SILENT;
        if (refused != null) return refused.isEmpty() ? SILENT : new Refusal(refused);
        return inSlot + coming > 1 ? new Refusal(ONE_ITEM) : null;
    }

    /** Before SandboxDrops (HIGHEST) marks the drops, so the item is marked with them. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        HexSession session = HexSession.of(event.getPlayer());
        if (session != null) session.dying(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void afterDeath(PlayerDeathEvent event) {
        HexSession session = HexSession.of(event.getPlayer());
        if (session != null) session.died(event);
    }

    /**
     * Their menu closing on the way out has ended the session already; one that's still here (its menu never
     * opened, say another plugin called it off) gives its item back now, before PlayerListener saves them (MONITOR).
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onQuit(PlayerQuitEvent event) {
        HexSession session = HexSession.of(event.getPlayer());
        if (session != null) session.end();
    }
}
