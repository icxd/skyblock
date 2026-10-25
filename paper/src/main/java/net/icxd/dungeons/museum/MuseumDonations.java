package net.icxd.dungeons.museum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.economy.CoinSound;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.museum.MuseumData.Entry;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Donating to the Museum and retrieving from it, as recorded (2026-10-25, 02:27.9 to 02:37.1). A click on an item in
 * their inventory, with a Museum menu open, asks to donate it ({@link ConfirmDonationMenu}); confirming takes it (an
 * armor set's every piece, from their inventory), marks it Co-op Soulbound, keeps it, says so with the XP it gave, plays
 * the coin chime and a level-up, and opens the menu it came from again. A click on a donated item in its category's
 * page gives it back, at once: it still counts. A lower tier can still go in once its higher tier has (the owner tried
 * it on Hypixel, 2026-10-25); it already counted, so it gives no more XP. What's refused, and what it says then, is
 * UNKNOWN (never recorded): an item the Museum doesn't take, one it already has, a set without all its pieces, a
 * retrieval with no room. Main thread.
 */
final class MuseumDonations {
    static final String RETRIEVED = "&7It still counts towards your &3Museum&7 progress, but not towards your &6total item value&7.";
    // UNKNOWN (never recorded): Hypixel's words for these.
    static final String CANT = "&cYou can't donate this item to your Museum!";
    static final String ALREADY = "&cYou have already donated this item to your Museum!";
    static final String NOT_ALL = "&cYou need every piece of this armor set in your inventory to donate it!";
    static final String FULL = "&cYour inventory is full!";
    static final String UNREADABLE = "&cThis item can't be retrieved right now.";

    private MuseumDonations() {
    }

    /** What a click would donate: an entry, and the inventory slot taken for each of its pieces (the clicked one's too). */
    record Plan(Entry entry, int clicked, List<Integer> slots) {
        Plan {
            slots = List.copyOf(slots);
        }
    }

    /** A plan, or what's said instead. */
    record Outcome(Plan plan, String refusal) {
    }

    /**
     * What clicking slot {@code clicked} would donate, given the SkyBlock id of what each inventory slot holds (null for
     * nothing, or what isn't kept). The first of the item's entries (a piece can be in two sets) that isn't in the
     * Museum (missing, retrieved, or counted only by its higher tier), with every piece there; a set takes the clicked
     * item for its piece and the first other for each other.
     */
    static Outcome plan(String[] ids, int clicked, Document profile, MuseumData data) {
        String id = clicked >= 0 && clicked < ids.length ? ids[clicked] : null;
        List<Entry> entries = data.entriesOf(id);
        if (entries.isEmpty()) return new Outcome(null, CANT);
        String refusal = ALREADY;
        for (Entry entry : entries) {
            Museum.Donation donation = Museum.donation(profile, entry.id());
            if (donation != null && !donation.retrieved()) continue;
            List<Integer> slots = slots(ids, clicked, entry);
            if (slots == null) {
                refusal = NOT_ALL;
                continue;
            }
            return new Outcome(new Plan(entry, clicked, slots), null);
        }
        return new Outcome(null, refusal);
    }

    /** The slot for each of the entry's pieces: the clicked one for its own; null if one isn't there. */
    private static List<Integer> slots(String[] ids, int clicked, Entry entry) {
        List<Integer> slots = new ArrayList<>();
        boolean[] used = new boolean[ids.length];
        Integer[] chosen = new Integer[entry.pieces().size()];
        // The clicked item first, for the piece it is.
        for (int p = 0; p < chosen.length; p++) {
            if (entry.pieces().get(p).contains(ids[clicked])) {
                chosen[p] = clicked;
                used[clicked] = true;
                break;
            }
        }
        for (int p = 0; p < chosen.length; p++) {
            if (chosen[p] != null) continue;
            for (int slot = 0; slot < ids.length && chosen[p] == null; slot++) {
                if (!used[slot] && ids[slot] != null && entry.pieces().get(p).contains(ids[slot])) {
                    chosen[p] = slot;
                    used[slot] = true;
                }
            }
            if (chosen[p] == null) return null;
        }
        slots.addAll(Arrays.asList(chosen));
        return slots;
    }

    // With the server

    /** The SkyBlock id of each slot of their inventory (its 36, hotbar first), null for nothing or what isn't kept. */
    static String[] ids(Player player) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        String[] ids = new String[contents.length];
        for (int i = 0; i < contents.length; i++) ids[i] = id(contents[i]);
        return ids;
    }

    private static String id(ItemStack stack) {
        if (!StoredInventory.saved(stack)) return null;
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag == null || tag.getString("id").isEmpty() ? null : tag.getString("id");
    }

    /**
     * A click on their own inventory's slot with a Museum menu open: asks to donate what's there, or says why not.
     *
     * @param back opens the menu it came from again (after the donation, or on Cancel)
     */
    static void clicked(Player viewer, int slot, Consumer<Player> back) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(viewer)) return;
        Outcome outcome = plan(ids(viewer), slot, user.profile(), Museum.data());
        if (outcome.refusal() != null) {
            viewer.sendMessage(Text.line(outcome.refusal()));
            return;
        }
        ItemStack shown = viewer.getInventory().getItem(slot);
        new ConfirmDonationMenu(viewer, outcome.plan(), shown == null ? null : shown.clone(), back).open(viewer, MenuSound.CLICK);
    }

    /** Confirmed: donates it, if it's all still where it was. */
    static void donate(Player viewer, Plan plan, ItemStack clicked, Consumer<Player> back) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(viewer)) return;
        Document profile = user.profile();
        MuseumData data = Museum.data();
        ItemStack[] contents = viewer.getInventory().getStorageContents();
        Outcome again = plan(ids(viewer), plan.clicked(), profile, data);
        ItemStack now = contents[plan.clicked()];
        if (again.plan() == null || !again.plan().equals(plan) || now == null || !now.isSimilar(clicked)) {
            // Moved, or gone: what it said then is UNKNOWN; this asks again.
            back.accept(viewer);
            return;
        }
        ItemStack[] given = new ItemStack[plan.slots().size()];
        for (int i = 0; i < given.length; i++) {
            int slot = plan.slots().get(i);
            ItemStack stack = contents[slot];
            given[i] = donated(stack);
            stack.setAmount(stack.getAmount() - 1);
            viewer.getInventory().setItem(slot, stack.getAmount() > 0 ? stack : null);
        }
        int before = Museum.xp(profile, data);
        List<Binary> stored = StoredInventory.encode(given);
        Museum.donate(profile, plan.entry().id(), System.currentTimeMillis(), stored);
        int gained = Museum.xp(profile, data) - before;
        user.save();
        viewer.sendMessage(Text.line(donatedLine(name(plan.entry(), given), plan.entry(), gained)));
        CoinSound.play(viewer);
        viewer.playSound(viewer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1, 1);
        // "+1 SkyBlock XP (Museum Donations) (96/100)" on the action bar, 0.1 s after (recorded).
        SkyBlockLevels.changed(viewer);
        back.accept(viewer);
    }

    /**
     * "&aYou donated your &fUndead Sword &ato the &cCombat &asection of the Museum! &b+1 SkyBlock XP", as recorded; with
     * no XP for one that already counted (donated again after it was retrieved: UNKNOWN on Hypixel).
     */
    static String donatedLine(String name, Entry entry, int gained) {
        return "&aYou donated your " + name + " &ato the " + MuseumMenu.colored(entry.category()) + " &asection of the Museum!"
                + (gained > 0 ? " &b+" + gained + " SkyBlock XP" : "");
    }

    /** "&aYou retrieved your &fUndead Sword &afrom the &3Museum&a!", as recorded. */
    static String retrievedLine(String name) {
        return "&aYou retrieved your " + name + " &afrom the &3Museum&a!";
    }

    /** One of it, marked as donated (so Co-op Soulbound), made again so its lore says so. */
    private static ItemStack donated(ItemStack stack) {
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null) {
            ItemStack one = stack.clone();
            one.setAmount(1);
            return one;
        }
        tag.setBoolean(ItemBuilder.DONATED_MUSEUM, true);
        return ItemBuilder.build(item, tag, 1);
    }

    /**
     * What the lines call it: an item by its name ("&fUndead Sword"); a set by its name in its first piece's rarity's
     * colour (UNKNOWN on Hypixel: no set was recorded).
     */
    static String name(Entry entry, ItemStack[] items) {
        ItemStack first = shown(items);
        NBTTagCompound tag = first == null ? null : ItemNBT.read(first);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null) return "&f" + entry.name();
        String named = ItemBuilder.name(item, tag).replace('§', '&');
        return entry.set() ? named.substring(0, 2) + entry.name() : named;
    }

    /** The one a donation shows: a set's helmet if it has one (UNKNOWN which Hypixel shows), else its first. */
    static ItemStack shown(ItemStack[] items) {
        ItemStack first = null;
        for (ItemStack item : items) {
            if (item == null || item.isEmpty()) continue;
            if (first == null) first = item;
            if (item.getType().getEquipmentSlot() == EquipmentSlot.HEAD) return item;
        }
        return first;
    }

    /** Its items, as stored: made again so they look as items do now; null for one that can't be read. */
    static ItemStack[] stored(Museum.Donation donation) {
        ItemStack[] items = new ItemStack[donation.items().size()];
        for (int i = 0; i < items.length; i++) {
            Object blob = donation.items().get(i);
            byte[] bytes = blob instanceof Binary binary ? binary.getData() : blob instanceof byte[] raw ? raw : null;
            if (bytes == null) continue;
            try {
                items[i] = ItemBuilder.refresh(ItemStack.deserializeBytes(bytes));
            } catch (RuntimeException e) {
                items[i] = null;
            }
        }
        return items;
    }

    /** Gives a donated entry's items back to them, if there's room: it still counts. */
    static void retrieve(Player viewer, Entry entry, Runnable reopen) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(viewer)) return;
        Document profile = user.profile();
        Museum.Donation donation = Museum.donation(profile, entry.id());
        if (donation == null || donation.retrieved()) return;
        ItemStack[] items = stored(donation);
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null && donation.items().get(i) != null) {
                // Its bytes stay in the Museum, for staff to look at.
                viewer.sendMessage(Text.line(UNREADABLE));
                return;
            }
        }
        List<ItemStack> back = new ArrayList<>();
        for (ItemStack item : items) if (item != null) back.add(item);
        int free = 0;
        for (ItemStack slot : viewer.getInventory().getStorageContents()) if (slot == null || slot.isEmpty()) free++;
        if (free < back.size()) {
            viewer.sendMessage(Text.line(FULL));
            return;
        }
        for (ItemStack item : back) viewer.getInventory().addItem(item);
        Museum.retrieved(profile, entry.id());
        user.save();
        viewer.sendMessage(Text.line(retrievedLine(name(entry, items))));
        viewer.sendMessage(Text.line(RETRIEVED));
        reopen.run();
    }
}
