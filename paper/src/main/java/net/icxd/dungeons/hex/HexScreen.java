package net.icxd.dungeons.hex;

import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.event.GUIOpenEvent;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.utils.Text;

/**
 * A menu the Hex's item is in: the Hex's main menu, its pages, and Geo's Gemstone Grinder. Its {@link HexSession}
 * holds the item and passes it from one to the next (see {@link HexSession#open}); opening one always goes
 * through the session, so the item is never given back and taken again on the way.
 *
 * <p>A screen with an {@link #inputSlot} (the main menu's 22, the grinder's 13) has the item really in that slot,
 * where it's put in and taken out as in a chest: by a click, a number key or a shift-click from their inventory,
 * one item at a time (see HexListener); the rest of its slots are buttons and glass. Any other screen shows the
 * item as a picture. Each draws itself from the item as it is now ({@link #draw}), and again whenever the item
 * changes: after a click on an input screen (on the next tick), and after {@link HexSession#replace}. A button
 * does nothing if the item isn't the one it was drawn for any more (see {@link #button}). Main thread.
 */
public abstract class HexScreen extends GUI {
    protected final HexSession session;
    protected final Player viewer;
    private Inventory inventory;
    private boolean redrawing;
    /** The item as it was when the buttons were last drawn (a copy); null for none. */
    private ItemStack drawnFor;

    protected HexScreen(HexSession session, String title) {
        super(title, Size.SIX);
        this.session = session;
        this.viewer = session.player();
    }

    public HexSession session() {
        return session;
    }

    /** The slot the item is really in, to take out and put in; -1 if this screen only shows it. */
    public int inputSlot() {
        return -1;
    }

    /**
     * Puts every slot's item (glass included) but the input slot's, with {@code set}, for the item as it is now
     * ({@code session.item()}, {@code session.hexItem()}).
     */
    protected abstract void draw();

    /** Draws it again, into the open menu too: after the item changed. */
    public final void redraw() {
        drawAll();
        if (inventory != null) refresh(inventory);
    }

    private void drawAll() {
        getItems().clear();
        ItemStack item = session.item();
        drawnFor = item == null ? null : item.clone();
        draw();
        if (inputSlot() >= 0) set(inputSlot(), null);
    }

    /** Opened through the session, which passes the item on (see {@link HexSession#open}). */
    @Override
    public final void open(Player player) {
        if (!session.opening(this)) {
            session.open(this);
            return;
        }
        super.open(player);
    }

    @Override
    public final void beforeOpen(Player player) {
        drawAll();
    }

    /** The item goes in the input slot before the menu is shown. */
    @Override
    public final void onOpen(GUIOpenEvent event) {
        inventory = event.getInventory();
        session.shown(this);
    }

    /** The session hears of it: the end, unless another of its screens is taking the item over. */
    @Override
    public final void onClose(InventoryCloseEvent event) {
        session.closed(this);
    }

    Inventory inventory() {
        return inventory;
    }

    /** What's in the input slot, taken out of it; null for nothing (or no input slot). */
    ItemStack removeInput() {
        if (inputSlot() < 0 || inventory == null) return null;
        ItemStack item = inventory.getItem(inputSlot());
        inventory.setItem(inputSlot(), null);
        return item == null || item.isEmpty() ? null : item;
    }

    /** Puts the item in the input slot; false if there's none (the session holds it then). */
    boolean putInput(ItemStack item) {
        if (inputSlot() < 0 || inventory == null) return false;
        inventory.setItem(inputSlot(), item);
        return true;
    }

    /** What's in the input slot now, left there; null for nothing. */
    ItemStack input() {
        if (inputSlot() < 0 || inventory == null) return null;
        ItemStack item = inventory.getItem(inputSlot());
        return item == null || item.isEmpty() ? null : item;
    }

    /**
     * A button of this screen: a left or right click (shift or not) runs {@code action} on the next tick, only
     * while this screen is still the one open with the item, their items aren't frozen (a hand-off), and the item
     * is still the one the button was drawn for. On an input screen they can swap the item in the tick between the
     * click and the action, or click a button drawn for the one before; then nothing happens but a redraw, so an
     * upgrade worked out for one item is never put on another.
     */
    protected GUIClickableItem button(int slot, ItemStack stack, Consumer<ClickType> action) {
        return GUIClickableItem.button(slot, stack, viewer, click -> {
            if (!ready()) return;
            if (!Objects.equals(drawnFor, session.item())) {
                redraw();
                return;
            }
            action.accept(click);
        });
    }

    protected GUIClickableItem button(int slot, ItemStack stack, Runnable action) {
        return button(slot, stack, click -> action.run());
    }

    private boolean ready() {
        return session.showing(this) && !InventorySyncListener.frozen(viewer);
    }

    /** Says a line to them in chat. */
    protected void say(String line) {
        viewer.sendMessage(Text.line(line));
    }

    // An input slot

    @Override
    public boolean allowHotkeying() {
        return inputSlot() >= 0;
    }

    /** Only its buttons wait out the cooldown between clicks: the item moves in and out as fast as in a chest. */
    @Override
    public boolean rateLimited(InventoryClickEvent event) {
        return event.getClickedInventory() == event.getView().getTopInventory() && event.getSlot() != inputSlot();
    }

    /** A shift-click on an item in their inventory puts it in the empty input slot, if the slot takes it (see HexListener). */
    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return false;
        event.setCancelled(true);
        ItemStack moving = event.getCurrentItem();
        if (inputSlot() < 0 || inventory == null || moving == null || moving.isEmpty() || input() != null) return true;
        HexListener.Refusal refusal = HexListener.refusal(0, moving.getAmount(), HexListener.takes(moving));
        if (refusal != null) {
            if (refusal.message() != null) say(refusal.message());
            return true;
        }
        inventory.setItem(inputSlot(), moving.clone());
        event.setCurrentItem(null);
        return true;
    }

    /** After every click: the item may have come or gone, so it's drawn again once the click is done. */
    @Override
    public void update(Inventory top) {
        if (inputSlot() < 0 || redrawing) return;
        redrawing = true;
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            redrawing = false;
            if (session.showing(this)) redraw();
        });
    }
}
