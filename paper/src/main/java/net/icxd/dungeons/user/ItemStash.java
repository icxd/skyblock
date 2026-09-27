package net.icxd.dungeons.user;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.listeners.InventorySyncListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Hypixel's item stash: items that go straight to a player (a dungeon mob's drops) and don't fit in their
 * inventory wait there, with Hypixel's messages (recorded, research critic.md 3.4): one when something goes
 * in, and a reminder every minute while anything is in it, until {@code /pickupstash} takes out what fits.
 * Kept on the profile they play on, under {@code stash} (each item as bytes, like the stored inventory), so
 * it follows them between servers. Main thread.
 */
public final class ItemStash {
    static final String STASH = "stash";
    /** "The Item Stash can hold up to 720 items" (the wiki); what happens to more is UNKNOWN, so they drop at their feet. */
    public static final int LIMIT = 720;
    /** The recorded reminders came a minute apart. */
    public static final long REMIND_TICKS = 1200;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private ItemStash() {
    }

    /** Into their inventory; what doesn't fit into their stash (and they're told), past its limit at their feet. */
    public static void give(Player player, ItemStack... items) {
        List<ItemStack> left = new ArrayList<>(player.getInventory().addItem(items).values());
        if (left.isEmpty()) return;
        User user = User.cached(player.getUniqueId());
        if (user == null || !user.isLoaded() || InventorySyncListener.frozen(player)) {
            for (ItemStack item : left) player.getWorld().dropItemNaturally(player.getLocation(), item);
            return;
        }
        List<ItemStack> stash = items(user.profile());
        int count = count(stash);
        List<ItemStack> over = new ArrayList<>();
        for (ItemStack item : left) {
            if (count + item.getAmount() > LIMIT) {
                over.add(item);
                continue;
            }
            stash.add(item);
            count += item.getAmount();
        }
        for (ItemStack item : over) player.getWorld().dropItemNaturally(player.getLocation(), item);
        if (over.size() == left.size()) return;
        put(user.profile(), stash);
        user.save();
        player.sendMessage(stashedMessage());
    }

    /** How many items are waiting in their stash (0 if their data isn't here). */
    public static int count(Player player) {
        User user = User.cached(player.getUniqueId());
        return user == null || !user.isLoaded() ? 0 : count(items(user.profile()));
    }

    /**
     * {@code /pickupstash}: as much as fits into their inventory. What's still left is reminded of at once
     * (what Hypixel says after a pick up is UNKNOWN).
     */
    public static void pickUp(Player player) {
        User user = User.cached(player.getUniqueId());
        if (user == null || !user.isLoaded() || InventorySyncListener.frozen(player)) return;
        List<ItemStack> stash = items(user.profile());
        if (stash.isEmpty()) return;
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack item : stash) left.addAll(player.getInventory().addItem(item).values());
        put(user.profile(), left);
        user.save();
        if (!left.isEmpty()) remind(player);
    }

    /** Every minute: everyone with something in their stash is reminded. */
    public static void remindAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (count(player) > 0) remind(player);
        }
    }

    private static void remind(Player player) {
        for (Component line : reminder(count(player))) player.sendMessage(line);
    }

    /** "One or more items didn't fit ... Click here to pick them up!", which picks them up (R1 02:06.8). */
    static Component stashedMessage() {
        return legacy("&eOne or more items didn't fit in your inventory and were added to your item stash! &6Click here &eto pick them up!")
                .clickEvent(ClickEvent.runCommand("/pickupstash"))
                .hoverEvent(HoverEvent.showText(legacy("&eClick to pickup!")));
    }

    /**
     * The minute's reminder, as recorded: a blank line, how many, the CLICK HERE line and another blank
     * ("items" as recorded; whether one item reads "item" is UNKNOWN).
     */
    static List<Component> reminder(int items) {
        HoverEvent<Component> hover = HoverEvent.showText(legacy("&eClick to pickup your items!"));
        ClickEvent click = ClickEvent.runCommand("/viewstash item");
        return List.of(Component.text(" "),
                legacy("&f                     &7You have &a" + items + " &7items stashed away!")
                        .clickEvent(click).hoverEvent(hover),
                legacy("&f                &6&l>>> &6&lCLICK HERE&e to pick them up! &6&l<<<").clickEvent(click).hoverEvent(hover),
                Component.text("  "));
    }

    private static Component legacy(String text) {
        return LEGACY.deserialize(text);
    }

    /** How many items these are (a stack of 3 is 3). */
    static int count(List<ItemStack> items) {
        int count = 0;
        for (ItemStack item : items) count += item.getAmount();
        return count;
    }

    /** What's in a profile's stash; items that can't be read are left out. */
    static List<ItemStack> items(Document profile) {
        List<ItemStack> out = new ArrayList<>();
        if (!(profile.get(STASH) instanceof List<?> list)) return out;
        for (Object entry : list) {
            byte[] bytes = entry instanceof Binary binary ? binary.getData() : entry instanceof byte[] raw ? raw : null;
            if (bytes == null) continue;
            try {
                out.add(ItemStack.deserializeBytes(bytes));
            } catch (RuntimeException ignored) {
                // Unreadable (a newer version's): skipped.
            }
        }
        return out;
    }

    private static void put(Document profile, List<ItemStack> items) {
        List<Binary> out = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) out.add(new Binary(item.serializeAsBytes()));
        }
        profile.put(STASH, out);
    }
}
