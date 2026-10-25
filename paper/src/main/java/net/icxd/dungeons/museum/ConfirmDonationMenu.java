package net.icxd.dungeons.museum;

import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;

/**
 * "Confirm Donation", as recorded (2026-10-25, 02:29.0): the clicked item at the top (a set's clicked piece; UNKNOWN
 * which Hypixel shows), Confirm Donation with its warning, and Cancel, in four rows of glass. Confirming donates it
 * ({@link MuseumDonations#donate}); Cancel goes back (UNKNOWN: never recorded). Main thread.
 */
final class ConfirmDonationMenu extends GUI {
    static final String TITLE = "Confirm Donation";
    static final int ITEM = 4;
    static final int CONFIRM = 20;
    static final int CANCEL = 24;
    static final Icon CONFIRM_ICON = new Icon(Material.LIME_TERRACOTTA, "&aConfirm Donation", "&cWARNING: Donating the above item to",
            "&cyour Museum marks it as Soulbound.", "", "&7This means you can no longer trade", "&7off the profile, auction, or sell any",
            "&7item that you donate to your Museum.", "", "&eClick to confirm!");
    static final Icon CANCEL_ICON = new Icon(Material.RED_TERRACOTTA, "&cCancel");

    private final Player viewer;
    private final MuseumDonations.Plan plan;
    private final ItemStack clicked;
    private final Consumer<Player> back;

    ConfirmDonationMenu(Player viewer, MuseumDonations.Plan plan, ItemStack clicked, Consumer<Player> back) {
        super(TITLE, Size.FOUR);
        this.viewer = viewer;
        this.plan = plan;
        this.clicked = clicked;
        this.back = back;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        if (clicked != null) set(ITEM, clicked.clone());
        set(GUIClickableItem.button(CONFIRM, CONFIRM_ICON.stack(), viewer, () -> MuseumDonations.donate(viewer, plan, clicked, back)));
        set(GUIClickableItem.button(CANCEL, CANCEL_ICON.stack(), viewer, () -> back.accept(viewer)));
    }
}
