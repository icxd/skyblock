package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.User;

/**
 * Select Power Stone, as recorded (the Loadouts and Storage tour, 01:07.4, 01:11.0 and 01:34.4, each from a
 * loadout's Power Stone): the powers they may pick (see {@link Powers#unlocked}), best first, each with its
 * stats at their Accessory Power and "Click to select reforge!"; the one the loadout has is lime glass,
 * "Power is selected!". Picking one gives it to the loadout and goes back to it; Clear Selection sets it to
 * None. From Loadouts' own Power Stone ("Choose your selected Power Stone.") it's the same menu for the
 * power they have selected now (not recorded: UNKNOWN), and goes back to Loadouts. More powers than its 28
 * slots go on more pages, with Previous and Next Page in the bottom corners as the wiki's Accessory Bag
 * Thaumaturgy has them (UNKNOWN: this menu's own, only one page was recorded). Main thread.
 */
final class PowerMenu extends GUI {
    static final String TITLE = "Select Power Stone";
    /** Where the powers go, seven a row. */
    static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    static final int PREVIOUS = 45;
    static final int GO_BACK = 48;
    static final int CLOSE = 49;
    static final int CLEAR = 50;
    static final int NEXT = 53;

    private final Player viewer;
    /** The loadout it's for (from 0), or -1 for their selected power. */
    private final int loadout;
    /** From 0. */
    private final int page;

    PowerMenu(Player viewer, int loadout) {
        this(viewer, loadout, 0);
    }

    private PowerMenu(Player viewer, int loadout, int page) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
        this.loadout = loadout;
        this.page = Math.max(0, page);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        String selected = selected(user);
        String back = loadout < 0 ? "Loadouts" : Loadouts.get(user.profile(), loadout).name();
        List<StorageTables.Power> all = AccessoryBag.unlockedPowers(user.profile());
        int pages = pages(all.size());
        int shown = Math.min(page, pages - 1);
        List<StorageTables.Power> powers = all.subList(shown * SLOTS.length, Math.min(all.size(), (shown + 1) * SLOTS.length));
        Map<Integer, Icon> icons = icons(powers, selected, AccessoryBag.accessoryPower(viewer), StorageTables.get(), back, shown, pages);
        for (Map.Entry<Integer, Icon> e : icons.entrySet()) {
            int slot = e.getKey();
            ItemStack stack = e.getValue().stack();
            int at = indexOf(slot);
            if (at >= 0 && at < powers.size()) {
                StorageTables.Power power = powers.get(at);
                if (power.name().equals(selected)) {
                    set(slot, stack);
                    continue;
                }
                set(GUIClickableItem.button(slot, LoadoutsMenu.powerLook(stack, power), viewer, () -> choose(power.name())));
                continue;
            }
            switch (slot) {
                case GO_BACK -> set(GUIClickableItem.button(slot, stack, viewer, this::back));
                case CLEAR -> set(GUIClickableItem.button(slot, stack, viewer, () -> choose(null)));
                case PREVIOUS -> set(GUIClickableItem.button(slot, stack, viewer, () -> new PowerMenu(viewer, loadout, shown - 1).open(viewer)));
                case NEXT -> set(GUIClickableItem.button(slot, stack, viewer, () -> new PowerMenu(viewer, loadout, shown + 1).open(viewer)));
                default -> set(slot, stack);
            }
        }
        set(GUIClickableItem.close(CLOSE));
    }

    /** How many pages this many powers take: {@link #SLOTS} a page, at least one. */
    static int pages(int powers) {
        return Math.max(1, (powers + SLOTS.length - 1) / SLOTS.length);
    }

    private String selected(User user) {
        if (loadout >= 0) return Loadouts.get(user.profile(), loadout).power();
        StorageTables.Power power = AccessoryBag.selectedPower(user.profile());
        return power == null ? null : power.name();
    }

    private static int indexOf(int slot) {
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == slot) return i;
        return -1;
    }

    /**
     * The menu's slots but Close: page {@code page}'s powers (of {@code pages}, from 0) in {@link #SLOTS}, Go
     * Back ("To Loadout 2"), Clear Selection, and the arrows to the other pages.
     */
    static Map<Integer, Icon> icons(List<StorageTables.Power> powers, String selected, int accessoryPower, StorageTables tables, String back,
                                    int page, int pages) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        for (int i = 0; i < powers.size() && i < SLOTS.length; i++) icons.put(SLOTS[i], power(powers.get(i), powers.get(i).name().equals(selected),
                accessoryPower, tables));
        if (page > 0) icons.put(PREVIOUS, new Icon(Material.ARROW, "&aPrevious Page", "&ePage " + page));
        icons.put(GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To " + back));
        icons.put(CLEAR, SetsMenu.clearSelection());
        if (page < pages - 1) icons.put(NEXT, new Icon(Material.ARROW, "&aNext Page", "&ePage " + (page + 2)));
        return icons;
    }

    /**
     * A power as the menu shows it: its tier, stats at this Accessory Power, Unique Power Bonus and the
     * Accessory Power it's worked out at. A Stone Power's is a head (its stone's, see LoadoutsMenu#powerLook),
     * the others are what the recording shows them as (a stick for Commando).
     */
    static Icon power(StorageTables.Power power, boolean selected, int accessoryPower, StorageTables tables) {
        List<String> lore = new ArrayList<>(List.of("&8" + power.tier(), "", "&7Stats:"));
        lore.addAll(Powers.statLines(power, accessoryPower, tables));
        lore.add("");
        if (!power.bonus().isEmpty()) {
            lore.add("&7Unique Power Bonus:");
            lore.addAll(Powers.bonusLines(power));
            lore.add("");
        }
        lore.addAll(List.of("&7You have: &6" + accessoryPower + " Magical Power", ""));
        lore.add(selected ? "&aPower is selected!" : "&eClick to select reforge!");
        Material material = selected ? Material.LIME_STAINED_GLASS_PANE : material(power);
        return new Icon(material, "&a" + power.name(), lore);
    }

    /** What a power shows as: a head for a Stone Power, else its material (stone if the table's is unknown). */
    static Material material(StorageTables.Power power) {
        if (power.stonePower()) return Material.PLAYER_HEAD;
        Material material = power.icon() == null ? null : Material.matchMaterial(power.icon());
        return material == null ? Material.STONE : material;
    }

    /** The loadout's power (or their selected one), then back where they came from. */
    private void choose(String power) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        if (loadout >= 0) {
            Loadouts.put(user.profile(), loadout, Loadouts.get(user.profile(), loadout).withPower(power));
        } else {
            AccessoryBag.select(user.profile(), power);
            AccessoryBag.changed(viewer);
            PlayerSession.of(viewer).invalidateStats();
        }
        back();
    }

    private void back() {
        if (loadout >= 0) new LoadoutMenu(viewer, loadout).open(viewer);
        else new LoadoutsMenu(viewer, 0).open(viewer);
    }
}
