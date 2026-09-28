package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.menu.SkyBlockMenu;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Storage ({@code /storage}, or the chest in the SkyBlock Menu), as recorded (the Loadouts and Storage
 * tour, 02:09.3): the Ender Chest's nine pages across the top, the ones they have in purple glass (or the
 * icon they chose) numbered by stack size, the rest locked in red; and the eighteen backpack slots below,
 * each backpack showing as itself, an empty slot in brown glass and a locked one in gray dye (the wiki's
 * Storage/UI: "Talk to Tia the Fairy to unlock more Backpack Slots!").
 *
 * <p>A page is left-clicked open and right-clicked to choose its icon. A backpack is put in an empty slot
 * by clicking the slot with the backpack, opened with a left click and taken out with a right one (only
 * when it's empty, the wiki's Storage: "Backpacks cannot be removed from the Storage if they contain
 * items"), with the recorded messages, and not twice within a few seconds ("Please wait before doing this
 * again.").
 *
 * <p>How many they have: Ender Chest pages come from the Community Shop and backpack slots from Tia the
 * Fairy for Fairy Souls (the wiki's Ender Chest and Fairy Souls), neither of which is here, so everyone
 * has all nine pages and all eighteen slots until they are (the owner, 2026-09-28: "all can be unlocked
 * for now since the NPCs are missing"). Main thread.
 */
public final class StorageMenu extends GUI {
    public static final String TITLE = "Storage";
    public static final int PAGES = 9;
    public static final int PAGE_SIZE = 45;
    public static final int BACKPACK_SLOTS = 18;
    static final int ENDER_CHEST = 4;
    static final int FIRST_PAGE = 9;
    static final int BACKPACKS = 22;
    static final int FIRST_BACKPACK = 27;
    static final int GO_BACK = 48;
    static final int CLOSE = 49;
    /** Between putting a backpack in and taking one out (recorded: refused 4.9 s after one came out, fine at 6.9 s; UNKNOWN exactly). */
    static final long BACKPACK_COOLDOWN_MILLIS = 5_000;
    private static final Map<UUID, Long> LAST_BACKPACK_CHANGE = new HashMap<>();

    /** A backpack slot as the menu shows it. */
    enum SlotState { LOCKED, EMPTY, FILLED }

    /**
     * A backpack slot: locked, empty, or holding a backpack, with the backpack's name in its rarity's
     * colour ("&5Greater Backpack") and size.
     */
    record BackpackSlot(SlotState state, String name, int size) {
        static final BackpackSlot LOCKED = new BackpackSlot(SlotState.LOCKED, null, 0);
        static final BackpackSlot EMPTY = new BackpackSlot(SlotState.EMPTY, null, 0);
    }

    /** What the menu shows of a profile: how many pages they have, each page's icon (null: glass), and each backpack slot. */
    record View(int pages, List<String> icons, List<BackpackSlot> backpacks) {
    }

    private final Player viewer;

    public StorageMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    /**
     * Ender Chest pages: all of them, a stand-in until the Community Shop sells the rest (the wiki's Ender
     * Chest: the game starts with one).
     */
    public static int pages() {
        return PAGES;
    }

    /** Backpack slots: all of them, a stand-in until Tia the Fairy gives the rest for Fairy Souls (the game starts with one). */
    public static int backpackSlots() {
        return BACKPACK_SLOTS;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document storage = StoredInventory.storage(user.profile());
        View view = view(storage);
        for (Map.Entry<Integer, Shown> e : icons(view).entrySet()) {
            int slot = e.getKey();
            ItemStack stack = e.getValue().stack();
            if (slot >= FIRST_PAGE && slot < FIRST_PAGE + view.pages()) {
                int page = slot - FIRST_PAGE + 1;
                set(GUIClickableItem.button(slot, stack, viewer, click -> {
                    if (click.isRightClick()) new IconMenu(viewer, page).open(viewer);
                    else EnderChestMenu.open(viewer, page);
                }));
                continue;
            }
            int backpack = slot - FIRST_BACKPACK + 1;
            SlotState state = backpack >= 1 && backpack <= BACKPACK_SLOTS ? view.backpacks().get(backpack - 1).state() : null;
            if (state == SlotState.FILLED) {
                set(GUIClickableItem.button(slot, looksLike(stack, StorageDocument.backpack(storage, backpack)), viewer, click -> {
                    if (click.isRightClick()) remove(backpack);
                    else BackpackMenu.open(viewer, backpack);
                }));
            } else if (state == SlotState.EMPTY) {
                set(GUIClickableItem.button(slot, stack, viewer, () -> place(backpack)));
            } else {
                set(slot, stack);
            }
        }
        set(GUIClickableItem.button(GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Menu").stack(), viewer,
                () -> new SkyBlockMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
    }

    /** The profile's pages and backpack slots, as the menu shows them. */
    static View view(Document storage) {
        List<String> icons = new ArrayList<>();
        for (int page = 1; page <= PAGES; page++) icons.add(StorageDocument.icon(storage, page));
        List<BackpackSlot> backpacks = new ArrayList<>();
        int unlocked = backpackSlots();
        for (int slot = 1; slot <= BACKPACK_SLOTS; slot++) {
            StorageDocument.Backpack backpack = StorageDocument.backpack(storage, slot);
            if (backpack == null) {
                backpacks.add(slot <= unlocked ? BackpackSlot.EMPTY : BackpackSlot.LOCKED);
                continue;
            }
            // A backpack in a slot that's locked now still shows, so it can be taken out.
            SkyBlockItem item = ItemRegistry.get(backpack.id());
            String name = item == null ? "&f" + backpack.id() : "&" + item.rarity().getCode() + item.name();
            backpacks.add(new BackpackSlot(SlotState.FILLED, name, StorageItems.backpackSize(item)));
        }
        return new View(pages(), icons, backpacks);
    }

    /** Every slot but the glass, Go Back and Close. */
    static Map<Integer, Shown> icons(View view) {
        Map<Integer, Shown> icons = new LinkedHashMap<>();
        icons.put(ENDER_CHEST, Shown.of(new Icon(Material.ENDER_CHEST, "&aEnder Chest", "&7Store global items you can access",
                "&7anywhere in your ender chest.")));
        for (int page = 1; page <= PAGES; page++) {
            int slot = FIRST_PAGE + page - 1;
            if (page > view.pages()) {
                icons.put(slot, Shown.of(new Icon(Material.RED_STAINED_GLASS_PANE, "&cLocked Page", "&7Unlock more Ender Chest pages in",
                        "&7the community shop!")));
                continue;
            }
            String chosen = view.icons().get(page - 1);
            Material icon = chosen == null ? null : Material.matchMaterial(chosen);
            icons.put(slot, Shown.of(new Icon(icon == null ? Material.PURPLE_STAINED_GLASS_PANE : icon, "&aEnder Chest Page " + page, "",
                    "&8Also accessible via /enderchest " + page, "", "&eLeft-click to open!", "&eRight-click to change icon!"), page));
        }
        icons.put(BACKPACKS, Shown.of(new Icon(Material.CHEST, "&aBackpacks", "&7Place backpack items in these slots",
                "&7to use them as additional storage", "&7that can be accessed anywhere.")));
        for (int slot = 1; slot <= BACKPACK_SLOTS; slot++) {
            BackpackSlot backpack = view.backpacks().get(slot - 1);
            Icon icon = switch (backpack.state()) {
                case FILLED -> new Icon(Material.PLAYER_HEAD, "&6Backpack Slot " + slot, backpack.name(),
                        "&7This backpack has &a" + backpack.size() + "&7 slots.", "", "&8Also accessible via /backpack " + slot, "",
                        "&eLeft-click to open!", "&eRight-click to remove!");
                case EMPTY -> new Icon(Material.BROWN_STAINED_GLASS_PANE, "&eEmpty Backpack Slot " + slot, "",
                        "&eLeft-click a backpack item on this", "&eslot to place it!");
                case LOCKED -> new Icon(Material.GRAY_DYE, "&cLocked Backpack Slot " + slot, "&7Talk to Tia the Fairy to",
                        "&7unlock more Backpack Slots!");
            };
            icons.put(FIRST_BACKPACK + slot - 1, Shown.of(icon, backpack.state() == SlotState.LOCKED ? 1 : slot));
        }
        return icons;
    }

    /** A filled slot shows the backpack itself (its skin), with the slot's name, lore and number. */
    private static ItemStack looksLike(ItemStack shown, StorageDocument.Backpack backpack) {
        ItemStack item = backpack == null ? null : StorageItems.peek(backpack.item());
        if (item == null) return shown;
        ItemMeta meta = item.getItemMeta();
        meta.displayName(shown.getItemMeta().displayName());
        meta.lore(shown.getItemMeta().lore());
        ItemStack look = new ItemStack(item.getType(), shown.getAmount());
        look.setItemMeta(meta);
        return look;
    }

    // Putting backpacks in and taking them out

    /** The cooldown's over (and starts again): else they're told to wait. */
    private boolean ready() {
        long now = System.currentTimeMillis();
        Long last = LAST_BACKPACK_CHANGE.get(viewer.getUniqueId());
        if (last != null && now - last < BACKPACK_COOLDOWN_MILLIS) {
            viewer.sendMessage(Text.line("&cPlease wait before doing this again."));
            return false;
        }
        LAST_BACKPACK_CHANGE.put(viewer.getUniqueId(), now);
        return true;
    }

    /**
     * One backpack from their cursor into an empty slot, with nothing in it (recorded: "Placing backpack in
     * slot 2..." then "Success!").
     */
    private void place(int slot) {
        User user = User.ifLoaded(viewer.getUniqueId());
        ItemStack cursor = viewer.getItemOnCursor();
        SkyBlockItem item = StorageItems.skyBlockItem(cursor);
        if (user == null || InventorySyncListener.frozen(viewer) || StorageItems.backpackSize(item) <= 0 || !StoredInventory.saved(cursor)) return;
        if (!ready()) return;
        viewer.sendMessage(Text.line("&ePlacing backpack in slot " + slot + "..."));
        ItemStack one = cursor.clone();
        one.setAmount(1);
        Document storage = StoredInventory.storage(user.profile());
        if (!StorageDocument.placeBackpack(storage, slot, item.id(), StoredInventory.encode(new ItemStack[]{one}).getFirst())) return;
        ItemStack rest = cursor.clone();
        rest.setAmount(cursor.getAmount() - 1);
        viewer.setItemOnCursor(rest.getAmount() > 0 ? rest : null);
        user.save();
        viewer.sendMessage(Text.line("&aSuccess!"));
        new StorageMenu(viewer).open(viewer);
    }

    /** An empty backpack out of its slot, into their inventory (recorded: "Removed backpack from slot 2!"). */
    private void remove(int slot) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || InventorySyncListener.frozen(viewer)) return;
        Document storage = StoredInventory.storage(user.profile());
        StorageDocument.Backpack backpack = StorageDocument.backpack(storage, slot);
        if (backpack == null) return;
        if (!backpack.empty()) {
            // What Hypixel says then is UNKNOWN.
            viewer.sendMessage(Text.line("&cYou can't remove a backpack that has items in it!"));
            return;
        }
        ItemStack item = StorageItems.peek(backpack.item());
        if (item == null) {
            // Its bytes stay in the slot, for staff to look at.
            viewer.sendMessage(Text.line("&cThis backpack can't be taken out right now."));
            return;
        }
        if (viewer.getInventory().firstEmpty() < 0) {
            // UNKNOWN too.
            viewer.sendMessage(Text.line("&cYou don't have enough space in your inventory!"));
            return;
        }
        if (!ready()) return;
        StorageDocument.removeBackpack(storage, slot);
        viewer.getInventory().addItem(item);
        user.save();
        viewer.sendMessage(Text.line("&aRemoved backpack from slot " + slot + "!"));
        new StorageMenu(viewer).open(viewer);
    }

    static void forget(UUID player) {
        LAST_BACKPACK_CHANGE.remove(player);
    }
}
