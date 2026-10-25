package net.icxd.dungeons.fairysouls;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;

/**
 * Tia's menu, "Fairy", as the wiki has it (Tia the Fairy/UI; never recorded): glass with the exchange in the third
 * row's middle and Close at the bottom, six rows (UNKNOWN: the wiki's menus are six when it doesn't say). The
 * exchange says how many souls they have to give and what the next exchange gives: +10 SkyBlock XP, and the
 * backpack slot it unlocks on Hypixel (every slot is open here anyway, see StorageMenu). With five or more, a click
 * gives her five; what she says then is UNKNOWN, so nothing is said, and the SkyBlock XP shows on the action bar as
 * any gain's does. Main thread.
 */
final class TiaMenu extends GUI {
    static final String TITLE = "Fairy";
    static final int EXCHANGE = 22;
    static final int CLOSE = 49;
    /** The wiki's "+10 SkyBlock XP" an exchange (the leveling data's Fairy Souls task, too). */
    static final int XP = 10;

    private final Player viewer;

    private TiaMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    static void show(Player player) {
        new TiaMenu(player).open(player, MenuSound.CLICK);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        Document profile = user == null ? new Document() : user.profile();
        set(GUIClickableItem.button(EXCHANGE, exchange(profile).stack(), viewer, () -> {
            User now = User.ifLoaded(viewer.getUniqueId());
            if (now == null || now.isReleased() || InventorySyncListener.frozen(viewer) || !FairySouls.exchange(now.profile())) return;
            SkyBlockLevels.changed(viewer);
            open(viewer, null);
        }));
        set(GUIClickableItem.close(CLOSE));
    }

    /** "&aExchange Fairy Souls", as the wiki has it with none: "&7Fairy Souls: &e0&7/&d5", the next reward, why not. */
    static Icon exchange(Document profile) {
        int unspent = FairySouls.unspent(profile);
        int next = FairySouls.exchanges(profile) + 1;
        List<String> lore = new ArrayList<>(List.of("&7Find &dFairy Souls &7around the", "&7world and bring them back to me",
                "&7and I will reward you with", "&7SkyBlock XP and Backpack Slots!", "",
                "&7Fairy Souls: &e" + unspent + "&7/&d" + FairySouls.PER_EXCHANGE, "", "&7Next Reward:", "&8+&b" + XP + " SkyBlock XP"));
        int slot = FairySouls.backpackSlot(next);
        if (slot > 0) lore.add("&6Backpack Slot #" + slot);
        lore.add("");
        // UNKNOWN: the line with enough; Hypixel's clickable items say "Click to ...".
        lore.add(unspent >= FairySouls.PER_EXCHANGE ? "&eClick to exchange!" : "&cYou don't have enough Fairy Souls!");
        return new Icon(Material.PLAYER_HEAD, "&aExchange Fairy Souls", lore, FairySouls.HEAD);
    }
}
