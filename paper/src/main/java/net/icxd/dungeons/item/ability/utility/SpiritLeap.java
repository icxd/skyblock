package net.icxd.dungeons.item.ability.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.utils.Utils;
import net.icxd.dungeons.item.bonus.SetBonuses;

/**
 * Spirit Leap and the Infinileap's: "Allows you to teleport to any teammate! Grants 1 second of immunity after
 * teleporting, immunity is cancelled upon dealing damage." In a run, a right click opens "Spirit Leap" (the
 * menu's title as the Skytils, Skyblocker, SkyHanni and Odin mods read it) with the other members' heads,
 * named as they are in the run ("§b[MVP§6+§b] Name": Odin reads the name after the first space); a click on
 * one takes them there, "Dead players and players who have left the Dungeon cannot be teleported to" (the
 * wiki), and a Spirit Leap is used up ("1 Spirit Leap is consumed on teleport"; the Infinileap never is).
 * Then "You have teleported to Name!" (the text Odin reads; its colours are UNKNOWN). The cooldown (5 seconds,
 * the Infinileap's 2) is between leaps: opening the menu doesn't start it (UNKNOWN). UNKNOWN too: where the
 * heads are (slots 11 on, as Odin reads them), their lore (dead and left ones say so: Skyblocker looks for
 * "dead" and "offline" in it) and what a click on a dead one says (nothing). Not from the trap room (see {@link
 * #usable}). Not built: the Ice puzzle's own landing spot, and outside a run the Aspiring Leap's recipe.
 * A ghost's Haunt is the same menu for a ghost ({@link Haunt}).
 */
final class SpiritLeap implements AbilityHandler {
    static final String NAME = "Spirit Leap";
    /** "Grants 1 second of immunity after teleporting". */
    static final long IMMUNITY_MILLIS = 1_000;
    /** The one that's used up; the Infinileap isn't. */
    private static final String USED_UP = "SPIRIT_LEAP";
    private static final int FIRST_SLOT = 11;

    enum Status { HERE, DEAD, AWAY }

    /** A teammate in the menu, and whether they can be leapt to. */
    record Target(DungeonRun.Teammate teammate, Status status) {
    }

    /** The menu's teammates, in party order: here, dead (a ghost), or away (not in the run's world). */
    static List<Target> targets(List<DungeonRun.Teammate> teammates, Predicate<UUID> here) {
        List<Target> out = new ArrayList<>();
        for (DungeonRun.Teammate teammate : teammates) {
            Status status = teammate.ghost() ? Status.DEAD : here.test(teammate.id()) ? Status.HERE : Status.AWAY;
            out.add(new Target(teammate, status));
        }
        return out;
    }

    /**
     * In a run, but not from its trap room: "Spirit Leaps cannot be used in trap rooms; however, players can teleport
     * to other players in trap rooms" (the wiki). What it says there is UNKNOWN.
     */
    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        if (RunManager.of(player) == null) return false;
        if (!RunItems.inTrapRoom(player)) return true;
        player.sendMessage(Utils.color("&cYou can't use this in a trap room!"));
        return false;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        DungeonRun run = RunManager.of(player);
        if (run == null) return;
        // The cooldown is between leaps: it starts with one (see leap).
        PlayerSession.of(player).startCooldown(cooldownKey(), 0);
        List<Target> targets = targets(run.teammates(player.getUniqueId()), id -> {
            Player online = Bukkit.getPlayer(id);
            return online != null && run.isHere(online);
        });
        new Menu(NAME, run, player, targets, (long) (block.cooldown() * 1000), item.id(), false).open(player);
    }

    /**
     * A dungeon ghost's Haunt: "Teleport to an alive player!" ("Players can teleport to a chosen alive teammate by
     * using their Haunt ability", the wiki's Ghosts). The menu is Spirit Leap's, titled "Teleport to Player" (the
     * title the SkyHanni, Skytils and Odin mods read with Spirit Leap's); a click on a living teammate takes the
     * ghost there. Nothing is used up and there's no immunity (a ghost can't be hurt); its 2 s cooldown starts
     * when it's used, as other abilities' do. Only a ghost in a running run uses it.
     */
    static final class Haunt implements AbilityHandler {
        static final String TITLE = "Teleport to Player";

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            return RunItems.ghost(player);
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            DungeonRun run = RunManager.of(player);
            if (run == null) return;
            List<Target> targets = targets(run.teammates(player.getUniqueId()), id -> {
                Player online = Bukkit.getPlayer(id);
                return online != null && run.isHere(online);
            });
            new Menu(TITLE, run, player, targets, 0, item.id(), true).open(player);
        }
    }

    private static String cooldownKey() {
        return "ability:" + NAME;
    }

    /** A ghost haunting {@code target} (see {@link Haunt}), if they're still alive and here, and it's still a ghost. */
    private static void haunt(DungeonRun run, Player player, UUID target) {
        Player to = Bukkit.getPlayer(target);
        if (to == null || !run.isHere(to) || run.isGhost(target) || !run.isGhost(player.getUniqueId())) return;
        player.teleport(to.getLocation());
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        // Hypixel's words for a haunt are UNKNOWN: a Spirit Leap's.
        player.sendMessage(Utils.color("&aYou have teleported to " + to.getName() + "!"));
    }

    /** To {@code target}, if they can still be leapt to and the cooldown's over. */
    private static void leap(DungeonRun run, Player player, UUID target, long cooldownMillis, String itemId) {
        PlayerSession session = PlayerSession.of(player);
        long left = session.cooldownLeft(cooldownKey());
        if (left > 0) {
            player.sendMessage("§cThis ability is on cooldown for " + Abilities.cooldownSeconds(left) + "s.");
            return;
        }
        Player to = Bukkit.getPlayer(target);
        if (to == null || !run.isHere(to) || !run.isHere(player) || run.isGhost(target) || run.isGhost(player.getUniqueId())) return;
        if (USED_UP.equals(itemId.toUpperCase(Locale.ROOT)) && !useOne(player)) return;
        if (cooldownMillis > 0) session.startCooldown(cooldownKey(), cooldownMillis);
        player.teleport(to.getLocation());
        SetBonuses.teleported(player);
        player.setFallDistance(0);
        Protection.immunity(player, NAME, IMMUNITY_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        player.sendMessage(Utils.color("&aYou have teleported to " + to.getName() + "!"));
    }

    /** Uses up one of their Spirit Leaps (the one in their hand first); false if they have none. */
    private static boolean useOne(Player player) {
        PlayerInventory inventory = player.getInventory();
        int held = inventory.getHeldItemSlot();
        if (take(inventory, held)) return true;
        for (int slot = 0; slot < inventory.getSize(); slot++) if (slot != held && take(inventory, slot)) return true;
        return false;
    }

    private static boolean take(PlayerInventory inventory, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (stack == null || stack.isEmpty()) return false;
        NBTTagCompound tag = ItemNBT.read(stack);
        if (tag == null || !USED_UP.equals(tag.getString("id"))) return false;
        if (stack.getAmount() > 1) stack.setAmount(stack.getAmount() - 1);
        else inventory.setItem(slot, null);
        return true;
    }

    /** The teammates to leap to (or, for a ghost's Haunt, to haunt). */
    private static final class Menu extends GUI {
        Menu(String title, DungeonRun run, Player player, List<Target> targets, long cooldownMillis, String itemId, boolean haunting) {
            super(title, Size.THREE);
            fill(filler());
            for (int i = 0; i < targets.size() && FIRST_SLOT + i < Size.THREE - 9; i++) {
                Target target = targets.get(i);
                int slot = FIRST_SLOT + i;
                ItemStack head = head(target);
                set(new GUIClickableItem() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        if (target.status() != Status.HERE) return;
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(Dungeons.getInstance(), haunting
                                ? () -> haunt(run, player, target.teammate().id())
                                : () -> leap(run, player, target.teammate().id(), cooldownMillis, itemId));
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

        private static ItemStack head(Target target) {
            String lore = switch (target.status()) {
                case HERE -> "&eClick to teleport!";
                case DEAD -> "&cThis player is dead!";
                case AWAY -> "&cThis player is offline!";
            };
            ItemStack head = item(Material.PLAYER_HEAD, target.teammate().display(), lore);
            Player online = Bukkit.getPlayer(target.teammate().id());
            if (online != null) {
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setPlayerProfile(online.getPlayerProfile());
                head.setItemMeta(meta);
            }
            return head;
        }
    }
}
