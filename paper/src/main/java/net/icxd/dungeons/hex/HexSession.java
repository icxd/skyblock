package net.icxd.dungeons.hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.storage.StoredItems;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * A player's time in the Hex (and Geo's Gemstone Grinder, which is opened with its item): the item they put in,
 * which it keeps from the first screen they open to the last, passing it between them (see {@link #open}) and
 * never through their inventory on the way. It's in exactly one place at a time: a screen's input slot (the main
 * menu's, the grinder's), or here (while a page only shows it).
 *
 * <p>It goes back to them (ItemStash#give: their inventory, else their stash) when the last screen closes,
 * however that happens: they close it, another menu opens, they disconnect, are handed off to another server or
 * switch profiles (each closes the menu first). Two ways around that: when the server stops, the plugin's
 * events are already off, so nothing hears the menu close; but every save of their data writes the item with it,
 * into the profile's overflow, which their next join gives back (see {@link #save}), so a save has it there or
 * wherever they took it since, never both (a crash too gives back what the last save had). And when they die,
 * Paper has worked out the drops before it closes the menu and empties the inventory after, so the item is put in
 * the drops (HexListener; kept, if they keep their inventory). Main thread.
 */
public final class HexSession {
    private static final Map<UUID, HexSession> OPEN = new HashMap<>();

    private final Player player;
    /** The screen with the item: null before the first is shown and once it's all over. */
    private HexScreen screen;
    /** Being opened by {@link #open}: the screens' open goes through here. */
    private HexScreen opening;
    /** The item while no screen's input slot has it: on a page that only shows it, or between two screens. */
    private ItemStack held;
    private boolean ended;
    /** When something was last paid for (Bukkit's tick); -1 for never. */
    private int paidTick = -1;
    /** How many of each item (by id) their storage has, till something is paid for or another screen opens. */
    private Map<String, Integer> stored;
    /** The copy of the item the last save put in the profile's overflow, and that profile's document. */
    private Binary saved;
    private Document savedIn;
    /** The copy put in the drops while they die (see HexListener). */
    private ItemStack deathDrop;

    private HexSession(Player player) {
        this.player = player;
    }

    /** Their session, while a screen of the Hex is open; null for none. */
    public static HexSession of(Player player) {
        return OPEN.get(player.getUniqueId());
    }

    /**
     * Opens a screen of the Hex for them: in the session they have open, with its item; else in a new one, empty
     * (the Hex's main menu from {@code /hex}, the grinder on its own).
     */
    public static HexSession open(Player player, Function<HexSession, ? extends HexScreen> screen) {
        HexSession session = of(player);
        // One left behind by a menu that never said it closed: its item goes back first.
        if (session != null && (session.screen == null || GUI.GUI_MAP.get(player.getUniqueId()) != session.screen)) session.end();
        if (session == null || session.ended) {
            session = new HexSession(player);
            OPEN.put(player.getUniqueId(), session);
        }
        session.open(screen.apply(session));
        return session;
    }

    public Player player() {
        return player;
    }

    /** Their data; null if it isn't loaded. */
    public User user() {
        return User.ifLoaded(player.getUniqueId());
    }

    /** On a Sandbox profile: no requirements, and everything is free (see HexCosts). */
    public boolean sandbox() {
        return HexRequirements.sandbox(player);
    }

    /** The screen the item is in now; null when none is. */
    public HexScreen screen() {
        return screen;
    }

    /** Whether this screen is the one they have open with the item (a button's click still counts). */
    public boolean showing(HexScreen s) {
        return !ended && screen == s && GUI.GUI_MAP.get(player.getUniqueId()) == s;
    }

    // The item

    /** The item in the Hex now (the stack itself: don't change it, see {@link #replace}); null for none. */
    public ItemStack item() {
        if (held != null) return held;
        return screen == null ? null : screen.input();
    }

    /** It as the categories see it; null for none, or an item that isn't a SkyBlock item. */
    public HexItem hexItem() {
        return HexItem.of(item(), player);
    }

    /** A copy of its data to change and give to {@link #replace}; null if it has none. */
    public NBTTagCompound tag() {
        ItemStack item = item();
        return item == null ? null : ItemNBT.read(item);
    }

    /** What kind of SkyBlock item it is; null for none. */
    public SkyBlockItem skyBlockItem() {
        HexItem item = hexItem();
        return item == null ? null : item.item();
    }

    /**
     * An upgrade bought: takes its cost (see HexCosts#take; nothing on a Sandbox profile), makes the item from
     * {@code tag} ({@link #replace}), saves the two together, and says "You applied a <upgrade> to your <item>!"
     * ({@code upgrade} null for nothing said). False, and nothing paid or changed, if they can't pay or the data
     * isn't a SkyBlock item's.
     */
    public boolean upgrade(HexCosts cost, NBTTagCompound tag, String upgrade) {
        if (ended || item() == null || screen == null || kind(tag) == null || !cost.take(this)) return false;
        replace(tag);
        User user = user();
        if (user != null) user.save();
        if (upgrade != null) applied(upgrade);
        return true;
    }

    /**
     * Makes the item again from this data (ItemBuilder#build, for them), in its place, and draws the screen
     * again: after an upgrade (see {@link #upgrade}, which also pays for it and saves). False, and nothing
     * changed, if there's no item or the data isn't a SkyBlock item's.
     */
    public boolean replace(NBTTagCompound tag) {
        ItemStack current = item();
        SkyBlockItem kind = kind(tag);
        if (ended || current == null || kind == null || screen == null) return false;
        ItemStack rebuilt = ItemBuilder.build(kind, tag, current.getAmount(), player);
        if (held != null || !screen.putInput(rebuilt)) held = rebuilt;
        screen.redraw();
        return true;
    }

    private static SkyBlockItem kind(NBTTagCompound tag) {
        return tag == null ? null : ItemRegistry.get(tag.getString("id"));
    }

    // Screens

    /**
     * Shows another screen with the item: it's taken off the one open now (out of its input slot) and put on the
     * next (into its input slot, if it has one) as that opens; the one closing doesn't give it back. If the next
     * doesn't open after all, it goes back where it was (or to them, if nothing's open).
     */
    public void open(HexScreen next) {
        if (ended || next.session() != this) return;
        HexScreen from = screen;
        // Open already: its menu made again would close this one, and with it the session.
        if (next == from) {
            if (showing(next)) next.redraw();
            return;
        }
        if (from != null) {
            ItemStack in = from.removeInput();
            if (in != null) held = in;
        }
        screen = next;
        stored = null;
        opening = next;
        try {
            next.open(player);
        } finally {
            opening = null;
            if (!ended && GUI.GUI_MAP.get(player.getUniqueId()) != next) notOpened(from, next);
        }
    }

    private void notOpened(HexScreen from, HexScreen next) {
        ItemStack in = next.removeInput();
        if (in != null) held = in;
        if (from != null && GUI.GUI_MAP.get(player.getUniqueId()) == from) {
            screen = from;
            if (held != null && from.putInput(held)) held = null;
            from.redraw();
        } else {
            end();
        }
    }

    boolean opening(HexScreen s) {
        return opening == s;
    }

    /** A screen is being shown (its menu is made): the item goes in its input slot, if it has one. */
    void shown(HexScreen s) {
        if (s != screen || held == null) return;
        if (s.putInput(held)) held = null;
    }

    /** A screen closed: unless the next one is taking over (see {@link #open}), that's the end. */
    void closed(HexScreen s) {
        if (s == screen) end();
    }

    /** The item goes back to them, and the session is over. */
    void end() {
        if (ended) return;
        ended = true;
        OPEN.remove(player.getUniqueId(), this);
        List<ItemStack> back = new ArrayList<>();
        if (held != null) back.add(held);
        if (screen != null) {
            ItemStack in = screen.removeInput();
            if (in != null) back.add(in);
        }
        held = null;
        screen = null;
        unsave();
        if (fastOne(paidTick, Bukkit.getCurrentTick())) player.sendMessage(Text.line("&cTrying to pull a fast one, eh?!"));
        if (!back.isEmpty()) ItemStash.give(player, back.toArray(new ItemStack[0]));
    }

    /**
     * The item's gone (dropped where they died): the session no longer has it, and nothing is given back. Main
     * thread.
     */
    void forget() {
        held = null;
        if (screen != null) screen.removeInput();
        if (screen != null && GUI.GUI_MAP.get(player.getUniqueId()) == screen) screen.redraw();
    }

    // Costs

    /** How many of an item (by id) they have where the Hex takes it from: their inventory, then their storage. */
    public int have(String id) {
        return HexCosts.inventoryCount(player, id) + stored().getOrDefault(id.toUpperCase(), 0);
    }

    /** Their storage's items by id, counted once until something's paid for (nothing else changes storage while the Hex is open). */
    Map<String, Integer> stored() {
        if (stored == null) {
            User user = user();
            stored = user == null ? Map.of() : StoredItems.counts(player, user.profile());
        }
        return stored;
    }

    /** Storage is counted again when next asked (before paying: what it has now). */
    void recount() {
        stored = null;
    }

    /** Something was paid for, now. */
    void paid() {
        paidTick = Bukkit.getCurrentTick();
        stored = null;
    }

    /**
     * "&aYou applied a Recombobulator 3000 to your Heroic Dreadlord Sword!" (SkyHanni's ReforgeHelper, which
     * matches {@code You applied an? .+ to your .+!}). UNKNOWN: its colours; the item's and the upgrade's own.
     */
    public void applied(String upgrade) {
        HexItem item = hexItem();
        String name = item == null ? "item" : item.name();
        player.sendMessage(Text.line(appliedLine(upgrade, name)));
    }

    static String appliedLine(String upgrade, String itemName) {
        String plain = upgrade.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
        String article = !plain.isEmpty() && "AEIOUaeiou".indexOf(plain.charAt(0)) >= 0 ? "an" : "a";
        return "&aYou applied " + article + " " + upgrade + " &ato your " + itemName + "&a!";
    }

    /**
     * "Trying to pull a fast one, eh?!" (the wiki's The Hex: when you buy an upgrade and quickly close the menu;
     * the upgrade is still applied): here, when it closes in the same tick something was paid for.
     */
    static boolean fastOne(int paidTick, int closedTick) {
        return paidTick >= 0 && closedTick == paidTick;
    }

    // Saves and deaths

    /**
     * With every save of their data (StoredInventory#captureWith): the item as it is now goes in the profile's
     * overflow, in place of the copy the save before put there; nothing, if the Hex has no item now (they took it
     * out, so it's saved where they put it). When the session ends normally, its copy comes out again ({@link
     * #unsave}), in the same moment the item goes back to them. See the class's doc.
     */
    void save(Document profile) {
        ItemStack item = item();
        Binary copy = item == null || !StoredInventory.saved(item) ? null : new Binary(item.serializeAsBytes());
        if (savedIn != null && savedIn != profile) unsave();
        Document storage = StoredInventory.storage(profile);
        storage.put(StoredInventory.OVERFLOW, overflow(list(storage.get(StoredInventory.OVERFLOW)), saved, copy));
        saved = copy;
        savedIn = copy == null ? null : profile;
    }

    private void unsave() {
        if (savedIn == null) return;
        Document storage = StoredInventory.storage(savedIn);
        storage.put(StoredInventory.OVERFLOW, overflow(list(storage.get(StoredInventory.OVERFLOW)), saved, null));
        saved = null;
        savedIn = null;
    }

    /** The overflow with the previous copy taken out (that very one: another item may be the same) and the new one after the rest. */
    static List<Object> overflow(List<?> overflow, Object previous, Object current) {
        List<Object> out = new ArrayList<>(overflow.size() + 1);
        for (Object entry : overflow) if (entry != previous || previous == null) out.add(entry);
        if (current != null) out.add(current);
        return out;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    /** Before SandboxDrops marks the drops: the item goes in them, unless they keep their inventory. */
    void dying(PlayerDeathEvent event) {
        ItemStack item = item();
        deathDrop = null;
        if (item == null || event.getKeepInventory()) return;
        deathDrop = item.clone();
        event.getDrops().add(deathDrop);
    }

    /** After everyone else: dropped for real, the session forgets it; otherwise it stays (the menu gives it back as it closes). */
    void died(PlayerDeathEvent event) {
        ItemStack drop = deathDrop;
        deathDrop = null;
        boolean inDrops = drop != null && event.getDrops().stream().anyMatch(d -> d == drop);
        if (dropped(event.isCancelled(), event.getKeepInventory())) {
            // Not in the drops (taken out after it was put in, or keepInventory turned off since): still dropped,
            // or the emptied inventory would lose it.
            ItemStack item = item();
            if (!inDrops && item != null) event.getDrops().add(drop != null ? drop : item.clone());
            forget();
        } else if (inDrops) {
            event.getDrops().removeIf(d -> d == drop);
        }
    }

    /**
     * Whether a death drops the Hex's item: yes when it happens (not called off, as in a dungeon) and the inventory
     * is emptied (no keepInventory, so the item couldn't go back into it).
     */
    static boolean dropped(boolean cancelled, boolean keepInventory) {
        return !cancelled && !keepInventory;
    }
}
