package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.utils.Utils;

/**
 * Revive Stones (MCW Revive Stone): one in your inventory when you die brings you back where you are
 * (see {@link Ghosts#died}); right-clicked, it revives a dead teammate after 5 seconds, with a menu to
 * pick one when several are dead, or "There are no players available to revive right now!". They're
 * gone when you leave the run ("This item will vanish from your inventory at the end of the
 * Dungeon!"): marked as run items like the map, and taken by id too, since an item made again from
 * its data (switching to it in the hotbar does that) loses the mark and a mob's drop never had it;
 * and by id they're never saved with a player's items (see {@link StoredInventory#neverSave}), so
 * leaving another way (sent to another server) doesn't keep one either.
 */
final class ReviveStones {
    /** The item's id in the item data. */
    static final String ID = "REVIVE_STONE";

    private ReviveStones() {
    }

    /** A Revive Stone, marked as a run item. Null if the item data has none. */
    static ItemStack item() {
        SkyBlockItem item = ItemRegistry.get(ID);
        if (item == null) return null;
        ItemStack stack = ItemBuilder.build(item);
        StoredInventory.markNotSaved(stack);
        return stack;
    }

    static boolean is(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag != null && ID.equals(tag.getString("id"));
    }

    /** How many a player carries, for the tab list's "Revive Stones: 2". */
    static int count(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) if (is(stack)) count += stack.getAmount();
        return count;
    }

    /** Uses up one of theirs; false if they have none. */
    static boolean take(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!is(stack)) continue;
            if (stack.getAmount() > 1) stack.setAmount(stack.getAmount() - 1);
            else inventory.setItem(slot, null);
            return true;
        }
        return false;
    }

    /** Gives one (to the inventory, or at their feet if it's full). */
    static void give(Player player) {
        ItemStack stack = item();
        if (stack == null) return;
        for (ItemStack left : player.getInventory().addItem(stack).values()) player.getWorld().dropItemNaturally(player.getLocation(), left);
    }

    /** Takes every Revive Stone away (leaving the run). */
    static void takeAll(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) if (is(inventory.getItem(slot))) inventory.setItem(slot, null);
    }

    /**
     * A right click with one in the run: one dead teammate is revived, several get a menu to pick from.
     * The menu's layout is UNKNOWN (MCW only says it exists): their heads in a row. A ghost another
     * Revive Stone is already bringing back isn't available.
     */
    static void use(DungeonRun run, Player player) {
        List<UUID> dead = new ArrayList<>();
        for (UUID id : run.ghosts().all()) {
            if (!id.equals(player.getUniqueId()) && Bukkit.getPlayer(id) != null && !run.ghosts().beingRevived(id)) dead.add(id);
        }
        if (dead.isEmpty()) {
            player.sendMessage(Utils.color(DeathText.NOBODY_TO_REVIVE));
            return;
        }
        if (dead.size() == 1) {
            start(run, player, dead.get(0));
            return;
        }
        new Pick(run, player, dead).open(player);
    }

    /**
     * "The reviving process itself takes 5 seconds": the stone goes now, the teammate comes back at the
     * reviver's side. One stone per ghost at a time; if the ghost is back another way meanwhile (its
     * timer, a fairy) or has left, the stone is given back (what Hypixel does then is UNKNOWN).
     */
    private static void start(DungeonRun run, Player player, UUID target) {
        // Picked from a menu that was open a while: they may be back, or another stone on its way.
        if (!run.ghosts().isGhost(target) || run.ghosts().beingRevived(target)) {
            player.sendMessage(Utils.color(DeathText.NOBODY_TO_REVIVE));
            return;
        }
        if (!take(player)) return;
        run.ghosts().startRevive(target);
        run.later(DeathRules.REVIVE_STONE_TICKS, () -> {
            run.ghosts().endRevive(target);
            Player ghost = Bukkit.getPlayer(target);
            boolean here = player.isOnline() && player.getWorld().equals(run.world);
            if (ghost == null || !run.ghosts().isGhost(target)) {
                if (here) give(player);
                return;
            }
            // Where they come back is UNKNOWN; next to whoever revived them (MCW mentions a bug that does so).
            run.ghosts().revive(ghost, here ? player.getLocation() : null);
        });
    }

    /** Which dead teammate to revive. */
    private static final class Pick extends GUI {
        Pick(DungeonRun run, Player player, List<UUID> dead) {
            super("Revive a Teammate", Size.THREE);
            fill(filler());
            for (int i = 0; i < dead.size() && i < 7; i++) {
                UUID id = dead.get(i);
                DungeonRun.Member member = run.member(id);
                int slot = 10 + i;
                ItemStack head = item(Material.PLAYER_HEAD, member == null ? "&7?" : member.display(), "", "&eClick to revive!");
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                Player online = Bukkit.getPlayer(id);
                if (online != null) meta.setPlayerProfile(online.getPlayerProfile());
                head.setItemMeta(meta);
                set(new GUIClickableItem() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> start(run, player, id));
                    }

                    @Override
                    public int slot() {
                        return slot;
                    }

                    @Override
                    public ItemStack stack() {
                        return head;
                    }
                });
            }
        }
    }
}
