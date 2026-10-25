package net.icxd.dungeons.hub;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.economy.Coins;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.storage.Bag;
import net.icxd.dungeons.storage.BagUpgrades;
import net.icxd.dungeons.storage.Storage;
import net.icxd.dungeons.storage.StorageTables;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Jacobus's menu, "Accessory Bag Upgrades", as recorded (2026-10-25, 00:53.5): four rows of glass, the upgrade at 11
 * and the Accessory Bag Shortcut at 15, Close at 31. The upgrade says where the bag's slots come from (its Redstone
 * Collection's, with the 9 everyone has, and Elizabeth's, none here: the Community Shop isn't) and what the next
 * one costs (see {@link BagUpgrades}). Clicking it with the coins in the purse buys it: 2 more slots, +2 SkyBlock XP
 * (on the action bar), and with the 5th the Jacobus Register (the wiki's). The shortcut opens the Accessory Bag, whose
 * Go Back comes back here.
 *
 * <p>UNKNOWN, not recorded: the line for the slots bought here (only once there are some), "Click to purchase!" with
 * enough coins, what a purchase says and sounds like (nothing), how the Register comes (into the inventory, or the
 * stash), and the upgrade without its cost once all 99 are bought (a click then says the wiki's line). Main thread.
 */
final class JacobusMenu extends GUI {
    static final String TITLE = "Accessory Bag Upgrades";
    static final int UPGRADE = 11;
    static final int SHORTCUT = 15;
    static final int CLOSE = 31;
    static final String REGISTER_HEAD = "3b648b9a44e280bcdf25f4a66a97bd5c33542e5e82415e15b475c6b999b8d635";
    static final String REGISTER = "JACOBUS_REGISTER";
    static final String MAXED = "&cYou have reached the maximum amount of Jacobus upgrades! That was NOT a smart business decision.";

    private final Player viewer;

    private JacobusMenu(Player viewer) {
        super(TITLE, Size.FOUR);
        this.viewer = viewer;
    }

    static void show(Player player, MenuSound sound) {
        new JacobusMenu(player).open(player, sound);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        Document profile = user == null ? new Document() : user.profile();
        int fromCollection = Bag.ACCESSORY_BAG.fromCollection(profile, StorageTables.get());
        set(GUIClickableItem.button(UPGRADE, upgrade(profile, fromCollection, user == null ? 0 : Purse.coins(user)).stack(), viewer, this::buy));
        set(GUIClickableItem.button(SHORTCUT, shortcut().stack(), viewer,
                () -> Storage.openBag(viewer, Bag.ACCESSORY_BAG, "&7To " + TITLE, back -> show(back, MenuSound.CLICK))));
        set(GUIClickableItem.close(CLOSE));
    }

    private void buy() {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(viewer)) return;
        Document profile = user.profile();
        int next = BagUpgrades.bought(profile) + 1;
        if (next > BagUpgrades.MOST) {
            viewer.sendMessage(Text.line(MAXED));
            return;
        }
        if (!Purse.take(user, BagUpgrades.cost(next))) return;
        BagUpgrades.bought(profile, next);
        if (next == BagUpgrades.REGISTER) giveRegister();
        // The bag is bigger: read again.
        Storage.changed(viewer);
        SkyBlockLevels.changed(viewer);
        open(viewer, null);
    }

    private void giveRegister() {
        SkyBlockItem register = ItemRegistry.get(REGISTER);
        if (register != null) ItemStash.give(viewer, ItemBuilder.build(register));
    }

    /** The upgrade, as recorded with none bought and 0 coins. */
    static Icon upgrade(Document profile, int fromCollection, double coins) {
        int bought = BagUpgrades.bought(profile);
        List<String> lore = new ArrayList<>(List.of("&7Increase the size of your", "&aAccessory Bag&7.", "",
                "&7Redstone collection: &c" + fromCollection + " Slots", "&7Elizabeth: &d+0 Slots"));
        if (bought > 0) lore.add("&7Jacobus: &6+" + BagUpgrades.slots(profile) + " Slots");
        if (bought < BagUpgrades.MOST) {
            long cost = BagUpgrades.cost(bought + 1);
            lore.addAll(List.of("", "&7Buying: &6+" + BagUpgrades.SLOTS + " Slots", "&7Cost: &6" + Coins.format(cost) + " Coins", ""));
            lore.add(coins >= cost ? "&eClick to purchase!" : "&cCan't afford upgrade!");
        }
        return new Icon(Material.PLAYER_HEAD, "&aAccessory Bag Upgrades", lore, REGISTER_HEAD);
    }

    static Icon shortcut() {
        return new Icon(Material.PLAYER_HEAD, "&aAccessory Bag Shortcut", List.of("&7Quickly access your accessory bag", "&7from right here!", "",
                "&eClick to open!"), Bag.ACCESSORY_BAG.texture());
    }
}
